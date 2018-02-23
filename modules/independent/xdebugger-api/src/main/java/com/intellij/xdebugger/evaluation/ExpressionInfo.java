/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.xdebugger.evaluation;

import com.intellij.openapi.util.TextRange;
import javax.annotation.Nonnull;

public class ExpressionInfo {
  private final TextRange textRange;
  private final String expressionText;
  private final String displayText;

  public ExpressionInfo(@Nonnull TextRange textRange) {
    this(textRange, null);
  }

  public ExpressionInfo(@Nonnull TextRange textRange, @javax.annotation.Nullable String expressionText) {
    this(textRange, expressionText, expressionText);
  }

  public ExpressionInfo(@Nonnull TextRange textRange, @javax.annotation.Nullable String expressionText, @javax.annotation.Nullable String displayText) {
    this.textRange = textRange;
    this.expressionText = expressionText;
    this.displayText = displayText;
  }

  /**
   * Text range to highlight as link,
   * will be used to compute evaluation and display text if these values not specified.
   */
  @Nonnull
  public TextRange getTextRange() {
    return textRange;
  }

  /**
   * Expression to evaluate
   */
  @javax.annotation.Nullable
  public String getExpressionText() {
    return expressionText;
  }

  @javax.annotation.Nullable
  public String getDisplayText() {
    return displayText;
  }
}