/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.progress.impl;

import com.google.common.collect.ConcurrentHashMultiset;
import com.intellij.concurrency.JobScheduler;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import com.intellij.util.containers.ConcurrentLongObjectMap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.SmartHashSet;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jetbrains.annotations.Nls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CoreProgressManager extends ProgressManager implements Disposable {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.progress.impl.CoreProgressManager");

  static final int CHECK_CANCELED_DELAY_MILLIS = 10;
  final AtomicInteger myCurrentUnsafeProgressCount = new AtomicInteger(0);
  private final AtomicInteger myCurrentModalProgressCount = new AtomicInteger(0);

  private static final boolean ENABLED = !"disabled".equals(System.getProperty("idea.ProcessCanceledException"));
  private static CheckCanceledHook ourCheckCanceledHook;
  private ScheduledFuture<?> myCheckCancelledFuture; // guarded by threadsUnderIndicator

  // indicator -> threads which are running under this indicator. guarded by threadsUnderIndicator.
  private static final Map<ProgressIndicator, Set<Thread>> threadsUnderIndicator = new THashMap<>();
  // the active indicator for the thread id
  private static final ConcurrentLongObjectMap<ProgressIndicator> currentIndicators = ContainerUtil.createConcurrentLongObjectMap();
  // top-level indicators for the thread id
  private static final ConcurrentLongObjectMap<ProgressIndicator> threadTopLevelIndicators = ContainerUtil.createConcurrentLongObjectMap();
  // threads which are running under canceled indicator
  static final Set<Thread> threadsUnderCanceledIndicator = new THashSet<>(); // guarded by threadsUnderIndicator
  private static volatile boolean shouldCheckCanceled;

  /** active (i.e. which have {@link #executeProcessUnderProgress(Runnable, ProgressIndicator)} method running) indicators
   *  which are not inherited from {@link StandardProgressIndicator}.
   *  for them an extra processing thread (see {@link #myCheckCancelledFuture}) has to be run
   *  to call their non-standard {@link ProgressIndicator#checkCanceled()} method periodically.
   */
  // multiset here (instead of a set) is for simplifying add/remove indicators on process-with-progress start/end with possibly identical indicators.
  private static final Collection<ProgressIndicator> nonStandardIndicators = ConcurrentHashMultiset.create();

  /** true if running in non-cancelable section started with
   * {@link #startNonCancelableSection()} or {@link #executeNonCancelableSection(Runnable)} in this thread
   */
  private static final ThreadLocal<Boolean> isInNonCancelableSection = new ThreadLocal<>(); // do not supply initial value to conserve memory

  // must be under threadsUnderIndicator lock
  private void startBackgroundNonStandardIndicatorsPing() {
    if (myCheckCancelledFuture == null) {
      myCheckCancelledFuture = JobScheduler.getScheduler().scheduleWithFixedDelay(() -> {
        for (ProgressIndicator indicator : nonStandardIndicators) {
          try {
            indicator.checkCanceled();
          }
          catch (ProcessCanceledException e) {
            indicatorCanceled(indicator);
          }
        }
      }, 0, CHECK_CANCELED_DELAY_MILLIS, TimeUnit.MILLISECONDS);
    }
  }

  // must be under threadsUnderIndicator lock
  private void stopBackgroundNonStandardIndicatorsPing() {
    if (myCheckCancelledFuture != null) {
      myCheckCancelledFuture.cancel(true);
      myCheckCancelledFuture = null;
    }
  }

  @Override
  public void dispose() {
    synchronized (threadsUnderIndicator) {
      stopBackgroundNonStandardIndicatorsPing();
    }
  }

  static boolean isThreadUnderIndicator(@Nonnull ProgressIndicator indicator, @Nonnull Thread thread) {
    synchronized (threadsUnderIndicator) {
      Set<Thread> threads = threadsUnderIndicator.get(indicator);
      return threads != null && threads.contains(thread);
    }
  }

  public static boolean runCheckCanceledHooks(@Nullable ProgressIndicator indicator) {
    CheckCanceledHook hook = ourCheckCanceledHook;
    return hook != null && hook.runHook(indicator);
  }

  @Override
  protected void doCheckCanceled() throws ProcessCanceledException {
    if (!shouldCheckCanceled) return;

    final ProgressIndicator progress = getProgressIndicator();
    if (progress != null && ENABLED) {
      progress.checkCanceled();
    }
    else {
      runCheckCanceledHooks(progress);
    }
  }

  @Override
  public boolean hasProgressIndicator() {
    return getProgressIndicator() != null;
  }

  @Override
  public boolean hasUnsafeProgressIndicator() {
    return myCurrentUnsafeProgressCount.get() > 0;
  }

  @Override
  public boolean hasModalProgressIndicator() {
    return myCurrentModalProgressCount.get() > 0;
  }

  @Override
  public void runProcess(@Nonnull final Runnable process, final ProgressIndicator progress) {
    executeProcessUnderProgress(() -> {
      try {
        try {
          if (progress != null && !progress.isRunning()) {
            progress.start();
          }
        }
        catch (RuntimeException e) {
          throw e;
        }
        catch (Throwable e) {
          throw new RuntimeException(e);
        }
        process.run();
      }
      finally {
        if (progress != null && progress.isRunning()) {
          progress.stop();
          if (progress instanceof ProgressIndicatorEx) {
            ((ProgressIndicatorEx)progress).processFinish();
          }
        }
      }
    }, progress);
  }

  @Override
  public <T> T runProcess(@Nonnull final Computable<T> process, ProgressIndicator progress) throws ProcessCanceledException {
    final Ref<T> ref = new Ref<>();
    runProcess(() -> ref.set(process.compute()), progress);
    return ref.get();
  }

  @Override
  public void executeNonCancelableSection(@Nonnull Runnable runnable) {
    if (isInNonCancelableSection()) {
      runnable.run();
    }
    else {
      try {
        isInNonCancelableSection.set(Boolean.TRUE);
        executeProcessUnderProgress(runnable, NonCancelableIndicator.INSTANCE);
      }
      finally {
        isInNonCancelableSection.remove();
      }
    }
  }

  @Override
  public void setCancelButtonText(String cancelButtonText) {

  }

  @Override
  public boolean runProcessWithProgressSynchronously(@Nonnull Runnable process,
                                                     @Nonnull @Nls String progressTitle,
                                                     boolean canBeCanceled,
                                                     @Nullable Project project) {
    return runProcessWithProgressSynchronously(process, progressTitle, canBeCanceled, project, null);
  }

  @Override
  public <T, E extends Exception> T runProcessWithProgressSynchronously(@Nonnull final ThrowableComputable<T, E> process,
                                                                        @Nonnull @Nls String progressTitle,
                                                                        boolean canBeCanceled,
                                                                        @Nullable Project project) throws E {
    final AtomicReference<T> result = new AtomicReference<>();
    final AtomicReference<Throwable> exception = new AtomicReference<>();

    runProcessWithProgressSynchronously(new Task.Modal(project, progressTitle, canBeCanceled) {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        try {
          T compute = process.compute();
          result.set(compute);
        }
        catch (Throwable t) {
          exception.set(t);
        }
      }
    }, null);

    //noinspection ThrowableResultOfMethodCallIgnored
    Throwable t = exception.get();
    if (t != null) {
      if (t instanceof Error) throw (Error)t;
      if (t instanceof RuntimeException) throw (RuntimeException)t;
      @SuppressWarnings("unchecked") E e = (E)t;
      throw e;
    }

    return result.get();
  }

  @Override
  public boolean runProcessWithProgressSynchronously(@Nonnull final Runnable process,
                                                     @Nonnull @Nls String progressTitle,
                                                     boolean canBeCanceled,
                                                     @Nullable Project project,
                                                     @Nullable JComponent parentComponent) {
    Task.Modal task = new Task.Modal(project, progressTitle, canBeCanceled) {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        process.run();
      }
    };
    return runProcessWithProgressSynchronously(task, parentComponent);
  }

  @Override
  public void runProcessWithProgressAsynchronously(@Nonnull Project project,
                                                   @Nonnull @Nls String progressTitle,
                                                   @Nonnull Runnable process,
                                                   @Nullable Runnable successRunnable,
                                                   @Nullable Runnable canceledRunnable) {
    runProcessWithProgressAsynchronously(project, progressTitle, process, successRunnable, canceledRunnable, PerformInBackgroundOption.DEAF);
  }

  @Override
  public void runProcessWithProgressAsynchronously(@Nonnull Project project,
                                                   @Nonnull @Nls String progressTitle,
                                                   @Nonnull final Runnable process,
                                                   @Nullable final Runnable successRunnable,
                                                   @Nullable final Runnable canceledRunnable,
                                                   @Nonnull PerformInBackgroundOption option) {
    runProcessWithProgressAsynchronously(new Task.Backgroundable(project, progressTitle, true, option) {
      @Override
      public void run(@Nonnull final ProgressIndicator indicator) {
        process.run();
      }


      @Override
      public void onCancel() {
        if (canceledRunnable != null) {
          canceledRunnable.run();
        }
      }

      @Override
      public void onSuccess() {
        if (successRunnable != null) {
          successRunnable.run();
        }
      }
    });
  }

  @Override
  public void run(@Nonnull final Task task) {
    if (task.isHeadless()) {
      if (ApplicationManager.getApplication().isDispatchThread()) {
        runProcessWithProgressSynchronously(task, null);
      }
      else {
        runProcessWithProgressInCurrentThread(task, new EmptyProgressIndicator(), ModalityState.defaultModalityState());
      }
    }
    else if (task.isModal()) {
      runSynchronously(task.asModal());
    }
    else {
      final Task.Backgroundable backgroundable = task.asBackgroundable();
      if (backgroundable.isConditionalModal() && !backgroundable.shouldStartInBackground()) {
        runSynchronously(task);
      }
      else {
        runAsynchronously(backgroundable);
      }
    }
  }

  private void runSynchronously(@Nonnull final Task task) {
    if (ApplicationManager.getApplication().isDispatchThread()) {
      runProcessWithProgressSynchronously(task, null);
    }
    else {
      ApplicationManager.getApplication().invokeAndWait(() -> runProcessWithProgressSynchronously(task, null));
    }
  }

  private void runAsynchronously(@Nonnull final Task.Backgroundable task) {
    if (ApplicationManager.getApplication().isDispatchThread()) {
      runProcessWithProgressAsynchronously(task);
    }
    else {
      ApplicationManager.getApplication().invokeLater(() -> runProcessWithProgressAsynchronously(task), ModalityState.defaultModalityState());
    }
  }

  @Nonnull
  public Future<?> runProcessWithProgressAsynchronously(@Nonnull Task.Backgroundable task) {
    return runProcessWithProgressAsynchronously(task, new EmptyProgressIndicator(), null);
  }

  @Nonnull
  public Future<?> runProcessWithProgressAsynchronously(@Nonnull final Task.Backgroundable task,
                                                        @Nonnull final ProgressIndicator progressIndicator,
                                                        @Nullable final Runnable continuation) {
    return runProcessWithProgressAsynchronously(task, progressIndicator, continuation, ModalityState.defaultModalityState());
  }

  @Nonnull
  public Future<?> runProcessWithProgressAsynchronously(@Nonnull final Task.Backgroundable task,
                                                        @Nonnull final ProgressIndicator progressIndicator,
                                                        @Nullable final Runnable continuation,
                                                        @Nonnull final ModalityState modalityState) {
    if (progressIndicator instanceof Disposable) {
      Disposer.register(ApplicationManager.getApplication(), (Disposable)progressIndicator);
    }

    final Runnable process = new TaskRunnable(task, progressIndicator, continuation);

    Runnable action = new TaskContainer(task) {
      @Override
      public void run() {
        boolean processCanceled = false;
        Throwable exception = null;
        try {
          runProcess(process, progressIndicator);
        }
        catch (ProcessCanceledException e) {
          processCanceled = true;
        }
        catch (Throwable e) {
          exception = e;
        }

        final boolean finalCanceled = processCanceled || progressIndicator.isCanceled();
        final Throwable finalException = exception;

        ApplicationManager.getApplication().invokeLater(() -> finishTask(task, finalCanceled, finalException), modalityState);
      }
    };

    return ApplicationManager.getApplication().executeOnPooledThread(action);
  }

  public boolean runProcessWithProgressSynchronously(@Nonnull final Task task, @Nullable final JComponent parentComponent) {
    final Ref<Throwable> exceptionRef = new Ref<>();
    TaskContainer taskContainer = new TaskContainer(task) {
      @Override
      public void run() {
        try {
          new TaskRunnable(task, getProgressIndicator()).run();
        }
        catch (ProcessCanceledException e) {
          throw e;
        }
        catch (Throwable e) {
          exceptionRef.set(e);
        }
      }
    };

    ApplicationEx application = (ApplicationEx)ApplicationManager.getApplication();
    boolean result = application.runProcessWithProgressSynchronously(taskContainer, task.getTitle(), task.isCancellable(),
                                                                     task.getProject(), parentComponent, task.getCancelText());

    finishTask(task, !result, exceptionRef.get());
    return result;
  }

  public void runProcessWithProgressInCurrentThread(@Nonnull final Task task,
                                                    @Nonnull final ProgressIndicator progressIndicator,
                                                    @Nonnull final ModalityState modalityState) {
    if (progressIndicator instanceof Disposable) {
      Disposer.register(ApplicationManager.getApplication(), (Disposable)progressIndicator);
    }

    final Runnable process = new TaskRunnable(task, progressIndicator);

    boolean processCanceled = false;
    Throwable exception = null;
    try {
      runProcess(process, progressIndicator);
    }
    catch (ProcessCanceledException e) {
      processCanceled = true;
    }
    catch (Throwable e) {
      exception = e;
    }

    if (ApplicationManager.getApplication().isDispatchThread()) {
      finishTask(task, processCanceled || progressIndicator.isCanceled(), exception);
    }
    else {
      final boolean finalCanceled = processCanceled;
      final Throwable finalException = exception;
      ApplicationManager.getApplication().invokeAndWait(
              () -> finishTask(task, finalCanceled || progressIndicator.isCanceled(), finalException), modalityState);
    }
  }

  static void finishTask(@Nonnull Task task, boolean canceled, @Nullable Throwable error) {
    try {
      if (error != null) {
        task.onThrowable(error);
      }
      else if (canceled) {
        task.onCancel();
      }
      else {
        task.onSuccess();
      }
    }
    finally {
      task.onFinished();
    }
  }

  @Override
  public void runProcessWithProgressAsynchronously(@Nonnull Task.Backgroundable task, @Nonnull ProgressIndicator progressIndicator) {
    runProcessWithProgressAsynchronously(task, progressIndicator, null);
  }

  @Override
  public ProgressIndicator getProgressIndicator() {
    return getCurrentIndicator(Thread.currentThread());
  }

  @Override
  public void executeProcessUnderProgress(@Nonnull Runnable process, ProgressIndicator progress) throws ProcessCanceledException {
    boolean modal = progress != null && progress.isModal();
    if (modal) myCurrentModalProgressCount.incrementAndGet();
    if (progress == null) myCurrentUnsafeProgressCount.incrementAndGet();

    try {
      ProgressIndicator oldIndicator = null;
      boolean set = progress != null && progress != (oldIndicator = getProgressIndicator());
      if (set) {
        Thread currentThread = Thread.currentThread();
        setCurrentIndicator(currentThread, progress);
        try {
          registerIndicatorAndRun(progress, currentThread, oldIndicator, process);
        }
        finally {
          setCurrentIndicator(currentThread, oldIndicator);
        }
      }
      else {
        process.run();
      }
    }
    finally {
      if (progress == null) myCurrentUnsafeProgressCount.decrementAndGet();
      if (modal) myCurrentModalProgressCount.decrementAndGet();
    }
  }

  @Override
  public boolean runInReadActionWithWriteActionPriority(@Nonnull Runnable action, @Nullable ProgressIndicator indicator) {
    ApplicationManager.getApplication().runReadAction(action);
    return true;
  }

  private void registerIndicatorAndRun(@Nonnull ProgressIndicator indicator,
                                       @Nonnull Thread currentThread,
                                       ProgressIndicator oldIndicator,
                                       @Nonnull Runnable process) {
    List<Set<Thread>> threadsUnderThisIndicator = new ArrayList<>();
    synchronized (threadsUnderIndicator) {
      for (ProgressIndicator thisIndicator = indicator; thisIndicator != null; thisIndicator = thisIndicator instanceof WrappedProgressIndicator ? ((WrappedProgressIndicator)thisIndicator).getOriginalProgressIndicator() : null) {
        Set<Thread> underIndicator = threadsUnderIndicator.get(thisIndicator);
        if (underIndicator == null) {
          underIndicator = new SmartHashSet<>();
          threadsUnderIndicator.put(thisIndicator, underIndicator);
        }
        boolean alreadyUnder = !underIndicator.add(currentThread);
        threadsUnderThisIndicator.add(alreadyUnder ? null : underIndicator);

        boolean isStandard = thisIndicator instanceof StandardProgressIndicator;
        if (!isStandard) {
          nonStandardIndicators.add(thisIndicator);
          startBackgroundNonStandardIndicatorsPing();
        }

        if (thisIndicator.isCanceled()) {
          threadsUnderCanceledIndicator.add(currentThread);
        }
        else {
          threadsUnderCanceledIndicator.remove(currentThread);
        }
      }

      updateShouldCheckCanceled();
    }

    try {
      process.run();
    }
    finally {
      synchronized (threadsUnderIndicator) {
        ProgressIndicator thisIndicator = null;
        // order doesn't matter
        for (int i = 0; i < threadsUnderThisIndicator.size(); i++) {
          thisIndicator = i == 0 ? indicator : ((WrappedProgressIndicator)thisIndicator).getOriginalProgressIndicator();
          Set<Thread> underIndicator = threadsUnderThisIndicator.get(i);
          boolean removed = underIndicator != null && underIndicator.remove(currentThread);
          if (removed && underIndicator.isEmpty()) {
            threadsUnderIndicator.remove(thisIndicator);
          }
          boolean isStandard = thisIndicator instanceof StandardProgressIndicator;
          if (!isStandard) {
            nonStandardIndicators.remove(thisIndicator);
            if (nonStandardIndicators.isEmpty()) {
              stopBackgroundNonStandardIndicatorsPing();
            }
          }
          // by this time oldIndicator may have been canceled
          if (oldIndicator != null && oldIndicator.isCanceled()) {
            threadsUnderCanceledIndicator.add(currentThread);
          }
          else {
            threadsUnderCanceledIndicator.remove(currentThread);
          }
        }
        updateShouldCheckCanceled();
      }
    }
  }

  @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
  protected final void updateShouldCheckCanceled() {
    ourCheckCanceledHook = createCheckCanceledHook();
    if (ourCheckCanceledHook != null) {
      shouldCheckCanceled = true;
      return;
    }

    synchronized (threadsUnderIndicator) {
      shouldCheckCanceled = !threadsUnderCanceledIndicator.isEmpty();
    }
  }

  @Nullable
  protected CheckCanceledHook createCheckCanceledHook() {
    return null;
  }

  @Override
  protected void indicatorCanceled(@Nonnull ProgressIndicator indicator) {
    // mark threads running under this indicator as canceled
    synchronized (threadsUnderIndicator) {
      Set<Thread> threads = threadsUnderIndicator.get(indicator);
      if (threads != null) {
        for (Thread thread : threads) {
          boolean underCancelledIndicator = false;
          for (ProgressIndicator currentIndicator = getCurrentIndicator(thread);
               currentIndicator != null;
               currentIndicator = currentIndicator instanceof WrappedProgressIndicator ?
                                  ((WrappedProgressIndicator)currentIndicator).getOriginalProgressIndicator() : null) {
            if (currentIndicator == indicator) {
              underCancelledIndicator = true;
              break;
            }
          }

          if (underCancelledIndicator) {
            threadsUnderCanceledIndicator.add(thread);
            //noinspection AssignmentToStaticFieldFromInstanceMethod
            shouldCheckCanceled = true;
          }
        }
      }
    }
  }

  @TestOnly
  public static boolean isCanceledThread(@Nonnull Thread thread) {
    synchronized (threadsUnderIndicator) {
      return threadsUnderCanceledIndicator.contains(thread);
    }
  }

  @Nonnull
  @Override
  public final NonCancelableSection startNonCancelableSection() {
    LOG.warn("Use executeNonCancelableSection() instead");
    if (isInNonCancelableSection()) return NonCancelableSection.EMPTY;
    final ProgressIndicator myOld = getProgressIndicator();

    final Thread currentThread = Thread.currentThread();
    final NonCancelableIndicator nonCancelor = new NonCancelableIndicator() {
      @Override
      public void done() {
        setCurrentIndicator(currentThread, myOld);
        isInNonCancelableSection.remove();
      }
    };
    isInNonCancelableSection.set(Boolean.TRUE);
    setCurrentIndicator(currentThread, nonCancelor);
    return nonCancelor;
  }

  @Override
  public boolean isInNonCancelableSection() {
    return isInNonCancelableSection.get() != null;
  }

  @Nonnull
  public static ModalityState getCurrentThreadProgressModality() {
    ProgressIndicator indicator = threadTopLevelIndicators.get(Thread.currentThread().getId());
    ModalityState modality = indicator == null ? null : indicator.getModalityState();
    return modality != null ? modality : ModalityState.NON_MODAL;
  }

  private static void setCurrentIndicator(@Nonnull Thread currentThread, ProgressIndicator indicator) {
    long id = currentThread.getId();
    if (indicator == null) {
      currentIndicators.remove(id);
      threadTopLevelIndicators.remove(id);
    }
    else {
      currentIndicators.put(id, indicator);
      if (!threadTopLevelIndicators.containsKey(id)) {
        threadTopLevelIndicators.put(id, indicator);
      }
    }
  }
  private static ProgressIndicator getCurrentIndicator(@Nonnull Thread thread) {
    return currentIndicators.get(thread.getId());
  }

  protected abstract static class TaskContainer implements Runnable {
    private final Task myTask;

    protected TaskContainer(@Nonnull Task task) {
      myTask = task;
    }

    @Nonnull
    public Task getTask() {
      return myTask;
    }

    @Override
    public String toString() {
      return myTask.toString();
    }
  }

  static class TaskRunnable extends TaskContainer {
    private final ProgressIndicator myIndicator;
    private final Runnable myContinuation;

    TaskRunnable(@Nonnull Task task, @Nonnull ProgressIndicator indicator) {
      this(task, indicator, null);
    }

    TaskRunnable(@Nonnull Task task, @Nonnull ProgressIndicator indicator, @Nullable Runnable continuation) {
      super(task);
      myIndicator = indicator;
      myContinuation = continuation;
    }

    @Override
    public void run() {
      try {
        getTask().run(myIndicator);
      }
      finally {
        try {
          if (myIndicator instanceof ProgressIndicatorEx) {
            ((ProgressIndicatorEx)myIndicator).finish(getTask());
          }
        }
        finally {
          if (myContinuation != null) {
            myContinuation.run();
          }
        }
      }
    }
  }

  @FunctionalInterface
  protected interface CheckCanceledHook {
    /**
     * @param indicator the indicator whose {@link ProgressIndicator#checkCanceled()} was called, or null if a non-progressive thread performed {@link ProgressManager#checkCanceled()}
     * @return true if the hook has done anything that might take some time.
     */
    boolean runHook(@Nullable ProgressIndicator indicator);
  }

}
