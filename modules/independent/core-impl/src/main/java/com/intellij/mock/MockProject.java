/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.mock;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ExtensionAreas;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.picocontainer.PicoContainer;

/**
 * @author yole
 */
public class MockProject extends MockComponentManager implements Project {
  private static final Logger LOG = Logger.getInstance("#com.intellij.mock.MockProject");
  private VirtualFile myBaseDir;

  public MockProject(PicoContainer parent, @Nonnull Disposable parentDisposable) {
    super(parent, parentDisposable);
    Extensions.instantiateArea(ExtensionAreas.PROJECT, this, null);
    Disposer.register(parentDisposable, new Disposable() {
      @Override
      public void dispose() {
        Extensions.disposeArea(MockProject.this);
      }
    });
  }

  @Override
  public boolean isDefault() {
    return false;
  }

  @Nonnull
  @Override
  public Condition getDisposed() {
    return new Condition() {
      @Override
      public boolean value(final Object o) {
        return isDisposed();
      }
    };
  }

  @Override
  public boolean isOpen() {
    return false;
  }

  @Override
  public boolean isInitialized() {
    return true;
  }

  @Override
  public VirtualFile getProjectFile() {
    return null;
  }

  @Override
  @Nonnull
  public String getName() {
    return "";
  }

  @Override
  @Nullable
  @NonNls
  public String getPresentableUrl() {
    return null;
  }

  @Override
  @Nonnull
  @NonNls
  public String getLocationHash() {
    return "mock";
  }

  @Override
  @Nonnull
  public String getProjectFilePath() {
    return "";
  }

  @Override
  public VirtualFile getWorkspaceFile() {
    return null;
  }

  public void setBaseDir(VirtualFile baseDir) {
    myBaseDir = baseDir;
  }

  @Override
  @Nullable
  public VirtualFile getBaseDir() {
    return myBaseDir;
  }

  @Override
  public String getBasePath() {
    return null;
  }

  @Override
  public void save() {
  }

  @Override
  public <T> T[] getExtensions(final ExtensionPointName<T> extensionPointName) {
    return Extensions.getArea(this).getExtensionPoint(extensionPointName).getExtensions();
  }

  public void projectOpened() {
    final ProjectComponent[] components = getComponents(ProjectComponent.class);
    for (ProjectComponent component : components) {
      try {
        component.projectOpened();
      }
      catch (Throwable e) {
        LOG.error(component.toString(), e);
      }
    }
  }
}
