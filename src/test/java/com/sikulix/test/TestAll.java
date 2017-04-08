/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.test;

import com.sikulix.api.*;
import com.sikulix.core.*;
import com.sikulix.remote.vnc.VNCDevice;
import com.sikulix.run.Runner;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.opencv.core.*;
import org.sikuli.script.Location;
import org.sikuli.script.Region;
import org.sikuli.script.Screen;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestAll {

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

  public TestAll() {
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
    SX.setOption("SX.withHook", "no");
  }

  @AfterClass
  public static void tearDownClass() {
    if (SX.isNotNull(hook)) {
      hook.stop();
      log.info("hook stopped");
    }
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
    if (SX.isOption("SX.withHook") && SX.isNull(hook)) {
      hook = Do.getHook();
      log.info("hook started");
    }
    button = Symbol.button(200, 80).setColor(Color.gray).fill(Color.lightGray).setLine(4);
    Do.setBundlePath(mavenRoot, "Images");
  }

  @After
  public void tearDown() {
    if (log.isLevel(SXLog.TRACE)) {
      log.stopTimer();
    }
    Picture.clearPath();
    resetDefaultScreen();
    Events.waitUntilFinished();
    Events.reset();
    if (SX.isSetLOCALDEVICE()) {
      Do.on().removeEvents();
      Do.on().resetMatches();
    }
    if (SX.isNotNull(theShow)) {
      theShow.stop();
      theShow.waitForEnd();
    }
    SX.setOption("Settings.MoveMouseDelay", "0.5");
    if (SX.isNull(result)) {
      result = currentTest;
    }
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
  public void test_010_startup() {
    currentTest = "test_010_startup_workdir";
    String workDir = SX.getUSERWORK();
    if (SX.isNotSet(workDir)) {
      SX.show();
    }
    result = workDir;
    assert SX.isSet(workDir);
  }

  @Test
  public void test_011_startup_native_load() {
    currentTest = "test_011_startup_native_load";
    Element.getNewMat();
    File test = SX.getFileExists(SX.getSXNATIVE(), SX.sxLibsCheckName);
    result = SX.sxLibsCheckName;
    assert !SX.isNull(test) && SX.existsFile(test);
  }

  @Test
  public void test_020_getBundlePath() {
    currentTest = "test_020_getBundlePath";
    String bundlePath = Do.getBundlePath();
    result = bundlePath;
    assert SX.existsFile(bundlePath);
  }

  @Test
  public void test_021_setBundlePathFile() {
    currentTest = "test_021_setBundlePathFile";
    boolean success = Do.setBundlePath(mavenRoot, defaultImagePath);
    result = Do.getBundlePath();
    success &= SX.existsFile(result);
    assert success;
  }

  @Test
  public void test_022_setBundlePathByClass() {
    currentTest = "test_022_setBundlePathByClass";
    boolean success = Do.setBundlePath(jarImagePathDefault);
    result = Do.getBundlePath();
    success &= SX.existsFile(result);
    assert success;
  }

  @Test
  public void test_023_setBundlePathJarByClass() {
    currentTest = "test_023_setBundlePathJarByClass";
    boolean success = Do.setBundlePath(jarImagePathClass);
    result = Do.getBundlePath();
    success &= SX.existsFile(result);
    assert success;
  }

  @Test
  public void test_024_setBundlePathHttp() {
    currentTest = "test_024_setBundlePathHttp";
    boolean success = Do.setBundlePath(httpRoot, defaultImagePath);
    result = Do.getBundlePath();
    success &= (httpImagePath).equals(result);
    assert success;
  }

  @Test
  public void test_029_getImagePath() {
    currentTest = "test_029_getImagePath";
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
  public void test_030_elementConstructors() {
    currentTest = "test_030_elementConstructors";
    Element elem = new Element();
    result = "Element();";
    assert Element.eType.ELEMENT.equals(elem.getType());
    Picture img = new Picture();
    result += " Picture();";
    assert Element.eType.PICTURE.equals(img.getType());
    Target tgt = new Target();
    result += " Target();";
    assert Element.eType.TARGET.equals(tgt.getType());
    tgt = new Target(img);
    result += " Target(image);";
    assert Element.eType.TARGET.equals(tgt.getType());
    tgt = new Target(tgt);
    result += " Target(target);";
    assert Element.eType.TARGET.equals(tgt.getType());
    Mat aMat = tgt.getContent();
    tgt = new Target(aMat);
    result += " Target(mat);";
    assert Element.eType.TARGET.equals(tgt.getType());
    tgt = new Target(img, 0.95, new Element(2, 3));
    result += " Target(image, 0.95, new Element(2,3));";
    assert Element.eType.TARGET.equals(tgt.getType());
  }

  @Test
  public void test_0031_loadImageFromFile() {
    currentTest = "test_0031_loadImageFromFile";
    boolean success = Do.setBundlePath(mavenRoot, "Images");
    result = "BundlePath: " + Do.getBundlePath();
    Picture img = new Picture(imageNameDefault);
    success &= img.isValid();
    if (success) {
      result = set("(%s) Image %s from " + img.getURL(), img.getTimeToLoad(), img.getName());
      if (log.isGlobalLevel(SXLog.TRACE)) {
        img.show();
      }
    }
    assert success;
  }

  @Test
  public void test_0032_loadImageFromJarByClass() {
    currentTest = "test_0032_loadImageFromJarByClass";
    boolean success = Do.setBundlePath(jarImagePathClass);
    result = "BundlePath: " + Do.getBundlePath();
    Picture img = new Picture(imageNameDefault);
    success &= img.isValid();
    if (success) {
      result = set("(%s) Image %s from " + img.getURL(), img.getTimeToLoad(), img.getName());
      if (log.isGlobalLevel(SXLog.TRACE)) {
        img.show();
      }
    }
    assert success;
  }

  @Test
  public void test_033_loadImageFromHttp() {
    currentTest = "test_033_loadImageFromHttp";
    boolean success = Do.setBundlePath(httpRoot, "master");
    result = "BundlePath: " + Do.getBundlePath();
    Picture img = new Picture(imageNameDefault);
    success &= img.isValid();
    if (success) {
      result = set("(%s) Image %s from " + img.getURL(), img.getTimeToLoad(), img.getName());
      if (log.isGlobalLevel(SXLog.TRACE)) {
        img.show();
      }
    }
    assert success;
  }

  @Test
  public void test_040_createFinderFromImage() {
    currentTest = "test_040_createFinderFromImage";
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
  public void test_041_findImageInSameImage() {
    currentTest = "test_041_findImageInSameImage";
    boolean success = Do.setBundlePath(mavenRoot, "Images");
    result = "Not Found";
    Picture base = new Picture(imageNameDefault);
    success &= base.isValid();
    Picture img = new Picture(base);
    Element match = null;
    String tEnd = "";
    if (success) {
      base.show(1);
      start();
      match = Do.find(img, base);
      tEnd = end();
      success &= match.isMatch() && 0.99 < match.getScore() &&
              0 == match.x && 0 == match.y &&
              match.w == (int) base.w && match.h == (int) base.h;
    }
    if (success) {
      result = match.toString();
      base.showMatch(1);
    }
    result = tEnd + result;
    assert success;
  }

  @Test
  public void test_042_findImageInOtherImage() {
    currentTest = "test_042_findImageInOtherImage";
    boolean success = Do.setBundlePath(mavenRoot, "Images");
    result = "Not Found";
    start();
    String tEnd = "";
    Picture target = new Picture(imageNameDefault);
    success &= target.isValid();
    Picture base = new Picture("shot-tile");
    success &= base.isValid();
    Element element = null;
    if (success) {
      base.show(1);
      start();
      element = Do.find(target, base);
      tEnd = end();
      success &= element.isMatch();
    }
    if (success) {
      result = element.toString();
    }
    result = tEnd + result;
    if (success) {
      base.showMatch(2);
    }
    assert success;
  }

  @Test
  public void test_043_findAllInImage() {
    currentTest = "test_043_findAllInImage";
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
      base.show(1);
      elements = Do.findAll(target, base);
      success &= elements.size() == expected;
    }
    result = String.format("#%d in (%dx%d) %% %.2f +- %.4f]", elements.size(),
            base.w, base.h, 100 * base.getLastScores()[0], 100 * base.getLastScores()[2]);
    result = end() + result;
    if (success) {
      base.showMatches(2);
    }
    assert success;
  }

  @Test
  public void test_44_findBestInImage() {
    currentTest = "test_44_findBestInImage";
    Picture base = new Picture("shot-tile");
    List<Object> pictures = new ArrayList<>();
    pictures.add("game-button");
    pictures.add(new Picture(imageNameDefault));
    pictures.add(new Picture(imageNameDefault));
    pictures.add(imageNameDefault);
    start();
    String tEnd = "";
    Element best = Do.findBest(pictures, base);
    tEnd = end();
    assert best.isMatch() : "not found";
    best.setName("best");
    result = String.format("%s", best);
    result = tEnd + result;
  }

  @Test
  public void test_45_findAnyInImage() {
    currentTest = "test_45_findAnyInImage";
    Picture base = new Picture("shot-tile");
    List<Object> pictures = new ArrayList<>();
    pictures.add("game-button");
    pictures.add(new Picture(imageNameDefault));
    pictures.add(new Picture(imageNameDefault));
    pictures.add(imageNameDefault);
    start();
    String tEnd = "";
    List<Element> matches = Do.findAny(pictures, base);
    tEnd = end();
    assert matches.size() == 4 : "invalid result";
    assert !matches.get(0).isMatch() : "first should not be found";
    assert matches.get(1).isMatch() : "second should be found";
    assert matches.get(2).isMatch() : "third should be found";
    assert matches.get(3).isMatch() : "forth should be found";
    result = tEnd + String.format("%d images", matches.size());
  }

  @Test
  public void test_050_captureDefaultScreen() {
    currentTest = "test_050_captureDefaultScreen";
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
  public void test_051_capturePartOfDefaultScreen() {
    currentTest = "test_051_capturePartOfDefaultScreen";
    assert prepareDefaultScreen(imageNameDefault);
    if (isHeadless) {
      return;
    }
    start();
    elemDisplayed = elemDisplayed.grow(20);
    Picture img = Do.capture(elemDisplayed);
    result = end() + img.toString();
    Do.wait(1.0);
    if (img.hasContent()) {
      img.show();
      return;
    }
    assert false;
  }

  @Test
  public void test_052_findInDefaultScreen() {
    currentTest = "test_052_findInDefaultScreen";
    assert prepareDefaultScreen("shot", imageNameDefault);
    if (isHeadless) {
      return;
    }
    match = Do.find(img);
    result = end() + match.toString();
    assert match.isValid();
    Do.on().showMatch(2);
  }

  @Test
  public void test_053_findAllInDefaultScreen() {
    log.startTimer();
    currentTest = "test_053_findAllInDefaultScreen";
    assert prepareDefaultScreen("shot-tile", imageNameDefault);
    if (isHeadless) {
      return;
    }
    int expected = (int) (base.w / 200) * (int) (base.h / 200);
    Do.wait(1.0);
    matches = Do.findAll(img);
    assert matches.size() == expected;
    result = String.format("#%d in (%dx%d) %% %.2f +- %.4f]", matches.size(),
            base.w, base.h, 100 * Do.onMain().getLastScores()[0], 100 * Do.onMain().getLastScores()[2]);
    result = end() + result;
    Do.showMatches();
  }

  @Test
  public void test_054_waitForOnDefaultScreen() {
    currentTest = "test_054_waitForOnDefaultScreen";
    showPauseBefore = 1;
    showPauseAfter = 2;
    assert prepareDefaultScreen("shot", imageNameDefault);
    if (isHeadless) {
      return;
    }
    int waitTime = (int) SX.getOptionNumber("Settings.AutoWaitTimeout", 3);
    Do.wait(img, waitTime);
    match = Do.getLastMatch();
    result = end() + match.toString();
    assert match.isValid();
    Do.showMatch();
  }

  @Test
  public void test_055_waitVanishOnDefaultScreen() {
    currentTest = "test_055_waitVanishOnDefaultScreen";
    showPauseAfter = 3;
    assert prepareDefaultScreen("shot", imageNameDefault);
    if (isHeadless) {
      return;
    }
    int waitTime = 5; //(int) SX.getOptionNumber("Settings.AutoWaitTimeout", 3);
    boolean vanished = Do.waitVanish(img, waitTime);
    result = end() + "vanished: " + vanished + " " + Do.onMain().getLastVanish();
    assert vanished && Do.onMain().hasVanish() && !Do.onMain().hasMatch();
    Do.showVanish();
  }

  @Test
  public void test_056_existsOnDefaultScreen() {
    currentTest = "test_056_existsOnDefaultScreen";
    assert prepareDefaultScreen("shot", imageNameDefault);
    if (isHeadless) {
      return;
    }
    boolean isThere = Do.exists(img);
    result = end() + "exists: " + Do.getLastMatch().toString();
    assert isThere;
    Do.showMatch();
  }

  @Test
  public void test_058_basicFindWithMask() {
    currentTest = "test_058_basicFindWithMask";
    if (!SX.isHeadless()) {
      Picture target = new Picture("sikulix2_center_small");
      Picture base = new Picture("shot-tile");
      base.show(1);
      Do.find(target, base);
      if (base.hasMatch()) {
        base.showMatch(2);
      } else {
        assert false : "image center_small not found";
      }
      target = new Picture("sikulix2_frame_filled");
      base.reset();
      Do.find(target, base);
      assert !base.hasMatch() : "image frame_filled should not be found";
      target = new Picture("sikulix2_frame");
      base.reset();
      Do.find(target, base);
      assert base.hasMatch() : "find: image frame not found";
      base.showMatch(2);
      base.reset();
      Do.findAll(target, base);
      assert base.hasMatches() : "findAll: image frame not found";
      base.showMatches(2);
    } else {
      result = "headless: not testing";
    }
  }

  @Test
  public void test_060_basicStory() {
    currentTest = "test_060_basicStory";
    result = "showing some grafics";
    if (!SX.isHeadless()) {
      Story story = new Story(0);
      story.add(Symbol.circle(700).setColor(Color.black).setLine(20).fill(Color.yellow));
      story.add(Symbol.rectangle(500, 300).setColor(Color.red).fill(Color.cyan).setLine(10));
      story.add(Symbol.ellipse(300, 100).setColor(Color.blue).setLine(8).fill());
      story.add(Symbol.square(200).setColor(Color.yellow).setLine(20), new Element(100, 100));
      story.show(5);
    } else {
      result = "headless: not testing";
    }
    assert true;
  }

  @Test
  public void test_070_handlingWhatImageNotOnImagePath() {
    currentTest = "test_070_handlingWhatImageNotOnImagePath";
    result = "what image not found on imagepath";
    String givenWhat = "noimagewhat";
    Element missing = Do.find(givenWhat);
    assert missing.getName().equals(givenWhat) && !missing.hasContent();
  }

  @Test
  public void test_071_handlingWhereImageNotOnImagePath() {
    currentTest = "test_071_handlingWhereImageNotOnImagePath";
    result = "where image not found on imagepath";
    boolean success = Do.setBundlePath(mavenRoot, "Images");
    String givenWhat = imageNameDefault;
    String givenWhere = "noimagewhere";
    Element missing = Do.find(givenWhat, givenWhere);
    assert missing.getName().equals(givenWhere) && !missing.hasContent();
  }

  @Test
  public void test_072_saveCapturePartOfDefaultScreen() {
    currentTest = "test_072_saveCapturePartOfDefaultScreen";
    assert prepareDefaultScreen(imageNameDefault);
    if (isHeadless) {
      return;
    }
    start();
    elemDisplayed = elemDisplayed.grow(20);
    elemDisplayed.load();
    result = end() + elemDisplayed.toString();
    if (elemDisplayed.hasContent()) {
      elemDisplayed.show();
      elemDisplayed.save("test_057_saveCapturePartOfDefaultScreen");
      return;
    }
    assert false;
  }

  @Test
  public void test_080_basicsObserve() {
    log.startTimer();
    currentTest = "test_080_basicsObserve";
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
  public void test_081_ObserveOnAppearWithHandler() {
    currentTest = "test_081_ObserveOnAppearWithHandler";
    showPauseAfter = 0;
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
  }

  @Test
  public void test_082_ObserveOnVanishWithHandler() {
    currentTest = "test_082_ObserveOnVanishWithHandler";
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
  }

  @Test
  public void test_090_edgeDetectionBasic() {
    currentTest = "test_090_edgeDetectionBasic";
    result = "basic edge detection sample";
    Do.setBundlePath(mavenRoot, "Images");
    Picture pBase, pEdges;
    pBase = new Picture("gui");
    pEdges = Finder.showEdges(pBase);
    pBase.show(1);
    pEdges.show(2);
    pBase = new Picture("gui-button");
    pEdges = Finder.showEdges(pBase);
    pBase.show(1);
    pEdges.show(2);
  }

  @Test
  public void test_091_edgeDetectionSegments() {
    currentTest = "test_091_edgeDetectionSegments";
    result = "segmenting a GUI into distinct elements";
    Do.setBundlePath(mavenRoot, "Images");
    Picture pBase = new Picture("gui");
    Story segmented = new Story(pBase);
    for (Element rect : Finder.getElements(pBase)) {
      segmented.add(rect);
    }
    segmented.addBorder();
    segmented.show();
  }

  @Test
  public void test_095_changeDetectionBasic() {
    currentTest = "test_095_changeDetectionBasic";
    result = "basic change detection sample";
    Do.setBundlePath(mavenRoot, "Images");
    Picture pBase, pChanged;
    pBase = new Picture("gui-button");
    pChanged = new Picture("gui-button-blank");
    pBase.show(2);
    //pChanged.show();
    SX.setOption("highLightLine", "1");
    List<Element> rectangles = Finder.detectChanges(pBase.getContent(), pChanged.getContent());
    assert rectangles.size() == 3;
    Story changed = new Story(pBase);
    for (Element rect : rectangles) {
      changed.add(rect);
    }
    changed.addBorder();
    changed.show(2);
  }

  @Test
  public void test_101_mouseHoverWithHookCheck() {
    currentTest = "test_101_mouseHoverWithHookCheck";
    if (!SX.isHeadless()) {
      result = "some mouse moves checked with NativeHook";
      SX.pause(1);
      Element elem = new Element(100, 100);
      Do.hover();
      Element center = Do.on().getCenter();
      assert Do.isMouseposition(hook, center.x, center.y);
      elem.hover();
      assert Do.isMouseposition(hook, elem.x, elem.y);
      Do.hover();
      assert Do.isMouseposition(hook, center.x, center.y);
      elem.hover(elem);
      assert Do.isMouseposition(hook, elem.x, elem.y);
      Do.hover();
      assert Do.isMouseposition(hook, center.x, center.y);
      Do.hover(elem);
      assert Do.isMouseposition(hook, elem.x, elem.y);
    }
    assert true;
  }

  @Test
  public void test_110_mouseClick() {
    currentTest = "test_110_mouseClick";
    if (!SX.isHeadless()) {
      result = "mouse click story button";
      boolean success = false;
      Story story = new Story(button).start();
      SX.pause(2);
      Picture button = story.whereShowing().capture();
      Do.find(button);
      Do.on().showMatch(2);
      Do.click();
      story.waitForEnd();
      assert !story.isRunning() : "story still running";
      assert !Do.exists(button, 0) : "button still visible";
      if (story.hasClickedSymbol()) {
        log.p("*** Story: clicked: %s", story.getClickedSymbol());
        success = true;
      }
      assert success : "story does not have clicked symbol";
    }
  }

  @Test
  public void test_111_mouseDragDrop() {
    currentTest = "test_111_mouseDragDrop";
    if (!SX.isHeadless()) {
      result = "mouse drag drop";
      SX.setOption("Settings.MoveMouseDelay", "0.5");
      for (int i = 0; i < 3; i++) {
        Story story = new Story(button).start();
        int xOff = (int) (Do.on().w / 3);
        int yOff = (int) (Do.on().h / 3);
        Element drag = null;
        Element drop = null;
        Element button = story.whereShowing();
        Picture pButton = button.capture();
        drag = new Element(button);
        drop = new Element(button, xOff, yOff);
        Do.dragDrop(button, drop);
        Element mButton = Do.find(pButton);
        String assertMsg = String.format("mouse (%d,%d) after dragDrop not in mButton: %s",
                Do.at().x, Do.at().y, mButton);
        assert (mButton.isMatch() && mButton.contains(Do.at())) : assertMsg;
        Do.at().click();
        story.stop();
        Do.wait(1.0);
        assert (!Do.exists(pButton, 0)) : "Story still visible after action";
        if (story.hasClickedSymbol()) {
          Symbol clickedSymbol = story.getClickedSymbol();
          log.p("dragDrop from: %s to %s clicked %s", drag.logString(), drop.logString(), clickedSymbol);
          assert (mButton.contains(clickedSymbol)) : "dragDrop not exact";
        } else {
          assert false : "no valid action";
        }
      }
    }
  }

  @Test
  public void test_190_toJSONbasic() {
    currentTest = "test_190_toJSONbasic";
    assert prepareDefaultScreen("shot", imageNameDefault);
    if (isHeadless) {
      return;
    }
    match = Do.find(img);
    result = end() + match.toString();
    assert match.isValid();
    result = "basic toJSON() calls";
    log.p("String: %s", Do.on());
    log.p("asJSON: %s", Do.on().toJSON());
    log.p("String: %s", match);
    log.p("asJSON: %s", match.toJSON());
    log.p("String: %s", img);
    log.p("asJSON: %s", img.toJSON());
    Region scr = new Region(Do.on());
    log.p("String: %s", scr);
    log.p("asJSON: %s", scr.toJSON());
    log.p("String: %s", scr.getCenter());
    log.p("asJSON: %s", scr.getCenter().toJSON());
    log.p("String: %s", scr.getScreen());
    log.p("asJSON: %s", scr.getScreen().asElement().toJSON());
    Element eScr = new Element(scr.toJSON());
  }

  @Test
  public void test_200_popup() {
    currentTest = "test_200_popup";
    result = "popup, popAsk, popError, input, input hidden";
    Boolean returnBool;
    Element loc = new Element(300, 300);
    returnBool = Do.popup("click OK", "popup autoclose", 2, loc);
    if (!SX.onTravisCI()) {
      assert SX.isNull(returnBool) : "popup: return not null";
    }
    returnBool = Do.popAsk("click No or Yes", "popAsk autoclose", 2, loc);
    if (!SX.onTravisCI()) {
      assert SX.isNull(returnBool) : "popAsk: return not null";
    }
    returnBool = Do.popError("click OK", "popError autoclose", 2, loc);
    if (!SX.onTravisCI()) {
      assert SX.isNull(returnBool) : "popError: return not null";
    }
    String returnString;
    returnString = Do.input("give me some text", "text input autoclose", "preset", 2, loc);
    if (!SX.onTravisCI()) {
      assert SX.isNull(returnString) : "input: return not null";
    }
    returnString = Do.input("enter password", "hidden input autoclose", "preset", true, 2, loc);
    if (!SX.onTravisCI()) {
      assert SX.isNull(returnString) : "input hidden: return not null";
    }
  }

  @Test
  public void test_300_oldAPI_Basic() {
    currentTest = "test_300_oldAPI_Basic";
    if (!SX.isHeadless()) {
      Screen.showMonitors();
      Screen scr = new Screen();
      assert scr.getID() == 0;
      assert SX.isRectangleEqual(scr, Do.on().getRectangle());
      scr.hover();
      Location center = scr.getCenter();
      assert Do.isMouseposition(hook, center.x, center.y);
      Element grow = center.grow(scr.w / 3, scr.h / 3);
      grow.show(3);
      result = "Screen basics: " + scr.toString();
      if (Do.getDevice().getNumberOfMonitors() > 1) {
        scr = new Screen(1);
        scr.hover();
        center = scr.getCenter();
        assert Do.isMouseposition(hook, center.x, center.y);
        grow = center.grow(scr.w / 3, scr.h / 3);
        grow.show(3);
        result += " with second monitor";
      }
    }
  }

  @Test
  public void test_300_oldAPI_Region() {
    currentTest = "test_300_oldAPI_Region";
    if (!SX.isHeadless()) {
      result = "old API: Region";
      Region region = new Region(100, 100, 100, 100);
      assert region.isValid() && region instanceof Region : "new Region(100, 100, 100, 100)";
      region = Region.create(100, 100, 100, 100);
      assert region.isValid() && region instanceof Region : "Region.create(100, 100, 100, 100)";
      region = Region.create(-200, -200, 100, 100);
      assert !region.isValid() && region instanceof Region : "Region.create(-200, -200, 100, 100)";
    }
  }

  @Test
  public void test_501_runJavaScriptBasic() {
    currentTest = "test_501_runJavaScriptBasic";
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
    currentTest = "test_502_runJavaScriptFromJar";
    if (!SX.isHeadless()) {
      result = "running JavaScript from jar: mouse moves to center";
      Runner.run("basic");
      Element center = Do.on().getCenter();
      assert Do.isMouseposition(hook, center.x, center.y) : "mouse should be at center of screen";
    }
  }

  @Test
  public void test_503_runJavaScriptFromNet() {
    currentTest = "test_503_runJavaScriptFromNet";
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
    currentTest = "test_504_runJavaScriptWithFind";
    if (!SX.isHeadless()) {
      result = "running JavaScript: find image on screen";
      Do.setBundlePath(mavenRoot, "Images");
      Picture picture = new Picture("shot-tile");
      assert picture.isValid() : "Image: shot-tile not valid";
      Story story = new Story(picture, 3).start();
      Runner.run("basic1");
      story.waitForEnd();
    }
  }

  //TODO test for write()

  @Test
  public void test_800_basicTesseract() {
    currentTest = "test_800_basicTesseract";
    if (!SX.onTravisCI()) {
      TextFinder textFinder = new TextFinder();
      assert textFinder.isValid() : "TextFinder not valid";
      String textRead = textFinder.read(new Picture("gui-button"));
      assert !"did not work".equals(textRead) : "read did not work";
    }
  }
  //</editor-fold>

  //<editor-fold desc="ignored">
  @Ignore
  public void test_601_basicVNC() {
    currentTest = "test_601_basicVNC";
    if (!SX.isHeadless() && !SX.onTravisCI()) {
      result = "capture something on a VNCScreen";
      IDevice vnc = new VNCDevice();
      vnc.start("192.168.2.24", 5900);
      Element area = new Element(100, 100, 300, 300);
      Picture picture;
      for (int n = 0; n < 3; n++) {
        start();
        picture = vnc.capture();
        log.p("time: %s", end());
        picture.show(2);
      }
      SX.pause(1);
      vnc.stop();
    }
  }
  //</editor-fold>

  //log.startTimer();
  @Test
  public void test_999_someThingToTest() {
    //log.startTimer();
    currentTest = "test_0999_someThingToTest";
    if (!SX.onTravisCI() && log.isGlobalLevel(log.TRACE)) {
      if (!SX.isHeadless()) {
// start
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

