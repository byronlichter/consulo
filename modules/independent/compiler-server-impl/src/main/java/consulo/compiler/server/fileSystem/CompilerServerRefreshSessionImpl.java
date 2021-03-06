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
package consulo.compiler.server.fileSystem;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshSession;
import javax.annotation.Nonnull;

import java.util.Collection;

/**
 * @author VISTALL
 * @since 11:34/14.08.13
 */
public class CompilerServerRefreshSessionImpl extends RefreshSession {
  @Override
  public long getId() {
    return 0;
  }

  @Override
  public boolean isAsynchronous() {
    return false;
  }

  @Override
  public void addFile(@Nonnull VirtualFile file) {
  }

  @Override
  public void addAllFiles(@Nonnull Collection<? extends VirtualFile> files) {

  }

  @Override
  public void launch() {
  }
}
