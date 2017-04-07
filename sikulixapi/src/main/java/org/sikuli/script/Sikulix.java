/*
 * Copyright 2016, sikulix.com
 * Released under the MIT License.
 */
package org.sikuli.script;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import org.sikuli.util.FileManager;


import java.io.File;

public class Sikulix extends SX {

  static SXLog log = null;

  public static void main(String[] args) {
    log = getLogger("Sikulix", args);
    log.on(1);
    log.info("Sikulix starting");

    terminate(1, "stopped intentionally");

    File lastSession = new File(getSXSTORE(), "LastAPIJavaScript.js");
    String runSomeJS = "";
    if (lastSession.exists()) {
      runSomeJS = FileManager.readFileToString(lastSession);
    }
    runSomeJS = Commands.inputText("enter some JavaScript (know what you do - may silently die ;-)"
            + "\nexample: run(\"git*\") will run the JavaScript showcase from GitHub"
            + "\nWhat you enter now will be shown the next time.",
            "API::JavaScriptRunner ", 10, 60, runSomeJS);
    if (runSomeJS.isEmpty()) {
      Commands.popup("Nothing to do!");
    } else {
      while (!runSomeJS.isEmpty()) {
        FileManager.writeStringToFile(runSomeJS, lastSession);
        Runner.runjs(null, null, runSomeJS, null);
        runSomeJS = Commands.inputText("Edit the JavaScript and/or press OK to run it (again)\n"
                + "Press Cancel to terminate",
                "API::JavaScriptRunner ", 10, 60, runSomeJS);
      }
    }
    System.exit(0);
  }
}
