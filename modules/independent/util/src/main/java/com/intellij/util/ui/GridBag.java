/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.util.ui;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.awt.*;

/**
 * Usage:
 * <pre>
 * {@code
 *
 * // First, configure default for every or a specific column:
 * GridBag bag = new GridBag()
 *     .setDefaultAnchor(0, GridBagConstraints.EAST)
 *     .setDefaultAnchor(1, GridBagConstraints.WEST)
 *     .setDefaultWeightX(1, 1)
 *     .setDefaultFill(GridBagConstraints.HORIZONTAL);
 *
 * // Then, add components to a panel:
 *
 * // The following code adds a new line with 2 components with default settings:
 * panel.add(c1, bag.nextLine().next())
 * panel.add(c1, bag.next())
 *
 * // The following code adds a component on the next line that covers all remaining columns:
 * panel.add(c1, bag.nextLine().coverLine())
 *
 * // The following code adds a component on the next line with overridden settings:
 * panel.add(c1, bag.nextLine().next().insets(...).weightx(...))
 *
 * // You also can pre-configure the object and pass it as a constraint:
 * bag.nextLine().next();
 * panel.add(c1, bag)
 * }
 * </pre>
 * Note that every call of {@link #nextLine()} or {@link #next()} resets settings to the defaults for the corresponding column.
 */
@SuppressWarnings({"JavaDoc"})
public class GridBag extends GridBagConstraints {
  private int myDefaultAnchor = anchor;
  @Nonnull
  private final TIntIntHashMap myDefaultColumnAnchors = new TIntIntHashMap();

  private int myDefaultFill = fill;
  @Nonnull
  private final TIntIntHashMap myDefaultColumnFills = new TIntIntHashMap();

  private double myDefaultWeightX = weightx;
  @Nonnull
  private final TIntDoubleHashMap myDefaultColumnWeightsX = new TIntDoubleHashMap();
  private double myDefaultWeightY = weighty;
  @Nonnull
  private final TIntDoubleHashMap myDefaultColumnWeightsY = new TIntDoubleHashMap();

  private int myDefaultPaddingX = ipadx;
  @Nonnull
  private final TIntIntHashMap myDefaultColumnPaddingsX = new TIntIntHashMap();
  private int myDefaultPaddingY = ipady;
  @Nonnull
  private final TIntIntHashMap myDefaultColumnPaddingsY = new TIntIntHashMap();

  @Nullable
  private Insets myDefaultInsets = insets;
  @Nonnull
  private final TIntObjectHashMap<Insets> myDefaultColumnInsets = new TIntObjectHashMap<Insets>();

  public GridBag() {
    gridx = gridy = -1;
  }

  @Nonnull
  public GridBag nextLine() {
    gridy++;
    gridx = -1;
    return reset();
  }

  @Nonnull
  public GridBag next() {
    gridx++;
    return reset();
  }

  public int getLine() {
    return gridy;
  }

  @Nonnull
  public GridBag setLine(int line) {
    gridy = line;
    return this;
  }

  public int getColumn() {
    return gridx;
  }

  @Nonnull
  public GridBag setColumn(int cell) {
    gridx = cell;
    return this;
  }

  @Nonnull
  public GridBag reset() {
    gridwidth = gridheight = 1;

    int column = gridx;

    anchor(getDefaultAnchor(column));
    fill = getDefaultFill(column);
    weightx(getDefaultWeightX(column));
    weighty(getDefaultWeightY(column));
    padx(getDefaultPaddingX(column));
    pady(getDefaultPaddingY(column));
    insets(getDefaultInsets(column));
    return this;
  }

  @Nonnull
  public GridBag anchor(int anchor) {
    this.anchor = anchor;
    return this;
  }

  @Nonnull
  public GridBag fillCell() {
    fill = GridBagConstraints.BOTH;
    return this;
  }

  @Nonnull
  public GridBag fillCellHorizontally() {
    fill = GridBagConstraints.HORIZONTAL;
    return this;
  }

  @Nonnull
  public GridBag fillCellVertically() {
    fill = GridBagConstraints.VERTICAL;
    return this;
  }

  public GridBag fillCellNone() {
    fill = GridBagConstraints.NONE;
    return this;
  }

  @Nonnull
  public GridBag weightx(double weight) {
    weightx = weight;
    return this;
  }


  @Nonnull
  public GridBag weighty(double weight) {
    weighty = weight;
    return this;
  }

  @Nonnull
  public GridBag coverLine() {
    gridwidth = GridBagConstraints.REMAINDER;
    return this;
  }

  @Nonnull
  public GridBag coverLine(int cells) {
    gridwidth = cells;
    return this;
  }

  @Nonnull
  public GridBag coverColumn() {
    gridheight = GridBagConstraints.REMAINDER;
    return this;
  }

  @Nonnull
  public GridBag coverColumn(int cells) {
    gridheight = cells;
    return this;
  }

  @Nonnull
  public GridBag padx(int padding) {
    ipadx = padding;
    return this;
  }

  @Nonnull
  public GridBag pady(int padding) {
    ipady = padding;
    return this;
  }


  /**
   * @see #insets(java.awt.Insets)
   */
  @Nonnull
  public GridBag insets(int top, int left, int bottom, int right) {
    return insets(new Insets(top, left, bottom, right));
  }

