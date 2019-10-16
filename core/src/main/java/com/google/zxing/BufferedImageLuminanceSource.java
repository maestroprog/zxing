/*
 * Copyright 2009 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * This LuminanceSource implementation is meant for J2SE clients and our blackbox unit tests.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 * @author code@elektrowolle.de (Wolfgang Jung)
 */
public final class BufferedImageLuminanceSource extends LuminanceSource {

  private static final double MINUS_45_IN_RADIANS = -0.7853981633974483; // Math.toRadians(-45.0)

  public BufferedImage getImage() {
    return image;
  }

  private final BufferedImage image;
  private final LuminanceThresholds thresholds;
  private final int left;
  private final int top;

  public BufferedImageLuminanceSource(BufferedImage image, LuminanceThresholds thresholds) {
    this(image, 0, 0, image.getWidth(), image.getHeight(), thresholds);
  }

  private int preparePixel(int pixel) {
    if ((pixel & 0xFF000000) == 0) {
      pixel = 0xFFFFFFFF; // = white
    }
//
//          // .299R + 0.587G + 0.114B (YUV/YIQ for PAL and NTSC),
//          // (306*R) >> 10 is approximately equal to R*0.299, and so on.
//          // 0x200 >> 10 is 0.5, it implements rounding.
//
    pixel = (306 * ((pixel >> 16) & 0xFF) +
            601 * ((pixel >> 8) & 0xFF) +
            117 * (pixel & 0xFF) +
            0x200) >> 10;
    return pixel;
  }

  public BufferedImageLuminanceSource(
          BufferedImage image,
          int left,
          int top,
          int width,
          int height,
          LuminanceThresholds thresholds
  ) {
    super(width, height);
    this.thresholds = thresholds;

    if (image.getType() == BufferedImage.TYPE_BYTE_GRAY) {
      this.image = image;
    } else {
      int sourceWidth = image.getWidth();
      int sourceHeight = image.getHeight();
      if (left + width > sourceWidth || top + height > sourceHeight) {
        throw new IllegalArgumentException("Crop rectangle does not fit within image data.");
      }

      this.image = new BufferedImage(sourceWidth, sourceHeight, BufferedImage.TYPE_BYTE_GRAY);

      float[][] core = {
              {0.05f, 0.1f, 0.2f, 0.1f, 0.05f},

              {0.1f, 0.5f, 0.5f, 0.5f, 0.1f},

              {0.2f, 0.5f, 1f, 0.5f, 0.2f},

              {0.1f, 0.5f, 0.5f, 0.5f, 0.1f},

              {0.05f, 0.1f, 0.2f, 0.1f, 0.05f},
      };

//      float[][] core = {
//              {0.1f, 0.3f, 0.5f, 0.3f, 0.1f},
//
//              {0.3f, 0.5f, 0.7f, 0.5f, 0.3f},
//
//              {0.5f, 0.7f, 1.0f, 0.7f, 0.5f},
//
//              {0.1f, 0.5f, 0.7f, 0.5f, 0.3f},
//
//              {0.1f, 0.3f, 0.5f, 0.3f, 0.1f},
//      };

      Graphics g = this.image.getGraphics();

      g.drawImage(image, 0, 0, null);
      g.dispose();

//      try {
//        ImageIO.write(this.image,"png",new File("qrr.png"));
//      } catch (IOException e) {
//        e.printStackTrace();
//      }

//      WritableRaster raster = this.image.getRaster();
//      int[] buffer = new int[width * height];
//      int[] newbuffer = new int[width * height];
//      image.getRGB(0, 0, width, height, buffer, 0, sourceWidth);
//      for (int y = top; y < top + height; y++) {
//        for (int x = 0; x < width; x++) {
//          int offset = y * width + x;
//          buffer[offset] = preparePixel(buffer[offset]);
//        }
//      }
//      double[] influences = new double[25];
//      for (int y = top; y < top + height - 5; y++) {
//        for (int x = 0; x < width - 5; x++) {
//          int offset = y * width + x;
//          int pixel = buffer[offset];
//
//          for (int i = 0; i < 5; i++) {
//            for (int j = 0; j < 5; j++) {
//              influences[j * 5 + i] = (buffer[(y + j) * width + (x + i)] - pixel) * core[i][j];
//            }
//          }
//          int result = pixel + (int) (Arrays.stream(influences).sum() / 25) & 0xFF;
////          if (result > 100) {
////            result = 255;
////          }
////          if (thresholds.getLightColor() != 0 || thresholds.getBlackColor() != 0) {
////            if (pixel >= thresholds.getLightColor()) {
////              result = 0xFF;
////            } else {
////              result = Math.max(0, result - thresholds.getBlackColor());
////            }
////          }
//          newbuffer[offset] = result;
//        }
//      }
//      raster.setPixels(0, 0, width, height, buffer);
//      for (int y = top; y < top + height; y++) {
//        image.getRGB(left, y, width, 1, buffer, 0, sourceWidth);
//        for (int x = 0; x < width; x++) {
//          int pixel = buffer[x];
//
//          // The color of fully-transparent pixels is irrelevant. They are often, technically, fully-transparent
//          // black (0 alpha, and then 0 RGB). They are often used, of course as the "white" area in a
//          // barcode image. Force any such pixel to be white:
//          if ((pixel & 0xFF000000) == 0) {
//            pixel = 0xFFFFFFFF; // = white
//          }
//
//          // .299R + 0.587G + 0.114B (YUV/YIQ for PAL and NTSC),
//          // (306*R) >> 10 is approximately equal to R*0.299, and so on.
//          // 0x200 >> 10 is 0.5, it implements rounding.
//
////          buffer[x] = pixel | 0x00000000;
//          pixel = (306 * ((pixel >> 16) & 0xFF) +
//            601 * ((pixel >> 8) & 0xFF) +
//            117 * (pixel & 0xFF) +
//            0x200) >> 10;
//          if (thresholds.getLightColor() != 0 || thresholds.getBlackColor() != 0) {
//            if (pixel >= thresholds.getLightColor()) {
//              pixel = 0xFF;
//            } else {
//              pixel = Math.max(0, pixel - thresholds.getBlackColor());
//            }
//          }
//          buffer[x] = pixel;
//        }
//        raster.setPixels(left, y, width, 1, buffer);
//      }
//      BufferedImage im = new BufferedImage(image.getWidth(),image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
//      im.setData(raster);
//      try {
//        ImageIO.write(im,"png",new File("qrr.png"));
//      } catch (IOException e) {
//        e.printStackTrace();
//      }
    }
    this.left = left;
    this.top = top;
  }

