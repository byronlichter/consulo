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
package com.intellij.ide.actions.exclusion;

import com.intellij.openapi.util.Key;
import javax.annotation.Nonnull;

import javax.swing.tree.MutableTreeNode;

/**
 * @author Dmitry Batkovich
 */
public interface ExclusionHandler<T extends MutableTreeNode> {
  Key<ExclusionHandler> EXCLUSION_HANDLER = Key.create("tree.exclusion.handler");

  boolean isNodeExclusionAvailable(@Nonnull T node);

  boolean isNodeExcluded(@Nonnull T node);

  void excludeNode(@Nonnull T node);

  void includeNode(@Nonnull T node);

  boolean isActionEnabled(boolean isExcludeAction);

  void onDone(boolean isExcludeAction);
}
