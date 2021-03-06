/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.codeInsight.deamon;

import com.intellij.codeInsight.daemon.impl.DaemonProgressIndicator;
import com.intellij.concurrency.JobLauncher;
import com.intellij.concurrency.JobSchedulerImpl;
import com.intellij.concurrency.SensitiveProgressWrapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.util.Processor;
import consulo.testFramework.MockApplicationTestCase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class JobUtilTest extends MockApplicationTestCase {
  public void testUnbalancedTaskJobUtilPerformance() {
    List<Integer> things = new ArrayList<Integer>(Collections.<Integer>nCopies(10000, null));
    int sum = 0;
    for (int i = 0; i < things.size(); i++) {
      int v = i < 9950 ? 1 : 1000;
      things.set(i, v);
      sum += things.get(i);
    }
    assertEquals(59950, sum);

    long start = System.currentTimeMillis();
    boolean b = JobLauncher.getInstance().invokeConcurrentlyUnderProgress(things, new ProgressIndicatorBase(), false, false, new Processor<Integer>() {
      @Override
      public boolean process(Integer o) {
        busySleep(o);
        return true;
      }
    });
    assertTrue(b);
    long elapsed = System.currentTimeMillis() - start;
    int expected = 2 * (9950 + 50 * 1000) / JobSchedulerImpl.getCPUCoresCount();
    String message = "Elapsed: " + elapsed + "; expected: " + expected;
    System.out.println(message);
    assertTrue(message, elapsed < expected);
  }
  private static final AtomicInteger COUNT = new AtomicInteger();

  private static int busySleep(int ms) {
    long end = System.currentTimeMillis() + ms;
    while (System.currentTimeMillis() < end);
    return COUNT.incrementAndGet();
  }

  public void testJobUtilFinishes() throws Exception {
    COUNT.set(0);
    int N = 100000;
    List<String> list = Collections.nCopies(N, null);
    final AtomicReference<Exception> exception = new AtomicReference<Exception>();
    final AtomicBoolean finished = new AtomicBoolean();

    JobLauncher.getInstance().invokeConcurrentlyUnderProgress(list, null, false, new Processor<String>() {
      @Override
      public boolean process(String name) {
        try {
          if (finished.get()) {
            throw new RuntimeException();
          }
          for (int i = 0; i < 1000; i++) {
            new BigDecimal(i).multiply(new BigDecimal(1));
          }
          busySleep(1);
          if (finished.get()) {
            throw new RuntimeException();
          }
        }
        catch (Exception e) {
          exception.set(e);
        }
        return true;
      }
    });
    finished.set(true);
    Thread.sleep(1000);
    if (exception.get() != null) throw exception.get();
    assertEquals(N, COUNT.get());
  }

  public void testJobUtilProcessesAllItems() throws Exception {
    List<String> list = Collections.nCopies(10000, null);
    final AtomicReference<Exception> exception = new AtomicReference<Exception>();
    for (int i=0; i<10; i++) {
      long start = System.currentTimeMillis();
      COUNT.set(0);
      JobLauncher.getInstance().invokeConcurrentlyUnderProgress(list, null, false, new Processor<String>() {
        @Override
        public boolean process(String name) {
          busySleep(1);
          return true;
        }
      });
      if (exception.get() != null) throw exception.get();
      long finish = System.currentTimeMillis();
      System.out.println("Elapsed: "+(finish-start)+"ms");
      assertEquals(list.size(), COUNT.get());
    }
  }

  public void testJobUtilRecursive() throws Exception {
    final List<String> list = Collections.nCopies(100, null);
    for (int i=0; i<10; i++) {
      COUNT.set(0);
      long start = System.currentTimeMillis();
      JobLauncher.getInstance().invokeConcurrentlyUnderProgress(list, null, false, new Processor<String>() {
        @Override
        public boolean process(String name) {
          JobLauncher.getInstance().invokeConcurrentlyUnderProgress(list, null, false, new Processor<String>() {
            @Override
            public boolean process(String name) {
              busySleep(1);
              return true;
            }
          });
          return true;
        }
      });
      long finish = System.currentTimeMillis();
      System.out.println("Elapsed: "+(finish-start)+"ms");
      assertEquals(list.size()*list.size(), COUNT.get());
    }
  }

  public void testCorrectProgressAndReadAction() throws Throwable {
    checkProgressAndReadAction(Collections.singletonList(null), new DaemonProgressIndicator(), true);
    checkProgressAndReadAction(Collections.singletonList(null), new DaemonProgressIndicator(), false);
    checkProgressAndReadAction(Collections.emptyList(), new DaemonProgressIndicator(), true);
    checkProgressAndReadAction(Collections.emptyList(), new DaemonProgressIndicator(), false);
    checkProgressAndReadAction(Arrays.asList(new Object(), new Object()), new DaemonProgressIndicator(), true);
    checkProgressAndReadAction(Arrays.asList(new Object(), new Object()), new DaemonProgressIndicator(), false);
    checkProgressAndReadAction(Arrays.asList(new Object(), new Object()), null, false);
  }

  private static void checkProgressAndReadAction(final List<Object> objects,
                                                 final DaemonProgressIndicator progress,
                                                 final boolean runInReadAction) throws Throwable {
    final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
    JobLauncher.getInstance().invokeConcurrentlyUnderProgress(objects, progress, runInReadAction, new Processor<Object>() {
      @Override
      public boolean process(Object o) {
        try {
          if (objects.size() <= 1 || JobSchedulerImpl.getCPUCoresCount() <= 2) {
            assertTrue(ApplicationManager.getApplication().isDispatchThread());
          }
          else {
            // generally we know nothing about current thread since FJP can help others task to execute while in current context
          }
          ProgressIndicator actualIndicator = ProgressManager.getInstance().getProgressIndicator();
          assertTrue(actualIndicator instanceof SensitiveProgressWrapper);
          actualIndicator = ((SensitiveProgressWrapper)actualIndicator).getOriginalProgressIndicator();
          if (progress != null) {
            assertSame(progress, actualIndicator);
          }
          else {
            assertNotNull(actualIndicator);
          }
          // there can be read access even if we didn't ask for it (e.g. when task under read action steals others work)
          assertTrue(!runInReadAction || ApplicationManager.getApplication().isReadAccessAllowed());
        }
        catch (Throwable e) {
          exception.set(e);
        }
        return true;
      }
    });
    if (exception.get() != null) throw exception.get();
  }

  public void testExceptionalCompletion() throws Throwable {
    final List<Object> objects = Collections.nCopies(100000000, null);
    COUNT.set(0);
    try {
      JobLauncher.getInstance().invokeConcurrentlyUnderProgress(objects, null, true, new Processor<Object>() {
        @Override
        public boolean process(Object o) {
          if (COUNT.incrementAndGet() == 100000) {
            System.out.println("PCE");
            throw new ProcessCanceledException();
          }
          return true;
        }
      });
      fail("PCE must have been thrown");
    }
    catch (ProcessCanceledException e) {
      // caught OK
    }
  }
  public void testNotNormalCompletion() throws Throwable {
    final List<Object> objects = Collections.nCopies(100000000, null);
    COUNT.set(0);
    boolean success = JobLauncher.getInstance().invokeConcurrentlyUnderProgress(objects, null, true, new Processor<Object>() {
      @Override
      public boolean process(Object o) {
        if (COUNT.incrementAndGet() == 100000) {
          System.out.println("PCE");
          return false;
        }
        return true;
      }
    });
    assertFalse(success);
  }

  public void testJobUtilCompletesEvenIfCannotGrabReadAction() throws Throwable {
    final List<Object> objects = Collections.nCopies(1000000, null);
    COUNT.set(0);
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        boolean success = JobLauncher.getInstance().invokeConcurrentlyUnderProgress(objects, null, true, false, new Processor<Object>() {
          @Override
          public boolean process(Object o) {
            COUNT.incrementAndGet();
            return true;
          }
        });
        assertTrue(success);
        assertEquals(objects.size(), COUNT.get());
      }
    });
  }

  public void testJobUtilRecursiveCancel() throws Exception {
    final List<String> list = Collections.nCopies(100, "");
    final List<Integer> ilist = Collections.nCopies(100, 0);
    for (int i=0; i<1/*0*/; i++) {
      COUNT.set(0);
      long start = System.currentTimeMillis();
      boolean success = false;
      try {
        success = JobLauncher.getInstance().invokeConcurrentlyUnderProgress(list, null, false, new Processor<String>() {
          @Override
          public boolean process(String name) {
            boolean nestedSuccess = JobLauncher.getInstance().invokeConcurrentlyUnderProgress(ilist, null, false, new Processor<Integer>() {
              @Override
              public boolean process(Integer integer) {
                if (busySleep(1) == 1000) {
                  System.out.println("PCE");
                  throw new RuntimeException("xxx");
                }
                return true;
              }
            });
            System.out.println("nestedSuccess = " + nestedSuccess);
            return true;
          }
        });
      }
      catch (ProcessCanceledException e) {
        // OK
      }
      catch (RuntimeException e) {
        assertEquals("xxx", e.getMessage());
      }
      long finish = System.currentTimeMillis();
      System.out.println("Elapsed: "+(finish-start)+"ms");
      //assertEquals(list.size()*list.size(), COUNT.get());
      assertFalse(success);
    }
  }
}
