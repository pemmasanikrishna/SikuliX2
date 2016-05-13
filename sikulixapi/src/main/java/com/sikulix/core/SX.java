/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import org.apache.commons.cli.*;
import org.sikuli.script.*;
import org.sikuli.util.SysJNA;
import org.sikuli.util.hotkey.HotkeyListener;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.*;
import java.security.CodeSource;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SX {

  // ******************************* Logging ****************************
  static int globalLogLevel = 0;
  static final int lvl = SXLog.DEBUG;

  static final SXLog log = new SXLog();

  public static void log(int level, String message, Object... args) {
    log.log(level, message, args);
  }

  public static void logp(String message, Object... args) {
    log(log.INFO, message, args);
  }

  public static void terminate(int retval, String message, Object... args) {
    log.log(SXLog.FATAL, message, args);
    System.exit(retval);
  }

  public static void p(String msg, Object... args) {
    System.out.println(String.format(msg, args));
  }

  public static SXLog getLogger(String className) {
    return getLogger(className, null);
  }

  public static SXLog getLogger(String className, String[] args) {
    return new SXLog(className, args);
  }

  private static String sxInstance = "";

  static String sxLock = "";
  static FileOutputStream isRunningFile = null;

  public static boolean isIDE() {
    return false;
  }

  public static void sxinit(String[] args) {
    if (null != args) {
      checkArgs(args);
    }
    if (isUnset(sxInstance)) {
      log(lvl, "sxinit: starting");
      sxGlobalClassReference = SX.class;

      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          log(lvl, "final cleanup");
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
            log(lvl, "cleanTemp: " + f.getName());
            ContentManager.deleteFileOrFolder("#" + f.getAbsolutePath());
          }
        }
      });

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
    }
    sxInstance = "DONE";
  }

  static List<String> sxArgs = new ArrayList<String>();
  static List<String> userArgs = new ArrayList<String>();
  static CommandLine sxCommandArgs = null;

  static void checkArgs(String[] args) {
    boolean hasUserArgs = false;
    boolean hasOptDebug = false;
    for (String arg : args) {
      if ("-d".equals(arg)) {
        hasOptDebug = true;
        p("*** Commandline Args ***");
      }
      if ("--".equals(arg)) {
        if (hasOptDebug) {
          p(">>> user args");
        }
        hasUserArgs = true;
        continue;
      }
      if (hasOptDebug) p("%s", arg);
      if (hasUserArgs) {
        userArgs.add(arg);
      } else {
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
      try {
        sxCommandArgs = parser.parse(opts, args);
      } catch (ParseException e) {
        p("Error: checkArgs: %s", e.getMessage());
      }
      if (sxCommandArgs != null) {
        if (hasOptDebug) {
          p("***** SikuliX Args");
        }
        for (Option o : sxCommandArgs.getOptions()) {
          if (sxCommandArgs.hasOption(o.getOpt())) {
            if (hasOptDebug) {
              p("%s: %s", o.getOpt(), sxCommandArgs.getOptionValue(o.getOpt()));
            }
          }
        }
        if (hasOptDebug) {
          p("***** END SikuliX Args");
          String dVal = getArg("d");
          if (dVal.isEmpty()) {
            globalLogLevel = 3;
          } else {
            try {
              globalLogLevel = Integer.parseInt(dVal);
            } catch (Exception ex) {
              globalLogLevel = -1;
            }
            if (globalLogLevel < 0) {
              p("Error: checkArgs: -d %s not valid", dVal);
            }
          }
          if (globalLogLevel > 0) {
            p("Info: checkArgs: globalLogLevel = %d", globalLogLevel);
          }
        }
      } else {
        p("Error: checkArgs: no valid args");
      }
    }
  }

  static String getArg(String arg) {
    if (sxCommandArgs != null && sxCommandArgs.hasOption(arg)) {
      String val = sxCommandArgs.getOptionValue(arg);
      return val == null ? "" : val;
    }
    return null;
  }

  public static String[] getUserArgs() {
    return userArgs.toArray(new String[0]);
  }

  static void globalGetSupport() {
    CodeSource codeSrc = sxGlobalClassReference.getProtectionDomain().getCodeSource();
    String base = null;
    if (codeSrc != null && codeSrc.getLocation() != null) {
      base = ContentManager.slashify(codeSrc.getLocation().getPath(), false);
    }
    appType = "from a jar";
    if (base != null) {
      fSxBaseJar = new File(base);
      String jn = fSxBaseJar.getName();
      fSxBase = fSxBaseJar.getParentFile();
      log(lvl, "runs as %s in:\n%s", jn, fSxBase.getAbsolutePath());
      if (jn.contains("classes")) {
        runningJar = false;
        fSxProject = fSxBase.getParentFile().getParentFile();
        log(lvl, "not jar - supposing Maven project:\n%s", fSxProject);
        appType = "in Maven project from classes";
        runningInProject = true;
      } else if ("target".equals(fSxBase.getName())) {
        fSxProject = fSxBase.getParentFile().getParentFile();
        log(lvl, "folder target detected - supposing Maven project:\n%s", fSxProject);
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

  static void globalGetMonitors() {
    if (!isHeadless()) {
      genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
      gdevs = genv.getScreenDevices();
      nMonitors = gdevs.length;
      if (nMonitors == 0) {
        terminate(1, "GraphicsEnvironment has no ScreenDevices");
      }
      monitorBounds = new com.sikulix.core.Region[nMonitors];
      rAllMonitors = null;
      Region currentBounds;
      for (int i = 0; i < nMonitors; i++) {
        currentBounds = new com.sikulix.core.Region(gdevs[i].getDefaultConfiguration().getBounds());
        if (null != rAllMonitors) {
          rAllMonitors = rAllMonitors.union(currentBounds);
        } else {
          rAllMonitors = currentBounds;
        }
        if (currentBounds.contains(new Location())) {
          if (mainMonitor < 0) {
            mainMonitor = i;
            log(lvl, "ScreenDevice %d has (0,0) --- will be primary Screen(0)", i);
          } else {
            log(lvl, "ScreenDevice %d too contains (0,0)!", i);
          }
        }
        log(lvl, "Monitor %d: (%d, %d) %d x %d", i,
                currentBounds.x, currentBounds.y, currentBounds.w, currentBounds.h);
        monitorBounds[i] = currentBounds;
      }
      if (mainMonitor < 0) {
        log(lvl, "No ScreenDevice has (0,0) --- using 0 as primary: %s", monitorBounds[0]);
        mainMonitor = 0;
      }
    } else {
      log(-1, "running in headless environment");
    }
  }

  static File fOptions;
  static Properties options = null;
  static String fnOptions = "SXOptions.txt";
  static boolean testing;

  static void loadOptionsSX() {
    for (String sFile : new String[]{getUSERWORK(), getUSERHOME(), getSXSTORE()}) {
      File aFile = getFile(sFile);
      log(lvl + 1, "loadOptions: check: %s", aFile);
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
        log(-1, "while checking Options file:\n%s", fOptions);
        fOptions = null;
        options = null;
      }
      testing = isOption("testing", false);
      if (testing) {
        //TODO Global Log Level
        log.logOnGlobal(3);
      }
      log(lvl, "found Options file at: %s", fOptions);
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
            log(-1, "loadOptions: not possible: %s = %s", sKey, options.getProperty(sKey));
          }
        }
      }
    }
  }

  static void exportNativeLibraries() {
    if (areLibsExported) {
      return;
    }
    File fSXNative = getFile(getSXNATIVE());
    if (!new File(fSXNative, sxLibsCheckName).exists()) {
      log(lvl, "exportNativeLibraries: folder empty or has wrong content");
      ContentManager.deleteFileOrFolder(fSXNative);
    }
    if (fSXNative.exists()) {
      log(lvl, "exportNativeLibraries: folder exists: %s", fSXNative);
    } else {
      fSXNative.mkdirs();
      if (!fSXNative.exists()) {
        terminate(1, "exportNativeLibraries: folder not available: %s", fSXNative);
      }
      log(lvl, "exportNativeLibraries: new folder: %s", fSXNative);
      URL uLibsFrom = null;
      uLibsFrom = sxGlobalClassReference.getResource(fpJarLibs);
      if (testing || uLibsFrom == null) {
        dumpClassPath("sikulix");
      }
      if (uLibsFrom == null) {
        terminate(1, "exportNativeLibraries: libs not on classpath: " + fpJarLibs);
      }
      log(lvl, "exportNativeLibraries: from: %s", uLibsFrom);
      extractResourcesToFolder(fpJarLibs, fSXNative, null);
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
      libsLoaded.put(aFile, false);
    }
    loadNativeLibrary(sfLibOpencvJava);
    if (isWindows()) {
      addToWindowsSystemPath(fSXNative);
      if (!checkJavaUsrPath(fSXNative)) {
        log(-1, "exportNativeLibraries: JavaUserPath: see errors - might not work and crash later");
      }
      String lib = "jawt.dll";
      File fJawtDll = new File(fSXNative, lib);
      ContentManager.deleteFileOrFolder(fJawtDll);
      ContentManager.xcopy(new File(getJHOME() + "/bin/" + lib), fJawtDll);
      if (!fJawtDll.exists()) {
        terminate(1, "exportNativeLibraries: problem copying %s", fJawtDll);
      }
      loadNativeLibrary(sfLibJIntellitype);
      loadNativeLibrary(sfLibWinUtil);
    } else if (isMac()) {
      loadNativeLibrary(sfLibMacUtil);
      loadNativeLibrary(sfLibMacHotkey);
    } else if (isLinux()) {
      loadNativeLibrary(sfLibJXGrabKey);
    }
    areLibsExported = true;
  }

  static void loadNativeLibrary(String aLib) {
    try {
      if (aLib.startsWith("_ext_")) {
        terminate(1, "loadNativeLibrary: loading external library not implemented: %s", aLib);
      } else {
        String sf_aLib = new File(getSXNATIVE(), aLib).getAbsolutePath();
        System.load(sf_aLib);
        log(lvl, "loadNativeLibrary: bundled: %s", aLib);
      }
    } catch (UnsatisfiedLinkError ex) {
      terminate(1, "loadNativeLibrary: loading library error: %s (%s)", aLib, ex.getMessage());
    }
  }


  // ******************************* System/Java ****************************

  static enum theSystem {
    WIN, MAC, LUX, FOO
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
      } else if (osName.toLowerCase().startsWith("mac")) {
        SXSYS = theSystem.MAC;
        osName = "Mac OSX";
      } else if (osName.toLowerCase().startsWith("linux")) {
        SXSYS = theSystem.LUX;
        osName = "Linux";
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
      terminate(1, "Value not set: JHOME");
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
          ContentManager.deleteFileOrFolder(new File(getSYSTEMP(), aFile));
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
    File fDir = getFile(oDir, null);
    if (!isUnset(fDir)) {
      fDir.mkdirs();
    }
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
    File fDir = getFile(oDir, null);
    if (!isUnset(fDir)) {
      fDir.mkdirs();
    }
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


  public static File getFile(String fpPath) {
    return getFile(fpPath, null);
  }

  public static File getFile(Object oPath, Object oSub) {
    String sPath = oPath instanceof File ? ((File) oPath).getAbsolutePath() : "";
    if (isUnset(sPath)) {
      sPath = oPath instanceof String ? (String) oPath : "";
    }
    if (isUnset(oSub)) {
      return new File(sPath);
    }
    String sSub = oPath instanceof String ? (String) oSub : "";
    return new File(sPath, sSub);
  }

  static String[] theExtensions = new String[]{"selenium4sikulix"};

  public static boolean existsFile(File fPath) {
    if (isUnset(fPath.getName())) {
      return false;
    }
    return fPath.exists();
  }

  public static boolean isNull(Object obj) {
    return null == obj;
  }

  public static boolean isUnset(Object obj) {
    if (obj instanceof String && ((String) obj).isEmpty()) return true;
    return null == obj;
  }

  public static File asExtension(String fpJar) {
    File fJarFound = new File(ContentManager.normalizeAbsolute(fpJar, false));
    if (!fJarFound.exists()) {
      String fpCPEntry = isOnClasspath(fJarFound.getName());
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
        String sxlocalrepo = ContentManager.slashify(prop.getProperty("sxlocalrepo"), true);
        String sxJythonVersion = prop.getProperty("sxjython");
        String sxJRubyVersion = prop.getProperty("sxjruby");

        log(lvl, "version: %s build: %s", sxVersion, sxBuild);
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

      sxLibsCheckName = String.format(sfLibsCheckFileLoaded, sxStamp);
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


  public static String getVersion() {
    return getSXVERSION();
  }

  public static File fSxBaseJar;
  public static File fSxBase;
  public static File fSxProject;
  public static boolean runningInProject = false;
  public static String fpContent;

  public static String sxJythonMaven;
  public static String sxJython;

  public static String sxJRubyMaven;
  public static String sxJRuby;

  protected static Map<String, String> tessData = new HashMap<String, String>();

  protected static String dlMavenRelease = "https://repo1.maven.org/maven2/";
  protected static String dlMavenSnapshot = "https://oss.sonatype.org/content/groups/public/";

  static void initSXVersionInfo() {
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
        log(lvl, "addToWindowsSystemPath: added: %s", libsPath);
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
      log(-1, "checkJavaUsrPath: get (%s)", ex);
    } catch (SecurityException ex) {
      log(-1, "checkJavaUsrPath: get (%s)", ex);
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
          log(lvl, "checkJavaUsrPath: added to ClassLoader.usrPaths");
          contained = true;
        }
      } catch (IllegalAccessException ex) {
        log(-1, "checkJavaUsrPath: set (%s)", ex);
      } catch (IllegalArgumentException ex) {
        log(-1, "checkJavaUsrPath: set (%s)", ex);
      }
      return contained;
    }
    return false;
  }

  // ******************************* ClassPath ****************************
  static List<URL> storeClassPath() {
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
  public static void loadOptions(String fpOptions) {
    log(-1, "loadOptions: not yet implemented");
  }

  public static boolean saveOptions(String fpOptions) {
    log(-1, "saveOptions: not yet implemented");
    return false;
  }

  public static boolean saveOptions() {
    log(-1, "saveOptions: not yet implemented");
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
    dumpClassPath("sikulix");
    if (isJythonReady) {
      JythonHelper.get().showSysPath();
    }
    p("***** show environment end");
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

  public static com.sikulix.core.Region getMonitor(int n) {
    if (isHeadless()) {
      return new com.sikulix.core.Region();
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
      log(-1, "not possible to get mouse position (PointerInfo == null)");
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
          log(lvl + 1, "extractResourceToFile done: %s", eFile);
          count++;
        } else {
          ecount++;
        }
      }
    }
    if (ecount > 0) {
      log(lvl, "files exported: %d - skipped: %d from %s to:\n%s", count, ecount, fpRessources, fFolder);
    } else {
      log(lvl, "files exported: %d from: %s to:\n%s", count, fpRessources, fFolder);
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
  public List<String> extractResourcesToFolderFromJar(String aJar, String fpRessources, File fFolder, FilenameFilter filter) {
    List<String> content = new ArrayList<String>();
    File faJar = new File(aJar);
    URL uaJar = null;
    fpRessources = ContentManager.slashify(fpRessources, false);
    if (faJar.isAbsolute()) {
      if (!faJar.exists()) {
        log(-1, "extractResourcesToFolderFromJar: does not exist: %s", faJar);
        return null;
      }
      try {
        uaJar = new URL("jar", null, "file:" + aJar);
        logp("%s", uaJar);
      } catch (MalformedURLException ex) {
        log(-1, "extractResourcesToFolderFromJar: bad URL for: %s", faJar);
        return null;
      }
    } else {
      uaJar = fromClasspath(aJar);
      if (uaJar == null) {
        log(-1, "extractResourcesToFolderFromJar: not on classpath: %s", aJar);
        return null;
      }
      try {
        String sJar = "file:" + uaJar.getPath() + "!/";
        uaJar = new URL("jar", null, sJar);
      } catch (MalformedURLException ex) {
        log(-1, "extractResourcesToFolderFromJar: bad URL for: %s", uaJar);
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
      log(-1, "extractResourceToFile: %s (%s)", content, ex);
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
      log(-1, "extractResourceToString error: %s from: %s (%s)", encoding, content, ex);
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
    log(lvl, "resourceLocation: (%s) %s", sxGlobalClassReference, folderOrFile);
    if (!folderOrFile.startsWith("/")) {
      folderOrFile = "/" + folderOrFile;
    }
    return sxGlobalClassReference.getResource(folderOrFile);
  }

  private static List<String> resourceList(String folder, FilenameFilter filter) {
    log(lvl, "resourceList: enter");
    List<String> files = new ArrayList<String>();
    if (!folder.startsWith("/")) {
      folder = "/" + folder;
    }
    URL uFolder = resourceLocation(folder);
    if (uFolder == null) {
      log(lvl, "resourceList: not found: %s", folder);
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
      log(lvl, "resourceList: having folder:\n%s", fFolder);
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
        log(lvl, "resourceList:\n%s", folder);
        log(-1, "resourceList: URL neither folder nor jar:\n%s", ex);
        return null;
      }
    }
    String[] parts = uFolder.getPath().split("!");
    if (parts.length < 2 || !parts[0].startsWith("file:")) {
      log(lvl, "resourceList:\n%s", folder);
      log(-1, "resourceList: not a valid jar URL:\n" + uFolder.getPath());
      return null;
    }
    String fpFolder = parts[1];
    log(lvl, "resourceList: having jar:\n%s", uFolder);
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
  public String[] resourceListAsFile(String folder, File target, FilenameFilter filter) {
    String content = resourceListAsString(folder, filter);
    if (content == null) {
      log(-1, "resourceListAsFile: did not work: %s", folder);
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
        log(-1, "resourceListAsFile: %s:\n%s", target, ex);
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
  public String[] resourceListAsSXContent(String folder, File targetFolder, FilenameFilter filter) {
    List<String> contentList = resourceList(folder, filter);
    if (contentList == null) {
      log(-1, "resourceListAsSikulixContent: did not work: %s", folder);
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
      log(-1, "resourceListAsFile: %s:\n%s", target, ex);
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
  public String[] resourceListAsSXContentFromJar(String aJar, String folder, File targetFolder, FilenameFilter filter) {
    List<String> contentList = extractResourcesToFolderFromJar(aJar, folder, null, filter);
    if (contentList == null || contentList.size() == 0) {
      log(-1, "resourceListAsSikulixContentFromJar: did not work: %s", folder);
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
      log(-1, "resourceListAsFile: %s:\n%s", target, ex);
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
  public String resourceListAsString(String folder, FilenameFilter filter) {
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
  public String resourceListAsString(String folder, FilenameFilter filter, String separator) {
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
        log(localLevel, "scanning folder:\n%s", fFolder);
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
          log(localLevel, "from %s adding: %s", (subFolder.isEmpty() ? "." : subFolder), entry);
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
    log(localLevel, "scanning jar:\n%s", uJar);
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
          log(localLevel, "from %s adding: %s", (zeSub.isEmpty() ? "." : zeSub), zefName);
        }
      }
    } catch (Exception ex) {
      log(-1, "doResourceListJar: %s", ex);
      return files;
    }
    return files;
  }

  private boolean copyFromJarToFolderWithList(URL uJar, String fpRessource, List<String> files, File fFolder) {
    int localLevel = testing ? lvl : lvl + 1;
    if (files == null || files.isEmpty()) {
      log(lvl, "copyFromJarToFolderWithList: list of files is empty");
      return false;
    }
    String fpJar = uJar.getPath().split("!")[0];
    if (!fpJar.endsWith(".jar")) {
      return false;
    }
    log(localLevel, "scanning jar:\n%s", uJar);
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
            log(-1, "extractResourcesToFolderFromJar: only ressource folders allowed - use filter");
            return false;
          }
          log(localLevel, "copying: %s", zPath);
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
      log(-1, "doResourceListJar: %s", ex);
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

  public class oneFileFilter implements FilenameFilter {

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

  static ArrayList<URL> imagePath = new ArrayList<URL>();

  private static void initImagePath() {
    if (imagePath.isEmpty()) {
      imagePath.add(ContentManager.makeURL(getSXIMAGES()));
    }
  }

  public static boolean setBundlePath() {
    initImagePath();
    imagePath.set(0, ContentManager.makeURL(getSXIMAGES()));
    return true;
  }

  public static boolean setBundlePath(String fpPath) {
    initImagePath();
    URL urlPath = ContentManager.makeURL(fpPath);
    if (!isUnset(urlPath)) {
      imagePath.set(0, urlPath);
      return true;
    }
    return false;
  }

  public static String getBundlePath() {
    initImagePath();
    return imagePath.get(0).getPath();
  }

  public static void addImagePath(String fpMain) {
    addImagePath(fpMain, null);
  }

  public static void addImagePath(String fpMain, String fpSub) {
    log(-1, "//TODO addImagePath: not implemented");
  }

  public static boolean addClassPath(String jarOrFolder) {
    URL uJarOrFolder = ContentManager.makeURL(jarOrFolder);
    if (!new File(jarOrFolder).exists()) {
      log(-1, "addToClasspath: does not exist - not added:\n%s", jarOrFolder);
      return false;
    }
    if (isOnClasspath(uJarOrFolder)) {
      return true;
    }
    log(lvl, "addToClasspath:\n%s", uJarOrFolder);
    Method method;
    URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
    Class sysclass = URLClassLoader.class;
    try {
      method = sysclass.getDeclaredMethod("addURL", new Class[]{URL.class});
      method.setAccessible(true);
      method.invoke(sysLoader, new Object[]{uJarOrFolder});
    } catch (Exception ex) {
      log(-1, "Did not work: %s", ex.getMessage());
      return false;
    }
    storeClassPath();
    return true;
  }

  // ******************************* Commands which system ****************************

  /**
   * @return path seperator : or ;
   */
  public String getSeparator() {
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
  public static String NL = "\n";

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
   *             the command, the rest are parameters and their values
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
  public void cleanUp(int n) {
    log(lvl, "cleanUp: %d", n);
//    ScreenHighlighter.closeAll();
//    Observer.cleanUp();
//    Mouse.reset();
//    Screen.getPrimaryScreen().getRobot().keyUp();
//    HotkeyManager.reset();
  }

}
