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


}
