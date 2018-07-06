package com.google.zxing.qrcode.detector;

import com.google.zxing.*;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.Decoder;
import com.sun.istack.internal.Nullable;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public class HardQrCodeReader {

  private static final int MAX_RADIUS = 3;
  private static final int MAX_STEP = 3;

  private HardFinder finder;

  public HardQrCodeReader(BufferedImage image) {

    BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
    InvertedLuminanceSource isource = new InvertedLuminanceSource(source);
    HybridBinarizer binarizer = new HybridBinarizer(isource);
    BinaryBitmap bitmap = new BinaryBitmap(binarizer);
    finder = new HardFinder(bitmap);
  }

  public HardFinder getFinder() {
    return this.finder;
  }

  /**
   * Считывает и декодирует все QR коды.
   */
  public Result[] read() throws NotFoundException {
    FinderPatternInfo[] infos;
    ArrayList<Result> results = new ArrayList<>();

    Map<DecodeHintType, Object> newHints = new EnumMap<>(DecodeHintType.class);
    newHints.put(DecodeHintType.TRY_HARDER, true);
    infos = finder.findPredict(newHints);

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

    return results.toArray(new Result[0]);
  }

  /**
   * Считывает переданные предсказанные QR коды.
   * Возвращает массив найденных подтвержденных координатов предсказанных QR кодов.
   */
  public DetectorResult[] readPredicted(FinderPatternInfo[] infos, @Nullable Function<DetectorResult, Boolean> callback) {

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
}
