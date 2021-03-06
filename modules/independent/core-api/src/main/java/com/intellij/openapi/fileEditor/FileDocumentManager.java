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
package com.intellij.openapi.fileEditor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.SavingRequestor;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import consulo.annotations.RequiredDispatchThread;
import consulo.annotations.RequiredReadAction;

public abstract class FileDocumentManager implements SavingRequestor {
  public static FileDocumentManager getInstance() {
    return ApplicationManager.getApplication().getComponent(FileDocumentManager.class);
  }

  @Nullable
  @RequiredReadAction
  public abstract Document getDocument(@Nonnull VirtualFile file);

  @Nullable
  public abstract Document getCachedDocument(@Nonnull VirtualFile file);

  @Nullable
  public abstract VirtualFile getFile(@Nonnull Document document);

  @RequiredDispatchThread
  public abstract void saveAllDocuments();
  @RequiredDispatchThread
  public abstract void saveDocument(@Nonnull Document document);

  /**
   * Saves the document without stripping the trailing spaces or adding a blank line in the end of the file.
   * @param document the document to save.
   */
  @RequiredDispatchThread
  public abstract void saveDocumentAsIs(@Nonnull Document document);
  
  @Nonnull
  public abstract Document[] getUnsavedDocuments();
  public abstract boolean isDocumentUnsaved(@Nonnull Document document);
  public abstract boolean isFileModified(@Nonnull VirtualFile file);

  @RequiredDispatchThread
  public abstract void reloadFromDisk(@Nonnull Document document);

  @Nonnull
  public abstract String getLineSeparator(@Nullable VirtualFile file, @Nullable Project project);

  /**
   * Requests writing access on given document, possibly involving interaction with user.
   *
   * @param document document
   * @param project project 
   * @return true if writing access allowed
   * @see com.intellij.openapi.vfs.ReadonlyStatusHandler#ensureFilesWritable(com.intellij.openapi.project.Project, com.intellij.openapi.vfs.VirtualFile...)
   */
  public abstract boolean requestWriting(@Nonnull Document document, @Nullable Project project);

  public static boolean fileForDocumentCheckedOutSuccessfully(@Nonnull Document document, @Nonnull Project project) {
    return getInstance().requestWriting(document, project);
  }

  @RequiredDispatchThread
  public abstract void reloadFiles(VirtualFile... files);
}
