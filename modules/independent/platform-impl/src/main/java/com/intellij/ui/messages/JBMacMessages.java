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
package com.intellij.ui.messages;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.ModalityHelper;
import com.intellij.ui.mac.MacMessageException;
import com.intellij.ui.mac.MacMessagesEmulation;
import com.intellij.ui.mac.foundation.MacUtil;
import com.intellij.util.ui.UIUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Denis Fokin
 */
public class JBMacMessages extends MacMessagesEmulation {

  @Override
  public int showYesNoCancelDialog(@Nonnull String title,
                                   String message,
                                   @Nonnull String defaultButton,
                                   String alternateButton,
                                   String otherButton,
                                   @Nullable Window window,
                                   @Nullable DialogWrapper.DoNotAskOption doNotAskOption) {
    if (window == null) {
      window = getForemostWindow(null);
    }
    SheetMessage sheetMessage = new SheetMessage(window, title, message, UIUtil.getQuestionIcon(),
                                                 new String [] {defaultButton, alternateButton, otherButton}, null, defaultButton, alternateButton);
    String resultString = sheetMessage.getResult();
    int result = resultString.equals(defaultButton) ? Messages.YES : resultString.equals(alternateButton) ? Messages.NO : Messages.CANCEL;
    if (doNotAskOption != null) {
      doNotAskOption.setToBeShown(sheetMessage.toBeShown(), result);
    }
    return result;
  }

  @Override
  public int showMessageDialog(@Nonnull String title,
                               String message,
                               @Nonnull String[] buttons,
                               boolean errorStyle,
                               @Nullable Window window,
                               int defaultOptionIndex,
                               int focusedOptionIndex,
                               @Nullable DialogWrapper.DoNotAskOption doNotAskDialogOption) {
    if (window == null) {
      window = getForemostWindow(null);
    }

    Icon icon = errorStyle ? UIUtil.getErrorIcon() : UIUtil.getInformationIcon();

    focusedOptionIndex = (defaultOptionIndex == focusedOptionIndex) ? buttons.length - 1 : focusedOptionIndex;

    SheetMessage sheetMessage = new SheetMessage(window, title, message, icon, buttons, doNotAskDialogOption, buttons[defaultOptionIndex],
                                                 buttons[focusedOptionIndex]);
    String result = sheetMessage.getResult();
    for (int i = 0; i < buttons.length; i++) {
      if (result.equals(buttons[i])) {
        if (doNotAskDialogOption != null) {
          doNotAskDialogOption.setToBeShown(sheetMessage.toBeShown(), i);
        }
        return i;
      }
    }
    return -1;
  }

  @Override
  public void showOkMessageDialog(@Nonnull String title, String message, @Nonnull String okText, @Nullable Window window) {
    if (window == null) {
      window = getForemostWindow(null);
    }
    new SheetMessage(window, title, message, UIUtil.getInformationIcon(), new String [] {okText}, null, null, okText);
  }

  @Override
  public void showOkMessageDialog(@Nonnull String title, String message, @Nonnull String okText) {
    final Window foremostWindow = getForemostWindow(null);
    new SheetMessage(foremostWindow, title, message, UIUtil.getInformationIcon(), new String [] {okText},null, null, okText);
  }

  private static Window getForemostWindow(final Window window) {
    Window _window = null;
    IdeFocusManager ideFocusManager = IdeFocusManager.getGlobalInstance();

    Component focusOwner = IdeFocusManager.findInstance().getFocusOwner();
    // Let's ask for a focused component first
    if (focusOwner != null) {
      _window = SwingUtilities.getWindowAncestor(focusOwner);
    }

    if (_window == null) {
      // Looks like ide lost focus, let's ask about the last focused component
      focusOwner = ideFocusManager.getLastFocusedFor(ideFocusManager.getLastFocusedFrame());
      if (focusOwner != null) {
        _window = SwingUtilities.getWindowAncestor(focusOwner);
      }
    }

    if (_window == null) {
      _window = WindowManager.getInstance().findVisibleFrame();
    }

    if (_window == null && window != null) {
      // It might be we just has not opened a frame yet.
      // So let's ask AWT
      focusOwner = window.getMostRecentFocusOwner();
      if (focusOwner != null) {
        _window = SwingUtilities.getWindowAncestor(focusOwner);
      }
    }

    if (_window != null) {
      // We have successfully found the window
      // Let's check that we have not missed a blocker
      if (ModalityHelper.isModalBlocked(_window)) {
        _window = ModalityHelper.getModalBlockerFor(_window);
      }
    }

    if (SystemInfo.isAppleJvm && MacUtil.getWindowTitle(_window) == null) {
      // With Apple JDK we cannot find a window if it does not have a title
      // Let's show a dialog instead of the message.
      throw new MacMessageException("MacMessage parent does not have a title.");
    }
    while (_window != null && MacUtil.getWindowTitle(_window) == null) {
      _window = _window.getOwner();
      //At least our frame should have a title
    }

    while (Registry.is("skip.untitled.windows.for.mac.messages") && _window != null && _window instanceof JDialog && !((JDialog)_window).isModal()) {
      _window = _window.getOwner();
    }

    return _window;
  }

  @Override
  public int showYesNoDialog(@Nonnull String title,
                             String message,
                             @Nonnull String yesButton,
                             @Nonnull String noButton,
                             @Nullable Window window) {
    if (window == null) {
      window = getForemostWindow(null);
    }
    SheetMessage sheetMessage = new SheetMessage(window, title, message, UIUtil.getQuestionIcon(),
                                                 new String [] {yesButton, noButton}, null, yesButton, noButton);
    return sheetMessage.getResult().equals(yesButton) ? Messages.YES : Messages.NO;
  }

  @Override
  public int showYesNoDialog(@Nonnull String title,
                             String message,
                             @Nonnull String yesButton,
                             @Nonnull String noButton,
                             @Nullable Window window,
                             @Nullable DialogWrapper.DoNotAskOption doNotAskDialogOption) {
    if (window == null) {
      window = getForemostWindow(null);
    }
    SheetMessage sheetMessage = new SheetMessage(window, title, message, UIUtil.getQuestionIcon(),
                                                 new String [] {yesButton, noButton}, doNotAskDialogOption, yesButton, noButton);
    int result = sheetMessage.getResult().equals(yesButton) ? Messages.YES : Messages.NO;
    if (doNotAskDialogOption != null) {
      doNotAskDialogOption.setToBeShown(sheetMessage.toBeShown(), result);
    }
    return result;
  }

  @Override
  public void showErrorDialog(@Nonnull String title, String message, @Nonnull String okButton, @Nullable Window window) {
    if (window == null) {
      window = getForemostWindow(null);
    }
    new SheetMessage(window, title, message, UIUtil.getErrorIcon(), new String [] {okButton}, null, null, okButton);
  }
}
