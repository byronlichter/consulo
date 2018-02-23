/*
 * Copyright 2013-2016 consulo.io
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
package consulo.diagram.builder.impl;

import consulo.diagram.builder.GraphNode;
import consulo.diagram.builder.GraphPositionStrategy;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 22:44/15.10.13
 */
public class GraphNodeImpl<E> implements GraphNode<E> {
  private final List<GraphNode<?>> myArrowNodes = new ArrayList<GraphNode<?>>();
  private final E myValue;
  private final GraphPositionStrategy myStrategy;

  public GraphNodeImpl(E value, GraphPositionStrategy strategy) {
    myValue = value;
    myStrategy = strategy;
  }

  @Override
  public void makeArrow(@Nonnull GraphNode<?> target) {
    myArrowNodes.add(target);
  }

  @Override
  public E getValue() {
    return myValue;
  }

  @Override
  public GraphPositionStrategy getStrategy() {
    return myStrategy;
  }

  @Override
  @Nonnull
  public List<GraphNode<?>> getArrowNodes() {
    return myArrowNodes;
  }
}
