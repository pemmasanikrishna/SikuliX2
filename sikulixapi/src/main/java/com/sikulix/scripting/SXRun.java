/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */
package com.sikulix.scripting;

import com.sikulix.api.Commands;
import com.sikulix.core.*;

import java.io.File;

public class SXRun extends Commands {

  static SXLog log = null;
  static {
    log = SX.getLogger("SXRun");
    log.on(SXLog.TRACE);
  }

  public static void main(String[] args) {

    log.trace("Sikulix starting");
    File lastSession = new File(getSXSTORE(), "LastAPIJavaScript.js");
    String runSomeJS = "";
    if (lastSession.exists()) {
      runSomeJS = Content.readFileToString(lastSession);
    }
    runSomeJS = inputText("enter some JavaScript (know what you do - may silently die ;-)"
            + "\nexample: run(\"git*\") will run the JavaScript showcase from GitHub"
            + "\nWhat you enter now will be shown the next time.",
            "API::JavaScriptRunner ", 10, 60, runSomeJS);
    if (runSomeJS.isEmpty()) {
      popup("Nothing to do!");
    } else {
      while (!runSomeJS.isEmpty()) {
        if (runSomeJS.startsWith("!")) {
          String fpScript = null;
          File fScript = null;
          if (runSomeJS.trim().length() == 1) {
            fpScript = Commands.popFile("SXRun::JavaScript");
            runSomeJS = "!" + fpScript;
          } else {
            fpScript = runSomeJS.substring(1);
          }
          fScript = new File(fpScript);
          if (fScript.exists() && fpScript.endsWith(".js")) {
            Content.writeStringToFile(runSomeJS, lastSession);
            SXRunner.runjs(fScript);
          } else {
            log.error("not a valid JavaScript file");
          }
        } else {
          SXRunner.runjs(runSomeJS);
        }
        runSomeJS = inputText("Edit the JavaScript and/or press OK to run it (again)\n"
                + "Press Cancel to terminate",
                "API::Scripting::SXRunner(JavaScript) ", 10, 60, runSomeJS);
      }
    }

    log.trace("Sikulix ending");
    System.exit(0);
  }
}
