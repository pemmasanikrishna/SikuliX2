/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.sikulix.api.Element;
import com.sikulix.api.Image;
import com.sikulix.api.Target;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class Finder {

  private static final SXLog log = SX.getLogger("SX.Finder");

  private static final double downSimDiff = 0.15;

  private boolean isImage = false;
  private Image image = null;

  private boolean isRegion = true;
  private Element region = null;
  private int offX = 0, offY = 0;

  private boolean isMat = false;

  private Mat base = new Mat();

  public enum FindType {
    ONE, ALL, VANISH, ANY, BEST
  }

  private static class Probe {

    public Target pattern = null;
    public double similarity = 0;
    public double downSim = 0;
    public Image img = null;
    public Mat mat = null;
    public Element lastSeen = null;
    public double lastSeenScore = 0;

    private boolean valid = false;

    public Probe(Target pattern) {
      if (pattern.isValid()) {
        this.pattern = pattern;
        similarity = pattern.getScore();
        downSim = ((int) ((similarity - downSimDiff) * 100)) / 100.0;
        mat = pattern.getContent();
        if (null != pattern.getLastMatch()) {
          lastSeen = pattern.getLastMatch();
          lastSeenScore = lastSeen.getScore();
        }
      }
    }

    public boolean isValid() {
      return valid;
    }
  }

  public static class Found implements Iterator<Element> {

    public String name = "";
    public boolean success = false;
    public FindType type = FindType.ONE;

    public Element region = null;
    private int baseX = 0;
    private int baseY = 0;
    public Image image = null;
    public boolean inRegion = true;

    public Mat base = null;

    public ObserveEvent[] events = null;
    public Target pattern = null;
    public Target[] patterns = null;
    public long timeout = 0;
    public long elapsed = -1;
    public Element match = null;
    private Element[] matches = null;

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
        width = pattern.w;
        height = pattern.h;
        givenScore = pattern.getScore();
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
      if (currentScore > pattern.getScore() && currentScore  > firstScore - scoreMaxDiff) {
        success = true;
      }
      if (withTrace) {
        log.trace("hasNext: %.4f (%d, %d)", currentScore, baseX + currentX, baseY + currentY);
      }
      return success;
    }

    @Override
    public synchronized Element next() {
      Element match = null;
      if (hasNext(false)) {
        match = new Element(new Element(baseX + currentX, baseY + currentY, width, height), currentScore);
        int newX = Math.max(currentX - margin, 0);
        int newY = Math.max(currentY - margin, 0);
        int newXX = Math.min(newX + 2 * margin, result.cols());
        int newYY = Math.min(newY + 2 * margin, result.rows());
        result.colRange(newX, newXX).rowRange(newY, newYY).setTo(new Scalar(0f));
        mRes = null;
      }
      log.trace("next: %s", match == null ? "no match" : match);
      return match;
    }

    public Element[] getMatches() {
      if (matches == null) {
        if (hasNext()) {
          List<Element> listMatches = new ArrayList<Element>();
          while (hasNext(false)) {
            listMatches.add(next());
          }
          return (Element []) listMatches.toArray();
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
      String inWhatJSON = !inRegion ? image.toString() : region.toString();
      String[] nameParts = name.split("_");
      String found = String.format(template, nameParts[0], nameParts[1], elapsed,
          pattern, inWhat, inWhatJSON, match);
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
      base = img.getContent();
      isRegion = false;
    } else {
      log.error("init: invalid image: %s", img);
    }
  }

  public Finder(SXElement elem) {
    if (!SX.isNull(elem) && elem.isRectangle()) {
      region = (Element) elem;
      offX = region.x;
      offY = region.y;
    } else {
      log.error("init: invalid region: %s", elem);
    }
  }

  protected Finder(Mat base) {
    if (base != null) {
      image = new Image(base);
      this.base = base;
    } else {
      log.error("init: invalid CV-Mat: %s", base);
    }
  }

  public void setIsMultiFinder() {
    log.terminate(1, "TODO setIsMultiFinder()");
  }

  protected void setBase(BufferedImage bImg) {
    log.terminate(1, "TODO setBase(BufferedImage bImg)");
//    base = new Image(bImg, "").getMat();
  }

  protected void setBase(Element reg) {
    isRegion = true;
    region = reg;
    offX = region.x;
    offY = region.y;
    base = region.capture().getContent();
  }

  protected long setBase() {
    if (!isRegion) {
      return 0;
    }
    long begin_t = new Date().getTime();
    base = region.capture().getContent();
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

  public Element getRegion() {
    return region;
  }

  public Image getImage() {
    return image;
  }

  protected boolean find(Found found) {
    if (found.type.equals(FindType.ANY) || found.type.equals(FindType.BEST)) {
      findAny(found);
    } else {
      doFind(found);
    }
    return found.success;
  }

  public String findText(String text) {
    log.terminate(1, "findText: not yet implemented");
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
    Element mFound = null;
    Probe probe = new Probe(found.pattern);
    found.base = base;
    boolean isIterator = FindType.ALL.equals(found.type);
    if (isRegion && !isIterator && !useOriginal && SX.isOption("CheckLastSeen") && probe.lastSeen != null) {
      // ****************************** check last seen
      begin_t = new Date().getTime();
      Finder lastSeenFinder = new Finder(probe.lastSeen);
      lastSeenFinder.setUseOriginal();
      found.pattern = new Target(probe.pattern).similar(probe.lastSeenScore - 0.01);
      lastSeenFinder.find(found);
      if (found.match != null) {
        mFound = found.match;
        success = true;
        log.trace("doFind: checkLastSeen: success %d msec", new Date().getTime() - begin_t);
      } else {
        log.trace("doFind: checkLastSeen: not found %d msec", new Date().getTime() - begin_t);
      }
      found.pattern = probe.pattern;
    }
    if (!success) {
      if (isRegion) {
        log.trace("doFind: capture: %d msec", setBase());
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
        log.trace("doFindDown: %d msec", new Date().getTime() - begin_t);
      }
      if (!isIterator && mMinMax != null) {
        // ************************************* check after downsized success
        if (base.size().equals(probe.mat.size())) {
          // trust downsized result, if images have same size
          mFound = new Element(new Element((int) offX, (int) offY, base.width(), base.height()),mMinMax.maxVal);
          success = true;
        } else {
          int maxLocX = (int) (mMinMax.maxLoc.x * rfactor);
          int maxLocY = (int) (mMinMax.maxLoc.y * rfactor);
          begin_t = new Date().getTime();
          int margin = ((int) probe.img.getResizeFactor()) + 1;
          Rect r = new Rect(Math.max(0, maxLocX - margin), Math.max(0, maxLocY - margin),
              Math.min(probe.img.w + 2 * margin, base.width()),
              Math.min(probe.img.h + 2 * margin, base.height()));
          result = doFindMatch(probe, base.submat(r), probe.mat);
          mMinMax = Core.minMaxLoc(result);
          if (mMinMax.maxVal > probe.similarity) {
            mFound = new Element(new Element((int) mMinMax.maxLoc.x + offX + r.x, (int) mMinMax.maxLoc.y + offY + r.y,
                    probe.img.w, probe.img.h), mMinMax.maxVal);
            success = true;
          }
          log.trace("doFind: check after doFindDown %%%.2f(%%%.2f) %d msec",
              mMinMax.maxVal * 100, probe.similarity * 100, new Date().getTime() - begin_t);
        }
      }
      if (isIterator || (!success && useOriginal)) {
        // ************************************** search in original
        begin_t = new Date().getTime();
        result = doFindMatch(probe, base, probe.mat);
        mMinMax = Core.minMaxLoc(result);
        if (mMinMax != null && mMinMax.maxVal > probe.similarity) {
          mFound = new Element(new Element((int) mMinMax.maxLoc.x + offX, (int) mMinMax.maxLoc.y + offY,
              probe.img.w, probe.img.h), mMinMax.maxVal);
          success = true;
        }
        if (!useOriginal) {
          log.trace("doFind: search in original: %d msec", new Date().getTime() - begin_t);
        }
      }
    }
    if (success) {
      probe.img.setLastMatch(new Element(mFound, mFound.getScore()));
      found.match = mFound;
      if (FindType.ALL.equals(found.type)) {
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
    log.trace("findBest: enter");
    findAnyCollect(found);
    if (found.type.equals(FindType.BEST)) {
      List<Element> mList = Arrays.asList(found.getMatches());
      if (mList != null) {
        Collections.sort(mList, new Comparator<Element>() {
          @Override
          public int compare(Element m1, Element m2) {
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
    Target[] patterns = null;
    ObserveEvent[] events = null;
    boolean isEvents = false;
    if (found.patterns != null) {
      patterns = found.patterns;
      targetCount = patterns.length;
    } else if (found.events != null) {
      isEvents = true;
      events = found.events;
      targetCount = events.length;
      patterns = new Target[targetCount];
      for (int np = 0; np < targetCount; np++) {
        patterns[np] = events[np].getPattern();
      }
    } else {
      log.error("findAnyCollect: found structure invalid");
      return;
    }
    Element[] mArray = new Element[targetCount];
    SubFindRun[] theSubs = new SubFindRun[targetCount];
    int nobj = 0;
    Found subFound = null;
    for (Target pattern  : patterns) {
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
    log.trace("findAnyCollect: waiting for SubFindRuns");
    nobj = 0;
    boolean all = false;
    while (!all) {
      all = true;
      for (SubFindRun sub : theSubs) {
        all &= sub.hasFinished();
      }
    }
    log.trace("findAnyCollect: SubFindRuns finished");
    nobj = 0;
    boolean anyMatch = false;
    for (Element match : mArray) {
      if (isEvents) {
        ObserveEvent evt = events[nobj];
        evt.setMatch(match);
        evt.setActive(false);
      } else if (match != null) {
        match.setMatchIndex(nobj);
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

    Element[] mArray;
    Image base;
    Object target;
    Element reg;
    boolean finished = false;
    int subN;
    Found subFound;

    public SubFindRun(Element[] pMArray, int pSubN, Found found) {
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
        log.error("findAnyCollect: image file not found:\n", target);
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
      log.trace("(%d) %s", n++, pm);
      printMatI(pm);
    }
    log.trace("contours: %s", contours);
    printMatI(contours);
    return true;
  }

  public void setMinChanges(int min) {
    log.terminate(1, "setMinChanges");
  }

  protected static Target evalTarget(Object target) throws IOException {
    boolean findingText = false;
    Image img = null;
    Target pattern = null;
    if (target instanceof String) {
      if (((String) target).startsWith("\t") && ((String) target).endsWith("\t")) {
        findingText = true;
      } else {
        img = new Image((String) target);
        if (img.isValid()) {
          pattern = new Target(img);
        } else {
          throw new IOException("Region: doFind: Image not useable: " + target.toString());
        }
      }
      if (findingText) {
        log.terminate(1, "//TODO implement findingText");
      }
    } else if (target instanceof Target) {
      if (((Target) target).isValid()) {
        pattern = (Target) target;
      } else {
        throw new IOException("Region: doFind: Pattern not useable: " + target.toString());
      }
    } else if (target instanceof Image) {
      if (((Image) target).isValid()) {
        pattern = new Target((Image) target);
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
        log.trace("(%d, %d) %s", r, c, Arrays.toString(data));
      }
    }
  }
}
