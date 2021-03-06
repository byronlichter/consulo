/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.openapi.editor;

import com.intellij.openapi.project.Project;
import javax.annotation.Nonnull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertSame;

/**
 * @author Denis Zhdanov
 * @since Aug 25, 2010 3:20:41 PM
 */
public class DefaultLineWrapPositionStrategyTest {

  private static final String EDGE_MARKER = "<EDGE>";
  private static final String WRAP_MARKER          = "<WRAP>";

  private Mockery                         myMockery;
  private DefaultLineWrapPositionStrategy myStrategy;
  private Project                         myProject;

  @Before
  public void setUp() {
    myStrategy = new DefaultLineWrapPositionStrategy();

    myMockery = new JUnit4Mockery() {{
      setImposteriser(ClassImposteriser.INSTANCE);
    }};
    myProject = myMockery.mock(Project.class);
  }

  @Test
  public void commaNotSeparated() {
    String document =
      "void method(String <WRAP>p1<EDGE>, String p2) {}";
    doTest(document, false);
  }

  @Test
  public void wrapOnExceedingWhiteSpace() {
    String document =
      "void method(String p1,<WRAP><EDGE> String p2) {}";
    doTest(document);
  }

  @Test
  public void preferWrapOnComma() {
    String document =
      "int variable = testMethod(var1 + var2, <WRAP>var3 + va<EDGE>r4);";
    doTest(document);
  }

  @Test
  public void longStringWithoutWrapPositionIsNotWrapped() {
    String document = 
      "-----------------<EDGE>---------------------------------------------------------";
    doTest(document);
  }
  
  private void doTest(final String document) {
    doTest(document, true);
  }

  private void doTest(final String document, boolean allowToBeyondMaxPreferredOffset) {
    final Context context = new Context(document);
    context.init();
    int actual = myStrategy.calculateWrapPosition(
      createMockDocument(context.document), myProject, 0, context.document.length(), context.edgeIndex,
      allowToBeyondMaxPreferredOffset, true
    );
    assertSame(context.wrapIndex, actual);
  }

  private Document createMockDocument(@Nonnull final String text) {
    final Document result = myMockery.mock(Document.class);
    myMockery.checking(new Expectations() {{
      allowing(result).getCharsSequence(); will(returnValue(text));
    }});
    return result;
  }
  
  /**
   * Utility class for parsing and initialising test data.
   * <p/>
   * <b>Note:</b> this class is line-oriented, i.e. it assumes that target document doesn't contain line feeds.
   */
  private static class Context {

    private final StringBuilder buffer = new StringBuilder();
    private final String rawDocument;

    private String document;
    private int    index;
    private int    wrapIndex = -1;
    private int    tmpWrapIndex;
    private int    edgeIndex;
    private int    tmpEdgeIndex;

    Context(String rawDocument) {
      if (rawDocument.contains("\n")) {
        throw new IllegalArgumentException(
          String.format("Don't expect to test multi-line documents but the one is detected: '%s'", rawDocument)
        );
      }
      this.rawDocument = rawDocument;
    }

    public void init() {
      tmpWrapIndex = rawDocument.indexOf(WRAP_MARKER);
      tmpEdgeIndex = rawDocument.indexOf(EDGE_MARKER);
      if (tmpWrapIndex >= 0 && tmpEdgeIndex >= 0) {
        if (tmpWrapIndex < tmpEdgeIndex) {
          processWrap();
          processMaxPreferredIndex();
        }
        else {
          processMaxPreferredIndex();
          processWrap();
        }
      }
      else {
        if (tmpWrapIndex >= 0) {
          processWrap();
        }
        if (tmpEdgeIndex >= 0) {
          processMaxPreferredIndex();
        }
      }
      
      buffer.append(rawDocument.substring(index));
      document = buffer.toString();
      if (edgeIndex <= 0) {
        edgeIndex = document.length();
      }
    }

    private void processWrap() {
      buffer.append(rawDocument.substring(index, tmpWrapIndex));
      index = tmpWrapIndex + WRAP_MARKER.length();
      wrapIndex = buffer.length();
      if (rawDocument.indexOf(WRAP_MARKER, index) >= 0) {
        throw new IllegalArgumentException(String.format("More than one wrap indicator is found at the document '%s'", rawDocument));
      }
    }

    private void processMaxPreferredIndex() {
      buffer.append(rawDocument.substring(index, tmpEdgeIndex));
      index = tmpEdgeIndex + EDGE_MARKER.length();
      edgeIndex = buffer.length();
      if (rawDocument.indexOf(EDGE_MARKER, index) >= 0) {
        throw new IllegalArgumentException(String.format("More than one max preferred offset is found at the document '%s'", rawDocument));
      }
    }
  }
}
