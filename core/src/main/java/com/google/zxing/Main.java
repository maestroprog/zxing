package com.google.zxing;

import com.google.zxing.qrcode.detector.HardQrCodeReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public class Main {
  public static void main(String[] args) throws IOException {
    int i = 0;

    for (String arg : args) {
      long time = System.currentTimeMillis();
      arg = arg.trim();
      System.out.println(i++ + " " + arg);

      Result[] results = new Result[0];
      try {
        results = new Main().findBrightness(arg);
      } catch (NotFoundException e) {
        try {
          results = new Main().findContrasted(arg);
        } catch (NotFoundException ex) {
          ex.printStackTrace();
        }
      }

      if (results.length == 0) {
        System.out.println("failed!" + ((System.currentTimeMillis() - time) / 1000f));
      }
      System.out.println(results.length);
      for (Result result : results) {
        System.out.println(result.getText());
        System.out.println(new String(result.getRawBytes()));
        System.out.println("recognized! " + ((System.currentTimeMillis() - time) / 1000f));
      }
    }
  }

  private Result[] findBrightness(String arg) throws IOException, NotFoundException {
    long time = 0;

    Result[] results = {};

    try {
      BufferedImage image = ImageIO.read(new File(arg));
      HardQrCodeReader hardQrCodeReader = new HardQrCodeReader(getHints());
      BrightnessFilterLuminanceSource luminanceSource = new BrightnessFilterLuminanceSource(image, null);
      time = System.currentTimeMillis();
      luminanceSource.getMatrix();
        InvertedLuminanceSource isource = new InvertedLuminanceSource(luminanceSource);
      System.out.println("luminance building..." + ((System.currentTimeMillis() - time) / 1000f));
      time = System.currentTimeMillis();
      results = hardQrCodeReader.read(isource);
      System.out.println("finding..." + ((System.currentTimeMillis() - time) / 1000f));
    } catch (NotFoundException e) {
      System.out.println("failed!" + ((System.currentTimeMillis() - time) / 1000f));
      throw e;
    }

    return results;
  }

  private Result[] findContrasted(String arg) throws IOException, NotFoundException {
    LuminanceThresholds[] thresholds = {
      new LuminanceThresholds(0, 0),
      new LuminanceThresholds(0xA0, 0x60),
      new LuminanceThresholds(0x90, 0x60),
      new LuminanceThresholds(0xB0, 0x60),
      new LuminanceThresholds(0x70, 0x50),
      new LuminanceThresholds(0x80, 0x60),
      new LuminanceThresholds(0x70, 0x40),
      new LuminanceThresholds(0x80, 0x50),
      new LuminanceThresholds(0x90, 0x50),
      new LuminanceThresholds(0xA0, 0x50),
      new LuminanceThresholds(0xB0, 0x90),
      new LuminanceThresholds(0xC0, 0x90),
    };

    long time = 0;

    Result[] results = {};

    for (LuminanceThresholds threshold : thresholds) {
      try {
        BufferedImage image = ImageIO.read(new File(arg));
        HardQrCodeReader hardQrCodeReader = new HardQrCodeReader(getHints());
        ContrastThresholdLuminanceSource luminanceSource = new ContrastThresholdLuminanceSource(image, threshold);
        time = System.currentTimeMillis();
        luminanceSource.getMatrix();
        InvertedLuminanceSource isource = new InvertedLuminanceSource(luminanceSource);
        System.out.println("luminance building..." + ((System.currentTimeMillis() - time) / 1000f));
        time = System.currentTimeMillis();
        results = hardQrCodeReader.read(isource);
        System.out.println("finding..." + ((System.currentTimeMillis() - time) / 1000f));

        return results;
      } catch (NotFoundException e) {
        System.out.println("failed!" + ((System.currentTimeMillis() - time) / 1000f));
      }
    }

    throw NotFoundException.getNotFoundInstance();
  }

  private Map<DecodeHintType, Object> getHints() {
    Map<DecodeHintType, Object> newHints = new EnumMap<>(DecodeHintType.class);
    newHints.put(DecodeHintType.CHARACTER_SET, "ISO-8859-1");
    newHints.put(DecodeHintType.TRY_HARDER, true);
    return newHints;
  }
}
