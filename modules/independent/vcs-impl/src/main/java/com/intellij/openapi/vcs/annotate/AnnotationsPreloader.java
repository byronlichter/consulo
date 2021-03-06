/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.openapi.vcs.annotate;

import com.intellij.ide.PowerSaveMode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import javax.annotation.Nonnull;

/**
 * @author egor
 */
public class AnnotationsPreloader {
  private static final Logger LOG = Logger.getInstance(AnnotationsPreloader.class);

  private final MergingUpdateQueue myUpdateQueue;
  private final Project myProject;

  public AnnotationsPreloader(final Project project) {
    myProject = project;
    myUpdateQueue = new MergingUpdateQueue("Annotations preloader queue", 1000, true, null, project, null, false);

    project.getMessageBus().connect(project).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
      @Override
      public void selectionChanged(@Nonnull FileEditorManagerEvent event) {
        if (!isEnabled()) return;
        VirtualFile file = event.getNewFile();
        if (file != null) {
          schedulePreloading(file);
        }
      }
    });
  }

  private static boolean isEnabled() {
    // TODO: check cores number?
    return Registry.is("vcs.annotations.preload") && !PowerSaveMode.isEnabled();
  }

  private void schedulePreloading(@Nonnull final VirtualFile file) {
    if (myProject.isDisposed() || file.getFileType().isBinary()) return;

    myUpdateQueue.queue(new Update(file) {
      @Override
      public void run() {
        try {
          long start = 0;
          if (LOG.isDebugEnabled()) {
            start = System.currentTimeMillis();
          }
          if (!FileEditorManager.getInstance(myProject).isFileOpen(file)) return;

          FileStatus fileStatus = FileStatusManager.getInstance(myProject).getStatus(file);
          if (fileStatus == FileStatus.UNKNOWN || fileStatus == FileStatus.ADDED || fileStatus == FileStatus.IGNORED) {
            return;
          }

          AbstractVcs vcs = ProjectLevelVcsManager.getInstance(myProject).getVcsFor(file);
          if (vcs == null) return;

          AnnotationProvider annotationProvider = vcs.getAnnotationProvider();
          if (annotationProvider == null || !annotationProvider.isCaching()) return;

          annotationProvider.annotate(file);
          if (LOG.isDebugEnabled()) {
            LOG.debug("Preloaded VCS annotations for ", file.getName(), " in ", String.valueOf(System.currentTimeMillis() - start), "ms");
          }
        }
        catch (VcsException e) {
          LOG.info(e);
        }
      }
    });
  }
}
