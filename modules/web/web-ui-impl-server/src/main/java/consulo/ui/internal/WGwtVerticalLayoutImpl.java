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
package consulo.ui.internal;

import com.vaadin.ui.AbstractLayout;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.shared.Size;
import consulo.ui.VerticalLayout;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class WGwtVerticalLayoutImpl extends AbstractLayout implements VerticalLayout, VaadinWrapper {
  private final List<com.vaadin.ui.Component> myChildren = new LinkedList<>();

  @Nonnull
  @Override
  @RequiredUIAccess
  public VerticalLayout add(@Nonnull Component component) {
    addComponent((com.vaadin.ui.Component)component);
    myChildren.add((com.vaadin.ui.Component)component);

    markAsDirtyRecursive();
    return this;
  }

  @Override
  public void removeComponent(com.vaadin.ui.Component c) {
    myChildren.remove(c);
    super.removeComponent(c);
  }

  @Override
  public void replaceComponent(com.vaadin.ui.Component component, com.vaadin.ui.Component newComponent) {
    removeComponent(component);

    add((Component)newComponent);
  }

  @Override
  public int getComponentCount() {
    return myChildren.size();
  }

  @Override
  public Iterator<com.vaadin.ui.Component> iterator() {
    return myChildren.iterator();
  }

  @Nullable
  @Override
  public Component getParentComponent() {
    return (Component)getParent();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@Nonnull Size size) {

  }
}
