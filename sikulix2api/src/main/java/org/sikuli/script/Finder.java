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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.sikuli.util.Debug;
import org.sikuli.util.Settings;

public class Finder implements Iterator<Match>{
  
  static RunTime runTime = RunTime.get();

  //<editor-fold defaultstate="collapsed" desc="logging">
  private static final int lvl = 3;
  private static final Logger logger = LogManager.getLogger("SX.Finder");

  private static void log(int level, String message, Object... args) {
    if (Debug.is(lvl)) {
      message = String.format(message, args).replaceFirst("\\n", "\n          ");
      if (level == 3) {
        logger.debug(message, args);
      } else if (level > 3) {
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

  private boolean isImageFinder = true;
  protected boolean isImage = false;
  protected Region region = null;
  protected boolean isRegion = false;
  protected IScreen screen = null;
  protected boolean isScreen = false;
  protected int offX, offY;
  protected long MaxTimePerScan;
  private Image bImage = null;
  protected Mat base = new Mat();
  private double waitingTime = Settings.AutoWaitTimeout;
  private int minChanges;
  private ImageFind firstFind = null;
  private boolean isReusable = false;
  protected boolean isMultiFinder = false;

  private Finder() {
  }

  public Finder(Image img) {
    if (img != null && img.isValid()) {
      bImage = img;
      base = img.getMat();
      isImage = true;
      init(img);
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
      init(reg);
    } else log(-1, "init: invalid region: %s", reg);
  }
  
  protected Finder(Mat base) {
    terminate(1, "TODO observe changes");
  }

  private void init(Object obj) {
    log(3, "for %s", obj);
  }

  public void setIsMultiFinder() {
    terminate(1, "TODO setIsMultiFinder()");
    base = new Mat();
    isMultiFinder = true;
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

  public boolean isValid() {
    if (!isImage && !isRegion) {
      return false;
    }
    return true;
  }

  public boolean find(Object target) {
    try {
      imageFind(evalTarget(target));
    } catch (IOException ex) {
      log(-1, "find: Exception: %s", ex.getMessage());
      return false;
    }
    return true;
  }

  public String findText(String text) {
    log(-1, "findText: not yet implemented");
    return null;
  }

//  protected <PSI> ImageFind findInner(PSI probe, double sim) {
//    ImageFind newFind = new ImageFind();
//    newFind.setIsInnerFind();
//    newFind.setSimilarity(sim);
//    if (!newFind.checkFind(this, probe)) {
//      return null;
//    }
//    firstFind = newFind;
//    if (newFind.isValid()) {
//      return newFind.doFind();
//    }
//    return null;
//  }

  private <PSI> ImageFind imageFind(PSI probe, Object... args) {
    ImageFind newFind = new ImageFind();
    newFind.setFindTimeout(waitingTime);
    if (!newFind.checkFind(this, probe, args)) {
      return null;
    }
    if (newFind.isValid() && !isReusable && firstFind == null) {
      firstFind = newFind;
    }
    ImageFind imgFind = newFind.doFind();
    log(lvl, "find: success: %s", imgFind.get());
    return imgFind;
  }

  public boolean findAll(Object target) {
    try {
      imageFindAll(evalTarget(target), ImageFind.BEST_FIRST, 0);
    } catch (IOException ex) {
      log(-1, "findAll: Exception: %s", ex.getMessage());
      return false;
    }
    return true;
  }

  private ImageFind imageFindAll(Pattern probe, int sorted, int count, Object... args) {
    ImageFind newFind = new ImageFind();
    newFind.setFinding(ImageFind.FINDING_ALL);
    newFind.setSorted(sorted);
    if (count > 0) {
      newFind.setCount(count);
    }
   if (!newFind.checkFind(this, probe, args)) {
      return null;
    }
    if (newFind.isValid() && !isReusable && firstFind == null) {
      firstFind = newFind;
    }
    ImageFind imgFind = newFind.doFind();
    log(lvl, "find: success: %s", imgFind.get());
    return imgFind;
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
    Mat se = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5,5));
    Imgproc.morphologyEx(diff, diff, Imgproc.MORPH_CLOSE, se);

    List<MatOfPoint> points = new ArrayList<MatOfPoint>();
    Mat contours = new Mat();
    Imgproc.findContours(diff, points, contours, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
    int n = 0;
    for (Mat pm: points) {
      log(lvl, "(%d) %s", n++, pm);
      printMatI(pm);
    }
    log(lvl, "contours: %s", contours);
    printMatI(contours);
    return true;
  }

  public void setMinChanges(int min) {
    minChanges = min;
  }

  @Override
  public boolean hasNext() {
    if (null != firstFind) {
      return firstFind.hasNext();
    }
    return false;
  }

  @Override
  public Match next() {
    if (firstFind != null) {
      return firstFind.next();
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
