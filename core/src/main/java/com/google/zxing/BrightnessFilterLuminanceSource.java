package com.google.zxing;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;

public class BrightnessFilterLuminanceSource extends BufferedImageLuminanceSource {
  final private static double[][] core1 = {
    {0.05f, 0.10f, 0.20f, 0.10f, 0.05f},
    {0.10f, 0.50f, 0.50f, 0.50f, 0.10f},
    {0.20f, 0.50f, 1.00f, 0.50f, 0.20f},
    {0.10f, 0.50f, 0.50f, 0.50f, 0.10f},
    {0.05f, 0.10f, 0.20f, 0.10f, 0.05f},
  };

  final private static double[][] core2 = {
    {0.1f, 0.3f, 0.5f, 0.3f, 0.1f},
    {0.3f, 0.5f, 0.7f, 0.5f, 0.3f},
    {0.5f, 0.7f, 1.0f, 0.7f, 0.5f},
    {0.1f, 0.5f, 0.7f, 0.5f, 0.3f},
    {0.1f, 0.3f, 0.5f, 0.3f, 0.1f},
  };

  private double[] currentCore;

  public BrightnessFilterLuminanceSource(
    BufferedImage image,
    double[][] core
  ) {
    super(image);

    initializeCore(core != null ? core : core1);
  }

  public BrightnessFilterLuminanceSource(
    BufferedImage image,
    int coreVersion
  ) {
    super(image);

    initializeCore(coreVersion == 1 ? core1 : core2);
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
    int[] newbuffer = new int[height * width];

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

      result[row * width + col] = ppixel;

      col++;
      if (col == width) {
        col = 0;
        row++;
      }
    }

    int pixel;
    int offset;
    int ii;
    double influences;
    for (int y = 0, x = 0; y < height - 5; ) {
      offset = y * width + x;
      pixel = result[offset];

      ii = 0;
      influences = 0.0;

      influences += (result[offset] - pixel) * currentCore[ii++];
      influences += (result[offset + width] - pixel) * currentCore[ii++];
      influences += (result[offset + 2 * width] - pixel) * currentCore[ii++];
      influences += (result[offset + 3 * width] - pixel) * currentCore[ii++];
      influences += (result[offset + 4 * width] - pixel) * currentCore[ii++];

      influences += (result[offset + 1] - pixel) * currentCore[ii++];
      influences += (result[offset + 1 + width] - pixel) * currentCore[ii++];
      influences += (result[offset + 1 + 2 * width] - pixel) * currentCore[ii++];
      influences += (result[offset + 1 + 3 * width] - pixel) * currentCore[ii++];
      influences += (result[offset + 1 + 4 * width] - pixel) * currentCore[ii++];

      influences += (result[offset + 2] - pixel) * currentCore[ii++];
      influences += (result[offset + 2 + width] - pixel) * currentCore[ii++];
      influences += (result[offset + 2 + 2 * width] - pixel) * currentCore[ii++];
      influences += (result[offset + 2 + 3 * width] - pixel) * currentCore[ii++];
      influences += (result[offset + 2 + 4 * width] - pixel) * currentCore[ii++];

      influences += (result[offset + 3] - pixel) * currentCore[ii++];
      influences += (result[offset + 3 + width] - pixel) * currentCore[ii++];
      influences += (result[offset + 3 + 2 * width] - pixel) * currentCore[ii++];
      influences += (result[offset + 3 + 3 * width] - pixel) * currentCore[ii++];
      influences += (result[offset + 3 + 4 * width] - pixel) * currentCore[ii++];

      influences += (result[offset + 4] - pixel) * currentCore[ii++];
      influences += (result[offset + 4 + width] - pixel) * currentCore[ii++];
      influences += (result[offset + 4 + 2 * width] - pixel) * currentCore[ii++];
      influences += (result[offset + 4 + 3 * width] - pixel) * currentCore[ii++];
      influences += (result[offset + 4 + 4 * width] - pixel) * currentCore[ii];

      newbuffer[offset] = (int) (pixel + (influences / 25.0)) & 0xFF;

      x++;
      if (x >= width - 5) {
        x = 0;
        y++;
      }
    }

    this.image.getRaster().setPixels(0, 0, width, height, newbuffer);
  }

  private void initializeCore(double[][] core) {
    currentCore = new double[25];
    int ic = 0;
    for (int i = 0; i < 5; i++) {
      for (int j = 0; j < 5; j++) {
        currentCore[ic++] = core[i][j];
      }
    }
  }
}
