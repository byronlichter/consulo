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
package com.intellij.execution.actions;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.KillableProcess;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.impl.ExecutionManagerImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.TaskInfo;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.reference.SoftReference;
import com.intellij.ui.components.JBList;
import com.intellij.ui.popup.list.GroupedItemsListRenderer;
import com.intellij.util.IconUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class StopAction extends DumbAwareAction implements AnAction.TransparentUpdate {
  private WeakReference<JBPopup> myActivePopupRef = null;

  private static boolean isPlaceGlobal(AnActionEvent e) {
    return ActionPlaces.isMainMenuOrActionSearch(e.getPlace()) ||
           ActionPlaces.MAIN_TOOLBAR.equals(e.getPlace()) ||
           ActionPlaces.NAVIGATION_BAR_TOOLBAR.equals(e.getPlace());
  }

  @Override
  public void update(final AnActionEvent e) {
    boolean enable = false;
    Icon icon = getTemplatePresentation().getIcon();
    String description = getTemplatePresentation().getDescription();
    Presentation presentation = e.getPresentation();
    if (isPlaceGlobal(e)) {
      List<RunContentDescriptor> stoppableDescriptors = getActiveStoppableDescriptors(e.getDataContext());
      List<Pair<TaskInfo, ProgressIndicator>> cancellableProcesses = getCancellableProcesses(e.getProject());
      int todoSize = stoppableDescriptors.size() + cancellableProcesses.size();
      if (todoSize > 1) {
        presentation.setText(getTemplatePresentation().getText() + "...");
      }
      else if (todoSize == 1) {
        if (stoppableDescriptors.size() == 1) {
          presentation
                  .setText(ExecutionBundle.message("stop.configuration.action.name", StringUtil.escapeMnemonics(stoppableDescriptors.get(0).getDisplayName())));
        }
        else {
          TaskInfo taskInfo = cancellableProcesses.get(0).first;
          presentation.setText(taskInfo.getCancelText() + " " + taskInfo.getTitle());
        }
      }
      else {
        presentation.setText(getTemplatePresentation().getText());
      }
      enable = todoSize > 0;
      if (todoSize > 1) {
        icon = IconUtil.addText(icon, String.valueOf(todoSize));
      }
    }
    else {
      RunContentDescriptor contentDescriptor = e.getData(LangDataKeys.RUN_CONTENT_DESCRIPTOR);
      ProcessHandler processHandler = contentDescriptor == null ? null : contentDescriptor.getProcessHandler();
      if (processHandler != null && !processHandler.isProcessTerminated()) {
        if (!processHandler.isProcessTerminating()) {
          enable = true;
        }
        else if (processHandler instanceof KillableProcess && ((KillableProcess)processHandler).canKillProcess()) {
          enable = true;
          icon = AllIcons.Debugger.KillProcess;
          description = "Kill process";
        }
      }

      RunProfile runProfile = e.getData(LangDataKeys.RUN_PROFILE);
      if (runProfile == null && contentDescriptor == null) {
        presentation.setText(getTemplatePresentation().getText());
      }
      else {
        presentation.setText(ExecutionBundle.message("stop.configuration.action.name", StringUtil
                .escapeMnemonics(runProfile == null ? contentDescriptor.getDisplayName() : runProfile.getName())));
      }
    }

    presentation.setEnabled(enable);
    presentation.setIcon(icon);
    presentation.setDescription(description);
  }

  @Override
  public void actionPerformed(final AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    Project project = e.getProject();
    List<Pair<TaskInfo, ProgressIndicator>> cancellableProcesses = getCancellableProcesses(project);
    List<RunContentDescriptor> stoppableDescriptors = getActiveStoppableDescriptors(dataContext);
    if (isPlaceGlobal(e)) {
      int todoSize = cancellableProcesses.size() + stoppableDescriptors.size();
      if (todoSize == 1) {
        if (!stoppableDescriptors.isEmpty()) {
          ExecutionManagerImpl.stopProcess(stoppableDescriptors.get(0));
        }
        else {
          cancellableProcesses.get(0).second.cancel();
        }
        return;
      }

      Pair<List<HandlerItem>, HandlerItem> handlerItems =
              getItemsList(cancellableProcesses, stoppableDescriptors, getRecentlyStartedContentDescriptor(dataContext));
      if (handlerItems == null || handlerItems.first.isEmpty()) {
        return;
      }

      final JBList<HandlerItem> list = new JBList<>(handlerItems.first);
      if (handlerItems.second != null) list.setSelectedValue(handlerItems.second, true);
      HandlerItem stopAllItem =
              new HandlerItem(ExecutionBundle.message("stop.all", KeymapUtil.getFirstKeyboardShortcutText("Stop")), AllIcons.Actions.Suspend, true) {
                @Override
                void stop() {
                  for (HandlerItem item : handlerItems.first) {
                    item.stop();
                  }
                }
              };
      ((DefaultListModel<HandlerItem>)list.getModel()).addElement(stopAllItem);
      JBPopup activePopup = SoftReference.dereference(myActivePopupRef);
      if (activePopup != null) {
        stopAllItem.stop();
        activePopup.cancel();
        return;
      }

      list.setCellRenderer(new GroupedItemsListRenderer<>(new ListItemDescriptorAdapter<HandlerItem>() {
        @Nullable
        @Override
        public String getTextFor(HandlerItem item) {
          return item.displayName;
        }

        @Nullable
        @Override
        public Icon getIconFor(HandlerItem item) {
          return item.icon;
        }

        @Override
        public boolean hasSeparatorAboveOf(HandlerItem item) {
          return item.hasSeparator;
        }
      }));

      JBPopup popup = JBPopupFactory.getInstance().createListPopupBuilder(list).setMovable(true)
              .setTitle(handlerItems.first.size() == 1 ? "Confirm process stop" : "Stop process").setFilteringEnabled(o -> ((HandlerItem)o).displayName)
              .setItemChoosenCallback(() -> {
                List valuesList = list.getSelectedValuesList();
                for (Object o : valuesList) {
                  if (o instanceof HandlerItem) ((HandlerItem)o).stop();
                }
              }).addListener(new JBPopupAdapter() {
                @Override
                public void onClosed(LightweightWindowEvent event) {
                  myActivePopupRef = null;
                }
              }).setRequestFocus(true).createPopup();
      myActivePopupRef = new WeakReference<>(popup);
      InputEvent inputEvent = e.getInputEvent();
      Component component = inputEvent != null ? inputEvent.getComponent() : null;
      if (component != null && ActionPlaces.MAIN_TOOLBAR.equals(e.getPlace())) {
        popup.showUnderneathOf(component);
      }
      else if (project == null) {
        popup.showInBestPositionFor(dataContext);
      }
      else {
        popup.showCenteredInCurrentWindow(project);
      }
    }
    else {
      ExecutionManagerImpl.stopProcess(getRecentlyStartedContentDescriptor(dataContext));
    }
  }

  @Nonnull
  private static List<Pair<TaskInfo, ProgressIndicator>> getCancellableProcesses(@Nullable Project project) {
    return Collections.emptyList();//Don't confuse users with 'Stop Everything' toolbar button
    //IdeFrame frame = ((WindowManagerEx)WindowManager.getInstance()).findFrameFor(project);
    //StatusBarEx statusBar = frame == null ? null : (StatusBarEx)frame.getStatusBar();
    //if (statusBar == null) return Collections.emptyList();
    //
    //return ContainerUtil.findAll(statusBar.getBackgroundProcesses(),
    //                             pair -> pair.first.isCancellable() && !pair.second.isCanceled());
  }

  @Nullable
  private static Pair<List<HandlerItem>, HandlerItem> getItemsList(List<Pair<TaskInfo, ProgressIndicator>> tasks,
                                                                   List<RunContentDescriptor> descriptors,
                                                                   RunContentDescriptor toSelect) {
    if (tasks.isEmpty() && descriptors.isEmpty()) {
      return null;
    }

    List<HandlerItem> items = new ArrayList<>(tasks.size() + descriptors.size());
    HandlerItem selected = null;
    for (final RunContentDescriptor descriptor : descriptors) {
      final ProcessHandler handler = descriptor.getProcessHandler();
      if (handler != null) {
        HandlerItem item = new HandlerItem(descriptor.getDisplayName(), descriptor.getIcon(), false) {
          @Override
          void stop() {
            ExecutionManagerImpl.stopProcess(descriptor);
          }
        };
        items.add(item);
        if (descriptor == toSelect) {
          selected = item;
        }
      }
    }

    boolean hasSeparator = true;
    for (final Pair<TaskInfo, ProgressIndicator> eachPair : tasks) {
      items.add(new HandlerItem(eachPair.first.getTitle(), AllIcons.Process.Step_passive, hasSeparator) {
        @Override
        void stop() {
          eachPair.second.cancel();
        }
      });
      hasSeparator = false;
    }
    return Pair.create(items, selected);
  }

  @Nullable
  static RunContentDescriptor getRecentlyStartedContentDescriptor(@Nonnull DataContext dataContext) {
    final RunContentDescriptor contentDescriptor = dataContext.getData(LangDataKeys.RUN_CONTENT_DESCRIPTOR);
    if (contentDescriptor != null) {
      // toolwindow case
      return contentDescriptor;
    }
    else {
      // main menu toolbar
      final Project project = dataContext.getData(CommonDataKeys.PROJECT);
      return project == null ? null : ExecutionManager.getInstance(project).getContentManager().getSelectedContent();
    }
  }

  @Nonnull
  private static List<RunContentDescriptor> getActiveStoppableDescriptors(final DataContext dataContext) {
    final Project project = dataContext.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return Collections.emptyList();
    }
    final List<RunContentDescriptor> runningProcesses = ExecutionManager.getInstance(project).getContentManager().getAllDescriptors();
    if (runningProcesses.isEmpty()) {
      return Collections.emptyList();
    }
    final List<RunContentDescriptor> activeDescriptors = new ArrayList<>();
    for (RunContentDescriptor descriptor : runningProcesses) {
      if (canBeStopped(descriptor)) {
        activeDescriptors.add(descriptor);
      }
    }
    return activeDescriptors;
  }

  private static boolean canBeStopped(@Nullable RunContentDescriptor descriptor) {
    @Nullable ProcessHandler processHandler = descriptor != null ? descriptor.getProcessHandler() : null;
    return processHandler != null &&
           !processHandler.isProcessTerminated() &&
           (!processHandler.isProcessTerminating() || processHandler instanceof KillableProcess && ((KillableProcess)processHandler).canKillProcess());
  }

  private abstract static class HandlerItem {
    final String displayName;
    final Icon icon;
    final boolean hasSeparator;

    private HandlerItem(String displayName, Icon icon, boolean hasSeparator) {
      this.displayName = displayName;
      this.icon = icon;
      this.hasSeparator = hasSeparator;
    }

    public String toString() {
      return displayName;
    }

    abstract void stop();
  }
}
