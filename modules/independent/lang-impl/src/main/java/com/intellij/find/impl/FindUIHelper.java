/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.find.impl;

import com.intellij.find.FindBundle;
import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.FindSettings;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import javax.annotation.Nonnull;

import javax.swing.*;

class FindUIHelper implements Disposable {
  @Nonnull
  private final Project myProject;
  @Nonnull
  private  FindModel myModel;
  FindModel myPreviousModel;
  @Nonnull
  private Runnable myOkHandler;

  FindUI myUI;

  FindUIHelper(@Nonnull Project project, @Nonnull FindModel model, @Nonnull Runnable okHandler) {
    myProject = project;
    myModel = model;
    myOkHandler = okHandler;
    myUI = getOrCreateUI();
    myUI.initByModel();
  }

  private FindUI getOrCreateUI() {
    boolean newInstanceRequired = myUI instanceof FindPopupPanel && !Registry.is("ide.find.as.popup") ||
                                  myUI instanceof FindDialog && Registry.is("ide.find.as.popup") ||
                                  myUI == null;
    if (newInstanceRequired) {
      if (Registry.is("ide.find.as.popup")) {
        myUI = new FindPopupPanel(this);
      }
      else {
        FindDialog findDialog = new FindDialog(this);
        registerAction("ReplaceInPath", true, findDialog);
        registerAction("FindInPath", false, findDialog);
        myUI = findDialog;
      }
      Disposer.register(myUI.getDisposable(), this);
    }
    return myUI;
  }

  private void registerAction(String actionName, boolean replace, FindDialog findDialog) {
    AnAction action = ActionManager.getInstance().getAction(actionName);
    JRootPane findDialogRootComponent = ((JDialog)findDialog.getWindow()).getRootPane();
    new AnAction() {
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        myModel.setReplaceState(replace);
        findDialog.initByModel();
      }
      //@NotNull
      //private DataContextWrapper prepareDataContextForFind(@NotNull AnActionEvent e) {
      //  DataContext dataContext = e.getDataContext();
      //  Project project = CommonDataKeys.PROJECT.getData(dataContext);
      //  Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
      //  final String selection = editor != null ? editor.getSelectionModel().getSelectedText() : null;
      //
      //  return new DataContextWrapper(dataContext) {
      //    @Nullable
      //    @Override
      //    public Object getData(@NonNls String dataId) {
      //      if (CommonDataKeys.PROJECT.is(dataId)) return project;
      //      if (PlatformDataKeys.PREDEFINED_TEXT.is(dataId)) return selection;
      //      return super.getData(dataId);
      //    }
      //  };
      //}

    }.registerCustomShortcutSet(action.getShortcutSet(), findDialogRootComponent);
  }


  boolean canSearchThisString() {
    return myUI != null && (!StringUtil.isEmpty(myUI.getStringToFind()) || !myModel.isReplaceState() && !myModel.isFindAllEnabled() && myUI.getFileTypeMask() != null);
  }


  @Nonnull
  public Project getProject() {
    return myProject;
  }

  @Nonnull
  public FindModel getModel() {
    return myModel;
  }

  public void setModel(@Nonnull FindModel model) {
    myModel = model;
    myUI.initByModel();
  }

  public void setOkHandler(@Nonnull Runnable okHandler) {
    myOkHandler = okHandler;
  }

  public void showUI() {
    myUI = getOrCreateUI();
    myUI.showUI();
  }

  @Override
  public void dispose() {
    if (myUI != null && !Disposer.isDisposed(myUI.getDisposable())) {
      Disposer.dispose(myUI.getDisposable());
    }
    myUI = null;
  }

  void updateFindSettings() {
    FindSettings findSettings = FindSettings.getInstance();
    findSettings.setCaseSensitive(myModel.isCaseSensitive());
    if (myModel.isReplaceState()) {
      findSettings.setPreserveCase(myModel.isPreserveCase());
    }

    findSettings.setWholeWordsOnly(myModel.isWholeWordsOnly());
    boolean saveContextBetweenRestarts = false;
    findSettings.setInStringLiteralsOnly(saveContextBetweenRestarts && myModel.isInStringLiteralsOnly());
    findSettings.setInCommentsOnly(saveContextBetweenRestarts && myModel.isInCommentsOnly());
    findSettings.setExceptComments(saveContextBetweenRestarts && myModel.isExceptComments());
    findSettings.setExceptStringLiterals(saveContextBetweenRestarts && myModel.isExceptStringLiterals());
    findSettings.setExceptCommentsAndLiterals(saveContextBetweenRestarts && myModel.isExceptCommentsAndStringLiterals());

    findSettings.setRegularExpressions(myModel.isRegularExpressions());
    if (!myModel.isMultipleFiles()){
      findSettings.setForward(myModel.isForward());
      findSettings.setFromCursor(myModel.isFromCursor());

      findSettings.setGlobal(myModel.isGlobal());
    } else{
      String directoryName = myModel.getDirectoryName();
      if (directoryName != null && !directoryName.isEmpty()) {
        findSettings.setWithSubdirectories(myModel.isWithSubdirectories());
      }
      else if (!StringUtil.isEmpty(myModel.getModuleName())) {
        //do nothing here
      }
      else if (myModel.getCustomScopeName() != null) {
        findSettings.setCustomScope(myModel.getCustomScopeName());
      }
    }

    findSettings.setFileMask(myModel.getFileFilter());
  }

  boolean isUseSeparateView() {
    return FindSettings.getInstance().isShowResultsInSeparateView();
  }

  boolean isSkipResultsWithOneUsage() {
    return FindSettings.getInstance().isSkipResultsWithOneUsage();
  }

  void setUseSeparateView(boolean separateView) {
    if (!myModel.isOpenInNewTabEnabled()) throw new IllegalStateException("'Open in new Tab' is not enabled");
    myModel.setOpenInNewTab(separateView);
    FindSettings.getInstance().setShowResultsInSeparateView(separateView);
  }

  void setSkipResultsWithOneUsage(boolean skip) {
    if (!isReplaceState()) {
      FindSettings.getInstance().setSkipResultsWithOneUsage(skip);
    }
  }

  String getTitle() {
    if (myModel.isReplaceState()){
      return myModel.isMultipleFiles()
             ? FindBundle.message("find.replace.in.project.dialog.title")
             : FindBundle.message("find.replace.text.dialog.title");
    }
    return myModel.isMultipleFiles() ?
           FindBundle.message("find.in.path.dialog.title") :
           FindBundle.message("find.text.dialog.title");
  }

  public boolean isReplaceState() {
    return myModel.isReplaceState();
  }

  @Nonnull
  public Runnable getOkHandler() {
    return myOkHandler;
  }

  public void doOKAction() {
    ((FindManagerImpl)FindManager.getInstance(myProject)).changeGlobalSettings(myModel);
    myOkHandler.run();
  }
}
