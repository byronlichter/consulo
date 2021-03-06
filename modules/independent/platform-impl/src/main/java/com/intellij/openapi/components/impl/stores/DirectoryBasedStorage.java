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
package com.intellij.openapi.components.impl.stores;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.components.store.ReadOnlyModificationException;
import com.intellij.openapi.components.store.StateStorageBase;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileAdapter;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.tracker.VirtualFileTracker;
import com.intellij.util.SystemProperties;
import com.intellij.util.containers.SmartHashSet;
import gnu.trove.TObjectObjectProcedure;
import org.jdom.Element;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

public class DirectoryBasedStorage extends StateStorageBase<DirectoryStorageData> {
  private final File myDir;
  private volatile VirtualFile myVirtualFile;
  private final StateSplitterEx mySplitter;

  private DirectoryStorageData myStorageData;

  public DirectoryBasedStorage(@Nullable TrackingPathMacroSubstitutor pathMacroSubstitutor,
                               @Nonnull String dir,
                               @Nonnull StateSplitterEx splitter,
                               @Nonnull Disposable parentDisposable,
                               @Nullable final Listener listener) {
    super(pathMacroSubstitutor);

    myDir = new File(dir);
    mySplitter = splitter;

    VirtualFileTracker virtualFileTracker = ServiceManager.getService(VirtualFileTracker.class);
    if (virtualFileTracker != null && listener != null) {
      virtualFileTracker.addTracker(LocalFileSystem.PROTOCOL_PREFIX + myDir.getAbsolutePath().replace(File.separatorChar, '/'), new VirtualFileAdapter() {
        @Override
        public void contentsChanged(@Nonnull VirtualFileEvent event) {
          notifyIfNeed(event);
        }

        @Override
        public void fileDeleted(@Nonnull VirtualFileEvent event) {
          if (event.getFile().equals(myVirtualFile)) {
            myVirtualFile = null;
          }
          notifyIfNeed(event);
        }

        @Override
        public void fileCreated(@Nonnull VirtualFileEvent event) {
          notifyIfNeed(event);
        }

        private void notifyIfNeed(@Nonnull VirtualFileEvent event) {
          // storage directory will be removed if the only child was removed
          if (event.getFile().isDirectory() || DirectoryStorageData.isStorageFile(event.getFile())) {
            listener.storageFileChanged(event, DirectoryBasedStorage.this);
          }
        }
      }, false, parentDisposable);
    }
  }

  @Override
  public void analyzeExternalChangesAndUpdateIfNeed(@Nonnull Set<String> result) {
    // todo reload only changed file, compute diff
    DirectoryStorageData oldData = myStorageData;
    DirectoryStorageData newData = loadState();
    myStorageData = newData;
    if (oldData == null) {
      result.addAll(newData.getComponentNames());
    }
    else {
      result.addAll(oldData.getComponentNames());
      result.addAll(newData.getComponentNames());
    }
  }

  @Nullable
  @Override
  protected Element getStateAndArchive(@Nonnull DirectoryStorageData storageData, @Nonnull String componentName) {
    return storageData.getCompositeStateAndArchive(componentName, mySplitter);
  }

  @Nonnull
  private DirectoryStorageData loadState() {
    DirectoryStorageData storageData = new DirectoryStorageData();
    storageData.loadFrom(getVirtualFile(), myPathMacroSubstitutor);
    return storageData;
  }

  @Nullable
  private VirtualFile getVirtualFile() {
    VirtualFile virtualFile = myVirtualFile;
    if (virtualFile == null) {
      myVirtualFile = virtualFile = LocalFileSystem.getInstance().findFileByIoFile(myDir);
    }
    return virtualFile;
  }

  @Override
  @Nonnull
  protected DirectoryStorageData getStorageData(boolean reloadData) {
    if (myStorageData != null && !reloadData) {
      return myStorageData;
    }

    myStorageData = loadState();
    return myStorageData;
  }

  @Override
  @Nullable
  public ExternalizationSession startExternalization() {
    return checkIsSavingDisabled() ? null : new MySaveSession(this, getStorageData());
  }

  @Nonnull
  public static VirtualFile createDir(@Nonnull File ioDir, @Nonnull Object requestor) {
    //noinspection ResultOfMethodCallIgnored
    ioDir.mkdirs();
    String parentFile = ioDir.getParent();
    VirtualFile parentVirtualFile = parentFile == null ? null : LocalFileSystem.getInstance().refreshAndFindFileByPath(parentFile.replace(File.separatorChar, '/'));
    if (parentVirtualFile == null) {
      throw new StateStorageException(ProjectBundle.message("project.configuration.save.file.not.found", parentFile));
    }
    return getFile(ioDir.getName(), parentVirtualFile, requestor);
  }

  @Nonnull
  public static VirtualFile getFile(@Nonnull String fileName, @Nonnull VirtualFile parentVirtualFile, @Nonnull Object requestor) {
    VirtualFile file = parentVirtualFile.findChild(fileName);
    if (file != null) {
      return file;
    }

    AccessToken token = ApplicationManager.getApplication().acquireWriteActionLock(DirectoryBasedStorage.class);
    try {
      return parentVirtualFile.createChildData(requestor, fileName);
    }
    catch (IOException e) {
      throw new StateStorageException(e);
    }
    finally {
      token.finish();
    }
  }

