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
package consulo.sandboxPlugin.ide.module.extension;

import consulo.annotations.RequiredDispatchThread;
import consulo.module.extension.MutableModuleExtension;
import consulo.roots.ModuleRootLayer;
import consulo.ui.CheckBox;
import consulo.ui.RequiredUIAccess;
import consulo.ui.VerticalLayout;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 30.08.14
 */
public class Sand2MutableModuleExtension extends Sand2ModuleExtension implements MutableModuleExtension<Sand2ModuleExtension> {
  public Sand2MutableModuleExtension(@Nonnull String id, @Nonnull ModuleRootLayer moduleRootLayer) {
    super(id, moduleRootLayer);
  }

  @RequiredDispatchThread
  @Nullable
  @Override
  public JComponent createConfigurablePanel(@Nonnull Runnable updateOnCheck) {
    throw new UnsupportedOperationException("This should never called. See #createConfigurablePanel2()");
  }

  @Override
  public void setEnabled(boolean val) {
    myIsEnabled = val;
  }

  @Override
  public boolean isModified(@Nonnull Sand2ModuleExtension originalExtension) {
    return myIsEnabled != originalExtension.isEnabled();
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public consulo.ui.Component createConfigurationComponent(@Nonnull Runnable updateOnCheck) {
    final VerticalLayout vertical = VerticalLayout.create();
    vertical.add(CheckBox.create("Omg new UI?"));
    return vertical;
  }
}
