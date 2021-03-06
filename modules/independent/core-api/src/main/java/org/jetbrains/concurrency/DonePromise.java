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
package org.jetbrains.concurrency;

import com.intellij.openapi.util.Getter;
import com.intellij.util.Consumer;
import com.intellij.util.Function;
import consulo.concurrency.Promises;
import javax.annotation.Nonnull;

@Deprecated
public class DonePromise<T> extends Promise<T> implements Getter<T> {
  private final T result;

  public DonePromise(T result) {
    this.result = result;
  }

  @Nonnull
  @Override
  public Promise<T> done(@Nonnull Consumer<T> done) {
    if (!AsyncPromise.isObsolete(done)) {
      done.consume(result);
    }
    return this;
  }

  @Nonnull
  @Override
  public Promise<T> processed(@Nonnull AsyncPromise<T> fulfilled) {
    fulfilled.setResult(result);
    return this;
  }

  @Override
  public Promise<T> processed(@Nonnull Consumer<T> processed) {
    done(processed);
    return this;
  }

  @Nonnull
  @Override
  public Promise<T> rejected(@Nonnull Consumer<Throwable> rejected) {
    return this;
  }

  @Nonnull
  @Override
  public <SUB_RESULT> Promise<SUB_RESULT> then(@Nonnull Function<T, SUB_RESULT> done) {
    if (done instanceof Obsolescent && ((Obsolescent)done).isObsolete()) {
      return Promises.reject("obsolete");
    }
    else {
      return Promises.resolve(done.fun(result));
    }
  }

  @Nonnull
  @Override
  public State getState() {
    return State.FULFILLED;
  }

  @Override
  public T get() {
    return result;
  }

  @Override
  public void notify(@Nonnull AsyncPromise<T> child) {
    child.setResult(result);
  }
}