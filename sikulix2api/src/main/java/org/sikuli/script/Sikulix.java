/*
 * Copyright 2015-2016, SikulixUtil.com
 * Released under the MIT License.
 */
package org.sikuli.script;

import java.io.File;
import java.net.URL;
import java.security.CodeSource;
import org.sikuli.util.Debug;
import org.sikuli.util.FileManager;
import org.sikuli.util.Settings;

public class Sikulix {

  private static RunTime rt;

  private static String imgLink = "http://www.sikulix.com/uploads/1/4/2/8/14281286";
  private static String imgHttp = "1389888228.jpg";
  private static String imgNet = imgLink + "/" + imgHttp;

  private static boolean runningFromJar;
  private static String jarPath;
  private static String jarParentPath;
  private static boolean shouldRunServer = false;

  static {
    CodeSource codeSrc =  Sikulix.class.getProtectionDomain().getCodeSource();

    if (codeSrc != null && codeSrc.getLocation() != null) {
      URL jarURL = codeSrc.getLocation();
      jarPath = FileManager.slashify(new File(jarURL.getPath()).getAbsolutePath(), false);
      jarParentPath = (new File(jarPath)).getParent();
      if (jarPath.endsWith(".jar")) {
        runningFromJar = true;
      } else {
        jarPath += "/";
      }
    }
  }

  public static void main(String[] args) throws FindFailed {

    Debug.on(3);

    rt = RunTime.get();
        
    if (args != null && "test".equals(args[0])) {
      System.out.println("********** Running Sikulix.main");

      Settings.InfoLogs = false;
      Settings.ActionLogs = false;

      ImagePath.add("org.sikuli.script.Sikulix/ImagesAPI.sikuli");
      Screen s = new Screen();

      Image.create("nbicons");

      Settings.CheckLastSeen = false;

      Settings.UseImageFinder = false;
      s.find("nbicons");

      Settings.UseImageFinder = true;
      s.find("nbicons");

      Settings.CheckLastSeen = true;

      Settings.UseImageFinder = false;
      s.find("nbicons");

      Settings.UseImageFinder = true;
      s.find("nbicons");
    }
//    Commands.endNormal(1);
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
  
  public static boolean isRunningFromJar() {
    return runningFromJar;
  }

  public static String getJarPath() {
    return jarPath;
  }

  public static String getJarParentPath() {
    return jarParentPath;
  }

  private static boolean runningSikulixapi = false;

  public static boolean isRunningSikulixapi() {
    return runningSikulixapi;
  }

  public static void setRunningSikulixapi(boolean runningAPI) {
    runningSikulixapi = runningAPI;
  }

  public static Screen init() {
    if (!RunTime.canRun()) {
      return null;
    }
//TODO collect initializations here
    Mouse.init();
    Keys.init();
    return new Screen();
  }






}
