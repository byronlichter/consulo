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
package com.intellij.openapi.vfs.encoding;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * @author cdr
 */
public abstract class EncodingProjectManager extends EncodingManager {
  public static EncodingProjectManager getInstance(Project project) {
    return project.getComponent(EncodingProjectManager.class);
  }

  @Nonnull
  public abstract Map<VirtualFile, Charset> getAllMappings();

  public abstract void setMapping(@Nonnull Map<VirtualFile, Charset> result);

  /**
   * @return Project encoding name (configured in Settings|File Encodings|Project Encoding) or empty string if it's configured to "System Default"
   */
  @Nonnull
  @Override
  public abstract String getDefaultCharsetName();

  /**
   * @return Project encoding (configured in Settings|File Encodings|Project Encoding)
   */
  @Nonnull
  @Override
  public abstract Charset getDefaultCharset();

  /**
   * Sets Project encoding (configured in Settings|File Encodings|Project Encoding). Use empty string to specify "System Default"
   */
  @Override
  public abstract void setDefaultCharsetName(@Nonnull String name);
}
