/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package org.sikuli.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

public abstract class SX {

  private static SXGlobal sxGlobal = null;

  // ******************************* Logging ****************************
  protected static int lvl;
  private static final int debugLvl = 3;
  private static final int stdLvl = debugLvl;
  private int currentLvl = 0;

  private Logger logger;

  public void setLogger(String cls, int level) {
    lvl = level;
    logger = LogManager.getLogger("SX." + cls);
    if (null == sxGlobal) {
      sxGlobal = SXGlobal.getInstance();
    }
  }

  public void setLogger(String cls) {
    setLogger(cls, stdLvl);
  }

  public boolean isLvl(int lvl) {
    return lvl < 0 || (currentLvl > 0 && lvl >= currentLvl);
  }

  public void logOff() {
    currentLvl = 0;
  }

  public void logOn(int level) {
    currentLvl = level;
  }

  public void debugOn() {
    currentLvl = debugLvl;
  }

  public void log(int level, String message, Object... args) {
    if (isLvl(level)) {
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

  public void logp(String message, Object... args) {
    log(1, message, args);
  }

  public void terminate(int retval, String message, Object... args) {
    logger.fatal(String.format(" *** terminating: " + message, args));
    System.exit(retval);
  }

  // ******************************* global init ****************************
  public static boolean runningScripts = false;
  public static boolean runningInteractive = false;

  public static String NL = "\n";

  public static boolean isIDE() {
    return false;
  }

  public static File isRunning = null;
  public static FileOutputStream isRunningFile = null;
  public static final String isRunningFilename = "s_i_k_u_l_i-ide-isrunning";

  public static File fTempPath = null;
  public static File fSXTempPath = null;
  public static String fpSXTempPath = "";

  // ******************************* System/Java ****************************

  static String userName = "";
  static File fUserHome = null;
  static File fWorkDir = null;

  static final String javahome = new File(System.getProperty("java.home")).getAbsolutePath();

  static int javaArch = 64;
  static int javaVersion = 7;
  static String javaShow = "not-set";

  public static boolean isJava8() {
    return javaVersion > 7;
  }

  static final String osNameSysProp = System.getProperty("os.name");
  static final String osVersionSysProp = System.getProperty("os.version");

  static String osName = "NotKnown";
  static String sysName = "NotKnown";
  static String osVersion = "";

  static boolean runningWindows = false;
  static boolean runningWinApp = false;

  static boolean runningMac = false;
  static boolean runningMacApp = false;

  static boolean runningLinux = false;
  static String linuxDistro = "???LINUX???";

  static enum theSystem {

    WIN, MAC, LUX, FOO
  }
  static theSystem runningOn = theSystem.FOO;

  // ******************************* App Data Path ****************************

  static File fSysAppPath = null;
  static File fSXAppPath = new File("???UNKNOWN???");
  static String appDataMsg = "";
  static File fSXDownloads = null;
  static File fSXEditor = null;
  static File fSXExtensions = null;
  static String[] theExtensions = new String[]{"selenium4sikulix"};
  static File fSXLib = null;
  static File fSXNative = null;
  public static File fSXStore = null;
  public static File fSXTesseract = null;

  static File asExtension(String fpPath) {
    File fPath = new File(fSXExtensions, fpPath);
    return fPath;
  }

  /**
   * print the current java system properties key-value pairs sorted by key
   */
  public static void dumpSysProps() {
    dumpSysProps(null);
  }

  /**
   * print the current java system properties key-value pairs sorted by key but only keys containing filter
   *
   * @param filter the filter string
   */
  public static void dumpSysProps(String filter) {
    filter = filter == null ? "" : filter;
    sxGlobal.logp("*** system properties dump " + filter);
    Properties sysProps = System.getProperties();
    ArrayList<String> keysProp = new ArrayList<String>();
    Integer nL = 0;
    String entry;
    for (Object e : sysProps.keySet()) {
      entry = (String) e;
      if (entry.length() > nL) {
        nL = entry.length();
      }
      if (filter.isEmpty() || !filter.isEmpty() && entry.contains(filter)) {
        keysProp.add(entry);
      }
    }
    Collections.sort(keysProp);
    String form = "%-" + nL.toString() + "s = %s";
    for (Object e : keysProp) {
      sxGlobal.logp(form, e, sysProps.get(e));
    }
    sxGlobal.logp("*** system properties dump end" + filter);
  }

  static GraphicsEnvironment genv = null;
  static GraphicsDevice[] gdevs;
  static Region[] monitorBounds = null;
  static Region rAllMonitors;
  static int mainMonitor = -1;
  static int nMonitors = 0;

  /**
   * checks, whether Java runs with a valid GraphicsEnvironment (usually means real screens connected)
   *
   * @return false if Java thinks it has access to screen(s), true otherwise
   */
  public static boolean isHeadless() {
    return GraphicsEnvironment.isHeadless();
  }

  public static Region getMonitor(int n) {
    if (isHeadless()) {
      return new Region();
    }
    n = (n < 0 || n >= nMonitors) ? mainMonitor : n;
    return monitorBounds[n];
  }

  public static GraphicsDevice getGraphicsDevice(int id) {
    return gdevs[id];
  }

  public static String getBundlePath() {
    sxGlobal.log(-1,"//TODO getBundlePath: not implemented");
    return "//TODO not implemented";
  }

  public static void addImagePath(String fpMain, String fpSub) {
    sxGlobal.log(-1,"//TODO addImagePath: not implemented");
  }

  public static void addImagePath(String fpMain) {
    addImagePath(fpMain, null);
  }

  public static boolean addClassPath(String fpPath) {
    sxGlobal.log(-1, "addClassPath: not implemented");
    return true;
  }

  public static void cleanUp(int n) {
    sxGlobal.log(lvl, "cleanUp: %d", n);
//    ScreenHighlighter.closeAll();
//    Observer.cleanUp();
//    Mouse.reset();
//    Screen.getPrimaryScreen().getRobot().keyUp();
//    HotkeyManager.reset();
  }
}
