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
package com.intellij.ide;

import com.intellij.ide.util.TipDialog;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.wm.ToolWindowManager;
import consulo.platform.Platform;
import consulo.ui.UIAccess;
import javax.annotation.Nonnull;

public class TipOfTheDayManager implements StartupActivity, DumbAware {
  private boolean myVeryFirstProjectOpening = true;

  @Override
  public void runActivity(@Nonnull UIAccess uiAccess, @Nonnull Project project) {
    if (!myVeryFirstProjectOpening || !GeneralSettings.getInstance().isShowTipsOnStartup()) {
      return;
    }

    myVeryFirstProjectOpening = false;

    Platform.onlyAtDesktop(() -> {
      ToolWindowManager.getInstance(project).invokeLater(() -> {
        if (project.isDisposed()) return;
        ToolWindowManager.getInstance(project).invokeLater(() -> {
          if (project.isDisposed()) return;

          new TipDialog().show();
        });
      });
    });
  }
}
