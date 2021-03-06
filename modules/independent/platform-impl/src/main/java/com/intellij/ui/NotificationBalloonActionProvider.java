/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.notification.impl.NotificationsConfigurable;
import com.intellij.notification.impl.NotificationsConfigurationImpl;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.util.Computable;
import com.intellij.util.Consumer;
import com.intellij.util.ui.JBRectangle;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexander Lobas
 */
public class NotificationBalloonActionProvider implements BalloonImpl.ActionProvider {
  private final BalloonImpl myBalloon;
  private final BalloonLayoutData myLayoutData;
  private final String myDisplayGroupId;
  private final Component myRepaintPanel;
  private BalloonImpl.ActionButton mySettingButton;
  private BalloonImpl.ActionButton myCloseButton;
  private List<BalloonImpl.ActionButton> myActions;

  private static final Rectangle CloseHoverBounds = new JBRectangle(5, 5, 12, 10);

  public NotificationBalloonActionProvider(@Nonnull BalloonImpl balloon,
                                           @Nullable Component repaintPanel,
                                           @Nonnull BalloonLayoutData layoutData,
                                           @Nullable String displayGroupId) {
    myLayoutData = layoutData;
    myDisplayGroupId = displayGroupId;
    myBalloon = balloon;
    myRepaintPanel = repaintPanel;
  }

  @Nonnull
  @Override
  public List<BalloonImpl.ActionButton> createActions() {
    myActions = new ArrayList<BalloonImpl.ActionButton>();

    if (!myLayoutData.showSettingButton || myDisplayGroupId == null ||
        !NotificationsConfigurationImpl.getInstanceImpl().isRegistered(myDisplayGroupId)) {
      mySettingButton = null;
    }
    else {
      mySettingButton = myBalloon.new ActionButton(
              AllIcons.Ide.Notification.Gear, AllIcons.Ide.Notification.GearHover,
              "Configure Notification", new Consumer<MouseEvent>() {
        @Override
        public void consume(MouseEvent event) {
          myBalloon.runWithSmartFadeoutPause(new Runnable() {
            @Override
            public void run() {
              final NotificationsConfigurable configurable = new NotificationsConfigurable();
              ShowSettingsUtil.getInstance().editConfigurable(myLayoutData.project, configurable, new Runnable() {
                @Override
                public void run() {
                  //noinspection ConstantConditions
                  configurable.enableSearch(myDisplayGroupId).run();
                }
              });
            }
          });
        }
      }) {
        @Override
        public void repaint() {
          super.repaint();
          if (myRepaintPanel != null) {
            myRepaintPanel.repaint();
          }
        }
      };
      myActions.add(mySettingButton);

      if (myRepaintPanel != null) {
        myLayoutData.showActions = new Computable<Boolean>() {
          @Override
          public Boolean compute() {
            for (BalloonImpl.ActionButton action : myActions) {
              if (!action.isShowing() || !action.hasPaint()) {
                return Boolean.FALSE;
              }
            }
            return Boolean.TRUE;
          }
        };
      }
    }

    myCloseButton = myBalloon.new ActionButton(
            AllIcons.Ide.Notification.Close, AllIcons.Ide.Notification.CloseHover,
            "Close Notification (Alt-Click close all notifications)", new Consumer<MouseEvent>() {
      @Override
      public void consume(MouseEvent event) {
        final int modifiers = event.getModifiers();
        //noinspection SSBasedInspection
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            if ((modifiers & InputEvent.ALT_MASK) != 0) {
              myLayoutData.closeAll.run();
            }
            else {
              myBalloon.hide();
            }
          }
        });
      }
    }) {
      @Override
      protected void paintIcon(@Nonnull Graphics g, @Nonnull Icon icon) {
        icon.paintIcon(this, g, CloseHoverBounds.x, CloseHoverBounds.y);
      }
    };
    myActions.add(myCloseButton);

    return myActions;
  }

  @Override
  public void layout(@Nonnull Rectangle bounds) {
    Dimension closeSize = myCloseButton.getPreferredSize();
    Insets borderInsets = myBalloon.getShadowBorderInsets();
    int x = bounds.x + bounds.width - borderInsets.right - closeSize.width - myLayoutData.configuration.rightActionsOffset.width;
    int y = bounds.y + borderInsets.top + myLayoutData.configuration.rightActionsOffset.height;
    myCloseButton.setBounds(x - CloseHoverBounds.x, y - CloseHoverBounds.y,
                            closeSize.width + CloseHoverBounds.width, closeSize.height + CloseHoverBounds.height);

    if (mySettingButton != null) {
      Dimension size = mySettingButton.getPreferredSize();
      mySettingButton.setBounds(x - size.width - myLayoutData.configuration.gearCloseSpace, y, size.width, size.height);
    }
  }
}