  @Override
  public byte[] getRow(int y, byte[] row) {
    if (y < 0 || y >= getHeight()) {
      throw new IllegalArgumentException("Requested row is outside the image: " + y);
    }
    int width = getWidth();
    if (row == null || row.length < width) {
      row = new byte[width];
    }
    // The underlying raster of image consists of bytes with the luminance values
    image.getRaster().getDataElements(left, top + y, width, 1, row);
    return row;
  }

  @Override
  public byte[] getMatrix() {
    int width = getWidth();
    int height = getHeight();
    int area = width * height;
    byte[] matrix = new byte[area];
    // The underlying raster of image consists of area bytes with the luminance values
    image.getRaster().getDataElements(left, top, width, height, matrix);
    return matrix;
  }

  @Override
  public boolean isCropSupported() {
    return true;
  }

  @Override
  public LuminanceSource crop(int left, int top, int width, int height) {
    return new BufferedImageLuminanceSource(image, this.left + left, this.top + top, width, height, thresholds);
  }

  /**
   * This is always true, since the image is a gray-scale image.
   *
   * @return true
   */
  @Override
  public boolean isRotateSupported() {
    return true;
  }

  @Override
  public LuminanceSource rotateCounterClockwise() {
    int sourceWidth = image.getWidth();
    int sourceHeight = image.getHeight();

    // Rotate 90 degrees counterclockwise.
    AffineTransform transform = new AffineTransform(0.0, -1.0, 1.0, 0.0, 0.0, sourceWidth);

    // Note width/height are flipped since we are rotating 90 degrees.
    BufferedImage rotatedImage = new BufferedImage(sourceHeight, sourceWidth, BufferedImage.TYPE_BYTE_GRAY);

    // Draw the original image into rotated, via transformation
    Graphics2D g = rotatedImage.createGraphics();
    g.drawImage(image, transform, null);
    g.dispose();

    // Maintain the cropped region, but rotate it too.
    int width = getWidth();
    return new BufferedImageLuminanceSource(rotatedImage, top, sourceWidth - (left + width), getHeight(), width, thresholds);
  }

  @Override
  public LuminanceSource rotateCounterClockwise45() {
    int width = getWidth();
    int height = getHeight();

    int oldCenterX = left + width / 2;
    int oldCenterY = top + height / 2;

    // Rotate 45 degrees counterclockwise.
    AffineTransform transform = AffineTransform.getRotateInstance(MINUS_45_IN_RADIANS, oldCenterX, oldCenterY);

    int sourceDimension = Math.max(image.getWidth(), image.getHeight());
    BufferedImage rotatedImage = new BufferedImage(sourceDimension, sourceDimension, BufferedImage.TYPE_BYTE_GRAY);

    // Draw the original image into rotated, via transformation
    Graphics2D g = rotatedImage.createGraphics();
    g.drawImage(image, transform, null);
    g.dispose();

    int halfDimension = Math.max(width, height) / 2;
    int newLeft = Math.max(0, oldCenterX - halfDimension);
    int newTop = Math.max(0, oldCenterY - halfDimension);
    int newRight = Math.min(sourceDimension - 1, oldCenterX + halfDimension);
    int newBottom = Math.min(sourceDimension - 1, oldCenterY + halfDimension);

    return new BufferedImageLuminanceSource(rotatedImage, newLeft, newTop, newRight - newLeft, newBottom - newTop, thresholds);
  }

}
