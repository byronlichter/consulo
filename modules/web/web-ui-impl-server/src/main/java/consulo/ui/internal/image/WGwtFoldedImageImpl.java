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
package consulo.ui.internal.image;

import consulo.ui.image.FoldedImage;
import consulo.ui.image.Image;
import consulo.web.gwt.shared.ui.state.image.FoldedImageState;
import consulo.web.gwt.shared.ui.state.image.MultiImageState;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 11-Sep-17
 */
public class WGwtFoldedImageImpl implements FoldedImage, WGwtImageWithState {
  private Image[] myImages;

  public WGwtFoldedImageImpl(Image[] images) {
    myImages = images;
  }

  @Override
  public int getHeight() {
    return myImages[0].getHeight();
  }

  @Override
  public int getWidth() {
    return myImages[0].getWidth();
  }

  @Nonnull
  @Override
  public Image[] getImages() {
    return myImages;
  }

  @Override
  public void toState(MultiImageState m) {
    FoldedImageState state = new FoldedImageState();
    state.myChildren = new MultiImageState[myImages.length];

    for (int i = 0; i < myImages.length; i++) {
      Image image = myImages[i];
      state.myChildren[i] = WGwtImageUrlCache.fixSwingImageRef(image).getState();
    }

    m.myFoldedImageState = state;
  }
}
