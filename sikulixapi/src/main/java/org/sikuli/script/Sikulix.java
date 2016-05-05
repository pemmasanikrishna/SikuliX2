/*
 * Copyright 2016, sikulix.com
 * Released under the MIT License.
 */
package org.sikuli.script;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sikuli.core.ContentManager;
import org.sikuli.core.SX;

import java.io.File;
import java.util.Date;

public class Sikulix extends SX {

  static {
    new Sikulix();
  }

  private Sikulix() {
    setLogger("Sikulix");
  }

  public static void main(String[] args) throws FindFailed {

    ImagePath.setBundlePath("org.sikuli.script.Sikulix/ImagesAPI");

    addImagePath("");
    
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
