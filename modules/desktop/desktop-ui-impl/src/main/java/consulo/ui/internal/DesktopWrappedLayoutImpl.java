/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ui.internal;

import com.intellij.ui.components.panels.Wrapper2;
import consulo.awt.TargetAWT;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.WrappedLayout;
import javax.annotation.Nonnull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 25-Oct-17
 */
public class DesktopWrappedLayoutImpl extends Wrapper2 implements WrappedLayout, SwingWrapper {
  @RequiredUIAccess
  @Nonnull
  @Override
  public WrappedLayout set(@javax.annotation.Nullable Component component) {
    setContent(component == null ? null : (JComponent)TargetAWT.to(component));
    return this;
  }
}
