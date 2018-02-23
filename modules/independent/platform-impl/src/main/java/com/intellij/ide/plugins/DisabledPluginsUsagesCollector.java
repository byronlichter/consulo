/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.ide.plugins;

import com.intellij.openapi.project.Project;
import com.intellij.internal.statistic.UsagesCollector;
import com.intellij.internal.statistic.beans.GroupDescriptor;
import com.intellij.internal.statistic.beans.UsageDescriptor;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Set;

public class DisabledPluginsUsagesCollector extends UsagesCollector {
  private static final String GROUP_ID = "disabled-plugins";

  @Override
  @Nonnull
  public GroupDescriptor getGroupId() {
    return GroupDescriptor.create(GROUP_ID, GroupDescriptor.HIGHER_PRIORITY);
  }

  @Override
  @Nonnull
  public Set<UsageDescriptor> getUsages(@Nullable Project project) {
    return ContainerUtil.map2Set(PluginManagerCore.getDisabledPlugins(), new Function<String, UsageDescriptor>() {
      @Override
      public UsageDescriptor fun(String descriptor) {
        return new UsageDescriptor(descriptor, 1);
      }
    });
  }
}
