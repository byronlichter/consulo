/*
 * Copyright 2013-2016 consulo.io
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
package consulo.web.gwt.shared.transport;

import consulo.annotations.DeprecationInfo;

import java.io.Serializable;
import java.util.List;

/**
 * @author VISTALL
 * @since 18-May-16
 */
@Deprecated
@DeprecationInfo("This is part of research 'consulo as web app'. Code was written in hacky style. Must be dropped, or replaced by Consulo UI API")
public class GwtProjectInfo implements Serializable {
  private String myProjectName;
  private GwtVirtualFile myBaseDirectory;
  private List<String> myModuleDirectoryUrls;

  public GwtProjectInfo(String projectName, GwtVirtualFile baseDirectory, List<String> moduleDirectoryUrls) {
    myProjectName = projectName;
    myBaseDirectory = baseDirectory;
    myModuleDirectoryUrls = moduleDirectoryUrls;
  }

  public GwtProjectInfo() {
  }

  public String getProjectName() {
    return myProjectName;
  }

  public GwtVirtualFile getBaseDirectory() {
    return myBaseDirectory;
  }

  public List<String> getModuleDirectoryUrls() {
    return myModuleDirectoryUrls;
  }
}
