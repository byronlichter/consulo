/*
 * Copyright 2013-2016 consulo.io
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

import consulo.awt.TargetAWT;
import consulo.ui.Component;
import consulo.ui.DockLayout;
import consulo.ui.RequiredUIAccess;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public class DesktopDockLayoutImpl extends JPanel implements DockLayout, SwingWrapper {
  public DesktopDockLayoutImpl() {
    super(new BorderLayout());
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public DockLayout top(@Nonnull Component component) {
    add(TargetAWT.to(component), BorderLayout.NORTH);
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public DockLayout bottom(@Nonnull Component component) {
    add(TargetAWT.to(component), BorderLayout.SOUTH);
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public DockLayout center(@Nonnull Component component) {
    add(TargetAWT.to(component), BorderLayout.CENTER);
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public DockLayout left(@Nonnull Component component) {
    add(TargetAWT.to(component), BorderLayout.WEST);
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public DockLayout right(@Nonnull Component component) {
    add(TargetAWT.to(component), BorderLayout.EAST);
    return this;
  }
}
