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
package com.intellij.util.indexing.impl;

import com.intellij.util.indexing.StorageException;
import gnu.trove.THashMap;
import gnu.trove.TObjectObjectProcedure;
import javax.annotation.Nonnull;

import java.util.Map;

public class EmptyInputDataDiffBuilder<Key, Value> extends InputDataDiffBuilder<Key,Value> {
  public EmptyInputDataDiffBuilder(int inputId) {
    super(inputId);
  }

  @Override
  public void differentiate(@Nonnull Map<Key, Value> newData,
                            @Nonnull final KeyValueUpdateProcessor<Key, Value> addProcessor,
                            @Nonnull KeyValueUpdateProcessor<Key, Value> updateProcessor,
                            @Nonnull RemovedKeyProcessor<Key> removeProcessor) throws StorageException {
    processKeys(newData, addProcessor, myInputId);
  }

  static <Key, Value >void processKeys(@Nonnull Map<Key, Value> currentData,
                                       @Nonnull final KeyValueUpdateProcessor<Key, Value> processor,
                                       final int inputId)
          throws StorageException {
    if (currentData instanceof THashMap) {
      final StorageException[] exception = new StorageException[]{null};
      ((THashMap<Key, Value>)currentData).forEachEntry(new TObjectObjectProcedure<Key, Value>() {
        @Override
        public boolean execute(Key k, Value v) {
          try {
            processor.process(k, v, inputId);
          }
          catch (StorageException e) {
            exception[0] = e;
            return false;
          }
          return true;
        }
      });
      if (exception[0] != null) {
        throw exception[0];
      }
    }
    else {
      for (Map.Entry<Key, Value> entry : currentData.entrySet()) {
        processor.process(entry.getKey(), entry.getValue(), inputId);
      }
    }
  }
}
