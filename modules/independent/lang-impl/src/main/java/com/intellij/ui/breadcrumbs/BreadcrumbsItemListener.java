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
package com.intellij.ui.breadcrumbs;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author spleaner
 */
public interface BreadcrumbsItemListener<T extends BreadcrumbsItem> {
  void itemSelected(@Nonnull final T item, final int modifiers);
  void itemHovered(@Nullable final T item);
}
