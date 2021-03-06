package com.intellij.application.options;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.ui.ListCellRendererWrapper;
import javax.annotation.Nonnull;

import javax.swing.*;

/**
 * @author yole
 */
public class ModuleListCellRenderer extends ListCellRendererWrapper<Module> {
  private final String myEmptySelectionText;

  public ModuleListCellRenderer() {
    this("[none]");
  }

  public ModuleListCellRenderer(@Nonnull String emptySelectionText) {
    myEmptySelectionText = emptySelectionText;
  }

  @Override
  public void customize(JList list, Module module, int index, boolean selected, boolean hasFocus) {
    if (module == null) {
      setText(myEmptySelectionText);
    }
    else {
      setIcon(AllIcons.Nodes.Module);
      setText(module.getName());
    }
  }
}
