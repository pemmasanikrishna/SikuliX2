/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package org.sikuli.core;

import org.sikuli.script.*;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Date;

public class SXGlobal extends SX {

  private static SXGlobal instance = null;

  private SXGlobal() {
  }

  protected static SXGlobal getInstance() {
    if (instance == null) {
      instance = new SXGlobal();
      instance.setLogger("Global");
      instance.doGlobalInit();
    }
    return instance;
  }

  private boolean doGlobalInit() {
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
    globalGetMonitors();

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
      NL = "\r\n";
    } else if (os.startsWith("mac")) {
      runningOn = theSystem.MAC;
      sysName = "mac";
      osName = "Mac OSX";
      runningMac = true;
    } else if (os.startsWith("linux")) {
      runningOn = theSystem.LUX;
      sysName = "linux";
      osName = "Linux";
      runningLinux = true;
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
    fSXNative.mkdir();
    fSXTesseract = new File(fSXAppPath, "Tesseract");
    fSXTesseract.mkdir();
    if (!(fSXEditor.exists() && fSXExtensions.exists() && fSXDownloads.exists() && fSXNative.exists()
            && fSXLib.exists() && fSXNative.exists() && fSXTesseract.exists() && fSXStore.exists())) {
      terminate(1, "SikuliX AppDataPath: should be checked - not completely available\n%s", fSXAppPath);
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

}
