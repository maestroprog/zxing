package com.google.zxing.qrcode.detector;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.multi.qrcode.detector.MultiFinderPatternFinder;

import java.util.Map;
import java.util.function.Function;

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
   */
  public void findConfirm(FinderPatternInfo info, Function<DetectorResult, Boolean> callback) throws NotFoundException {
    FinderPattern tl = info.getTopLeft();
    FinderPattern bl = info.getBottomLeft();
    FinderPattern tr = info.getTopRight();

    int radius = HardQrCodeReader.MAX_RADIUS;

    for (int blX = (int) bl.getX(); blX < bl.getX() + radius; blX += 1) {
      for (int blY = (int) bl.getY(); blY < bl.getY() + radius; blY += 1) {

        FinderPattern _bl = new FinderPattern(blX, blY, bl.getEstimatedModuleSize(), bl.getCount());

        for (int tlX = (int) tl.getX(); tlX < tl.getX() + radius; tlX += 1) {
          for (int tlY = (int) tl.getY(); tlY < tl.getY() + radius; tlY += 1) {

            FinderPattern _tl = new FinderPattern(tlX, tlY, tr.getEstimatedModuleSize(), tl.getCount());

            for (int trX = (int) tr.getX(); trX < tr.getX() + radius; trX += 1) {
              for (int trY = (int) tr.getY(); trY < tr.getY() + radius; trY += 1) {

                FinderPattern _tr = new FinderPattern(trX, trY, tr.getEstimatedModuleSize(), tr.getCount());

                try {
                  Detector detector = new Detector(bitmap.getBlackMatrix());
                  FinderPattern[] centers = {_bl, _tl, _tr};
                  FinderPatternInfo i = new FinderPatternInfo(centers);

                  try {
                    if (callback.apply(detector.processFinderPatternInfo(i))) {
                      return;
                    }
                  } catch (Throwable ignored) {
                    // Вдруг будут непонятные ошибки, пока ничего не делаем.
                  }

                } catch (NotFoundException ignored) {
                }
              }
            }
          }
        }
      }
    }

    throw NotFoundException.getNotFoundInstance();
  }
}
