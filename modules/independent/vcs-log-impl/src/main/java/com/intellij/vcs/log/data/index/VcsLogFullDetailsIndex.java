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
package com.intellij.vcs.log.data.index;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.Consumer;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.impl.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorIntegerDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.vcs.log.VcsFullCommitDetails;
import com.intellij.vcs.log.impl.FatalErrorHandler;
import com.intellij.vcs.log.util.PersistentUtil;
import gnu.trove.TIntHashSet;
import javax.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.ObjIntConsumer;

import static com.intellij.vcs.log.data.index.VcsLogPersistentIndex.getVersion;

public class VcsLogFullDetailsIndex<T> implements Disposable {
  protected static final String INDEX = "index";
  @Nonnull
  protected final MyMapReduceIndex myMapReduceIndex;
  @Nonnull
  private final ID<Integer, T> myID;
  @Nonnull
  private final String myLogId;
  @Nonnull
  private final String myName;
  @Nonnull
  protected final DataIndexer<Integer, T, VcsFullCommitDetails> myIndexer;
  @Nonnull
  private final FatalErrorHandler myFatalErrorHandler;

  public VcsLogFullDetailsIndex(@Nonnull String logId,
                                @Nonnull String name,
                                final int version,
                                @Nonnull DataIndexer<Integer, T, VcsFullCommitDetails> indexer,
                                @Nonnull DataExternalizer<T> externalizer,
                                @Nonnull FatalErrorHandler fatalErrorHandler,
                                @Nonnull Disposable disposableParent)
          throws IOException {
    myID = ID.create(name);
    myName = name;
    myLogId = logId;
    myIndexer = indexer;
    myFatalErrorHandler = fatalErrorHandler;

    myMapReduceIndex = new MyMapReduceIndex(myIndexer, externalizer, version);

    Disposer.register(disposableParent, this);
  }

  @Nonnull
  public TIntHashSet getCommitsWithAnyKey(@Nonnull Set<Integer> keys) throws StorageException {
    TIntHashSet result = new TIntHashSet();

    for (Integer key : keys) {
      iterateCommitIds(key, result::add);
    }

    return result;
  }

  @Nonnull
  public TIntHashSet getCommitsWithAllKeys(@Nonnull Collection<Integer> keys) throws StorageException {
    return InvertedIndexUtil.collectInputIdsContainingAllKeys(myMapReduceIndex, keys, (k) -> {
      ProgressManager.checkCanceled();
      return true;
    }, null, null);
  }

  private void iterateCommitIds(int key, @Nonnull Consumer<Integer> consumer) throws StorageException {
    ValueContainer<T> data = myMapReduceIndex.getData(key);
    data.forEach((id, value) -> {
      consumer.consume(id);
      return true;
    });
  }

  protected void iterateCommitIdsAndValues(int key, @Nonnull ObjIntConsumer<T> consumer) throws StorageException {
    myMapReduceIndex.getData(key).forEach((id, value) -> {
      consumer.accept(value, id);
      return true;
    });
  }

  public void update(int commitId, @Nonnull VcsFullCommitDetails details) throws IOException {
    myMapReduceIndex.update(commitId, details).compute();
  }

  public void flush() throws StorageException {
    myMapReduceIndex.flush();
  }

  @Override
  public void dispose() {
    myMapReduceIndex.dispose();
  }

  @Nonnull
  public static File getStorageFile(@Nonnull String kind, @Nonnull String id) {
    return PersistentUtil.getStorageFile(INDEX, kind, id, getVersion(), false);
  }

  private class MyMapReduceIndex extends MapReduceIndex<Integer, T, VcsFullCommitDetails> {
    public MyMapReduceIndex(@Nonnull DataIndexer<Integer, T, VcsFullCommitDetails> indexer,
                            @Nonnull DataExternalizer<T> externalizer,
                            int version) throws IOException {
      super(new MyIndexExtension(indexer, externalizer, version),
            new MapIndexStorage<Integer, T>(getStorageFile(myName, myLogId),
                                            EnumeratorIntegerDescriptor.INSTANCE,
                                            externalizer, 5000, false) {
              @Override
              protected void checkCanceled() {
                ProgressManager.checkCanceled();
              }
            }, new EmptyForwardIndex<>());
    }

    @Override
    public void checkCanceled() {
      ProgressManager.checkCanceled();
    }

    @Override
    public void requestRebuild(@Nonnull Exception ex) {
      myFatalErrorHandler.consume(this, ex);
    }
  }

  private class MyIndexExtension extends IndexExtension<Integer, T, VcsFullCommitDetails> {
    @Nonnull
    private final DataIndexer<Integer, T, VcsFullCommitDetails> myIndexer;
    @Nonnull
    private final DataExternalizer<T> myExternalizer;
    private final int myVersion;

    public MyIndexExtension(@Nonnull DataIndexer<Integer, T, VcsFullCommitDetails> indexer,
                            @Nonnull DataExternalizer<T> externalizer,
                            int version) {
      myIndexer = indexer;
      myExternalizer = externalizer;
      myVersion = version;
    }

    @Nonnull
    @Override
    public ID<Integer, T> getName() {
      return myID;
    }

    @Nonnull
    @Override
    public DataIndexer<Integer, T, VcsFullCommitDetails> getIndexer() {
      return myIndexer;
    }

    @Nonnull
    @Override
    public KeyDescriptor<Integer> getKeyDescriptor() {
      return EnumeratorIntegerDescriptor.INSTANCE;
    }

    @Nonnull
    @Override
    public DataExternalizer<T> getValueExternalizer() {
      return myExternalizer;
    }

    @Override
    public int getVersion() {
      return myVersion;
    }
  }

  private static class EmptyForwardIndex<T> implements ForwardIndex<Integer, T> {
    @Nonnull
    @Override
    public InputDataDiffBuilder<Integer, T> getDiffBuilder(int inputId) {
      return new EmptyInputDataDiffBuilder<>(inputId);
    }

    @Override
    public void putInputData(int inputId, @Nonnull Map<Integer, T> data) throws IOException {
    }

    @Override
    public void flush() {
    }

    @Override
    public void clear() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }
  }
}
