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
package com.intellij.psi.search.scope.packageSet;

import javax.annotation.Nullable;
import javax.annotation.Nonnull;

public class NamedScope {
  private final String myName;
  private final PackageSet myValue;

  public NamedScope(@Nonnull String name, @Nullable PackageSet value) {
    myName = name;
    myValue = value;
  }

  @Nonnull
  public String getName() {
    return myName;
  }

  @javax.annotation.Nullable
  public PackageSet getValue() {
    return myValue;
  }

  public NamedScope createCopy() {
    return new NamedScope(myName, myValue != null ? myValue.createCopy() : null);
  }

  public static class UnnamedScope extends NamedScope {
    public UnnamedScope(@Nonnull PackageSet value) {
      super(value.getText(), value);
    }
  }

  @Override
  public String toString() {
    return "Scope '" + myName + "'; set:" + (myValue == null ? null : myValue.getText());
  }
}