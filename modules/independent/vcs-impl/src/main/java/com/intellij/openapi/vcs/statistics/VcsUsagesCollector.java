/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.openapi.vcs.statistics;

import com.intellij.internal.statistic.AbstractApplicationUsagesCollector;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.internal.statistic.beans.GroupDescriptor;
import com.intellij.internal.statistic.beans.UsageDescriptor;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;

import javax.annotation.Nonnull;

import java.util.Set;

public class VcsUsagesCollector extends AbstractApplicationUsagesCollector {
    private static final String GROUP_ID = "vcs";

    @Nonnull
    public GroupDescriptor getGroupId() {
        return GroupDescriptor.create(GROUP_ID, GroupDescriptor.HIGHER_PRIORITY);
    }

    @Nonnull
    public Set<UsageDescriptor> getProjectUsages(@Nonnull Project project) {
        return ContainerUtil.map2Set(ProjectLevelVcsManager.getInstance(project).getAllActiveVcss(), new Function<AbstractVcs, UsageDescriptor>() {
            @Override
            public UsageDescriptor fun(AbstractVcs vcs) {
                return new UsageDescriptor(vcs.getName(), 1);
            }
        });
    }
}

