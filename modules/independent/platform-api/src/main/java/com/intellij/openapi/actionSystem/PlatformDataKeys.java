/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.actionSystem;

import com.intellij.ide.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diff.DiffViewer;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.ContentManager;

import java.awt.*;
import java.util.Comparator;

/**
 * @author yole
 */
public interface PlatformDataKeys extends CommonDataKeys {
  Key<FileEditor> FILE_EDITOR = Key.create("fileEditor");

  /**
   *  Returns the text of currently selected file/file revision
   */
  Key<String> FILE_TEXT = Key.create("fileText");

  /**
   * Returns Boolean.TRUE if action is executed in modal context and
   * Boolean.FALSE if action is executed not in modal context. If context
   * is unknown then the value of this data constant is <code>null</code>.
   */
  Key<Boolean> IS_MODAL_CONTEXT = Key.create("isModalContext");
  Key<DiffViewer> DIFF_VIEWER = Key.create("diffViewer");
  Key<DiffViewer> COMPOSITE_DIFF_VIEWER = Key.create("compositeDiffViewer");

  /**
   * Returns help id (String)
   */
  Key<String> HELP_ID = Key.create("helpId");

  /**
   * Returns project if project node is selected (in project view)
   */
  Key<Project> PROJECT_CONTEXT = Key.create("context.Project");

  /**
   * Returns java.awt.Component currently in focus, DataContext should be retrieved for
   */
  Key<Component> CONTEXT_COMPONENT = Key.create("contextComponent");
  Key<CopyProvider> COPY_PROVIDER = Key.create("copyProvider");
  Key<CutProvider> CUT_PROVIDER = Key.create("cutProvider");
  Key<PasteProvider> PASTE_PROVIDER = Key.create("pasteProvider");
  Key<DeleteProvider> DELETE_ELEMENT_PROVIDER = Key.create("deleteElementProvider");
  Key<Object> SELECTED_ITEM = Key.create("selectedItem");
  Key<Object[]> SELECTED_ITEMS = Key.create("selectedItems");
  Key<Rectangle> DOMINANT_HINT_AREA_RECTANGLE = Key.create("dominant.hint.rectangle");
  Key<ContentManager> CONTENT_MANAGER = Key.create("contentManager");
  Key<ToolWindow> TOOL_WINDOW = Key.create("TOOL_WINDOW");
  Key<TreeExpander> TREE_EXPANDER = Key.create("treeExpander");
  Key<ExporterToTextFile> EXPORTER_TO_TEXT_FILE = Key.create("exporterToTextFile");
  Key<VirtualFile> PROJECT_FILE_DIRECTORY = Key.create("context.ProjectFileDirectory");
  Key<Disposable> UI_DISPOSABLE = Key.create("ui.disposable");

  Key<ContentManager> NONEMPTY_CONTENT_MANAGER = Key.create("nonemptyContentManager");
  Key<ModalityState> MODALITY_STATE = Key.create("ModalityState");
  Key<Boolean> SOURCE_NAVIGATION_LOCKED = Key.create("sourceNavigationLocked");

  Key<String> PREDEFINED_TEXT = Key.create("predefined.text.value");

  Key<String> SEARCH_INPUT_TEXT = Key.create("search.input.text.value");
  /**
   * Returns java.awt.Point to guess where to show context menu invoked by key.
   * This point should be relative to the currently focused component
   */
  Key<Point> CONTEXT_MENU_POINT = Key.create("contextMenuPoint");

  /**
   * It's allowed to assign multiple actions to the same keyboard shortcut. Actions system filters them on the current
   * context basis during processing (e.g. we can have two actions assigned to the same shortcut but one of them is
   * configured to be inapplicable in modal dialog context).
   * <p/>
   * However, there is a possible case that there is still more than one action applicable for particular keyboard shortcut
   * after filtering. The first one is executed then. Hence, actions processing order becomes very important.
   * <p/>
   * Current key allows to specify custom actions sorter to use if any. I.e. every component can define it's custom
   * sorting rule in order to define priorities for target actions (classes of actions).
   *
   * @deprecated use com.intellij.openapi.actionSystem.ActionPromoter
   */
  @Deprecated
  Key<Comparator<? super AnAction>> ACTIONS_SORTER = Key.create("actionsSorter");
}
