/*
 * Copyright 2016, sikulix.com
 * Released under the MIT License.
 */
package org.sikuli.script;

import org.sikuli.core.ContentManager;
import org.sikuli.core.SX;

import java.io.File;

public class Sikulix extends SX {

  private Sikulix(String[] args) {
    logOnGlobal(3);
    //logOffError();
    sxinit(args);
    setLogger("Sikulix");
  }

  public static void main(String[] args) {
    new Sikulix(args).run(args);
  }

  private void run(String[] args) {

    logOn(3);
    logp("Sikulix starting");
    show();
    setBundlePath("org.sikuli.script.Sikulix/ImagesAPI");

    addImagePath("");

    terminate(1, "stopped intentionally");
    
    File lastSession = new File(SX.fSXStore, "LastAPIJavaScript.js");
    String runSomeJS = "";
    if (lastSession.exists()) {
      runSomeJS = ContentManager.readFileToString(lastSession);
    }
    runSomeJS = Commands.inputText("enter some JavaScript (know what you do - may silently die ;-)"
            + "\nexample: run(\"git*\") will run the JavaScript showcase from GitHub"
            + "\nWhat you enter now will be shown the next time.",
            "API::JavaScriptRunner ", 10, 60, runSomeJS);
    if (runSomeJS.isEmpty()) {
      Commands.popup("Nothing to do!");
    } else {
      while (!runSomeJS.isEmpty()) {
        ContentManager.writeStringToFile(runSomeJS, lastSession);
        Runner.runjs(null, null, runSomeJS, null);
        runSomeJS = Commands.inputText("Edit the JavaScript and/or press OK to run it (again)\n"
                + "Press Cancel to terminate",
                "API::JavaScriptRunner ", 10, 60, runSomeJS);
      }
    }
    System.exit(0);
  }
}
