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

/**
 * @author cdr
 */
package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkAdditionalData;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.RootProvider;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;

/**
 * used to override SdkHome location in order to provide correct paths
 */
public final class MockSdkWrapper implements Sdk {
  private final String myHomePath;
  private final Sdk myDelegate;

  public MockSdkWrapper(String homePath, @Nonnull Sdk delegate) {
    myHomePath = homePath;
    myDelegate = delegate;
  }

  @Override
  public VirtualFile getHomeDirectory() {
    return LocalFileSystem.getInstance().findFileByIoFile(new File(getHomePath()));
  }

  @Override
  public String getHomePath() {
    final String homePath = FileUtil.toSystemDependentName(myHomePath == null ? myDelegate.getHomePath() : myHomePath);
    return StringUtil.trimEnd(homePath, File.separator);
  }

  @Override
  @Nonnull
  public SdkTypeId getSdkType() {
    return myDelegate.getSdkType();
  }

  @Override
  public boolean isPredefined() {
    return myDelegate.isPredefined();
  }

  @Override
  @Nonnull
  public String getName() {
    return myDelegate.getName();
  }

  @Override
  public String getVersionString() {
    return myDelegate.getVersionString();
  }

  @Override
  @Nonnull
  public RootProvider getRootProvider() {
    return myDelegate.getRootProvider();
  }

  @Override
  public <T> T getUserData(@Nonnull Key<T> key) {
    return myDelegate.getUserData(key);
  }

  @Override
  public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
    myDelegate.putUserData(key, value);
  }

  @Override
  @Nonnull
  public Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
  }

  @Override
  public SdkAdditionalData getSdkAdditionalData() {
    return null;
  }

  @Override
  @Nonnull
  public SdkModificator getSdkModificator() {
    return null;
  }

  public Sdk getDelegate() {
    return myDelegate;
  }
}