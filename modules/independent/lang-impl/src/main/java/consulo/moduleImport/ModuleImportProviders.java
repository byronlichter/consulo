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
package consulo.moduleImport;

import com.intellij.projectImport.ProjectImportProvider;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 30-Jan-17
 */
public class ModuleImportProviders {
  @Nonnull
  public static List<ModuleImportProvider> getExtensions(boolean forImportAction) {
    List<ModuleImportProvider> list = new ArrayList<>();
    for (ModuleImportProvider<?> provider : ModuleImportProvider.EP_NAME.getExtensions()) {
      if (forImportAction) {
        if (!provider.isOnlyForNewImport()) {
          list.add(provider);
        }
      }
      else {
        list.add(provider);
      }
    }
    for (ProjectImportProvider provider : ProjectImportProvider.EP_NAME.getExtensions()) {
      list.add(new LegacyModuleImportProvider(provider));
    }
    return list;
  }
}
