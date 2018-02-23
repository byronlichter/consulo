/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.codeInsight.template;

import com.intellij.codeInsight.template.impl.TemplateImpl;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateSettings;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.util.PsiUtilCore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Eugene.Kudelevsky
 */
public class CustomTemplateCallback {
  private final TemplateManager myTemplateManager;
  @Nonnull
  private final Editor myEditor;
  @Nonnull
  private final PsiFile myFile;
  private final int myOffset;
  @Nonnull
  private final Project myProject;
  private final boolean myInInjectedFragment;
  private Set<TemplateContextType> myApplicableContextTypes;

  public CustomTemplateCallback(@Nonnull Editor editor, @Nonnull PsiFile file) {
    myProject = file.getProject();
    myTemplateManager = TemplateManager.getInstance(myProject);

    myOffset = getOffset(editor);
    PsiElement element = InjectedLanguageUtil.findInjectedElementNoCommit(file, myOffset);
    myFile = element != null ? element.getContainingFile() : file;

    myInInjectedFragment = InjectedLanguageManager.getInstance(myProject).isInjectedFragment(myFile);
    myEditor = myInInjectedFragment ? InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, file, myOffset) : editor;
  }

  public TemplateManager getTemplateManager() {
    return myTemplateManager;
  }

  @Nonnull
  public PsiFile getFile() {
    return myFile;
  }

  @Nonnull
  public PsiElement getContext() {
    return getContext(myFile, myOffset);
  }

  public int getOffset() {
    return myOffset;
  }

  public static int getOffset(@Nonnull Editor editor) {
    return Math.max(editor.getSelectionModel().getSelectionStart() - 1, 0);
  }

  @Nullable
  public TemplateImpl findApplicableTemplate(@Nonnull String key) {
    List<TemplateImpl> templates = findApplicableTemplates(key);
    return templates.size() > 0 ? templates.get(0) : null;
  }

  @Nonnull
  public List<TemplateImpl> findApplicableTemplates(@Nonnull String key) {
    List<TemplateImpl> result = new ArrayList<TemplateImpl>();
    for (TemplateImpl candidate : getMatchingTemplates(key)) {
      if (isAvailableTemplate(candidate)) {
        result.add(candidate);
      }
    }
    return result;
  }

  private boolean isAvailableTemplate(@Nonnull TemplateImpl template) {
    if (myApplicableContextTypes == null) {
      myApplicableContextTypes = TemplateManagerImpl.getApplicableContextTypes(myFile, myOffset);
    }
    return !template.isDeactivated() && TemplateManagerImpl.isApplicable(template, myApplicableContextTypes);
  }

  public void startTemplate(@Nonnull Template template, Map<String, String> predefinedValues, TemplateEditingListener listener) {
    if(myInInjectedFragment) {
      template.setToReformat(false);
    }
    myTemplateManager.startTemplate(myEditor, template, false, predefinedValues, listener);
  }

  @Nonnull
  private static List<TemplateImpl> getMatchingTemplates(@Nonnull String templateKey) {
    TemplateSettings settings = TemplateSettings.getInstance();
    List<TemplateImpl> candidates = new ArrayList<TemplateImpl>();
    for (TemplateImpl template : settings.getTemplates(templateKey)) {
      if (!template.isDeactivated()) {
        candidates.add(template);
      }
    }
    return candidates;
  }

  @Nonnull
  public Editor getEditor() {
    return myEditor;
  }

  @Nonnull
  public FileType getFileType() {
    return myFile.getFileType();
  }

  @Nonnull
  public Project getProject() {
    return myProject;
  }

  public void deleteTemplateKey(@Nonnull String key) {
    int caretAt = myEditor.getCaretModel().getOffset();
    myEditor.getDocument().deleteString(caretAt - key.length(), caretAt);
  }

  @Nonnull
  public static PsiElement getContext(@Nonnull PsiFile file, int offset) {
    PsiElement element = null;
    if (!InjectedLanguageManager.getInstance(file.getProject()).isInjectedFragment(file)) {
      element = InjectedLanguageUtil.findInjectedElementNoCommit(file, offset);
    }
    if (element == null) {
      element = PsiUtilCore.getElementAtOffset(file, offset);
    }
    return element;
  }

  public boolean isInInjectedFragment() {
    return myInInjectedFragment;
  }
}
