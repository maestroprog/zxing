package com.google.zxing.qrcode.detector;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.multi.qrcode.detector.MultiFinderPatternFinder;

import java.util.ArrayList;
import java.util.Map;

public class HardFinder {

  private BinaryBitmap bitmap;

  public HardFinder(BinaryBitmap bitmap) {
    this.bitmap = bitmap;
  }

  public BinaryBitmap getBitmap() {
    return bitmap;
  }

  /**
   * Поиск с предсказанием местоположения QR кодов в разных местах.
   */
  public FinderPatternInfo[] findPredict(Map<DecodeHintType, ?> newHints) throws NotFoundException {

    MultiFinderPatternFinder finder = new MultiFinderPatternFinder(bitmap.getBlackMatrix());

    return finder.findMulti(newHints);


//    ArrayList<FinderPatternInfo> infos = new ArrayList<>();
//    infos.add(finder.find(newHints));
//    return infos.toArray(new FinderPatternInfo[0]);

  }

  /**
   * Поиск с подтверждением и уточнением местонахождения QR кода в определённой позиции.
   * Возвращает все возможные корректные координаты QR кода.
   */
  public DetectorResult[] findConfirm(FinderPatternInfo info, int radius, int step) throws NotFoundException {

    ArrayList<DetectorResult> detectorResults = new ArrayList<>();

    FinderPattern tl = info.getTopLeft();
    FinderPattern bl = info.getBottomLeft();
    FinderPattern tr = info.getTopRight();

    for (int blX = (int) bl.getX() - radius; blX < bl.getX() + radius; blX += step) {
      for (int blY = (int) bl.getY() - radius; blY < bl.getY() + radius; blY += step) {

        FinderPattern _bl = new FinderPattern(blX, blY, bl.getEstimatedModuleSize(), bl.getCount());

        for (int tlX = (int) tl.getX() - radius; tlX < tl.getX() + radius; tlX += step) {
          for (int tlY = (int) tl.getY() - radius; tlY < tl.getY() + radius; tlY += step) {

            FinderPattern _tl = new FinderPattern(tlX, tlY, tr.getEstimatedModuleSize(), tl.getCount());

            for (int trX = (int) tr.getX() - radius; trX < tr.getX() + radius; trX += step) {
              for (int trY = (int) tr.getY() - radius; trY < tr.getY() + radius; trY += step) {

                FinderPattern _tr = new FinderPattern(trX, trY, tr.getEstimatedModuleSize(), tr.getCount());

                try {
                  // Клонируем матрицу чтобы не повредить её для следующих итераций. TODO убедиться что матрица повреждается если не клонировать.
                  Detector detector = new Detector(bitmap.getBlackMatrix());
                  FinderPattern[] centers = {_bl, _tl, _tr};
                  FinderPatternInfo i = new FinderPatternInfo(centers);

                  detectorResults.add(detector.processFinderPatternInfo(i));

                } catch (NotFoundException | FormatException ignored) {
                }
              }
            }
          }
        }
      }
    }

    if (detectorResults.size() > 0) {
      return detectorResults.toArray(new DetectorResult[0]);
    }

    throw NotFoundException.getNotFoundInstance();
  }
}
