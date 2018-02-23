/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.psi.search.searches;

import com.intellij.openapi.application.DumbAwareSearchParameters;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.*;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;

/**
 * Locates all references to a specified PSI element.
 *
 * @see PsiReference
 * @author max
 */
public class ReferencesSearch extends ExtensibleQueryFactory<PsiReference, ReferencesSearch.SearchParameters> {
  public static final ExtensionPointName<QueryExecutor> EP_NAME = ExtensionPointName.create("com.intellij.referencesSearch");
  private static final ReferencesSearch INSTANCE = new ReferencesSearch();

  private ReferencesSearch() {
  }

  public static class SearchParameters implements DumbAwareSearchParameters {
    private final PsiElement myElementToSearch;
    private final SearchScope myScope;
    private final boolean myIgnoreAccessScope;
    private final SearchRequestCollector myOptimizer;
    private final Project myProject;
    private final boolean isSharedOptimizer;

    public SearchParameters(@Nonnull PsiElement elementToSearch, @Nonnull SearchScope scope, boolean ignoreAccessScope, @javax.annotation.Nullable SearchRequestCollector optimizer) {
      myElementToSearch = elementToSearch;
      myScope = scope;
      myIgnoreAccessScope = ignoreAccessScope;
      isSharedOptimizer = optimizer != null;
      myOptimizer = optimizer == null ? new SearchRequestCollector(new SearchSession()) : optimizer;
      myProject = PsiUtilCore.getProjectInReadAction(elementToSearch);
    }

    public SearchParameters(@Nonnull PsiElement elementToSearch, @Nonnull SearchScope scope, final boolean ignoreAccessScope) {
      this(elementToSearch, scope, ignoreAccessScope, null);
    }

    @Override
    @Nonnull
    public Project getProject() {
      return myProject;
    }

    @Nonnull
    public PsiElement getElementToSearch() {
      return myElementToSearch;
    }

    /**
     * @return the user-visible search scope, most often "Project Files" or "Project and Libraries".
     * Searchers most likely need to use {@link #getEffectiveSearchScope()}.
     */
    public SearchScope getScopeDeterminedByUser() {
      return myScope;
    }


    /**
     * Same as {@link #getScopeDeterminedByUser()}. Use {@link #getEffectiveSearchScope} instead
     */
    @Deprecated
    @Nonnull
    public SearchScope getScope() {
      return myScope;
    }

    public boolean isIgnoreAccessScope() {
      return myIgnoreAccessScope;
    }

    @Nonnull
    public SearchRequestCollector getOptimizer() {
      return myOptimizer;
    }

    @Nonnull
    public SearchScope getEffectiveSearchScope () {
      if (myIgnoreAccessScope) {
        return myScope;
      }
      SearchScope accessScope = PsiSearchHelper.SERVICE.getInstance(myElementToSearch.getProject()).getUseScope(myElementToSearch);
      return myScope.intersectWith(accessScope);
    }
  }

  /**
   * Searches for references to the specified element in the scope in which such references are expected to be found, according to
   * dependencies and access rules.
   *
   * @param element the element (declaration) the references to which are requested.
   * @return the query allowing to enumerate the references.
   */
  @Nonnull
  public static Query<PsiReference> search(@Nonnull PsiElement element) {
    return search(element, GlobalSearchScope.allScope(PsiUtilCore.getProjectInReadAction(element)), false);
  }

  /**
   * Searches for references to the specified element in the specified scope.
   *
   * @param element the element (declaration) the references to which are requested.
   * @param searchScope the scope in which the search is performed.
   * @return the query allowing to enumerate the references.
   */
  @Nonnull
  public static Query<PsiReference> search(@Nonnull PsiElement element, @Nonnull SearchScope searchScope) {
    return search(element, searchScope, false);
  }

  /**
   * Searches for references to the specified element in the specified scope, optionally returning also references which
   * are invalid because of access rules (e.g. references to a private method from a different class).
   *
   * @param element the element (declaration) the references to which are requested.
   * @param searchScope the scope in which the search is performed.
   * @param ignoreAccessScope if true, references which are invalid because of access rules are included in the results.
   * @return the query allowing to enumerate the references.
   */
  @Nonnull
  public static Query<PsiReference> search(@Nonnull PsiElement element, @Nonnull SearchScope searchScope, boolean ignoreAccessScope) {
    return search(new SearchParameters(element, searchScope, ignoreAccessScope));
  }

  /**
   * Searches for references to the specified element according to the specified parameters.
   *
   * @param parameters the parameters for the search (contain also the element the references to which are requested).
   * @return the query allowing to enumerate the references.
   */
  @Nonnull
  public static Query<PsiReference> search(@Nonnull final SearchParameters parameters) {
    final Query<PsiReference> result = INSTANCE.createQuery(parameters);
    if (parameters.isSharedOptimizer) {
      return uniqueResults(result);
    }

    final SearchRequestCollector requests = parameters.getOptimizer();

    final PsiElement element = parameters.getElementToSearch();

    return uniqueResults(new MergeQuery<PsiReference>(result, new SearchRequestQuery(PsiUtilCore.getProjectInReadAction(element), requests)));
  }

  @Nonnull
  private static Query<PsiReference> uniqueResults(@Nonnull Query<PsiReference> composite) {
    return new UniqueResultsQuery<PsiReference, ReferenceDescriptor>(composite, ContainerUtil.<ReferenceDescriptor>canonicalStrategy(), ReferenceDescriptor.MAPPER);
  }

  public static void searchOptimized(@Nonnull PsiElement element,
                                     @Nonnull SearchScope searchScope,
                                     boolean ignoreAccessScope,
                                     @Nonnull SearchRequestCollector collector,
                                     @Nonnull final Processor<PsiReference> processor) {
    searchOptimized(element, searchScope, ignoreAccessScope, collector, false, new PairProcessor<PsiReference, SearchRequestCollector>() {
      @Override
      public boolean process(PsiReference psiReference, SearchRequestCollector collector) {
        return processor.process(psiReference);
      }
    });
  }

  public static void searchOptimized(@Nonnull PsiElement element,
                                     @Nonnull SearchScope searchScope,
                                     boolean ignoreAccessScope,
                                     @Nonnull SearchRequestCollector collector,
                                     final boolean inReadAction,
                                     @Nonnull PairProcessor<PsiReference, SearchRequestCollector> processor) {
    final SearchRequestCollector nested = new SearchRequestCollector(collector.getSearchSession());
    Query<PsiReference> query = search(new SearchParameters(element, searchScope, ignoreAccessScope, nested));
    collector.searchQuery(new QuerySearchRequest(query, nested, inReadAction, processor));
  }
}
