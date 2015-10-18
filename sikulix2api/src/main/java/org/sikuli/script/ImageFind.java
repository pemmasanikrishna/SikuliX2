/*
 * Copyright 2010-2014, Sikuli.org, sikulix.com
 * Released under the MIT License.
 *
 * modified RaiMan
 */
package org.sikuli.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.sikuli.util.Debug;
import org.sikuli.util.Settings;

/**
 * UNDER DEVELOPMENT --- SURELY HAS BUGS ;-)
 * Intended replacement for Finder together with ImageFinder
 * completely implementing the OpenCV usage on the Java level.
 */
//TODO ImageFind needed???
public class ImageFind {

  private Finder owner = null;

  private boolean isValid = false;
  private boolean isInnerFind = false;

  private Image pImage = null;
  private Mat probe = new Mat();
  private boolean isPlainColor = false;
  private boolean isBlack = false;
  private double similarity = Settings.MinSimilarity;
  private double waitingTime = Settings.AutoWaitTimeout;
  private boolean shouldCheckLastSeen = Settings.CheckLastSeen;
  private Object[] findArgs = null;

  private int resizeMinDownSample = 12;
  private double resizeFactor;
  private float[] resizeLevels = new float[] {1f, 0.4f};
  private int resizeMaxLevel = resizeLevels.length - 1;
  private double resizeMinSim = 0.9;
  private double resizeMinFactor = 1.5;
  private Core.MinMaxLocResult findDownRes = null;

  private int sorted;
  public static final int AS_ROWS = 0;
  public static final int AS_COLUMNS = 1;
  public static final int BEST_FIRST = 2;

  private int finding = -1;
  public static final int FINDING_ANY = 0;
  public static final int FINDING_SOME = 1;
  public static final int FINDING_ALL = 2;

  private int count = 0;
  public static int SOME_COUNT = 5;

  public static int ALL_MAX = 100;
  private int allMax = 0;

  private List<Match> matches = Collections.synchronizedList(new ArrayList<Match>());

  private boolean repeating;
  private long lastFindTime = 0;
  private long lastSearchTime = 0;

  public ImageFind() {
    matches.add(null);
  }

  public boolean isValid() {
    return true;
  }

  private Match checkFound(Core.MinMaxLocResult res) {
    Match match = null;
    Finder f;
    Rect r = null;
    if (owner.inImage()) {
      int off = ((int) resizeFactor) + 1;
      r = getSubMatRect(owner.base, (int) res.maxLoc.x, (int) res.maxLoc.y,
                            probe.width(), probe.height(), off);
      f = new Finder(owner.base.submat(r));
    } else {
      f = new Finder((new Region((int) res.maxLoc.x + owner.offX, (int) res.maxLoc.y + owner.offY,
                            probe.width(), probe.height())).grow(((int) resizeFactor) + 1));
    }
    if (null != f.findInner(probe, similarity)) {
      log(lvl, "check after downsampling: success");
      match = f.next();
      if (owner.inImage()) {
        match.x += r.x;
        match.y += r.y;
      }
    }
    return match;
  }

  private static Rect getSubMatRect(Mat mat, int x, int y, int w, int h, int margin) {
    x = Math.max(0, x - margin);
    y = Math.max(0, y - margin);
    w = Math.min(w + 2 * margin, mat.width() - x);
    h = Math.min(h + 2 * margin, mat.height()- y);
    return new Rect(x, y, w, h);
  }

  private Core.MinMaxLocResult doFindDown(int level, double factor) {
    Debug.enter(me + ": doFindDown (%d - 1/%.2f)", level, factor * resizeLevels[level]);
    Debug timer = Debug.startTimer("doFindDown");
    Mat b = new Mat();
    Mat p = new Mat();
    Core.MinMaxLocResult dres = null;
    double rfactor;
    if (factor > 0.0) {
      rfactor = factor * resizeLevels[level];
      if (rfactor < resizeMinFactor) return null;
      Size sb = new Size(owner.base.cols()/rfactor, owner.base.rows()/factor);
      Size sp = new Size(probe.cols()/rfactor, probe.rows()/factor);
      Imgproc.resize(owner.base, b, sb, 0, 0, Imgproc.INTER_AREA);
      Imgproc.resize(probe, p, sp, 0, 0, Imgproc.INTER_AREA);
      dres = doFindMatch(b, p);
      log(lvl, "doFindDown: score: %.2f at (%d, %d)", dres.maxVal,
              (int) (dres.maxLoc.x * rfactor), (int) (dres.maxLoc.y * rfactor));
    } else {
      dres = doFindMatch(owner.base, probe);
      timer.end();
      return dres;
    }
    if (dres.maxVal < resizeMinSim) {
      if (level == resizeMaxLevel) {
        timer.end();
        return null;
      }
      if (level == 0) {
        findDownRes = null;
      }
      level++;
      doFindDown(level, factor);
    } else {
        dres.maxLoc.x *= rfactor;
        dres.maxLoc.y *= rfactor;
        findDownRes = dres;
    }
    timer.end();
    return null;
  }

  private Core.MinMaxLocResult doFindMatch(Mat base, Mat probe) {
    Mat res = new Mat();
    Mat bi = new Mat();
    Mat pi = new Mat();
    if (!isPlainColor) {
      Imgproc.matchTemplate(base, probe, res, Imgproc.TM_CCOEFF_NORMED);
    } else {
      if (isBlack) {
        Core.bitwise_not(base, bi);
        Core.bitwise_not(probe, pi);
      } else {
        bi = base;
        pi = probe;
      }
      Imgproc.matchTemplate(bi, pi, res, Imgproc.TM_SQDIFF_NORMED);
      Core.subtract(Mat.ones(res.size(), CvType.CV_32F), res, res);
    }
    return Core.minMaxLoc(res);
  }
}
