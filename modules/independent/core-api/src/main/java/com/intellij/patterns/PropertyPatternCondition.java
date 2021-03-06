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
package com.intellij.patterns;

import com.intellij.util.PairProcessor;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author peter
 */
public abstract class PropertyPatternCondition<T,P> extends PatternConditionPlus<T, P>{

  public PropertyPatternCondition(@NonNls String methodName, final ElementPattern propertyPattern) {
    super(methodName, propertyPattern);
  }

  @Override
  public boolean processValues(T t, ProcessingContext context, PairProcessor<P, ProcessingContext> processor) {
    return processor.process(getPropertyValue(t), context);
  }

  @Nullable
  public abstract P getPropertyValue(@Nonnull Object o);
}
