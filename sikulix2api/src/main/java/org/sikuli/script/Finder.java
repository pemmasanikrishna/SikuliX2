/*
 * Copyright 2010-2014, Sikuli.org, sikulix.com
 * Released under the MIT License.
 *
 * modified RaiMan
 */
package org.sikuli.script;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.sikuli.util.Debug;
import org.sikuli.util.Settings;

public class Finder implements Iterator<Match> {

  static RunTime runTime = RunTime.get();

  //<editor-fold defaultstate="collapsed" desc="logging">
  private static final int lvl = 3;
  private static final Logger logger = LogManager.getLogger("SX.Finder");

  private static void log(int level, String message, Object... args) {
    if (Debug.is(lvl)) {
      message = String.format(message, args).replaceFirst("\\n", "\n          ");
      if (level == lvl) {
        logger.debug(message, args);
      } else if (level > lvl) {
        logger.trace(message, args);
      } else if (level == -1) {
        logger.error(message, args);
      } else {
        logger.info(message, args);
      }
    }
  }

  private void logp(String message, Object... args) {
    System.out.println(String.format(message, args));
  }

  public void terminate(int retval, String message, Object... args) {
    logger.fatal(String.format(" *** terminating: " + message, args));
    System.exit(retval);
  }
//</editor-fold>

  private boolean isImage = false;

  private boolean isRegion = false;
  private Region region = null;
  private int offX = 0, offY = 0;

  private boolean isMat = false;
  private Mat base = new Mat();
  
  private static final double downSimDiff = 0.15;

  private static class Probe {

    public Pattern pattern = null;
    public double similarity = 0;
    public double downSim = 0;
    public Image img = null;
    public Mat mat = null;
    public Region lastSeen = null;
    public double lastSeenScore = 0;
    public Core.MinMaxLocResult downResult = null;

    private boolean valid = false;

    public Probe(Pattern pattern) {
      if (pattern.isValid()) {
        this.pattern = pattern;
        similarity = pattern.getSimilar();
        downSim = ((int) ((similarity - downSimDiff) * 100))/100;
        img = pattern.getImage();
        mat = img.getMat();
        if (null != img.getLastSeen()) {
          lastSeen = new Region(img.getLastSeen());
          lastSeenScore = img.getLastSeenScore();
        }
      }
    }

    public boolean isValid() {
      return valid;
    }
  }

  private Finder() {
  }

  public Finder(Image img) {
    if (img != null && img.isValid()) {
      base = img.getMat();
      isImage = true;
    } else {
      log(-1, "init: invalid image: %s", img);
    }
  }

  public Finder(Region reg) {
    if (reg != null) {
      region = reg;
      offX = region.x;
      offY = region.y;
      isRegion = true;
    } else {
      log(-1, "init: invalid region: %s", reg);
    }
  }

  protected Finder(Mat base) {
    if (base != null) {
      this.base = base;
      isMat = true;
    } else {
      log(-1, "init: invalid CV-Mat: %s", base);
    }
  }

  public void setIsMultiFinder() {
    terminate(1, "TODO setIsMultiFinder()");
  }

  public boolean setImage(Image img) {
    return isImage;
  }

  public boolean inImage() {
    return isImage;
  }

  public boolean setRegion(Region reg) {
    return isRegion;
  }

  public boolean inRegion() {
    return isImage;
  }

  protected void setBase(BufferedImage bImg) {
    terminate(1, "TODO setBase(BufferedImage bImg)");
//    base = new Image(bImg, "").getMat();
  }

  protected long setBase() {
    if (!isRegion) {
      return 0;
    }
    long begin_t = new Date().getTime();
    base = region.captureThis().getMat();
    return new Date().getTime() - begin_t;
  }

  public boolean isValid() {
    if (!isImage && !isRegion) {
      return false;
    }
    return true;
  }

  public boolean find(Object target) {
    try {
      doFind(evalTarget(target));
    } catch (IOException ex) {
      log(-1, "find: Exception: %s", ex.getMessage());
      return false;
    }
    return true;
  }

  public String findText(String text) {
    terminate(1, "findText: not yet implemented");
    return null;
  }

  private final double resizeMinFactor = 1.5;
  private final float[] resizeLevels = new float[]{1f, 0.4f};
  private int resizeMaxLevel = resizeLevels.length - 1;
  private double resizeMinSim = 0.8;
  private boolean useOriginal = false;

  public void setUseOriginal() {
    useOriginal = true;
  }

