/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.sikulix.api.Element;
//import com.sikulix.scripting.JythonHelper;
import org.apache.commons.cli.*;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.sikuli.script.Region;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.util.*;
import java.util.List;

import static com.sikulix.core.SX.NATIVES.HOTKEY;
import static com.sikulix.core.SX.NATIVES.OPENCV;
import static com.sikulix.core.SX.NATIVES.SYSUTIL;

public class SX {

  private static long startTime = new Date().getTime();

  //<editor-fold desc="*** logging">
  public static final int INFO = 1;
  public static final int DEBUG = 3;
  public static final int TRACE = 4;
  public static final int ERROR = -1;
  public static final int FATAL = -2;

  private static final SXLog log = new SXLog();

  private static void info(String message, Object... args) {
    log.info(message, args);
  }

  public static void debug(String message, Object... args) {
    log.debug(message, args);
  }

  public static void trace(String message, Object... args) {
    log.trace(message, args);
  }

  public static void error(String message, Object... args) {
    log.error(message, args);
  }

  public static void terminate(int retval, String message, Object... args) {
    if (retval != 0) {
      log.fatal(message, args);
    } else {
      info(message, args);
    }
    System.exit(retval);
  }

  public static void p(String msg, Object... args) {
    log.p(msg, args);
  }

  public static SXLog getLogger(String className) {
    return getLogger(className, null, -1);
  }

  public static SXLog getLogger(String className, int level) {
    return getLogger(className, null, level);
  }

  public static SXLog getLogger(String className, String[] args) {
    return getLogger(className, args, -1);
  }

  public static SXLog getLogger(String className, String[] args, int level) {
    return new SXLog(className, args, level);
  }
  //</editor-fold>

  //<editor-fold desc="*** init">
  private static String sxInstance = null;

  private static boolean shouldLock = false;
  private static FileOutputStream isRunningFile = null;
  static final Class sxGlobalClassReference = SX.class;

