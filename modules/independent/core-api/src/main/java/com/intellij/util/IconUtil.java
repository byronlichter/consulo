/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.util;

import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.ScalableIcon;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.JBColor;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.RowIcon;
import com.intellij.util.ui.*;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * @author max
 * @author Konstantin Bulenkov
 */
public class IconUtil {

  @Nonnull
  public static Icon cropIcon(@Nonnull Icon icon, int maxWidth, int maxHeight) {
    if (icon.getIconHeight() <= maxHeight && icon.getIconWidth() <= maxWidth) {
      return icon;
    }

    final int w = Math.min(icon.getIconWidth(), maxWidth);
    final int h = Math.min(icon.getIconHeight(), maxHeight);

    final BufferedImage image = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration()
            .createCompatibleImage(icon.getIconWidth(), icon.getIconHeight(), Transparency.TRANSLUCENT);
    final Graphics2D g = image.createGraphics();
    icon.paintIcon(new JPanel(), g, 0, 0);
    g.dispose();

    final BufferedImage img = UIUtil.createImage(w, h, Transparency.TRANSLUCENT);
    final int offX = icon.getIconWidth() > maxWidth ? (icon.getIconWidth() - maxWidth) / 2 : 0;
    final int offY = icon.getIconHeight() > maxHeight ? (icon.getIconHeight() - maxHeight) / 2 : 0;
    for (int col = 0; col < w; col++) {
      for (int row = 0; row < h; row++) {
        img.setRGB(col, row, image.getRGB(col + offX, row + offY));
      }
    }

    return new ImageIcon(img);
  }