  private boolean doFind(Pattern pattern) {
    boolean success = false;
    long begin_t = 0;
    Core.MinMaxLocResult mMinMax = null;
    Match mFound = null;
    Probe probe = new Probe(pattern);
    if (!useOriginal && Settings.CheckLastSeen && probe.lastSeen != null) {
      begin_t = new Date().getTime();
      Finder lastSeenFinder = new Finder(probe.lastSeen);
      lastSeenFinder.setUseOriginal();
      if (lastSeenFinder.find(new Pattern(probe.img).similar(probe.lastSeenScore - 0.01))) {
        mFound = lastSeenFinder.next();
        success = true;
        log(lvl + 1, "doFind: checkLastSeen: success %d msec", new Date().getTime() - begin_t);
      } else {
        log(lvl + 1, "doFind: checkLastSeen: not found %d msec", new Date().getTime() - begin_t);
      }
    }
    if (!success) {
      if (isRegion) {
        log(lvl+1, "doFind: capture: %d msec", setBase());
      }
      if (!useOriginal && probe.img.getResizeFactor() > resizeMinFactor) {
        mMinMax = doFindDown(probe, 0, probe.img.getResizeFactor());
      }
      if (mMinMax != null) {
        begin_t = new Date().getTime();
        Rect r = null;
        r = getSubMatRect(base, (int) mMinMax.maxLoc.x, (int) mMinMax.maxLoc.y,
                           probe.img.getWidth(), probe.img.getHeight(), 
                           ((int) probe.img.getResizeFactor()) + 1);
        mMinMax = doFindMatch(probe, base.submat(r), probe.mat);
        if (mMinMax != null && mMinMax.maxVal > probe.similarity) {
          mFound = new Match((int) mMinMax.maxLoc.x + offX +r.x, (int) mMinMax.maxLoc.y + offY + r.y,
                    probe.img.getWidth(), probe.img.getHeight(), mMinMax.maxVal);
          success = true;
        }
        log(lvl + 1, "doFind: check after doFindDown %%%.2f(%%%.2f) %d msec", 
                      mMinMax.maxVal * 100, probe.similarity * 100, new Date().getTime() - begin_t);
      }
      if (!success && useOriginal) {
        begin_t = new Date().getTime();
        mMinMax = doFindMatch(probe, base, probe.mat);
        if (mMinMax != null && mMinMax.maxVal > probe.similarity) {
          mFound = new Match((int) mMinMax.maxLoc.x + offX, (int) mMinMax.maxLoc.y + offY,
                  probe.img.getWidth(), probe.img.getHeight(), mMinMax.maxVal);
          success = true;
        }
        if (!useOriginal) {
          log(lvl+1, "doFind: search in original: %d msec", new Date().getTime() - begin_t);
        }
      }
    }
    if (success) {
      set(mFound);
      probe.img.setLastSeen(mFound.getRect(), mFound.getScore());
    }
    return success;
  }

  private Core.MinMaxLocResult doFindDown(Probe probe, int level, double factor) {
    long begin_t = new Date().getTime();
    Mat mBase = new Mat();
    Mat mPattern = new Mat();
    Core.MinMaxLocResult mMinMax = null;
    double rfactor;
    rfactor = factor * resizeLevels[level];
    if (rfactor < resizeMinFactor) {
      return null;
    }
    Size sb = new Size(base.cols() / rfactor, base.rows() / factor);
    Size sp = new Size(probe.mat.cols() / rfactor, probe.mat.rows() / factor);
    Imgproc.resize(base, mBase, sb, 0, 0, Imgproc.INTER_AREA);
    Imgproc.resize(probe.mat, mPattern, sp, 0, 0, Imgproc.INTER_AREA);
    mMinMax = doFindMatch(probe, mBase, mPattern);
    log(lvl + 1, "doFindDown (%d - 1/%.2f - %%%.2f(%.0f))", 
                  level, factor * resizeLevels[level], mMinMax.maxVal * 100, probe.downSim * 100);
    if (mMinMax.maxVal < probe.downSim) {
      if (level == resizeMaxLevel) {
        log(lvl+1, "doFindDown: %d msec", new Date().getTime() - begin_t);
        return null;
      }
      if (level == 0) {
        probe.downResult = null;
      }
      level++;
      log(lvl+1, "doFindDown: %d msec", new Date().getTime() - begin_t);
      doFindDown(probe, level, factor);
    } else {
      mMinMax.maxLoc.x *= rfactor;
      mMinMax.maxLoc.y *= rfactor;
      log(lvl+1, "doFindDown: %d msec", new Date().getTime() - begin_t);
      return mMinMax;
    }
    return null;
  }

