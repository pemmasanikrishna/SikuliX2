package com.sikulix.sikulixapitest;

import java.util.Date;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.sikuli.script.App;
import org.sikuli.script.ImagePath;
import org.sikuli.script.Match;
import org.sikuli.script.Region;
import org.sikuli.script.RunTime;
import org.sikuli.util.Debug;
import org.sikuli.util.Settings;

public class TestFindBasic {

  private static final String logname = "TestFindBasic";
  private static RunTime sxRuntime = RunTime.get();
  private static Region window = null;
  private static String theClass = "";
  private static String images = "";

//<editor-fold defaultstate="collapsed" desc="logging">
  private static final Logger logger = LogManager.getLogger("SX." + logname);

  private static void debug(String message, Object... args) {
    message = String.format(message, args).replaceFirst("\\n", "\n          ");
    logger.debug(message);
  }

  private static void trace(String message, Object... args) {
    message = String.format(message, args).replaceFirst("\\n", "\n          ");
    logger.trace(message, args);
  }

  private static void error(String message, Object... args) {
    message = String.format(message, args).replaceFirst("\\n", "\n          ");
    logger.error(message);
  }

  private static void info(String message, Object... args) {
    message = String.format(message, args).replaceFirst("\\n", "\n          ");
    logger.info(message);
  }

  private void entry(Object... args) {
    if (args.length == 0) {
      start("");
    } else {
      start(args[0].toString());
    }
    logger.entry(args);
  }

  private void exit(Object... args) {
    end();
    if (args.length == 0) {
      logger.exit(0);
    } else {
      logger.exit(String.format("msec:%d return:%d", duration, args[0]));
    }
  }

  private static void print(String message, Object... args) {
    System.out.println(String.format(message, args));
  }

  private static void terminate(int retval, String message, Object... args) {
    logger.fatal(String.format(" *** terminating: " + message, args));
    System.exit(retval);
  }

  private void start(String desc) {
    startTime = new Date().getTime();
    testdesc = desc;
  }

  private void end() {
    duration = new Date().getTime() - startTime;
  }
//</editor-fold>

  private static long startTime;
  private long duration = 0;
  private String testdesc = "";

  public TestFindBasic() {
    theClass = this.getClass().toString().substring(6);
    images = theClass + "/images.sikuli";
    trace("TestRun: %s", theClass);
  }

  @BeforeClass
  public static void setUpClass() {
    Settings.InfoLogs = false;
    Settings.ActionLogs = false;
    Settings.DebugLogs = false;
    trace("setUpClass:");
    App.openLink("http://www.sikulix.com/uploads/1/4/2/8/14281286/1389697664.png");
    App.pause(3);
    window = App.focusedWindow();
    trace("%s", window.toJSON());
    Debug.on(3);
  }

  @AfterClass
  public static void tearDownClass() {
    trace("tearDownClass:");
    ImagePath.remove(images);
    Debug.off();
    App.closeWindow();
  }

  @Before
  public void setUp() {
    debug("------- setUp --------------------------------------");
    ImagePath.add(images);
  }

  @After
  public void tearDown() {
    debug("----- tearDown msec:%d --- %s *****", duration, testdesc);
  }

  @Test
  public void testFirstFind() {
    start("first find - image loaded and cached");
    Match found = window.exists("logo");
    end();
    debug("aTest: found: %s", found.toJSON());
    assertTrue(found != null);
  }

  @Test
  public void testSecondFind() {
    start("second find - cached image reused");
    Match found = window.exists("logo");
    end();
    debug("aTest: found: %s", found.toJSON());
    assertTrue(found != null);
  }
}
