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
package consulo.ui.ex;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.WindowInfo;
import com.vaadin.ui.AbstractComponent;
import consulo.ui.internal.image.WGwtImageUrlCache;
import consulo.web.gwt.shared.ui.ex.state.toolWindow.ToolWindowStripeButtonRpc;
import consulo.web.gwt.shared.ui.ex.state.toolWindow.ToolWindowStripeButtonState;
import consulo.web.wm.impl.WebToolWindowInternalDecorator;
import consulo.wm.impl.ToolWindowBase;
import javax.annotation.Nonnull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 25-Sep-17
 */
public class WGwtToolWindowStripeButton extends AbstractComponent implements ToolWindowStripeButton {
  private WebToolWindowInternalDecorator myDecorator;

  private ToolWindowStripeButtonRpc myRpc = () -> {
    if (isSelected()) {
      myDecorator.fireHidden();
    }
    else {
      myDecorator.fireActivated();
    }
  };

  public WGwtToolWindowStripeButton(WebToolWindowInternalDecorator decorator, WGwtToolWindowPanel toolWindowPanel) {
    myDecorator = decorator;

    registerRpc(myRpc);
  }

  @Override
  public void beforeClientResponse(boolean initial) {
    super.beforeClientResponse(initial);

    WindowInfo windowInfo = getWindowInfo();
    ToolWindowStripeButtonState state = getState();
    ToolWindow toolWindow = myDecorator.getToolWindow();

    state.mySecondary = windowInfo.isSplit();
    state.caption = windowInfo.getId();

    Icon icon = toolWindow.getIcon();

    try {
      state.myImageState = icon == null ? null : WGwtImageUrlCache.fixSwingImageRef((consulo.ui.image.Image)icon).getState();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  protected ToolWindowStripeButtonState getState() {
    return (ToolWindowStripeButtonState)super.getState();
  }

  @Nonnull
  @Override
  public WindowInfo getWindowInfo() {
    return myDecorator.getWindowInfo();
  }

  @Override
  public void apply(@Nonnull WindowInfo info) {
    setSelected(info.isVisible() || info.isActive());
    updateState();
    markAsDirty();
  }

  private void updateState() {
    ToolWindowBase window = (ToolWindowBase)myDecorator.getToolWindow();
    boolean toShow = window.isAvailable() || window.isPlaceholderMode();
    if (UISettings.getInstance().ALWAYS_SHOW_WINDOW_BUTTONS) {
      setVisible(window.isShowStripeButton() || isSelected());
    }
    else {
      setVisible(toShow && (window.isShowStripeButton() || isSelected()));
    }
    setEnabled(toShow && !window.isPlaceholderMode());
  }

  public boolean isSelected() {
    return getState().mySelected;
  }

  public void setSelected(boolean selected) {
    getState().mySelected = selected;
  }

  @Override
  public void updatePresentation() {

  }

  @Override
  public void dispose() {

  }
}