  private Core.MinMaxLocResult doFindMatch(Probe probe, Mat base, Mat target) {
    Mat res = new Mat();
    Mat bi = new Mat();
    Mat pi = new Mat();
    if (!probe.img.isPlainColor()) {
      Imgproc.matchTemplate(base, target, res, Imgproc.TM_CCOEFF_NORMED);
    } else {
      if (probe.img.isBlack()) {
        Core.bitwise_not(base, bi);
        Core.bitwise_not(target, pi);
      } else {
        bi = base;
        pi = target;
      }
      Imgproc.matchTemplate(bi, pi, res, Imgproc.TM_SQDIFF_NORMED);
      Core.subtract(Mat.ones(res.size(), CvType.CV_32F), res, res);
    }
    return Core.minMaxLoc(res);
  }

  private static Rect getSubMatRect(Mat mat, int x, int y, int w, int h, int margin) {
    x = Math.max(0, x - margin);
    y = Math.max(0, y - margin);
    w = Math.min(w + 2 * margin, mat.width() - x);
    h = Math.min(h + 2 * margin, mat.height() - y);
    return new Rect(x, y, w, h);
  }

  private List<Match> matches = Collections.synchronizedList(new ArrayList<Match>());

  private Match set(Match m) {
    if (matches.size() > 0) {
      matches.set(0, m);
    } else {
      matches.add(m);
    }
    return m;
  }

  public boolean findAll(Object target) {
    terminate(1, "not implemented: findAll");
    return true;
  }

  public boolean hasChanges(Mat current) {
    int PIXEL_DIFF_THRESHOLD = 5;
    int IMAGE_DIFF_THRESHOLD = 5;
    Mat bg = new Mat();
    Mat cg = new Mat();
    Mat diff = new Mat();
    Mat tdiff = new Mat();

    Imgproc.cvtColor(base, bg, Imgproc.COLOR_BGR2GRAY);
    Imgproc.cvtColor(current, cg, Imgproc.COLOR_BGR2GRAY);
    Core.absdiff(bg, cg, diff);
    Imgproc.threshold(diff, tdiff, PIXEL_DIFF_THRESHOLD, 0.0, Imgproc.THRESH_TOZERO);
    if (Core.countNonZero(tdiff) <= IMAGE_DIFF_THRESHOLD) {
      return false;
    }

    Imgproc.threshold(diff, diff, PIXEL_DIFF_THRESHOLD, 255, Imgproc.THRESH_BINARY);
    Imgproc.dilate(diff, diff, new Mat());
    Mat se = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
    Imgproc.morphologyEx(diff, diff, Imgproc.MORPH_CLOSE, se);

    List<MatOfPoint> points = new ArrayList<MatOfPoint>();
    Mat contours = new Mat();
    Imgproc.findContours(diff, points, contours, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
    int n = 0;
    for (Mat pm : points) {
      log(lvl, "(%d) %s", n++, pm);
      printMatI(pm);
    }
    log(lvl, "contours: %s", contours);
    printMatI(contours);
    return true;
  }

  public void setMinChanges(int min) {
    terminate(1, "setMinChanges");
  }

  @Override
  public boolean hasNext() {
    return matches.size() > 0;
  }

  @Override
  public Match next() {
    if (hasNext()) {
      return matches.get(0);
    }
    return null;
  }

  @Override
  public void remove() {
  }

  protected Pattern evalTarget(Object target) throws IOException {
    boolean findingText = false;
    Image img = null;
    Pattern pattern = null;
    if (target instanceof String) {
      if (((String) target).startsWith("\t") && ((String) target).endsWith("\t")) {
        findingText = true;
      } else {
        img = new Image((String) target);
        if (img.isValid()) {
          pattern = new Pattern(img);
        } else if (img.isText()) {
          findingText = true;
        } else {
          throw new IOException("Region: doFind: Image not useable: " + target.toString());
        }
      }
      if (findingText) {
        if (TextRecognizer.getInstance() != null) {
          pattern = new Pattern((String) target, Pattern.Type.TEXT);
        }
      }
    } else if (target instanceof Pattern) {
      if (((Pattern) target).isValid()) {
        pattern = (Pattern) target;
      } else {
        throw new IOException("Region: doFind: Pattern not useable: " + target.toString());
      }
    } else if (target instanceof Image) {
      if (((Image) target).isValid()) {
        pattern = new Pattern((Image) target);
      } else {
        throw new IOException("Region: doFind: Image not useable: " + target.toString());
      }
    }
    if (null == pattern) {
      throw new UnsupportedOperationException("Region: doFind: invalid target: " + target.toString());
    }
    return pattern;
  }

  private static void printMatI(Mat mat) {
    int[] data = new int[mat.channels()];
    for (int r = 0; r < mat.rows(); r++) {
      for (int c = 0; c < mat.cols(); c++) {
        mat.get(r, c, data);
        log(lvl, "(%d, %d) %s", r, c, Arrays.toString(data));
      }
    }
  }

}
