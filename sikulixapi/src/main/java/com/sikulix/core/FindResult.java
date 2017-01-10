/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.sikulix.api.Element;
import com.sikulix.api.Target;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Range;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FindResult implements Iterator<Element> {

  private static final SXLog log = SX.getLogger("SX.FindResult");

  private FindResult() {
  }

  public FindResult(Mat result, Element target) {
    this.result = result;
    this.target = target;
  }

  public FindResult(Mat result, Element target, int[] off) {
    this(result, target);
    offX = off[0];
    offY = off[1];
  }

  public String name = "";
//  public boolean success = false;

  private Element target = null;
  private Mat result = null;
  private Core.MinMaxLocResult resultMinMax = null;
  private int offX = 0;
  private int offY = 0;

  private double currentScore = -1;
  double firstScore = -1;
  double scoreMaxDiff = 0.005;

  private int currentX = -1;
  private int currentY = -1;

//  public synchronized boolean hasNext_old() {
//    boolean success = false;
//    if (currentScore < 0) {
//      width = target.w;
//      height = target.h;
//      givenScore = target.getScore();
//      if (givenScore < 0.95) {
//        margin = 4;
//      } else if (givenScore < 0.85) {
//        margin = 8;
//      } else if (givenScore < 0.71) {
//        margin = 16;
//      }
//    }
//    if (resultMinMax == null) {
//      resultMinMax = Core.minMaxLoc(result);
//      currentScore = resultMinMax.maxVal;
//      currentX = (int) resultMinMax.maxLoc.x;
//      currentY = (int) resultMinMax.maxLoc.y;
//      if (firstScore == 0) {
//        firstScore = currentScore;
//      }
//    }
//    if (currentScore > target.getScore() && currentScore > firstScore - scoreMaxDiff) {
//      success = true;
//    }
//    return success;
//  }

  public synchronized boolean hasNext() {
    resultMinMax = Core.minMaxLoc(result);
    currentScore = resultMinMax.maxVal;
    currentX = (int) resultMinMax.maxLoc.x;
    currentY = (int) resultMinMax.maxLoc.y;
    if (firstScore < 0) {
      firstScore = currentScore;
    }
    if (currentScore > target.getScore() && currentScore > firstScore - scoreMaxDiff) {
      return true;
    }
    return false;
  }

//  public synchronized Element next_old() {
//    Element match = null;
//    if (hasNext()) {
//      match = new Element(new Element(currentX + offX , currentY + offY, width, height), currentScore);
//      int newX = Math.max(currentX - margin, 0);
//      int newY = Math.max(currentY - margin, 0);
//      int newXX = Math.min(newX + 2 * margin, result.cols());
//      int newYY = Math.min(newY + 2 * margin, result.rows());
//      result.colRange(newX, newXX).rowRange(newY, newYY).setTo(new Scalar(0f));
//      resultMinMax = null;
//    }
//    return match;
//  }

  public synchronized Element next() {
    Element match = null;
    if (hasNext()) {
      match = new Element(new Element(currentX + offX , currentY + offY, target.w, target.h), currentScore);
      int margin = getPurgeMargin();
      Range rangeX = new Range(Math.max(currentX - margin, 0), currentX + 1);
      Range rangeY = new Range(Math.max(currentY - margin, 0), currentY +1);
      result.colRange(rangeX).rowRange(rangeY).setTo(new Scalar(0f));
    }
    return match;
  }

  private int getPurgeMargin() {
    if (currentScore < 0.95) {
      return 4;
    } else if (currentScore < 0.85) {
      return 8;
    } else if (currentScore < 0.71) {
      return 16;
    }
    return 2;
  }

  public List<Element> getMatches() {
    if (hasNext()) {
      List<Element> listMatches = new ArrayList<Element>();
      while (hasNext()) {
        listMatches.add(next());
      }
      return listMatches;
    }
    return null;
  }

  @Override
  public void remove() {
  }
}
