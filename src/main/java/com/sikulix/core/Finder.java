/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.sikulix.api.Element;
import com.sikulix.api.Image;
import com.sikulix.api.Target;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.util.*;
import java.util.List;

public class Finder {

  private static final SXLog log = SX.getLogger("SX.Finder");

  private static final double downSimDiff = 0.15;

  private Element baseElement = null;
  private Mat base = new Mat();
  private Mat result = new Mat();

  private Finder() {
  }

  public Finder(Element elem) {
    if (elem != null && elem.isValid()) {
      baseElement = elem;
      base = elem.getContent();
    } else {
      log.error("init: invalid element: %s", elem);
    }
  }

  private enum FindType {
    ONE, ALL
  }

  public boolean isValid() {
    return !base.empty();
  }

  public Element find(Element target) {
    FindResult findResult = doFind(target, FindType.ONE);
    if (SX.isNotNull(findResult) && findResult.hasNext()) {
      baseElement.setLastMatch(findResult.next());
      return baseElement.getLastMatch();
    }
    return new Element();
  }

  public List<Element> findAll(Element target) {
    FindResult findResult = doFind(target, FindType.ALL);
    List<Element> matches = findResult.getMatches();
    Collections.sort(matches);
    Collections.sort(matches);
    baseElement.setLastMatches(matches);
    baseElement.setLastScores(findResult.getScores());
    return matches;
  }

  public Element findBest(List<Element> targets) {
    List<Element> mList = findAny(targets);
    if (mList != null) {
      if (mList.size() > 1) {
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
      }
      return mList.get(0);
    }
    return null;
  }

  public List<Element> findAny(List<Element> targets) {
    List<Element> mList = findAnyCollect(targets);
    return mList;
  }


  private final double resizeMinFactor = 1.5;
  private final double[] resizeLevels = new double[]{1f, 0.4f};
  private int resizeMaxLevel = resizeLevels.length - 1;
  private double resizeMinSim = 0.8;
  private boolean isCheckLastSeen = false;

  private FindResult doFind(Element elem, FindType findType) {
    log.on(SXLog.TRACE);
    if (!elem.isTarget()) {
      return null;
    }
    Element target = elem;
    if (target.getWantedScore() < 0) {
      target.setWantedScore(0.8);
    }
    boolean success = false;
    long begin_t = 0;
    Core.MinMaxLocResult mMinMax = null;
    FindResult findResult = null;
    if (FindType.ONE.equals(findType) && !isCheckLastSeen && SX.isOption("CheckLastSeen") && target.getLastSeen().isValid()) {
      begin_t = new Date().getTime();
      Finder lastSeenFinder = new Finder(target.getLastSeen());
      lastSeenFinder.isCheckLastSeen = true;
      target = new Target(target, target.getLastSeen().getScore() - 0.01);
      findResult = lastSeenFinder.doFind(target, FindType.ONE);
      if (findResult.hasNext()) {
        log.trace("doFind: checkLastSeen: success %d msec", new Date().getTime() - begin_t);
        return findResult;
      } else {
        log.trace("doFind: checkLastSeen: not found %d msec", new Date().getTime() - begin_t);
      }
    }
    double rfactor = 0;
    if (FindType.ONE.equals(findType) && target.getResizeFactor() > resizeMinFactor) {
      // ************************************************* search in downsized
      begin_t = new Date().getTime();
      double imgFactor = target.getResizeFactor();
      Size sb, sp;
      Mat mBase = new Mat(), mPattern = new Mat();
      result = null;
      for (double factor : resizeLevels) {
        rfactor = factor * imgFactor;
        sb = new Size(base.cols() / rfactor, base.rows() / rfactor);
        sp = new Size(target.getContent().cols() / rfactor, target.getContent().rows() / rfactor);
        Imgproc.resize(base, mBase, sb, 0, 0, Imgproc.INTER_AREA);
        Imgproc.resize(target.getContent(), mPattern, sp, 0, 0, Imgproc.INTER_AREA);
        result = doFindMatch(target, mBase, mPattern);
        mMinMax = Core.minMaxLoc(result);
        double wantedScore = ((int) ((target.getWantedScore() - downSimDiff) * 100)) / 100.0;
        if (mMinMax.maxVal > wantedScore) {
          break;
        }
      }
      log.trace("doFind: down: %.2f%% %d msec", mMinMax.maxVal, new Date().getTime() - begin_t);
    }
    if (FindType.ONE.equals(findType) || mMinMax != null) {
      // ************************************* check after downsized success
      if (base.size().equals(target.getContent().size())) {
        // trust downsized result, if images have same size
        return new FindResult(result, target);
      } else {
        int maxLocX = (int) (mMinMax.maxLoc.x * rfactor);
        int maxLocY = (int) (mMinMax.maxLoc.y * rfactor);
        begin_t = new Date().getTime();
        int margin = ((int) target.getResizeFactor()) + 1;
        Rect rectSub = new Rect(Math.max(0, maxLocX - margin), Math.max(0, maxLocY - margin),
                Math.min(target.w + 2 * margin, base.width()),
                Math.min(target.h + 2 * margin, base.height()));
        result = doFindMatch(target, base.submat(rectSub), null);
        mMinMax = Core.minMaxLoc(result);
        if (mMinMax.maxVal > target.getWantedScore()) {
          findResult = new FindResult(result, target, new int[]{rectSub.x, rectSub.y});
        }
        if (SX.isNotNull(findResult)) {
          log.trace("doFind: after down: %.2f%%(%.2f%%) %d msec",
                  mMinMax.maxVal * 100, target.getWantedScore() * 100, new Date().getTime() - begin_t);
          return findResult;
        }
      }
    }
    // ************************************** search in original
    begin_t = new Date().getTime();
    result = doFindMatch(target, base, null);
    mMinMax = Core.minMaxLoc(result);
    if (!isCheckLastSeen || FindType.ALL.equals(findType)) {
      log.trace("doFind: search in original: %d msec", new Date().getTime() - begin_t);
    }
    if (mMinMax.maxVal > target.getWantedScore()) {
      return new FindResult(result, target);
    }
    return null;
  }

