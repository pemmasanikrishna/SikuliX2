/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */
package com.sikulix.scripting;

import com.sikulix.core.*;
import org.sikuli.script.Commands;

import java.io.File;

public class Run extends SX {

  static SXLog log = null;

  public static void main(String[] args) {
    log = getLogger("Sikulix", args);
    log.on(1);
    log.trace("Sikulix starting");

    terminate(1, "stopped intentionally");

    String[] userArgs = getUserArgs();

//    log.info("a new Screen: %s", new Screen());
//    log.info("a new Region/Rectangle: %s", new Region());
//    log.info("a new Location/Point: %s", new Location());
//    log.info("a new Image: %s", new Image());
//    log.info("a new Pattern: %s", new Pattern());
//    log.info("a new Match: %s", new Match());
//    log.info("a new Window: %s", new Window());

//    log.info("Match: %s", new Match().setTarget(10,10));
    Region reg = new Region(500, 100, 300, 600);

    Image img = reg.capture();
    log.info("%s", img);
    img.show(1);

    terminate(1, "stopped intentionally");

    terminate(1, "stopped intentionally");

    File lastSession = new File(getSXSTORE(), "LastAPIJavaScript.js");
    String runSomeJS = "";
    if (lastSession.exists()) {
      runSomeJS = Content.readFileToString(lastSession);
    }
    runSomeJS = Commands.inputText("enter some JavaScript (know what you do - may silently die ;-)"
            + "\nexample: run(\"git*\") will run the JavaScript showcase from GitHub"
            + "\nWhat you enter now will be shown the next time.",
            "API::JavaScriptRunner ", 10, 60, runSomeJS);
    if (runSomeJS.isEmpty()) {
      Commands.popup("Nothing to do!");
    } else {
      while (!runSomeJS.isEmpty()) {
        Content.writeStringToFile(runSomeJS, lastSession);
        Runner.runjs(null, null, runSomeJS, null);
        runSomeJS = Commands.inputText("Edit the JavaScript and/or press OK to run it (again)\n"
                + "Press Cancel to terminate",
                "API::JavaScriptRunner ", 10, 60, runSomeJS);
      }
    }

    log.trace("Sikulix ending");
    System.exit(0);
  }
}
