/*
 * Copyright 2016, sikulix.com
 * Released under the MIT License.
 */
package org.sikuli.script;

import com.sikulix.core.*;
import com.sikulix.core.Image;
import com.sikulix.core.Location;
import com.sikulix.core.Match;
import com.sikulix.core.Pattern;
import com.sikulix.core.Region;
import com.sikulix.core.Screen;

import java.io.File;

public class Sikulix extends SX {

  static SXLog log = null;

  public static void main(String[] args) {
//    logOn(1);
    log = getLogger("Sikulix", args);
    String[] userArgs = getUserArgs();
    p("*** User Args ***");
    for (String arg : userArgs) {
      p("%s", arg);
    }
    p("*** END User Args ***");

    log.info("Sikulix starting");

//    show();
//    logp("before: %s", getBundlePath());
//    setBundlePath("org.sikuli.script.Sikulix/ImagesAPI");
//    logp("after: %s", getBundlePath());

//    log.info("a new Screen: %s", new Screen());
//    log.info("a new Region/Rectangle: %s", new Region());
//    log.info("a new Location/Point: %s", new Location());
//    log.info("a new Image: %s", new Image());
//    log.info("a new Pattern: %s", new Pattern());
//    log.info("a new Match: %s", new Match());
//    log.info("a new Window: %s", new Window());

    log.info("Match: %s", new Match().setTarget(10,10));

    terminate(1, "stopped intentionally");

    addImagePath("");

    terminate(1, "stopped intentionally");

    File lastSession = new File(getSXSTORE(), "LastAPIJavaScript.js");
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
