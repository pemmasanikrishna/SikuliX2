/*
 * Copyright 2015-2016, SikulixUtil.com
 * Released under the MIT License.
 */
package org.sikuli.script;

import java.io.File;
import java.util.Date;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sikuli.util.Debug;
import org.sikuli.util.FileManager;
import org.sikuli.util.Settings;

public class Sikulix {

  private static RunTime rt;

//<editor-fold defaultstate="collapsed" desc="logging">
  private static final int lvl = 3;
  private static final Logger logger = LogManager.getLogger("SX.Main");

  private static void log(int level, String message, Object... args) {
    if (Debug.is(lvl)) {
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

  private static void logp(String message, Object... args) {
    System.out.println(String.format(message, args));
  }

  private static void terminate(int retval, String message, Object... args) {
    logger.fatal(String.format(" *** terminating: " + message, args));
    System.exit(retval);
  }
  
  private static long started = 0;
  
  private static void start() {
    started = new Date().getTime();
  }
    
  private static long end() {
    return end("");
  }
  
  private static long end(String message) {
    long ended = new Date().getTime();
    long diff = ended - started;
    if (!message.isEmpty()) {
      logp("[time] %s: %d msec", message, diff);
    }
    started = ended;
    return diff;
  }
  
//</editor-fold>

  public static void main(String[] args) throws FindFailed {

    Debug.on(3);

    rt = RunTime.get();

//<editor-fold defaultstate="collapsed" desc="basic find test (arg0 test)">
    if (args != null && args.length > 0 && "test".equals(args[0])) {
      System.out.println("********** Running Sikulix.main");

      Settings.InfoLogs = false;
      Settings.ActionLogs = false;

      ImagePath.add("org.sikuli.script.Sikulix/ImagesAPI.sikuli");
      Screen s = new Screen();

      Image.create("nbicons");

      Settings.CheckLastSeen = false;

      Settings.UseImageFinder = true;
      s.find("nbicons");

      Settings.CheckLastSeen = true;

      Settings.UseImageFinder = true;
      s.find("nbicons");
    }
//</editor-fold>

    new Screen();
    ImagePath.add("org.sikuli.script.Sikulix/ImagesAPI.sikuli");
    Image img = null; 
    
//    start();
//    img = Image.create("sxpower");
//    img.getMatOld();
//    log(lvl, "(%d) BufImage: %s", end(), img);
//    
//    start();
//    img = new Image("sxpower");
//    log(lvl, "(%d) MatImage: %s", end(), img);
    
    start();
    img = new Image(new Screen().capture());
//    String result = FileManager.saveTmpImage(img.get(), "png");
//    img.getMatOld();
    log(lvl, "(%d) ScrBufImage: %s", end(), img);

//    start();
//    img = new Image(new Screen().capture());
    logp(img.getMat().toString());
//    log(lvl, "(%d) ScrBufImage: %s", end(), img);
    
//*****************************************    Commands.endNormal(1);
    System.exit(1);

    if (rt.runningWinApp) {
      Commands.popup("Hello World\nNot much else to do ( yet ;-)", Sikulix.rt.fSxBaseJar.getName());
      try {
        Screen scr = new Screen();
        scr.find(new Image(scr.userCapture("grab something to find"))).highlight(3);
      } catch (Exception ex) {
        Commands.popup("Uuups :-(\n" + ex.getMessage(), Sikulix.rt.fSxBaseJar.getName());
      }
      Commands.popup("Hello World\nNothing else to do ( yet ;-)", Sikulix.rt.fSxBaseJar.getName());
      System.exit(1);
    }

    String version = String.format("(%s-%s)", rt.getVersion(), rt.sxBuildStamp);
    File lastSession = new File(rt.fSikulixStore, "LastAPIJavaScript.js");
    String runSomeJS = "";
    if (lastSession.exists()) {
      runSomeJS = FileManager.readFileToString(lastSession);
    }
    runSomeJS = Commands.inputText("enter some JavaScript (know what you do - may silently die ;-)"
        + "\nexample: run(\"git*\") will run the JavaScript showcase from GitHub"
        + "\nWhat you enter now will be shown the next time.",
        "API::JavaScriptRunner " + version, 10, 60, runSomeJS);
    if (runSomeJS.isEmpty()) {
      Commands.popup("Nothing to do!", version);
    } else {
      while (!runSomeJS.isEmpty()) {
        FileManager.writeStringToFile(runSomeJS, lastSession);
        Runner.runjs(null, null, runSomeJS, null);
        runSomeJS = Commands.inputText("Edit the JavaScript and/or press OK to run it (again)\n"
            + "Press Cancel to terminate",
            "API::JavaScriptRunner " + version, 10, 60, runSomeJS);
      }
    }
    System.exit(0);
  }
}
