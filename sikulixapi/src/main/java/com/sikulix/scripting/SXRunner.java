/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.scripting;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.FileReader;

public class SXRunner {

  static SXLog log;
  static {
    log = SX.getLogger("SXRunner");
    log.on(SXLog.INFO);
  }

  static ScriptEngine engine = null;

  public static Object run(String type, Object... args) {
    if ("js".equals(type)) {
      return runjs(args);
    }
    log.terminate(1, "run: scripttype %s not implemented", type);
    return null;
  }

  public static Object runjs(Object... args) {
    log.trace("running JavaScript");
    String beforeRun = "";
    String sxapi = new File(SX.getUSERWORK(), "target/test-classes/JavaScript/sxapi.js").getAbsolutePath();
    beforeRun += String.format("load(\"%s\");", sxapi);
    Object script = null;
    Object returnValue = null;
    if (args.length > 0) {
      if (SX.isNull(engine)) {
        engine = new ScriptEngineManager().getEngineByName("nashorn");
        if (SX.isNull(engine)) {
          log.terminate(1, "ScriptEngine(nashorn) not available");
        }
      }
      script = args[0];
      if (script instanceof String) {
        try {
          engine.eval(beforeRun);
          returnValue = engine.eval((String) script);
        } catch (ScriptException e) {
          log.error("Nashorn: eval(String): %s", e.getMessage());
        }
      } else if (script instanceof File) {
        try {
          engine.eval(beforeRun);
          returnValue = engine.eval(new FileReader((File) script));
        } catch (Exception e) {
          log.error("Nashorn: eval(File): %s", e.getMessage());
        }
      }
    }
    return returnValue;
  }
}
