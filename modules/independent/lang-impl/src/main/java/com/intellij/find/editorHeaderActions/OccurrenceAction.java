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
package com.intellij.find.editorHeaderActions;

import com.intellij.find.EditorSearchSession;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ShortcutProvider;
import com.intellij.openapi.actionSystem.ShortcutSet;
import com.intellij.openapi.project.DumbAwareAction;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;

public abstract class OccurrenceAction extends DumbAwareAction implements ShortcutProvider {
  protected OccurrenceAction(@Nonnull String baseActionId, @Nonnull Icon icon) {
    copyFrom(ActionManager.getInstance().getAction(baseActionId));
    getTemplatePresentation().setIcon(icon);
  }

  @Override
  public void update(AnActionEvent e) {
    EditorSearchSession search = e.getData(EditorSearchSession.SESSION_KEY);
    if (search == null) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    boolean isFind = !search.getFindModel().isReplaceState();
    boolean hasMatches = search.hasMatches();
    e.getPresentation().setVisible(isFind);
    e.getPresentation().setEnabled(isFind && hasMatches);
  }

  @Nullable
  @Override
  public ShortcutSet getShortcut() {
    return getShortcutSet();
  }
}
