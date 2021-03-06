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
package com.intellij.application.options.codeStyle.arrangement.match;

import com.intellij.application.options.codeStyle.arrangement.ArrangementConstants;
import com.intellij.application.options.codeStyle.arrangement.color.ArrangementColorsProvider;
import com.intellij.application.options.codeStyle.arrangement.util.TitleWithToolbar;
import com.intellij.ide.ui.customization.CustomizationUtil;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.util.Key;
import com.intellij.psi.codeStyle.arrangement.match.ArrangementSectionRule;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementStandardSettingsManager;
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementRuleAliasToken;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.GridBag;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.List;

/**
 * @author Denis Zhdanov
 * @since 10/30/12 5:28 PM
 */
public class ArrangementMatchingRulesPanel extends JPanel implements DataProvider {

  @Nonnull
  protected final ArrangementSectionRulesControl myControl;

  public ArrangementMatchingRulesPanel(@Nonnull Language language,
                                       @Nonnull ArrangementStandardSettingsManager settingsManager,
                                       @Nonnull ArrangementColorsProvider colorsProvider)
  {
    super(new GridBagLayout());

    JBScrollPane scrollPane = new JBScrollPane();
    final JViewport viewport = scrollPane.getViewport();
    ArrangementSectionRulesControl.RepresentationCallback callback = new ArrangementSectionRulesControl.RepresentationCallback() {
      @Override
      public void ensureVisible(@Nonnull Rectangle r) {
        Rectangle visibleRect = viewport.getViewRect();
        if (r.y <= visibleRect.y) {
          return;
        }

        int excessiveHeight = r.y + r.height - (visibleRect.y + visibleRect.height);
        if (excessiveHeight <= 0) {
          return;
        }

        int verticalShift = Math.min(r.y - visibleRect.y, excessiveHeight);
        if (verticalShift > 0) {
          viewport.setViewPosition(new Point(visibleRect.x, visibleRect.y + verticalShift));
        }
      }
    };
    myControl = createRulesControl(language, settingsManager, colorsProvider, callback);
    scrollPane.setViewportView(myControl);
    CustomizationUtil.installPopupHandler(
            myControl, ArrangementConstants.ACTION_GROUP_MATCHING_RULES_CONTEXT_MENU, ArrangementConstants.MATCHING_RULES_CONTROL_PLACE
    );

    TitleWithToolbar top = new TitleWithToolbar(
            ApplicationBundle.message("arrangement.settings.section.match"),
            ArrangementConstants.ACTION_GROUP_MATCHING_RULES_CONTROL_TOOLBAR,
            ArrangementConstants.MATCHING_RULES_CONTROL_TOOLBAR_PLACE,
            myControl
    );
    add(top, new GridBag().coverLine().fillCellHorizontally().weightx(1));
    add(scrollPane, new GridBag().fillCell().weightx(1).weighty(1).insets(0, ArrangementConstants.HORIZONTAL_PADDING, 0, 0));
  }

  protected ArrangementSectionRulesControl createRulesControl(@Nonnull Language language,
                                                              @Nonnull ArrangementStandardSettingsManager settingsManager,
                                                              @Nonnull ArrangementColorsProvider colorsProvider,
                                                              @Nonnull ArrangementSectionRulesControl.RepresentationCallback callback) {
    return new ArrangementSectionRulesControl(language, settingsManager, colorsProvider, callback);
  }

  @Nonnull
  public List<ArrangementSectionRule> getSections() {
    return myControl.getSections();
  }

  public void setSections(@Nullable List<ArrangementSectionRule> rules) {
    myControl.setSections(rules);
  }

  @Nullable
  public Collection<StdArrangementRuleAliasToken> getRulesAliases() {
    return myControl.getRulesAliases();
  }

  public void setRulesAliases(@Nullable Collection<StdArrangementRuleAliasToken> aliases) {
    myControl.setRulesAliases(aliases);
  }

  public void hideEditor() {
    myControl.hideEditor();
  }

  @Nullable
  @Override
  public Object getData(@Nonnull Key dataId) {
    if (ArrangementSectionRulesControl.KEY == dataId) {
      return myControl;
    }
    return null;
  }
}
