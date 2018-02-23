/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.openapi.fileTypes;

import com.intellij.openapi.util.text.StringUtilRt;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

/**
 * @author max
 */
public class ExtensionFileNameMatcher implements FileNameMatcher {
  private final String myExtension;
  private final String myDotExtension;

  public ExtensionFileNameMatcher(@Nonnull @NonNls String extension) {
    myExtension = extension.toLowerCase();
    myDotExtension = "." + myExtension;
  }

  @Override
  public boolean accept(@Nonnull @NonNls CharSequence fileName) {
    return StringUtilRt.endsWithIgnoreCase(fileName, myDotExtension);
  }

  @Override
  @NonNls
  @Nonnull
  public String getPresentableString() {
    return "*." + myExtension;
  }

  public String getExtension() {
    return myExtension;
  }


  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final ExtensionFileNameMatcher that = (ExtensionFileNameMatcher)o;

    if (!myExtension.equals(that.myExtension)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myExtension.hashCode();
  }

  @Override
  public String toString() {
    return getPresentableString();
  }
}
