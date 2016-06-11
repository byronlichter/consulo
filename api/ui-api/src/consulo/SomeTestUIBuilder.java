/*
 * Copyright 2013-2016 must-be.org
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
package consulo;

import consulo.ui.*;
import consulo.ui.layout.DockLayout;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class SomeTestUIBuilder {
  public static Component build(UIAccess uiAccess) {
    DockLayout dockLayout = UIFactory.Layouts.dock();

    final CheckBox top = create("top");
    dockLayout.top(top);
    final CheckBox left = create("left");
    dockLayout.left(left);
    final CheckBox right = create("right");
    dockLayout.right(right);
    final CheckBox bottom = create("bottom");
    dockLayout.bottom(bottom);

    final CheckBox center = create("center");
    center.addSelectListener(new CheckBox.SelectListener() {
      @Override
      @RequiredUIThread
      public void selectChanged(@NotNull CheckBox checkBox) {
        top.setSelected(checkBox.isSelected());
        left.setSelected(checkBox.isSelected());
        right.setSelected(checkBox.isSelected());
        bottom.setSelected(checkBox.isSelected());
      }
    });
    dockLayout.center(center);

    return dockLayout;
  }

  private static CheckBox create(String text) {
    return UIFactory.Components.checkBox("UI proxy?=" + text, true);
  }
}
