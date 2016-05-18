/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.sikulix.scripting.JythonHelper;
import org.apache.commons.cli.*;
import org.sikuli.script.App;
import org.sikuli.script.Key;
import org.sikuli.util.SysJNA;
import org.sikuli.util.hotkey.HotkeyListener;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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

  private static String sxLock = "";
  private static FileOutputStream isRunningFile = null;
  static final Class sxGlobalClassReference = SX.class;

  static void sxinit(String[] args) {
    if (null == sxInstance) {
      sxInstance = "SX INIT DONE";

      //<editor-fold desc="*** shutdown hook">
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          trace("final cleanup");
          if (!isUnset(sxLock)) {
            try {
              isRunningFile.close();
            } catch (IOException ex) {
            }
            getFile(sxLock).delete();
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
                  if (isObsolete || aFile.equals(getFile(getSXTEMP()))) {
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

      // TODO Content class must be initialized for use in shutdown
      Content.start();

      //<editor-fold desc="*** sx lock">
      File fLock = new File(getSYSTEMP(), "SikuliX2-i-s-r-u-n-n-i-n-g");
      String shouldTerminate = "";
      try {
        fLock.createNewFile();
        isRunningFile = new FileOutputStream(fLock);
        if (isNull(isRunningFile.getChannel().tryLock())) {
          shouldTerminate = "SikuliX2 already running";
        }
      } catch (Exception ex) {
        shouldTerminate = "cannot access SX2 lock: " + ex.toString();
      }
      if (!isUnset(shouldTerminate)) {
        terminate(1, shouldTerminate);
      }
      //</editor-fold>

      // *** command line args
      if (!isNull(args)) {
        checkArgs(args);
      }

      trace("sxinit: starting");

      // *** get the version info
      getSXVERSION();

      // *** check how we are running
      sxRunningAs();

      // *** get SX options
      loadOptions();

      // *** get monitor setup
      globalGetMonitors();

      trace("sxinit: complete %.3f", (new Date().getTime() - startTime) / 1000.0f);
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
  static File fSxProject;
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

  static void sxRunningAs() {
    CodeSource codeSrc = sxGlobalClassReference.getProtectionDomain().getCodeSource();
    String base = null;
    if (codeSrc != null && codeSrc.getLocation() != null) {
      base = Content.slashify(codeSrc.getLocation().getPath(), false);
    }
    appType = "from a jar";
    if (base != null) {
      fSxBaseJar = new File(base);
      String jn = fSxBaseJar.getName();
      fSxBase = fSxBaseJar.getParentFile();
      debug("runs as %s in:\n%s", jn, fSxBase.getAbsolutePath());
      if (jn.contains("classes")) {
        runningJar = false;
        fSxProject = fSxBase.getParentFile().getParentFile();
        debug("not jar - supposing Maven project:\n%s", fSxProject);
        appType = "in Maven project from classes";
        runningInProject = true;
      } else if ("target".equals(fSxBase.getName())) {
        fSxProject = fSxBase.getParentFile().getParentFile();
        debug("folder target detected - supposing Maven project:\n%s", fSxProject);
        appType = "in Maven project from some jar";
        runningInProject = true;
      } else {
        if (isWindows()) {
          if (jn.endsWith(".exe")) {
            setSXASAPP(true);
            runningJar = false;
            appType = "as application .exe";
          }
        } else if (isMac()) {
          if (fSxBase.getAbsolutePath().contains("SikuliX.app/Content")) {
            setSXASAPP(true);
            appType = "as application .app";
            if (!fSxBase.getAbsolutePath().startsWith("/Applications")) {
              appType += " (not from /Applications folder)";
            }
          }
        }
      }
    } else {
      terminate(1, "no valid Java context for SikuliX available "
              + "(java.security.CodeSource.getLocation() is null)");
    }
  }
  //</editor-fold>

  //<editor-fold desc="*** get SX options">
  private static File fOptions;
  private static Properties options = null;
  private static String fnOptions = "SXOptions.txt";

  private static void loadOptions() {
    for (String sFile : new String[]{getUSERWORK(), getUSERHOME(), getSXSTORE()}) {
      File aFile = getFile(sFile);
      trace("loadOptions: check: %s", aFile);
      fOptions = new File(aFile, fnOptions);
      if (fOptions.exists()) {
        break;
      } else {
        fOptions = null;
      }
    }
    if (fOptions != null) {
      options = new Properties();
      try {
        InputStream is;
        is = new FileInputStream(fOptions);
        options.load(is);
        is.close();
      } catch (Exception ex) {
        error("while checking Options file:\n%s", fOptions);
        fOptions = null;
        options = null;
      }
      debug("found Options file at: %s", fOptions);
    }
    if (hasOptions()) {
      for (Object oKey : options.keySet()) {
        String sKey = (String) oKey;
        String[] parts = sKey.split("\\.");
        if (parts.length == 1) {
          continue;
        }
        String sClass = parts[0];
        String sAttr = parts[1];
        Class cClass = null;
        Field cField = null;
        Class ccField = null;
        if (sClass.contains("Settings")) {
          try {
            cClass = Class.forName("org.sikuli.basics.Settings");
            cField = cClass.getField(sAttr);
            ccField = cField.getType();
            if (ccField.getName() == "boolean") {
              cField.setBoolean(null, isOption(sKey));
            } else if (ccField.getName() == "int") {
              cField.setInt(null, getOptionNumber(sKey));
            } else if (ccField.getName() == "float") {
              cField.setInt(null, getOptionNumber(sKey));
            } else if (ccField.getName() == "double") {
              cField.setInt(null, getOptionNumber(sKey));
            } else if (ccField.getName() == "String") {
              cField.set(null, getOption(sKey));
            }
          } catch (Exception ex) {
            error("loadOptions: not possible: %s = %s", sKey, options.getProperty(sKey));
          }
        }
      }
    }
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
    if (isUnset(SXSYSTEM)) {
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
    if (isUnset(SYSTEMVERSION)) {
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
   * ***** Property SXASAPP *****
   *
   * @return to know wether running as .exe/.app
   */
  public static boolean isSXASAPP() {
    if (isUnset(SXASAPP)) {
      //TODO isSXASAPP detect running as .exe/.app
      setSXASAPP(false);
    }
    return SXASAPP;
  }

  static Boolean SXASAPP = null;

  public static boolean setSXASAPP(boolean val) {
    SXASAPP = val;
    return SXASAPP;
  }


  /**
   * ***** Property JHOME *****
   *
   * @return the Java installation path
   */
  public static String getJHOME() {
    if (isUnset(JHOME)) {
      String jhome = System.getProperty("java.home");
      if (!isUnset(jhome)) {
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
    if (isUnset(JVERSION)) {
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
    if (isUnset(JVERSIONint)) {
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
    if (isUnset(SYSTEMP)) {
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
   * ***** Property SXTEMP *****
   *
   * @return the path to the area where Sikulix stores temporary stuff (located in SYSTEMP)
   */
  public static String getSXTEMP() {
    if (isUnset(SXTEMP)) {
      File fSXTempPath = getFile(getSYSTEMP(), String.format("Sikulix_%d", getRandomInt()));
      for (String aFile : getFile(SYSTEMP).list()) {
        if ((aFile.startsWith("Sikulix") && (new File(aFile).isFile()))
                || (aFile.startsWith("jffi") && aFile.endsWith(".tmp"))) {
          Content.deleteFileOrFolder(new File(getSYSTEMP(), aFile));
        }
      }
      fSXTempPath.mkdirs();
      if (!fSXTempPath.exists()) {
        terminate(1, "getSXTEMP: could not create: %s", fSXTempPath.getAbsolutePath());
      }
      SXTEMP = fSXTempPath.getAbsolutePath();
    }
    return SXTEMP;
  }

  static String SXTEMP = "";

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
    if (isUnset(USERHOME)) {
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
    if (isUnset(USERWORK)) {
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
    if (isUnset(SYSAPP)) {
      String appDataMsg = "";
      File fSysAppPath = null;
      if (isWindows()) {
        String sDir = System.getenv("APPDATA");
        if (sDir == null || sDir.isEmpty()) {
          terminate(1, "setSYSAPP: Windows: %s not valid", "%APPDATA%");
        }
        fSysAppPath = getFile(sDir);
      } else if (isMac()) {
        fSysAppPath = getFile(USERHOME, "Library/Application Support");
      } else if (isLinux()) {
        fSysAppPath = getFile(USERHOME);
        SXAPPdefault = ".Sikulix/SX2";
      }
      SYSAPP = fSysAppPath.getAbsolutePath();
    }
    return SYSAPP;
  }

  static String SYSAPP = "";
  //</editor-fold>

  //<editor-fold desc="*** SX app data folder">

  /**
   * ***** Property SXAPP *****
   *
   * @return the path to the area in SYSAPP where Sikulix stores all stuff
   */
  public static String getSXAPP() {
    if (isUnset(SXAPP)) {
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
    if (isUnset(SXDOWNLOADS)) {
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
    if (!isUnset(fDir)) {
      fDir.mkdirs();
    }
    if (isUnset(fDir) || !existsFile(fDir) || !fDir.isDirectory()) {
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
    if (isUnset(SXNATIVE)) {
      String fBase = getSXAPP();
      File fDir = getFile(fBase, SXNATIVEdefault);
      setSXNATIVE(fDir);
    }
    return SXNATIVE;
  }

  static String SXNATIVE = "";
  static String SXNATIVEdefault = "Native";

  public static String setSXNATIVE(Object oDir) {
    File fDir = getFolder(oDir);
    if (isUnset(fDir) || !existsFile(fDir) || !fDir.isDirectory()) {
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
    if (isUnset(SXLIB)) {
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
    if (!isUnset(fDir)) {
      fDir.mkdirs();
    }
    if (isUnset(fDir) || !existsFile(fDir) || !fDir.isDirectory()) {
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
    if (isUnset(SXSTORE)) {
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
    if (isUnset(fDir) || !existsFile(fDir) || !fDir.isDirectory()) {
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
    if (isUnset(SXEDITOR)) {
      String fBase = getSXAPP();
      File fDir = getFile(fBase, SXEDITORdefault);
      setSXEDITOR(fDir);
    }
    return SXEDITOR;
  }

  static String SXEDITOR = "";
  static String SXEDITORdefault = "Editor";

  public static String setSXEDITOR(Object oDir) {
    File fDir = getFile(oDir, null);
    if (!isUnset(fDir)) {
      fDir.mkdirs();
    }
    if (isUnset(fDir) || !existsFile(fDir) || !fDir.isDirectory()) {
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
    if (isUnset(SXTESSERACT)) {
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
    if (!isUnset(fDir)) {
      fDir.mkdirs();
    }
    if (isUnset(fDir) || !existsFile(fDir) || !fDir.isDirectory()) {
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
    if (isUnset(SXEXTENSIONS)) {
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
    if (!isUnset(fDir)) {
      fDir.mkdirs();
    }
    if (isUnset(fDir) || !existsFile(fDir) || !fDir.isDirectory()) {
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
    if (isUnset(SXIMAGES)) {
      String fBase = getSXAPP();
      File fDir = getFile(fBase, SXIMAGESdefault);
      setSXIMAGES(fDir);
    }
    return SXIMAGES;
  }

  static String SXIMAGES = "";
  static String SXIMAGESdefault = "Images";

  public static String setSXIMAGES(Object oDir) {
    File fDir = getFile(oDir, null);
    if (!isUnset(fDir)) {
      fDir.mkdirs();
    }
    if (isUnset(fDir) || !existsFile(fDir) || !fDir.isDirectory()) {
      terminate(1, "setSXIMAGES: not posssible or not valid: %s", fDir);
    }
    SXIMAGES = fDir.getAbsolutePath();
    return SXIMAGES;
  }
  //</editor-fold>

  //<editor-fold desc="*** SX version info">

  /**
   * ***** Property SXVERSION *****
   *
   * @return Sikulix version
   */
  public static String getSXVERSION() {
    if (isUnset(SXVERSION)) {
      String sxVersion = "?sxVersion?";
      String sxBuild = "?sxBuild?";
      String sxVersionShow = "?sxVersionShow?";
      String sxStamp = "?sxStamp?";
      Properties prop = new Properties();
      String sVersionFile = "sikulixversion.txt";
      try {
        InputStream is;
        is = sxGlobalClassReference.getClassLoader().getResourceAsStream("Settings/" + sVersionFile);
        if (is == null) {
          terminate(1, "getSXVERSION: not found on classpath: %s", "Settings/" + sVersionFile);
        }
        prop.load(is);
        is.close();

        sxVersion = prop.getProperty("sxversion");
        sxBuild = prop.getProperty("sxbuild");
        sxBuild = sxBuild.replaceAll("\\-", "");
        sxBuild = sxBuild.replaceAll("_", "");
        sxBuild = sxBuild.replaceAll("\\:", "");
        String sxlocalrepo = Content.slashify(prop.getProperty("sxlocalrepo"), true);
        String sxJythonVersion = prop.getProperty("sxjython");
        String sxJRubyVersion = prop.getProperty("sxjruby");

        debug("version: %s build: %s", sxVersion, sxBuild);
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
      } catch (Exception e) {
        terminate(1, "getSXVERSION: load failed for %s error(%s)", sVersionFile, e.getMessage());
      }
      tessData.put("eng", "http://tesseract-ocr.googlecode.com/files/tesseract-ocr-3.02.eng.tar.gz");

      sxLibsCheckName = String.format(sxLibsCheckStamp, sxStamp);
      SXVERSION = sxVersion;
      SXBUILD = sxBuild;
      SXVERSIONSHOW = String.format("%s (%s)", sxVersion, sxBuild);
      SXSTAMP = sxStamp;
    }
    return SXVERSION;
  }

  static String SXVERSION = "";

  /**
   * ***** Property SXBUILD *****
   *
   * @return Sikulix build timestamp
   */
  public static String getSXBUILD() {
    if (isUnset(SXBUILD)) {
      getSXVERSION();
    }
    return SXBUILD;
  }

  static String SXBUILD = "";

  /**
   * ***** Property SXVERSIONSHOW *****
   *
   * @return Version (Build)
   */
  public static String getSXVERSIONSHOW() {
    if (isUnset(SXVERSIONSHOW)) {
      getSXVERSION();
    }
    return SXVERSIONSHOW;
  }

  static String SXVERSIONSHOW = "";

  /**
   * ***** Property SXSTAMP *****
   *
   * @return Version_Build
   */
  public static String getSXSTAMP() {
    if (isUnset(SXSTAMP)) {
      getSXVERSION();
    }
    return SXSTAMP;
  }

  static String SXSTAMP = "";
  //</editor-fold>

  //<editor-fold desc="*** monitor info">
  static GraphicsEnvironment genv = null;
  static GraphicsDevice[] gdevs;
  static Rectangle[] monitorBounds = null;
  static Rectangle rAllMonitors;
  public static int mainMonitor = -1;
  public static int nMonitors = 0;

  static void globalGetMonitors() {
    if (!isHeadless()) {
      genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
      gdevs = genv.getScreenDevices();
      nMonitors = gdevs.length;
      if (nMonitors == 0) {
        terminate(1, "GraphicsEnvironment has no ScreenDevices");
      }
      monitorBounds = new Rectangle[nMonitors];
      rAllMonitors = null;
      Rectangle currentBounds;
      for (int i = 0; i < nMonitors; i++) {
        currentBounds = new Rectangle(gdevs[i].getDefaultConfiguration().getBounds());
        if (null != rAllMonitors) {
          rAllMonitors = rAllMonitors.union(currentBounds);
        } else {
          rAllMonitors = currentBounds;
        }
        if (currentBounds.contains(new Point())) {
          if (mainMonitor < 0) {
            mainMonitor = i;
            debug("ScreenDevice %d has (0,0) --- will be primary Screen(0)", i);
          } else {
            debug("ScreenDevice %d too contains (0,0)!", i);
          }
        }
        debug("Monitor %d: (%d, %d) %d x %d", i,
                currentBounds.x, currentBounds.y, currentBounds.width, currentBounds.height);
        monitorBounds[i] = currentBounds;
      }
      if (mainMonitor < 0) {
        debug("No ScreenDevice has (0,0) --- using 0 as primary: %s", monitorBounds[0]);
        mainMonitor = 0;
      }
    } else {
      error("running in headless environment");
    }
  }

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
    return new Region(monitorBounds[n]);
  }

  public static Region getMonitor() {
    if (isHeadless()) {
      return new Region();
    }
    return new Region(monitorBounds[mainMonitor]);
  }

  public static GraphicsDevice getGraphicsDevice(int id) {
    return gdevs[id];
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
    String syspath = SysJNA.WinKernel32.getEnvironmentVariable("PATH");
    if (syspath == null) {
      terminate(1, "addToWindowsSystemPath: cannot access system path");
    } else {
      String libsPath = (fLibsFolder.getAbsolutePath()).replaceAll("/", "\\");
      if (!syspath.toUpperCase().contains(libsPath.toUpperCase())) {
        if (!SysJNA.WinKernel32.setEnvironmentVariable("PATH", libsPath + ";" + syspath)) {
          terminate(999, "", "");
        }
        syspath = SysJNA.WinKernel32.getEnvironmentVariable("PATH");
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

  static void exportNativeLibraries() {
    if (areLibsExported) {
      return;
    }
    File fSXNative = getFile(getSXNATIVE());
    if (!new File(fSXNative, sxLibsCheckName).exists()) {
      debug("exportNativeLibraries: folder empty or has wrong content");
      Content.deleteFileOrFolder(fSXNative);
    }
    if (fSXNative.exists()) {
      debug("exportNativeLibraries: folder exists: %s", fSXNative);
    } else {
      fSXNative.mkdirs();
      if (!fSXNative.exists()) {
        terminate(1, "exportNativeLibraries: folder not available: %s", fSXNative);
      }
      debug("exportNativeLibraries: new folder: %s", fSXNative);
      fpJarLibs = "/Native/" + SXSYSshort;
      URL uLibsFrom = sxGlobalClassReference.getResource(fpJarLibs);
      if (uLibsFrom == null) {
        Content.dumpClassPath("sikulix");
        terminate(1, "exportNativeLibraries: libs not on classpath: " + fpJarLibs);
      }
      debug("exportNativeLibraries: from: %s", uLibsFrom);
      Content.extractResourcesToFolder(fpJarLibs, fSXNative, null);
      if (!new File(fSXNative, sflibsCheckFileStored).exists()) {
        terminate(1, "exportNativeLibraries: did not work");
      }
      new File(fSXNative, sflibsCheckFileStored).renameTo(new File(fSXNative, sxLibsCheckName));
      if (!new File(fSXNative, sxLibsCheckName).exists()) {
        terminate(1, "exportNativeLibraries: did not work");
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

  public static enum NATIVES {
    OPENCV, TESSERACT, SYSUTIL, HOTKEY
  }

  static void loadNative(NATIVES type) {
    if (libsLoaded.isEmpty()) {
      for (NATIVES nType : NATIVES.values()) {
        libsLoaded.put(nType, false);
      }
      exportNativeLibraries();
      if (isWindows()) {
        addToWindowsSystemPath(getFile(getSXNATIVE()));
        if (!checkJavaUsrPath(getFile(getSXNATIVE()))) {
          error("exportNativeLibraries: JavaUserPath: see errors - might not work and crash later");
        }
        String lib = "jawt.dll";
        File fJawtDll = new File(getFile(getSXNATIVE()), lib);
        Content.deleteFileOrFolder(fJawtDll);
        Content.xcopy(new File(getJHOME() + "/bin/" + lib), fJawtDll);
        if (!fJawtDll.exists()) {
          terminate(1, "exportNativeLibraries: problem copying %s", fJawtDll);
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
      terminate(1, "loadNative: %s not yet supported", type);
    }
    libsLoaded.put(type, true);
  }

  static void loadNativeLibrary(String aLib) {
    try {
      if (aLib.startsWith("_ext_")) {
        terminate(1, "loadNativeLibrary: loading external library not implemented: %s", aLib);
      } else {
        String sf_aLib = new File(getSXNATIVE(), aLib).getAbsolutePath();
        System.load(sf_aLib);
        debug("loadNativeLibrary: bundled: %s", aLib);
      }
    } catch (UnsatisfiedLinkError ex) {
      terminate(1, "loadNativeLibrary: loading library error: %s (%s)", aLib, ex.getMessage());
    }
  }
  //</editor-fold>

  //<editor-fold desc="*** handle options">
  public static void loadOptions(String fpOptions) {
    error("loadOptions: not yet implemented");
  }

  public static boolean saveOptions(String fpOptions) {
    error("saveOptions: not yet implemented");
    return false;
  }

  public static boolean saveOptions() {
    error("saveOptions: not yet implemented");
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
  //</editor-fold>

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

  //<editor-fold desc="*** global helper methods">
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
    p("***** show environment (%s)", getSXVERSIONSHOW());
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
    if (isJythonReady) {
      JythonHelper.get().showSysPath();
    }
    p("***** show environment end");
  }

  public static File getFile(Object... args) {
    if (args.length < 1) {
      return null;
    }
    Object oPath = args[0];
    Object oSub = "";
    if (args.length > 1) {
      oSub = args[1];
    }
    File fPath = null;
    if (isUnset(oSub)) {
      fPath = new File(oPath.toString());
    } else {
      fPath = new File(oPath.toString(), oSub.toString());
    }
    try {
      return fPath.getCanonicalFile();
    } catch (IOException e) {
      error("getFile: %s %s error(%s)", oPath, oSub, e.getMessage());
      return null;
    }
  }

  public static File getFolder(Object... args) {
    File aFile = getFile(args);
    if (isUnset(aFile)) {
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
    if (isUnset(aFile)) {
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
    File aFile = getFile(args);
    if (isUnset(aFile)) {
      return null;
    }
    String sSub = "";
    if (args.length > 2) {
      sSub = args[2].toString();
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
    return netURL;
  }

  public static boolean existsFile(Object aPath) {
    if (aPath instanceof URL) {
      //TODO implement existsFile(URL)
    }
    return (getFile(aPath).exists());
  }

  public static boolean isNull(Object obj) {
    return null == obj;
  }

  public static boolean isUnset(Object obj) {
    if (obj instanceof String && ((String) obj).isEmpty()) return true;
    return null == obj;
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

  public static Location at() {
    PointerInfo mp = MouseInfo.getPointerInfo();
    if (mp != null) {
      return new Location(MouseInfo.getPointerInfo().getLocation());
    } else {
      error("not possible to get mouse position (PointerInfo == null)");
      return null;
    }
  }
  //</editor-fold>

  //<editor-fold desc="*** candidates for Content">
  public static String canoPath(File aFile) {
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

    Object arg0 = args[0];
    String fpSub = args.length > 1 ? (String) args[1] : "";
    if (arg0 instanceof File) {
      fpMain = canoPath((File) arg0);
    } else if (arg0 instanceof String) {
      if (((String) arg0).startsWith("http")) {
        proto = "http:";
        fpMain = (String) arg0;
      } else {
        fpMain = canoPath(getFile(arg0.toString()));
      }
    }
    if ("file:".equals(proto)) {
      if (fpMain.endsWith(".jar")) {
        if (!existsFile(fpMain)) {
          log.error("makeURL: not exists: %s", fpMain);
        }
        fpMain = "file:" + fpMain + "!/";
        proto = "jar:";
      }
    }
    if (!isUnset(fpSub)) {
      if ("file:".equals(proto)) {
        fpMain = canoPath(getFile(fpMain, fpSub));
      } else {
        if (!fpSub.startsWith("/")) {
          fpSub = "/" + fpSub;
        }
        fpMain += fpSub;
      }
    }
    if (!"http:".equals(proto)) {
      if ("file:".equals(proto) && isUnset(getFolder(fpMain))) {
        log.error("makeURL: not exists: %s", fpMain);
      }
      sURL = proto + fpMain;
    } else {
      sURL = fpMain;
    }
    try {
      url = new URL(sURL);
    } catch (MalformedURLException e) {
      log.error("makeURL: not valid: %s %s", arg0, (isUnset(fpSub) ? "" : ", " + fpSub));
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
        sPath = sPath.replaceFirst("!/", "/");
      }
      sPath = getFile(sPath).getAbsolutePath();
    } else {
      sPath = uPath.toExternalForm();
    }
    return sPath;
  }
  //</editor-fold>

  /**
   * ***** Property SXROBOT *****
   *
   * @return
   */
  public static Robot getSXROBOT() {
    if (isUnset(SXROBOT)) {
      try {
        SXROBOT = new Robot();
      } catch (AWTException e) {
        terminate(1, "getSXROBOT: not possible: %s", e.getMessage());
      }
    }
    return SXROBOT;
  }

  static Robot SXROBOT = null;
}
