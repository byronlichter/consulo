/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.openapi.project.impl;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import javax.annotation.Nonnull;

/**
 * Reports some project lifecycle events. Note that these events are published on application-level {@link com.intellij.util.messages.MessageBus}.
 * They're also delivered for subscribers on project and module levels, but they will need to check that the events are relevant, i.e. the
 * {@code project} parameter is the project those subscribers are associated with.
 *
 * @author max
 */
public interface ProjectLifecycleListener {
  Topic<ProjectLifecycleListener> TOPIC = Topic.create("Various stages of project lifecycle notifications", ProjectLifecycleListener.class);

  default void projectComponentsInitialized(@Nonnull Project project) {
  }

  default void beforeProjectLoaded(@Nonnull Project project) {
  }

  default void afterProjectClosed(@Nonnull Project project) {
  }

  /**
   * @deprecated Use {@link ProjectLifecycleListener}
   */
  @Deprecated
  abstract class Adapter implements ProjectLifecycleListener {
  }
}
