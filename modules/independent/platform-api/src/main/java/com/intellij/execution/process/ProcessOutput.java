/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.execution.process;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

/**
 * @author yole
 */
public class ProcessOutput {
  private static final int ERROR_EXIT_CODE = -1;

  private final StringBuilder myStdoutBuilder = new StringBuilder();
  private final StringBuilder myStderrBuilder = new StringBuilder();
  private int myExitCode;
  private boolean myTimeout;
  private boolean myCancelled;

  public ProcessOutput() {
    myExitCode = ERROR_EXIT_CODE; // until set explicitly, exit code denotes an error.
  }

  public ProcessOutput(final int exitCode) {
    myExitCode = exitCode;
  }

  public void appendStdout(@Nullable String text) {
    myStdoutBuilder.append(text);
  }

  public void appendStderr(@Nullable String text) {
    myStderrBuilder.append(text);
  }

  @Nonnull
  public String getStdout() {
    return myStdoutBuilder.toString();
  }

  @Nonnull
  public String getStderr() {
    return myStderrBuilder.toString();
  }

  @Nonnull
  public List<String> getStdoutLines() {
    return getStdoutLines(true);
  }

  @Nonnull
  public List<String> getStdoutLines(boolean excludeEmptyLines) {
    return splitLines(getStdout(), excludeEmptyLines);
  }

  @Nonnull
  public List<String> getStderrLines() {
    return getStderrLines(true);
  }

  @Nonnull
  public List<String> getStderrLines(boolean excludeEmptyLines) {
    return splitLines(getStderr(), excludeEmptyLines);
  }

  @Nonnull
  private static List<String> splitLines(String s, boolean excludeEmptyLines) {
    String converted = StringUtil.convertLineSeparators(s);
    return StringUtil.split(converted, "\n", true, excludeEmptyLines);
  }

  /**
   * If exit code is nonzero or the process timed out, logs stderr and exit code and returns false,
   * else just returns true.
   *
   * @param logger where to put error information
   * @return true iff exit code is zero
   */
  public boolean checkSuccess(@Nonnull final Logger logger) {
    if (getExitCode() != 0 || isTimeout()) {
      logger.info(getStderr() + (isTimeout()? "\nTimed out" : "\nExit code " + getExitCode()));
      return false;
    }
    return true;
  }

  public void setExitCode(int exitCode) {
    myExitCode = exitCode;
  }

  public int getExitCode() {
    return myExitCode;
  }

  /**
   * @return true if exit code wasn't set and is still set to default value (this might happen,
   * for example, when our CapturingProcessHandler.runProcess() is interrupted)
   */
  public boolean hasErrorExitCode() {
    return getExitCode() == ERROR_EXIT_CODE;
  }

  public void setTimeout() {
    myTimeout = true;
  }

  public boolean isTimeout() {
    return myTimeout;
  }

  public void setCancelled() {
    myCancelled = true;
  }

  public boolean isCancelled() {
    return myCancelled;
  }
}
