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
package com.intellij.util.indexing;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.Pair;
import gnu.trove.THashMap;
import gnu.trove.TObjectIntHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

class IndexConfiguration {
  private final Map<ID<?, ?>, Pair<UpdatableIndex<?, ?, FileContent>, FileBasedIndex.InputFilter>> myIndices =
          new THashMap<>();
  private final TObjectIntHashMap<ID<?, ?>> myIndexIdToVersionMap = new TObjectIntHashMap<>();
  private final List<ID<?, ?>> myIndicesWithoutFileTypeInfo = new ArrayList<>();
  private final Map<FileType, List<ID<?, ?>>> myFileType2IndicesWithFileTypeInfoMap = new THashMap<>();
  private volatile boolean myFreezed;

  <K, V> UpdatableIndex<K, V, FileContent> getIndex(ID<K, V> indexId) {
    assert myFreezed;
    final Pair<UpdatableIndex<?, ?, FileContent>, FileBasedIndex.InputFilter> pair = myIndices.get(indexId);

    assert pair != null : "Index data is absent for index " + indexId;

    //noinspection unchecked
    return (UpdatableIndex<K, V, FileContent>)pair.getFirst();
  }

  FileBasedIndex.InputFilter getInputFilter(@Nonnull ID<?, ?> indexId) {
    assert myFreezed;
    final Pair<UpdatableIndex<?, ?, FileContent>, FileBasedIndex.InputFilter> pair = myIndices.get(indexId);

    assert pair != null : "Index data is absent for index " + indexId;

    return pair.getSecond();
  }

  void freeze() {
    myFreezed = true;
  }

  <K, V> void registerIndex(ID<K, V> name,
                            UpdatableIndex<K, V, FileContent> index,
                            FileBasedIndex.InputFilter inputFilter,
                            int version,
                            @Nullable Collection<FileType> associatedFileTypes
  ) {
    assert !myFreezed;

    synchronized (myIndices) {
      myIndexIdToVersionMap.put(name, version);

      if (associatedFileTypes != null && !associatedFileTypes.isEmpty()) {
        for(FileType fileType:associatedFileTypes) {
          List<ID<?, ?>> ids = myFileType2IndicesWithFileTypeInfoMap.get(fileType);
          if (ids == null) myFileType2IndicesWithFileTypeInfoMap.put(fileType, ids = new ArrayList<>(5));
          ids.add(name);
        }
      } else {
        myIndicesWithoutFileTypeInfo.add(name);
      }

      myIndices.put(name, new Pair<>(index, inputFilter));
    }
  }

  List<ID<?, ?>> getFileTypesForIndex(FileType fileType) {
    assert myFreezed;
    List<ID<?, ?>> ids = myFileType2IndicesWithFileTypeInfoMap.get(fileType);
    if (ids == null) ids = myIndicesWithoutFileTypeInfo;
    return ids;
  }

  void finalizeFileTypeMappingForIndices() {
    assert !myFreezed;
    synchronized (myIndices) {
      for (List<ID<?, ?>> value : myFileType2IndicesWithFileTypeInfoMap.values()) {
        value.addAll(myIndicesWithoutFileTypeInfo);
      }
    }
  }

  Collection<ID<?, ?>> getIndexIDs() {
    assert myFreezed;
    return myIndices.keySet();
  }

  boolean hasIndex(ID<?, ?> name) {
    assert myFreezed;
    return myIndices.containsKey(name);
  }

  int getIndexVersion(ID<?, ?> id) {
    assert myFreezed;
    return myIndexIdToVersionMap.get(id);
  }
}
