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
package com.intellij.openapi.roots.ui.configuration.artifacts;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Key;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementType;
import com.intellij.packaging.ui.ArtifactEditor;
import com.intellij.packaging.ui.ArtifactEditorContext;
import javax.annotation.Nonnull;

import javax.swing.*;

/**
 * @author nik
 */
public interface ArtifactEditorEx extends ArtifactEditor, Disposable {
  Key<ArtifactEditorEx> ARTIFACTS_EDITOR_KEY = Key.create("artifactsEditor");

  void addNewPackagingElement(@Nonnull PackagingElementType<?> type);

  void removeSelectedElements();

  void removePackagingElement(@Nonnull String pathToParent, @Nonnull PackagingElement<?> element);

  void replacePackagingElement(@Nonnull String pathToParent, @Nonnull PackagingElement<?> element, @Nonnull PackagingElement<?> replacement);

  LayoutTreeComponent getLayoutTreeComponent();

  Artifact getArtifact();

  CompositePackagingElement<?> getRootElement();

  ArtifactEditorContext getContext();

  JComponent getMainComponent();

  ComplexElementSubstitutionParameters getSubstitutionParameters();

  void queueValidation();

  void rebuildTries();
}
