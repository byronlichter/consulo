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

package com.intellij.application.options;

import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;

public class CodeCompletionOptions implements SearchableConfigurable, Configurable.NoScroll {
  private CodeCompletionPanel myPanel;

  @Override
  public boolean isModified() {
    return myPanel != null && myPanel.isModified();
  }

  @Override
  public JComponent createComponent() {
    myPanel = new CodeCompletionPanel();
    return myPanel.myPanel;
  }

  @Override
  public String getDisplayName() {
    return ApplicationBundle.message("title.code.completion");
  }

  @Override
  public void reset() {
    myPanel.reset();
  }

  @Override
  public void apply() {
    myPanel.apply();
  }

  @Override
  public void disposeUIResources() {
    myPanel = null;
  }

  @Override
  public String getHelpTopic() {
    return "reference.settingsdialog.IDE.editor.code.completion";
  }

  @Override
  @Nonnull
  public String getId() {
    return "editor.preferences.completion";
  }

  @Override
  @Nullable
  public Runnable enableSearch(String option) {
    return null;
  }
}
