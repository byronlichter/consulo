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

package com.intellij.util;

import com.intellij.concurrency.AsyncFuture;
import com.intellij.concurrency.AsyncFutureFactory;
import com.intellij.concurrency.AsyncFutureResult;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author max
 */
public class CollectionQuery<T> implements Query<T> {
  private final Collection<T> myCollection;

  public CollectionQuery(@Nonnull final Collection<T> collection) {
    myCollection = collection;
  }

  @Override
  @Nonnull
  public Collection<T> findAll() {
    return myCollection;
  }

  @Override
  public T findFirst() {
    final Iterator<T> i = iterator();
    return i.hasNext() ? i.next() : null;
  }

  @Override
  public boolean forEach(@Nonnull final Processor<T> consumer) {
    return ContainerUtil.process(myCollection, consumer);
  }

  @Nonnull
  @Override
  public AsyncFuture<Boolean> forEachAsync(@Nonnull Processor<T> consumer) {
    AsyncFutureResult<Boolean>  result = AsyncFutureFactory.getInstance().createAsyncFutureResult();
    try {
      result.set(forEach(consumer));
    } catch (Throwable t) {
      result.setException(t);
    }
    return result;
  }

  @Nonnull
  @Override
  public T[] toArray(@Nonnull final T[] a) {
    return findAll().toArray(a);
  }

  @Override
  public Iterator<T> iterator() {
    return myCollection.iterator();
  }
}
