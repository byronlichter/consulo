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
package com.intellij.openapi.util;

import javax.annotation.Nonnull;

/**
 * Lazy value with ability to reset (and recompute) the value.
 * Thread-safe version: {@link AtomicClearableLazyValue}.
 */
public abstract class ClearableLazyValue<T> {
  private T myValue;

  @Nonnull
  protected abstract T compute();

  @Nonnull
  public T getValue() {
    if (myValue == null) {
      myValue = compute();
    }
    return myValue;
  }

  public void drop() {
    myValue = null;
  }
}
