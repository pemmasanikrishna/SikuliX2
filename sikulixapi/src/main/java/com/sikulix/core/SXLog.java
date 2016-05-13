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


  protected SXLog() {
    setLogger("SX.Global");
  }

  public SXLog(String className) {
    init(className, null);
  }

  public SXLog(String className, String[] args) {
    init(className, args);
  }

  private void init(String className, String[] args) {
    SX.sxinit(args);
    setLogger(className);
  }

  final int stdLvl = DEBUG;
  int currentLogLevel = 0;
  boolean logError = true;

  Logger logger;

  public void setLogger(String cls, int level) {
    currentLogLevel = level;
    logger = LogManager.getLogger(cls);
  }

  public void setLogger(String cls) {
    setLogger(cls, stdLvl);
  }

  public boolean isLvl(int level) {
    if (level < 0) {
      return logError;
    }
    if (SX.globalLogLevel > 0 && SX.globalLogLevel >= level) {
      return true;
    }
    if (currentLogLevel > 0 && currentLogLevel >= level) {
      return true;
    }
    return false;
  }

  public void logOff() {
    currentLogLevel = 0;
    SX.globalLogLevel = 0;
  }

  public void logOffError() {
    logError = false;
  }

  public void logOnError() {
    logError = true;
  }

  public void logOn(int level) {
    currentLogLevel = level;
  }

  public void logOnGlobal(int level) {
    SX.globalLogLevel = level;
  }

  public void log(int level, String message, Object... args) {
    if (isLvl(level)) {
      message = String.format(message, args).replaceAll("\\n", " ");
      if (level == DEBUG) {
        logger.debug(message, args);
      } else if (level > DEBUG) {
        logger.trace(message, args);
      } else if (level == ERROR) {
        if (logError) {
          logger.error(message, args);
        }
      } else if (level == FATAL) {
        logger.fatal("*** terminating: " + message, args);
      } else {
        logger.info(message, args);
      }
    }
  }

  public void logp(String message, Object... args) {
    log(1, message, args);
  }

  public void terminate(int retval, String message, Object... args) {
    //TODO terminate: check IDE is running (additionally popup)
    logger.fatal(String.format(" *** terminating: " + message, args));
    System.exit(retval);
  }

  public void p(String msg, Object... args) {
    System.out.println(String.format(msg, args));
  }

  public void info(String message, Object... args) {
    logp(message, args);
  }

  public void error(String message, Object... args) {
    log(-1, message, args);
  }

  public void debug(String message, Object... args) {
    log(3, message, args);
  }

  public void trace(String message, Object... args) {
    log(4, message, args);
  }

}
