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
package com.intellij.openapi.editor.impl.event;

import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import javax.annotation.Nonnull;

import java.util.EventListener;

public interface MarkupModelListener extends EventListener {
  MarkupModelListener[] EMPTY_ARRAY = new MarkupModelListener[0];

  void afterAdded(@Nonnull RangeHighlighterEx highlighter);

  void beforeRemoved(@Nonnull RangeHighlighterEx highlighter);

  void attributesChanged(@Nonnull RangeHighlighterEx highlighter, boolean renderersChanged, boolean fontStyleChanged);

  abstract class Adapter implements MarkupModelListener {
    @Override
    public void afterAdded(@Nonnull RangeHighlighterEx highlighter) {
    }

    @Override
    public void beforeRemoved(@Nonnull RangeHighlighterEx highlighter) {
    }

    @Override
    public void attributesChanged(@Nonnull RangeHighlighterEx highlighter, boolean renderersChanged, boolean fontStyleChanged) {
    }
  }
}
