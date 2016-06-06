/*
 * Copyright 2010-2014, Sikuli.org, sikulix.com
 * Released under the MIT License.
 *
 * modified RaiMan
 */
package org.sikuli.script;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.sikulix.core.SX;
import com.sikulix.util.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.sikuli.util.Debug;

import static com.sikulix.util.Settings.CheckLastSeen;

public class Finder {

  static RunTime runTime = RunTime.getRunTime();

  //<editor-fold defaultstate="collapsed" desc="logging">
  private static final int lvl = 3;
  private static final Logger logger = LogManager.getLogger("SX.Finder");

  private static void log(int level, String message, Object... args) {
    if (Debug.is(lvl) || level < 0) {
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

  private static void logp(String message, Object... args) {
    System.out.println(String.format(message, args));
  }

  public static void terminate(int retval, String message, Object... args) {
    logger.fatal(String.format(" *** terminating: " + message, args));
    System.exit(retval);
  }
  
  private long started = 0;
  
  private void start() {
    started = new Date().getTime();
  }

  private long end() {
    return end("");
  }

  private long end(String message) {
    long ended = new Date().getTime();
    long diff = ended - started;
    if (!message.isEmpty()) {
      logp("[time] %s: %d msec", message, diff);
    }
    started = ended;
    return diff;
  }
//</editor-fold>

  private static final double downSimDiff = 0.15;

  private boolean isImage = false;
  private Image image = null;

  private boolean isRegion = true;
  private Region region = null;
  private int offX = 0, offY = 0;

  private boolean isMat = false;

  private Mat base = new Mat();

  private static class Probe {

    public Pattern pattern = null;
    public double similarity = 0;
    public double downSim = 0;
    public Image img = null;
    public Mat mat = null;
    public Region lastSeen = null;
    public double lastSeenScore = 0;

    private boolean valid = false;

    public Probe(Pattern pattern) {
      if (pattern.isValid()) {
        this.pattern = pattern;
        similarity = pattern.getSimilar();
        downSim = ((int) ((similarity - downSimDiff) * 100)) / 100.0;
        //TODO cast to Image???
        img = new Image(pattern.getImage());
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

  public static class Found implements Iterator<Match> {

    public String name = "";
    public boolean success = false;
    public Region.FindType type = Region.FindType.ONE;

    public Region region = null;
    private int baseX = 0;
    private int baseY = 0;
    public Image image = null;
    public boolean inRegion = true;

    public Mat base = null;

    public ObserveEvent[] events = null;
    public Pattern pattern = null;
    public Pattern[] patterns = null;
    public long timeout = 0;
    public long elapsed = -1;
    public Match match = null;
    private Match[] matches = null;

    public Finder finder = null;

    public boolean isIterator = false;
    public Mat result;
    private double currentScore = -1;
    private int currentX = -1;
    private int currentY = -1;
    private int width = 0;
    private int height = 0;
    private Core.MinMaxLocResult mRes = null;
    int margin = 2;
    double givenScore = 0;
    double firstScore = 0;
    double scoreMaxDiff = 0.05;

    @Override
    public synchronized boolean hasNext() {
      return hasNext(true);
    }
    
    public synchronized boolean hasNext(boolean withTrace) {
      boolean success = false;
      if (currentScore < 0) {
        width = pattern.getImage().getW();
        height = pattern.getImage().getH();
        givenScore = pattern.getSimilar();
        if (givenScore < 0.95) {
          margin = 4;
        } else if (givenScore < 0.85) {
          margin = 8;
        } else if (givenScore < 0.71) {
          margin = 16;
        }
      }
      if (mRes == null) {
        mRes = Core.minMaxLoc(result);
        currentScore = mRes.maxVal;
        currentX = (int) mRes.maxLoc.x;
        currentY = (int) mRes.maxLoc.y;
        if (firstScore == 0) {
          firstScore = currentScore;
        }
      }
      if (currentScore > pattern.getSimilar() && currentScore  > firstScore - scoreMaxDiff) {
        success = true;
      }
      if (withTrace) {
        log(lvl + 1, "hasNext: %.4f (%d, %d)", currentScore, baseX + currentX, baseY + currentY);
      }
      return success;
    }

    @Override
    public synchronized Match next() {
      Match match = null;
      if (hasNext(false)) {
        match = new Match(new Rectangle(baseX + currentX, baseY + currentY, width, height), currentScore);
        int newX = Math.max(currentX - margin, 0);
        int newY = Math.max(currentY - margin, 0);
        int newXX = Math.min(newX + 2 * margin, result.cols());
        int newYY = Math.min(newY + 2 * margin, result.rows());
        result.colRange(newX, newXX).rowRange(newY, newYY).setTo(new Scalar(0f));
        mRes = null;
      }
      log(lvl + 1, "next: %s", match == null ? "no match" : match.toJSON());
      return match;
    }
    
    public Match[] getMatches() {
      if (matches == null) {
        if (hasNext()) {
          List<Match> listMatches = new ArrayList<Match>();
          while (hasNext(false)) {
            listMatches.add(next());
          }
          return (Match[]) listMatches.toArray();
        }
      }
      return matches;
    }

    public Found(Finder fndr) {
      finder = fndr;
      inRegion = finder.inRegion();
      if (inRegion) {
        region = finder.getRegion();
        baseX = region.x;
        baseY = region.y;
      } else {
        image = finder.getImage();
      }
    }

    public String toJSON() {
      String template = "{name:[\"%s\", \"%s\"], elapsed:%s, pattern:%s, %s:%s, match:%s}";
      String inWhat = !inRegion ? "in_image" : "in_region";
      String inWhatJSON = !inRegion ? image.toJSON(false) : region.toJSON();
      String[] nameParts = name.split("_");
      String found = String.format(template, nameParts[0], nameParts[1], elapsed,
          pattern, inWhat, inWhatJSON, match.toJSON());
      return found;
    }

    @Override
    public String toString() {
      return toJSON();
    }
    
    @Override
    public void remove() {}
  }

  protected Finder() {
  }

  public Finder(Image img) {
    if (img != null && img.isValid()) {
      image = img;
      base = img.getMat();
      isRegion = false;
    } else {
      log(-1, "init: invalid image: %s", img);
    }
  }

  public Finder(Region reg) {
    if (reg != null) {
      region = reg;
      offX = region.x;
      offY = region.y;
    } else {
      log(-1, "init: invalid region: %s", reg);
    }
  }

  protected Finder(Mat base) {
    if (base != null) {
      image = new Image(base);
      this.base = base;
    } else {
      log(-1, "init: invalid CV-Mat: %s", base);
    }
  }

  public void setIsMultiFinder() {
    terminate(1, "TODO setIsMultiFinder()");
  }

  protected void setBase(BufferedImage bImg) {
    terminate(1, "TODO setBase(BufferedImage bImg)");
//    base = new Image(bImg, "").getMat();
  }

  protected void setBase(Region reg) {
    isRegion = true;
    region = reg;
    offX = region.x;
    offY = region.y;
    base = region.captureThis().getMat();
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
  
  public boolean inRegion() {
    return isRegion;
  }
  
  public Region getRegion() {
    return region;
  }

  public Image getImage() {
    return image;
  }

  protected boolean find(Found found) {
    if (found.type.equals(Region.FindType.ANY) || found.type.equals(Region.FindType.BEST)) {
      findAny(found);
    } else {
      doFind(found);
    }
    return found.success;
  }

  public String findText(String text) {
    terminate(1, "findText: not yet implemented");
    return null;
  }

  private final double resizeMinFactor = 1.5;
  private final double[] resizeLevels = new double[]{1f, 0.4f};
  private int resizeMaxLevel = resizeLevels.length - 1;
  private double resizeMinSim = 0.8;
  private boolean useOriginal = false;

  public void setUseOriginal() {
    useOriginal = true;
  }

  private void doFind(Found found) {
    boolean success = false;
    long begin_t = 0;
    Mat result = new Mat();
    Core.MinMaxLocResult mMinMax = null;
    Match mFound = null;
    Probe probe = new Probe(found.pattern);
    found.base = base;
    boolean isIterator = Region.FindType.ALL.equals(found.type);
    if (isRegion && !isIterator && !useOriginal && Settings.CheckLastSeen && probe.lastSeen != null) {
      // ****************************** check last seen
      begin_t = new Date().getTime();
      Finder lastSeenFinder = new Finder(probe.lastSeen);
      lastSeenFinder.setUseOriginal();
      //TODO cast to Pattern???
      found.pattern = (org.sikuli.script.Pattern) new Pattern(probe.img).similar(probe.lastSeenScore - 0.01);
      lastSeenFinder.find(found);
      if (found.match != null) {
        mFound = found.match;
        success = true;
        log(lvl + 1, "doFind: checkLastSeen: success %d msec", new Date().getTime() - begin_t);
      } else {
        log(lvl + 1, "doFind: checkLastSeen: not found %d msec", new Date().getTime() - begin_t);
      }
      found.pattern = probe.pattern;
    }
    if (!success) {
      if (isRegion) {
        log(lvl + 1, "doFind: capture: %d msec", setBase());
      }
      double rfactor = 0;
      if (!isIterator && !useOriginal && probe.img.getResizeFactor() > resizeMinFactor) {
        // ************************************************* search in downsized
        begin_t = new Date().getTime();
        double imgFactor = probe.img.getResizeFactor();
        Size sb, sp;
        Mat mBase = new Mat(), mPattern = new Mat();
        result = null;
        for (double factor : resizeLevels) {
          rfactor = factor * imgFactor;
          sb = new Size(base.cols() / rfactor, base.rows() / rfactor);
          sp = new Size(probe.mat.cols() / rfactor, probe.mat.rows() / rfactor);
          Imgproc.resize(base, mBase, sb, 0, 0, Imgproc.INTER_AREA);
          Imgproc.resize(probe.mat, mPattern, sp, 0, 0, Imgproc.INTER_AREA);
          result = doFindMatch(probe, mBase, mPattern);
          mMinMax = Core.minMaxLoc(result);
          if (mMinMax.maxVal > probe.downSim) {
            break;
          }
        }
        log(lvl + 1, "doFindDown: %d msec", new Date().getTime() - begin_t);
      }
      if (!isIterator && mMinMax != null) {
        // ************************************* check after downsized success
        if (base.size().equals(probe.mat.size())) {
          // trust downsized result, if images have same size
          mFound = new Match((int) offX, (int) offY,
              base.width(), base.height(), mMinMax.maxVal);
          success = true;
        } else {
          int maxLocX = (int) (mMinMax.maxLoc.x * rfactor);
          int maxLocY = (int) (mMinMax.maxLoc.y * rfactor);
          begin_t = new Date().getTime();
          int margin = ((int) probe.img.getResizeFactor()) + 1;
          Rect r = new Rect(Math.max(0, maxLocX - margin), Math.max(0, maxLocY - margin),
              Math.min(probe.img.getWidth() + 2 * margin, base.width()),
              Math.min(probe.img.getHeight() + 2 * margin, base.height()));
          result = doFindMatch(probe, base.submat(r), probe.mat);
          mMinMax = Core.minMaxLoc(result);
          if (mMinMax.maxVal > probe.similarity) {
            mFound = new Match((int) mMinMax.maxLoc.x + offX + r.x, (int) mMinMax.maxLoc.y + offY + r.y,
                probe.img.getWidth(), probe.img.getHeight(), mMinMax.maxVal);
            success = true;
          }
          log(lvl + 1, "doFind: check after doFindDown %%%.2f(%%%.2f) %d msec",
              mMinMax.maxVal * 100, probe.similarity * 100, new Date().getTime() - begin_t);
        }
      }
      if (isIterator || (!success && useOriginal)) {
        // ************************************** search in original 
        begin_t = new Date().getTime();
        result = doFindMatch(probe, base, probe.mat);
        mMinMax = Core.minMaxLoc(result);
        if (mMinMax != null && mMinMax.maxVal > probe.similarity) {
          mFound = new Match((int) mMinMax.maxLoc.x + offX, (int) mMinMax.maxLoc.y + offY,
              probe.img.getWidth(), probe.img.getHeight(), mMinMax.maxVal);
          success = true;
        }
        if (!useOriginal) {
          log(lvl + 1, "doFind: search in original: %d msec", new Date().getTime() - begin_t);
        }
      }
    }
    if (success) {
      probe.img.setLastSeen(mFound.getRect(), mFound.getScore());
      found.match = mFound;
      if (Region.FindType.ALL.equals(found.type)) {
        found.result = result;
      }
    }
    found.success = success;
  }

  private Mat doFindMatch(Probe probe, Mat base, Mat target) {
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
    return res;
  }
    
  public void findAny(Found found) {
    log(lvl, "findBest: enter");
    findAnyCollect(found);
    if (found.type.equals(Region.FindType.BEST)) {
      List<Match> mList = Arrays.asList(found.getMatches());
      if (mList != null) {
        Collections.sort(mList, new Comparator<Match>() {
          @Override
          public int compare(Match m1, Match m2) {
            double ms = m2.getScore() - m1.getScore();
            if (ms < 0) {
              return -1;
            } else if (ms > 0) {
              return 1;
            }
            return 0;
          }
        });
        found.match = mList.get(0);
      }
    }
  }

  private void findAnyCollect(Found found) {
    int targetCount = 0;
    Pattern[] patterns = null;
    ObserveEvent[] events = null;
    boolean isEvents = false;
    if (found.patterns != null) {
      patterns = found.patterns;
      targetCount = patterns.length;
    } else if (found.events != null) {
      isEvents = true;
      events = found.events;
      targetCount = events.length;
      patterns = new Pattern[targetCount];
      for (int np = 0; np < targetCount; np++) {
        patterns[np] = events[np].getPattern();
      }
    } else {
      log(-1, "findAnyCollect: found structure invalid");
      return;
    }
    Match[] mArray = new Match[targetCount];
    SubFindRun[] theSubs = new SubFindRun[targetCount];
    int nobj = 0;
    Found subFound = null;
    for (Pattern pattern  : patterns) {
      mArray[nobj] = null;
      theSubs[nobj] = null;
      if (pattern != null) {
        subFound = new Found(this);
        subFound.pattern = pattern;
        theSubs[nobj] = new SubFindRun(mArray, nobj, subFound);
        new Thread(theSubs[nobj]).start();
      }
      nobj++;
    }
    log(lvl, "findAnyCollect: waiting for SubFindRuns");
    nobj = 0;
    boolean all = false;
    while (!all) {
      all = true;
      for (SubFindRun sub : theSubs) {
        all &= sub.hasFinished();
      }
    }
    log(lvl, "findAnyCollect: SubFindRuns finished");
    nobj = 0;
    boolean anyMatch = false;
    for (Match match : mArray) {
      if (isEvents) {
        ObserveEvent evt = events[nobj];
        evt.setMatch(match);
        evt.setActive(false);
      } else if (match != null) {
        match.setIndex(nobj);
        anyMatch = true;
      }
      nobj++;
    }
    if (!isEvents) {
      found.matches = mArray;
      found.success = anyMatch;
    }
  }

  private class SubFindRun implements Runnable {

    Match[] mArray;
    Image base;
    Object target;
    Region reg;
    boolean finished = false;
    int subN;
    Found subFound;

    public SubFindRun(Match[] pMArray, int pSubN, Found found) {
      subN = pSubN;
      mArray = pMArray;
      subFound = found;
    }

    @Override
    public void run() {
      try {
        doFind(subFound);
        mArray[subN] = null;
        if (subFound.success && subFound.match != null) {
          mArray[subN] = subFound.match;
        }
      } catch (Exception ex) {
        log(-1, "findAnyCollect: image file not found:\n", target);
      }
      hasFinished(true);
    }

    public boolean hasFinished() {
      return hasFinished(false);
    }

    public synchronized boolean hasFinished(boolean state) {
      if (state) {
        finished = true;
      }
      return finished;
    }
  }

  private static Rect getSubMatRect(Mat mat, int x, int y, int w, int h, int margin) {
    x = Math.max(0, x - margin);
    y = Math.max(0, y - margin);
    w = Math.min(w + 2 * margin, mat.width() - x);
    h = Math.min(h + 2 * margin, mat.height() - y);
    return new Rect(x, y, w, h);
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

  protected static Pattern evalTarget(Object target) throws IOException {
    boolean findingText = false;
    Image img = null;
    Pattern pattern = null;
    if (target instanceof String) {
      if (((String) target).startsWith("\t") && ((String) target).endsWith("\t")) {
        findingText = true;
      } else {
        img = Image.get((String) target);
        if (img.isUseable()) {
          pattern = new Pattern(img);
        } else if (img.isText()) {
          findingText = true;
        } else {
          throw new IOException("Region: doFind: Image not useable: " + target.toString());
        }
      }
      if (findingText) {
        if (TextRecognizer.getInstance() != null) {
          //TODO text Pattern ???
          pattern = new Pattern((String) target);
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
