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

package com.intellij.openapi.application;

import com.intellij.openapi.util.Computable;
import com.intellij.util.Processor;
import consulo.annotations.RequiredReadAction;
import javax.annotation.Nonnull;

/**
 * @author cdr
 */
public abstract class ReadActionProcessor<T> implements Processor<T> {
  @Override
  public boolean process(final T t) {
    return ApplicationManager.getApplication().runReadAction((Computable<Boolean>)() -> processInReadAction(t));
  }

  @RequiredReadAction
  public abstract boolean processInReadAction(T t);

  @Nonnull
  public static <T> Processor<T> wrapInReadAction(@Nonnull final Processor<T> processor) {
    return t -> ApplicationManager.getApplication().runReadAction((Computable<Boolean>)() -> processor.process(t));
  }
}
