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
package com.intellij.ide.actions;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Trinity;
import com.intellij.ui.ComboboxSpeedSearch;
import com.intellij.ui.ComboboxWithBrowseButton;
import com.intellij.ui.ListCellRendererWrapper;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class TemplateKindCombo extends ComboboxWithBrowseButton {
  private static final Logger LOG = Logger.getInstance("#com.intellij.ide.actions.TemplateKindCombo");

  public TemplateKindCombo() {
    getComboBox().setRenderer(new ListCellRendererWrapper() {
      @Override
      public void customize(final JList list, final Object value, final int index, final boolean selected, final boolean cellHasFocus) {
        if (value instanceof Trinity) {
          setText((String)((Trinity)value).first);
          setIcon ((Icon)((Trinity)value).second);
        }
      }
    });

    new ComboboxSpeedSearch(getComboBox()) {
      @Override
      protected String getElementText(Object element) {
        if (element instanceof Trinity) {
          return (String)((Trinity)element).first;
        }
        return null;
      }
    };
    setButtonListener(null);
  }

  public void addItem(String presentableName, Icon icon, String templateName) {
    getComboBox().addItem(new Trinity<String, Icon, String>(presentableName, icon, templateName));
  }

  public String getSelectedName() {
    //noinspection unchecked
    final Trinity<String, Icon, String> trinity = (Trinity<String, Icon, String>)getComboBox().getSelectedItem();
    if (trinity == null) {
      // design time
      return null;
    }
    return trinity.third;
  }

  public void setSelectedName(@Nullable String name) {
    if (name == null) return;
    ComboBoxModel model = getComboBox().getModel();
    for (int i = 0, n = model.getSize(); i < n; i++) {
      Trinity<String, Icon, String> trinity = (Trinity<String, Icon, String>)model.getElementAt(i);
      if (name.equals(trinity.third)) {
        getComboBox().setSelectedItem(trinity);
        return;
      }
    }
  }

  public void registerUpDownHint(JComponent component) {
    new AnAction() {
      @Override
      public void actionPerformed(AnActionEvent e) {
        if (e.getInputEvent() instanceof KeyEvent) {
          final int code = ((KeyEvent)e.getInputEvent()).getKeyCode();
          scrollBy(code == KeyEvent.VK_DOWN ? 1 : code == KeyEvent.VK_UP ? -1 : 0);
        }
      }
    }.registerCustomShortcutSet(new CustomShortcutSet(KeyEvent.VK_UP, KeyEvent.VK_DOWN), component);
  }

  private void scrollBy(int delta) {
    if (delta == 0) return;
    final int size = getComboBox().getModel().getSize();
    int next = getComboBox().getSelectedIndex() + delta;
    if (next < 0 || next >= size) {
      if (!UISettings.getInstance().CYCLE_SCROLLING) {
        return;
      }
      next = (next + size) % size;
    }
    getComboBox().setSelectedIndex(next);
  }

  /**
   * @param listener pass <code>null</code> to hide browse button
   */
  public void setButtonListener(@Nullable ActionListener listener) {
    getButton().setVisible(listener != null);
    if (listener != null) {
      addActionListener(listener);
    }
  }

  public void clear() {
    getComboBox().removeAllItems();
  }
}