  /**
   * Pass -1 to use a default value for this column.
   * E.g, Insets(10, -1, -1, -1) means that 'top' will be changed to 10 and other sides will be set to defaults for this column.
   */
  @Nonnull
  public GridBag insets(@Nullable Insets insets) {
    if (insets != null && (insets.top < 0 || insets.bottom < 0 || insets.left < 0 || insets.right < 0)) {
      Insets def = getDefaultInsets(gridx);
      insets = (Insets)insets.clone();
      if (insets.top < 0) insets.top = def == null ? 0 : def.top;
      if (insets.left < 0) insets.left = def == null ? 0 : def.left;
      if (insets.bottom < 0) insets.bottom = def == null ? 0 : def.bottom;
      if (insets.right < 0) insets.right = def == null ? 0 : def.right;
    }
    this.insets = insets;
    return this;
  }

  public int getDefaultAnchor() {
    return myDefaultAnchor;
  }

  @Nonnull
  public GridBag setDefaultAnchor(int anchor) {
    myDefaultAnchor = anchor;
    return this;
  }

  public int getDefaultAnchor(int column) {
    return myDefaultColumnAnchors.containsKey(column) ? myDefaultColumnAnchors.get(column) : getDefaultAnchor();
  }

  @Nonnull
  public GridBag setDefaultAnchor(int column, int anchor) {
    if (anchor == -1) {
      myDefaultColumnAnchors.remove(column);
    }
    else {
      myDefaultColumnAnchors.put(column, anchor);
    }
    return this;
  }

  public int getDefaultFill() {
    return myDefaultFill;
  }

  @Nonnull
  public GridBag setDefaultFill(int fill) {
    myDefaultFill = fill;
    return this;
  }

  public int getDefaultFill(int column) {
    return myDefaultColumnFills.containsKey(column) ? myDefaultColumnFills.get(column) : getDefaultFill();
  }

  @Nonnull
  public GridBag setDefaultFill(int column, int fill) {
    if (fill == -1) {
      myDefaultColumnFills.remove(column);
    }
    else {
      myDefaultColumnFills.put(column, fill);
    }
    return this;
  }

  public double getDefaultWeightX() {
    return myDefaultWeightX;
  }

  @Nonnull
  public GridBag setDefaultWeightX(double weight) {
    myDefaultWeightX = weight;
    return this;
  }

  public double getDefaultWeightX(int column) {
    return myDefaultColumnWeightsX.containsKey(column) ? myDefaultColumnWeightsX.get(column) : getDefaultWeightX();
  }

  @Nonnull
  public GridBag setDefaultWeightX(int column, double weight) {
    if (weight == -1) {
      myDefaultColumnWeightsX.remove(column);
    }
    else {
      myDefaultColumnWeightsX.put(column, weight);
    }
    return this;
  }


  public double getDefaultWeightY() {
    return myDefaultWeightY;
  }

  @Nonnull
  public GridBag setDefaultWeightY(double weight) {
    myDefaultWeightY = weight;
    return this;
  }

  public double getDefaultWeightY(int column) {
    return myDefaultColumnWeightsY.containsKey(column) ? myDefaultColumnWeightsY.get(column) : getDefaultWeightY();
  }

  @Nonnull
  public GridBag setDefaultWeightY(int column, double weight) {
    if (weight == -1) {
      myDefaultColumnWeightsY.remove(column);
    }
    else {
      myDefaultColumnWeightsY.put(column, weight);
    }
    return this;
  }


  public int getDefaultPaddingX() {
    return myDefaultPaddingX;
  }

  @Nonnull
  public GridBag setDefaultPaddingX(int padding) {
    myDefaultPaddingX = padding;
    return this;
  }

  public int getDefaultPaddingX(int column) {
    return myDefaultColumnPaddingsX.containsKey(column) ? myDefaultColumnPaddingsX.get(column) : getDefaultPaddingX();
  }

  @Nonnull
  public GridBag setDefaultPaddingX(int column, int padding) {
    if (padding == -1) {
      myDefaultColumnPaddingsX.remove(column);
    }
    else {
      myDefaultColumnPaddingsX.put(column, padding);
    }
    return this;
  }

  public int getDefaultPaddingY() {
    return myDefaultPaddingY;
  }

  @Nonnull
  public GridBag setDefaultPaddingY(int padding) {
    myDefaultPaddingY = padding;
    return this;
  }

  public int getDefaultPaddingY(int column) {
    return myDefaultColumnPaddingsY.containsKey(column) ? myDefaultColumnPaddingsY.get(column) : getDefaultPaddingY();
  }

  @Nonnull
  public GridBag setDefaultPaddingY(int column, int padding) {
    if (padding == -1) {
      myDefaultColumnPaddingsY.remove(column);
    }
    else {
      myDefaultColumnPaddingsY.put(column, padding);
    }
    return this;
  }

  @Nullable
  public Insets getDefaultInsets() {
    return myDefaultInsets;
  }

  @Nonnull
  public GridBag setDefaultInsets(int top, int left, int bottom, int right) {
    return setDefaultInsets(new Insets(top, left, bottom, right));
  }

  public GridBag setDefaultInsets(@Nullable Insets insets) {
    myDefaultInsets = insets;
    return this;
  }

  @Nullable
  public Insets getDefaultInsets(int column) {
    return myDefaultColumnInsets.containsKey(column) ? myDefaultColumnInsets.get(column) : getDefaultInsets();
  }

  @Nonnull
  public GridBag setDefaultInsets(int column, int top, int left, int bottom, int right) {
    return setDefaultInsets(column, new Insets(top, left, bottom, right));
  }

  @Nonnull
  public GridBag setDefaultInsets(int column, @Nullable Insets insets) {
    if (insets == null) {
      myDefaultColumnInsets.remove(column);
    }
    else {
      myDefaultColumnInsets.put(column, insets);
    }
    return this;
  }
}
