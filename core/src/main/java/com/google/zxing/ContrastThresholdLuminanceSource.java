package com.google.zxing;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;

public class ContrastThresholdLuminanceSource extends BufferedImageLuminanceSource {
  private LuminanceThresholds thresholds;

  public ContrastThresholdLuminanceSource(BufferedImage image, LuminanceThresholds thresholds) {
    super(image);
    this.thresholds = thresholds;
  }

  @Override
  protected void prepareImage() {
    if (image.getType() == BufferedImage.TYPE_BYTE_GRAY) {
      return;
    }

    int sourceWidth = image.getWidth();
    int sourceHeight = image.getHeight();
    if (this.getWidth() > sourceWidth || this.getHeight() > sourceHeight) {
      throw new IllegalArgumentException("Crop rectangle does not fit within image data.");
    }

    BufferedImage image = this.image;
    this.image = new BufferedImage(sourceWidth, sourceHeight, BufferedImage.TYPE_BYTE_GRAY);

    int width = getWidth();
    int height = getHeight();

    DataBufferByte rasterDataBuffer = (DataBufferByte) image.getRaster().getDataBuffer();
    byte[] pixels = rasterDataBuffer.getData();
    int[] result = new int[height * width];

    final boolean hasAlphaChannel = image.getAlphaRaster() != null;
    final int pixelLength = hasAlphaChannel ? 4 : 3;
    final int pixelOffset = hasAlphaChannel ? 1 : 0;

    for (int pixel = 0, row = 0, col = 0; pixel + 2 < pixels.length; pixel += pixelLength) {

      // .299R + 0.587G + 0.114B (YUV/YIQ for PAL and NTSC),
      // (306*R) >> 10 is approximately equal to R*0.299, and so on.
      // 0x200 >> 10 is 0.5, it implements rounding.

      int ppixel = (306 * ((pixels[pixel + 2 + pixelOffset]) & 0xFF) +
        601 * ((pixels[pixel + 1 + pixelOffset]) & 0xFF) +
        117 * (pixels[pixel + pixelOffset] & 0xFF) +
        0x200) >> 10;

      if (thresholds.getLightColor() != 0 || thresholds.getBlackColor() != 0) {
        if (ppixel >= thresholds.getLightColor()) {
          ppixel = 0xFF;
        } else {
          ppixel = Math.max(0, ppixel - thresholds.getBlackColor());
        }
      }

      result[row * width + col] = ppixel;
      col++;
      if (col == width) {
        col = 0;
        row++;
      }
    }

    this.image.getRaster().setPixels(0, 0, width, height, result);
  }
}
