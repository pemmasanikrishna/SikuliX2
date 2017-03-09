/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.test;

import com.sikulix.api.*;
import com.sikulix.core.*;
import com.sikulix.run.Runner;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRun {

  //<editor-fold desc="housekeeping">
  static final int logLevel = SX.INFO;
  static SXLog log = SX.getLogger("TestSXScripting");
  private Object result = "";
  private String currentTest = "";
  private static int nTest = 0;
  private static boolean testLimit = false;

  private static String defaultImagePath = "Images";
  private static String mavenRoot = "target/classes";
  private static String mavenImagePath = mavenRoot + "/" + defaultImagePath;
  private static String jarImagePathDefault = "." + "/" + defaultImagePath;
  private static String jarImagePathClass = "TestJar" + "/" + defaultImagePath;
  private static String httpRoot = "http://download.sikulix.com";
  private static String httpImagePath = httpRoot + "/" + defaultImagePath;
  private static String imageNameDefault = "sikulix2";
  private static String imageDefault = imageNameDefault + ".png";

  public TestRun() {
    log.trace("TestAPI()");
  }

  private String set(String form, Object... args) {
    return String.format(form, args);
  }

  long startTime = -1;

  private void start() {
    startTime = new Date().getTime();
    log.trace("local timer start");
  }

  private String end() {
    String duration = "";
    if (startTime > 0) {
      duration = "(" + (new Date().getTime() - startTime) + ") ";
      startTime = -1;
    }
    log.trace("local timer end");
    return duration;
  }

  private static NativeHook hook = null;
  private static Symbol button = null;

  @BeforeClass
  public static void setUpClass() {
    if (SX.existsFile(SX.getFolder(SX.getSXAPP()))) {
      Content.deleteFileOrFolder(SX.getFolder(SX.getSXAPP()), new Content.FileFilter() {
        @Override
        public boolean accept(File entry) {
          if (entry.getAbsolutePath().contains("SX2/Images") ||
                  entry.getAbsolutePath().contains("SX2/Native")) {
            return true;
          }
          return false;
        }
      });
    }
    SX.setBaseClass();
    SX.setOption("SX.withHook", "yes");
  }

  @AfterClass
  public static void tearDownClass() {
    if (SX.isNotNull(hook)) {
      hook.stop();
    }
    log.info("hook stopped");
  }

  @Before
  public void setUp() {
    result = null;
    startTime = -1;
    if (testLimit && nTest > 0) {
      SX.terminate(1, "by intention");
    }
    log.on(logLevel);
    if (log.isLevel(SXLog.TRACE)) {
      log.startTimer();
    }
    if (SX.isNull(hook)) {
      hook = Do.getHook();
      log.info("hook started");
    }
    button = Symbol.button(188, 68).setColor(Color.red).fill(Color.cyan).setLine(10);
    Do.setBundlePath(httpRoot, defaultImagePath);
  }

  @After
  public void tearDown() {
    if (log.isLevel(SXLog.TRACE)) {
      log.stopTimer();
    }
    Picture.clearPath();
    resetDefaultScreen();
    Events.reset();
    if (SX.isSetSXLOCALDEVICE()) {
      Do.on().removeEvents();
    }
    SX.setOption("Settings.MoveMouseDelay", "0.5");
    log.info("!%2d: result: %s: %s ", nTest++, currentTest, result);
  }

  Picture base = null;
  Picture img = null;
  Element match = null;
  List<Element> matches = new ArrayList<>();
  boolean isHeadless = false;
  Element elemDisplayed = null;
  int showPauseAfter = 2;
  int showPauseBefore = 0;
  Story theShow = null;
  //</editor-fold>

  private void prepareDefaultScreen() {
    prepareDefaultScreen(null, null);
  }

  private boolean prepareDefaultScreen(String fnImg) {
    return prepareDefaultScreen(fnImg, null);
  }

  private boolean prepareDefaultScreen(String fnBase, String fnImg) {
    result = "headless: not tested";
    if (!SX.isHeadless()) {
      if (SX.isNull(fnBase)) {
        return true;
      }
      Do.setBundlePath(mavenRoot, "Images");
      base = new Picture(fnBase);
      if (base.hasContent()) {
        theShow = new Story(base, showPauseAfter, showPauseBefore).start();
        elemDisplayed = theShow.whereShowing();
        if (SX.isNull(fnImg)) {
          return true;
        }
        result = "Not Found";
        start();
        img = new Picture(fnImg);
        if (img.hasContent()) {
          return true;
        }
      }
      return false;
    }
    isHeadless = true;
    return true;
  }

  private void resetDefaultScreen() {
    base = null;
    img = null;
    match = null;
    matches = new ArrayList<>();
    isHeadless = false;
    elemDisplayed = null;
    showPauseAfter = 2;
    showPauseBefore = 0;
  }

  //<editor-fold desc="running">
  @Test
  public void test_000_play() {
    currentTest = "test_000_play";
    result = "nothing to do";
    assert true;
  }

  @Test
  public void test_501_runJavaScriptBasic() {
    currentTest = "test_001_runJavaScriptBasic";
    if (!SX.isHeadless()) {
      result = "running JavaScript: mouse moves to center";
      String script = "var element = Do.hover();\n" +
              "print('Hello from JavaScript: mouse at: ' + element);";
      Runner.run(Runner.ScriptType.JAVASCRIPT, script);
      Element center = Do.on().getCenter();
      assert Do.isMouseposition(hook, center.x, center.y) : "mouse should be at center of screen";
    }
  }

  @Test
  public void test_502_runJavaScriptFromJar() {
    currentTest = "test_002_runJavaScriptFromJar";
    if (!SX.isHeadless()) {
      result = "running JavaScript from jar: mouse moves to center";
      Runner.run("basic");
      Element center = Do.on().getCenter();
      assert Do.isMouseposition(hook, center.x, center.y) : "mouse should be at center of screen";
    }
  }

  @Test
  public void test_503_runJavaScriptFromNet() {
    currentTest = "test_003_runJavaScriptFromNet";
    if (!SX.isHeadless()) {
      result = "running JavaScript from net: mouse moves to center";
      Object result = Runner.run(Runner.ScriptType.FROMNET, "basic");
      assert SX.isNotNull(result) : "script from net not valid";
      Element center = Do.on().getCenter();
      assert Do.isMouseposition(hook, center.x, center.y) : "mouse should be at center of screen";
    }
  }

  @Test
  public void test_504_runJavaScriptWithFind() {
    currentTest = "test_004_runJavaScriptWithFind";
    if (!SX.isHeadless()) {
      result = "running JavaScript: find image on screen";
      Picture picture = new Picture("shot-tile");
      assert picture.isValid() : "Image: shot-tile not valid";
      Story story = new Story(picture, 3).start();
      Runner.run("basic1");
      story.waitForEnd();
    }
  }

  //</editor-fold>

  //<editor-fold desc="ignored">
  //</editor-fold>

  //log.startTimer();
  @Test
  public void test_999_someThingToTest() {
    //log.startTimer();
    currentTest = "test_0999_someThingToTest";
    if (!SX.onTravisCI() && log.isGlobalLevel(log.TRACE)) {
      if (!SX.isHeadless()) {
// start
        result = "nothing to do here";
//end
      } else {
        result = "headless: not testing";
      }
    } else {
      result = "TravisCI or NonInteractive: not testing";
    }
    assert true;
  }
}
