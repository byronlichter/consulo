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
package com.intellij.xdebugger.impl.breakpoints.ui.grouping;

import com.intellij.ide.presentation.VirtualFilePresentation;
import com.intellij.xdebugger.breakpoints.ui.XBreakpointGroup;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author nik
 */
public class XBreakpointFileGroup extends XBreakpointGroup {
  private final VirtualFile myFile;

  public XBreakpointFileGroup(@Nonnull VirtualFile file) {
    myFile = file;
  }

  @Nullable
  public Icon getIcon(final boolean isOpen) {
    return VirtualFilePresentation.getIcon(myFile);
  }

  @Nonnull
  public String getName() {
    return myFile.getPresentableUrl();
  }

  public VirtualFile getFile() {
    return myFile;
  }
}
