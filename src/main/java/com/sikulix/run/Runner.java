/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.run;

import com.sikulix.core.Content;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Runner {

  static SXLog log = SX.getLogger("SX.Runner");

  public enum ScriptType {FROMUNKNOWN, JAVASCRIPT, PYTHON, RUBY, FROMJAR, FROMNET}

  public enum ScriptOption {WITHTRACE}

  ;
  private static Map<ScriptType, String> scriptTypes = new HashMap<>();

  static {
    scriptTypes.put(ScriptType.JAVASCRIPT, ".js");
    scriptTypes.put(ScriptType.PYTHON, ".py");
    scriptTypes.put(ScriptType.RUBY, ".rb");
  }

  static URL scriptPath = null;
  static String inJarFolderSX = "/Scripts";


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

    Object[] args;
    boolean running = false;
    boolean valid = false;
    ScriptType type = ScriptType.FROMUNKNOWN;
    String scriptName = "";
    URL scriptURL = null;
    String script = "";

    public RunBox(Object[] args) {
      this.args = args;
      if (ScriptOption.WITHTRACE.equals(args[args.length - 1])) {
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
          setValid();
          script = (String) args[firstarg];
          return;
        }
        if (SX.isNotNull(type) && !type.toString().startsWith("FROM")) {
          log.error("RunBox.init: %s not implemented", type);
          return;
        }
        scriptName = (String) args[firstarg];
        String args1 = "";
        String args2 = "";
        if (args.length > firstarg + 1 && args[firstarg + 1] instanceof String) {
          args1 = (String) args[firstarg + 1];
        }
        if (args.length > firstarg + 2 && args[firstarg + 2] instanceof String) {
          args2 = (String) args[firstarg + 2];
        }
        if (ScriptType.FROMUNKNOWN.equals(type) || ScriptType.FROMJAR.equals(type)) {
          String scriptFolder = args1;
          String classReference = args2;
          script = getScriptFromJar(scriptName, scriptFolder, classReference);
          if (SX.isSet(script)) {
            setValid();
            return;
          }
        }
        if (ScriptType.FROMUNKNOWN.equals(type) || ScriptType.FROMNET.equals(type)) {
          String scriptFolder = args1;
          String httpRoot = args2;
          script = getScriptFromNet(scriptName, scriptFolder, httpRoot);
          if (SX.isSet(script)) {
            setValid();
            return;
          }
        }
      } else if (args[firstarg] instanceof File) {

      } else if (args[firstarg] instanceof URL) {

      } else {
        log.error("RunBox.init: invalid args (arg0: %s)", args[0]);
      }
      if (SX.isNotNull(scriptURL)) {
        //TODO load script
      }
      if (isValid()) {
        log.trace("Runbox: init: success for: %s", args[firstarg]);
      }
    }

    private String getScriptFromJar(String scriptName, String scriptFolder, String classReference) {
      String scriptText = "";
      if (SX.isNotSet(classReference)) {
        if (SX.isNotSet(scriptFolder)) {
          scriptFolder = inJarFolderSX;
        }
        scriptFolder += "/" + scriptName;
        for (ScriptType scriptType : scriptTypes.keySet()) {
          String scriptFile = scriptName + scriptTypes.get(scriptType);
          try {
            scriptText = Content.extractResourceToString(scriptFolder, scriptFile);
          } catch (Exception ex) {
            continue;
          }
          if (SX.isSet(scriptText)) {
            type = scriptType;
            break;
          }
          scriptText = "";
        }
      } else {
        log.error("RunBox: getScriptFromJar: non-SX classReference not implemented");
      }
      return scriptText;
    }

    private String getScriptFromNet(String scriptName, String scriptFolder, String httpRoot) {
      String scriptText = "";
      if (SX.isNotSet(httpRoot)) {
        httpRoot = SX.getSXWEBDOWNLOAD();
      }
      if (SX.isNotSet(scriptFolder)) {
        scriptFolder = "/Scripts";
      }
      URL url = SX.getNetURL(httpRoot, scriptFolder + "/" + scriptName);
      if (SX.isNotNull(url)) {
        for (ScriptType scriptType : scriptTypes.keySet()) {
          String scriptFile = scriptName + scriptTypes.get(scriptType);
          scriptText = Content.downloadScriptToString(SX.getURL(url, scriptFile));
          if (SX.isSet(scriptText)) {
            type = scriptType;
            break;
          }
          scriptText = "";
        }
      }
      if (SX.isNotSet(scriptText)) {
        log.error("getScriptFromNet: script not valid: %s", url);
      }
      return scriptText;
    }

    public boolean isValid() {
      return valid;
    }

    public void setValid() {
      valid = true;
    }

    public Object getReturnObject() {
      return new Integer(0);
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
      String scriptBefore = "var Do = Java.type('com.sikulix.api.Do');\n";
      if (log.isLevel(SXLog.TRACE)) {
        scriptBefore += "print('Hello from JavaScript: SikuliX support loaded');\n";
      }
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