  @Nonnull
  public static Icon flip(@Nonnull Icon icon, boolean horizontal) {
    int w = icon.getIconWidth();
    int h = icon.getIconHeight();
    BufferedImage first = UIUtil.createImage(w, h, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = first.createGraphics();
    icon.paintIcon(new JPanel(), g, 0, 0);
    g.dispose();

    BufferedImage second = UIUtil.createImage(w, h, BufferedImage.TYPE_INT_ARGB);
    g = second.createGraphics();
    if (horizontal) {
      g.drawImage(first, 0, 0, w, h, w, 0, 0, h, null);
    }
    else {
      g.drawImage(first, 0, 0, w, h, 0, h, w, 0, null);
    }
    g.dispose();
    return new ImageIcon(second);
  }

  @Nonnull
  public static Icon getEmptyIcon(boolean showVisibility) {
    RowIcon baseIcon = new RowIcon(2);
    baseIcon.setIcon(EmptyIcon.create(getDefaultNodeIconSize()), 0);
    if (showVisibility) {
      baseIcon.setIcon(EmptyIcon.create(getDefaultNodeIconSize()), 1);
    }
    return baseIcon;
  }

  public static int getDefaultNodeIconSize() {
    return JBUI.scale(16);
  }

  public static Image toImage(@Nonnull Icon icon) {
    if (icon instanceof ImageIcon) {
      return ((ImageIcon)icon).getImage();
    }
    else {
      final int w = icon.getIconWidth();
      final int h = icon.getIconHeight();
      final BufferedImage image = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(w, h, Transparency.TRANSLUCENT);
      final Graphics2D g = image.createGraphics();
      icon.paintIcon(null, g, 0, 0);
      g.dispose();
      return image;
    }
  }

  public static Icon getAddIcon() {
    return getToolbarDecoratorIcon("add.png");
  }

  public static Icon getRemoveIcon() {
    return getToolbarDecoratorIcon("remove.png");
  }

  public static Icon getMoveUpIcon() {
    return getToolbarDecoratorIcon("moveUp.png");
  }

  public static Icon getMoveDownIcon() {
    return getToolbarDecoratorIcon("moveDown.png");
  }

  public static Icon getEditIcon() {
    return getToolbarDecoratorIcon("edit.png");
  }

  public static Icon getAddClassIcon() {
    return getToolbarDecoratorIcon("addClass.png");
  }

  public static Icon getAddPatternIcon() {
    return getToolbarDecoratorIcon("addPattern.png");
  }

  public static Icon getAddJiraPatternIcon() {
    return getToolbarDecoratorIcon("addJira.png");
  }

  public static Icon getAddYouTrackPatternIcon() {
    return getToolbarDecoratorIcon("addYouTrack.png");
  }

  public static Icon getAddBlankLineIcon() {
    return getToolbarDecoratorIcon("addBlankLine.png");
  }

  public static Icon getAddPackageIcon() {
    return getToolbarDecoratorIcon("addPackage.png");
  }

  public static Icon getAddLinkIcon() {
    return getToolbarDecoratorIcon("addLink.png");
  }

  public static Icon getAddFolderIcon() {
    return getToolbarDecoratorIcon("addFolder.png");
  }

  public static Icon getAnalyzeIcon() {
    return getToolbarDecoratorIcon("analyze.png");
  }

  public static void paintInCenterOf(@Nonnull Component c, Graphics g, Icon icon) {
    final int x = (c.getWidth() - icon.getIconWidth()) / 2;
    final int y = (c.getHeight() - icon.getIconHeight()) / 2;
    icon.paintIcon(c, g, x, y);
  }

  public static Icon getToolbarDecoratorIcon(String name) {
    return IconLoader.getIcon(getToolbarDecoratorIconsFolder() + name);
  }

  private static String getToolbarDecoratorIconsFolder() {
    return "/toolbarDecorator/" + (SystemInfo.isMac ? "mac/" : "");
  }

  /**
   * Result icons look like original but have equal (maximum) size
   */
  @Nonnull
  public static Icon[] getEqualSizedIcons(@Nonnull Icon... icons) {
    Icon[] result = new Icon[icons.length];
    int width = 0;
    int height = 0;
    for (Icon icon : icons) {
      width = Math.max(width, icon.getIconWidth());
      height = Math.max(height, icon.getIconHeight());
    }
    for (int i = 0; i < icons.length; i++) {
      result[i] = new IconSizeWrapper(icons[i], width, height);
    }
    return result;
  }

  public static Icon toSize(@Nonnull Icon icon, int width, int height) {
    return new IconSizeWrapper(icon, JBUI.scale(width), JBUI.scale(height));
  }

  public static class IconSizeWrapper implements Icon {
    private final Icon myIcon;
    private final int myWidth;
    private final int myHeight;

    protected IconSizeWrapper(@Nullable Icon icon, int width, int height) {
      myIcon = icon;
      myWidth = width;
      myHeight = height;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      paintIcon(myIcon, c, g, x, y);
    }

    protected void paintIcon(@Nullable Icon icon, Component c, Graphics g, int x, int y) {
      if (icon == null) return;
      x += (myWidth - icon.getIconWidth()) / 2;
      y += (myHeight - icon.getIconHeight()) / 2;
      icon.paintIcon(c, g, x, y);
    }

    @Override
    public int getIconWidth() {
      return myWidth;
    }

    @Override
    public int getIconHeight() {
      return myHeight;
    }
  }

  public static Icon scale(@Nonnull final Icon source, double _scale) {
    final double scale = Math.min(32, Math.max(.1, _scale));
    return new Icon() {
      @Override
      public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D)g.create();
        try {
          AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);
          transform.preConcatenate(g2d.getTransform());
          g2d.setTransform(transform);
          source.paintIcon(c, g2d, x, y);
        }
        finally {
          g2d.dispose();
        }
      }

      @Override
      public int getIconWidth() {
        return (int)(source.getIconWidth() * scale);
      }

