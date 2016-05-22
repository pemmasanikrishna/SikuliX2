/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    if (level == FATAL) {
      message = "*** terminating: " + message;
    }
    if (shouldLog(level)) {
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
}

