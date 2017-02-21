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
import java.io.FileReader;

public class Runner {

  static SXLog log = SX.getLogger("SX.Runner");

  public enum ScriptType {JAVASCRIPT, PYTHON, RUBY}

  public static Object run(ScriptType type, Object... args) {
    if (ScriptType.JAVASCRIPT.equals(type)) {
      log.trace("%s: starting run", ScriptType.JAVASCRIPT);
      RunBox runBox = new RunBox();
      new Thread(runBox).start();
      runBox.running = true;
      while (runBox.running) {
        SX.pause(1);
      }
      return null;
    }
    log.terminate(1, "run: scripttype %s not implemented", type);
    return null;
  }

  private static class RunBox implements Runnable {

    ScriptType type = ScriptType.JAVASCRIPT;
    Object[] args = new Object[0];
    boolean running = false;

    public RunBox() {
      //log.on(SXLog.TRACE);
    }

    @Override
    public void run() {
      if (ScriptType.JAVASCRIPT.equals(type)) {
        runJS(args);
      }
      running = false;
    }

    private void runJS(Object... args) {
      ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
      String script = "var Do = Java.type('com.sikulix.api.Do');";
      if (args.length == 0) {
        script += "var element = Do.hover(); " +
                "print('Hello from JavaScript: mouse at: ' + element);";
      }
      try {
        engine.eval(script);
      } catch (ScriptException e) {
        log.trace("%s: error: %s", ScriptType.JAVASCRIPT, e.getMessage());
      }
      log.trace("%s: ending run", ScriptType.JAVASCRIPT);
    }
  }

  }