      @Override
      public int getIconHeight() {
        return (int)(source.getIconHeight() * scale);
      }
    };
  }

  /**
   * Returns a scaled icon instance, in scale of the provided font size.
   * <p>
   * The method delegates to {@link ScalableIcon#scale(float)} when applicable,
   * otherwise defaults to {@link #scale(Icon, double)}
   * <p>
   * Refer to {@link #scale(Icon, Component, float)} for more details.
   *
   * @param icon     the icon to scale
   * @param ancestor the component (or its ancestor) painting the icon, or null when not available
   * @param fontSize the reference font size
   * @return the scaled icon
   */
  @Nonnull
  public static Icon scaleByFont(@Nonnull Icon icon, @Nullable Component ancestor, float fontSize) {
    float scale = JBUI.getFontScale(fontSize);
    /*if (icon instanceof ScalableIcon) {
      if (icon instanceof ScaleContextAware) {
        ScaleContextAware ctxIcon = (ScaleContextAware)icon;
        ctxIcon.updateScaleContext(ancestor != null ? ScaleContext.create(ancestor) : null);
        // take into account the user scale of the icon
        double usrScale = ctxIcon.getScaleContext().getScale(USR_SCALE);
        scale /= usrScale;
      }
      return ((ScalableIcon)icon).scale(scale);
    }*/
    return scale(icon, scale);
  }

  @Nonnull
  public static Icon colorize(@Nonnull final Icon source, @Nonnull Color color) {
    return colorize(source, color, false);
  }

  @Nonnull
  public static Icon colorize(@Nonnull final Icon source, @Nonnull Color color, boolean keepGray) {
    float[] base = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);

    final BufferedImage image = UIUtil.createImage(source.getIconWidth(), source.getIconHeight(), Transparency.TRANSLUCENT);
    final Graphics2D g = image.createGraphics();
    source.paintIcon(null, g, 0, 0);
    g.dispose();

    final BufferedImage img = UIUtil.createImage(source.getIconWidth(), source.getIconHeight(), Transparency.TRANSLUCENT);
    int[] rgba = new int[4];
    float[] hsb = new float[3];
    for (int y = 0; y < image.getRaster().getHeight(); y++) {
      for (int x = 0; x < image.getRaster().getWidth(); x++) {
        image.getRaster().getPixel(x, y, rgba);
        if (rgba[3] != 0) {
          Color.RGBtoHSB(rgba[0], rgba[1], rgba[2], hsb);
          int rgb = Color.HSBtoRGB(base[0], base[1] * (keepGray ? hsb[1] : 1f), base[2] * hsb[2]);
          img.getRaster().setPixel(x, y, new int[]{rgb >> 16 & 0xff, rgb >> 8 & 0xff, rgb & 0xff, rgba[3]});
        }
      }
    }

    return createImageIcon(img);
  }

  @Nonnull
  public static JBImageIcon createImageIcon(@Nonnull final BufferedImage img) {
    return new JBImageIcon(img) {
      @Override
      public int getIconWidth() {
        return getImage() instanceof JBHiDPIScaledImage ? super.getIconWidth() / 2 : super.getIconWidth();
      }

      @Override
      public int getIconHeight() {
        return getImage() instanceof JBHiDPIScaledImage ? super.getIconHeight() / 2 : super.getIconHeight();
      }
    };
  }

  @Nonnull
  public static Icon textToIcon(final String text, final Component component, final float fontSize) {
    final Font font = JBFont.create(JBUI.Fonts.label().deriveFont(fontSize));
    FontMetrics metrics = component.getFontMetrics(font);
    final int width = metrics.stringWidth(text) + JBUI.scale(4);
    final int height = metrics.getHeight();

    return new Icon() {
      @Override
      public void paintIcon(Component c, Graphics g, int x, int y) {
        g = g.create();
        try {
          GraphicsUtil.setupAntialiasing(g);
          g.setFont(font);
          UIUtil.drawStringWithHighlighting(g, text, x + JBUI.scale(2), y + height - JBUI.scale(1), JBColor.foreground(), JBColor.background());
        }
        finally {
          g.dispose();
        }
      }

      @Override
      public int getIconWidth() {
        return width;
      }

      @Override
      public int getIconHeight() {
        return height;
      }
    };
  }

  public static Icon addText(@Nonnull Icon base, @Nonnull String text) {
    LayeredIcon icon = new LayeredIcon(2);
    icon.setIcon(base, 0);
    icon.setIcon(textToIcon(text, new JLabel(), JBUI.scale(6f)), 1, SwingConstants.SOUTH_EAST);
    return icon;
  }
}
