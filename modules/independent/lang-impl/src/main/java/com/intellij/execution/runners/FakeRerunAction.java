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
package com.intellij.execution.runners;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.impl.ExecutionManagerImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import consulo.annotations.RequiredDispatchThread;

import javax.swing.*;

class FakeRerunAction extends AnAction implements DumbAware {
  @RequiredDispatchThread
  @Override
  public void update(@Nonnull AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    ExecutionEnvironment environment = getEnvironment(event);
    if (environment != null) {
      presentation.setText(ExecutionBundle.message("rerun.configuration.action.name", environment.getRunProfile().getName()));
      presentation.setIcon(ExecutionManagerImpl.isProcessRunning(getDescriptor(event)) ? AllIcons.Actions.Restart : environment.getExecutor().getIcon());
      presentation.setEnabled(isEnabled(event));
      return;
    }

    presentation.setEnabled(false);
  }

  @RequiredDispatchThread
  @Override
  public void actionPerformed(@Nonnull AnActionEvent event) {
    ExecutionEnvironment environment = getEnvironment(event);
    if (environment != null) {
      ExecutionUtil.restart(environment);
    }
  }

  @Nullable
  protected RunContentDescriptor getDescriptor(AnActionEvent event) {
    return event.getData(LangDataKeys.RUN_CONTENT_DESCRIPTOR);
  }

  @Nullable
  protected ExecutionEnvironment getEnvironment(@Nonnull AnActionEvent event) {
    ExecutionEnvironment environment = event.getData(LangDataKeys.EXECUTION_ENVIRONMENT);
    if (environment == null) {
      Project project = event.getProject();
      RunContentDescriptor contentDescriptor = project == null ? null : ExecutionManager.getInstance(project).getContentManager().getSelectedContent();
      if (contentDescriptor != null) {
        JComponent component = contentDescriptor.getComponent();
        if (component != null) {
          environment = DataManager.getInstance().getDataContext(component).getData(LangDataKeys.EXECUTION_ENVIRONMENT);
        }
      }
    }
    return environment;
  }

  protected boolean isEnabled(AnActionEvent event) {
    RunContentDescriptor descriptor = getDescriptor(event);
    ProcessHandler processHandler = descriptor == null ? null : descriptor.getProcessHandler();
    ExecutionEnvironment environment = getEnvironment(event);
    return environment != null &&
           !ExecutorRegistry.getInstance().isStarting(environment) &&
           !(processHandler != null && processHandler.isProcessTerminating());
  }
}
