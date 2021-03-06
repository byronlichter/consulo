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
package com.intellij.mock;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.BaseComponent;
import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusFactory;
import com.intellij.util.pico.DefaultPicoContainer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MockComponentManager extends UserDataHolderBase implements ComponentManager {
  private final MessageBus myMessageBus = MessageBusFactory.newMessageBus(this);
  private final MutablePicoContainer myPicoContainer;

  private final Map<Class, Object> myComponents = new HashMap<Class, Object>();

  public MockComponentManager(@Nullable PicoContainer parent, @Nonnull Disposable parentDisposable) {
    myPicoContainer = new DefaultPicoContainer(parent) {
      private final Set<Object> myDisposableComponents = ContainerUtil.newConcurrentSet();

      @Override
      @Nullable
      public Object getComponentInstance(final Object componentKey) {
        final Object o = super.getComponentInstance(componentKey);
        if (o instanceof Disposable && o != MockComponentManager.this) {
          if (myDisposableComponents.add(o))
            Disposer.register(MockComponentManager.this, (Disposable)o);
        }
        return o;
      }
    };
    myPicoContainer.registerComponentInstance(this);
    Disposer.register(parentDisposable, this);
  }

  @Override
  public BaseComponent getComponent(@Nonnull String name) {
    return null;
  }

  public <T> void registerService(@Nonnull Class<T> serviceInterface, @Nonnull Class<? extends T> serviceImplementation) {
    myPicoContainer.unregisterComponent(serviceInterface.getName());
    myPicoContainer.registerComponentImplementation(serviceInterface.getName(), serviceImplementation);
  }

  public <T> void registerService(@Nonnull Class<T> serviceImplementation) {
    registerService(serviceImplementation, serviceImplementation);
  }

  public <T> void registerService(@Nonnull Class<T> serviceInterface, @Nonnull T serviceImplementation) {
    myPicoContainer.registerComponentInstance(serviceInterface.getName(), serviceImplementation);
  }

  public <T> void addComponent(@Nonnull Class<T> interfaceClass, @Nonnull T instance) {
    myComponents.put(interfaceClass, instance);
  }

  @Override
  public <T> T getComponent(@Nonnull Class<T> interfaceClass) {
    final Object o = myPicoContainer.getComponentInstance(interfaceClass);
    return (T)(o != null ? o : myComponents.get(interfaceClass));
  }

  @Override
  public <T> T getComponent(@Nonnull Class<T> interfaceClass, T defaultImplementation) {
    return getComponent(interfaceClass);
  }

  @Override
  public boolean hasComponent(@Nonnull Class interfaceClass) {
    return false;
  }

  @Override
  @Nonnull
  public <T> T[] getComponents(@Nonnull Class<T> baseClass) {
    final List<?> list = myPicoContainer.getComponentInstancesOfType(baseClass);
    return list.toArray((T[])Array.newInstance(baseClass, 0));
  }

  @Override
  @Nonnull
  public MutablePicoContainer getPicoContainer() {
    return myPicoContainer;
  }

  @Nonnull
  @Override
  public MessageBus getMessageBus() {
    return myMessageBus;
  }

  @Override
  public boolean isDisposed() {
    return false;
  }

  @Override
  public void dispose() {
  }

  @Nonnull
  @Override
  public <T> T[] getExtensions(@Nonnull final ExtensionPointName<T> extensionPointName) {
    throw new UnsupportedOperationException("getExtensions()");
  }

  @Nonnull
  @Override
  public Condition getDisposed() {
    return Conditions.alwaysFalse();
  }
}
