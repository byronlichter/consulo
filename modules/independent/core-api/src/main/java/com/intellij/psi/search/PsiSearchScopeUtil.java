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
package com.intellij.psi.search;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PsiSearchScopeUtil {

  @Nullable
  public static SearchScope union(@Nullable SearchScope a, @Nullable SearchScope b) {
    return a == null ? b : b == null ? a : a.union(b);
  }

  /**
   * @deprecated
   * Use com.intellij.psi.search.SearchScope#union(com.intellij.psi.search.SearchScope)
   */
  @Deprecated
  @Nonnull
  public static SearchScope scopesUnion(@Nonnull SearchScope scope1, @Nonnull SearchScope scope2) {
    return scope1.union(scope2);
  }

  public static boolean isInScope(@Nonnull SearchScope scope, @Nonnull PsiElement element) {
    if (scope instanceof LocalSearchScope) {
      LocalSearchScope local = (LocalSearchScope)scope;
      return isInScope(local, element);
    }
    else {
      GlobalSearchScope globalScope = (GlobalSearchScope)scope;
      return isInScope(globalScope, element);
    }
  }

  public static boolean isInScope(@Nonnull GlobalSearchScope globalScope, @Nonnull PsiElement element) {
    PsiFile file = element.getContainingFile();
    if (file == null) {
      return true;
    }
    final PsiElement context = file.getContext();
    if (context != null) file = context.getContainingFile();
    if (file == null) return false;
    VirtualFile virtualFile = file.getVirtualFile();
    return virtualFile == null || globalScope.contains(file.getVirtualFile());
  }

  public static boolean isInScope(@Nonnull LocalSearchScope local, @Nonnull PsiElement element) {
    PsiElement[] scopeElements = local.getScope();
    for (final PsiElement scopeElement : scopeElements) {
      if (PsiTreeUtil.isAncestor(scopeElement, element, false)) return true;
    }
    return false;
  }
}