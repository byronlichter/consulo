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

package com.intellij.codeInsight.daemon.impl.analysis;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCoreUtil;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SingleRootFileViewProvider;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.util.PsiUtilBase;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import java.util.*;

@State(name="HighlightingSettingsPerFile", storages = @Storage( file = StoragePathMacros.WORKSPACE_FILE))
public class HighlightingSettingsPerFile extends HighlightingLevelManager implements PersistentStateComponent<Element> {
  @NonNls private static final String SETTING_TAG = "setting";
  @NonNls private static final String ROOT_ATT_PREFIX = "root";
  @NonNls private static final String FILE_ATT = "file";

  public static HighlightingSettingsPerFile getInstance(Project project){
    return (HighlightingSettingsPerFile)ServiceManager.getService(project, HighlightingLevelManager.class);
  }

  private final Map<VirtualFile, FileHighlightingSetting[]> myHighlightSettings = new HashMap<VirtualFile, FileHighlightingSetting[]>();

  @Nonnull
  public FileHighlightingSetting getHighlightingSettingForRoot(@Nonnull PsiElement root){
    final PsiFile containingFile = root.getContainingFile();
    final VirtualFile virtualFile = containingFile.getVirtualFile();
    FileHighlightingSetting[] fileHighlightingSettings = myHighlightSettings.get(virtualFile);
    final int index = PsiUtilBase.getRootIndex(root);

    if(fileHighlightingSettings == null || fileHighlightingSettings.length <= index) {
      return getDefaultHighlightingSetting(root.getProject(), virtualFile);
    }
    return fileHighlightingSettings[index];
  }

  @Nonnull
  private static FileHighlightingSetting getDefaultHighlightingSetting(@Nonnull Project project, final VirtualFile virtualFile) {
    if (virtualFile != null) {
      DefaultHighlightingSettingProvider[] providers = DefaultHighlightingSettingProvider.EP_NAME.getExtensions();
      List<DefaultHighlightingSettingProvider> filtered =
        DumbService.getInstance(project).filterByDumbAwareness(Arrays.asList(providers));
      for (DefaultHighlightingSettingProvider p : filtered) {
        FileHighlightingSetting setting = p.getDefaultSetting(project, virtualFile);
        if (setting != null) {
          return setting;
        }
      }
    }
    return FileHighlightingSetting.FORCE_HIGHLIGHTING;
  }

  @Nonnull
  private static FileHighlightingSetting[] getDefaults(@Nonnull PsiFile file){
    final int rootsCount = file.getViewProvider().getLanguages().size();
    final FileHighlightingSetting[] fileHighlightingSettings = new FileHighlightingSetting[rootsCount];
    for (int i = 0; i < fileHighlightingSettings.length; i++) {
      fileHighlightingSettings[i] = FileHighlightingSetting.FORCE_HIGHLIGHTING;
    }
    return fileHighlightingSettings;
  }

  public void setHighlightingSettingForRoot(@Nonnull PsiElement root, @Nonnull FileHighlightingSetting setting) {
    final PsiFile containingFile = root.getContainingFile();
    final VirtualFile virtualFile = containingFile.getVirtualFile();
    if (virtualFile == null) return;
    FileHighlightingSetting[] defaults = myHighlightSettings.get(virtualFile);
    int rootIndex = PsiUtilBase.getRootIndex(root);
    if (defaults != null && rootIndex >= defaults.length) defaults = null;
    if (defaults == null) defaults = getDefaults(containingFile);
    defaults[rootIndex] = setting;
    boolean toRemove = true;
    for (FileHighlightingSetting aDefault : defaults) {
      if (aDefault != FileHighlightingSetting.NONE) toRemove = false;
    }
    if (toRemove) {
      myHighlightSettings.remove(virtualFile);
    }
    else {
      myHighlightSettings.put(virtualFile, defaults);
    }
  }

  @Override
  public void loadState(Element element) {
    List children = element.getChildren(SETTING_TAG);
    for (final Object aChildren : children) {
      final Element child = (Element)aChildren;
      final String url = child.getAttributeValue(FILE_ATT);
      if (url == null) continue;
      final VirtualFile fileByUrl = VirtualFileManager.getInstance().findFileByUrl(url);
      if (fileByUrl != null) {
        final List<FileHighlightingSetting> settings = new ArrayList<FileHighlightingSetting>();
        int index = 0;
        while (child.getAttributeValue(ROOT_ATT_PREFIX + index) != null) {
          final String attributeValue = child.getAttributeValue(ROOT_ATT_PREFIX + index++);
          settings.add(Enum.valueOf(FileHighlightingSetting.class, attributeValue));
        }
        myHighlightSettings.put(fileByUrl, settings.toArray(new FileHighlightingSetting[settings.size()]));
      }
    }
  }

  @Override
  public Element getState() {
    final Element element = new Element("state");
    for (Map.Entry<VirtualFile, FileHighlightingSetting[]> entry : myHighlightSettings.entrySet()) {
      final Element child = new Element(SETTING_TAG);

      final VirtualFile vFile = entry.getKey();
      if (!vFile.isValid()) continue;
      child.setAttribute(FILE_ATT, vFile.getUrl());
      for (int i = 0; i < entry.getValue().length; i++) {
        final FileHighlightingSetting fileHighlightingSetting = entry.getValue()[i];
        child.setAttribute(ROOT_ATT_PREFIX + i, fileHighlightingSetting.toString());
      }
      element.addContent(child);
    }
    return element;
  }

  @Override
  public boolean shouldHighlight(@Nonnull PsiElement psiRoot) {
    final FileHighlightingSetting settingForRoot = getHighlightingSettingForRoot(psiRoot);
    return settingForRoot != FileHighlightingSetting.SKIP_HIGHLIGHTING;
  }

  @Override
  public boolean shouldInspect(@Nonnull PsiElement psiRoot) {
    if (ApplicationManager.getApplication().isUnitTestMode()) return true;

    if (!shouldHighlight(psiRoot)) return false;
    final Project project = psiRoot.getProject();
    final VirtualFile virtualFile = psiRoot.getContainingFile().getVirtualFile();
    if (virtualFile == null || !virtualFile.isValid()) return false;

    if (ProjectCoreUtil.isProjectOrWorkspaceFile(virtualFile)) return false;

    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    if (ProjectScope.getLibrariesScope(project).contains(virtualFile) && !fileIndex.isInContent(virtualFile)) return false;

    if (SingleRootFileViewProvider.isTooLargeForIntelligence(virtualFile)) return false;

    final FileHighlightingSetting settingForRoot = getHighlightingSettingForRoot(psiRoot);
    return settingForRoot != FileHighlightingSetting.SKIP_INSPECTION;
  }
}
