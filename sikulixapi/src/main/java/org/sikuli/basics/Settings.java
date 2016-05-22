/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package org.sikuli.basics;

public class Settings {
  //<editor-fold desc="*** public features from class Settings (deprecated - now Options)">
  public static String BundlePath = null;
  public static String OcrDataPath = null;
  public static boolean OcrTextSearch = false;
  public static boolean OcrTextRead = false;
  public static String OcrLanguage = "eng";

  public static final float FOREVER = Float.POSITIVE_INFINITY;
  public static boolean TRUE = true;
  public static boolean FALSE = false;

  public static boolean ThrowException = true; // throw FindFailed exception
  public static float AutoWaitTimeout = 3f; // in seconds
  public static float WaitScanRate = 3f; // frames per second
  public static float ObserveScanRate = 3f; // frames per second
  public static int ObserveMinChangedPixels = 50; // in pixels
  public static int RepeatWaitTime = 1; // wait 1 second for visual to vanish after action
  public static double MinSimilarity = 0.7;
  public static boolean CheckLastSeen = true;

  public static double DelayValue = 0.3;
  public static double DelayBeforeMouseDown = DelayValue;
  public static double DelayBeforeDrag = DelayValue;
  public static double DelayBeforeDrop = DelayValue;

  /**
   * Specify a delay between the key presses in seconds as 0.nnn. This only
   * applies to the next type and is then reset to 0 again. A value &gt; 1 is cut
   * to 1.0 (max delay of 1 second)
   */
  public static double TypeDelay = 0.0;

  /**
   * Specify a delay between the mouse down and up in seconds as 0.nnn. This
   * only applies to the next click action and is then reset to 0 again. A value
   * &gt; 1 is cut to 1.0 (max delay of 1 second)
   */
  public static double ClickDelay = 0.0;

  /**
   * true = start slow motion mode, false: stop it (default: false) show a
   * visual for SlowMotionDelay seconds (default: 2)
   */
  public static boolean ShowActions = false;

  public static float SlowMotionDelay = 2.0f; // in seconds
  public static float MoveMouseDelay = 0.5f; // in seconds

  /**
   * true = highlight every match (default: false) (show red rectangle around)
   * for DefaultHighlightTime seconds (default: 2)
   */
  public static boolean Highlight = false;
  public static float DefaultHighlightTime = 2f;
  public static float WaitAfterHighlight = 0.3f;

  public static boolean ActionLogs = true;
  public static boolean InfoLogs = true;
  public static boolean DebugLogs = false;
  public static boolean ProfileLogs = false;

  public static boolean LogTime = false;
  public static boolean UserLogs = true;
  public static String UserLogPrefix = "user";
  public static boolean UserLogTime = true;
  //</editor-fold>
}
