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
package com.intellij.execution.testframework.sm.runner;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.testframework.Printer;
import com.intellij.execution.testframework.ui.BaseTestsOutputConsoleView;
import com.intellij.execution.testframework.ui.TestsOutputConsolePrinter;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.StringTokenizer;

/**
 * @author Sergey Simonchik
 */
public final class TestProxyPrinterProvider {

  private final TestProxyFilterProvider myFilterProvider;
  private BaseTestsOutputConsoleView myTestOutputConsoleView;

  public TestProxyPrinterProvider(@Nonnull BaseTestsOutputConsoleView testsOutputConsoleView,
                                  @Nonnull TestProxyFilterProvider filterProvider) {
    myTestOutputConsoleView = testsOutputConsoleView;
    myFilterProvider = filterProvider;
  }

  @Nullable
  public Printer getPrinterByType(@Nonnull String nodeType, @Nonnull String nodeName, @Nullable String nodeArguments) {
    Filter filter = myFilterProvider.getFilter(nodeType, nodeName, nodeArguments);
    if (filter != null) {
      return new HyperlinkPrinter(myTestOutputConsoleView, HyperlinkPrinter.ERROR_CONTENT_TYPE, filter);
    }
    return null;
  }

  private static class HyperlinkPrinter extends TestsOutputConsolePrinter {

    public static final Condition<ConsoleViewContentType> ERROR_CONTENT_TYPE = new Condition<ConsoleViewContentType>() {
      @Override
      public boolean value(ConsoleViewContentType contentType) {
        return ConsoleViewContentType.ERROR_OUTPUT == contentType;
      }
    };
    private static final String NL = "\n";

    private final Condition<ConsoleViewContentType> myContentTypeCondition;
    private final Filter myFilter;

    public HyperlinkPrinter(@Nonnull BaseTestsOutputConsoleView testsOutputConsoleView,
                            @Nonnull Condition<ConsoleViewContentType> contentTypeCondition,
                            @Nonnull Filter filter) {
      super(testsOutputConsoleView, testsOutputConsoleView.getProperties(), null);
      myContentTypeCondition = contentTypeCondition;
      myFilter = filter;
    }

    @Override
    public void print(String text, ConsoleViewContentType contentType) {
      if (contentType == null || !myContentTypeCondition.value(contentType)) {
        defaultPrint(text, contentType);
        return;
      }
      text = StringUtil.replace(text, "\r\n", NL, false);
      StringTokenizer tokenizer = new StringTokenizer(text, NL, true);
      while (tokenizer.hasMoreTokens()) {
        String line = tokenizer.nextToken();
        if (NL.equals(line)) {
          defaultPrint(line, contentType);
        }
        else {
          printLine(line, contentType);
        }
      }
    }

    private void defaultPrint(String text, ConsoleViewContentType contentType) {
      super.print(text, contentType);
    }

    private void printLine(@Nonnull String line, @Nonnull ConsoleViewContentType contentType) {
      Filter.Result result = myFilter.applyFilter(line, line.length());
      if (result != null) {
        defaultPrint(line.substring(0, result.highlightStartOffset), contentType);
        String linkText = line.substring(result.highlightStartOffset, result.highlightEndOffset);
        printHyperlink(linkText, result.hyperlinkInfo);
        defaultPrint(line.substring(result.highlightEndOffset), contentType);
      }
      else {
        defaultPrint(line, contentType);
      }
    }

  }

}
