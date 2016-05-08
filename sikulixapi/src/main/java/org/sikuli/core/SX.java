/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package org.sikuli.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sikuli.script.App;
import org.sikuli.script.Key;
import org.sikuli.util.SysJNA;
import org.sikuli.util.hotkey.HotkeyListener;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public abstract class SX {

  public static SXGlobal sxGlobal = null;

  public void sxinit(String[] args) {
    if (null != args) {
      //TODO process args
    }
    if (null == sxGlobal) {
      SXGlobal.getInstance();
    }
  }

  // ******************************* Logging ****************************
  protected static int lvl;
  private static final int stdLvl = 3;
  private int currentLogLevel = 0;
  private static int globalLogLevel = 0;
  private static boolean logError = true;

  private Logger logger;

  public void setLogger(String cls, int level) {
    lvl = level;
    logger = LogManager.getLogger("SX." + cls);
  }

  public void setLogger(String cls) {
    setLogger(cls, stdLvl);
  }

  public boolean isLvl(int level) {
    if (level < 0) {
      return logError;
    }
    if (globalLogLevel > 0 && globalLogLevel >= level) {
      return true;
    }
    if (currentLogLevel > 0 && currentLogLevel >= level) {
      return true;
    }
    return false;
  }

  public void logOff() {
    currentLogLevel = 0;
    globalLogLevel = 0;
  }

  public void logOffError() {
    logError = false;
  }

  public void logOnError() {
    logError = true;
  }

  public void logOn(int level) {
    currentLogLevel = level;
  }

  public void logOnGlobal(int level) {
    globalLogLevel = level;
  }

  public void log(int level, String message, Object... args) {
    if (isLvl(level)) {
      message = String.format(message, args).replaceAll("\\n", " ");
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

  // ******************************* Visual variants ****************************
  public enum clazzType {
    REGION, LOCATION, IMAGE, SCREEN, MATCH, PATTERN
  }

  protected static clazzType clazz;

  public boolean isRegion() {
    return clazzType.REGION.equals(clazz);
  }

  public boolean isRectangle() {
    return isRegion();
  }

  public boolean isLocation() {
    return clazzType.LOCATION.equals(clazz);
  }

  public boolean isPoint() {
    return isLocation();
  }

  public boolean isImage() {
    return clazzType.IMAGE.equals(clazz);
  }

  public boolean isMatch() {
    return clazzType.MATCH.equals(clazz);
  }

  public boolean isPattern() {
    return clazzType.PATTERN.equals(clazz);
  }

  public boolean isScreen() {
    return clazzType.SCREEN.equals(clazz);
  }

  // ******************************* globals ****************************
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
  public static File fUserHome = null;
  public static File fWorkDir = null;

  public static final String javahome = new File(System.getProperty("java.home")).getAbsolutePath();

  public static int javaArch = 64;
  public static int javaVersion = 7;
  static String javaShow = "?javaShow?";

  public static boolean isJava8() {
    return javaVersion > 7;
  }

  public static boolean isJava7() {
    return javaVersion > 6;
  }

  static final String osNameSysProp = System.getProperty("os.name");
  static final String osVersionSysProp = System.getProperty("os.version");

  static String osName = "?osName?";
  static String sysName = "?sysName?";
  static String osVersion = "";
  static String osShow = "?osShow?";

  public static String getSystem() {
    return osShow;
  }

  public static String getSystemVersion() {
    return osVersion;
  }

  public static boolean isOSX10() {
    return osVersion.startsWith("10.10.") || osVersion.startsWith("10.11.");
  }

  static boolean runningWindows = false;
  public static boolean runningWinApp = false;

  static boolean runningMac = false;
  public static boolean runningMacApp = false;

  static boolean runningLinux = false;
  static String linuxDistro = "???LINUX???";

  static enum theSystem {

    WIN, MAC, LUX, FOO
  }

  static theSystem runningOn = theSystem.FOO;

  // ******************************* App Data Path ****************************

  static File fSysAppPath = null;
  public static File fSXAppPath = new File("???UNKNOWN???");

  public static String getAppDataPath() {
    return fSXAppPath.getAbsolutePath();
  }

  static String appDataMsg = "";
  static File fSXDownloads = null;
  static File fSXEditor = null;
  static File fSXExtensions = null;
  static String[] theExtensions = new String[]{"selenium4sikulix"};
  static File fSXLib = null;
  public static File fSXStore = null;
  static File fSXTesseract = null;
  public static File fSXNative = null;

  public static File asExtension(String fpJar) {
    File fJarFound = new File(ContentManager.normalizeAbsolute(fpJar, false));
    if (!fJarFound.exists()) {
      String fpCPEntry = isOnClasspath(fJarFound.getName());
      if (fpCPEntry == null) {
        fJarFound = new File(fSXExtensions, fpJar);
        if (!fJarFound.exists()) {
          fJarFound = new File(fSXLib, fpJar);
          if (!fJarFound.exists()) {
            fJarFound = null;
          }
        }
      } else {
        fJarFound = new File(fpCPEntry, fJarFound.getName());
      }
    } else {
      return null;
    }
    return fJarFound;
  }

  // ******************************* Native ****************************

  public static File fLibsProvided;
  public static boolean useLibsProvided;
  public static String linuxNeededLibs = "";
  public static String linuxAppSupport = "";
  static boolean areLibsExported = false;
  static String fpJarLibs = null;
  static Map<String, Boolean> libsLoaded = new HashMap<String, Boolean>();

  static String sfLibsCheckFileLoaded = "MadeForSikuliX_%s";
  static String sflibsCheckFileStored = "MadeForSikuliX2";
  static String sxLibsCheckName;
  public static String sfLibOpencvJava = "_ext_opencv_java";
  public static String sfLibJXGrabKey = "_ext_JXGrabKey";
  public static String sfLibJIntellitype = "_ext_JIntellitype";
  public static String sfLibWinUtil = "_ext_WinUtil";
  public static String sfLibMacUtil = "_ext_MacUtil";
  public static String sfLibMacHotkey = "_ext_MacHotkeyManager";

  static class LibsFilter implements FilenameFilter {

    String sAccept = "";

    public LibsFilter(String toAccept) {
      sAccept = toAccept;
    }

    @Override
    public boolean accept(File dir, String name) {
      if (dir.getPath().contains(sAccept)) {
        return true;
      }
      return false;
    }
  }

  static void addToWindowsSystemPath(File fLibsFolder) {
    String syspath = SysJNA.WinKernel32.getEnvironmentVariable("PATH");
    if (syspath == null) {
      sxGlobal.terminate(1, "addToWindowsSystemPath: cannot access system path");
    } else {
      String libsPath = (fLibsFolder.getAbsolutePath()).replaceAll("/", "\\");
      if (!syspath.toUpperCase().contains(libsPath.toUpperCase())) {
        if (!SysJNA.WinKernel32.setEnvironmentVariable("PATH", libsPath + ";" + syspath)) {
          sxGlobal.terminate(999, "", "");
        }
        syspath = SysJNA.WinKernel32.getEnvironmentVariable("PATH");
        if (!syspath.toUpperCase().contains(libsPath.toUpperCase())) {
          sxGlobal.terminate(1, "addToWindowsSystemPath: did not work: %s", syspath);
        }
        sxGlobal.log(lvl, "addToWindowsSystemPath: added: %s", libsPath);
      }
    }
  }

  static boolean checkJavaUsrPath(File fLibsFolder) {
    String fpLibsFolder = fLibsFolder.getAbsolutePath();
    Field usrPathsField = null;
    boolean contained = false;
    try {
      usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
    } catch (NoSuchFieldException ex) {
      sxGlobal.log(-1, "checkJavaUsrPath: get (%s)", ex);
    } catch (SecurityException ex) {
      sxGlobal.log(-1, "checkJavaUsrPath: get (%s)", ex);
    }
    if (usrPathsField != null) {
      usrPathsField.setAccessible(true);
      try {
        //get array of paths
        String[] javapaths = (String[]) usrPathsField.get(null);
        //check if the path to add is already present
        for (String p : javapaths) {
          if (new File(p).equals(fLibsFolder)) {
            contained = true;
            break;
          }
        }
        //add the new path
        if (!contained) {
          final String[] newPaths = Arrays.copyOf(javapaths, javapaths.length + 1);
          newPaths[newPaths.length - 1] = fpLibsFolder;
          usrPathsField.set(null, newPaths);
          sxGlobal.log(lvl, "checkJavaUsrPath: added to ClassLoader.usrPaths");
          contained = true;
        }
      } catch (IllegalAccessException ex) {
        sxGlobal.log(-1, "checkJavaUsrPath: set (%s)", ex);
      } catch (IllegalArgumentException ex) {
        sxGlobal.log(-1, "checkJavaUsrPath: set (%s)", ex);
      }
      return contained;
    }
    return false;
  }

  // ******************************* Sikulix ****************************

  static String sxVersion;

  public static String getVersion() {
    return sxVersion;
  }

  static String sxBuild = "?sxBuild?";
  static String sxVersionShow = "?sxVersionShow?";
  static String sxStamp = "?sxStamp?";

  public static File fSxBaseJar;
  public static File fSxBase;
  public static File fSxProject;
  public static boolean runningInProject = false;
  public static String fpContent;

  public static String sxJythonMaven;
  public static String sxJythonMaven25;
  public static String sxJython;
  public static String sxJython25;

  public static String sxJRubyMaven;
  public static String sxJRuby;

  protected Map<String, String> tessData = new HashMap<String, String>();

  protected String dlMavenRelease = "https://repo1.maven.org/maven2/";
  protected String dlMavenSnapshot = "https://oss.sonatype.org/content/groups/public/";

  // ******************************* ClassPath ****************************
  private static List<URL> storeClassPath() {
    URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
    return Arrays.asList(sysLoader.getURLs());
  }

  public static void dumpClassPath() {
    dumpClassPath(null);
  }

  public static void dumpClassPath(String filter) {
    filter = filter == null ? "" : filter;
    p("*** classpath dump %s", filter);
    String sEntry;
    filter = filter.toUpperCase();
    int n = 0;
    for (URL uEntry : storeClassPath()) {
      sEntry = uEntry.getPath();
      if (!filter.isEmpty()) {
        if (!sEntry.toUpperCase().contains(filter)) {
          n++;
          continue;
        }
      }
      p("%3d: %s", n, sEntry);
      n++;
    }
    p("*** classpath dump end");
  }

  public static String isOnClasspath(String artefact, boolean isJar) {
    artefact = ContentManager.slashify(artefact, false);
    String cpe = null;
    for (URL entry : storeClassPath()) {
      String sEntry = ContentManager.slashify(new File(entry.getPath()).getPath(), false);
      if (sEntry.contains(artefact)) {
        if (isJar) {
          if (!sEntry.endsWith(".jar")) {
            continue;
          }
          if (!new File(sEntry).getName().contains(artefact)) {
            continue;
          }
          if (new File(sEntry).getName().contains("4" + artefact)) {
            continue;
          }
        }
        cpe = new File(entry.getPath()).getPath();
        break;
      }
    }
    return cpe;
  }

  public static String isJarOnClasspath(String artefact) {
    return isOnClasspath(artefact, true);
  }

  public static String isOnClasspath(String artefact) {
    return isOnClasspath(artefact, false);
  }

  public static URL fromClasspath(String artefact) {
    artefact = ContentManager.slashify(artefact, false).toUpperCase();
    URL cpe = null;
    for (URL entry : storeClassPath()) {
      String sEntry = ContentManager.slashify(new File(entry.getPath()).getPath(), false);
      if (sEntry.toUpperCase().contains(artefact)) {
        return entry;
      }
    }
    return cpe;
  }

  public static boolean isOnClasspath(URL path) {
    for (URL entry : storeClassPath()) {
      if (new File(path.getPath()).equals(new File(entry.getPath()))) {
        return true;
      }
    }
    return false;
  }

  // ******************************* Settings ****************************
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
  private static boolean ShowActions = false;

  public static boolean isShowActions() {
    return ShowActions;
  }

  public static void setShowActions(boolean show) {
    if (show) {
      MoveMouseDelaySaved = MoveMouseDelay;
    } else {
      MoveMouseDelay = MoveMouseDelaySaved;
    }
    ShowActions = show;
  }

  public static float SlowMotionDelay = 2.0f; // in seconds
  public static float MoveMouseDelay = 0.5f; // in seconds
  private static float MoveMouseDelaySaved = MoveMouseDelay;

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

  // ******************************* Options ****************************
  static File fOptions;
  static Properties options = null;
  static String fnOptions = "SikulixOptions.txt";
  static String fnPrefs = "SikulixPreferences.txt";
  static boolean testing;

  public static void loadOptions(String fpOptions) {
    sxGlobal.log(-1, "loadOptions: not yet implemented");
  }

  public static boolean saveOptions(String fpOptions) {
    sxGlobal.log(-1, "saveOptions: not yet implemented");
    return false;
  }

  public static boolean saveOptions() {
    sxGlobal.log(-1, "saveOptions: not yet implemented");
    return false;
  }

  public static boolean isOption(String pName) {
    return isOption(pName, false);
  }

  public static boolean isOption(String pName, Boolean bDefault) {
    if (options == null) {
      return bDefault;
    }
    String pVal = options.getProperty(pName, bDefault.toString()).toLowerCase();
    if (pVal.isEmpty()) {
      return bDefault;
    } else if (pVal.contains("yes") || pVal.contains("true") || pVal.contains("on")) {
      return true;
    } else if (pVal.contains("no") || pVal.contains("false") || pVal.contains("off")) {
      return false;
    }
    return true;
  }

  public static String getOption(String pName) {
    if (options == null) {
      return "";
    }
    String pVal = options.getProperty(pName, "");
    return pVal;
  }

  public static String getOption(String pName, String sDefault) {
    if (options == null) {
      options = new Properties();
      options.setProperty(pName, sDefault);
      return sDefault;
    }
    String pVal = options.getProperty(pName, sDefault);
    if (pVal.isEmpty()) {
      options.setProperty(pName, sDefault);
      return sDefault;
    }
    return pVal;
  }

  public static void setOption(String pName, String sValue) {
    if (options == null) {
      options = new Properties();
    }
    options.setProperty(pName, sValue);
  }

  public static int getOptionNumber(String pName) {
    if (options == null) {
      return 0;
    }
    String pVal = options.getProperty(pName, "0");
    int nVal = 0;
    try {
      nVal = Integer.decode(pVal);
    } catch (Exception ex) {
    }
    return nVal;
  }

  public static int getOptionNumber(String pName, Integer nDefault) {
    if (options == null) {
      return nDefault;
    }
    String pVal = options.getProperty(pName, nDefault.toString());
    int nVal = nDefault;
    try {
      nVal = Integer.decode(pVal);
    } catch (Exception ex) {
    }
    return nVal;
  }

  public static Map<String, String> getOptions() {
    Map<String, String> mapOptions = new HashMap<String, String>();
    if (options != null) {
      Enumeration<?> optionNames = options.propertyNames();
      String optionName;
      while (optionNames.hasMoreElements()) {
        optionName = (String) optionNames.nextElement();
        mapOptions.put(optionName, getOption(optionName));
      }
    }
    return mapOptions;
  }

  public static boolean hasOptions() {
    return options != null && options.size() > 0;
  }

  public static void dumpOptions() {
    if (hasOptions()) {
      p("*** options dump:\n%s", (fOptions == null ? "" : fOptions));
      for (String sOpt : getOptions().keySet()) {
        p("%s = %s", sOpt, getOption(sOpt));
      }
      p("*** options dump end");
    }
  }

  // ******************************* Helpers ****************************
  private static void p(String msg, Object... args) {
    System.out.println(String.format(msg, args));
  }

  public static void dumpSysProps() {
    dumpSysProps(null);
  }

  public static void dumpSysProps(String filter) {
    filter = filter == null ? "" : filter;
    p("*** system properties dump " + filter);
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
      p(form, e, sysProps.get(e));
    }
    p("*** system properties dump end" + filter);
  }

  public static boolean runningJar = true;
  private static boolean isJythonReady = false;
  static String appType = "?appType?";

  public static void show() {
    if (hasOptions()) {
      dumpOptions();
    }
    p("***** show environment (%s)", sxVersionShow);
    p("user.home: %s", fUserHome);
    p("user.dir (work dir): %s", fWorkDir);
    p("java.io.tmpdir: %s", fTempPath);
    p("running %dBit on %s (%s) as %s", javaArch, osName,
            (linuxDistro.contains("???") ? osVersion : linuxDistro), appType);
    p(javaShow);
    p("app data folder: %s", fSXAppPath);
    p("libs folder: %s", fSXNative);
    if (runningJar) {
      p("executing jar: %s", fSxBaseJar);
    }
    dumpClassPath("sikulix");
    if (isJythonReady) {
      JythonHelper.get().showSysPath();
    }
    p("***** show environment end");
  }

  public static void getStatus() {
    show();
    p("***** System Information Dump *****");
    p(String.format("*** SystemInfo\n%s", osShow));
    System.getProperties().list(System.out);
    p("*** System Environment");
    for (String key : System.getenv().keySet()) {
      p(String.format("%s = %s", key, System.getenv(key)));
    }
    p("*** Java Class Path");
    URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
    URL[] urls = sysLoader.getURLs();
    for (int i = 0; i < urls.length; i++) {
      p(String.format("%d: %s", i, urls[i].getPath().replaceAll("%20", " ")));
    }
    p("***** System Information Dump ***** end *****");
  }

  public static void pause(int time) {
    try {
      Thread.sleep(time * 1000);
    } catch (InterruptedException ex) {
    }
  }

  public static void pause(float time) {
    try {
      Thread.sleep((int) (time * 1000));
    } catch (InterruptedException ex) {
    }
  }

  public static void pause(double time) {
    try {
      Thread.sleep((int) (time * 1000));
    } catch (InterruptedException ex) {
    }
  }
  // ******************************* ImageCache ****************************

  private static int ImageCache = 64;

  /**
   * set the maximum to be used for the {@link Image} cache
   * <br>the start up value is 64 (meaning MB)
   * <br>using 0 switches off caching and clears the cache in that moment
   *
   * @param max cache size in MB
   */
  public static void setImageCache(int max) {
    if (ImageCache > max) {
      clearCache(max);
    }
    ImageCache = max;
  }

  public static int getImageCache() {
    return ImageCache;
  }

  public static void clearCache(int max) {
    //TODO Image Cache
  }


  // ******************************* Monitors ****************************

  static GraphicsEnvironment genv = null;
  static GraphicsDevice[] gdevs;
  static Region[] monitorBounds = null;
  static Region rAllMonitors;
  public static int mainMonitor = -1;
  public static int nMonitors = 0;

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

  // ******************************* Devices ****************************

  public static Location at() {
    PointerInfo mp = MouseInfo.getPointerInfo();
    if (mp != null) {
      return new Location(MouseInfo.getPointerInfo().getLocation());
    } else {
      sxGlobal.log(-1, "not possible to get mouse position (PointerInfo == null)");
      return null;
    }
  }

  // ******************************* Proxy ****************************

  static String proxyName = "";
  static String proxyIP = "";
  static InetAddress proxyAddress = null;
  static String proxyPort = "";
  static boolean proxyChecked = false;
  static Proxy sxProxy = null;

  // ******************************* Extract Ressources ****************************
  static Class sxGlobalClassReference;

  public static List<String> extractTessData(File folder) {
    List<String> files = new ArrayList<String>();

    String tessdata = "/sikulixtessdata";
    URL uContentList = sxGlobalClassReference.getResource(tessdata + "/" + fpContent);
    if (uContentList != null) {
      files = doResourceListWithList(tessdata, files, null);
      if (files.size() > 0) {
        files = doExtractToFolderWithList(tessdata, folder, files);
      }
    } else {
      files = extractResourcesToFolder("/sikulixtessdata", folder, null);
    }
    return (files.size() == 0 ? null : files);
  }

  /**
   * export all resource files from the given subtree on classpath to the given folder retaining the subtree<br>
   * to export a specific file from classpath use extractResourceToFile or extractResourceToString
   *
   * @param fpRessources path of the subtree relative to root
   * @param fFolder      folder where to export (if null, only list - no export)
   * @param filter       implementation of interface FilenameFilter or null for no filtering
   * @return the filtered list of files (compact sikulixcontent format)
   */
  public static List<String> extractResourcesToFolder(String fpRessources, File fFolder, FilenameFilter filter) {
    List<String> content = null;
    content = resourceList(fpRessources, filter);
    if (content == null) {
      return null;
    }
    if (fFolder == null) {
      return content;
    }
    return doExtractToFolderWithList(fpRessources, fFolder, content);
  }

  private static List<String> doExtractToFolderWithList(String fpRessources, File fFolder, List<String> content) {
    int count = 0;
    int ecount = 0;
    String subFolder = "";
    if (content != null && content.size() > 0) {
      for (String eFile : content) {
        if (eFile == null) {
          continue;
        }
        if (eFile.endsWith("/")) {
          subFolder = eFile.substring(0, eFile.length() - 1);
          continue;
        }
        if (!subFolder.isEmpty()) {
          eFile = new File(subFolder, eFile).getPath();
        }
        if (extractResourceToFile(fpRessources, eFile, fFolder)) {
          sxGlobal.log(lvl + 1, "extractResourceToFile done: %s", eFile);
          count++;
        } else {
          ecount++;
        }
      }
    }
    if (ecount > 0) {
      sxGlobal.log(lvl, "files exported: %d - skipped: %d from %s to:\n%s", count, ecount, fpRessources, fFolder);
    } else {
      sxGlobal.log(lvl, "files exported: %d from: %s to:\n%s", count, fpRessources, fFolder);
    }
    return content;
  }

  /**
   * export all resource files from the given subtree in given jar to the given folder retaining the subtree
   *
   * @param aJar         absolute path to an existing jar or a string identifying the jar on classpath (no leading /)
   * @param fpRessources path of the subtree or file relative to root
   * @param fFolder      folder where to export (if null, only list - no export)
   * @param filter       implementation of interface FilenameFilter or null for no filtering
   * @return the filtered list of files (compact sikulixcontent format)
   */
  public static List<String> extractResourcesToFolderFromJar(String aJar, String fpRessources, File fFolder, FilenameFilter filter) {
    List<String> content = new ArrayList<String>();
    File faJar = new File(aJar);
    URL uaJar = null;
    fpRessources = ContentManager.slashify(fpRessources, false);
    if (faJar.isAbsolute()) {
      if (!faJar.exists()) {
        sxGlobal.log(-1, "extractResourcesToFolderFromJar: does not exist: %s", faJar);
        return null;
      }
      try {
        uaJar = new URL("jar", null, "file:" + aJar);
        sxGlobal.logp("%s", uaJar);
      } catch (MalformedURLException ex) {
        sxGlobal.log(-1, "extractResourcesToFolderFromJar: bad URL for: %s", faJar);
        return null;
      }
    } else {
      uaJar = fromClasspath(aJar);
      if (uaJar == null) {
        sxGlobal.log(-1, "extractResourcesToFolderFromJar: not on classpath: %s", aJar);
        return null;
      }
      try {
        String sJar = "file:" + uaJar.getPath() + "!/";
        uaJar = new URL("jar", null, sJar);
      } catch (MalformedURLException ex) {
        sxGlobal.log(-1, "extractResourcesToFolderFromJar: bad URL for: %s", uaJar);
        return null;
      }
    }
    content = doResourceListJar(uaJar, fpRessources, content, filter);
    if (fFolder == null) {
      return content;
    }
    copyFromJarToFolderWithList(uaJar, fpRessources, content, fFolder);
    return content;
  }

  /**
   * store a resource found on classpath to a file in the given folder with same filename
   *
   * @param inPrefix a subtree found in classpath
   * @param inFile   the filename combined with the prefix on classpath
   * @param outDir   a folder where to export
   * @return success
   */
  public static boolean extractResourceToFile(String inPrefix, String inFile, File outDir) {
    return extractResourceToFile(inPrefix, inFile, outDir, "");
  }

  /**
   * store a resource found on classpath to a file in the given folder
   *
   * @param inPrefix a subtree found in classpath
   * @param inFile   the filename combined with the prefix on classpath
   * @param outDir   a folder where to export
   * @param outFile  the filename for export
   * @return success
   */
  public static boolean extractResourceToFile(String inPrefix, String inFile, File outDir, String outFile) {
    InputStream aIS;
    FileOutputStream aFileOS;
    String content = inPrefix + "/" + inFile;
    try {
      content = isWindows() ? content.replace("\\", "/") : content;
      if (!content.startsWith("/")) {
        content = "/" + content;
      }
      aIS = (InputStream) sxGlobalClassReference.getResourceAsStream(content);
      if (aIS == null) {
        throw new IOException("resource not accessible");
      }
      File out = outFile.isEmpty() ? new File(outDir, inFile) : new File(outDir, inFile);
      if (!out.getParentFile().exists()) {
        out.getParentFile().mkdirs();
      }
      aFileOS = new FileOutputStream(out);
      copy(aIS, aFileOS);
      aIS.close();
      aFileOS.close();
    } catch (Exception ex) {
      sxGlobal.log(-1, "extractResourceToFile: %s (%s)", content, ex);
      return false;
    }
    return true;
  }

  /**
   * store the content of a resource found on classpath in the returned string
   *
   * @param inPrefix a subtree from root found in classpath (leading /)
   * @param inFile   the filename combined with the prefix on classpath
   * @param encoding
   * @return file content
   */
  public static String extractResourceToString(String inPrefix, String inFile, String encoding) {
    InputStream aIS = null;
    String out = null;
    String content = inPrefix + "/" + inFile;
    if (!content.startsWith("/")) {
      content = "/" + content;
    }
    try {
      content = isWindows() ? content.replace("\\", "/") : content;
      aIS = (InputStream) sxGlobalClassReference.getResourceAsStream(content);
      if (aIS == null) {
        throw new IOException("extractResourceToString: resource not accessible: " + content);
      }
      if (encoding == null || encoding.isEmpty()) {
        encoding = "UTF-8";
        out = new String(copy(aIS), "UTF-8");
      } else {
        out = new String(copy(aIS), encoding);
      }
      aIS.close();
      aIS = null;
    } catch (Exception ex) {
      sxGlobal.log(-1, "extractResourceToString error: %s from: %s (%s)", encoding, content, ex);
    }
    try {
      if (aIS != null) {
        aIS.close();
      }
    } catch (Exception ex) {
    }
    return out;
  }

  public static URL resourceLocation(String folderOrFile) {
    sxGlobal.log(lvl, "resourceLocation: (%s) %s", sxGlobalClassReference, folderOrFile);
    if (!folderOrFile.startsWith("/")) {
      folderOrFile = "/" + folderOrFile;
    }
    return sxGlobalClassReference.getResource(folderOrFile);
  }

  private static List<String> resourceList(String folder, FilenameFilter filter) {
    sxGlobal.log(lvl, "resourceList: enter");
    List<String> files = new ArrayList<String>();
    if (!folder.startsWith("/")) {
      folder = "/" + folder;
    }
    URL uFolder = resourceLocation(folder);
    if (uFolder == null) {
      sxGlobal.log(lvl, "resourceList: not found: %s", folder);
      return files;
    }
    try {
      uFolder = new URL(uFolder.toExternalForm().replaceAll(" ", "%20"));
    } catch (Exception ex) {
    }
    URL uContentList = sxGlobalClassReference.getResource(folder + "/" + fpContent);
    if (uContentList != null) {
      return doResourceListWithList(folder, files, filter);
    }
    File fFolder = null;
    try {
      fFolder = new File(uFolder.toURI());
      sxGlobal.log(lvl, "resourceList: having folder:\n%s", fFolder);
      String sFolder = ContentManager.normalizeAbsolute(fFolder.getPath(), false);
      if (":".equals(sFolder.substring(2, 3))) {
        sFolder = sFolder.substring(1);
      }
      files.add(sFolder);
      files = doResourceListFolder(new File(sFolder), files, filter);
      files.remove(0);
      return files;
    } catch (Exception ex) {
      if (!"jar".equals(uFolder.getProtocol())) {
        sxGlobal.log(lvl, "resourceList:\n%s", folder);
        sxGlobal.log(-1, "resourceList: URL neither folder nor jar:\n%s", ex);
        return null;
      }
    }
    String[] parts = uFolder.getPath().split("!");
    if (parts.length < 2 || !parts[0].startsWith("file:")) {
      sxGlobal.log(lvl, "resourceList:\n%s", folder);
      sxGlobal.log(-1, "resourceList: not a valid jar URL:\n" + uFolder.getPath());
      return null;
    }
    String fpFolder = parts[1];
    sxGlobal.log(lvl, "resourceList: having jar:\n%s", uFolder);
    return doResourceListJar(uFolder, fpFolder, files, filter);
  }

  /**
   * write the list as it is produced by calling extractResourcesToFolder to the given file with system line
   * separator<br>
   * non-compact format: every file with full path
   *
   * @param folder path of the subtree relative to root with leading /
   * @param target the file to write the list (if null, only list - no file)
   * @param filter implementation of interface FilenameFilter or null for no filtering
   * @return success
   */
  public static String[] resourceListAsFile(String folder, File target, FilenameFilter filter) {
    String content = resourceListAsString(folder, filter);
    if (content == null) {
      sxGlobal.log(-1, "resourceListAsFile: did not work: %s", folder);
      return null;
    }
    if (target != null) {
      try {
        ContentManager.deleteFileOrFolder(target.getAbsolutePath());
        target.getParentFile().mkdirs();
        PrintWriter aPW = new PrintWriter(target);
        aPW.write(content);
        aPW.close();
      } catch (Exception ex) {
        sxGlobal.log(-1, "resourceListAsFile: %s:\n%s", target, ex);
      }
    }
    return content.split(System.getProperty("line.separator"));
  }

  /**
   * write the list as it is produced by calling extractResourcesToFolder to the given file with system line
   * separator<br>
   * compact sikulixcontent format
   *
   * @param folder       path of the subtree relative to root with leading /
   * @param targetFolder the folder where to store the file sikulixcontent (if null, only list - no export)
   * @param filter       implementation of interface FilenameFilter or null for no filtering
   * @return success
   */
  public static String[] resourceListAsSXContent(String folder, File targetFolder, FilenameFilter filter) {
    List<String> contentList = resourceList(folder, filter);
    if (contentList == null) {
      sxGlobal.log(-1, "resourceListAsSikulixContent: did not work: %s", folder);
      return null;
    }
    File target = null;
    String arrString[] = new String[contentList.size()];
    try {
      PrintWriter aPW = null;
      if (targetFolder != null) {
        target = new File(targetFolder, fpContent);
        ContentManager.deleteFileOrFolder(target);
        target.getParentFile().mkdirs();
        aPW = new PrintWriter(target);
      }
      int n = 0;
      for (String line : contentList) {
        arrString[n++] = line;
        if (targetFolder != null) {
          aPW.println(line);
        }
      }
      if (targetFolder != null) {
        aPW.close();
      }
    } catch (Exception ex) {
      sxGlobal.log(-1, "resourceListAsFile: %s:\n%s", target, ex);
    }
    return arrString;
  }

  /**
   * write the list as it is produced by calling extractResourcesToFolder to the given file with system line
   * separator<br>
   * compact sikulixcontent format
   *
   * @param aJar         absolute path to an existing jar or a string identifying the jar on classpath (no leading /)
   * @param folder       path of the subtree relative to root with leading /
   * @param targetFolder the folder where to store the file sikulixcontent (if null, only list - no export)
   * @param filter       implementation of interface FilenameFilter or null for no filtering
   * @return success
   */
  public static String[] resourceListAsSXContentFromJar(String aJar, String folder, File targetFolder, FilenameFilter filter) {
    List<String> contentList = extractResourcesToFolderFromJar(aJar, folder, null, filter);
    if (contentList == null || contentList.size() == 0) {
      sxGlobal.log(-1, "resourceListAsSikulixContentFromJar: did not work: %s", folder);
      return null;
    }
    File target = null;
    String arrString[] = new String[contentList.size()];
    try {
      PrintWriter aPW = null;
      if (targetFolder != null) {
        target = new File(targetFolder, fpContent);
        ContentManager.deleteFileOrFolder(target);
        target.getParentFile().mkdirs();
        aPW = new PrintWriter(target);
      }
      int n = 0;
      for (String line : contentList) {
        arrString[n++] = line;
        if (targetFolder != null) {
          aPW.println(line);
        }
      }
      if (targetFolder != null) {
        aPW.close();
      }
    } catch (Exception ex) {
      sxGlobal.log(-1, "resourceListAsFile: %s:\n%s", target, ex);
    }
    return arrString;
  }

  /**
   * write the list produced by calling extractResourcesToFolder to the returned string with system line separator<br>
   * non-compact format: every file with full path
   *
   * @param folder path of the subtree relative to root with leading /
   * @param filter implementation of interface FilenameFilter or null for no filtering
   * @return the resulting string
   */
  public static String resourceListAsString(String folder, FilenameFilter filter) {
    return resourceListAsString(folder, filter, null);
  }

  /**
   * write the list produced by calling extractResourcesToFolder to the returned string with given separator<br>
   * non-compact format: every file with full path
   *
   * @param folder    path of the subtree relative to root with leading /
   * @param filter    implementation of interface FilenameFilter or null for no filtering
   * @param separator to be used to separate the entries
   * @return the resulting string
   */
  public static String resourceListAsString(String folder, FilenameFilter filter, String separator) {
    List<String> aList = resourceList(folder, filter);
    if (aList == null) {
      return null;
    }
    if (separator == null) {
      separator = System.getProperty("line.separator");
    }
    String out = "";
    String subFolder = "";
    if (aList != null && aList.size() > 0) {
      for (String eFile : aList) {
        if (eFile == null) {
          continue;
        }
        if (eFile.endsWith("/")) {
          subFolder = eFile.substring(0, eFile.length() - 1);
          continue;
        }
        if (!subFolder.isEmpty()) {
          eFile = new File(subFolder, eFile).getPath();
        }
        out += eFile.replace("\\", "/") + separator;
      }
    }
    return out;
  }

  private static List<String> doResourceListFolder(File fFolder, List<String> files, FilenameFilter filter) {
    int localLevel = lvl + 1;
    String subFolder = "";
    if (fFolder.isDirectory()) {
      if (!ContentManager.pathEquals(fFolder.getPath(), files.get(0))) {
        subFolder = fFolder.getPath().substring(files.get(0).length() + 1).replace("\\", "/") + "/";
        if (filter != null && !filter.accept(new File(files.get(0), subFolder), "")) {
          return files;
        }
      } else {
        sxGlobal.log(localLevel, "scanning folder:\n%s", fFolder);
        subFolder = "/";
        files.add(subFolder);
      }
      String[] subList = fFolder.list();
      for (String entry : subList) {
        File fEntry = new File(fFolder, entry);
        if (fEntry.isDirectory()) {
          files.add(fEntry.getAbsolutePath().substring(1 + files.get(0).length()).replace("\\", "/") + "/");
          doResourceListFolder(fEntry, files, filter);
          files.add(subFolder);
        } else {
          if (filter != null && !filter.accept(fFolder, entry)) {
            continue;
          }
          sxGlobal.log(localLevel, "from %s adding: %s", (subFolder.isEmpty() ? "." : subFolder), entry);
          files.add(fEntry.getAbsolutePath().substring(1 + fFolder.getPath().length()));
        }
      }
    }
    return files;
  }

  private static List<String> doResourceListWithList(String folder, List<String> files, FilenameFilter filter) {
    String content = extractResourceToString(folder, fpContent, "");
    String[] contentList = content.split(content.indexOf("\r") != -1 ? "\r\n" : "\n");
    if (filter == null) {
      files.addAll(Arrays.asList(contentList));
    } else {
      for (String fpFile : contentList) {
        if (filter.accept(new File(fpFile), "")) {
          files.add(fpFile);
        }
      }
    }
    return files;
  }

  private static List<String> doResourceListJar(URL uJar, String fpResource, List<String> files, FilenameFilter filter) {
    int localLevel = lvl + 1;
    ZipInputStream zJar;
    String fpJar = uJar.getPath().split("!")[0];
    String fileSep = "/";
    if (!fpJar.endsWith(".jar")) {
      return files;
    }
    sxGlobal.log(localLevel, "scanning jar:\n%s", uJar);
    fpResource = fpResource.startsWith("/") ? fpResource.substring(1) : fpResource;
    File fFolder = new File(fpResource);
    File fSubFolder = null;
    ZipEntry zEntry;
    String subFolder = "";
    boolean skip = false;
    try {
      zJar = new ZipInputStream(new URL(fpJar).openStream());
      while ((zEntry = zJar.getNextEntry()) != null) {
        if (zEntry.getName().endsWith("/")) {
          continue;
        }
        String zePath = zEntry.getName();
        if (zePath.startsWith(fpResource)) {
          if (fpResource.length() == zePath.length()) {
            files.add(zePath);
            return files;
          }
          String zeName = zePath.substring(fpResource.length() + 1);
          int nSep = zeName.lastIndexOf(fileSep);
          String zefName = zeName.substring(nSep + 1, zeName.length());
          String zeSub = "";
          if (nSep > -1) {
            zeSub = zeName.substring(0, nSep + 1);
            if (!subFolder.equals(zeSub)) {
              subFolder = zeSub;
              fSubFolder = new File(fFolder, subFolder);
              skip = false;
              if (filter != null && !filter.accept(fSubFolder, "")) {
                skip = true;
                continue;
              }
              files.add(zeSub);
            }
            if (skip) {
              continue;
            }
          } else {
            if (!subFolder.isEmpty()) {
              subFolder = "";
              fSubFolder = fFolder;
              files.add("/");
            }
          }
          if (filter != null && !filter.accept(fSubFolder, zefName)) {
            continue;
          }
          files.add(zefName);
          sxGlobal.log(localLevel, "from %s adding: %s", (zeSub.isEmpty() ? "." : zeSub), zefName);
        }
      }
    } catch (Exception ex) {
      sxGlobal.log(-1, "doResourceListJar: %s", ex);
      return files;
    }
    return files;
  }

  private static boolean copyFromJarToFolderWithList(URL uJar, String fpRessource, List<String> files, File fFolder) {
    int localLevel = testing ? lvl : lvl + 1;
    if (files == null || files.isEmpty()) {
      sxGlobal.log(lvl, "copyFromJarToFolderWithList: list of files is empty");
      return false;
    }
    String fpJar = uJar.getPath().split("!")[0];
    if (!fpJar.endsWith(".jar")) {
      return false;
    }
    sxGlobal.log(localLevel, "scanning jar:\n%s", uJar);
    fpRessource = fpRessource.startsWith("/") ? fpRessource.substring(1) : fpRessource;

    String subFolder = "";

    int maxFiles = files.size() - 1;
    int nFiles = 0;

    ZipEntry zEntry;
    ZipInputStream zJar;
    String zPath;
    int prefix = fpRessource.length();
    fpRessource += !fpRessource.isEmpty() ? "/" : "";
    String current = "/";
    boolean shouldStop = false;
    try {
      zJar = new ZipInputStream(new URL(fpJar).openStream());
      while ((zEntry = zJar.getNextEntry()) != null) {
        zPath = zEntry.getName();
        if (zPath.endsWith("/")) {
          continue;
        }
        while (current.endsWith("/")) {
          if (nFiles > maxFiles) {
            shouldStop = true;
            break;
          }
          subFolder = current.length() == 1 ? "" : current;
          current = files.get(nFiles++);
          if (!current.endsWith("/")) {
            current = fpRessource + subFolder + current;
            break;
          }
        }
        if (shouldStop) {
          break;
        }
        if (zPath.startsWith(current)) {
          if (zPath.length() == fpRessource.length() - 1) {
            sxGlobal.log(-1, "extractResourcesToFolderFromJar: only ressource folders allowed - use filter");
            return false;
          }
          sxGlobal.log(localLevel, "copying: %s", zPath);
          File out = new File(fFolder, zPath.substring(prefix));
          if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
          }
          FileOutputStream aFileOS = new FileOutputStream(out);
          copy(zJar, aFileOS);
          aFileOS.close();
          if (nFiles > maxFiles) {
            break;
          }
          current = files.get(nFiles++);
          if (!current.endsWith("/")) {
            current = fpRessource + subFolder + current;
          }
        }
      }
      zJar.close();
    } catch (Exception ex) {
      sxGlobal.log(-1, "doResourceListJar: %s", ex);
      return false;
    }
    return true;
  }

  private static void copy(InputStream in, OutputStream out) throws IOException {
    byte[] tmp = new byte[8192];
    int len;
    while (true) {
      len = in.read(tmp);
      if (len <= 0) {
        break;
      }
      out.write(tmp, 0, len);
    }
    out.flush();
  }

  private static byte[] copy(InputStream inputStream) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int length = 0;
    while ((length = inputStream.read(buffer)) != -1) {
      baos.write(buffer, 0, length);
    }
    return baos.toByteArray();
  }

  public static class oneFileFilter implements FilenameFilter {

    String aFile;

    public oneFileFilter(String aFileGiven) {
      aFile = aFileGiven;
    }

    @Override
    public boolean accept(File dir, String name) {
      if (name.contains(aFile)) {
        return true;
      }
      return false;
    }
  }

