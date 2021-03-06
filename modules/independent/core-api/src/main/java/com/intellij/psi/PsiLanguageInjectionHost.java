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

package com.intellij.psi;

import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.intellij.lang.injection.*;

import java.util.List;

/**
 * Marks psi element as (potentially) containing text in other language.
 * Injected language PSI does not embed into the PSI tree of the hosting element,
 * but is used by IDEA for highlighting, completion and other code insight actions.
 * In order to do the injection, you have to
 * <ul>
 * <li>Implement {@link com.intellij.psi.LanguageInjector} to describe exact place where injection should occur.</li>
 * <li>Register injection in {@link com.intellij.psi.LanguageInjector#EXTENSION_POINT_NAME} extension point.</li>
 * </ul>
 * Currently, language can be injected into string literals, XML tag contents and XML attributes.
 * You don't have to implement PsiLanguageInjectionHost by yourself, unless you want to inject something into your own custom PSI.
 * For all returned injected PSI elements, {@link InjectedLanguageManager#getInjectionHost(PsiElement)} returns PsiLanguageInjectionHost they were injected into.
 */
public interface PsiLanguageInjectionHost extends PsiElement {
  boolean isValidHost();

  PsiLanguageInjectionHost updateText(@Nonnull String text);

  @Nonnull
  LiteralTextEscaper<? extends PsiLanguageInjectionHost> createLiteralTextEscaper();


  interface InjectedPsiVisitor {
    void visit(@Nonnull PsiFile injectedPsi, @Nonnull List<Shred> places);
  }

  interface Shred {
    /**
     * @return returns null when the host document marker is invalid
     */
    @Nullable
    Segment getHostRangeMarker();

    @Nonnull
    TextRange getRangeInsideHost();

    boolean isValid();

    void dispose();

    @Nullable
    PsiLanguageInjectionHost getHost();

    @Nonnull
    TextRange getRange();

    @Nonnull
    String getPrefix();

    @Nonnull
    String getSuffix();
  }
}
