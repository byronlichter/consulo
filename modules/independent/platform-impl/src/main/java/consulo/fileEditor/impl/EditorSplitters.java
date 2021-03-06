/*
 * Copyright 2013-2017 consulo.io
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
package consulo.fileEditor.impl;

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.impl.DesktopEditorsSplitters;
import com.intellij.openapi.fileEditor.impl.EditorWithProviderComposite;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.annotations.DeprecationInfo;
import consulo.ui.Component;
import org.jdom.Element;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 28-Oct-17
 */
public interface EditorSplitters {
  Key<DesktopEditorsSplitters> KEY = Key.create("EditorsSplitters");

  void readExternal(Element element);

  void writeExternal(Element element);

  void openFiles();

  int getSplitCount();

  void startListeningFocus();

  void clear();

  EditorWindow getOrCreateCurrentWindow(VirtualFile file);

  void setCurrentWindow(EditorWindow window, boolean requestFocus);

  @Nullable
  EditorWindow getCurrentWindow();

  void updateFileIcon(VirtualFile virtualFile);

  void updateFileName(VirtualFile virtualFile);

  void updateFileColor(VirtualFile virtualFile);

  void updateFileBackgroundColor(VirtualFile virtualFile);

  VirtualFile[] getOpenFiles();

  AccessToken increaseChange();

  boolean isInsideChange();

  void closeFile(VirtualFile file, boolean moveFocus);

  @Nonnull
  VirtualFile[] getSelectedFiles();

  @Nonnull
  FileEditor[] getSelectedEditors();

  @Nonnull
  List<EditorWithProviderComposite> findEditorComposites(@Nonnull VirtualFile file);

  EditorWithProviderComposite[] getEditorsComposites();

  EditorWindow[] getWindows();

  EditorWindow[] getOrderedWindows();

  @Nullable
  VirtualFile getCurrentFile();

  @Nonnull
  default Component getUIComponent() {
    throw new AbstractMethodError();
  }

  @Deprecated
  @DeprecationInfo("See #getUIComponent()")
  @Nonnull
  default javax.swing.JComponent getComponent() {
    throw new AbstractMethodError();
  }
}
