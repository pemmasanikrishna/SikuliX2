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
import java.net.URL;

public class Runner {

  static SXLog log = SX.getLogger("SX.Runner");

  public enum ScriptType {JAVASCRIPT, PYTHON, RUBY, WITHTRACE}

  public static Object run(ScriptType type, Object... args) {
    if (ScriptType.JAVASCRIPT.equals(type)) {
      log.trace("%s: starting run", ScriptType.JAVASCRIPT);
      RunBox runBox = new RunBox(args);
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

    public RunBox(Object[] args) {
      this.args = args;
      if (ScriptType.WITHTRACE.equals(args[args.length-1])) {
        log.on(SXLog.TRACE);
      }
    }

    @Override
    public void run() {
      if (ScriptType.JAVASCRIPT.equals(type)) {
        runJS(args);
      }
      running = false;
    }

    private boolean runJS(Object... args) {
      ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
      String scriptBefore = "var Do = Java.type('com.sikulix.api.Do');\n" +
                            "print('Hello from JavaScript: SikuliX support loaded');\n";
      String script = "";
      if (args.length == 0) {
        script = "print('Hello from JavaScript: nothing to do');";
      } else if (args.length > 0) {
        if (args[0] instanceof String) {
          String scriptText = (String) args[0];
          if (scriptText.contains(";")) {
            script += scriptBefore + scriptText;
            log.trace("%s: running script given as text", ScriptType.JAVASCRIPT);
            if (log.isLevel(SXLog.TRACE)) {
              log.p(scriptText);
              log.p("---------- end of script");
            }
          } else {
            log.error("%s: not a valid script - might be filename (not implemented)", ScriptType.JAVASCRIPT);
          }
        } else if (args[0] instanceof File) {
          log.error("%s: script as %s (not implemented)", "File", ScriptType.JAVASCRIPT);
        } else if (args[0] instanceof URL) {
          log.error("%s: script as %s (not implemented)", "URL", ScriptType.JAVASCRIPT);
        }
      }
      if (args.length > 1 && !ScriptType.WITHTRACE.equals(args[1])) {
        log.error("%s: script parameter not implemented", ScriptType.JAVASCRIPT);
        return false;
      }
      try {
        engine.eval(script);
      } catch (ScriptException e) {
        log.trace("%s: error: %s", ScriptType.JAVASCRIPT, e.getMessage());
        return false;
      }
      log.trace("%s: ending run", ScriptType.JAVASCRIPT);
      return true;
    }
  }

  }
