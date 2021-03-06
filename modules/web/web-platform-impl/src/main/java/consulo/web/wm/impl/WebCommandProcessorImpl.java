/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.wm.impl;

import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.wm.impl.CommandProcessorBase;
import consulo.web.application.WebApplication;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 13-Oct-17
 */
public class WebCommandProcessorImpl extends CommandProcessorBase {
  @Nonnull
  @Override
  protected ActionCallback invokeLater(@Nonnull Runnable command, @Nonnull Condition<?> condition) {
    ActionCallback actionCallback = new ActionCallback();

    WebApplication.invokeOnCurrentSession(() -> {
      try {
        command.run();
      }
      finally {
        actionCallback.setDone();
      }
    });
    return actionCallback;
  }
}
