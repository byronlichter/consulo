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
package com.intellij.diff;

import com.intellij.diff.requests.DiffRequest;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Key;
import consulo.annotations.RequiredDispatchThread;
import javax.annotation.Nonnull;

import javax.swing.*;

public interface DiffRequestPanel extends Disposable {
  void setRequest(@javax.annotation.Nullable DiffRequest request);

  /*
   * Sets request to show.
   * Will not override current request, if their keys are not null and equal.
   */
  void setRequest(@javax.annotation.Nullable DiffRequest request, @javax.annotation.Nullable Object identity);

  @Nonnull
  JComponent getComponent();

  @javax.annotation.Nullable
  JComponent getPreferredFocusedComponent();

  @RequiredDispatchThread
  <T> void putContextHints(@Nonnull Key<T> key, @javax.annotation.Nullable T value);
}
