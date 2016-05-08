/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package org.sikuli.core;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.security.CodeSource;
import java.util.Date;
import java.util.Properties;

class SXGlobal extends SX {

  private SXGlobal() {
  }

  protected static void getInstance() {
    if (sxGlobal == null) {
      sxGlobal = new SXGlobal();
      sxGlobal.setLogger("Global");
      sxGlobal.doGlobalInit();
    }
  }

  private boolean doGlobalInit() {
    log(lvl, "doGlobalInit: starting");
    sxGlobalClassReference = sxGlobal.getClass();
    String tmpdir = System.getProperty("java.io.tmpdir");
    if (tmpdir != null && !tmpdir.isEmpty()) {
      fTempPath = new File(tmpdir);
    } else {
      terminate(1, "init: java.io.tmpdir not valid (null or empty");
    }
    fSXTempPath = new File(fTempPath,
            String.format("Sikulix_%d", ContentManager.getRandomInt()));
    fpSXTempPath = fSXTempPath.getAbsolutePath();
    fSXTempPath.mkdirs();

    String aFolder = System.getProperty("user.home");
    if (aFolder == null || aFolder.isEmpty() || !(fUserHome = new File(aFolder)).exists()) {
      terminate(-1, "JavaSystemProperty::user.home not valid");
    }

    aFolder = System.getProperty("user.dir");
    if (aFolder == null || aFolder.isEmpty() || !(fWorkDir = new File(aFolder)).exists()) {
      terminate(-1, "JavaSystemProperty::user.dir not valid");
    }

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        log(lvl, "final cleanup");
        if (isRunning != null) {
          try {
            isRunningFile.close();
          } catch (IOException ex) {
          }
          isRunning.delete();
        }
        for (File f : fTempPath.listFiles(new FilenameFilter() {
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
                if (isObsolete || aFile.equals(fSXTempPath)) {
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

    if (isIDE() && !runningScripts) {
      isRunning = new File(fTempPath, isRunningFilename);
      boolean shouldTerminate = false;
      try {
        isRunning.createNewFile();
        isRunningFile = new FileOutputStream(isRunning);
        if (null == isRunningFile.getChannel().tryLock()) {
          org.sikuli.script.Commands.popError("Terminating: IDE already running");
          shouldTerminate = true;
        }
      } catch (Exception ex) {
        org.sikuli.script.Commands.popError("Terminating: FatalError: cannot access IDE lock for/n" + isRunning);
        shouldTerminate = true;
      }
      if (shouldTerminate) {
        System.exit(1);
      }
    }

    for (String aFile : fTempPath.list()) {
      if ((aFile.startsWith("Sikulix") && (new File(aFile).isFile()))
              || (aFile.startsWith("jffi") && aFile.endsWith(".tmp"))) {
        ContentManager.deleteFileOrFolder(new File(fTempPath, aFile));
      }
    }

    globalGetSystem();
    globalGetSupport();
    loadOptionsSX();
    initSXVersionInfo();
    globalGetMonitors();
    extractResourcesToFolder("Lib", fSXLib, null);
    exportNativeLibraries();

    return true;
  }

  private void globalGetSystem() {
    String vJava = System.getProperty("java.runtime.version");
    String vVM = System.getProperty("java.vm.version");
    String vClass = System.getProperty("java.class.version");
    String vSysArch = System.getProperty("os.arch");
    if (vSysArch == null || !vSysArch.contains("64")) {
      terminate(1, "Java arch not 64-Bit or not detected: os.arch = %s", vSysArch);
    }
    try {
      javaVersion = Integer.parseInt(vJava.substring(2, 3));
      javaShow = String.format("java %d-%d version %s vm %s class %s arch %s",
              javaVersion, javaArch, vJava, vVM, vClass, vSysArch);
    } catch (Exception ex) {
      javaShow = String.format("java ???-%d version %s vm %s class %s arch %s",
              javaArch, vJava, vVM, vClass, vSysArch);
      logp(javaShow);
      dumpSysProps();
      terminate(1, "Java version not detected: java.runtime.version = %s", vJava);
    }

    osVersion = osVersionSysProp;
    String os = osNameSysProp.toLowerCase();
    if (os.startsWith("windows")) {
      runningOn = theSystem.WIN;
      sysName = "windows";
      osName = "Windows";
      runningWindows = true;
      fpJarLibs = "/Native/windows";
      NL = "\r\n";
    } else if (os.startsWith("mac")) {
      runningOn = theSystem.MAC;
      sysName = "mac";
      osName = "Mac OSX";
      runningMac = true;
      fpJarLibs = "/Native/mac";
    } else if (os.startsWith("linux")) {
      runningOn = theSystem.LUX;
      sysName = "linux";
      osName = "Linux";
      runningLinux = true;
      fpJarLibs = "/Native/linux";
      String result = "*** error ***";
//    result = runcmd("lsb_release -i -r -s");
//TODO apache commons exec
      if (result.contains("*** error ***")) {
        log(-1, "command returns error: lsb_release -i -r -s\n%s", result);
      } else {
        linuxDistro = result.replaceAll("\n", " ").trim();
      }
    } else {
      terminate(-1, "running on not supported System: %s (%s)", os, osVersion);
    }
    osShow = String.format("%s (%s)", osName, osVersion);
    log(lvl, "running on: %s", osShow);
    log(lvl, "running: %s", javaShow);
  }

  private void globalGetSupport() {
    if (runningWindows) {
      appDataMsg = "init: Windows: %APPDATA% not valid (null or empty) or is not accessible:\n%s";
      String tmpdir = System.getenv("APPDATA");
      if (tmpdir != null && !tmpdir.isEmpty()) {
        fSysAppPath = new File(tmpdir);
        fSXAppPath = new File(fSysAppPath, "Sikulix");
      }
    } else if (runningMac) {
      appDataMsg = "init: Mac: SikulxAppData does not exist or is not accessible:\n%s";
      fSysAppPath = new File(fUserHome, "Library/Application Support");
      fSXAppPath = new File(fSysAppPath, "Sikulix");
    } else if (runningLinux) {
      fSysAppPath = fUserHome;
      fSXAppPath = new File(fSysAppPath, ".Sikulix");
      appDataMsg = "init: Linux: SikulxAppData does not exist or is not accessible:\n%s";
    }
    fSXAppPath = new File(fSXAppPath, "SX2");
    fSXStore = new File(fSXAppPath, "Store");
    fSXStore.mkdir();
    fSXDownloads = new File(fSXAppPath, "Downloads");
    fSXDownloads.mkdir();
    fSXEditor = new File(fSXAppPath, "Editor");
    fSXEditor.mkdir();
    fSXExtensions = new File(fSXAppPath, "Extensions");
    fSXExtensions.mkdir();
    fSXLib = new File(fSXAppPath, "Lib");
    fSXLib.mkdir();
    fSXNative = new File(fSXAppPath, "Native");
    fSXTesseract = new File(fSXAppPath, "Tesseract");
    fSXTesseract.mkdir();
    if (!(fSXEditor.exists() && fSXExtensions.exists() && fSXDownloads.exists()
            && fSXLib.exists() && fSXTesseract.exists() && fSXStore.exists())) {
      terminate(1, "AppDataPath: not completely available: %s", fSXAppPath);
    }
    log(lvl, "AppDataPath: %s", fSXAppPath);

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
        if (runningWindows) {
          if (jn.endsWith(".exe")) {
            runningWinApp = true;
            runningJar = false;
            appType = "as application .exe";
          }
        } else if (runningMac) {
          if (fSxBase.getAbsolutePath().contains("SikuliX.app/Content")) {
            runningMacApp = true;
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

  private void globalGetMonitors() {
    if (!isHeadless()) {
      genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
      gdevs = genv.getScreenDevices();
      nMonitors = gdevs.length;
      if (nMonitors == 0) {
        terminate(1, "GraphicsEnvironment has no ScreenDevices");
      }
      monitorBounds = new Region[nMonitors];
      rAllMonitors = null;
      Region currentBounds;
      for (int i = 0; i < nMonitors; i++) {
        currentBounds = new Region(gdevs[i].getDefaultConfiguration().getBounds());
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

  private void loadOptionsSX() {
    for (File aFile : new File[]{fWorkDir, fUserHome, fSXStore}) {
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
        logOn(3);
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

  private void initSXVersionInfo() {
    Properties prop = new Properties();
    String sVersionFile = "sikulixversion.txt";
    log(lvl, "initSXVersionInfo: reading from %s", sVersionFile);
    try {
      InputStream is;
      is = sxGlobalClassReference.getClassLoader().getResourceAsStream("Settings/" + sVersionFile);
      if (is == null) {
        terminate(1, "initSXVersionInfo: not found on classpath: %s", "Settings/" + sVersionFile);
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
      String SikuliJRubyVersion = prop.getProperty("sxjruby");

      log(lvl, "version: %s build: %s", sxVersion, sxBuild);
      sxVersionShow = String.format("%s (%s)", sxVersion, sxBuild);
      sxStamp = String.format("%s_%s", sxVersion, sxBuild);

      // used for download of production versions
      String dlProdLink = "https://launchpad.net/raiman/sikulix2013+/";
      String dlProdLinkSuffix = "/+download/";
      // used for download of development versions (nightly builds)
      String dlDevLink = "http://nightly.sikuli.de/";

      String osn = "UnKnown";
      String os = System.getProperty("os.name").toLowerCase();
      if (os.startsWith("mac")) {
        osn = "Mac";
      } else if (os.startsWith("windows")) {
        osn = "Windows";
      } else if (os.startsWith("linux")) {
        osn = "Linux";
      }

      sxJythonMaven = "org/python/jython-standalone/"
              + sxJythonVersion + "/jython-standalone-" + sxJythonVersion + ".jar";
      sxJython = sxlocalrepo + sxJythonMaven;
      sxJRubyMaven = "org/jruby/jruby-complete/"
              + SikuliJRubyVersion + "/jruby-complete-" + SikuliJRubyVersion + ".jar";
      sxJRuby = sxlocalrepo + sxJRubyMaven;
    } catch (Exception e) {
      terminate(1, "initSXVersionInfo: load failed for %s error(%s)", sVersionFile, e.getMessage());
    }
    tessData.put("eng", "http://tesseract-ocr.googlecode.com/files/tesseract-ocr-3.02.eng.tar.gz");

    sxLibsCheckName = String.format(sfLibsCheckFileLoaded, sxStamp);
  }

  void exportNativeLibraries() {
    if (areLibsExported) {
      return;
    }
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
      ContentManager.xcopy(new File(javahome + "/bin/" + lib), fJawtDll);
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

  void loadNativeLibrary(String aLib) {
    try {
      if (aLib.startsWith("_ext_")) {
        terminate(1, "loadNativeLibrary: loading external library not implemented: %s", aLib);
      } else {
        String sf_aLib = new File(fSXNative, aLib).getAbsolutePath();
        System.load(sf_aLib);
        log(lvl, "loadNativeLibrary: bundled: %s", aLib);
      }
    } catch (UnsatisfiedLinkError ex) {
      terminate(1, "loadNativeLibrary: loading library error: %s (%s)", aLib, ex.getMessage());
    }
  }
}
