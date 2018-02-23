/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.test;

import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import javax.annotation.Nonnull;

/**
 * @author Denis Zhdanov
 * @since 8/8/13 5:12 PM
 */
public class TestExternalProjectSettings extends ExternalProjectSettings {

  @Nonnull
  @Override
  public ExternalProjectSettings clone() {
    throw new UnsupportedOperationException();
  }
}