// ******************************* Commands Pathhandling ****************************

  public static String setBundlePath(String fpPath) {
    sxGlobal.log(-1, "//TODO setBundlePath: not implemented");
    return "//TODO not implemented";
  }

  public static void addImagePath(String fpMain, String fpSub) {
    sxGlobal.log(-1, "//TODO addImagePath: not implemented");
  }

  public static void addImagePath(String fpMain) {
    addImagePath(fpMain, null);
  }

  public static String getBundlePath() {
    sxGlobal.log(-1, "//TODO getBundlePath: not implemented");
    return "//TODO not implemented";
  }

  public static boolean addClassPath(String jarOrFolder) {
    URL uJarOrFolder = ContentManager.makeURL(jarOrFolder);
    if (!new File(jarOrFolder).exists()) {
      sxGlobal.log(-1, "addToClasspath: does not exist - not added:\n%s", jarOrFolder);
      return false;
    }
    if (isOnClasspath(uJarOrFolder)) {
      return true;
    }
    sxGlobal.log(lvl, "addToClasspath:\n%s", uJarOrFolder);
    Method method;
    URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
    Class sysclass = URLClassLoader.class;
    try {
      method = sysclass.getDeclaredMethod("addURL", new Class[]{URL.class});
      method.setAccessible(true);
      method.invoke(sysLoader, new Object[]{uJarOrFolder});
    } catch (Exception ex) {
      sxGlobal.log(-1, "Did not work: %s", ex.getMessage());
      return false;
    }
    storeClassPath();
    return true;
  }

  // ******************************* Commands which system ****************************
  /**
   * @return true/false
   */
  public static boolean isWindows() {
    return runningWindows;
  }

  /**
   * @return true/false
   */
  public static boolean isLinux() {
    return runningLinux;
  }

  /**
   * @return true/false
   */
  public static boolean isMac() {
    return runningMac;
  }

  /**
   * @return path seperator : or ;
   */
  public static String getSeparator() {
    if (isWindows()) {
      return ";";
    }
    return ":";
  }