  static void sxinit(String[] args) {
    if (null == sxInstance) {
      sxInstance = "SX INIT DONE";

      //<editor-fold desc="*** shutdown hook">
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          if (shouldLock && isSet(isRunningFile)) {
            try {
              isRunningFile.close();
            } catch (IOException ex) {
            }
          }
          for (File f : getFile(getSYSTEMP()).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
              File aFile = new File(dir, name);
              boolean isObsolete = false;
              long lastTime = aFile.lastModified();
              if (lastTime == 0) {
                return false;
              }
              if (lastTime < ((new Date().getTime()) - 7 * 24 * 60 * 60 * 1000)) {
                isObsolete = true;
              }
              if (name.contains("BridJExtractedLibraries") && isObsolete) {
                return true;
              }
              if (name.toLowerCase().contains("sikuli")) {
                if (name.contains("Sikulix_")) {
                  if (isObsolete || aFile.equals(getFile(getTEMP()))) {
                    return true;
                  }
                } else {
                  return true;
                }
              }
              return false;
            }
          })) {
            trace("cleanTemp: " + f.getName());
            Content.deleteFileOrFolder("#" + f.getAbsolutePath());
          }
        }
      });
      //</editor-fold>

      // TODO Content class must be initialized for lock in shutdown
      Content.start();

      //<editor-fold desc="*** sx lock (not active)">
      if (shouldLock) {
        File fLock = new File(getSYSTEMP(), "SikuliX2-i-s-r-u-n-n-i-n-g");
        String shouldTerminate = "";
        try {
          fLock.createNewFile();
          isRunningFile = new FileOutputStream(fLock);
          if (isNull(isRunningFile.getChannel().tryLock())) {
            shouldTerminate = "SikuliX2 already running";
            isRunningFile = null;
          }
        } catch (Exception ex) {
          shouldTerminate = "cannot access SX2 lock: " + ex.toString();
          isRunningFile = null;
        }
        if (isSet(shouldTerminate)) {
          terminate(1, shouldTerminate);
        }
      }
      //</editor-fold>

      // *** command line args
      if (!isNull(args)) {
        checkArgs(args);
      }

      trace("!sxinit: entry");

      // *** get SX options
      loadOptions();

      // *** get the version info
      getVERSION();

      // *** check how we are running
      sxRunningAs();

      //TODO i18n SXGlobal_sxinit_complete=complete %.3f
      trace("!sxinit: exit %.3f", (new Date().getTime() - startTime) / 1000.0f);
    }
  }
  //</editor-fold>

  //<editor-fold desc="*** command line args">
  private static List<String> sxArgs = new ArrayList<String>();
  private static List<String> userArgs = new ArrayList<String>();
  private static CommandLine sxCommandArgs = null;

  static void checkArgs(String[] args) {
    boolean hasUserArgs = false;
    for (String arg : args) {
      if ("--".equals(arg)) {
        hasUserArgs = true;
        continue;
      }
      if (hasUserArgs) {
        trace("checkargs: user: %s", arg);
        userArgs.add(arg);
      } else {
        trace("checkargs: --sx: %s", arg);
        sxArgs.add(arg);
      }
    }
    if (sxArgs.size() > 0) {
      CommandLineParser parser = new PosixParser();
      Options opts = new Options();
      opts.addOption(OptionBuilder.hasOptionalArg().create('d'));
      opts.addOption(OptionBuilder.hasArg().create('o'));
      opts.addOption(OptionBuilder.hasArgs().create('r'));
      opts.addOption(OptionBuilder.hasArgs().create('t'));
      opts.addOption(OptionBuilder.hasArg(false).create('c'));
      opts.addOption(OptionBuilder.hasArg(false).create('q'));
      try {
        sxCommandArgs = parser.parse(opts, sxArgs.toArray(new String[0]));
      } catch (ParseException e) {
        terminate(1, "checkArgs: %s", e.getMessage());
      }
      if (!isNull(sxCommandArgs)) {
        if (isArg("q")) {
          log.globalStop();
        } else if (isArg("d")) {
          log.globalOn(log.DEBUG);
        }
      }
    }
    //TODO make options from SX args
  }

  private static boolean isArg(String arg) {
    return sxCommandArgs != null && sxCommandArgs.hasOption(arg);
  }

  private static String getArg(String arg) {
    if (sxCommandArgs != null && sxCommandArgs.hasOption(arg)) {
      String val = sxCommandArgs.getOptionValue(arg);
      return val == null ? "" : val;
    }
    return null;
  }

  public static String[] getUserArgs() {
    return userArgs.toArray(new String[0]);
  }
  //</editor-fold>

  //<editor-fold desc="*** check how we are running">
  public static String sxGlobalClassNameIDE = "";

  private static boolean isJythonReady = false;
  static String appType = "?appType?";

  static File fSxBaseJar;
  static File fSxBase;
  //TODO getter
  public static File fSxProject;
  static boolean runningInProject = false;
  static final String fpContent = "sikulixcontent";

  static String sxJythonMaven;
  static String sxJython;

  static String sxJRubyMaven;
  static String sxJRuby;

  static Map<String, String> tessData = new HashMap<String, String>();

  static String dlMavenRelease = "https://repo1.maven.org/maven2/";
  static String dlMavenSnapshot = "https://oss.sonatype.org/content/groups/public/";

  private static boolean runningJar = true;

  static String whereIs(Class clazz) {
    CodeSource codeSrc = clazz.getProtectionDomain().getCodeSource();
    String base = null;
    if (codeSrc != null && codeSrc.getLocation() != null) {
      base = Content.slashify(codeSrc.getLocation().getPath(), false);
    }
    return base;
  }

  static void sxRunningAs() {
    appType = "from a jar";
    String base = whereIs(sxGlobalClassReference);
    if (base != null) {
      fSxBaseJar = new File(base);
      String jn = fSxBaseJar.getName();
      fSxBase = fSxBaseJar.getParentFile();
      debug("sxRunningAs: runs as %s in: %s", jn, fSxBase.getAbsolutePath());
      if (jn.contains("classes")) {
        runningJar = false;
        fSxProject = fSxBase.getParentFile().getParentFile();
        debug("sxRunningAs: not jar - supposing Maven project: %s", fSxProject);
        appType = "in Maven project from classes";
        runningInProject = true;
      } else if ("target".equals(fSxBase.getName())) {
        fSxProject = fSxBase.getParentFile().getParentFile();
        debug("sxRunningAs: folder target detected - supposing Maven project: %s", fSxProject);
        appType = "in Maven project from some jar";
        runningInProject = true;
      } else {
        if (isWindows()) {
          if (jn.endsWith(".exe")) {
            setASAPP(true);
            runningJar = false;
            appType = "as application .exe";
          }
        } else if (isMac()) {
          if (fSxBase.getAbsolutePath().contains("SikuliX.app/Content")) {
            setASAPP(true);
            appType = "as application .app";
            if (!fSxBase.getAbsolutePath().startsWith("/Applications")) {
              appType += " (not from /Applications folder)";
            }
          }
        }
      }
    } else {
      terminate(1, "sxRunningAs: no valid Java context for SikuliX available "
              + "(java.security.CodeSource.getLocation() is null)");
    }
  }
  //</editor-fold>

  //<editor-fold desc="*** get SX options at startup">
  private static File fOptions = null;
  private static String fnOptions = "sxoptions.txt";

  private static PropertiesConfiguration sxOptions = null;

  private static void loadOptions() {
    boolean success = true;
    URL urlOptions = SX.class.getClassLoader().getResource("Settings/sxoptions.txt");
    if (!isNull(urlOptions)) {
      Configurations configs = new Configurations();
      try {
        sxOptions = configs.properties(urlOptions);
      } catch (ConfigurationException cex) {
        success = false;
      }
    } else {
      success = false;
    }
    if (!success) {
      terminate(1, "loadOptions: SX Options not available: %s", urlOptions);
    }

    PropertiesConfiguration extraOptions = null;

    File aFile = null;
    String argFile = getArg("o");
    if (!isNull(argFile)) {
      aFile = getFile(argFile);
      if (!aFile.isDirectory()) {
        if (aFile.exists()) {
          fOptions = aFile;
          trace("loadOptions: arg: %s (from arg -o)", aFile);
        } else {
          fnOptions = aFile.getName();
          trace("loadOptions: file name given: %s (from arg -o)", fnOptions);
        }
      }
    }

    if (isNull(fOptions)) {
      for (String sFile : new String[]{getUSERWORK(), getUSERHOME(), getSXSTORE()}) {
        if (isNull(sFile)) {
          continue;
        }
        aFile = getFile(sFile);
        trace("loadOptions: check: %s", aFile);
        fOptions = new File(aFile, fnOptions);
        if (fOptions.exists()) {
          break;
        } else {
          fOptions = null;
        }
      }
    }
    if (fOptions != null) {
      trace("loadOptions: found Options file at: %s", fOptions);
      Configurations configs = new Configurations();
      try {
        extraOptions = configs.properties(fOptions);
      } catch (ConfigurationException cex) {
        error("loadOptions: Options not valid: %s", cex.getMessage());
      }
      if (!isNull(extraOptions)) {
        mergeExtraOptions(sxOptions, extraOptions);
      }
    } else {
      trace("loadOptions: no extra Options file found");
    }
  }


  private static void mergeExtraOptions(PropertiesConfiguration baseOptions, PropertiesConfiguration extraOptions) {
    if (isNull(extraOptions) || extraOptions.size() == 0) {
      return;
    }
    trace("loadOptions: have to merge extra Options");
    Iterator<String> allKeys = extraOptions.getKeys();
    while (allKeys.hasNext()) {
      String key = allKeys.next();
      if ("sxversion".equals(key)) {
        baseOptions.setProperty("sxversion_saved", extraOptions.getProperty(key));
        continue;
      }
      if ("sxbuild".equals(key)) {
        baseOptions.setProperty("sxbuild_saved", extraOptions.getProperty(key));
        continue;
      }
      Object value = baseOptions.getProperty(key);
      if (isNull(value)) {
        baseOptions.addProperty(key, extraOptions.getProperty(key));
        trace("Option added: %s", key);
      } else {
        Object extraValue = extraOptions.getProperty(key);
        if (!value.getClass().getName().equals(extraValue.getClass().getName()) ||
                !value.toString().equals(extraValue.toString())) {
          baseOptions.setProperty(key, extraValue);
          trace("Option changed: %s = %s", key, extraValue);
        }
      }
    }
  }

