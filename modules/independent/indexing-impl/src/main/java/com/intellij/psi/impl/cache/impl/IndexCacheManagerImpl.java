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

package com.intellij.psi.impl.cache.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadActionProcessor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.cache.CacheManager;
import com.intellij.psi.impl.cache.impl.id.IdIndex;
import com.intellij.psi.impl.cache.impl.id.IdIndexEntry;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import javax.annotation.Nonnull;
import consulo.annotations.RequiredReadAction;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 *         Date: Jan 16, 2008
 */
public class IndexCacheManagerImpl extends CacheManager{
  private final Project myProject;
  private final PsiManager myPsiManager;

  public IndexCacheManagerImpl(PsiManager psiManager) {
    myPsiManager = psiManager;
    myProject = psiManager.getProject();
  }

  @Override
  @Nonnull
  public PsiFile[] getFilesWithWord(@Nonnull final String word, final short occurenceMask, @Nonnull final GlobalSearchScope scope, final boolean caseSensitively) {
    if (myProject.isDefault()) {
      return PsiFile.EMPTY_ARRAY;
    }
    CommonProcessors.CollectProcessor<PsiFile> processor = new CommonProcessors.CollectProcessor<PsiFile>();
    processFilesWithWord(processor, word, occurenceMask, scope, caseSensitively);
    return processor.getResults().isEmpty() ? PsiFile.EMPTY_ARRAY : processor.toArray(PsiFile.EMPTY_ARRAY);
  }

  @Override
  @Nonnull
  public VirtualFile[] getVirtualFilesWithWord(@Nonnull final String word, final short occurenceMask, @Nonnull final GlobalSearchScope scope, final boolean caseSensitively) {
    if (myProject.isDefault()) {
      return VirtualFile.EMPTY_ARRAY;
    }

    final List<VirtualFile> vFiles = new ArrayList<VirtualFile>(5);
    collectVirtualFilesWithWord(new CommonProcessors.CollectProcessor<VirtualFile>(vFiles), word, occurenceMask, scope, caseSensitively);
    return vFiles.isEmpty() ? VirtualFile.EMPTY_ARRAY : vFiles.toArray(new VirtualFile[vFiles.size()]);
  }

  // IMPORTANT!!!
  // Since implementation of virtualFileProcessor.process() may call indices directly or indirectly,
  // we cannot call it inside FileBasedIndex.processValues() method except in collecting form
  // If we do, deadlocks are possible (IDEADEV-42137). Process the files without not holding indices' read lock.
  private boolean collectVirtualFilesWithWord(@Nonnull final Processor<VirtualFile> fileProcessor,
                                              @Nonnull final String word, final short occurrenceMask,
                                              @Nonnull final GlobalSearchScope scope, final boolean caseSensitively) {
    if (myProject.isDefault()) {
      return true;
    }

    try {
      return ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
        @Override
        public Boolean compute() {
          return FileBasedIndex.getInstance().processValues(IdIndex.NAME, new IdIndexEntry(word, caseSensitively), null, new FileBasedIndex.ValueProcessor<Integer>() {
            final FileIndexFacade index = FileIndexFacade.getInstance(myProject);
            @Override
            public boolean process(final VirtualFile file, final Integer value) {
              ProgressIndicatorProvider.checkCanceled();
              final int mask = value.intValue();
              if ((mask & occurrenceMask) != 0 && index.shouldBeFound(scope, file)) {
                if (!fileProcessor.process(file)) return false;
              }
              return true;
            }
          }, scope);
        }
      });
    }
    catch (IndexNotReadyException e) {
      throw new ProcessCanceledException();
    }
  }

  @Override
  public boolean processFilesWithWord(@Nonnull final Processor<PsiFile> psiFileProcessor, @Nonnull final String word, final short occurrenceMask, @Nonnull final GlobalSearchScope scope, final boolean caseSensitively) {
    final List<VirtualFile> vFiles = new ArrayList<VirtualFile>(5);
    collectVirtualFilesWithWord(new CommonProcessors.CollectProcessor<VirtualFile>(vFiles), word, occurrenceMask, scope, caseSensitively);
    if (vFiles.isEmpty()) return true;

    final Processor<VirtualFile> virtualFileProcessor = new ReadActionProcessor<VirtualFile>() {
      @RequiredReadAction
      @Override
      public boolean processInReadAction(VirtualFile virtualFile) {
        if (virtualFile.isValid()) {
          final PsiFile psiFile = myPsiManager.findFile(virtualFile);
          return psiFile == null || psiFileProcessor.process(psiFile);
        }
        return true;
      }
    };


    // IMPORTANT!!!
    // Since implementation of virtualFileProcessor.process() may call indices directly or indirectly,
    // we cannot call it inside FileBasedIndex.processValues() method
    // If we do, deadlocks are possible (IDEADEV-42137). So first we obtain files with the word specified,
    // and then process them not holding indices' read lock.
    for (VirtualFile vFile : vFiles) {
      ProgressIndicatorProvider.checkCanceled();
      if (!virtualFileProcessor.process(vFile)) {
        return false;
      }
    }
    return true;
  }
}
