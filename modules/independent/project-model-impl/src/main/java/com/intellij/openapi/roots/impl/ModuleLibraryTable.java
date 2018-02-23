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

package com.intellij.openapi.roots.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.impl.libraries.LibraryTableBase;
import com.intellij.openapi.roots.impl.libraries.LibraryTableImplUtil;
import com.intellij.openapi.roots.libraries.*;
import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.ConvertingIterator;
import com.intellij.util.containers.Convertor;
import com.intellij.util.containers.FilteringIterator;
import consulo.roots.impl.ModuleRootLayerImpl;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Iterator;

/**
 *  @author dsl
 */
public class ModuleLibraryTable implements LibraryTable, LibraryTableBase.ModifiableModelEx {
  private static final ModuleLibraryOrderEntryCondition MODULE_LIBRARY_ORDER_ENTRY_FILTER = new ModuleLibraryOrderEntryCondition();
  private static final OrderEntryToLibraryConvertor ORDER_ENTRY_TO_LIBRARY_CONVERTOR = new OrderEntryToLibraryConvertor();
  private final ModuleRootLayerImpl myRootLayer;
  public static final LibraryTablePresentation MODULE_LIBRARY_TABLE_PRESENTATION = new LibraryTablePresentation() {
    @Override
    public String getDisplayName(boolean plural) {
      return ProjectBundle.message("module.library.display.name", plural ? 2 : 1);
    }

    @Override
    public String getDescription() {
      return ProjectBundle.message("libraries.node.text.module");
    }

    @Override
    public String getLibraryTableEditorTitle() {
      return ProjectBundle.message("library.configure.module.title");
    }
  };

  public ModuleLibraryTable(ModuleRootLayerImpl rootLayer) {
    myRootLayer = rootLayer;
  }

  @Override
  @Nonnull
  public Library[] getLibraries() {
    final ArrayList<Library> result = new ArrayList<Library>();
    final Iterator<Library> libraryIterator = getLibraryIterator();
    ContainerUtil.addAll(result, libraryIterator);
    return result.toArray(new Library[result.size()]);
  }

  @Override
  public Library createLibrary() {
    return createLibrary(null);
  }

  @Override
  public Library createLibrary(String name) {
    return createLibrary(name, null);
  }

  @Override
  public Library createLibrary(String name, @javax.annotation.Nullable PersistentLibraryKind kind) {
    final ModuleLibraryOrderEntryImpl orderEntry = new ModuleLibraryOrderEntryImpl(name, kind, myRootLayer);
    myRootLayer.addOrderEntry(orderEntry);
    return orderEntry.getLibrary();
  }

  @Override
  public void removeLibrary(@Nonnull Library library) {
    final Iterator<OrderEntry> orderIterator = myRootLayer.getOrderIterator();
    while (orderIterator.hasNext()) {
      OrderEntry orderEntry = orderIterator.next();
      if (orderEntry instanceof LibraryOrderEntry) {
        final LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry) orderEntry;
        if (libraryOrderEntry.isModuleLevel()) {
          if (library.equals(libraryOrderEntry.getLibrary())) {
            myRootLayer.removeOrderEntry(orderEntry);
            //Disposer.dispose(library);
            return;
          }
        }
      }
    }
  }

  @Override
  @Nonnull
  public Iterator<Library> getLibraryIterator() {
    FilteringIterator<OrderEntry, LibraryOrderEntry> filteringIterator =
      new FilteringIterator<OrderEntry, LibraryOrderEntry>(myRootLayer.getOrderIterator(), MODULE_LIBRARY_ORDER_ENTRY_FILTER);
    return new ConvertingIterator<LibraryOrderEntry, Library>(filteringIterator, ORDER_ENTRY_TO_LIBRARY_CONVERTOR);
  }

  @Override
  public String getTableLevel() {
    return LibraryTableImplUtil.MODULE_LEVEL;
  }

  @Override
  public LibraryTablePresentation getPresentation() {
    return MODULE_LIBRARY_TABLE_PRESENTATION;
  }

  @Override
  public boolean isEditable() {
    return true;
  }

  @Override
  @javax.annotation.Nullable
  public Library getLibraryByName(@Nonnull String name) {
    final Iterator<Library> libraryIterator = getLibraryIterator();
    while (libraryIterator.hasNext()) {
      Library library = libraryIterator.next();
      if (name.equals(library.getName())) return library;
    }
    return null;
  }

  @Override
  public void addListener(Listener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addListener(Listener listener, Disposable parentDisposable) {
    throw new UnsupportedOperationException("Method addListener is not yet implemented in " + getClass().getName());
  }

  @Override
  public void removeListener(Listener listener) {
    throw new UnsupportedOperationException();
  }

  public Module getModule() {
    return myRootLayer.getModule();
  }


  private static class ModuleLibraryOrderEntryCondition implements Condition<OrderEntry> {
    @Override
    public boolean value(OrderEntry entry) {
      return entry instanceof LibraryOrderEntry && ((LibraryOrderEntry)entry).isModuleLevel() && ((LibraryOrderEntry)entry).getLibrary() != null;
    }
  }

  private static class OrderEntryToLibraryConvertor implements Convertor<LibraryOrderEntry, Library> {
    @Override
    public Library convert(LibraryOrderEntry o) {
      return o.getLibrary();
    }
  }

  @Override
  public void commit() {
  }

  @Override
  public boolean isChanged() {
    return myRootLayer.getRootModel().isChanged();
  }

  @Override
  public ModifiableModel getModifiableModel() {
    return this;
  }
}
