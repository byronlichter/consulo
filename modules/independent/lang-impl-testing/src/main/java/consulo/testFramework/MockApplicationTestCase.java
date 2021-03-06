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
package consulo.testFramework;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.UsefulTestCase;
import consulo.testFramework.application.MockApplicationEnvironment;

import java.io.File;
import java.io.IOException;

/**
 * @author VISTALL
 * @since 10-Sep-17
 */
public class MockApplicationTestCase extends UsefulTestCase {
  protected Disposable myRootDisposable;
  private File myTempDir;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myRootDisposable = Disposer.newDisposable();
    new MockApplicationEnvironment(myRootDisposable);
  }

  public File getTempDir() throws IOException {
    if (myTempDir == null) {
      myTempDir = FileUtil.createTempDirectory(getName(), getClass().getName(), false);
    }

    return myTempDir;
  }

  public Disposable getRootDisposable() {
    return myRootDisposable;
  }

  @Override
  protected boolean shouldContainTempFiles() {
    return false;
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();

    if (myTempDir != null) {
      FileUtil.asyncDelete(myTempDir);
    }
    Disposer.dispose(myRootDisposable);
  }
}
