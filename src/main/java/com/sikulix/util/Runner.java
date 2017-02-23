/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.util;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.net.URL;

public class Runner {

  static SXLog log = SX.getLogger("SX.Runner");

  public enum ScriptType {JAVASCRIPT, PYTHON, RUBY, WITHTRACE}

  static URL scriptPath = null;

  public static URL getScriptPath() {
    return scriptPath;
  }

  public static boolean setScriptPath(Object... args) {
    scriptPath = SX.getURL(args);
    return SX.isNotNull(scriptPath);
  }

  public static Object run(Object... args) {
    if (args.length == 0) {
      log.error("run: no args");
      return null;
    }
    RunBox runBox = new RunBox(args);
    if (runBox.isValid()) {
      log.trace("starting run: %s", args[0]);
      new Thread(runBox).start();
      runBox.running = true;
      while (runBox.running) {
        SX.pause(1);
      }
      log.trace("ending run: %s with ", args[0], runBox.getReturnObject());
      return runBox.getReturnObject();
    }
    return null;
  }

  private static class RunBox implements Runnable {

    ScriptType type = null;
    Object[] args = new Object[0];
    boolean running = false;
    boolean valid = false;
    String script = "";
    String scriptName = "";
    URL scriptURL = null;

    public RunBox(Object[] args) {
      this.args = args;
      if (ScriptType.WITHTRACE.equals(args[args.length - 1])) {
        log.on(SXLog.TRACE);
      }
      init();
    }

    private void init() {
      int firstarg = 0;
      if (args[0] instanceof ScriptType) {
        if (args.length > 1) {
          firstarg = 1;
          type = (ScriptType) args[0];

        }
      }
      if (args[firstarg] instanceof String) {
        if (ScriptType.JAVASCRIPT.equals(type)) {
          scriptName = "givenAsText";
          valid = true;
          script = (String) args[firstarg];
        } else if (SX.isNotNull(type)){
          log.error("RunBox.init: %s not implemented", type);
        } else {

        }
      } else if (args[firstarg] instanceof File) {

      } else if (args[firstarg] instanceof URL) {

      } else {
        log.error("RunBox.init: invalid args (arg0: %s)", args[0]);
      }
      if (SX.isNotNull(scriptURL)) {
        //TODO load script
      }
    }

    public boolean isValid() {
      return valid;
    }

    public Object getReturnObject() {
      return null;
    }

    @Override
    public void run() {
      if (ScriptType.JAVASCRIPT.equals(type)) {
        runJS();
      }
      running = false;
    }

    private boolean runJS() {
      ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
      String scriptBefore = "var Do = Java.type('com.sikulix.api.Do');\n" +
              "print('Hello from JavaScript: SikuliX support loaded');\n";
      String scriptText = scriptBefore;
      scriptText += script;
      log.trace("%s: running script %s", ScriptType.JAVASCRIPT, scriptName);
      if (log.isLevel(SXLog.TRACE)) {
        log.p(script);
        log.p("---------- end of script");
      }
      try {
        engine.eval(scriptText);
      } catch (ScriptException e) {
        log.trace("%s: error: %s", ScriptType.JAVASCRIPT, e.getMessage());
        return false;
      }
      log.trace("%s: ending run", ScriptType.JAVASCRIPT);
      return true;
    }
  }
}