//</editor-fold> at start

  //<editor-fold desc="*** handle options at runtime">
  public static void loadOptions(String fpOptions) {
    error("loadOptions: not yet implemented");
  }

  public static boolean saveOptions(String fpOptions) {
    error("saveOptions: not yet implemented");
    return false;
  }

  public static boolean saveOptions() {
    try {
      sxOptions.write(new FileWriter(SX.getFile(SX.getSXSTORE(), "sxoptions.txt")));
    } catch (Exception e) {
      log.error("saveOptions: %s", e);
    }
    return false;
  }

  public static boolean hasOptions() {
    return sxOptions != null && sxOptions.size() > 0;
  }

  public static boolean isOption(String pName) {
    return isOption(pName, false);
  }

  public static boolean isOption(String pName, Boolean bDefault) {
    if (sxOptions == null) {
      return bDefault;
    }
    String pVal = sxOptions.getString(pName, bDefault.toString()).toLowerCase();
    if (pVal.contains("yes") || pVal.contains("true") || pVal.contains("on")) {
      return true;
    }
    return false;
  }

  public static String getOption(String pName) {
    return getOption(pName, "");
  }

  public static String getOption(String pName, String sDefault) {
    if (!hasOptions()) {
      return "";
    }
    return sxOptions.getString(pName, sDefault);
  }

  public static void setOption(String pName, String sValue) {
    sxOptions.setProperty(pName, sValue);
  }

  public static double getOptionNumber(String pName) {
    return getOptionNumber(pName, 0);
  }

  public static double getOptionNumber(String pName, double nDefault) {
    double nVal = sxOptions.getDouble(pName, nDefault);
    return nVal;
  }

  public static Map<String, String> getOptions() {
    Map<String, String> mapOptions = new HashMap<String, String>();
    if (hasOptions()) {
      Iterator<String> allKeys = sxOptions.getKeys();
      while (allKeys.hasNext()) {
        String key = allKeys.next();
        mapOptions.put(key, getOption(key));
      }
    }
    return mapOptions;
  }

  public static void dumpOptions() {
    if (hasOptions()) {
      p("*** options dump");
      for (String sOpt : getOptions().keySet()) {
        p("%s = %s", sOpt, getOption(sOpt));
      }
      p("*** options dump end");
    }
  }

  public static String getValidImageFilename(String fname) {
    String validEndings = ".png.jpg.jpeg.tiff.bmp";
    String defaultEnding = ".png";
    int dot = fname.lastIndexOf(".");
    String ending = defaultEnding;
    if (dot > 0) {
      ending = fname.substring(dot);
      if (validEndings.contains(ending.toLowerCase())) {
        return fname;
      }
    } else {
      fname += ending;
      return fname;
    }
    return "";
  }
  //</editor-fold>

  //<editor-fold desc="*** system/java version info">
  static enum theSystem {
    WIN, MAC, LUX, FOO
  }

  /**
   * @return path seperator : or ;
   */
  public String getSeparator() {
    if (isWindows()) {
      return ";";
    }
    return ":";
  }

  /**
   * ***** Property SXSYSTEM *****
   *
   * @return info about the system running on
   */

  public static String getSYSTEM() {
    if (isNotSet(SXSYSTEM)) {
      String osName = System.getProperty("os.name");
      String osVersion = System.getProperty("os.version");
      if (osName.toLowerCase().startsWith("windows")) {
        SXSYS = theSystem.WIN;
        osName = "Windows";
        SXSYSshort = "windows";
      } else if (osName.toLowerCase().startsWith("mac")) {
        SXSYS = theSystem.MAC;
        osName = "Mac OSX";
        SXSYSshort = "mac";
      } else if (osName.toLowerCase().startsWith("linux")) {
        SXSYS = theSystem.LUX;
        osName = "Linux";
        SXSYSshort = "linux";
      } else {
        terminate(-1, "running on not supported System: %s (%s)", osName, osVersion);
      }
      SYSTEMVERSION = osVersion;
      SXSYSTEM = String.format("%s (%s)", osName, SYSTEMVERSION);
    }
    return SXSYSTEM;
  }

  static String SXSYSTEM = "";
  static theSystem SXSYS = theSystem.FOO;
  static String SXSYSshort = "";


  /**
   * ***** Property SXSYSTEMVERSION *****
   *
   * @return the running system's version info
   */
  public static String getSYSTEMVERSION() {
    if (isNotSet(SYSTEMVERSION)) {
      getSYSTEM();
    }
    return SYSTEMVERSION;
  }

  static String SYSTEMVERSION = "";

  /**
   * @return true/false
   */
  public static boolean isWindows() {
    getSYSTEM();
    return theSystem.WIN.equals(SXSYS);
  }

  /**
   * @return true/false
   */
  public static boolean isLinux() {
    getSYSTEM();
    return theSystem.LUX.equals(SXSYS);
  }

  /**
   * @return true/false
   */
  public static boolean isMac() {
    getSYSTEM();
    return theSystem.MAC.equals(SXSYS);
  }

  public static boolean isOSX10() {
    return getSYSTEMVERSION().startsWith("10.10.") || getSYSTEMVERSION().startsWith("10.11.");
  }

  /**
   * ***** Property ASAPP *****
   *
   * @return to know wether running as .exe/.app
   */
  public static boolean asAPP() {
    if (isNotSet(ASAPP)) {
      //TODO getASAPP detect running as .exe/.app
      setASAPP(false);
    }
    return ASAPP;
  }

  static Boolean ASAPP = null;

  public static boolean setASAPP(boolean val) {
    ASAPP = val;
    return ASAPP;
  }


  /**
   * ***** Property JHOME *****
   *
   * @return the Java installation path
   */
  public static String getJHOME() {
    if (isNotSet(JHOME)) {
      String jhome = System.getProperty("java.home");
      if (isSet(jhome)) {
        JHOME = jhome;
      }
    }
    return JHOME;
  }

  static String JHOME = "";

  /**
   * ***** Property JVERSION *****
   *
   * @return Java version info
   */
  public static String getJVERSION() {
    if (isNotSet(JVERSION)) {
      String vJava = System.getProperty("java.runtime.version");
      String vVM = System.getProperty("java.vm.version");
      String vClass = System.getProperty("java.class.version");
      String vSysArch = System.getProperty("os.arch");
      int javaVersion = 0;
      if (vSysArch == null || !vSysArch.contains("64")) {
        terminate(1, "Java arch not 64-Bit or not detected: JavaSystemProperty::os.arch = %s", vSysArch);
      }
      try {
        javaVersion = Integer.parseInt(vJava.substring(2, 3));
        JVERSION = String.format("Java %s vm %s class %s arch %s", vJava, vVM, vClass, vSysArch);
      } catch (Exception ex) {
        terminate(1, "Java version not detected: JavaSystemProperty::java.runtime.version = %s", vJava);
      }
      if (javaVersion < 7 || javaVersion > 8) {
        terminate(1, "Java version must be 7 or 8");
      }
    }
    return JVERSION;
  }

  static String JVERSION = "";

  /**
   * ***** Property JVERSIONint *****
   *
   * @return Java version number
   */
  public static int getJVERSIONint() {
    if (isNotSet(JVERSIONint)) {
      JVERSIONint = Integer.parseInt(getJVERSION().substring(5, 6));
    }
    return JVERSIONint;
  }

  static Integer JVERSIONint = null;

  public static boolean isJava8() {
    return getJVERSIONint() > 7;
  }

  public static boolean isJava7() {
    return getJVERSIONint() > 6;
  }
  //</editor-fold>

  //<editor-fold desc="*** temp folders">

  /**
   * ***** Property SYSTEMP *****
   *
   * @return the path for temporary stuff according to JavaSystemProperty::java.io.tmpdir
   */
  public static String getSYSTEMP() {
    if (isNotSet(SYSTEMP)) {
      String tmpdir = System.getProperty("java.io.tmpdir");
      if (tmpdir == null || tmpdir.isEmpty() || !getFile(tmpdir).exists()) {
        terminate(1, "JavaSystemProperty::java.io.tmpdir not valid");
      }
      SYSTEMP = getFile(tmpdir).getAbsolutePath();
    }
    return SYSTEMP;
  }

  static String SYSTEMP = "";

  /**
   * ***** Property TEMP *****
   *
   * @return the path to the area where Sikulix stores temporary stuff (located in SYSTEMP)
   */
  public static String getTEMP() {
    if (isNotSet(TEMP)) {
      File fSXTempPath = getFile(getSYSTEMP(), String.format("Sikulix_%d", getRandomInt()));
      for (String aFile : getFile(SYSTEMP).list()) {
        if ((aFile.startsWith("Sikulix") && (new File(aFile).isFile()))
                || (aFile.startsWith("jffi") && aFile.endsWith(".tmp"))) {
          Content.deleteFileOrFolder(new File(getSYSTEMP(), aFile));
        }
      }
      fSXTempPath.mkdirs();
      if (!fSXTempPath.exists()) {
        terminate(1, "getTEMP: could not create: %s", fSXTempPath.getAbsolutePath());
      }
      TEMP = fSXTempPath.getAbsolutePath();
    }
    return TEMP;
  }

  static String TEMP = "";

  /**
   * @return a positive random int > 0 using Java's Random().nextInt()
   */
  static int getRandomInt() {
    int rand = 1 + new Random().nextInt();
    return (rand < 0 ? rand * -1 : rand);
  }
  //</editor-fold>

  //<editor-fold desc="*** user/work/appdata folder">

  /**
   * ***** Property USERHOME *****
   *
   * @return the system specific User's home folder
   */
  public static String getUSERHOME() {
    if (isNotSet(USERHOME)) {
      String aFolder = System.getProperty("user.home");
      if (aFolder == null || aFolder.isEmpty() || !getFile(aFolder).exists()) {
        terminate(-1, "getUSERHOME: JavaSystemProperty::user.home not valid");
      }
      USERHOME = getFile(aFolder).getAbsolutePath();
    }
    return USERHOME;
  }

  static String USERHOME = "";

  /**
   * ***** Property USERWORK *****
   *
   * @return the working folder from JavaSystemProperty::user.dir
   */
  public static String getUSERWORK() {
    if (isNotSet(USERWORK)) {
      String aFolder = System.getProperty("user.dir");
      if (aFolder == null || aFolder.isEmpty() || !new File(aFolder).exists()) {
        terminate(-1, "getUSERWORK: JavaSystemProperty::user.dir not valid");
      }
      USERWORK = getFolder(aFolder).getAbsolutePath();
    }
    return USERWORK;
  }

  static String USERWORK = "";


  /**
   * ***** Property SYSAPP *****
   *
   * @return the system specific path to the users application storage area
   */
  public static String getSYSAPP() {
    if (isNotSet(SYSAPP)) {
      String appDataMsg = "";
      File fSysAppPath = null;
      if (isWindows()) {
        String sDir = System.getenv("APPDATA");
        if (sDir == null || sDir.isEmpty()) {
          terminate(1, "setSYSAPP: Windows: %s not valid", "%APPDATA%");
        }
        fSysAppPath = getFile(sDir);
      } else if (isMac()) {
        fSysAppPath = getFile(getUSERHOME(), "Library/Application Support");
      } else if (isLinux()) {
        fSysAppPath = getFile(getUSERHOME());
        SXAPPdefault = ".Sikulix/SX2";
      }
      SYSAPP = fSysAppPath.getAbsolutePath();
    }
    return SYSAPP;
  }

  static String SYSAPP = "";
  //</editor-fold>

  //<editor-fold desc="*** SX app data folder">
  public static String getSXWEBHOME() {
    if (isNotSet(SXWEBHOME)) {
      SXWEBHOME = SXWEBHOMEdefault;
    }
    return SXWEBHOME;
  }

  static String SXWEBHOME = "";
  static String SXWEBHOMEdefault = "http://sikulix.com";

  public static String getSXWEBDOWNLOAD() {
    if (isNotSet(SXWEBDOWNLOAD)) {
      SXWEBDOWNLOAD = SXWEBDOWNLOADdefault;
    }
    return SXWEBDOWNLOAD;
  }

  static String SXWEBDOWNLOAD = "";
  static String SXWEBDOWNLOADdefault = "http://download.sikulix.com";

  /**
   * ***** Property SXAPP *****
   *
   * @return the path to the area in SYSAPP where Sikulix stores all stuff
   */
  public static String getSXAPP() {
    if (isNotSet(SXAPP)) {
      File fDir = getFile(getSYSAPP(), SXAPPdefault);
      fDir.mkdirs();
      if (!fDir.exists()) {
        terminate(1, "setSXAPP: folder not available or cannot be created: %s", fDir);
      }
      SXAPP = fDir.getAbsolutePath();
    }
    return SXAPP;
  }

  static String SXAPP = "";
  static String SXAPPdefault = "Sikulix/SX2";

  /**
   * ***** Property SXDOWNLOADS *****
   *
   * @return path where Sikulix stores downloaded stuff
   */
  public static String getSXDOWNLOADS() {
    if (isNotSet(SXDOWNLOADS)) {
      String fBase = getSXAPP();
      File fDir = getFile(fBase, SXDOWNLOADSdefault);
      setSXDOWNLOADS(fDir);
    }
    return SXDOWNLOADS;
  }

  static String SXDOWNLOADS = "";
  static String SXDOWNLOADSdefault = "Downloads";

  public static String setSXDOWNLOADS(Object oDir) {
    File fDir = getFile(oDir, null);
    if (isSet(fDir)) {
      fDir.mkdirs();
    }
    if (isNotSet(fDir) || !existsFile(fDir) || !fDir.isDirectory()) {
      terminate(1, "setSXDOWNLOADS: not posssible or not valid: %s", fDir);
    }
    SXDOWNLOADS = fDir.getAbsolutePath();
    return SXDOWNLOADS;
  }

  /**
   * ***** Property SXNATIVE *****
   *
   * @return path where Sikulix stores the native stuff
   */
  public static String getSXNATIVE() {
    if (isNotSet(SXNATIVE)) {
      String fBase = getSXAPP();
      File fDir = getFolder(fBase, SXNATIVEdefault);
      setSXNATIVE(fDir);
    }
    return SXNATIVE;
  }

  static String SXNATIVE = "";
  static String SXNATIVEdefault = "Native";

  public static String setSXNATIVE(Object oDir) {
    File fDir = getFolder(oDir);
    if (isNotSet(fDir) || !existsFile(fDir) || !fDir.isDirectory()) {
      terminate(1, "setSXNATIVE: not posssible or not valid: %s", fDir);
    }
    SXNATIVE = fDir.getAbsolutePath();
    return SXNATIVE;
  }

  /**
   * ***** Property SXLIB *****
   *
   * @return path to folder containing complementary stuff for scripting languages
   */
  public static String getSXLIB() {
    if (isNotSet(SXLIB)) {
      String fBase = getSXAPP();
      File fDir = getFile(fBase, SXLIBdefault);
      setSXLIB(fDir);
    }
    return SXLIB;
  }

  static String SXLIB = "";
  static String SXLIBdefault = "LIB";

  public static String setSXLIB(Object oDir) {
    File fDir = getFile(oDir, null);
    if (isSet(fDir)) {
      fDir.mkdirs();
    }
    if (isNotSet(fDir) || !existsFile(fDir) || !fDir.isDirectory()) {
      terminate(1, "setSXLIB: not posssible or not valid: %s", fDir);
    }
    SXLIB = fDir.getAbsolutePath();
    return SXLIB;
  }

  /**
   * ***** Property SXSTORE *****
   *
   * @return path where other stuff is found or stored at runtime (options, logs, ...)
   */
  public static String getSXSTORE() {
    if (isNotSet(SXSTORE)) {
      String fBase = getSXAPP();
      File fDir = getFile(fBase, SXSTOREdefault);
      setSXSTORE(fDir);
    }
    return SXSTORE;
  }

  static String SXSTORE = "";
  static String SXSTOREdefault = "Store";

  public static String setSXSTORE(Object oDir) {
    File fDir = getFolder(oDir);
    if (isNotSet(fDir) || !existsFile(fDir) || !fDir.isDirectory()) {
      terminate(1, "setSXSTORE: not posssible or not valid: %s", fDir);
    }
    SXSTORE = fDir.getAbsolutePath();
    return SXSTORE;
  }

  /**
   * ***** Property SXEDITOR *****
   *
   * @return path to folder containing supporting stuff for Sikulix IDE
   */
  public static String getSXEDITOR() {
    if (isNotSet(SXEDITOR)) {
      String fBase = getSXAPP();
      File fDir = getFolder(fBase, SXEDITORdefault);
      setSXEDITOR(fDir);
    }
    return SXEDITOR;
  }

  static String SXEDITOR = "";
  static String SXEDITORdefault = "Extensions/SXEditor";

  public static String setSXEDITOR(Object oDir) {
    File fDir = getFile(oDir);
    if (isSet(fDir)) {
      fDir.mkdirs();
    }
    if (isNotSet(fDir) || !existsFile(fDir) || !fDir.isDirectory()) {
      terminate(1, "setSXEDITOR: not posssible or not valid: %s", fDir);
    }
    SXEDITOR = fDir.getAbsolutePath();
    return SXEDITOR;
  }

  /**
   * ***** Property SXTESSERACT *****
   *
   * @return path to folder for stuff supporting Tesseract
   */
  public static String getSXTESSERACT() {
    if (isNotSet(SXTESSERACT)) {
      String fBase = getSXAPP();
      File fDir = getFile(fBase, SXTESSERACTdefault);
      setSXTESSERACT(fDir);
    }
    return SXTESSERACT;
  }

  static String SXTESSERACT = "";
  static String SXTESSERACTdefault = "TESSERACT";

  public static String setSXTESSERACT(Object oDir) {
    File fDir = getFile(oDir, null);
    if (isSet(fDir)) {
      fDir.mkdirs();
    }
    if (isNotSet(fDir) || !existsFile(fDir) || !fDir.isDirectory()) {
      terminate(1, "setSXTESSERACT: not posssible or not valid: %s", fDir);
    }
    SXTESSERACT = fDir.getAbsolutePath();
    return SXTESSERACT;
  }

  /**
   * ***** Property SXEXTENSIONS *****
   *
   * @return path to folder containg extensions or plugins
   */
  public static String getSXEXTENSIONS() {
    if (isNotSet(SXEXTENSIONS)) {
      String fBase = getSXAPP();
      File fDir = getFile(fBase, SXEXTENSIONSdefault);
      setSXEXTENSIONS(fDir);
    }
    return SXEXTENSIONS;
  }

  static String SXEXTENSIONS = "";
  static String SXEXTENSIONSdefault = "Extensions";

  static String[] theExtensions = new String[]{"selenium4sikulix"};

  public static String setSXEXTENSIONS(Object oDir) {
    File fDir = getFile(oDir, null);
    if (isSet(fDir)) {
      fDir.mkdirs();
    }
    if (isNotSet(fDir) || !existsFile(fDir) || !fDir.isDirectory()) {
      terminate(1, "setSXEXTENSIONS: not posssible or not valid: %s", fDir);
    }
    SXEXTENSIONS = fDir.getAbsolutePath();
    return SXEXTENSIONS;
  }

  public static File asExtension(String fpJar) {
    File fJarFound = new File(Content.normalizeAbsolute(fpJar, false));
    if (!fJarFound.exists()) {
      String fpCPEntry = Content.isOnClasspath(fJarFound.getName());
      if (fpCPEntry == null) {
        fJarFound = new File(getSXEXTENSIONS(), fpJar);
        if (!fJarFound.exists()) {
          fJarFound = new File(getSXLIB(), fpJar);
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

  /**
   * ***** Property SXIMAGES *****
   *
   * @return
   */
  public static String getSXIMAGES() {
    if (isNotSet(SXIMAGES)) {
      String fBase = getSXAPP();
      File fDir = getFolder(fBase, SXIMAGESdefault);
      setSXIMAGES(fDir);
    }
    return SXIMAGES;
  }

  static String SXIMAGES = "";
  static String SXIMAGESdefault = "Images";

  public static String setSXIMAGES(Object oDir) {
    File fDir = getFile(oDir, null);
    if (isSet(fDir)) {
      fDir.mkdirs();
    }
    if (isNotSet(fDir) || !existsFile(fDir) || !fDir.isDirectory()) {
      terminate(1, "setSXIMAGES: not posssible or not valid: %s", fDir);
    }
    SXIMAGES = fDir.getAbsolutePath();
    return SXIMAGES;
  }
  //</editor-fold>

  //<editor-fold desc="*** SX version info">

  /**
   * ***** Property VERSION *****
   *
   * @return Sikulix version
   */
  public static String getVERSION() {
    if (isNotSet(VERSION)) {
      String sxVersion = "?sxVersion?";
      String sxBuild = "?sxBuild?";
      String sxVersionShow = "?sxVersionShow?";
      String sxStamp = "?sxStamp?";
      sxVersion = sxOptions.getString("sxversion");
      sxBuild = sxOptions.getString("sxbuild");
      sxBuild = sxBuild.replaceAll("\\-", "");
      sxBuild = sxBuild.replaceAll("_", "");
      sxBuild = sxBuild.replaceAll("\\:", "");
      String sxlocalrepo = Content.slashify(sxOptions.getString("sxlocalrepo"), true);
      String sxJythonVersion = sxOptions.getString("sxjython");
      String sxJRubyVersion = sxOptions.getString("sxjruby");

      debug("getVERSION: version: %s build: %s", sxVersion, sxBuild);
      sxStamp = String.format("%s_%s", sxVersion, sxBuild);

      // used for download of production versions
      String dlProdLink = "https://launchpad.net/raiman/sikulix2013+/";
      String dlProdLinkSuffix = "/+download/";
      // used for download of development versions (nightly builds)
      String dlDevLink = "http://nightly.sikuli.de/";

      sxJythonMaven = "org/python/jython-standalone/"
              + sxJythonVersion + "/jython-standalone-" + sxJythonVersion + ".jar";
      sxJython = sxlocalrepo + sxJythonMaven;
      sxJRubyMaven = "org/jruby/jruby-complete/"
              + sxJRubyVersion + "/jruby-complete-" + sxJRubyVersion + ".jar";
      sxJRuby = sxlocalrepo + sxJRubyMaven;
      tessData.put("eng", "http://download.sikulix.com/tesseract-ocr-3.02.eng.tar.gz");

      sxLibsCheckName = String.format(sxLibsCheckStamp, sxStamp);
      VERSION = sxVersion;
      BUILD = sxBuild;
      VERSIONSHOW = String.format("%s (%s)", sxVersion, sxBuild);
      STAMP = sxStamp;
    }
    return VERSION;
  }

  static String VERSION = "";

  /**
   * ***** Property BUILD *****
   *
   * @return Sikulix build timestamp
   */
  public static String getBUILD() {
    if (isNotSet(BUILD)) {
      getVERSION();
    }
    return BUILD;
  }

  static String BUILD = "";

  /**
   * ***** Property VERSIONSHOW *****
   *
   * @return Version (Build)
   */
  public static String getVERSIONSHOW() {
    if (isNotSet(VERSIONSHOW)) {
      getVERSION();
    }
    return VERSIONSHOW;
  }

  static String VERSIONSHOW = "";

  /**
   * ***** Property STAMP *****
   *
   * @return Version_Build
   */
  public static String getSTAMP() {
    if (isNotSet(STAMP)) {
      getVERSION();
    }
    return STAMP;
  }

  static String STAMP = "";
  //</editor-fold>

  //<editor-fold desc="*** monitor info">

  /**
   * checks, whether Java runs with a valid GraphicsEnvironment (usually means real screens connected)
   *
   * @return false if Java thinks it has access to screen(s), true otherwise
   */
  public static boolean isHeadless() {
    return GraphicsEnvironment.isHeadless();
  }

  public static boolean onTravisCI() {
    return SX.isSet(System.getenv("TRAVIS"), "true");
  }
  //</editor-fold>

  //<editor-fold desc="*** handle native libs">
  public static File fLibsProvided;
  public static boolean useLibsProvided;
  public static String linuxNeededLibs = "";
  public static String linuxAppSupport = "";
  static boolean areLibsExported = false;
  static String fpJarLibs = null;
  static Map<NATIVES, Boolean> libsLoaded = new HashMap<NATIVES, Boolean>();

  static String sxLibsCheckStamp = "MadeForSikuliX_%s";
  static String sflibsCheckFileStored = "MadeForSikuliX2";
  public static String sxLibsCheckName = "";
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
    String syspath = SXJNA.WinKernel32.getEnvironmentVariable("PATH");
    if (syspath == null) {
      terminate(1, "addToWindowsSystemPath: cannot access system path");
    } else {
      String libsPath = (fLibsFolder.getAbsolutePath()).replaceAll("/", "\\");
      if (!syspath.toUpperCase().contains(libsPath.toUpperCase())) {
        if (!SXJNA.WinKernel32.setEnvironmentVariable("PATH", libsPath + ";" + syspath)) {
          terminate(999, "", "");
        }
        syspath = SXJNA.WinKernel32.getEnvironmentVariable("PATH");
        if (!syspath.toUpperCase().contains(libsPath.toUpperCase())) {
          terminate(1, "addToWindowsSystemPath: did not work: %s", syspath);
        }
        debug("addToWindowsSystemPath: added: %s", libsPath);
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
      error("checkJavaUsrPath: get (%s)", ex);
    } catch (SecurityException ex) {
      error("checkJavaUsrPath: get (%s)", ex);
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
          debug("checkJavaUsrPath: added to ClassLoader.usrPaths");
          contained = true;
        }
      } catch (IllegalAccessException ex) {
        error("checkJavaUsrPath: set (%s)", ex);
      } catch (IllegalArgumentException ex) {
        error("checkJavaUsrPath: set (%s)", ex);
      }
      return contained;
    }
    return false;
  }

  static void exportLibraries() {
    if (areLibsExported) {
      return;
    }
    File fSXNative = getFile(getSXNATIVE());
    if (!new File(fSXNative, sxLibsCheckName).exists()) {
      debug("exportLibraries: folder empty or has wrong content");
      Content.deleteFileOrFolder(fSXNative);
    }
    if (fSXNative.exists()) {
      debug("exportLibraries: folder exists: %s", fSXNative);
    } else {
      fSXNative.mkdirs();
      if (!fSXNative.exists()) {
        terminate(1, "exportLibraries: folder not available: %s", fSXNative);
      }
      debug("exportLibraries: new folder: %s", fSXNative);
      fpJarLibs = "/Native/" + SXSYSshort;
      extractLibraries(sxGlobalClassReference, fpJarLibs, fSXNative);
      try {
        extractLibraries(Class.forName("com.sikulix.opencv.Sikulix"), fpJarLibs, fSXNative);
      } catch (ClassNotFoundException e) {
        log.error("exportLibraries: package com.sikulix.opencv not on classpath");
      }
      if (!new File(fSXNative, sflibsCheckFileStored).exists()) {
        terminate(1, "exportLibraries: did not work");
      }
      new File(fSXNative, sflibsCheckFileStored).renameTo(new File(fSXNative, sxLibsCheckName));
      if (!new File(fSXNative, sxLibsCheckName).exists()) {
        terminate(1, "exportLibraries: did not work");
      }
    }
    for (String aFile : fSXNative.list()) {
      if (aFile.contains("opencv_java")) {
        sfLibOpencvJava = aFile;
      } else if (aFile.contains("JXGrabKey")) {
        sfLibJXGrabKey = aFile;
      } else if (aFile.contains("JIntellitype")) {
        sfLibJIntellitype = aFile;
      } else if (aFile.contains("WinUtil")) {
        sfLibWinUtil = aFile;
      } else if (aFile.contains("MacUtil")) {
        sfLibMacUtil = aFile;
      } else if (aFile.contains("MacHotkey")) {
        sfLibMacHotkey = aFile;
      }
    }
    areLibsExported = true;
  }

  private static void extractLibraries(Class classRef, String from, File fTo) {
    String classLocation = whereIs(classRef);
    List<String> libraries;
    String source = classLocation;
    String sourceType = " from jar";
    if (classLocation.endsWith(".jar")) {
      libraries = Content.extractResourcesToFolderFromJar(classLocation, from, fTo, null);
    } else {
      URL uLibsFrom = classRef.getResource(from);
      libraries = Content.extractResourcesToFolder(from, fTo, null);
      source = uLibsFrom.toString();
      sourceType = "";
    }
    int libCount = libraries.size();
    if ( libCount == 0) {
      error("extractLibraries: (none)%s: %s", sourceType, source);
    } else {
      if (libraries.contains("MadeForSikuliX2")) {
        libCount--;
      }
      trace("extractLibraries: (%d)%s: %s", libCount, sourceType, source);
    }
  }

  public static enum NATIVES {
    OPENCV, TESSERACT, SYSUTIL, HOTKEY
  }

  public static boolean loadNative(NATIVES type) {
    boolean success = true;
    if (libsLoaded.isEmpty()) {
      for (NATIVES nType : NATIVES.values()) {
        libsLoaded.put(nType, false);
      }
      exportLibraries();
      if (isWindows()) {
        addToWindowsSystemPath(getFile(getSXNATIVE()));
        if (!checkJavaUsrPath(getFile(getSXNATIVE()))) {
          error("exportLibraries: JavaUserPath: see errors - might not work and crash later");
        }
        String lib = "jawt.dll";
        File fJawtDll = new File(getFile(getSXNATIVE()), lib);
        Content.deleteFileOrFolder(fJawtDll);
        Content.xcopy(new File(getJHOME() + "/bin/" + lib), fJawtDll);
        if (!fJawtDll.exists()) {
          terminate(1, "exportLibraries: problem copying %s", fJawtDll);
        }
      }
    }
    if (OPENCV.equals(type) && !libsLoaded.get(OPENCV)) {
      loadNativeLibrary(sfLibOpencvJava);
    } else if (SYSUTIL.equals(type) && !libsLoaded.get(SYSUTIL)) {
      if (isWindows()) {
        loadNativeLibrary(sfLibWinUtil);
      } else if (isMac()) {
        loadNativeLibrary(sfLibMacUtil);
      }
    } else if (HOTKEY.equals(type) && !libsLoaded.get(HOTKEY)) {
      if (isWindows()) {
        loadNativeLibrary(sfLibJIntellitype);
      } else if (isMac()) {
        loadNativeLibrary(sfLibMacHotkey);
      } else if (isLinux()) {
        loadNativeLibrary(sfLibJXGrabKey);
      }
    } else {
      success = false;
    }
    if (success) {
      libsLoaded.put(type, true);
    }
    return success;
  }

  static void loadNativeLibrary(String aLib) {
    try {
      if (aLib.startsWith("_ext_")) {
        error("loadNativeLibrary: loading external library not implemented: %s", aLib);
      } else {
        String sf_aLib = new File(getSXNATIVE(), aLib).getAbsolutePath();
        System.load(sf_aLib);
        trace("loadNativeLibrary: bundled: %s", aLib);
      }
    } catch (UnsatisfiedLinkError ex) {
      terminate(1, "loadNativeLibrary: loading library error: %s (%s)", aLib, ex.getMessage());
    }
  }
  //</editor-fold>

  //<editor-fold desc="*** global helper methods">

  /**
   * check wether the given object is in JSON format as ["ID", ...]
   *
   * @param json
   * @return true if object is in JSON format, false otherwise
   */
  public static boolean isJSON(Object json) {
    if (json instanceof String) {
      return ((String) json).trim().startsWith("[\"") || ((String) json).trim().startsWith("{\"");
    }
    return false;
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

  public static void show() {
    if (hasOptions()) {
      dumpOptions();
    }
    p("***** show environment (%s)", getVERSIONSHOW());
    p("user.home: %s", getUSERHOME());
    p("user.dir (work dir): %s", getUSERWORK());
    p("java.io.tmpdir: %s", getSYSTEMP());
    p("running on %s", getSYSTEM());
    p(getJVERSION());
    p("app data folder: %s", getSXAPP());
    p("libs folder: %s", getSXNATIVE());
    if (runningJar) {
      p("executing jar: %s", fSxBaseJar);
    }
    Content.dumpClassPath("sikulix");
    //TODO ScriptingHelper
//    if (isJythonReady) {
//      JythonHelper.get().showSysPath();
//    }
    p("***** show environment end");
  }

  private static File getFileMake(Object... args) {
    if (args.length < 1) {
      return null;
    }
    Object oPath = args[0];
    Object oSub = "";
    if (args.length > 1) {
      oSub = args[1];
    }
    File fPath = null;
    if (isNotSet(oSub)) {
      fPath = new File(oPath.toString());
    } else {
      fPath = new File(oPath.toString(), oSub.toString());
    }
    try {
      fPath = fPath.getCanonicalFile();
    } catch (IOException e) {
      error("getFile: %s %s error(%s)", oPath, oSub, e.getMessage());
    }
    return fPath;
  }

  public static File getFile(Object... args) {
    return getFileMake(args);
  }

  public static File getFileExists(Object... args) {
    File fPath = getFileMake(args);
    if (!isNull(fPath) && !fPath.exists()) {
      error("getFile: %s error(not available)", fPath);
      return null;
    }
    return fPath;
  }

  public static File getFolder(Object... args) {
    File aFile = getFileMake(args);
    if (isNotSet(aFile)) {
      return null;
    }
    if (aFile.isDirectory()) {
      return aFile;
    }
    aFile.mkdirs();
    if (aFile.isDirectory()) {
      return aFile;
    }
    error("getFolder: %s error(... does not exist)", aFile);
    return null;
  }

  public static URL getFileURL(Object... args) {
    File aFile = getFile(args);
    if (isNotSet(aFile)) {
      return null;
    }
    try {
      return new URL("file:" + aFile.toString());
    } catch (MalformedURLException e) {
      error("getFileURL: %s error(%s)", aFile, e.getMessage());
      return null;
    }
  }

  public static URL getJarURL(Object... args) {
    if (args.length == 0) {
      return null;
    }
    File aFile = getFile(args[0]);
    if (isNotSet(aFile)) {
      return null;
    }
    String sSub = "";
    if (args.length > 1) {
      sSub = args[1].toString();
    }
    try {
      return new URL("jar:file:" + aFile.toString() + "!/" + sSub);
    } catch (MalformedURLException e) {
      error("getJarURL: %s %s error(%s)", aFile, sSub, e.getMessage());
      return null;
    }
  }

  public static URL getNetURL(Object... args) {
    //TODO implment getNetURL()
    URL netURL = null;
    String path = null;
    if (args.length > 0) {
      String sSub = "";
      if (args.length > 1) {
        sSub = args[1].toString();
        if (sSub.startsWith("/")) {
          sSub = sSub.substring(1);
        }
      }
      if (args[0] instanceof String) {
        path = (String) args[0];
        if (!path.startsWith("http://") && !path.startsWith("https://")) {
          path = "http://" + path;
        }
      } else if (args[0] instanceof URL && ((URL) args[0]).getProtocol().startsWith("http")) {
        path = ((URL) args[0]).toExternalForm();
      } else {
        log.error("getNetURL: invalid arg0: %s", args[0]);
        return null;
      }
      if (!sSub.isEmpty()) {
        if (!path.endsWith("/")) {
          path += "/";
        }
        path += sSub;
      }
      try {
        return new URL(path);
      } catch (MalformedURLException e) {
        error("getURL: %s %s error(%s)", args[0], (args.length > 1 ? args[1] : ""), e.getMessage());
        return null;
      }
    }
    return netURL;
  }

  public static URL getURL(Object... args) {
    URL theURL = null;
    if (args.length > 0) {
      if (args[0] instanceof String) {
        String path = (String) args[0];
        if (path.startsWith("http")) {
          return getNetURL(args);
        } else if (path.startsWith("jar:") || path.endsWith(".jar")) {
          return getJarURL(args);
        } else {
          return getFileURL(args);
        }
      } else if (args[0] instanceof URL) {
        if (((URL) args[0]).getProtocol().startsWith("http")) {
          theURL = getNetURL(args);
        } else {
          log.error("getURL: not implemented: %s", args[0]);
        }
      } else if (args[0] instanceof File) {
        log.error("getURL: File not implemented: %s", args[0]);
      } else {
        log.error("getURL: invalid arg: %s", args[0]);
      }
    }
    return theURL;
  }

  public static boolean existsFile(Object aPath) {
    if (aPath instanceof URL) {
      //TODO implement existsFile(URL)
      return false;
    }
    return (getFile(aPath).exists());
  }

  public static boolean existsFile(Object aPath, String name) {
    if (aPath instanceof URL) {
      //TODO implement existsFile(URL, name)
      return false;
    }
    return (getFile(aPath, name).exists());
  }

  public static boolean existsImageFile(Object aPath, String name) {
    if (aPath instanceof URL) {
      //TODO implement existsImageFile(URL, name)
      return false;
    }
    File imgFile = new File(getValidImageFilename(getFile(aPath, name).getAbsolutePath()));
    return (imgFile.exists());
  }

  public static boolean isNull(Object obj) {
    return null == obj;
  }

  public static boolean isNotNull(Object obj) {
    return null != obj;
  }

  public static boolean isNotSet(Object obj) {
    if (null != obj && obj instanceof String) {
      if (((String) obj).isEmpty()) {
        return true;
      } else {
        return false;
      }
    }
    return null == obj;
  }

  public static boolean isSet(Object obj) {
    if (null != obj && obj instanceof String) {
      if (((String) obj).isEmpty()) {
        return false;
      } else {
        return true;
      }
    }
    return null != obj;
  }

  public static boolean isSet(String var, String val) {
    if (null != var && null != val) {
      if (var.isEmpty()) {
        return false;
      } else {
        return val.equals(var);
      }
    }
    return false;
  }

  public static boolean isRectangleEqual(Object base, Rectangle rect) {
    Rectangle rBase = null;
    if (base instanceof Element) {
      rBase = ((Element) base).getRectangle();
    } else if (base instanceof Region) {
      rBase = ((Region) base).getRect();
    } else if (base instanceof Rectangle) {
      rBase = (Rectangle) base;
    }
    if (SX.isNotNull(rBase)) {
      return rBase.x == rect.x && rBase.y == rect.y && rBase.width == rect.width && rBase.height == rect.height;
    }
    return false;
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

  public static Element at() {
    PointerInfo mp = MouseInfo.getPointerInfo();
    if (mp != null) {
      return new Element(MouseInfo.getPointerInfo().getLocation());
    } else {
      error("not possible to get mouse position (PointerInfo == null)");
      return null;
    }
  }
  //</editor-fold>

  //<editor-fold desc="*** candidates for Content">
  public static String canonicalPath(File aFile) {
    try {
      return aFile.getCanonicalPath();
    } catch (IOException e) {
      return aFile.getAbsolutePath();
    }
  }

  public static URL makeURL(Object... args) {
    if (args.length < 1) {
      return null;
    }

    URL url = null;
    String proto = "file:";
    String sURL = "";

    String fpMain = "";

    Object objMain = args[0];
    String strMain = "";
    String fpSubOrAlt = args.length > 1 ? (String) args[1] : "";
    if (objMain instanceof File) {
      fpMain = canonicalPath((File) objMain);
    } else if (objMain instanceof String) {
      strMain = (String) objMain;
      if ((strMain).startsWith("http")) {
        proto = "http:";
        fpMain = strMain;
      } else {
        fpMain = canonicalPath(getFile(strMain));
        // check for class based path
        if (isSet(strMain) &&
                !new File(fpMain).exists() && !new File(strMain).isAbsolute()) {
          url = makeURLfromClass(strMain, fpSubOrAlt);
        }
      }
    }
    if (isNotSet(url)) {
      if ("file:".equals(proto)) {
        if (fpMain.endsWith(".jar")) {
          if (!existsFile(fpMain)) {
            log.error("makeURL: not exists: %s", fpMain);
          }
          fpMain = "file:" + fpMain + "!/";
          proto = "jar:";
        }
      }
      if (isSet(fpSubOrAlt)) {
        if ("file:".equals(proto)) {
          fpMain = canonicalPath(getFile(fpMain, fpSubOrAlt));
        } else {
          if (!fpSubOrAlt.startsWith("/") && !fpMain.endsWith("/")) {
            fpSubOrAlt = "/" + fpSubOrAlt;
          }
          fpMain += fpSubOrAlt;
        }
      }
      if ("http:".equals(proto)) {
        sURL = fpMain;
      } else {
        if ("file:".equals(proto) && !existsFile(fpMain)) {
          log.error("makeURL: not exists: %s", fpMain);
        }
        sURL = proto + fpMain;
      }
      try {
        url = new URL(sURL);
      } catch (MalformedURLException e) {
        log.error("makeURL: not valid: %s %s", objMain, (isNotSet(fpSubOrAlt) ? "" : ", " + fpSubOrAlt));
      }
    }
    return url;
  }

  private static URL makeURLfromClass(String fpMain, String fpAlt) {
    URL url = null;
    Class cls = null;
    String klassName;
    String fpSubPath = "";
    int n = fpMain.indexOf("/");
    if (n > 0) {
      klassName = fpMain.substring(0, n);
      if (n < fpMain.length() - 2) {
        fpSubPath = fpMain.substring(n + 1);
      }
    } else {
      klassName = fpMain;
    }
    if (".".equals(klassName)) {
      if (isSet(SX.getBaseClass())) {
        klassName = SX.getBaseClass();
      } else {
        klassName = sxGlobalClassReference.getName();
      }
    }
    try {
      cls = Class.forName(klassName);
    } catch (ClassNotFoundException ex) {
      log.error("makeURLfromPath: class %s not found on classpath.", klassName);
    }
    if (cls != null) {
      CodeSource codeSrc = cls.getProtectionDomain().getCodeSource();
      if (codeSrc != null && codeSrc.getLocation() != null) {
        url = codeSrc.getLocation();
        if (url.getPath().endsWith(".jar")) {
          url = getJarURL(url.getPath(), fpSubPath);
        } else {
          if (isNotSet(fpAlt)) {
            url = getFileURL(url.getPath(), fpSubPath);
          } else {
            url = getFileURL(fpAlt);
          }
        }
      }
    }
    return url;
  }

  public static String makePath(URL uPath) {
    String sPath = "";
    String proto = "";
    sPath = uPath.getPath();
    proto = uPath.getProtocol();
    if ("file".equals(proto) || "jar".equals(proto)) {
      if ("jar".equals(proto)) {
        sPath = sPath.replaceFirst("file:", "");
        sPath = sPath.split("!/")[0];
      }
      sPath = getFile(sPath).getAbsolutePath();
    } else {
      sPath = uPath.toExternalForm();
    }
    return sPath;
  }
  //</editor-fold>

  /**
   * ***** Property LOCALDEVICE *****
   *
   * @return
   */
  public static LocalDevice getLOCALDEVICE() {
    if (isNotSet(LOCALDEVICE)) {
      LOCALDEVICE = (LocalDevice) new LocalDevice().start();
    }
    return LOCALDEVICE;
  }

  public static boolean isSetLOCALDEVICE() {
    return SX.isNotNull(LOCALDEVICE);
  }

  public static void setLOCALDEVICE(LocalDevice LOCALDEVICE) {
    SX.LOCALDEVICE = LOCALDEVICE;
  }

  private static LocalDevice LOCALDEVICE = null;

  private static String baseClass = "";

  public static void setBaseClass() {
    log.trace("setBaseClass: start");
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    boolean takeit = false;
    for (StackTraceElement traceElement : stackTrace) {
      String tName = traceElement.getClassName();
      if (takeit) {
        baseClass = tName;
        break;
      }
      if (tName.equals(SX.class.getName())) {
        takeit = true;
      }
    }
  }

  public static String getBaseClass() {
    return baseClass;
  }
}
