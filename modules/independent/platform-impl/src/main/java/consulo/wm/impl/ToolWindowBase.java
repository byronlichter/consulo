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
package consulo.wm.impl;

import com.intellij.ide.UiActivity;
import com.intellij.ide.UiActivityMonitor;
import com.intellij.ide.impl.ContentManagerWatcher;
import com.intellij.notification.EventLog;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowContentUiType;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.impl.ContentImpl;
import com.intellij.util.ObjectUtil;
import com.intellij.util.ui.JBUI;
import consulo.ui.shared.Rectangle2D;
import consulo.ui.RequiredUIAccess;
import consulo.ui.UIAccess;
import consulo.ui.ex.ToolWindowInternalDecorator;
import consulo.ui.image.Image;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * @author VISTALL
 * @since 14-Oct-17
 * <p>
 * Extracted part independent part from IDEA ToolWindowImpl (named DesktopToolWindowImpl)
 */
public abstract class ToolWindowBase implements ToolWindowEx {
  private static final Logger LOG = Logger.getInstance(ToolWindowBase.class);

  protected final ToolWindowManagerBase myToolWindowManager;
  protected ContentManager myContentManager;

  private final PropertyChangeSupport myChangeSupport;
  private final String myId;
  protected boolean myAvailable;
  private String myStripeTitle;

  private ToolWindowInternalDecorator myDecorator;

  private boolean myHideOnEmptyContent = false;
  private boolean myPlaceholderMode;
  private ToolWindowFactory myContentFactory;

  private ActionCallback myActivation = new ActionCallback.Done();

  // fixme [vistall] later replace for consulo.ui.Image
  private Object myIcon;

  private boolean myUseLastFocused = true;

  @RequiredUIAccess
  protected ToolWindowBase(final ToolWindowManagerBase toolWindowManager, final String id, boolean canCloseContent, @Nullable Object component) {
    myToolWindowManager = toolWindowManager;
    myChangeSupport = new PropertyChangeSupport(this);
    myId = id;
    myAvailable = true;

    init(canCloseContent, component);
  }

  @RequiredUIAccess
  protected abstract void init(boolean canCloseContent, @Nullable Object component);

  @Override
  public final void addPropertyChangeListener(final PropertyChangeListener l) {
    myChangeSupport.addPropertyChangeListener(l);
  }

  @Override
  public final void removePropertyChangeListener(final PropertyChangeListener l) {
    myChangeSupport.removePropertyChangeListener(l);
  }

  @RequiredUIAccess
  @Override
  public final void activate(final Runnable runnable) {
    activate(runnable, true);
  }

  @RequiredUIAccess
  @Override
  public void activate(@Nullable final Runnable runnable, final boolean autoFocusContents) {
    activate(runnable, autoFocusContents, true);
  }

  @RequiredUIAccess
  @Override
  public void activate(@Nullable final Runnable runnable, boolean autoFocusContents, boolean forced) {
    UIAccess.assertIsUIThread();

    final UiActivity activity = new UiActivity.Focus("toolWindow:" + myId);
    UiActivityMonitor.getInstance().addActivity(myToolWindowManager.getProject(), activity, ModalityState.NON_MODAL);

    myToolWindowManager.activateToolWindow(myId, forced, autoFocusContents);

    getActivation().doWhenDone(() -> myToolWindowManager.invokeLater(() -> {
      if (runnable != null) {
        runnable.run();
      }
      UiActivityMonitor.getInstance().removeActivity(myToolWindowManager.getProject(), activity);
    }));
  }

  @RequiredUIAccess
  @Override
  public final boolean isActive() {
    UIAccess.assertIsUIThread();
    if (myToolWindowManager.isEditorComponentActive()) return false;
    return myToolWindowManager.isToolWindowActive(myId) || myDecorator != null && myDecorator.isFocused();
  }

  @RequiredUIAccess
  @Override
  public final void show(final Runnable runnable) {
    UIAccess.assertIsUIThread();
    myToolWindowManager.showToolWindow(myId);
    if (runnable != null) {
      getActivation().doWhenDone(() -> myToolWindowManager.invokeLater(runnable));
    }
  }

  @RequiredUIAccess
  @Override
  public final void hide(@Nullable final Runnable runnable) {
    UIAccess.assertIsUIThread();
    myToolWindowManager.hideToolWindow(myId, false);
    if (runnable != null) {
      myToolWindowManager.invokeLater(runnable);
    }
  }

  @Override
  public final boolean isVisible() {
    return myToolWindowManager.isToolWindowVisible(myId);
  }

  @Override
  public final ToolWindowAnchor getAnchor() {
    return myToolWindowManager.getToolWindowAnchor(myId);
  }

  @RequiredUIAccess
  @Override
  public final void setAnchor(@Nonnull final ToolWindowAnchor anchor, @Nullable final Runnable runnable) {
    UIAccess.assertIsUIThread();
    myToolWindowManager.setToolWindowAnchor(myId, anchor);
    if (runnable != null) {
      myToolWindowManager.invokeLater(runnable);
    }
  }

