/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.diagnostic;

import com.intellij.util.ArrayUtil;
import com.intellij.util.ExceptionUtil;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.lang.reflect.Constructor;

public abstract class Logger {
  public interface Factory {
    Logger getLoggerInstance(String category);
  }

  private static class DefaultFactory implements Factory {
    @Override
    public Logger getLoggerInstance(String category) {
      return new DefaultLogger(category);
    }
  }

  private static Factory ourFactory = new DefaultFactory();

  public static void setFactory(Class<? extends Factory> factory) {
    if (isInitialized()) {
      if (factory.isInstance(ourFactory)) {
        return;
      }

      //noinspection UseOfSystemOutOrSystemErr
      System.out.println("Changing log factory\n" + ExceptionUtil.getThrowableText(new Throwable()));
    }

    try {
      Constructor<? extends Factory> constructor = factory.getDeclaredConstructor();
      constructor.setAccessible(true);
      ourFactory = constructor.newInstance();
    }
    catch (Exception e) {
      //noinspection CallToPrintStackTrace
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public static boolean isInitialized() {
    return !(ourFactory instanceof DefaultFactory);
  }

  public static Logger getInstance(@NonNls String category) {
    return ourFactory.getLoggerInstance(category);
  }

  public static Logger getInstance(Class cl) {
    return getInstance(cl.getName());
  }

  public boolean isTraceEnabled() {
    return isDebugEnabled();
  }

  /**
   * Log a message with 'trace' level which finer-grained than 'debug' level. Use this method instead of {@link #debug(String)} for internal
   * events of a subsystem to avoid overwhelming the log if 'debug' level is enabled.
   */
  public void trace(String message) {
    debug(message);
  }

  public abstract boolean isDebugEnabled();

  public abstract void debug(@NonNls String message);

  public abstract void debug(@Nullable Throwable t);

  public abstract void debug(@NonNls String message, @Nullable Throwable t);

  public void debug(@Nonnull String message, Object... details) {
    if (isDebugEnabled()) {
      StringBuilder sb = new StringBuilder();
      sb.append(message);
      for (Object detail : details) {
        sb.append(String.valueOf(detail));
      }
      debug(sb.toString());
    }
  }

  public void info(@Nonnull Throwable t) {
    info(t.getMessage(), t);
  }

  public abstract void info(@NonNls String message);

  public abstract void info(@NonNls String message, @Nullable Throwable t);

  public void warn(@NonNls String message) {
    warn(message, null);
  }

  public void warn(@Nonnull Throwable t) {
    warn(t.getMessage(), t);
  }

  public abstract void warn(@NonNls String message, @Nullable Throwable t);

  public void error(@NonNls String message) {
    error(message, new Throwable(), ArrayUtil.EMPTY_STRING_ARRAY);
  }
  public void error(Object message) {
    error(String.valueOf(message));
  }

  public void error(@NonNls String message, Attachment... attachments) {
    error(message);
  }

  public void error(@NonNls String message, @NonNls String... details) {
    error(message, new Throwable(), details);
  }

  public void error(@NonNls String message, @Nullable Throwable e) {
    error(message, e, ArrayUtil.EMPTY_STRING_ARRAY);
  }

  public void error(@Nonnull Throwable t) {
    error(t.getMessage(), t, ArrayUtil.EMPTY_STRING_ARRAY);
  }

  public abstract void error(@NonNls String message, @Nullable Throwable t, @NonNls @Nonnull String... details);

  public boolean assertTrue(boolean value, @Nullable @NonNls Object message) {
    if (!value) {
      @NonNls String resultMessage = "Assertion failed";
      if (message != null) resultMessage += ": " + message;
      error(resultMessage, new Throwable());
    }

    return value;
  }

  public boolean assertTrue(boolean value) {
    return value || assertTrue(false, null);
  }
}