  private static class MySaveSession implements SaveSession, ExternalizationSession {
    private final DirectoryBasedStorage storage;
    private final DirectoryStorageData originalStorageData;
    private DirectoryStorageData copiedStorageData;

    private final Set<String> dirtyFileNames = new SmartHashSet<String>();
    private final Set<String> removedFileNames = new SmartHashSet<String>();

    private MySaveSession(@Nonnull DirectoryBasedStorage storage, @Nonnull DirectoryStorageData storageData) {
      this.storage = storage;
      originalStorageData = storageData;
    }

    @Override
    public void setState(@Nonnull Object component, @Nonnull String componentName, @Nonnull Object state, Storage storageSpec) {
      Element compositeState;
      try {
        compositeState = DefaultStateSerializer.serializeState(state, storageSpec);
      }
      catch (WriteExternalException e) {
        LOG.debug(e);
        return;
      }
      catch (Throwable e) {
        LOG.error("Unable to serialize " + componentName + " state", e);
        return;
      }

      removedFileNames.addAll(originalStorageData.getFileNames(componentName));
      if (JDOMUtil.isEmpty(compositeState)) {
        doSetState(componentName, null, null);
      }
      else {
        for (Pair<Element, String> pair : storage.mySplitter.splitState(compositeState)) {
          removedFileNames.remove(pair.second);
          doSetState(componentName, pair.second, pair.first);
        }

        if (!removedFileNames.isEmpty()) {
          for (String fileName : removedFileNames) {
            doSetState(componentName, fileName, null);
          }
        }
      }
    }

    private void doSetState(@Nonnull String componentName, @Nullable String fileName, @Nullable Element subState) {
      if (copiedStorageData == null) {
        copiedStorageData = DirectoryStorageData.setStateAndCloneIfNeed(componentName, fileName, subState, originalStorageData);
        if (copiedStorageData != null && fileName != null) {
          dirtyFileNames.add(fileName);
        }
      }
      else if (copiedStorageData.setState(componentName, fileName, subState) != null && fileName != null) {
        dirtyFileNames.add(fileName);
      }
    }

    @Override
    @Nullable
    public SaveSession createSaveSession() {
      return storage.checkIsSavingDisabled() || copiedStorageData == null ? null : this;
    }

    @Override
    public void save() {
      VirtualFile dir = storage.getVirtualFile();
      if (copiedStorageData.isEmpty()) {
        if (dir != null && dir.exists()) {
          try {
            StorageUtil.deleteFile(this, dir);
          }
          catch (IOException e) {
            throw new StateStorageException(e);
          }
        }
        storage.myStorageData = copiedStorageData;
        return;
      }

      if (dir == null || !dir.isValid()) {
        dir = createDir(storage.myDir, this);
      }

      if (!dirtyFileNames.isEmpty()) {
        saveStates(dir);
      }
      if (dir.exists() && !removedFileNames.isEmpty()) {
        deleteFiles(dir);
      }

      storage.myVirtualFile = dir;
      storage.myStorageData = copiedStorageData;
    }

    private void saveStates(@Nonnull final VirtualFile dir) {
      final Element storeElement = new Element(StorageData.COMPONENT);

      for (final String componentName : copiedStorageData.getComponentNames()) {
        copiedStorageData.processComponent(componentName, new TObjectObjectProcedure<String, Object>() {
          @Override
          public boolean execute(String fileName, Object state) {
            if (!dirtyFileNames.contains(fileName)) {
              return true;
            }

            Element element = copiedStorageData.stateToElement(fileName, state);
            if (storage.myPathMacroSubstitutor != null) {
              storage.myPathMacroSubstitutor.collapsePaths(element);
            }

            try {
              storeElement.setAttribute(StorageData.NAME, componentName);
              storeElement.addContent(element);

              BufferExposingByteArrayOutputStream byteOut;
              VirtualFile file = getFile(fileName, dir, MySaveSession.this);
              if (file.exists()) {
                byteOut = StorageUtil.writeToBytes(storeElement, StorageUtil.loadFile(file).second);
              }
              else {
                byteOut = StorageUtil.writeToBytes(storeElement, SystemProperties.getLineSeparator());
              }
              StorageUtil.writeFile(null, MySaveSession.this, file, byteOut, null);
            }
            catch (IOException e) {
              LOG.error(e);
            }
            finally {
              element.detach();
            }
            return true;
          }
        });
      }
    }

    private void deleteFiles(@Nonnull VirtualFile dir) {
      AccessToken token = ApplicationManager.getApplication().acquireWriteActionLock(DirectoryBasedStorage.class);
      try {
        for (VirtualFile file : dir.getChildren()) {
          if (removedFileNames.contains(file.getName())) {
            deleteFile(file, this);
          }
        }
      }
      finally {
        token.finish();
      }
    }
  }

  public static void deleteFile(@Nonnull VirtualFile file, @Nonnull Object requestor) {
    try {
      file.delete(requestor);
    }
    catch (FileNotFoundException ignored) {
      throw new ReadOnlyModificationException(file);
    }
    catch (IOException e) {
      throw new StateStorageException(e);
    }
  }
}
