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

package com.intellij.psi.impl;

import com.intellij.extapi.psi.PsiElementBase;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import consulo.annotations.RequiredWriteAction;

import javax.swing.*;

/**
 * @author Dmitry Avdeev
 */
public abstract class FakePsiElement extends PsiElementBase implements PsiNamedElement, ItemPresentation {

  @Override
  public ItemPresentation getPresentation() {
    return this;
  }

  @Override
  @Nonnull
  public Language getLanguage() {
    return Language.ANY;
  }

  @Override
  @Nonnull
  public PsiElement[] getChildren() {
    return PsiElement.EMPTY_ARRAY;
  }

  @Override
  @Nullable
  public PsiElement getFirstChild() {
    return null;
  }

  @Override
  @Nullable
  public PsiElement getLastChild() {
    return null;
  }

  @Override
  @Nullable
  public PsiElement getNextSibling() {
    return null;
  }

  @Override
  @Nullable
  public PsiElement getPrevSibling() {
    return null;
  }

  @Override
  @Nullable
  public TextRange getTextRange() {
    return null;
  }

  @Override
  public int getStartOffsetInParent() {
    return 0;
  }

  @Override
  public int getTextLength() {
    return 0;
  }

  @Override
  @Nullable
  public PsiElement findElementAt(int offset) {
    return null;
  }

  @Override
  public int getTextOffset() {
    return 0;
  }

  @Override
  @Nullable
  @NonNls
  public String getText() {
    return null;
  }

  @Override
  @Nonnull
  public char[] textToCharArray() {
    return new char[0];
  }

  @Override
  public boolean textContains(char c) {
    return false;
  }

  @Override
  @Nullable
  public ASTNode getNode() {
    return null;
  }

  @Override
  public String getPresentableText() {
    return getName();
  }

  @Override
  @Nullable
  public String getLocationString() {
    return null;
  }

  @Override
  @Nullable
  public Icon getIcon(boolean open) {
    return null;
  }

  @RequiredWriteAction
  @Override
  public PsiElement setName(@NonNls @Nonnull String name) throws IncorrectOperationException {
    return null;
  }
}
