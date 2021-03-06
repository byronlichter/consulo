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
package com.intellij.diff.actions.impl;

import com.intellij.diff.chains.DiffRequestChain;
import com.intellij.diff.chains.DiffRequestProducer;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Consumer;
import javax.annotation.Nonnull;

import consulo.annotations.RequiredDispatchThread;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

public class GoToChangePopupBuilder {
  public interface Chain extends DiffRequestChain {
    @Nonnull
    AnAction createGoToChangeAction(@Nonnull Consumer<Integer> onSelected);
  }

  @Nonnull
  public static AnAction create(@Nonnull DiffRequestChain chain, @Nonnull Consumer<Integer> onSelected) {
    if (chain instanceof Chain) {
      return ((Chain)chain).createGoToChangeAction(onSelected);
    }
    return new SimpleGoToChangePopupAction(chain, onSelected);
  }

  public static abstract class BaseGoToChangePopupAction<Chain extends DiffRequestChain> extends GoToChangePopupAction {
    @Nonnull
    protected final Chain myChain;
    @Nonnull
    protected final Consumer<Integer> myOnSelected;

    public BaseGoToChangePopupAction(@Nonnull Chain chain, @Nonnull Consumer<Integer> onSelected) {
      myChain = chain;
      myOnSelected = onSelected;
    }

    @RequiredDispatchThread
    @Override
    public void update(@Nonnull AnActionEvent e) {
      if (myChain.getRequests().size() <= 1) {
        e.getPresentation().setEnabledAndVisible(false);
        return;
      }

      e.getPresentation().setEnabledAndVisible(true);
    }

    @RequiredDispatchThread
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      JBPopup popup = createPopup(e);

      InputEvent event = e.getInputEvent();
      if (event instanceof MouseEvent) {
        popup.show(new RelativePoint((MouseEvent)event));
      }
      else {
        popup.showInBestPositionFor(e.getDataContext());
      }
    }

    @Nonnull
    protected abstract JBPopup createPopup(@Nonnull AnActionEvent e);
  }

  private static class SimpleGoToChangePopupAction extends BaseGoToChangePopupAction {

    public SimpleGoToChangePopupAction(@Nonnull DiffRequestChain chain, @Nonnull Consumer<Integer> onSelected) {
      super(chain, onSelected);
    }

    @Nonnull
    @Override
    protected JBPopup createPopup(@Nonnull AnActionEvent e) {
      return JBPopupFactory.getInstance().createListPopup(new MyListPopupStep(e.getProject()));
    }

    private class MyListPopupStep extends BaseListPopupStep<DiffRequestProducer> {
      private final Project myProject;

      public MyListPopupStep(@javax.annotation.Nullable Project project) {
        super("Go To Change", myChain.getRequests());
        setDefaultOptionIndex(myChain.getIndex());
        myProject = project;
      }

      @Nonnull
      @Override
      public String getTextFor(DiffRequestProducer value) {
        return value.getName();
      }

      @Override
      public boolean isSpeedSearchEnabled() {
        return true;
      }

      @Override
      public PopupStep onChosen(final DiffRequestProducer selectedValue, boolean finalChoice) {
        return doFinalStep(new Runnable() {
          @Override
          public void run() {
            int index = myChain.getRequests().indexOf(selectedValue);
            myOnSelected.consume(index);
          }
        });
      }
    }
  }
}
