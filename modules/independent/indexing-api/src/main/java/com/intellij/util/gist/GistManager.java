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
package com.intellij.util.gist;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.NullableFunction;
import com.intellij.util.io.DataExternalizer;
import javax.annotation.Nonnull;

/**
 * A helper class for working with file gists: associating persistent data with current VFS or PSI file contents.
 *
 * @since 171.*
 * @author peter
 */
public abstract class GistManager {

  @Nonnull
  public static GistManager getInstance() {
    return ApplicationManager.getApplication().getComponent(GistManager.class);
  }

  /**
   * Create a new {@link VirtualFileGist}.
   * @param id a unique identifier of this data
   * @param version should be incremented each time the {@code externalizer} or {@code calcData} logic changes.
   * @param externalizer used to store the data to the disk and retrieve it
   * @param calcData calculates the data by the file content when needed
   * @param <Data> the type of the data to cache
   * @return the gist object, where {@link VirtualFileGist#getFileData} can later be used to retrieve the cached data
   */
  @Nonnull
  public abstract <Data> VirtualFileGist<Data> newVirtualFileGist(@Nonnull String id,
                                                                  int version,
                                                                  @Nonnull DataExternalizer<Data> externalizer,
                                                                  @Nonnull VirtualFileGist.GistCalculator<Data> calcData);

  /**
   * Create a new {@link PsiFileGist}.
   * @param id a unique identifier of this data
   * @param version should be incremented each time the {@code externalizer} or {@code calcData} logic changes.
   * @param externalizer used to store the data to the disk and retrieve it
   * @param calcData calculates the data by the file content when needed
   * @param <Data> the type of the data to cache
   * @return the gist object, where {@link PsiFileGist#getFileData} can later be used to retrieve the cached data
   */
  @Nonnull
  public abstract <Data> PsiFileGist<Data> newPsiFileGist(@Nonnull String id,
                                                          int version,
                                                          @Nonnull DataExternalizer<Data> externalizer,
                                                          @Nonnull NullableFunction<PsiFile, Data> calcData);

}
