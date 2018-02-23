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
package com.intellij.diff.tools.util.side;

import com.intellij.diff.tools.holders.EditorHolder;
import com.intellij.diff.tools.holders.TextEditorHolder;
import com.intellij.diff.tools.util.DiffSplitter;
import com.intellij.diff.tools.util.ThreeDiffSplitter;
import com.intellij.diff.util.Side;
import com.intellij.diff.util.ThreeSide;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.util.Consumer;
import javax.annotation.Nonnull;

import consulo.annotations.RequiredDispatchThread;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ThreesideContentPanel extends JPanel {
  @Nonnull
  private final ThreeDiffSplitter mySplitter;
  @javax.annotation.Nullable
  private final EditorEx myBaseEditor;

  public ThreesideContentPanel(@Nonnull List<? extends EditorHolder> holders, @Nonnull List<JComponent> titleComponents) {
    super(new BorderLayout());
    assert holders.size() == 3;
    assert titleComponents.size() == 3;

    EditorHolder baseHolder = ThreeSide.BASE.select(holders);
    myBaseEditor = baseHolder instanceof TextEditorHolder ? ((TextEditorHolder)baseHolder).getEditor() : null;

    ArrayList<JComponent> components = new ArrayList<JComponent>(3);
    for (int i = 0; i < 3; i++) {
      components.add(new HolderPanel(holders.get(i), titleComponents.get(i)));
    }

    mySplitter = new ThreeDiffSplitter(components);
    add(mySplitter, BorderLayout.CENTER);
  }

  @RequiredDispatchThread
  public void setPainter(@javax.annotation.Nullable DiffSplitter.Painter painter, @Nonnull Side side) {
    mySplitter.setPainter(painter, side);
  }

  public void repaintDividers() {
    if (myBaseEditor != null) myBaseEditor.getScrollPane().getVerticalScrollBar().repaint();
    mySplitter.repaintDividers();
  }

  public void repaintDivider(@Nonnull Side side) {
    if (side == Side.RIGHT && myBaseEditor != null) myBaseEditor.getScrollPane().getVerticalScrollBar().repaint();
    mySplitter.repaintDivider(side);
  }

  public void setScrollbarPainter(@Nonnull Consumer<Graphics> painter) {
    if (myBaseEditor != null) myBaseEditor.registerScrollBarRepaintCallback(painter);
  }
}