// ******************************* Commands Clipboard ****************************
  /**
   * @return content
   */
  public static String getClipboard() {
    return App.getClipboard();
  }

  /**
   * set content
   *
   * @param text text
   */
  public static void setClipboard(String text) {
    App.setClipboard(text);
  }

// ******************************* Commands Keys ****************************
  /**
   * get the lock state of the given key
   *
   * @param key respective key specifier according class Key
   * @return true/false
   */
  public static boolean isLockOn(char key) {
    return Key.isLockOn(key);
  }

  /**
   * @return System dependent key
   */
  public static int getHotkeyModifier() {
    return Key.getHotkeyModifier();
  }

  /**
   * @param key       respective key specifier according class Key
   * @param modifiers respective key specifier according class KeyModifiers
   * @param listener  a HotKeyListener instance
   * @return true if ok, false otherwise
   */
  public static boolean addHotkey(String key, int modifiers, HotkeyListener listener) {
    return Key.addHotkey(key, modifiers, listener);
  }

  /**
   * @param key       respective key specifier according class Key
   * @param modifiers respective key specifier according class KeyModifiers
   * @param listener  a HotKeyListener instance
   * @return true if ok, false otherwise
   */
  public static boolean addHotkey(char key, int modifiers, HotkeyListener listener) {
    return Key.addHotkey(key, modifiers, listener);
  }

  /**
   * @param key       respective key specifier according class Key
   * @param modifiers respective key specifier according class KeyModifiers
   * @return true if ok, false otherwise
   */
  public static boolean removeHotkey(String key, int modifiers) {
    return Key.removeHotkey(key, modifiers);
  }

  /**
   * @param key       respective key specifier according class Key
   * @param modifiers respective key specifier according class KeyModifiers
   * @return true if ok, false otherwise
   */
  public static boolean removeHotkey(char key, int modifiers) {
    return Key.removeHotkey(key, modifiers);
  }

