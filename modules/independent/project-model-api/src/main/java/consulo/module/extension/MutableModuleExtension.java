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
package consulo.module.extension;

import consulo.roots.ModifiableModuleRootLayer;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import javax.annotation.Nonnull;

import consulo.annotations.RequiredDispatchThread;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 10:58/19.05.13
 */
public interface MutableModuleExtension<T extends ModuleExtension<T>> extends ModuleExtension<T> {
  @javax.annotation.Nullable
  @RequiredDispatchThread
  default JComponent createConfigurablePanel(@Nonnull Runnable updateOnCheck) {
    return null;
  }

  @javax.annotation.Nullable
  @RequiredUIAccess
  default Component createConfigurationComponent(@Nonnull Runnable updateOnCheck) {
    return null;
  }

  @Override
  @Nonnull
  ModifiableModuleRootLayer getModuleRootLayer();

  void setEnabled(boolean val);

  boolean isModified(@Nonnull T originalExtension);
}
