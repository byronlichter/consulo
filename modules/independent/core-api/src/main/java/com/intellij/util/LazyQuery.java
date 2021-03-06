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

import com.intellij.openapi.util.NotNullLazyValue;
import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author peter
 */
public abstract class LazyQuery<T> implements Query<T> {
  private final NotNullLazyValue<Query<T>> myQuery = new NotNullLazyValue<Query<T>>() {
    @Override
    @Nonnull
    protected Query<T> compute() {
      return computeQuery();
    }
  };

  @Nonnull
  protected abstract Query<T> computeQuery();

  @Override
  @Nonnull
  public Collection<T> findAll() {
    return myQuery.getValue().findAll();
  }

  @Override
  public T findFirst() {
    return myQuery.getValue().findFirst();
  }

  @Override
  public boolean forEach(@Nonnull final Processor<T> consumer) {
    return myQuery.getValue().forEach(consumer);
  }

  @Nonnull
  @Override
  public T[] toArray(@Nonnull final T[] a) {
    return myQuery.getValue().toArray(a);
  }

  @Override
  public Iterator<T> iterator() {
    return myQuery.getValue().iterator();
  }
}
