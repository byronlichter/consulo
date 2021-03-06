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
package com.intellij.ui.components;

import com.intellij.ide.PowerSaveMode;
import com.intellij.ide.RemoteDesktopService;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.registry.RegistryValue;

import javax.swing.*;
import java.awt.*;

import static com.intellij.openapi.application.ApplicationManager.getApplication;

/**
 * @author Sergey.Malenkov
 */
final class ScrollSettings {
  private static final RegistryValue PIXEL_PERFECT_SCROLLING = Registry.get("idea.true.smooth.scrolling.pixel.perfect");
  private static final RegistryValue HIGH_PRECISION_SCROLLING = Registry.get("idea.true.smooth.scrolling.high.precision");
  private static final RegistryValue DEBUG_ENABLED = Registry.get("idea.true.smooth.scrolling.debug");
  private static final RegistryValue BACKGROUND_FROM_VIEW = Registry.get("ide.scroll.background.auto");
  private static final RegistryValue HEADER_OVER_CORNER = Registry.get("ide.scroll.layout.header.over.corner");
  private static final RegistryValue GAP_NEEDED_FOR_ANY_COMPONENT = Registry.get("ide.scroll.align.component");
  private static final RegistryValue HORIZONTAL_GAP_NEEDED_ON_MAC = Registry.get("mac.scroll.horizontal.gap");
  private static final RegistryValue THUMB_SMALL_IF_OPAQUE = Registry.get("ide.scroll.thumb.small.if.opaque");

  static boolean isEligibleFor(Component component) {
    if (component == null || !component.isShowing()) return false;

    Application application = getApplication();
    if (application == null || application.isUnitTestMode()) return false;
    if (PowerSaveMode.isEnabled()) return false;
    if (RemoteDesktopService.isRemoteSession()) return false;

    UISettings settings = UISettings.getInstanceOrNull();
    return settings != null && settings.SMOOTH_SCROLLING;
  }

  static boolean isHighPrecisionEnabled() {
    return HIGH_PRECISION_SCROLLING.asBoolean();
  }

  static boolean isPixelPerfectEnabled() {
    return PIXEL_PERFECT_SCROLLING.asBoolean();
  }

  static boolean isDebugEnabled() {
    return DEBUG_ENABLED.asBoolean();
  }

  static boolean isBackgroundFromView() {
    return BACKGROUND_FROM_VIEW.asBoolean();
  }

  static boolean isHeaderOverCorner(JViewport viewport) {
    Component view = viewport == null ? null : viewport.getView();
    return !isNotSupportedYet(view) && HEADER_OVER_CORNER.asBoolean();
  }

  static boolean isNotSupportedYet(Component view) {
    return view instanceof JTable;
  }

  static boolean isGapNeededForAnyComponent() {
    return GAP_NEEDED_FOR_ANY_COMPONENT.asBoolean();
  }

  static boolean isHorizontalGapNeededOnMac() {
    return HORIZONTAL_GAP_NEEDED_ON_MAC.asBoolean();
  }

  static boolean isThumbSmallIfOpaque() {
    return THUMB_SMALL_IF_OPAQUE.asBoolean();
  }

  /* A heuristics that disables scrolling interpolation in diff / merge windows.
     We need to to make scrolling synchronization compatible with the interpolation first.

     NOTE: The implementation is a temporary, ad-hoc heuristics that is needed solely to
           facilitate testing of the experimental "true smooth scrolling" feature. */
  static boolean isInterpolationEligibleFor(JScrollBar scrollbar) {
    Window window = (Window)scrollbar.getTopLevelAncestor();

    if (window instanceof JDialog && "Commit Changes".equals(((JDialog)window).getTitle())) {
      return false;
    }

    if (!(window instanceof RootPaneContainer)) {
      return true;
    }

    Component[] components = ((RootPaneContainer)window).getContentPane().getComponents();

    if (components.length == 1 && components[0].getClass().getName().contains("DiffWindow")) {
      return false;
    }

    if (components.length == 2 && components[0] instanceof Container) {
      Component[] subComponents = ((Container)components[0]).getComponents();
      if (subComponents.length == 1) {
        String name = subComponents[0].getClass().getName();
        if (name.contains("DiffWindow") || name.contains("MergeWindow")) {
          return false;
        }
      }
    }

    return true;
  }
}
