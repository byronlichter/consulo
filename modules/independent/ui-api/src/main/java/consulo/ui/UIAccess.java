/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public interface UIAccess {
  /**
   * @return if current thread can access to ui write mode
   */
  static boolean isUIThread() {
    return UIInternal.get()._UIAccess_isUIThread();
  }

  /**
   * If we inside ui thread, we can get ui access
   *
   * @return ui access - or throw exception
   */
  @RequiredUIAccess
  @Nonnull
  static UIAccess get() {
    assertIsUIThread();

    return UIInternal.get()._UIAccess_get();
  }

  @RequiredUIAccess
  static void assertIsUIThread() {
    if (!isUIThread()) {
      throw new IllegalArgumentException("Call must be wrapped inside UI thread");
    }
  }

  boolean isValid();

  void give(@RequiredUIAccess @Nonnull Runnable runnable);

  void giveAndWait(@RequiredUIAccess @Nonnull Runnable runnable);
}
