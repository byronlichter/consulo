/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.testFramework;

import com.intellij.concurrency.JobSchedulerImpl;
import com.intellij.ide.DataManager;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.idea.Bombed;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.ui.Queryable;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.util.Alarm;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.containers.HashMap;
import com.intellij.util.io.ZipUtil;
import com.intellij.util.ui.UIUtil;
import junit.framework.AssertionFailedError;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.junit.Assert;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.InvocationEvent;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author yole
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class PlatformTestUtil {
  public static final boolean COVERAGE_ENABLED_BUILD = "true".equals(System.getProperty("idea.coverage.enabled.build"));

  @Nonnull
  public static String lowercaseFirstLetter(@Nonnull String name, boolean lowercaseFirstLetter) {
    if (lowercaseFirstLetter && !UsefulTestCase.isAllUppercaseName(name)) {
      name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
    return name;
  }

  @Nullable
  protected static String toString(@javax.annotation.Nullable Object node, @javax.annotation.Nullable Queryable.PrintInfo printInfo) {
    if (node instanceof AbstractTreeNode) {
      if (printInfo != null) {
        return ((AbstractTreeNode)node).toTestString(printInfo);
      }
      else {
        @SuppressWarnings({"deprecation", "UnnecessaryLocalVariable"})
        final String presentation = ((AbstractTreeNode)node).getTestPresentation();
        return presentation;
      }
    }
    if (node == null) {
      return "NULL";
    }
    return node.toString();
  }

  public static String print(JTree tree, boolean withSelection) {
    return print(tree, withSelection, null);
  }

  public static String print(JTree tree, boolean withSelection, @javax.annotation.Nullable Condition<String> nodePrintCondition) {
    StringBuilder buffer = new StringBuilder();
    final Collection<String> strings = printAsList(tree, withSelection, nodePrintCondition);
    for (String string : strings) {
      buffer.append(string).append("\n");
    }
    return buffer.toString();
  }

  public static Collection<String> printAsList(JTree tree, boolean withSelection, @javax.annotation.Nullable Condition<String> nodePrintCondition) {
    Collection<String> strings = new ArrayList<String>();
    Object root = tree.getModel().getRoot();
    printImpl(tree, root, strings, 0, withSelection, nodePrintCondition);
    return strings;
  }

  private static void printImpl(JTree tree,
                                Object root,
                                Collection<String> strings,
                                int level,
                                boolean withSelection,
                                @javax.annotation.Nullable Condition<String> nodePrintCondition) {
    DefaultMutableTreeNode defaultMutableTreeNode = (DefaultMutableTreeNode)root;

    final Object userObject = defaultMutableTreeNode.getUserObject();
    String nodeText;
    if (userObject != null) {
      nodeText = toString(userObject, null);
    }
    else {
      nodeText = "null";
    }

    if (nodePrintCondition != null && !nodePrintCondition.value(nodeText)) return;

    final StringBuilder buff = new StringBuilder();
    StringUtil.repeatSymbol(buff, ' ', level);

    final boolean expanded = tree.isExpanded(new TreePath(defaultMutableTreeNode.getPath()));
    if (!defaultMutableTreeNode.isLeaf()) {
      buff.append(expanded ? "-" : "+");
    }

    final boolean selected = tree.getSelectionModel().isPathSelected(new TreePath(defaultMutableTreeNode.getPath()));
    if (withSelection && selected) {
      buff.append("[");
    }

    buff.append(nodeText);

    if (withSelection && selected) {
      buff.append("]");
    }

    strings.add(buff.toString());

    int childCount = tree.getModel().getChildCount(root);
    if (expanded) {
      for (int i = 0; i < childCount; i++) {
        printImpl(tree, tree.getModel().getChild(root, i), strings, level + 1, withSelection, nodePrintCondition);
      }
    }
  }

  public static void assertTreeEqual(JTree tree, @NonNls String expected) {
    assertTreeEqual(tree, expected, false);
  }

  public static void assertTreeEqualIgnoringNodesOrder(JTree tree, @NonNls String expected) {
    assertTreeEqualIgnoringNodesOrder(tree, expected, false);
  }

  public static void assertTreeEqual(JTree tree, String expected, boolean checkSelected) {
    String treeStringPresentation = print(tree, checkSelected);
    assertEquals(expected, treeStringPresentation);
  }

  public static void assertTreeEqualIgnoringNodesOrder(JTree tree, String expected, boolean checkSelected) {
    final Collection<String> actualNodesPresentation = printAsList(tree, checkSelected, null);
    final List<String> expectedNodes = StringUtil.split(expected, "\n");
    UsefulTestCase.assertSameElements(actualNodesPresentation, expectedNodes);
  }

  @TestOnly
  public static void waitForAlarm(final int delay) throws InterruptedException {
    assert !ApplicationManager.getApplication().isWriteAccessAllowed(): "It's a bad idea to wait for an alarm under the write action. Somebody creates an alarm which requires read action and you are deadlocked.";
    assert ApplicationManager.getApplication().isDispatchThread();

    final AtomicBoolean invoked = new AtomicBoolean();
    final Alarm alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
    alarm.addRequest(new Runnable() {
      @Override
      public void run() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            alarm.addRequest(new Runnable() {
              @Override
              public void run() {
                invoked.set(true);
              }
            }, delay);
          }
        });
      }
    }, delay);

    UIUtil.dispatchAllInvocationEvents();

    boolean sleptAlready = false;
    while (!invoked.get()) {
      UIUtil.dispatchAllInvocationEvents();
      //noinspection BusyWait
      Thread.sleep(sleptAlready ? 10 : delay);
      sleptAlready = true;
    }
    UIUtil.dispatchAllInvocationEvents();
  }

  @TestOnly
  public static void dispatchAllInvocationEventsInIdeEventQueue() throws InterruptedException {
    assert SwingUtilities.isEventDispatchThread() : Thread.currentThread();
    final EventQueue eventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
    while (true) {
      AWTEvent event = eventQueue.peekEvent();
      if (event == null) break;
        AWTEvent event1 = eventQueue.getNextEvent();
        if (event1 instanceof InvocationEvent) {
          IdeEventQueue.getInstance().dispatchEvent(event1);
        }
    }
  }

  private static Date raidDate(Bombed bombed) {
    final Calendar instance = Calendar.getInstance();
    instance.set(Calendar.YEAR, bombed.year());
    instance.set(Calendar.MONTH, bombed.month());
    instance.set(Calendar.DAY_OF_MONTH, bombed.day());
    instance.set(Calendar.HOUR_OF_DAY, bombed.time());
    instance.set(Calendar.MINUTE, 0);

    return instance.getTime();
  }

  public static boolean bombExplodes(Bombed bombedAnnotation) {
    Date now = new Date();
    return now.after(raidDate(bombedAnnotation));
  }

  public static boolean isRotten(Bombed bomb) {
    long bombRotPeriod = 30L * 24 * 60 * 60 * 1000; // month
    return new Date().after(new Date(raidDate(bomb).getTime() + bombRotPeriod));
  }

  public static StringBuilder print(AbstractTreeStructure structure,
                                    Object node,
                                    int currentLevel,
                                    @Nullable Comparator comparator,
                                    int maxRowCount,
                                    char paddingChar,
                                    @javax.annotation.Nullable Queryable.PrintInfo printInfo) {
    StringBuilder buffer = new StringBuilder();
    doPrint(buffer, currentLevel, node, structure, comparator, maxRowCount, 0, paddingChar, printInfo);
    return buffer;
  }

  private static int doPrint(StringBuilder buffer,
                             int currentLevel,
                             Object node,
                             AbstractTreeStructure structure,
                             @javax.annotation.Nullable Comparator comparator,
                             int maxRowCount,
                             int currentLine,
                             char paddingChar,
                             @javax.annotation.Nullable Queryable.PrintInfo printInfo) {
    if (currentLine >= maxRowCount && maxRowCount != -1) return currentLine;

    StringUtil.repeatSymbol(buffer, paddingChar, currentLevel);
    buffer.append(toString(node, printInfo)).append("\n");
    currentLine++;
    Object[] children = structure.getChildElements(node);

    if (comparator != null) {
      ArrayList<?> list = new ArrayList<Object>(Arrays.asList(children));
      @SuppressWarnings({"UnnecessaryLocalVariable", "unchecked"}) Comparator<Object> c = comparator;
      Collections.sort(list, c);
      children = ArrayUtil.toObjectArray(list);
    }
    for (Object child : children) {
      currentLine = doPrint(buffer, currentLevel + 1, child, structure, comparator, maxRowCount, currentLine, paddingChar, printInfo);
    }

    return currentLine;
  }

  public static String print(Object[] objects) {
    return print(Arrays.asList(objects));
  }

  public static String print(Collection c) {
    StringBuilder result = new StringBuilder();
    for (Iterator iterator = c.iterator(); iterator.hasNext();) {
      Object each = iterator.next();
      result.append(toString(each, null));
      if (iterator.hasNext()) {
        result.append("\n");
      }
    }

    return result.toString();
  }

  public static String print(ListModel model) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < model.getSize(); i++) {
      result.append(toString(model.getElementAt(i), null));
      result.append("\n");
    }
    return result.toString();
  }

  public static String print(JTree tree) {
    return print(tree, false);
  }

  public static void assertTreeStructureEquals(final AbstractTreeStructure treeStructure, final String expected) {
    assertEquals(expected, print(treeStructure, treeStructure.getRootElement(), 0, null, -1, ' ', null).toString());
  }

  public static void invokeNamedAction(final String actionId) {
    final AnAction action = ActionManager.getInstance().getAction(actionId);
    assertNotNull(action);
    final Presentation presentation = new Presentation();
    @SuppressWarnings("deprecation") final DataContext context = DataManager.getInstance().getDataContext();
    final AnActionEvent event = new AnActionEvent(null, context, "", presentation, ActionManager.getInstance(), 0);
    action.update(event);
    Assert.assertTrue(presentation.isEnabled());
    action.actionPerformed(event);
  }

  public static void assertTiming(final String message, final long expectedMs, final long actual) {
    if (COVERAGE_ENABLED_BUILD) return;

    final long expectedOnMyMachine = Math.max(1, expectedMs * Timings.MACHINE_TIMING / Timings.ETALON_TIMING);

    // Allow 10% more in case of test machine is busy.
    String logMessage = message;
    if (actual > expectedOnMyMachine) {
      int percentage = (int)(100.0 * (actual - expectedOnMyMachine) / expectedOnMyMachine);
      logMessage += ". Operation took " + percentage + "% longer than expected";
    }
    logMessage += ". Expected on my machine: " + expectedOnMyMachine + "." +
                  " Actual: " + actual + "." +
                  " Expected on Standard machine: " + expectedMs + ";" +
                  " Actual on Standard: " + actual * Timings.ETALON_TIMING / Timings.MACHINE_TIMING + ";" +
                  " Timings: CPU=" + Timings.CPU_TIMING +
                  ", I/O=" + Timings.IO_TIMING + "." +
                  " (" + (int)(Timings.MACHINE_TIMING*1.0/Timings.ETALON_TIMING*100) + "% of the Standard)" +
                  ".";
    final double acceptableChangeFactor = 1.1;
    if (actual < expectedOnMyMachine) {
      System.out.println(logMessage);
      TeamCityLogger.info(logMessage);
    }
    else if (actual < expectedOnMyMachine * acceptableChangeFactor) {
      TeamCityLogger.warning(logMessage, null);
    }
    else {
      // throw AssertionFailedError to try one more time
      throw new AssertionFailedError(logMessage);
    }
  }

  /**
   * example usage: startPerformanceTest("calculating pi",100, testRunnable).cpuBound().assertTiming();
   */
  public static TestInfo startPerformanceTest(@NonNls @Nonnull String message, int expectedMs, @Nonnull ThrowableRunnable test) {
    return new TestInfo(test, expectedMs,message);
  }

  // calculates average of the median values in the selected part of the array. E.g. for part=3 returns average in the middle third.
  public static long averageAmongMedians(@Nonnull long[] time, int part) {
    assert part >= 1;
    int n = time.length;
    Arrays.sort(time);
    long total = 0;
    for (int i= n /2- n / part /2; i< n /2+ n / part /2; i++) {
      total += time[i];
    }
    return total/(n / part);
  }

  public static boolean canRunTest(@Nonnull Class testCaseClass) {
    if (GraphicsEnvironment.isHeadless()) {
      for (Class<?> clazz = testCaseClass; clazz != null; clazz = clazz.getSuperclass()) {
        if (clazz.getAnnotation(SkipInHeadlessEnvironment.class) != null) {
          System.out.println("Class '" + testCaseClass.getName() + "' is skipped because it requires working UI environment");
          return false;
        }
      }
    }
    return true;
  }

  public static void assertPathsEqual(@javax.annotation.Nullable String expected, @javax.annotation.Nullable String actual) {
    if (expected != null) expected = FileUtil.toSystemIndependentName(expected);
    if (actual != null) actual = FileUtil.toSystemIndependentName(actual);
    assertEquals(expected, actual);
  }

  public static class TestInfo {
    private final ThrowableRunnable test; // runnable to measure
    private final int expectedMs;           // millis the test is expected to run
    private ThrowableRunnable setup;      // to run before each test
    private boolean usesAllCPUCores;      // true if the test runs faster on multi-core
    private int attempts = 4;             // number of retries if performance failed
    private final String message;         // to print on fail
    private boolean adjustForIO = true;   // true if test uses IO, timings need to be re-calibrated according to this agent disk performance
    private boolean adjustForCPU = true;  // true if test uses CPU, timings need to be re-calibrated according to this agent CPU speed

    private TestInfo(@Nonnull ThrowableRunnable test, int expectedMs, String message) {
      this.test = test;
      this.expectedMs = expectedMs;
      assert expectedMs > 0 : "Expected must be > 0. Was: "+ expectedMs;
      this.message = message;
    }

    public TestInfo setup(@Nonnull ThrowableRunnable setup) { assert this.setup == null;this.setup = setup; return this; }
    public TestInfo usesAllCPUCores() { assert adjustForCPU : "This test configured to be io-bound, it cannot use all cores"; usesAllCPUCores = true; return this; }
    public TestInfo cpuBound() { adjustForIO = false; adjustForCPU = true; return this; }
    public TestInfo ioBound() { adjustForIO = true; adjustForCPU = false; return this; }
    public TestInfo attempts(int attempts) { this.attempts = attempts; return this; }

    public void assertTiming() {
      assert expectedMs != 0 : "Must call .expect() before run test";
      if (COVERAGE_ENABLED_BUILD) return;

      while (true) {
        attempts--;
        long start;
        try {
          if (setup != null) setup.run();
          start = System.currentTimeMillis();
          test.run();
        }
        catch (Throwable throwable) {
          throw new RuntimeException(throwable);
        }
        long finish = System.currentTimeMillis();
        long duration = finish - start;

        int expectedOnMyMachine = expectedMs;
        if (adjustForCPU) {
          expectedOnMyMachine = adjust(expectedOnMyMachine, Timings.CPU_TIMING, Timings.ETALON_CPU_TIMING);

          expectedOnMyMachine = usesAllCPUCores ? expectedOnMyMachine * 8 / JobSchedulerImpl.CORES_COUNT : expectedOnMyMachine;
        }
        if (adjustForIO) {
          expectedOnMyMachine = adjust(expectedOnMyMachine, Timings.IO_TIMING, Timings.ETALON_IO_TIMING);
        }

        // Allow 10% more in case of test machine is busy.
        String logMessage = message;
        if (duration > expectedOnMyMachine) {
          int percentage = (int)(100.0 * (duration - expectedOnMyMachine) / expectedOnMyMachine);
          logMessage += ": " + percentage + "% longer";
        }
        logMessage +=
          ". Expected: " + formatTime(expectedOnMyMachine) + ". Actual: " + formatTime(duration) + "." + Timings.getStatistics();
        final double acceptableChangeFactor = 1.1;
        if (duration < expectedOnMyMachine) {
          int percentage = (int)(100.0 * (expectedOnMyMachine - duration) / expectedOnMyMachine);
          logMessage = percentage + "% faster. " + logMessage;

          TeamCityLogger.info(logMessage);
          System.out.println("SUCCESS: " + logMessage);
        }
        else if (duration < expectedOnMyMachine * acceptableChangeFactor) {
          TeamCityLogger.warning(logMessage, null);
          System.out.println("WARNING: " + logMessage);
        }
        else {
          // try one more time
          if (attempts == 0) {
            //try {
            //  Object result = Class.forName("com.intellij.util.ProfilingUtil").getMethod("captureCPUSnapshot").invoke(null);
            //  System.err.println("CPU snapshot captured in '"+result+"'");
            //}
            //catch (Exception e) {
            //}

            throw new AssertionFailedError(logMessage);
          }
          System.gc();
          System.gc();
          System.gc();
          String s = "Another epic fail (remaining attempts: " + attempts + "): " + logMessage;
          TeamCityLogger.warning(s, null);
          System.err.println(s);
          //if (attempts == 1) {
          //  try {
          //    Class.forName("com.intellij.util.ProfilingUtil").getMethod("startCPUProfiling").invoke(null);
          //  }
          //  catch (Exception e) {
          //  }
          //}
          continue;
        }
        break;
      }
    }

    private static String formatTime(long millis) {
      String hint = "";
      DecimalFormat format = new DecimalFormat("#.0", DecimalFormatSymbols.getInstance(Locale.US));
      if (millis >= 60 * 1000) hint = format.format(millis / 60 / 1000.f) + "m";
      if (millis >= 1000) hint += (hint.isEmpty() ? "" : " ") + format.format(millis / 1000.f) + "s";
      String result = millis + "ms";
      if (!hint.isEmpty()) {
        result = result + " (" + hint + ")";
      }
      return result;
    }

    private static int adjust(int expectedOnMyMachine, long thisTiming, long ethanolTiming) {
      // most of our algorithms are quadratic. sad but true.
      double speed = 1.0 * thisTiming / ethanolTiming;
      double delta = speed < 1
                 ? 0.9 + Math.pow(speed - 0.7, 2)
                 : 0.45 + Math.pow(speed - 0.25, 2);
      expectedOnMyMachine *= delta;
      return expectedOnMyMachine;
    }
  }


  public static void assertTiming(String message, long expected, @Nonnull Runnable actionToMeasure) {
    assertTiming(message, expected, 4, actionToMeasure);
  }

  public static long measure(@Nonnull Runnable actionToMeasure) {
    long start = System.currentTimeMillis();
    actionToMeasure.run();
    long finish = System.currentTimeMillis();
    return finish - start;
  }

  public static void assertTiming(String message, long expected, int attempts, @Nonnull Runnable actionToMeasure) {
    while (true) {
      attempts--;
      long duration = measure(actionToMeasure);
      try {
        assertTiming(message, expected, duration);
        break;
      }
      catch (AssertionFailedError e) {
        if (attempts == 0) throw e;
        System.gc();
        System.gc();
        System.gc();
        String s = "Another epic fail (remaining attempts: " + attempts + "): " + e.getMessage();
        TeamCityLogger.warning(s, null);
        System.err.println(s);
      }
    }
  }

  private static HashMap<String, VirtualFile> buildNameToFileMap(VirtualFile[] files, @javax.annotation.Nullable VirtualFileFilter filter) {
    HashMap<String, VirtualFile> map = new HashMap<String, VirtualFile>();
    for (VirtualFile file : files) {
      if (filter != null && !filter.accept(file)) continue;
      map.put(file.getName(), file);
    }
    return map;
  }

  public static void assertDirectoriesEqual(VirtualFile dirAfter, VirtualFile dirBefore) throws IOException {
    assertDirectoriesEqual(dirAfter, dirBefore, null);
  }

  @SuppressWarnings("UnsafeVfsRecursion")
  public static void assertDirectoriesEqual(VirtualFile dirAfter, VirtualFile dirBefore, @Nullable VirtualFileFilter fileFilter) throws IOException {
    FileDocumentManager.getInstance().saveAllDocuments();

    VirtualFile[] childrenAfter = dirAfter.getChildren();
    if (dirAfter.isInLocalFileSystem()) {
      File[] ioAfter = new File(dirAfter.getPath()).listFiles();
      shallowCompare(childrenAfter, ioAfter);
    }

    VirtualFile[] childrenBefore = dirBefore.getChildren();
    if (dirBefore.isInLocalFileSystem()) {
      File[] ioBefore = new File(dirBefore.getPath()).listFiles();
      shallowCompare(childrenBefore, ioBefore);
    }

    HashMap<String, VirtualFile> mapAfter = buildNameToFileMap(childrenAfter, fileFilter);
    HashMap<String, VirtualFile> mapBefore = buildNameToFileMap(childrenBefore, fileFilter);

    Set<String> keySetAfter = mapAfter.keySet();
    Set<String> keySetBefore = mapBefore.keySet();
    assertEquals(dirAfter.getPath(), keySetAfter, keySetBefore);

    for (String name : keySetAfter) {
      VirtualFile fileAfter = mapAfter.get(name);
      VirtualFile fileBefore = mapBefore.get(name);
      if (fileAfter.isDirectory()) {
        assertDirectoriesEqual(fileAfter, fileBefore, fileFilter);
      }
      else {
        assertFilesEqual(fileAfter, fileBefore);
      }
    }
  }

  private static void shallowCompare(VirtualFile[] vfs, @javax.annotation.Nullable File[] io) {
    List<String> vfsPaths = new ArrayList<String>();
    for (VirtualFile file : vfs) {
      vfsPaths.add(file.getPath());
    }

    List<String> ioPaths = new ArrayList<String>();
    if (io != null) {
      for (File file : io) {
        ioPaths.add(file.getPath().replace(File.separatorChar, '/'));
      }
    }

    assertEquals(sortAndJoin(vfsPaths), sortAndJoin(ioPaths));
  }

  private static String sortAndJoin(List<String> strings) {
    Collections.sort(strings);
    StringBuilder buf = new StringBuilder();
    for (String string : strings) {
      buf.append(string);
      buf.append('\n');
    }
    return buf.toString();
  }

  public static void assertFilesEqual(VirtualFile fileAfter, VirtualFile fileBefore) throws IOException {
    try {
      assertJarFilesEqual(VfsUtilCore.virtualToIoFile(fileAfter), VfsUtilCore.virtualToIoFile(fileBefore));
    }
    catch (IOException e) {
      FileDocumentManager manager = FileDocumentManager.getInstance();

      Document docBefore = manager.getDocument(fileBefore);
      boolean canLoadBeforeText = !fileBefore.getFileType().isBinary() || fileBefore.getFileType() == UnknownFileType.INSTANCE;
      String textB = docBefore != null
                     ? docBefore.getText()
                     : !canLoadBeforeText
                       ? null
                       : LoadTextUtil.getTextByBinaryPresentation(fileBefore.contentsToByteArray(false), fileBefore).toString();

      Document docAfter = manager.getDocument(fileAfter);
      boolean canLoadAfterText = !fileBefore.getFileType().isBinary() || fileBefore.getFileType() == UnknownFileType.INSTANCE;
      String textA = docAfter != null
                     ? docAfter.getText()
                     : !canLoadAfterText
                       ? null
                       : LoadTextUtil.getTextByBinaryPresentation(fileAfter.contentsToByteArray(false), fileAfter).toString();

      if (textA != null && textB != null) {
        assertEquals(fileAfter.getPath(), textA, textB);
      }
      else {
        Assert.assertArrayEquals(fileAfter.getPath(), fileAfter.contentsToByteArray(), fileBefore.contentsToByteArray());
      }
    }
  }

  public static void assertJarFilesEqual(File file1, File file2) throws IOException {
    final File tempDirectory1;
    final File tempDirectory2;

    final JarFile jarFile1 = new JarFile(file1);
    try {
      final JarFile jarFile2 = new JarFile(file2);
      try {
        tempDirectory1 = PlatformTestCase.createTempDir("tmp1");
        tempDirectory2 = PlatformTestCase.createTempDir("tmp2");
        ZipUtil.extract(jarFile1, tempDirectory1, null);
        ZipUtil.extract(jarFile2, tempDirectory2, null);
      }
      finally {
        jarFile2.close();
      }
    }
    finally {
      jarFile1.close();
    }

    final VirtualFile dirAfter = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDirectory1);
    assertNotNull(tempDirectory1.toString(), dirAfter);
    final VirtualFile dirBefore = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDirectory2);
    assertNotNull(tempDirectory2.toString(), dirBefore);
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        dirAfter.refresh(false, true);
        dirBefore.refresh(false, true);
      }
    });
    assertDirectoriesEqual(dirAfter, dirBefore);
  }

  public static void assertElementsEqual(final Element expected, final Element actual) throws IOException {
    if (!JDOMUtil.areElementsEqual(expected, actual)) {
      junit.framework.Assert.assertEquals(printElement(expected), printElement(actual));
    }
  }

  public static String printElement(final Element element) throws IOException {
    final StringWriter writer = new StringWriter();
    JDOMUtil.writeElement(element, writer, "\n");
    return writer.getBuffer().toString();
  }

  @Deprecated
  public static String getCommunityPath() {
    return PathManager.getHomePath();
  }

  public static Comparator<AbstractTreeNode> createComparator(final Queryable.PrintInfo printInfo) {
    return new Comparator<AbstractTreeNode>() {
      @Override
      public int compare(final AbstractTreeNode o1, final AbstractTreeNode o2) {
        String displayText1 = o1.toTestString(printInfo);
        String displayText2 = o2.toTestString(printInfo);
        return Comparing.compare(displayText1, displayText2);
      }
    };
  }

  @Nonnull
  public static <T> T notNull(@Nullable T t) {
    assertNotNull(t);
    return t;
  }

  @Nonnull
  public static String loadFileText(@Nonnull String fileName) throws IOException {
    return StringUtil.convertLineSeparators(FileUtil.loadFile(new File(fileName)));
  }
}