  private Mat doFindMatch(Element target, Mat base, Mat probe) {
    Mat result = new Mat();
    Mat plainBase = base;
    Mat plainProbe = probe;
    if (SX.isNull(probe)) {
      probe = target.getContent();
    }
    if (!target.isPlainColor()) {
      Imgproc.matchTemplate(base, probe, result, Imgproc.TM_CCOEFF_NORMED);
    } else {
      if (target.isBlack()) {
        Core.bitwise_not(base, plainBase);
        Core.bitwise_not(probe, plainProbe);
      }
      Imgproc.matchTemplate(plainBase, plainProbe, result, Imgproc.TM_SQDIFF_NORMED);
      Core.subtract(Mat.ones(result.size(), CvType.CV_32F), result, result);
    }
    return result;
  }

  private List<Element> findAnyCollect(List<Element> targets) {
    int targetCount = 0;
    if (SX.isNull(targets)) {
      return null;
    } else {
      targetCount = targets.size();
    }
    List<Element> matches = new ArrayList<>();
    SubFindRun[] theSubs = new SubFindRun[targetCount];
    int nobj = 0;
    for (Element target : targets) {
      matches.add(null);
      theSubs[nobj] = null;
      if (target != null) {
        theSubs[nobj] = new SubFindRun(matches, nobj, target);
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
    for (Element match : matches) {
      if (match != null) {
        match.setMatchIndex(nobj);
      }
      nobj++;
    }
    return matches;
  }

  private class SubFindRun implements Runnable {

    List<Element> matches;
    boolean finished = false;
    int subN;
    Element target;

    public SubFindRun(List<Element> matches, int pSubN, Element target) {
      subN = pSubN;
      this.matches = matches;
      this.target = target;
    }

    @Override
    public void run() {
      try {
        Element match = find(target);
        matches.set(subN, null);
        if (SX.isNotNull(match)) {
          matches.set(subN, match);
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

  private static void printMatI(Mat mat) {
    int[] data = new int[mat.channels()];
    for (int r = 0; r < mat.rows(); r++) {
      for (int c = 0; c < mat.cols(); c++) {
        mat.get(r, c, data);
        log.trace("(%d, %d) %s", r, c, Arrays.toString(data));
      }
    }
  }

  public void setMinChanges(int min) {
    log.terminate(1, "setMinChanges");
  }

  protected static Target evalTarget(Object target) throws IOException {
    //TODO evalTarget(Object target)
    return null;
  }
}
