/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.execution;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunDialog;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.icons.AllIcons;
import com.intellij.internal.statistic.UsageTrigger;
import com.intellij.internal.statistic.beans.ConvertUsagesUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.LayeredIcon;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;

public class ProgramRunnerUtil {
  private static final Logger LOG = Logger.getInstance(ProgramRunnerUtil.class);

  private ProgramRunnerUtil() {
  }

  @Nullable
  public static ProgramRunner getRunner(@Nonnull final String executorId, final RunnerAndConfigurationSettings configuration) {
    return configuration == null ? null : RunnerRegistry.getInstance().getRunner(executorId, configuration.getConfiguration());
  }

  public static void executeConfiguration(@Nonnull ExecutionEnvironment environment, boolean showSettings, boolean assignNewId) {
    if (ExecutorRegistry.getInstance().isStarting(environment)) {
      return;
    }

    RunnerAndConfigurationSettings runnerAndConfigurationSettings = environment.getRunnerAndConfigurationSettings();
    if (runnerAndConfigurationSettings != null) {
      if (!ExecutionTargetManager.canRun(environment)) {
        ExecutionUtil.handleExecutionError(environment, new ExecutionException(
                StringUtil.escapeXml("Cannot run '" + environment.getRunProfile().getName() + "' on '" + environment.getExecutionTarget().getDisplayName() + "'")));
        return;
      }

      if (!RunManagerImpl.canRunConfiguration(environment) || (showSettings && runnerAndConfigurationSettings.isEditBeforeRun())) {
        if (!RunDialog.editConfiguration(environment, "Edit configuration")) {
          return;
        }

        while (!RunManagerImpl.canRunConfiguration(environment)) {
          if (Messages.YES == Messages
                  .showYesNoDialog(environment.getProject(), "Configuration is still incorrect. Do you want to edit it again?", "Change Configuration Settings",
                                   "Edit", "Continue Anyway", Messages.getErrorIcon())) {
            if (!RunDialog.editConfiguration(environment, "Edit configuration")) {
              return;
            }
          }
          else {
            break;
          }
        }
      }

      ConfigurationType configurationType = runnerAndConfigurationSettings.getType();
      if (configurationType != null) {
        UsageTrigger.trigger("execute." + ConvertUsagesUtil.ensureProperKey(configurationType.getId()) + "." + environment.getExecutor().getId());
      }
    }

    try {
      if (assignNewId) {
        environment.assignNewExecutionId();
      }
      environment.getRunner().execute(environment);
    }
    catch (ExecutionException e) {
      String name = runnerAndConfigurationSettings != null ? runnerAndConfigurationSettings.getName() : null;
      if (name == null) {
        name = environment.getRunProfile().getName();
      }
      if (name == null && environment.getContentToReuse() != null) {
        name = environment.getContentToReuse().getDisplayName();
      }
      if (name == null) {
        name = "<Unknown>";
      }
      ExecutionUtil.handleExecutionError(environment.getProject(), environment.getExecutor().getToolWindowId(), name, e);
    }
  }

  public static void executeConfiguration(@Nonnull Project project,
                                          @Nonnull RunnerAndConfigurationSettings configuration,
                                          @Nonnull Executor executor) {
    ExecutionEnvironmentBuilder builder;
    try {
      builder = ExecutionEnvironmentBuilder.create(executor, configuration);
    }
    catch (ExecutionException e) {
      LOG.error(e);
      return;
    }

    executeConfiguration(builder
                                 .contentToReuse(null)
                                 .dataContext(null)
                                 .activeTarget()
                                 .build(), true, true);
  }

  public static Icon getConfigurationIcon(final RunnerAndConfigurationSettings settings,
                                          final boolean invalid) {
    Icon icon = getRawIcon(settings);

    final Icon configurationIcon = settings.isTemporary() ?  getTemporaryIcon(icon): icon;
    if (invalid) {
      return LayeredIcon.create(configurationIcon, AllIcons.RunConfigurations.InvalidConfigurationLayer);
    }

    return configurationIcon;
  }

  @Nonnull
  public static Icon getRawIcon(RunnerAndConfigurationSettings settings) {
    RunConfiguration configuration = settings.getConfiguration();
    ConfigurationFactory factory = settings.getFactory();
    Icon icon =  factory != null ? factory.getIcon(configuration) : null;
    if (icon == null) icon = AllIcons.RunConfigurations.Unknown;
    return icon;
  }

  public static Icon getTemporaryIcon(@Nonnull Icon rawIcon) {
    return IconLoader.getTransparentIcon(rawIcon, 0.3f);
  }

  public static String shortenName(final String name, final int toBeAdded) {
    if (name == null) return "";
    final int symbols = Math.max(10, 20 - toBeAdded);
    if (name.length() < symbols) return name;
    else return name.substring(0, symbols) + "...";
  }
}
