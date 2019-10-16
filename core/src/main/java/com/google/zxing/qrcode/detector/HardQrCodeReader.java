package com.google.zxing.qrcode.detector;

import com.google.zxing.*;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.Decoder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Map;

public class HardQrCodeReader {
  static int MAX_RADIUS = 1;
  static int MAX_TRY = 7;
  private Map<DecodeHintType, Object> newHints;

  public static void setMaxRadius(int maxRadius) {
    MAX_RADIUS = maxRadius;
  }

  public static void setMaxTry(int maxTry) {
    MAX_TRY = maxTry;
  }

  public HardQrCodeReader(Map<DecodeHintType, Object> newHints) {
    this.newHints = newHints;
  }

  /**
   * Считывает и декодирует все QR коды.
   */
  public Result[] read(LuminanceSource luminanceSource) throws NotFoundException {
    FinderPatternInfo[] infos;
    ArrayList<Result> results = new ArrayList<>();

    HybridBinarizer binarizer = new HybridBinarizer(luminanceSource);
    BinaryBitmap bitmap = new BinaryBitmap(binarizer);
    HardFinder finder = new HardFinder(bitmap);

    infos = finder.findPredict(newHints);

    // Считываем предсказанные QR коды, заодно внутри замыкания пишем результаты декодирования кодов, если это получается делать.
    for (FinderPatternInfo info : infos) {
      try {
        finder.findConfirm(info, (DetectorResult detectorResult) -> {
          Decoder decoder = new Decoder();
          DecoderResult decoderResult;
          try {
            decoderResult = decoder.decode(detectorResult.getBits(), newHints);
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
      } catch (NotFoundException ignored) {
      }
    }

    if (results.isEmpty()) {
      throw NotFoundException.getNotFoundInstance();
    }

    return results.toArray(new Result[0]);
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
