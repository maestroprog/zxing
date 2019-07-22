package com.google.zxing.qrcode.detector;

import com.google.zxing.*;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.Decoder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public class HardQrCodeReader {

  private static int MAX_RADIUS = 3;
  private static int MAX_STEP = 3;

  public static void setMaxRadius(int maxRadius) {
    if (maxRadius > 4) {
//      throw new IllegalArgumentException("Too big radius value! Recommended value must be lower than 3.");
    }
    MAX_RADIUS = maxRadius;
  }

  public static void setMaxStep(int maxStep) {
    if (maxStep > 4) {
//      throw new IllegalArgumentException("Too big step value! Recommended value must be lower than 3.");
    }
    MAX_STEP = maxStep;
  }

  private HardFinder finder;
  private BufferedImage sourceImage;

  public HardQrCodeReader(BufferedImage image) {
    sourceImage = resize(image, image.getWidth() * 3, image.getHeight() * 3);
  }

  /**
   * Считывает и декодирует все QR коды.
   */
  public Result[] read() throws NotFoundException {
    FinderPatternInfo[] infos;
    ArrayList<Result> results = new ArrayList<>();

    Map<DecodeHintType, Object> newHints = new EnumMap<>(DecodeHintType.class);
    newHints.put(DecodeHintType.TRY_HARDER, true);

    LuminanceThresholds[] thresholds = {
//      new LuminanceThresholds(0x90, 0x40),
      new LuminanceThresholds(0x90, 0x50),
      new LuminanceThresholds(0x90, 0x60),
//      new LuminanceThresholds(0x90, 0x70),

//      new LuminanceThresholds(0x80, 0x40),
      new LuminanceThresholds(0x80, 0x50),
      new LuminanceThresholds(0x80, 0x60),
//      new LuminanceThresholds(0x80, 0x70),

//      new LuminanceThresholds(0xA0, 0x40),
      new LuminanceThresholds(0xA0, 0x50),
      new LuminanceThresholds(0xA0, 0x60),
//      new LuminanceThresholds(0xA0, 0x70),

//      new LuminanceThresholds(0xB0, 0x40),
      new LuminanceThresholds(0xB0, 0x50),
      new LuminanceThresholds(0xB0, 0x60),
//      new LuminanceThresholds(0xB0, 0x70),

//      new LuminanceThresholds(0xC0, 0x40),
      new LuminanceThresholds(0xC0, 0x50),
      new LuminanceThresholds(0xC0, 0x60),
//      new LuminanceThresholds(0xC0, 0x70),

//      new LuminanceThresholds(0xD0, 0x40),
      new LuminanceThresholds(0xD0, 0x50),
      new LuminanceThresholds(0xD0, 0x60),
//      new LuminanceThresholds(0xD0, 0x70),
    };

    for (LuminanceThresholds threshold : thresholds) {

      BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(sourceImage, threshold);
      InvertedLuminanceSource isource = new InvertedLuminanceSource(source);
      HybridBinarizer binarizer = new HybridBinarizer(isource);
      BinaryBitmap bitmap = new BinaryBitmap(binarizer);
      finder = new HardFinder(bitmap);
      try {
        infos = finder.findPredict(newHints);
      } catch (NotFoundException e) {
        continue;
      }

      // Считываем предсказанные QR коды, заодно внутри замыкания пишем результаты декодирования кодов, если это получается делать.
      this.readPredicted(infos, (DetectorResult detectorResult) -> {
        Decoder decoder = new Decoder();
        DecoderResult decoderResult;
        try {
          decoderResult = decoder.decode(detectorResult.getBits());
        } catch (ChecksumException | FormatException ignored) {
          return Boolean.FALSE;
        }

        results.add(new Result(
          decoderResult.getText(),
          decoderResult.getRawBytes(),
          detectorResult.getPoints(),
          BarcodeFormat.QR_CODE
        ));

        return Boolean.TRUE;
      });

      if (!results.isEmpty()) {
        break;
      }
    }

    if (results.isEmpty()) {
      throw NotFoundException.getNotFoundInstance();
    }

    return results.toArray(new Result[0]);
  }

  /**
   * Считывает переданные предсказанные QR коды.
   * Возвращает массив найденных подтвержденных координатов предсказанных QR кодов.
   */
  public DetectorResult[] readPredicted(FinderPatternInfo[] infos, Function<DetectorResult, Boolean> callback) {

    ArrayList<DetectorResult> results = new ArrayList<>();

    for (FinderPatternInfo info : infos) {

      find_cycle:
      for (int radius = 1; radius <= MAX_RADIUS; radius++) {

        for (int step = 1; step <= MAX_STEP; step++) {

          try {

            for (DetectorResult detectorResult : finder.findConfirm(info, radius, step)) {

              if (callback != null) {
                try {
                  if (callback.apply(detectorResult)) {

                    results.add(detectorResult);

                    break find_cycle; // прерываем поиск кода, переходим к следующему коду
                  }
                } catch (Throwable ignored) {
                  // Вдруг будут непонятные ошибки, пока ничего не делаем.
                }
              }
            }

          } catch (NotFoundException ignored) {
          }
        }
      }
    }

    return results.toArray(new DetectorResult[0]);
  }

//  private DetectorResult[] findConfirm(FinderPatternInfo info, int radius, int step) throws NotFoundException {
//    FinderPattern[] patterns = {info.getBottomLeft(), info.getTopLeft(), info.getTopRight()};
//
//    float topLeftX = Arrays
//      .stream(patterns)
//      .map(ResultPoint::getX)
//      .min(Float::compareTo)
//      .orElse(0.0f);
//    float topLeftY = Arrays
//      .stream(patterns)
//      .map(ResultPoint::getY)
//      .min(Float::compareTo)
//      .orElse(0.0f);
//    float bottomRightX = Arrays
//      .stream(patterns)
//      .map(ResultPoint::getX)
//      .max(Float::compareTo)
//      .orElse(0.0f);
//    float bottomRightY = Arrays
//      .stream(patterns)
//      .map(ResultPoint::getY)
//      .max(Float::compareTo)
//      .orElse(0.0f);
//
//    float width = bottomRightX - topLeftX;
//    float height = bottomRightY - topLeftY;
//    int sizes = (int) Math.max(width, height);
//    float ws = 128f;
//    float is = 20f;
//    float indent = sizes * is / ws;
//    sizes += indent * 2;
//
//    float cropX = (float) Math.ceil(Math.max(0, topLeftX - indent));
//    float cropY = (float) Math.ceil(Math.max(0, topLeftY - indent));
//    int rightBorder = (int) Math.floor(Math.min(sizes, sourceImage.getWidth() - cropX));
//    int bottomBorder = (int) Math.floor(Math.min(sizes, sourceImage.getHeight() - cropY));
//    float multiplier = 1.0f;
//    int scaledSizes = (int) Math.round(sizes * multiplier);
//
////    System.out.println(" " + cropX + " "+ cropY + " " + rightBorder + " " + bottomBorder);
//    BufferedImage scaledImage = resize(sourceImage.getSubimage((int) cropX, (int) cropY, rightBorder, bottomBorder), scaledSizes, scaledSizes);
//
//    BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(scaledImage);
//    InvertedLuminanceSource isource = new InvertedLuminanceSource(source);
//    HybridBinarizer binarizer = new HybridBinarizer(isource);
//    BinaryBitmap bitmap = new BinaryBitmap(binarizer);
//    HardFinder finder = new HardFinder(bitmap);
//
////    System.out.println(info.getBottomLeft().getEstimatedModuleSize());
////    System.out.println(info.getBottomLeft().getCount());
////    System.out.println(info.getTopLeft().getEstimatedModuleSize());
////    System.out.println(info.getTopRight().getEstimatedModuleSize());
//
//    FinderPattern[] newPatterns = {
//      new FinderPattern(
//        (info.getBottomLeft().getX() - cropX) * multiplier,
//        (info.getBottomLeft().getY() - cropY) * multiplier,
//        info.getBottomLeft().getEstimatedModuleSize() * multiplier,
//        info.getBottomLeft().getCount()
//      ),
//      new FinderPattern(
//        (info.getTopLeft().getX() - cropX) * multiplier,
//        (info.getTopLeft().getY() - cropY) * multiplier,
//        info.getTopLeft().getEstimatedModuleSize() * multiplier,
//        info.getTopLeft().getCount()
//      ),
//      new FinderPattern(
//        (info.getTopRight().getX() - cropX) * multiplier,
//        (info.getTopRight().getY() - cropY) * multiplier,
//        info.getTopRight().getEstimatedModuleSize() * multiplier,
//        info.getTopRight().getCount()
//      )
//    };
//
////    System.out.println(sizes);
//    for (FinderPattern pattern : patterns) {
////      System.out.println((pattern.getX() - cropX) + ":" + (pattern.getY() - cropY) + " " + pattern.getEstimatedModuleSize());
////      System.out.println((pattern.getX() ) + ":" + (pattern.getY() ) + " " + pattern.getEstimatedModuleSize());
//      sourceImage.setRGB((int) pattern.getX(), (int) pattern.getY(), 255 << 16);
//    }
////    System.out.println(scaledSizes);
//    for (FinderPattern pattern : newPatterns) {
////      System.out.println(pattern.getX() + ":" + pattern.getY()+ " " + pattern.getEstimatedModuleSize());
//      scaledImage.setRGB((int) pattern.getX(), (int) pattern.getY(), 255 << 16);
//    }
////    System.out.println("\n");
//
//    FinderPatternInfo newInfo = new FinderPatternInfo(newPatterns);
//
////    try {
////      ImageIO.write(scaledImage, "png", new File("qr.png"));
//    return finder.findConfirm(newInfo, radius, step);
////    } catch (IOException e) {
////      return new DetectorResult[0];
////    }
//  }

  private static BufferedImage resize(BufferedImage img, int newW, int newH) {
    Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_AREA_AVERAGING);
    BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);

    Graphics2D g2d = dimg.createGraphics();
    g2d.drawImage(tmp, 0, 0, null);
    g2d.dispose();

    return dimg;
  }
}
