/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import org.opencv.core.Mat;

import java.awt.image.BufferedImage;

public class Target extends Image {

  private static eType eClazz = eType.TARGET;
  private static SXLog log = SX.getLogger("SX." + eClazz.toString());

  //<editor-fold desc="***** construct">
  public Target() {
  }

  protected void setClazz() {
    clazz = eClazz;
  }

  protected void copy(Element elem) {
    super.copy(elem);
  }

  protected void initAfter() {
    initName(eClazz);
  }

  public Target(BufferedImage bimg) {
    super(bimg);
  }

  public Target(Mat mat) {
    super(mat);
  }

  public Target(String fpImage) {
    super(fpImage);
  }

  public Target(Target pat) {
    super(pat);
  }

  public Target(Image img) {
    super(img);
  }

  public Target(Element elem, double score) {
    super(elem, score);
  }

  public Target(Element elem, double score, Element off) {
    super(elem, score, off);
  }

  public Target(Element elem, Element off) {
    super(elem, off);
  }
  //</editor-fold>

  //<editor-fold desc="***** set, get">
  public void similar(double score) {
    setWantedScore(score);
  }

  public double getSimilar() {
    return getWantedScore();
  }

  /**
   * sets the minimum wanted similarity score to the value which means exact match (default 0.99)
   *
   * @return the Pattern object itself
   */
  public Target exact() {
    setScore(exactAs);
    return this;
  }

  public static void setExactAs(double minimumScore) {
    exactAs = minimumScore;
  }

  public static double getExactAs() {
    return exactAs;
  }

  private static double exactAs = 0.99f;
  //</editor-fold>
}
