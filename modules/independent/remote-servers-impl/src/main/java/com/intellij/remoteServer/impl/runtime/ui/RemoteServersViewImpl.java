package com.intellij.remoteServer.impl.runtime.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.remoteServer.runtime.ServerConnection;
import com.intellij.remoteServer.runtime.ui.RemoteServersView;
import com.intellij.util.ui.UIUtil;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public class RemoteServersViewImpl extends RemoteServersView {
  @Nonnull
  private final Project myProject;

  public RemoteServersViewImpl(@Nonnull Project project) {
    myProject = project;
  }

  @Override
  public void showServerConnection(@Nonnull final ServerConnection<?> connection) {
    final ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(ServersToolWindow.ID);
    if (toolWindow != null) {
      toolWindow.activate(new Runnable() {
        @Override
        public void run() {
          ServersToolWindowContent content = getServersViewComponent(toolWindow);
          if (content != null) {
            content.select(connection);
          }
        }
      });
    }
  }

  private static ServersToolWindowContent getServersViewComponent(ToolWindow toolWindow) {
    //todo[nik] register ServersToolWindowContent as project service?
    return UIUtil.findComponentOfType(toolWindow.getComponent(), ServersToolWindowContent.class);
  }

  @Override
  public void showDeployment(@Nonnull final ServerConnection<?> connection, @Nonnull final String deploymentName) {
    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
    final ToolWindow toolWindow = toolWindowManager.getToolWindow(ServersToolWindow.ID);
    if (toolWindow != null) {
      toolWindowManager.invokeLater(new Runnable() {
        @Override
        public void run() {
          ServersToolWindowContent component = getServersViewComponent(toolWindow);
          if (component != null) {
            component.select(connection, deploymentName);
          }
        }
      });
    }
  }
}
