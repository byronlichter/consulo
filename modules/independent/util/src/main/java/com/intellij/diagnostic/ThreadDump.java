/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.diagnostic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents thread dump of the IDE captured by its performance diagnostic tool.
 */
public class ThreadDump {
  private final String myRawDump;
  private final StackTraceElement[] myEdtStack;

  ThreadDump(@Nonnull String rawDump, @Nullable StackTraceElement[] edtStack) {
    myRawDump = rawDump;
    myEdtStack = edtStack;
  }

  /**
   * @return full thread dump as a string
   */
  @Nonnull
  public String getRawDump() {
    return myRawDump;
  }

  /**
   * @return state of the AWT thread from the dump
   */
  @Nullable
  public StackTraceElement[] getEDTStackTrace() {
    return myEdtStack;
  }

}
