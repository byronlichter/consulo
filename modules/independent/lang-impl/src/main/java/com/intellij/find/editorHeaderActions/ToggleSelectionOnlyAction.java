package com.intellij.find.editorHeaderActions;

import com.intellij.find.EditorSearchSession;
import com.intellij.find.SearchSession;
import com.intellij.openapi.actionSystem.AnActionEvent;
import javax.annotation.Nonnull;

public class ToggleSelectionOnlyAction extends EditorHeaderToggleAction {
  public ToggleSelectionOnlyAction() {
    super("In &Selection");
  }

  @Override
  public void update(AnActionEvent e) {
    super.update(e);

    EditorSearchSession session = e.getData(EditorSearchSession.SESSION_KEY);
    e.getPresentation().setEnabledAndVisible(session != null && session.getFindModel().isReplaceState());
  }

  @Override
  protected boolean isSelected(@Nonnull SearchSession session) {
    return !session.getFindModel().isGlobal();
  }

  @Override
  protected void setSelected(@Nonnull SearchSession session, boolean selected) {
    session.getFindModel().setGlobal(!selected);
  }
}
