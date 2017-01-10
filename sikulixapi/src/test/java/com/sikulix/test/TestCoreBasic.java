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
import java.util.Date;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestCoreBasic {

  //<editor-fold desc="housekeeping">
  static final int logLevel = SX.INFO;
  static SXLog log = SX.getLogger("TestCoreBasic");
  private Object result = "";
  private String currentTest = "";
  private static int nTest = 0;
  private static boolean testLimit = false;

  private static String defaultImagePath = "Images";
  private static String mavenRoot = "target/test-classes";
  private static String mavenImagePath = mavenRoot + "/" + defaultImagePath;
  private static String jarImagePathDefault = "." + "/" + defaultImagePath;
  private static String jarImagePathClass = "TestJar" + "/" + defaultImagePath;
  private static String httpRoot = "https://raw.githubusercontent.com/RaiMan/SikuliX2";
  private static String httpImagePath = httpRoot + "/" + defaultImagePath;
  private static String imageNameDefault = "sikulix2";
  private static String imageDefault = imageNameDefault + ".png";

  public TestCoreBasic() {
    log.trace("TestCoreBasic()");
  }

  private String set(String form, Object... args) {
    return String.format(form, args);
  }

  long startTime = -1;

  private void start() {
    startTime = new Date().getTime();
  }

  private String end() {
    String duration = "";
    if (startTime > 0) {
      duration = "(" + (new Date().getTime() - startTime) + ") ";
      startTime = -1;
    }
    return duration;
  }

  @BeforeClass
  public static void setUpClass() {
    log.trace("setUpClass()");
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
    log.trace("tearDownClass()");
  }

  @Before
  public void setUp() {
    log.trace("setUp");
    result = null;
    startTime = -1;
    if (testLimit && nTest > 0) {
      SX.terminate(1, "by intention");
    }
    log.on(logLevel);
  }

  @After
  public void tearDown() {
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
    if (!SX.isLinux()) {
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
  }

  @Test
  public void test_31_loadImageFromFile() {
    currentTest = "test_31_loadImageFromFile";
    boolean success = Do.setBundlePath(mavenRoot, "Images");
    result = "BundlePath: " + Do.getBundlePath();
    Image img = new Image(imageNameDefault);
    success &= img.isValid();
    if (success) {
      result = set("(%s) ImageURL: " + img.getURL(), img.timeToLoad);
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
      result = set("(%s) ImageURL: " + img.getURL(), img.timeToLoad);
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
      result = set("(%s) ImageURL: " + img.getURL(), img.timeToLoad);
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
    Image img = new Image(imageNameDefault);
    success &= img.isValid();
    Finder finder = null;
    if (success) {
      finder = new Finder(img);
      success &= finder.isValid();
    }
    Element element = null;
    if (success) {
      element = finder.find(img);
      success &= element.isMatch() && 0.99 < element.getScore() &&
              0 == element.x && 0 == element.y &&
              element.w == (int) img.w && element.h == (int) img.h;
    }
    if (success) {
      result = element.toString();
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
    Image base = new Image("shot");
    success &= base.isValid();
    Finder finder = null;
    if (success) {
      finder = new Finder(base);
      success &= finder.isValid();
    }
    Element element = null;
    if (success) {
      element = finder.find(target);
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
  //</editor-fold>

  //<editor-fold desc="ignored">
  @Ignore
  public void test_50_nativeHook() {
    currentTest = "test_50_nativeHook";
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
