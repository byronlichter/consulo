/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.openapi.vfs;

import com.intellij.openapi.vfs.newvfs.NewVirtualFileSystem;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static java.util.Collections.singleton;

public abstract class LocalFileSystem extends NewVirtualFileSystem {
  @NonNls public static final String PROTOCOL = StandardFileSystems.FILE_PROTOCOL;
  @NonNls public static final String PROTOCOL_PREFIX = StandardFileSystems.FILE_PROTOCOL_PREFIX;

  @SuppressWarnings("UtilityClassWithoutPrivateConstructor")
  private static class LocalFileSystemHolder {
    private static final LocalFileSystem ourInstance = (LocalFileSystem)VirtualFileManager.getInstance().getFileSystem(PROTOCOL);
  }

  public static LocalFileSystem getInstance() {
    return LocalFileSystemHolder.ourInstance;
  }

  @Nullable
  public abstract VirtualFile findFileByIoFile(@Nonnull File file);

  @Nullable
  public abstract VirtualFile refreshAndFindFileByIoFile(@Nonnull File file);

  /**
   * Performs a non-recursive synchronous refresh of specified files.
   *
   * @param files files to refresh.
   * @since 6.0
   */
  public abstract void refreshIoFiles(@Nonnull Iterable<File> files);

  public abstract void refreshIoFiles(@Nonnull Iterable<File> files, boolean async, boolean recursive, @Nullable Runnable onFinish);

  /**
   * Performs a non-recursive synchronous refresh of specified files.
   *
   * @param files files to refresh.
   * @since 6.0
   */
  public abstract void refreshFiles(@Nonnull Iterable<VirtualFile> files);

  public abstract void refreshFiles(@Nonnull Iterable<VirtualFile> files, boolean async, boolean recursive, @Nullable Runnable onFinish);

  public interface WatchRequest {
    @Nonnull
    String getRootPath();

    boolean isToWatchRecursively();
  }

  @Nullable
  public WatchRequest addRootToWatch(@Nonnull String rootPath, boolean watchRecursively) {
    Set<WatchRequest> result = addRootsToWatch(singleton(rootPath), watchRecursively);
    return result.size() == 1 ? result.iterator().next() : null;
  }

  @Nonnull
  public Set<WatchRequest> addRootsToWatch(@Nonnull Collection<String> rootPaths, boolean watchRecursively) {
    if (rootPaths.isEmpty()) {
      return Collections.emptySet();
    }
    else if (watchRecursively) {
      return replaceWatchedRoots(Collections.emptySet(), rootPaths, null);
    }
    else {
      return replaceWatchedRoots(Collections.emptySet(), null, rootPaths);
    }
  }

  public void removeWatchedRoot(@Nullable WatchRequest watchRequest) {
    if (watchRequest != null) {
      removeWatchedRoots(singleton(watchRequest));
    }
  }

  public void removeWatchedRoots(@Nonnull Collection<WatchRequest> watchRequests) {
    if (!watchRequests.isEmpty()) {
      replaceWatchedRoots(watchRequests, null, null);
    }
  }

  @Nullable
  public WatchRequest replaceWatchedRoot(@Nullable WatchRequest watchRequest, @Nonnull String rootPath, boolean watchRecursively) {
    Set<WatchRequest> requests = watchRequest != null ? singleton(watchRequest) : Collections.emptySet();
    Set<String> roots = singleton(rootPath);
    Set<WatchRequest> result = watchRecursively ? replaceWatchedRoots(requests, roots, null) : replaceWatchedRoots(requests, null, roots);
    return result.size() == 1 ? result.iterator().next() : null;
  }

  @Nonnull
  public abstract Set<WatchRequest> replaceWatchedRoots(@Nonnull Collection<WatchRequest> watchRequests,
                                                        @Nullable Collection<String> recursiveRoots,
                                                        @Nullable Collection<String> flatRoots);

  /**
   * Registers a handler that allows a version control system plugin to intercept file operations in the local file system
   * and to perform them through the VCS tool.
   *
   * @param handler the handler instance.
   */
  public abstract void registerAuxiliaryFileOperationsHandler(@Nonnull LocalFileOperationsHandler handler);

  /**
   * Unregisters a handler that allows a version control system plugin to intercept file operations in the local file system
   * and to perform them through the VCS tool.
   *
   * @param handler the handler instance.
   */
  public abstract void unregisterAuxiliaryFileOperationsHandler(@Nonnull LocalFileOperationsHandler handler);

  public abstract boolean processCachedFilesInSubtree(@Nonnull VirtualFile file, @Nonnull Processor<VirtualFile> processor);
}
