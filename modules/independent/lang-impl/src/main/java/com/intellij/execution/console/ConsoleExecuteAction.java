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
package com.intellij.execution.console;

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.EmptyAction;
import com.intellij.openapi.command.impl.UndoManagerImpl;
import com.intellij.openapi.command.undo.DocumentReferenceManager;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ConsoleExecuteAction extends DumbAwareAction {
  static final String CONSOLE_EXECUTE_ACTION_ID = "Console.Execute";

  private final LanguageConsoleView myConsoleView;
  final ConsoleExecuteActionHandler myExecuteActionHandler;
  private final Condition<LanguageConsoleView> myEnabledCondition;

  @SuppressWarnings("UnusedDeclaration")
  public ConsoleExecuteAction(@Nonnull LanguageConsoleView console, @Nonnull BaseConsoleExecuteActionHandler executeActionHandler) {
    this(console, executeActionHandler, CONSOLE_EXECUTE_ACTION_ID, Conditions.<LanguageConsoleView>alwaysTrue());
  }

  ConsoleExecuteAction(@Nonnull LanguageConsoleView console, final @Nonnull ConsoleExecuteActionHandler executeActionHandler, @Nullable Condition<LanguageConsoleView> enabledCondition) {
    this(console, executeActionHandler, CONSOLE_EXECUTE_ACTION_ID, enabledCondition);
  }

  public ConsoleExecuteAction(@Nonnull LanguageConsoleView console,
                              @Nonnull BaseConsoleExecuteActionHandler executeActionHandler,
                              @Nullable Condition<LanguageConsoleView> enabledCondition) {
    this(console, executeActionHandler, CONSOLE_EXECUTE_ACTION_ID, enabledCondition);
  }

  public ConsoleExecuteAction(@Nonnull LanguageConsoleView consoleView,
                              @Nonnull ConsoleExecuteActionHandler executeActionHandler,
                              @Nonnull String emptyExecuteActionId,
                              @Nullable Condition<LanguageConsoleView> enabledCondition) {
    super(null, null, AllIcons.Actions.Execute);

    myConsoleView = consoleView;
    myExecuteActionHandler = executeActionHandler;
    myEnabledCondition = enabledCondition == null ? Conditions.<LanguageConsoleView>alwaysTrue() : enabledCondition;

    EmptyAction.setupAction(this, emptyExecuteActionId, null);
  }

  @Override
  public final void update(@Nonnull AnActionEvent e) {
    EditorEx editor = myConsoleView.getConsoleEditor();
    boolean enabled = !editor.isRendererMode() && isEnabled() &&
                      (myExecuteActionHandler.isEmptyCommandExecutionAllowed() || !StringUtil.isEmptyOrSpaces(editor.getDocument().getCharsSequence()));
    if (enabled) {
      Lookup lookup = LookupManager.getActiveLookup(editor);
      // we should check getCurrentItem() also - fast typing could produce outdated lookup, such lookup reports isCompletion() true
      enabled = lookup == null || !lookup.isCompletion() || lookup.getCurrentItem() == null ||
                (lookup instanceof LookupImpl && ((LookupImpl)lookup).getFocusDegree() == LookupImpl.FocusDegree.UNFOCUSED);
    }

    e.getPresentation().setEnabled(enabled);
  }

  @Override
  public final void actionPerformed(@Nonnull AnActionEvent e) {
    myExecuteActionHandler.runExecuteAction(myConsoleView);
  }

  public boolean isEnabled() {
    return myEnabledCondition.value(myConsoleView);
  }

  public void execute(@Nullable TextRange range, @Nonnull String text, @Nullable EditorEx editor) {
    if (range == null) {
      ((LanguageConsoleImpl)myConsoleView).doAddPromptToHistory();
      myConsoleView.print(text, ConsoleViewContentType.USER_INPUT);
      if (!text.endsWith("\n")) {
        myConsoleView.print("\n", ConsoleViewContentType.USER_INPUT);
      }
    }
    else {
      assert editor != null;
      ((LanguageConsoleImpl)myConsoleView).addTextRangeToHistory(range, editor, myExecuteActionHandler.myPreserveMarkup);
    }
    myExecuteActionHandler.addToCommandHistoryAndExecute(myConsoleView, text);
  }

  public static abstract class ConsoleExecuteActionHandler {

    private boolean myAddToHistory = true;
    final boolean myPreserveMarkup;

    boolean myUseProcessStdIn;

    public ConsoleExecuteActionHandler(boolean preserveMarkup) {
      myPreserveMarkup = preserveMarkup;
    }

    public boolean isEmptyCommandExecutionAllowed() {
      return true;
    }

    public final void setAddCurrentToHistory(boolean addCurrentToHistory) {
      myAddToHistory = addCurrentToHistory;
    }

    protected void beforeExecution(@Nonnull LanguageConsoleView consoleView) {
    }

    public void runExecuteAction(@Nonnull LanguageConsoleView consoleView) {
      if (!myUseProcessStdIn) {
        beforeExecution(consoleView);
      }

      String text = ((LanguageConsoleImpl)consoleView).prepareExecuteAction(myAddToHistory && !myUseProcessStdIn,
                                                                            myPreserveMarkup, true);
      ((UndoManagerImpl)UndoManager.getInstance(consoleView.getProject())).invalidateActionsFor(DocumentReferenceManager.getInstance().create(
              consoleView.getCurrentEditor().getDocument()));

      if (myUseProcessStdIn) {
        consoleView.print(text, ConsoleViewContentType.USER_INPUT);
        consoleView.print("\n", ConsoleViewContentType.USER_INPUT);
      }
      else {
        addToCommandHistoryAndExecute(consoleView, text);
      }
    }

    private void addToCommandHistoryAndExecute(@Nonnull LanguageConsoleView consoleView, @Nonnull String text) {
      ConsoleHistoryController.addToHistory(consoleView, text);
      doExecute(text, consoleView);
    }

    abstract void doExecute(@Nonnull String text, @Nonnull LanguageConsoleView consoleView);
  }
}