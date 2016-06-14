/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.sikulixcoretest;

import com.sikulix.api.Image;
import com.sikulix.api.Commands;
import com.sikulix.api.Location;
import com.sikulix.api.Mouse;
import com.sikulix.core.*;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.File;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestCoreBasic extends Commands {

  static final int logLevel = INFO;
  static SXLog log = getLogger("TestCoreBasic");
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
    if (existsFile(getFile(getSXAPP()))) {
      Content.deleteFileOrFolder(getFolder(getSXAPP()), new Content.FileFilter() {
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
  }

  @AfterClass
  public static void tearDownClass() {
    log.trace("tearDownClass()");
  }

  @Before
  public void setUp() {
    log.trace("setUp");
    if (testLimit && nTest > 0) {
      terminate(1, "by intention");
    }
    log.on(logLevel);
  }

  @After
  public void tearDown() {
    log.info("%2d: result: %s: %s ", nTest++, currentTest, result);
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
    String workDir = getUSERWORK();
    if (isUnset(workDir)) {
      show();
    }
    result = workDir;
    assert !isUnset(workDir);
  }

  @Test
  public void test_11_startup_native_load() {
    currentTest = "test_11_startup_native_load";
    Image img = new Image();
    File test = getFile(getSXNATIVE(), sxLibsCheckName);
    result = test.toString();
    assert (isLinux() ? true : existsFile(test));
  }

  @Test
  public void test_20_getBundlePath() {
    currentTest = "test_20_getBundlePath";
    String bundlePath = getBundlePath();
    result = bundlePath;
    assert existsFile(bundlePath);
  }

  @Test
  public void test_21_setBundlePath() {
    currentTest = "test_21_setBundlePath";
    String bundlePath = "My Images";
    boolean success = setBundlePath(bundlePath);
    result = getBundlePath();
    success &= existsFile(result);
    assert success;
  }

  @Test
  public void test_22_getImagePath() {
    currentTest = "test_22_getImagePath";
    String[] paths = getImagePath();
    result = "[";
    for (String path : paths) {
      result += path + ", ";
    }
    result += "]";
    assert 1 == paths.length;
  }

  @Test
  public void test_30_popat() {
    boolean assertVal = true;
    if (!SX.isHeadless()) {
      currentTest = "test_30_popat";
      popat(300, 300);
      popup("Use mouse to click OK", "testing popat");
      Location loc = Mouse.at();
      result = String.format("testing popat - clicked at (%d, %d)", loc.x, loc.y);
      assertVal = loc.x > 300 && loc.x < 450;
    } else {
      result = "headless: not testing popat";
    }
    assert assertVal;
  }
}
