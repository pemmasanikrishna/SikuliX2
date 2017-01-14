/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.test;

import com.sikulix.api.*;
import com.sikulix.core.*;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.opencv.core.Mat;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSXAPI {

  //<editor-fold desc="housekeeping">
  static final int logLevel = SX.INFO;
  static SXLog log = SX.getLogger("TestSXAPI");
  private Object result = "";
  private String currentTest = "";
  private static int nTest = 0;
  private static boolean testLimit = false;

  private static String defaultImagePath = "Images";
  private static String mavenRoot = "target/classes";
  private static String mavenImagePath = mavenRoot + "/" + defaultImagePath;
  private static String jarImagePathDefault = "." + "/" + defaultImagePath;
  private static String jarImagePathClass = "TestJar" + "/" + defaultImagePath;
  private static String httpRoot = "https://raw.githubusercontent.com/RaiMan/SikuliX2";
  private static String httpImagePath = httpRoot + "/" + defaultImagePath;
  private static String imageNameDefault = "sikulix2";
  private static String imageDefault = imageNameDefault + ".png";

  public TestSXAPI() {
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
    Do.setBaseClass();
  }

  @AfterClass
  public static void tearDownClass() {
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
  }

  @After
  public void tearDown() {
    if (log.isLevel(SXLog.TRACE)) {
      log.stopTimer();
    }
    Image.clearPath();
    log.info("!%2d: result: %s: %s ", nTest++, currentTest, result);
  }
  //</editor-fold>

  //<editor-fold desc="running">
  @Test
  public void test_00_play() {
    currentTest = "test_00_play";
    result = "nothing to do";
    assert true;
  }

  @Test
  public void test_10_startup() {
    currentTest = "test_10_startup_workdir";
    String workDir = SX.getUSERWORK();
    if (SX.isNotSet(workDir)) {
      SX.show();
    }
    result = workDir;
    assert SX.isSet(workDir);
  }

  @Test
  public void test_11_startup_native_load() {
    currentTest = "test_11_startup_native_load";
    File test = SX.getFileExists(SX.getSXNATIVE(), SX.sxLibsCheckName);
    result = SX.sxLibsCheckName;
    assert !SX.isNull(test) && SX.existsFile(test);
  }

  @Test
  public void test_20_getBundlePath() {
    currentTest = "test_20_getBundlePath";
    String bundlePath = Do.getBundlePath();
    result = bundlePath;
    assert SX.existsFile(bundlePath);
  }

  @Test
  public void test_21_setBundlePathFile() {
    currentTest = "test_21_setBundlePathFile";
    boolean success = Do.setBundlePath(mavenRoot, defaultImagePath);
    result = Do.getBundlePath();
    success &= SX.existsFile(result);
    assert success;
  }

  @Test
  public void test_22_setBundlePathByClass() {
    currentTest = "test_22_setBundlePathByClass";
    boolean success = Do.setBundlePath(jarImagePathDefault);
    result = Do.getBundlePath();
    success &= SX.existsFile(result);
    assert success;
  }

  @Test
  public void test_23_setBundlePathJarByClass() {
    currentTest = "test_23_setBundlePathJarByClass";
    boolean success = Do.setBundlePath(jarImagePathClass);
    result = Do.getBundlePath();
    success &= SX.existsFile(result);
    assert success;
  }

  @Test
  public void test_24_setBundlePathHttp() {
    currentTest = "test_24_setBundlePathHttp";
    boolean success = Do.setBundlePath(httpRoot, defaultImagePath);
    result = Do.getBundlePath();
    success &= (httpImagePath).equals(result);
    assert success;
  }

  @Test
  public void test_29_getImagePath() {
    currentTest = "test_29_getImagePath";
    Do.setBundlePath(jarImagePathDefault);
    Do.addImagePath(jarImagePathClass);
    Do.addImagePath(httpImagePath);
    String[] paths = Do.getImagePath();
    result = "[";
    for (String path : paths) {
      result += path + ", ";
    }
    result += "]";
    assert 3 == paths.length;
  }

  @Test
  public void test_30_elementConstructors() {
    currentTest = "test_30_elementConstructors";
    Element elem = new Element();
    result = "Element();";
    assert SXElement.eType.ELEMENT.equals(elem.getType());
    Image img = new Image();
    result += " Image();";
    assert SXElement.eType.IMAGE.equals(img.getType());
    Target tgt = new Target();
    result += " Target();";
    assert SXElement.eType.TARGET.equals(tgt.getType());
    tgt = new Target(img);
    result += " Target(image);";
    assert SXElement.eType.TARGET.equals(tgt.getType());
    tgt = new Target(tgt);
    result += " Target(target);";
    assert SXElement.eType.TARGET.equals(tgt.getType());
    Mat aMat = tgt.getContent();
    tgt = new Target(aMat);
    result += " Target(mat);";
    assert SXElement.eType.TARGET.equals(tgt.getType());
    tgt = new Target(img, 0.95, new Element(2, 3));
    result += " Target(image, 0.95, new Element(2,3));";
    assert SXElement.eType.TARGET.equals(tgt.getType());
  }

  @Test
  public void test_31_loadImageFromFile() {
    currentTest = "test_31_loadImageFromFile";
    boolean success = Do.setBundlePath(mavenRoot, "Images");
    result = "BundlePath: " + Do.getBundlePath();
    Image img = new Image(imageNameDefault);
    success &= img.isValid();
    if (success) {
      result = set("(%s) Image %s from " + img.getURL(), img.timeToLoad, img.getName());
      if (log.isLevel(SXLog.DEBUG)) {
        img.show();
      }
    }
    assert success;
  }

  @Test
  public void test_32_loadImageFromJarByClass() {
    currentTest = "test_32_loadImageFromJarByClass";
    boolean success = Do.setBundlePath(jarImagePathClass);
    result = "BundlePath: " + Do.getBundlePath();
    Image img = new Image(imageNameDefault);
    success &= img.isValid();
    if (success) {
      result = set("(%s) Image %s from " + img.getURL(), img.timeToLoad, img.getName());
      if (log.isLevel(SXLog.DEBUG)) {
        img.show();
      }
    }
    assert success;
  }

  @Test
  public void test_33_loadImageFromHttp() {
    currentTest = "test_33_loadImageFromHttp";
    boolean success = Do.setBundlePath(httpRoot, "master");
    result = "BundlePath: " + Do.getBundlePath();
    Image img = new Image(imageNameDefault);
    success &= img.isValid();
    if (success) {
      result = set("(%s) Image %s from " + img.getURL(), img.timeToLoad, img.getName());
      if (log.isLevel(SXLog.DEBUG)) {
        img.show();
      }
    }
    assert success;
  }

  @Test
  public void test_40_createFinderFromImage() {
    currentTest = "test_40_createFinderFromImage";
    boolean success = Do.setBundlePath(mavenRoot, "Images");
    result = "";
    Image img = new Image(imageNameDefault);
    success &= img.isValid();
    if (success) {
      Finder finder = new Finder(img);
      success &= finder.isValid();
    }
    assert success;
  }

  @Test
  public void test_41_findImageInSameImage() {
    currentTest = "test_41_findImageInSameImage";
    boolean success = Do.setBundlePath(mavenRoot, "Images");
    result = "Not Found";
    start();
    Image base = new Image(imageNameDefault);
    success &= base.isValid();
    Image img = new Image(base);
    Element element = null;
    if (success) {
      element = Do.find(img, base);
      success &= element.isMatch() && 0.99 < element.getScore() &&
              0 == element.x && 0 == element.y &&
              element.w == (int) base.w && element.h == (int) base.h;
    }
    if (success) {
      result = element.toString();
      base.showMatch();
    }
    result = end() + result;
    assert success;
  }

  @Test
  public void test_42_findImageInOtherImage() {
    currentTest = "test_42_findImageInOtherImage";
    boolean success = Do.setBundlePath(mavenRoot, "Images");
    result = "Not Found";
    start();
    Image target = new Image(imageNameDefault);
    success &= target.isValid();
    Image base = new Image("shot-tile");
    success &= base.isValid();
    Element element = null;
    if (success) {
      element = Do.find(target, base);
      success &= element.isMatch();
    }
    if (success) {
      result = element.toString();
    }
    result = end() + result;
    if (success) {
      base.showMatch();
    }
    assert success;
  }

  @Test
  public void test_43_findAllInImage() {
    currentTest = "test_43_findAllInImage";
    boolean success = Do.setBundlePath(mavenRoot, "Images");
    result = "Not Found";
    start();
    Image target = new Image(imageNameDefault);
    success &= target.isValid();
    Image base = new Image("shot-tile");
    int expected = (int) (base.w / 200) * (int) (base.h / 200);
    success &= base.isValid();
    List<Element> elements = new ArrayList<>();
    if (success) {
      elements = Do.findAll(target, base);
      success &= elements.size() == expected;
    }
    result = String.format("#%d in (%dx%d) %% %.2f +- %.4f]", elements.size(),
            base.w, base.h, 100 * base.getLastScores()[0], 100 * base.getLastScores()[2]);
    result = end() + result;
    if (success) {
      base.showMatches();
    }
    assert success;
  }

  @Test
  public void test_50_captureDefaultScreen() {
    currentTest = "test_50_captureDefaultScreen";
    prepareDefaultScreen();
    if (isHeadless) {
      return;
    }
    start();
    Image img = Do.capture();
    result = end() + img.toString();
    assert img.hasContent();
  }


  @Test
  public void test_51_capturePartOfDefaultScreen() {
    currentTest = "test_51_capturePartOfDefaultScreen";
    assert prepareDefaultScreen(imageNameDefault);
    if (isHeadless) {
      return;
    }
    start();
    Image img = Do.capture((Element) elemDisplayed.grow());
    result = end() + img.toString();
    SX.getMain().stopShowing();
    if (img.hasContent()) {
      img.show();
      return;
    }
    assert false;
  }

  Image base = null;
  Image img = null;
  Element match = null;
  List<Element> matches = new ArrayList<>();
  boolean isHeadless = false;
  Element elemDisplayed = null;
  int showPause = 3;

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
      base = new Image(fnBase);
      if (base.hasContent()) {
        base.showContent();
        SX.pause(showPause);
        elemDisplayed = SX.getMain().whereShowing();
        if (SX.isNull(fnImg)) {
          return true;
        }
        result = "Not Found";
        start();
        img = new Image(fnImg);
        if (img.hasContent()) {
          return true;
        }
      }
      return false;
    }
    isHeadless = true;
    return true;
  }

  @Test
  public void test_52_findInDefaultScreen() {
    currentTest = "test_52_findInDefaultScreen";
    assert prepareDefaultScreen("shot", imageNameDefault);
    if (isHeadless) {
      return;
    }
    match = Do.find(img);
    result = end() + match.toString();
    assert match.isValid();
    SX.getMain().showMatch();
  }

  @Test
  public void test_53_findAllInDefaultScreen() {
    currentTest = "test_53_findAllInDefaultScreen";
    assert prepareDefaultScreen("shot-tile", imageNameDefault);
    if (isHeadless) {
      return;
    }
    int expected = (int) (base.w / 200) * (int) (base.h / 200);
    matches = Do.findAll(img);
    assert matches.size() == expected;
    result = String.format("#%d in (%dx%d) %% %.2f +- %.4f]", matches.size(),
            base.w, base.h, 100 * SX.getMain().getLastScores()[0], 100 * SX.getMain().getLastScores()[2]);
    result = end() + result;
    SX.getMain().showMatches();
  }

//</editor-fold>

  //<editor-fold desc="ignored">
  @Ignore
  public void test_60_nativeHook() {
    currentTest = "test_60_nativeHook";
    if (!SX.isHeadless()) {
      NativeHook hook = NativeHook.start();
      SX.pause(3);
      hook.stop();
      result = "NativeHook works";
    } else {
      result = "headless: NativeHook not tested";
    }
    assert true;
  }

  @Ignore
  public void test_90_popat() {
    boolean assertVal = true;
    currentTest = "test_90_popat";
    if (!SX.onTravisCI()) {
      Do.popat(300, 300);
      Do.popup("Use mouse to click OK", "testing popat");
      Element loc = Element.at();
      result = String.format("clicked at (%d, %d)", loc.x, loc.y);
      assertVal = loc.x > 300 && loc.x < 450;
    } else {
      result = "TravisCI: not testing";
    }
    assert assertVal;
  }
  //</editor-fold>
}
