/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.sikulixcoretest;

import com.sikulix.api.*;
import com.sikulix.core.*;
import com.sikulix.scripting.SXClient;
import com.sikulix.scripting.SXRunner;
import com.sikulix.scripting.SXServer;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.opencv.core.Mat;

import java.io.File;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestCoreBasic {

  static final int logLevel = SX.INFO;
  static SXLog log = SX.getLogger("TestCoreBasic");
  private Object result = null;
  private String currentTest = "";
  private static int nTest = 0;
  private static boolean testLimit = false;

  public TestCoreBasic() {
    log.trace("TestCoreBasic()");
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
    Commands.setBaseClass();
  }

  @AfterClass
  public static void tearDownClass() {
    log.trace("tearDownClass()");
  }

  @Before
  public void setUp() {
    log.trace("setUp");
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
  public void test_15_elementConstructors() {
    currentTest = "test_15_elementConstructors";
    String bundlePath = "target/test-classes";
    boolean success = Commands.setBundlePath(bundlePath, "Images");
    assert success;
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
      img = new Image("ich.png");
      result += " Image(\"ich.png\");";
      assert SXElement.eType.IMAGE.equals(img.getType());
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
  public void test_20_getBundlePath() {
    currentTest = "test_20_getBundlePath";
    String bundlePath = Commands.getBundlePath();
    result = bundlePath;
    assert SX.existsFile(bundlePath);
  }

  @Test
  public void test_21_setBundlePathFile() {
    currentTest = "test_21_setBundlePathFile";
    String bundlePath = "target/test-classes";
    boolean success = Commands.setBundlePath(bundlePath, "Images");
    result = Commands.getBundlePath();
    success &= SX.existsFile(result);
    assert success;
  }

  @Test
  public void test_22_setBundlePathByClass() {
    currentTest = "test_22_setBundlePathByClass";
    String bundlePath = "./Images";
    boolean success = Commands.setBundlePath(bundlePath);
    result = Commands.getBundlePath();
    success &= SX.existsFile(result);
    assert success;
  }

  @Test
  public void test_23_setBundlePathJarByClass() {
    currentTest = "test_23_setBundlePathJarByClass";
    String bundlePath = "TestJar/Images";
    boolean success = Commands.setBundlePath(bundlePath);
    result = Commands.getBundlePath();
    success &= SX.existsFile(result);
    assert success;
  }

  @Test
  public void test_24_setBundlePathHttp() {
    currentTest = "test_24_setBundlePathHttp";
    String bundlePath = "http://sikulix.com";
    String subPath = "images";
    boolean success = Commands.setBundlePath(bundlePath, subPath);
    result = Commands.getBundlePath();
    success &= (bundlePath + "/" + subPath).equals(result);
    assert success;
  }

  @Test
  public void test_29_getImagePath() {
    currentTest = "test_29_getImagePath";
    Commands.setBundlePath("./Images");
    Commands.addImagePath("TestJar/Images");
    Commands.addImagePath("http://sikulix.com/images");
    String[] paths = Commands.getImagePath();
    result = "[";
    for (String path : paths) {
      result += path + ", ";
    }
    result += "]";
    assert 3 == paths.length;
  }

  @Test
  public void test_30_nativeHook() {
    currentTest = "test_30_nativeHook";
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

  @Test
  public void test_40_startServer() {
    currentTest = "test_40_startServer";
    result = "Server works\n";
    Runnable server = new Runnable() {
      @Override
      public void run() {
        SXServer.start(null);
      }
    };
    new Thread(server).start();
    SX.pause(3);
    Element vis = new Element(300, 300, 500, 500);
    vis.setLastMatch(new Element(new Element(400, 400, 50, 50), 0.92345678, new Element(100, 100)));
    JSONObject jVis = SXJson.makeElement(vis);
    SXClient.get("/");
    SXClient.post("/session/12", jVis.toString());
    SX.pause(1);
    boolean retVal = SXClient.stopServer();
    SX.pause(4);
//    result += jVis.toString(2);
    assert retVal;
  }

  @Test
  public void test_50_runJavaScript() {
//    log.globalOn(SXLog.TRACE);
    log.isSX();
    log.on(SXLog.INFO);
    boolean assertVal = true;
    currentTest = "test_50_runJavaScript";
    if (!SX.isLinux()) {
      result = "JavaScript works";
      Commands.setBundlePath("target/test-classes/Images");
      Object objResult = SXRunner.runjs(new File(SX.getUSERWORK(), "target/test-classes/JavaScript/test.js"));
    } else {
      result = "Linux: not testing";
    }
    assert assertVal;
  }

  @Test
  public void test_90_popat() {
    boolean assertVal = true;
    currentTest = "test_90_popat";
    if (!SX.onTravisCI()) {
      Commands.popat(300, 300);
      Commands.popup("Use mouse to click OK", "testing popat");
      Element loc = Mouse.at();
      result = String.format("clicked at (%d, %d)", loc.x, loc.y);
      assertVal = loc.x > 300 && loc.x < 450;
    } else {
      result = "TravisCI: not testing";
    }
    assert assertVal;
  }
}
