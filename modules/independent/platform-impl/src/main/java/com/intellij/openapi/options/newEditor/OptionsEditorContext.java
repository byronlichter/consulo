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
package com.intellij.openapi.options.newEditor;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.MultiValuesMap;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.ui.speedSearch.ElementFilter;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;

class OptionsEditorContext {
  ElementFilter.Active myFilter;

  CopyOnWriteArraySet<OptionsEditorColleague> myColleagues = new CopyOnWriteArraySet<>();

  Configurable myCurrentConfigurable;
  Set<Configurable> myModified = new CopyOnWriteArraySet<>();
  Map<Configurable, ConfigurationException> myErrors = new HashMap<>();
  private boolean myHoldingFilter;
  private final Map<Configurable,  Configurable> myConfigurableToParentMap = new HashMap<>();
  private final MultiValuesMap<Configurable, Configurable> myParentToChildrenMap = new MultiValuesMap<>();


  public OptionsEditorContext(ElementFilter.Active filter) {
    myFilter = filter;
  }

  ActionCallback fireSelected(@Nullable final Configurable configurable, @Nonnull OptionsEditorColleague requestor) {
    if (myCurrentConfigurable == configurable) return new ActionCallback.Rejected();

    final Configurable old = myCurrentConfigurable;
    myCurrentConfigurable = configurable;

    return notify(colleague -> colleague.onSelected(configurable, old), requestor);
  }

  ActionCallback fireModifiedAdded(@Nonnull final Configurable configurable, @Nullable OptionsEditorColleague requestor) {
    if (myModified.contains(configurable)) return new ActionCallback.Rejected();

    myModified.add(configurable);

    return notify(colleague -> colleague.onModifiedAdded(configurable), requestor);
  }

  ActionCallback fireModifiedRemoved(@Nonnull final Configurable configurable, @Nullable OptionsEditorColleague requestor) {
    if (!myModified.contains(configurable)) return new ActionCallback.Rejected();

    myModified.remove(configurable);

    return notify(colleague -> colleague.onModifiedRemoved(configurable), requestor);
  }

  ActionCallback fireErrorsChanged(final Map<Configurable, ConfigurationException> errors, OptionsEditorColleague requestor) {
    if (myErrors.equals(errors)) return new ActionCallback.Rejected();

    myErrors = errors != null ? errors : new HashMap<>();

    return notify(OptionsEditorColleague::onErrorsChanged, requestor);
  }

  ActionCallback notify(Function<OptionsEditorColleague, ActionCallback> action, OptionsEditorColleague requestor) {
    final ActionCallback.Chunk chunk = new ActionCallback.Chunk();
    for (OptionsEditorColleague each : myColleagues) {
      if (each != requestor) {
        chunk.add(action.apply(each));
      }
    }

    return chunk.getWhenProcessed();
  }

  public void fireReset(final Configurable configurable) {
    if (myModified.contains(configurable)) {
      fireModifiedRemoved(configurable, null);
    }

    if (myErrors.containsKey(configurable)) {
      final HashMap<Configurable, ConfigurationException> newErrors = new HashMap<>();
      newErrors.remove(configurable);
      fireErrorsChanged(newErrors, null);
    }
  }

  public boolean isModified(final Configurable configurable) {
    return myModified.contains(configurable);
  }

  public void setHoldingFilter(final boolean holding) {
    myHoldingFilter = holding;
  }

  public boolean isHoldingFilter() {
    return myHoldingFilter;
  }

  public Configurable getParentConfigurable(final Configurable configurable) {
    return myConfigurableToParentMap.get(configurable);
  }

  public void registerKid(final Configurable parent, final Configurable kid) {
    myConfigurableToParentMap.put(kid,parent);
    myParentToChildrenMap.put(parent, kid);
  }

  public Collection<Configurable> getChildren(final Configurable parent) {
    Collection<Configurable> result = myParentToChildrenMap.get(parent);
    return result == null ? Collections.<Configurable>emptySet() : result;
  }

  @Nonnull
  ElementFilter<Configurable> getFilter() {
    return myFilter;
  }

  public Configurable getCurrentConfigurable() {
    return myCurrentConfigurable;
  }

  public Set<Configurable> getModified() {
    return myModified;
  }

  public Map<Configurable, ConfigurationException> getErrors() {
    return myErrors;
  }

  public void addColleague(final OptionsEditorColleague colleague) {
    myColleagues.add(colleague);
  }
}