  @RequiredUIAccess
  @Override
  public boolean isSplitMode() {
    UIAccess.assertIsUIThread();
    return myToolWindowManager.isSplitMode(myId);
  }

  @RequiredUIAccess
  @Override
  public void setContentUiType(@Nonnull ToolWindowContentUiType type, @Nullable Runnable runnable) {
    UIAccess.assertIsUIThread();
    myToolWindowManager.setContentUiType(myId, type);
    if (runnable != null) {
      myToolWindowManager.invokeLater(runnable);
    }
  }

  @Override
  public void setDefaultContentUiType(@Nonnull ToolWindowContentUiType type) {
    myToolWindowManager.setDefaultContentUiType(this, type);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public ToolWindowContentUiType getContentUiType() {
    UIAccess.assertIsUIThread();
    return myToolWindowManager.getContentUiType(myId);
  }

  @RequiredUIAccess
  @Override
  public void setSplitMode(final boolean isSideTool, @Nullable final Runnable runnable) {
    UIAccess.assertIsUIThread();
    myToolWindowManager.setSideTool(myId, isSideTool);
    if (runnable != null) {
      myToolWindowManager.invokeLater(runnable);
    }
  }

  @RequiredUIAccess
  @Override
  public final void setAutoHide(final boolean state) {
    UIAccess.assertIsUIThread();
    myToolWindowManager.setToolWindowAutoHide(myId, state);
  }

  @RequiredUIAccess
  @Override
  public final boolean isAutoHide() {
    UIAccess.assertIsUIThread();
    return myToolWindowManager.isToolWindowAutoHide(myId);
  }

  @Nonnull
  @Override
  public final ToolWindowType getType() {
    return myToolWindowManager.getToolWindowType(myId);
  }

  @RequiredUIAccess
  @Override
  public final void setType(@Nonnull final ToolWindowType type, @Nullable final Runnable runnable) {
    UIAccess.assertIsUIThread();
    myToolWindowManager.setToolWindowType(myId, type);
    if (runnable != null) {
      myToolWindowManager.invokeLater(runnable);
    }
  }

  @RequiredUIAccess
  @Override
  public final ToolWindowType getInternalType() {
    UIAccess.assertIsUIThread();
    return myToolWindowManager.getToolWindowInternalType(myId);
  }

  @Override
  public ToolWindowInternalDecorator getDecorator() {
    return myDecorator;
  }

  @Override
  public void setAdditionalGearActions(ActionGroup additionalGearActions) {
    getDecorator().setAdditionalGearActions(additionalGearActions);
  }

  @Override
  public void setTitleActions(AnAction... actions) {
    getDecorator().setTitleActions(actions);
  }

  @RequiredUIAccess
  @Override
  public final void setAvailable(final boolean available, final Runnable runnable) {
    UIAccess.assertIsUIThread();
    final Boolean oldAvailable = myAvailable ? Boolean.TRUE : Boolean.FALSE;
    myAvailable = available;
    myChangeSupport.firePropertyChange(PROP_AVAILABLE, oldAvailable, myAvailable ? Boolean.TRUE : Boolean.FALSE);
    if (runnable != null) {
      myToolWindowManager.invokeLater(runnable);
    }
  }

  @Override
  public void installWatcher(@Nonnull ContentManager contentManager) {
    new ContentManagerWatcher(this, contentManager);
  }

  /**
   * @return <code>true</code> if the component passed into constructor is not instance of
   * <code>ContentManager</code> class. Otherwise it delegates the functionality to the
   * passed content manager.
   */
  @Override
  public boolean isAvailable() {
    return myAvailable;
  }

  @Override
  public ContentManager getContentManager() {
    ensureContentInitialized();
    return myContentManager;
  }

  @Override
  @Nonnull
  public final String getId() {
    return myId;
  }

  @RequiredUIAccess
  @Override
  public final String getTitle() {
    UIAccess.assertIsUIThread();
    return getSelectedContent().getDisplayName();
  }

  @RequiredUIAccess
  @Override
  @Nonnull
  public final String getStripeTitle() {
    UIAccess.assertIsUIThread();
    return ObjectUtil.notNull(myStripeTitle, myId);
  }

  @RequiredUIAccess
  @Override
  public final void setTitle(String title) {
    UIAccess.assertIsUIThread();
    String oldTitle = getTitle();
    getSelectedContent().setDisplayName(title);
    myChangeSupport.firePropertyChange(PROP_TITLE, oldTitle, title);
  }

  @RequiredUIAccess
  @Override
  public final void setStripeTitle(@Nonnull String stripeTitle) {
    UIAccess.assertIsUIThread();
    String oldTitle = myStripeTitle;
    myStripeTitle = stripeTitle;
    myChangeSupport.firePropertyChange(PROP_STRIPE_TITLE, oldTitle, stripeTitle);
  }

  private Content getSelectedContent() {
    final Content selected = getContentManager().getSelectedContent();
    return selected != null ? selected : EMPTY_CONTENT;
  }

  public void setDecorator(final ToolWindowInternalDecorator decorator) {
    myDecorator = decorator;
  }

  public void fireActivated() {
    if (myDecorator != null) {
      myDecorator.fireActivated();
    }
  }

  public void fireHidden() {
    if (myDecorator != null) {
      myDecorator.fireHidden();
    }
  }

  public void fireHiddenSide() {
    if (myDecorator != null) {
      myDecorator.fireHiddenSide();
    }
  }

  public ToolWindowManagerBase getToolWindowManager() {
    return myToolWindowManager;
  }

  @Nullable
  public ActionGroup getPopupGroup() {
    return myDecorator != null ? myDecorator.createPopupGroup() : null;
  }

  @Override
  public void setDefaultState(@Nullable final ToolWindowAnchor anchor, @Nullable final ToolWindowType type, @Nullable final Rectangle2D floatingBounds) {
    myToolWindowManager.setDefaultState(this, anchor, type, floatingBounds);
  }

  @Override
  public void setToHideOnEmptyContent(final boolean hideOnEmpty) {
    myHideOnEmptyContent = hideOnEmpty;
  }

  @Override
  public boolean isToHideOnEmptyContent() {
    return myHideOnEmptyContent;
  }

  @Override
  public void setShowStripeButton(boolean show) {
    myToolWindowManager.setShowStripeButton(myId, show);
  }

  @Override
  public boolean isShowStripeButton() {
    return myToolWindowManager.isShowStripeButton(myId);
  }

  @Override
  public boolean isDisposed() {
    return myContentManager.isDisposed();
  }

  public boolean isPlaceholderMode() {
    return myPlaceholderMode;
  }

  public void setPlaceholderMode(final boolean placeholderMode) {
    myPlaceholderMode = placeholderMode;
  }

  @Override
  public ActionCallback getActivation() {
    return myActivation;
  }

  public ActionCallback setActivation(ActionCallback activation) {
    if (myActivation != null && !myActivation.isProcessed() && !myActivation.equals(activation)) {
      myActivation.setRejected();
    }

    myActivation = activation;
    return myActivation;
  }

  public void setContentFactory(ToolWindowFactory contentFactory) {
    myContentFactory = contentFactory;
    contentFactory.init(this);
  }

  public void ensureContentInitialized() {
    if (myContentFactory != null) {
      ToolWindowFactory contentFactory = myContentFactory;
      // clear it first to avoid SOE
      myContentFactory = null;

      if(!myToolWindowManager.isUnified()) {
        myContentManager.removeAllContents(false);
        contentFactory.createToolWindowContent(myToolWindowManager.getProject(), this);
      }
      else {
        if(contentFactory.isUnified()) {
          myContentManager.removeAllContents(false);
          contentFactory.createToolWindowContent(myToolWindowManager.getProject(), this);
        }
        else {
          myContentFactory = null;
          // nothing
          // FIXME just leave initialize label
        }
      }
    }
  }

  @Override
  public void setUseLastFocusedOnActivation(boolean focus) {
    myUseLastFocused = focus;
  }

  @Override
  public boolean isUseLastFocusedOnActivation() {
    return myUseLastFocused;
  }

  @RequiredUIAccess
  @Override
  public void setUIIcon(@Nullable Image image) {
    UIAccess.assertIsUIThread();
    Object oldIcon = myIcon;

    myIcon = image;
    myChangeSupport.firePropertyChange(PROP_ICON, oldIcon, image);
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public Image getUIIcon() {
    UIAccess.assertIsUIThread();
    if (myIcon instanceof Image) {
      return (Image)myIcon;
    }
    return null;
  }

  /**
   * FIXME [VISTALL] remove this method then swing dependency removed
   */
  @Nullable
  public Object getIconObject() {
    return myIcon;
  }

  // TODO [VISTALL]  AWT & Swing dependency
  // region AWT & Swing dependency
  private static final Content EMPTY_CONTENT = new ContentImpl(new javax.swing.JLabel(), "", false);

  @RequiredUIAccess
  @Override
  @Deprecated
  public final javax.swing.Icon getIcon() {
    UIAccess.assertIsUIThread();
    if (myIcon instanceof Icon) {
      return (Icon)myIcon;
    }
    return null;
  }

  @RequiredUIAccess
  @Override
  @Deprecated
  public void setIcon(final javax.swing.Icon icon) {
    UIAccess.assertIsUIThread();
    final javax.swing.Icon oldIcon = getIcon();
    if (!EventLog.LOG_TOOL_WINDOW_ID.equals(getId())) {
      if (oldIcon != icon && icon != null && !(icon instanceof LayeredIcon) && (icon.getIconHeight() != Math.floor(JBUI.scale(13f)) || icon.getIconWidth() != Math.floor(JBUI.scale(13f)))) {
        LOG.warn("ToolWindow icons should be 13x13. Please fix ToolWindow (ID:  " + getId() + ") or icon " + icon);
      }
    }
    myIcon = icon;
    myChangeSupport.firePropertyChange(PROP_ICON, oldIcon, icon);
  }

  // endregion
}
