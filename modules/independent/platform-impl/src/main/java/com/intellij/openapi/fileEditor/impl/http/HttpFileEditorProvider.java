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
package com.intellij.openapi.fileEditor.impl.http;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.impl.text.TextEditorState;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.impl.http.HttpVirtualFile;
import org.jdom.Element;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public class HttpFileEditorProvider implements FileEditorProvider, DumbAware {
  public boolean accept(@Nonnull final Project project, @Nonnull final VirtualFile file) {
    return file instanceof HttpVirtualFile && !file.isDirectory();
  }

  @Nonnull
  public FileEditor createEditor(@Nonnull final Project project, @Nonnull final VirtualFile file) {
    return new HttpFileEditor(project, (HttpVirtualFile)file); 
  }

  @Nonnull
  public FileEditorState readState(@Nonnull final Element sourceElement, @Nonnull final Project project, @Nonnull final VirtualFile file) {
    return new TextEditorState();
  }

  public void writeState(@Nonnull final FileEditorState state, @Nonnull final Project project, @Nonnull final Element targetElement) {
  }

  @Nonnull
  public String getEditorTypeId() {
    return "httpFileEditor";
  }

  @Nonnull
  public FileEditorPolicy getPolicy() {
    return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
  }
}
