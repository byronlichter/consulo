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
package consulo.web.servlet.ui;

import com.intellij.openapi.util.Key;
import com.vaadin.server.Page;
import com.vaadin.ui.UI;
import consulo.ui.*;
import consulo.ui.shared.border.BorderPosition;
import consulo.ui.shared.border.BorderStyle;
import consulo.ui.internal.WGwtRootPanelImpl;
import consulo.ui.shared.Size;
import consulo.ui.style.ColorKey;
import javax.annotation.Nonnull;

import java.util.EventListener;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 11-Sep-17
 */
class VaadinUIWindowImpl implements Window {
  private final UI myUI;
  private WGwtRootPanelImpl myRootPanel = new WGwtRootPanelImpl();

  private boolean myDisposed;

  public VaadinUIWindowImpl(UI ui) {
    myUI = ui;
    myUI.setSizeFull();
    myUI.setContent(myRootPanel);
  }

  @Override
  public void dispose() {
    myDisposed = true;
  }

  @RequiredUIAccess
  @Override
  public void addBorder(@Nonnull BorderPosition borderPosition, BorderStyle borderStyle, ColorKey colorKey, int width) {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public void removeBorder(@Nonnull BorderPosition borderPosition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isVisible() {
    return myUI.isVisible();
  }

  @RequiredUIAccess
  @Override
  public void setVisible(boolean value) {
    myUI.setVisible(value);
  }

  @Override
  public boolean isEnabled() {
    return myUI.isEnabled();
  }

  @RequiredUIAccess
  @Override
  public void setEnabled(boolean value) {
    myUI.setEnabled(value);
  }

  @javax.annotation.Nullable
  @Override
  public Component getParentComponent() {
    return (Component)myUI.getParent();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@Nonnull Size size) {

  }

  @Nonnull
  @Override
  public <T> Runnable addUserDataProvider(@Nonnull Key<T> key, @Nonnull Supplier<T> supplier) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public Runnable addUserDataProvider(@Nonnull Function<Key<?>, Object> function) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public <T extends EventListener> T getListenerDispatcher(@Nonnull Class<T> eventClass) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public <T extends EventListener> Runnable addListener(@Nonnull Class<T> eventClass, @Nonnull T listener) {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public void setTitle(@Nonnull String title) {
    Page.getCurrent().setTitle(title);
  }

  @RequiredUIAccess
  @Override
  public void setContent(@Nonnull Component content) {
    if (myDisposed) {
      throw new IllegalArgumentException("Already disposed");
    }

    myRootPanel.setCenterComponent(content);
  }

  @RequiredUIAccess
  @Override
  public void setMenuBar(@javax.annotation.Nullable MenuBar menuBar) {
    myRootPanel.setMenuBar(menuBar);
  }

  @Override
  public void setResizable(boolean value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setClosable(boolean value) {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public void show() {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public void close() {
    myUI.close();
  }

  @javax.annotation.Nullable
  @Override
  public <T> T getUserData(@Nonnull Key<T> key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> void putUserData(@Nonnull Key<T> key, @javax.annotation.Nullable T value) {
    throw new UnsupportedOperationException();
  }
}
