package com.sikulix.sikulixapitest;

import java.io.File;
import java.util.Date;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import static org.junit.Assert.*;
import org.junit.runners.MethodSorters;
import org.sikuli.script.App;
import org.sikuli.script.Image;
import org.sikuli.script.ImagePath;
import org.sikuli.script.Match;
import org.sikuli.script.Region;
import org.sikuli.script.RunTime;
import org.sikuli.util.Debug;
import org.sikuli.util.FileManager;
import org.sikuli.util.Settings;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestFindBasic {

  private static final RunTime sxRT = RunTime.get();

  private static final String loggerName = "TestFindBasic";

//<editor-fold defaultstate="collapsed" desc="logging">
  private static final Logger logger = LogManager.getLogger("SX." + loggerName);

  private static void trace(String message, Object... args) {
    if (logger.getLevel().isLessSpecificThan(Level.TRACE)) {
      message = String.format(message, args).replaceFirst("\\n", "\n          ");
      logger.trace(message, args);
    }
  }

  private static void debug(String message, Object... args) {
    if (logger.getLevel().isLessSpecificThan(Level.DEBUG)) {
      message = String.format(message, args).replaceFirst("\\n", "\n          ");
      logger.debug(message);
    }
  }

  private static void info(String message, Object... args) {
    if (logger.getLevel().isLessSpecificThan(Level.INFO)) {
      message = String.format(message, args).replaceFirst("\\n", "\n          ");
      logger.info(message);
    }
  }

  private static void error(String message, Object... args) {
    if (logger.getLevel().isLessSpecificThan(Level.OFF)) {
      message = String.format(message, args).replaceFirst("\\n", "\n          ");
      logger.error(message);
    }
  }

  private static void terminate(int retval, String message, Object... args) {
    logger.fatal(String.format(" *** terminating: " + message, args));
    System.exit(retval);
  }

  private Timer entry(Object... args) {
    Timer start = null;
    if (args.length == 0) {
      start = new Timer("");
    } else {
      start = new Timer(args[0].toString());
    }
    logger.entry(args);
    return start;
  }

  private void exit(Timer start, Object... args) {
    String msg = "";
    if (args.length > 0) {
      msg = String.format(" return: %s", args[0].toString());
    }
    logger.exit(String.format("%s%s", ((Timer) args[0]).end(), msg));
  }

  private static void print(String message, Object... args) {
    System.out.println(String.format(message, args));
  }

  protected class Timer {

    private long start;
    private String desc = "";

    protected Timer(String desc) {
      start = new Date().getTime();
      this.desc = desc;
      trace("Timer start (%d) %s", start, desc);
    }

    protected long lap(boolean restart) {
      long duration = new Date().getTime() - start;
      if (restart) {
        start = new Date().getTime();
      }
      return duration;
    }

    private String end(Object... args) {
      if (args.length > 0 && args[0] != null) {
        desc = args[0].toString();
      }
      long duration = new Date().getTime() - start;
      return String.format("(msec: %d) %s", duration, desc);
    }
  }
//</editor-fold>

  private static Region window = null;
  private static String theClass = "";
  private static String images = "";
  private static final String testImages = "_testImages";
  private static String testImg = "testImages/testImg";
  private static File fTestImages = null;
  private static String bImage = "";
  private static String sImage = "";

  public TestFindBasic() {
    theClass = this.getClass().toString().substring(6);
    images = theClass + "/images.sikuli";
    trace("--- Test class: %s", theClass);
    ImagePath.setBundlePath(images);
    fTestImages = new File(ImagePath.getBundlePath(), testImages);
    fTestImages.mkdir();
    if (bImage.isEmpty()) {
      Image img = Image.get("logo");
      sImage = String.format("(%d x %d)", img.getWidth(), img.getHeight());
      bImage = String.format("(%d x %d)", window.w, window.h);
      ImagePath.reset();
    }
  }

  @BeforeClass
  public static void setUpClass() {
    trace("--- Class setup:");
    Settings.InfoLogs = false;
    Settings.ActionLogs = false;
    Settings.DebugLogs = false;
    trace("setUpClass:");
    App.openLink("http://www.sikulix.com/uploads/1/4/2/8/14281286/1389697664.png");
    App.pause(3);
    window = App.focusedWindow();
    trace("searching in region %s", window.toJSON());
    Debug.on(3);
  }

  @AfterClass
  public static void tearDownClass() {
    trace("--- Class teardown:");
    FileManager.deleteFileOrFolder(fTestImages);
    Debug.off();
    App.closeWindow();
  }

  @Before
  public void setUp() {
    trace("--- setup:");
    ImagePath.setBundlePath(images);
  }

  @After
  public void tearDown() {
    trace("--- teardown:");
    ImagePath.reset();
  }

  @Test
  public void test_1_FirstFind() {
    Timer timer = new Timer("first find - image loaded and cached");
    Match found = window.exists("logo");
    String msg = timer.end();
    debug("found: %s", found.toJSON());
    info("--- 1 testFirstFind: %s %s in %s", msg, sImage, bImage);
    assertTrue(found != null);
  }

  @Test
  public void test_2_SecondFind() {
    Image.get("logo");
    Timer timer = new Timer("second find - cached image reused - no check last seen");
    Settings.CheckLastSeen = false;
    Match found = window.exists("logo");
    String msg = timer.end();
    debug("found: %s", found.toJSON());
    info("--- 2 testSecondFind: %s  %s in %s", msg, sImage, bImage);
    assertTrue(found != null);
  }

  @Test
  public void test_3_SecondFindLastSeen() {
    Image.get("logo");
    Match found = window.exists("logo");
    Settings.CheckLastSeen = true;
    Timer timer = new Timer("second find - cached image reused - check last seen");
    found = window.exists("logo");
    String msg = timer.end();
    debug("found: %s", found.toJSON());
    info("--- 3 testSecondFindLastSeen: %s %s in %s", msg, sImage, bImage);
    assertTrue(found != null);
  }

  @Test
  public void test_4_FindInImageLoaded() {
    String anImageFile = window.save(testImg + "4");
    Timer timer = new Timer("find in an image loaded from filesystem");
    Image anImage = Image.get(anImageFile);
    Match found = anImage.find("logo");
    String msg = timer.end();
    debug("found: %s", found.toJSON());
    info("--- 4 testFindInImageLoaded: %s %s in %s", msg, sImage, bImage);
    assertTrue(found != null);
  }

  @Test
  public void test_5_FindInImageInMemory() {
    Image anImage = Image.get(window.save(testImg));
    Timer timer = new Timer("find in an image in cache");
    Match found = anImage.find("logo");
    String msg = timer.end();
    debug("found: %s", found.toJSON());
    info("--- 5 testFindInImageInMemory: %s %s in %s", msg, sImage, bImage);
    assertTrue(found != null);
  }

  @Test
  public void test_6_Compare2ImagesInMemory() {
    Image anImage = Image.get(window.save(testImg));
    Timer timer = new Timer("compare 2 cached images ");
    Match found = anImage.find(anImage);
    String msg = timer.end();
    debug("found: %s", found.toJSON());
    info("--- 6 testCompare2ImagesInMemory: %s %s in %s", msg, bImage, bImage);
    assertTrue(found != null);
  }

  @Test
  public void test_7_Compare2ImagesLoaded() {
    Timer timer = new Timer("compare 2 images loaded from filesystem");
    String anImageFile1 = window.save(testImg + "7");
    String anImageFile2 = window.save(testImg + "71");
    debug("capture and store 2 images: %d msec", timer.lap(true));
    Match found = Image.get(anImageFile1).find(anImageFile2);
    String msg = timer.end();
    debug("found: %s", found.toJSON());
    info("--- 7 testCompare2ImagesLoaded: %s %s in %s", msg, bImage, bImage);
    assertTrue(found != null);
  }
}