// ******************************* Commands run something ****************************
  public final static String runCmdError = "*****error*****";
  private String lastResult = "";

  /**
   * run a system command finally using Java::Runtime.getRuntime().exec(args) and waiting for completion
   *
   * @param cmd the command as it would be given on command line, quoting is preserved
   * @return the output produced by the command (sysout [+ "*** error ***" + syserr] if the syserr part is present, the
   * command might have failed
   */
  public String runcmd(String cmd) {
    return runcmd(new String[]{cmd});
  }

  /**
   * run a system command finally using Java::Runtime.getRuntime().exec(args) and waiting for completion
   *
   * @param args the command as it would be given on command line splitted into the space devided parts, first part is
   *            the command, the rest are parameters and their values
   * @return the output produced by the command (sysout [+ "*** error ***" + syserr] if the syserr part is present, the
   * command might have failed
   */
  public String runcmd(String args[]) {
    if (args.length == 0) {
      return "";
    }
    boolean silent = false;
    if (args.length == 1) {
      String separator = "\"";
      ArrayList<String> argsx = new ArrayList<String>();
      StringTokenizer toks;
      String tok;
      String cmd = args[0];
      if (isWindows()) {
        cmd = cmd.replaceAll("\\\\ ", "%20;");
      }
      toks = new StringTokenizer(cmd);
      while (toks.hasMoreTokens()) {
        tok = toks.nextToken(" ");
        if (tok.length() == 0) {
          continue;
        }
        if (separator.equals(tok)) {
          continue;
        }
        if (tok.startsWith(separator)) {
          if (tok.endsWith(separator)) {
            tok = tok.substring(1, tok.length() - 1);
          } else {
            tok = tok.substring(1);
            tok += toks.nextToken(separator);
          }
        }
        argsx.add(tok.replaceAll("%20;", " "));
      }
      args = argsx.toArray(new String[0]);
    }
    if (args[0].startsWith("!")) {
      silent = true;
      args[0] = args[0].substring(1);
    }
    if (args[0].startsWith("#")) {
      String pgm = args[0].substring(1);
      args[0] = (new File(pgm)).getAbsolutePath();
      runcmd(new String[]{"chmod", "ugo+x", args[0]});
    }
    String result = "";
    String error = runCmdError + NL;
    boolean hasError = false;
    int retVal;
    try {
      log(lvl, arrayToString(args));
      Process process = Runtime.getRuntime().exec(args);
      BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
      BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
      String s;
      while ((s = stdInput.readLine()) != null) {
        if (!s.isEmpty()) {
          result += s + NL;
        }
      }
      if ((s = stdError.readLine()) != null) {
        hasError = true;
        if (!s.isEmpty()) {
          error += s + NL;
        }
      }
      process.waitFor();
      retVal = process.exitValue();
      process.destroy();
    } catch (Exception e) {
      log(-1, "fatal error: " + e);
      result = String.format(error + "%s", e);
      retVal = 9999;
      hasError = true;
    }
    if (hasError) {
      result += error;
    }
    lastResult = result;
    return String.format("%d%s%s", retVal, NL, result);
  }

  public String getLastCommandResult() {
    return lastResult;
  }

  private String[] args = new String[0];
  private String[] sargs = new String[0];

  public void setArgs(String[] args, String[] sargs) {
    this.args = args;
    this.sargs = sargs;
  }

  public String[] getSikuliArgs() {
    return sargs;
  }

  public String[] getArgs() {
    return args;
  }

  public void printArgs() {
    String[] xargs = getSikuliArgs();
    if (xargs.length > 0) {
      log(lvl, "--- Sikuli parameters ---");
      for (int i = 0; i < xargs.length; i++) {
        log(lvl, "%d: %s", i + 1, xargs[i]);
      }
    }
    xargs = getArgs();
    if (xargs.length > 0) {
      log(lvl, "--- User parameters ---");
      for (int i = 0; i < xargs.length; i++) {
        log(lvl, "%d: %s", i + 1, xargs[i]);
      }
    }
  }

  public static int checkArgs(String[] args) {
    sxGlobal.terminate(1, "checkArgs: not implemented");
    return 0;
  }

  static String arrayToString(String[] args) {
    String ret = "";
    for (String s : args) {
      if (s.contains(" ")) {
        s = "\"" + s + "\"";
      }
      ret += s + " ";
    }
    return ret;
  }

  // ******************************* Commands cleanup ****************************
  public static void cleanUp(int n) {
    sxGlobal.log(lvl, "cleanUp: %d", n);
//    ScreenHighlighter.closeAll();
//    Observer.cleanUp();
//    Mouse.reset();
//    Screen.getPrimaryScreen().getRobot().keyUp();
//    HotkeyManager.reset();
  }

}
