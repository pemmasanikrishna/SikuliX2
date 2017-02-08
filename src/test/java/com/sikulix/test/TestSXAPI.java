/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.test;

import com.sikulix.api.*;
import com.sikulix.core.*;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.opencv.core.Mat;
import org.sikuli.script.Screen;

import java.awt.Color;
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
    Picture.clearPath();
    resetDefaultScreen();
    Events.reset();
    Do.on().removeEvents();
    log.info("!%2d: result: %s: %s ", nTest++, currentTest, result);
  }

  Picture base = null;
  Picture img = null;
  Element match = new Element();
  List<Element> matches = new ArrayList<>();
  boolean isHeadless = false;
  Element elemDisplayed = new Element();
  int showPauseAfter = 0;
  int showPauseBefore = 0;
  Story theShow = null;

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
        theShow = base.showStart(showPauseAfter, showPauseBefore);
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
    match = new Element();
    matches = new ArrayList<>();
    isHeadless = false;
    elemDisplayed = new Element();
    showPauseAfter = 3;
    showPauseBefore = 0;
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
    Picture img = new Picture();
    result += " Picture();";
    assert SXElement.eType.PICTURE.equals(img.getType());
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
    Picture img = new Picture(imageNameDefault);
    success &= img.isValid();
    if (success) {
      result = set("(%s) Image %s from " + img.getURL(), img.getTimeToLoad(), img.getName());
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
    Picture img = new Picture(imageNameDefault);
    success &= img.isValid();
    if (success) {
      result = set("(%s) Image %s from " + img.getURL(), img.getTimeToLoad(), img.getName());
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
    Picture img = new Picture(imageNameDefault);
    success &= img.isValid();
    if (success) {
      result = set("(%s) Image %s from " + img.getURL(), img.getTimeToLoad(), img.getName());
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
    Picture img = new Picture(imageNameDefault);
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
    Picture base = new Picture(imageNameDefault);
    success &= base.isValid();
    Picture img = new Picture(base);
    Element element = null;
    if (success) {
      base.show();
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
    Picture target = new Picture(imageNameDefault);
    success &= target.isValid();
    Picture base = new Picture("shot-tile");
    success &= base.isValid();
    Element element = null;
    if (success) {
      base.show();
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
    Picture target = new Picture(imageNameDefault);
    success &= target.isValid();
    Picture base = new Picture("shot-tile");
    int expected = (int) (base.w / 200) * (int) (base.h / 200);
    success &= base.isValid();
    List<Element> elements = new ArrayList<>();
    if (success) {
      base.show();
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
    Picture img = Do.capture();
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
    elemDisplayed.grow(20);
    Picture img = Do.capture(elemDisplayed);
    result = end() + img.toString();
    theShow.stop();
    if (img.hasContent()) {
      img.show();
      return;
    }
    assert false;
  }

  @Test
  public void test_52_findInDefaultScreen() {
    currentTest = "test_52_findInDefaultScreen";
    assert prepareDefaultScreen("shot", imageNameDefault);
    if (isHeadless) {
      return;
    }
    match = Do.find(img);
    theShow.stop();
    result = end() + match.toString();
    assert match.isValid();
    Do.on().showMatch();
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
    theShow.stop();
    assert matches.size() == expected;
    result = String.format("#%d in (%dx%d) %% %.2f +- %.4f]", matches.size(),
            base.w, base.h, 100 * SX.getMain().getLastScores()[0], 100 * SX.getMain().getLastScores()[2]);
    result = end() + result;
    Do.on().showMatches();
  }

  @Test
  public void test_54_waitForOnDefaultScreen() {
    currentTest = "test_54_waitForOnDefaultScreen";
    showPauseBefore = 2;
    assert prepareDefaultScreen("shot", imageNameDefault);
    if (isHeadless) {
      return;
    }
    int waitTime = (int) SX.getOptionNumber("Settings.AutoWaitTimeout", 5);
    match = Do.wait(img, waitTime);
    theShow.stop();
    result = end() + match.toString();
    assert match.isValid();
    Do.on().showMatch();
  }

  @Test
  public void test_55_waitVanishOnDefaultScreen() {
    currentTest = "test_55_waitVanishOnDefaultScreen";
    showPauseAfter = 3;
    assert prepareDefaultScreen("shot", imageNameDefault);
    if (isHeadless) {
      return;
    }
    int waitTime = 5; //(int) SX.getOptionNumber("Settings.AutoWaitTimeout", 3);
    boolean vanished = Do.waitVanish(img, waitTime);
    result = end() + "vanished: " + vanished + " " + SX.getMain().getLastVanish();
    assert vanished && SX.getMain().hasVanish() && !SX.getMain().hasMatch();
    Do.on().showVanish();
  }

  @Test
  public void test_56_existsOnDefaultScreen() {
    currentTest = "test_56_existsOnDefaultScreen";
    assert prepareDefaultScreen("shot", imageNameDefault);
    if (isHeadless) {
      return;
    }
    boolean isThere = Do.exists(img);
    theShow.stop();
    result = end() + "exists: " + Do.getLastMatch().toString();
    assert isThere;
    Do.on().showMatch();
  }

  @Test
  public void test_57_saveCapturePartOfDefaultScreen() {
    currentTest = "test_57_saveCapturePartOfDefaultScreen";
    assert prepareDefaultScreen(imageNameDefault);
    if (isHeadless) {
      return;
    }
    start();
    elemDisplayed.grow(20);
    elemDisplayed.load();
    result = end() + elemDisplayed.toString();
    theShow.stop();
    if (elemDisplayed.hasContent()) {
      elemDisplayed.show();
      elemDisplayed.save("test_57_saveCapturePartOfDefaultScreen");
      return;
    }
    assert false;
  }

  @Test
  public void test_58_basicStory() {
    currentTest = "test_58_basicStory";
    result = "showing some grafics";
    if (!SX.isHeadless()) {
      Story story = new Story();
      story.add(Symbol.circle(700).setColor(Color.black).setLine(20).fill(Color.yellow));
      story.add(Symbol.rectangle(500, 300).setColor(Color.red).fill(Color.cyan).setLine(10));
      story.add(Symbol.ellipse(300, 100).setColor(Color.blue).setLine(8).fill());
      story.add(Symbol.square(50).setColor(Color.yellow));
      story.show(5);
    } else {
      result = "headless: not testing";
    }
    assert true;
  }

  @Test
  public void test_70_handlingWhatImageNotOnImagePath() {
    currentTest = "test_70_handlingWhatImageNotOnImagePath";
    result = "what image not found on imagepath";
    String givenWhat = "noimagewhat";
    Element missing = Do.find(givenWhat);
    assert missing.getName().equals(givenWhat) && !missing.hasContent();
  }

  @Test
  public void test_71_handlingWhereImageNotOnImagePath() {
    currentTest = "test_71_handlingWhereImageNotOnImagePath";
    result = "where image not found on imagepath";
    boolean success = Do.setBundlePath(mavenRoot, "Images");
    String givenWhat = imageNameDefault;
    String givenWhere = "noimagewhere";
    Element missing = Do.find(givenWhat, givenWhere);
    assert missing.getName().equals(givenWhere) && !missing.hasContent();
  }

  @Test
  public void test_90_nativeHook() {
    currentTest = "test_990_nativeHook";
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
  public void test_91_popat() {
    currentTest = "test_991_popat";
    boolean assertVal = true;
    if (!SX.onTravisCI() && log.isGlobalLevel(log.TRACE)) {
      Do.popat(300, 300);
      Do.popup("Use mouse to click OK", "testing popat");
      Element loc = Element.at();
      result = String.format("clicked at (%d, %d)", loc.x, loc.y);
      assertVal = loc.x > 300 && loc.x < 450;
    } else {
      result = "TravisCI or NonInteractive: not testing";
    }
    assert assertVal;
  }

  @Test
  public void test_92_oldAPI_Screen() {
    currentTest = "test_92_oldAPI_Screen";
    Screen.showMonitors();
    new Screen(1).show();
    result = new Screen(1).toString();
  }

  @Test
  public void test_80_basicsObserve() {
    currentTest = "test_80_basicsObserve";
    result = "basic observe features";
    boolean success = Do.setBundlePath(mavenRoot, "Images");
    Events.shouldProcessEvents(false);
    Element where = Do.on();
    Element what = new Picture(imageNameDefault);
    Event evt = where.onAppear(what);
    log.trace("Event added: %s", evt);
    evt = where.onChange();
    log.trace("Event added: %s", evt);
    evt = where.onVanish(imageNameDefault);
    log.trace("Event added: %s", evt);
    where.observe();
    int nEvents = Events.getEventCount(where);
    assert nEvents == 3;
    assert Events.hasEvent(what, where);
    assert SX.isNotNull(Events.getEvent(what, where));
    Events.stopObserving();
    assert where.hasEvents();
    for (int i = 0; i < nEvents; i++) {
      assert SX.isNotNull(where.nextEvent());
    }
    assert !where.hasEvents();
    assert SX.isNull(where.nextEvent());
  }

  @Test
  public void test_81_ObserveOnAppearWithHandler() {
    currentTest = "test_81_ObserveOnAppearWithHandler";
    assert prepareDefaultScreen("shot", imageNameDefault);
    result = "observe onAppear with handler";
    Element where = Do.on();
    Element what = new Picture(imageNameDefault);
    where.onAppear(what, new Handler() {
      public void run(Event event) {
        log.trace("Event handler for %s", event);
        showEvent(event);
        assert event.hasMatch();
        event.getWhere().showMatch(1);
        if (event.getCount() < 2) {
          event.repeat(3);
        }
      }
    });
    where.observe();
    Events.waitUntilFinished();
    theShow.stop();
  }

  @Test
  public void test_82_ObserveOnVanishWithHandler() {
    currentTest = "test_82_ObserveOnVanishWithHandler";
    showPauseAfter = 2;
    assert prepareDefaultScreen("shot", imageNameDefault);
    result = "observe onVanish with handler";
    Element where = Do.on();
    Element what = new Picture(imageNameDefault);
    where.onVanish(what, new Handler() {
      public void run(Event event) {
        log.trace("Event handler for %s", event);
        showEvent(event);
        assert event.hasVanish();
        event.getWhere().showVanish(1);
      }
    });
    where.observe();
    Events.waitUntilFinished();
    theShow.stop();
  }

  @Test
  public void test_90_edgeDetectionBasic() {
    currentTest = "test_90_edgeDetectionBasic";
    result = "basic edge detection sample";
    Do.setBundlePath(mavenRoot, "Images");
    Picture pBase, pEdges;
    pBase = new Picture("gui");
    pEdges = Finder.detectEdges(pBase);
    pBase.show();
    pEdges.show();
    pBase = new Picture("gui-button");
    pEdges = Finder.detectEdges(pBase);
    pBase.show();
    pEdges.show();
  }

  @Test
  public void test_91_changeDetectionBasic() {
    currentTest = "test_91_changeDetectionBasic";
    result = "basic change detection sample";
    Do.setBundlePath(mavenRoot, "Images");
    Picture pBase, pChanged;
    pBase = new Picture("gui-button");
    pChanged = new Picture("gui-button-blank");
    pBase.show();
    pChanged.show();
    SX.setOption("highLightLine", "1");
    List<Element> rectangles = Finder.detectChanges(pBase.getContent(), pChanged.getContent());
    assert rectangles.size() == 3;
    Story changed = new Story(pBase);
    for (Element rect : rectangles) {
      changed.add(rect);
    }
    changed.addBorder();
    changed.show(3);
  }
  //</editor-fold>

  //<editor-fold desc="ignored">
  //</editor-fold>

  //log.startTimer();
  @Test
  public void test_999_someThingToTest() {
    //log.startTimer();
    currentTest = "test_999_someThingToTest";
    if (!SX.onTravisCI() && log.isGlobalLevel(log.TRACE)) {
      if (!SX.isHeadless()) {
// ******************* start
        result = "nothing to do here";
// ******************* end
      } else {
        result = "headless: not testing";
      }
    } else {
      result = "TravisCI or NonInteractive: not testing";
    }
    assert true;
  }
}
