/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.packaging.artifacts;

import org.consulo.util.pointers.NamedPointer;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.DeprecationInfo;

/**
 * @author nik
 */
public interface ArtifactPointer extends NamedPointer<Artifact> {

  @NotNull
  @Deprecated
  @DeprecationInfo(value = "Use #getName()", until = "2.0")
  String getArtifactName();

  @Nullable
  @Deprecated
  @DeprecationInfo(value = "Use #get()", until = "2.0")
  Artifact getArtifact();

  @NotNull
  String getArtifactName(@NotNull ArtifactModel artifactModel);

  @Nullable
  Artifact findArtifact(@NotNull ArtifactModel artifactModel);

}
