/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SXLog {

  public static final int INFO = 1;
  public static final int DEBUG = 3;
  public static final int TRACE = 4;
  public static final int ERROR = -1;
  public static final int FATAL = -2;

  int initLogLevel = -1;
  int currentLogLevel = -1;

  static int globalLogLevel = -1;
  boolean logError = true;

  Logger logger = null;

  protected SXLog() {
    init(null, null, -1);
    logger = LogManager.getLogger("SX.Global");
    getTranslation("sxinit: entry", "");
  }

  public SXLog(String className, String[] args, int level) {
    init(className, args, level);
  }

  private void init(String className, String[] args, int level) {
    if (initLogLevel < 0) {
      initLogLevel = 0;
      String logOption = System.getProperty("sikulix.logging");
      if (!SX.isUnset(logOption)) {
        logOption = logOption.toLowerCase();
        if (logOption.startsWith("q")) {
          initLogLevel = -1;
        } else if (logOption.startsWith("d")) {
          initLogLevel = 3;
        } else if (logOption.startsWith("t")) {
          initLogLevel = 4;
        }
      }
      if (initLogLevel < 0) {
        initLogLevel = 0;
        globalLogLevel = -1;
      } else {
        globalLogLevel = initLogLevel;
      }
    }
    if (level < 0) {
      currentLogLevel = initLogLevel;
    } else {
      on(level);
    }
    if (SX.isNull(className)) {
      return;
    }
    SX.sxinit(args);
    logger = LogManager.getLogger(className);
  }

  public void off() {
    currentLogLevel = 0;
  }

  public void stop() {
    currentLogLevel = 0;
    errorOff();
  }

  public void errorOff() {
    logError = false;
  }

  public void errorOn() {
    logError = true;
  }

  public void on(int level) {
    if (level > 0 && level <= TRACE) {
      currentLogLevel = level;
    } else {
      if (level < 1) {
        currentLogLevel = 0;
      } else {
        currentLogLevel = 1;
      }
    }
  }

  public void globalOn(int level) {
    if (level > 0 && level <= TRACE) {
      globalLogLevel = level;
    } else {
      if (level < 1) {
        globalLogLevel = 0;
      } else {
        globalLogLevel = 1;
      }
    }
  }

  public void globalStop() {
    globalLogLevel = -1;
  }

  public void p(String msg, Object... args) {
    System.out.println(String.format(msg, args));
  }

  public void info(String message, Object... args) {
    log(INFO, message, args);
  }

  public void error(String message, Object... args) {
    log(ERROR, message, args);
  }

  public void debug(String message, Object... args) {
    log(DEBUG, message, args);
  }

  public void trace(String message, Object... args) {
    log(TRACE, message, args);
  }

  public void fatal(String message, Object... args) {
    log(FATAL, message, args);
  }

  public void terminate(int retval, String message, Object... args) {
    //TODO terminate: check IDE is running (additionally popup)
    if (retval != 0) {
      log(FATAL, message, args);
    } else {
      log(INFO, message, args);
    }
    System.exit(retval);
  }

  private boolean shouldLog(int level) {
    if (level == FATAL) {
      return true;
    }
    if (globalLogLevel < 0) {
      return false;
    }
    if (level < 0) {
      return logError;
    }
    if (currentLogLevel == 0) {
      return globalLogLevel >= level;
    }
    return currentLogLevel >= level;
  }

  private void log(int level, String message, Object... args) {
    String msgPlus = "";
    if (level == FATAL) {
      msgPlus = "terminating";
    }
    if (shouldLog(level)) {
      message = getTranslation(message, msgPlus);
       message = String.format(message, args).replaceAll("\\n", " ");
      if (level == DEBUG) {
        logger.debug(message, args);
      } else if (level > DEBUG) {
        logger.trace(message, args);
      } else if (level == ERROR) {
        if (logError) {
          logger.error(message, args);
        }
      } else {
        logger.info(message, args);
      }
    }
  }

  private static Map<String, Properties> translateProps = new HashMap<>();

  private boolean translation = true;

  public void setTranslation(boolean state) {
    translation = state;
  }

  private String getTranslation(String msg, String msgPlus) {
    if (!translation) {
      return (!SX.isUnset(msgPlus) ? "*** " + msgPlus + ": " : "") + msg;
    }
    String orgMsg = msg;
    String clazz = logger.getName().replaceAll("\\.", "");
    Properties currentProps = null;
    if (!translateProps.containsKey(clazz)) {
      currentProps = null;
      InputStream isProps = null;
      try {
        String resProp = "I18n/" + clazz + "_en_US.properties";
        isProps = this.getClass().getClassLoader().getResourceAsStream(resProp);
        if (!SX.isNull(isProps)) {
          currentProps = new Properties();
          currentProps.load(isProps);
        }
      } catch (IOException e) {
        isProps = null;
      }
      if (SX.isNull(isProps)) {
        System.out.println(String.format("SX.Log: getTranslation: missing ressource for %s", clazz));
        currentProps = null;
      }
      translateProps.put(clazz, currentProps);
    }
    msgPlus = getTranslationGlobal(msgPlus);
    String tKey = "";
    String msgToTranslate = "";
    if (!SX.isNull(currentProps = translateProps.get(clazz))) {
      tKey = clazz;
      String method = "no_method:";
      String phrase = "";
      String[] parts = null;
      if (msg.contains(":")) {
        parts = msg.split(":");
        method = parts[0];
        tKey += "_" + method;
        msg = msg.substring(method.length() + 1).trim();
        msgToTranslate = msg;
      }
      if (msg.contains(" ")) {
        parts = msg.split(" ");
        phrase = parts[0].replaceAll(":", "");
      } else {
        phrase = msg;
      }
      tKey = tKey + "_" + phrase.toLowerCase();
      tKey = tKey.replaceAll("%", "#");
      String trans = currentProps.getProperty(tKey);
      if (!SX.isNull(trans)) {
        return method + ": " + trans;
      }
    }
    return "*** " + String.format("%s (%s = %s)", getTranslationGlobal("translation"),
            tKey, msgToTranslate.replaceAll("%", "#")) + ": "
            + (!SX.isUnset(msgPlus) ? "*** " + msgPlus + ": " : "") + orgMsg;
  }

  private String getTranslationGlobal(String msg) {
    if (!SX.isUnset(msg)) {
      Properties props = translateProps.get("SXGlobal");
      String transMsgPlus = msg;
      if (!SX.isNull(props)) {
        transMsgPlus = props.getProperty("SXGlobal_" + msg.toLowerCase().split(" ")[0]);
      }
      msg = transMsgPlus;
    }
    return msg;
  }
}

