/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.test;

import com.sikulix.api.*;
import com.sikulix.api.Event;
import com.sikulix.core.*;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.opencv.core.Mat;
import org.sikuli.script.Location;
import org.sikuli.script.Region;
import org.sikuli.script.Screen;

import javax.swing.*;
import java.awt.*;
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
    button = Symbol.button(200, 80).setColor(Color.gray).fill(Color.lightGray).setLine(4);
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
    if (SX.isSetSXLOCALDEVICE()) {
      Do.on().removeEvents();
    }
    if (SX.isNotNull(theShow)) {
      theShow.stop();
      theShow.waitForEnd();
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
    start();
    Picture base = new Picture(imageNameDefault);
    success &= base.isValid();
    Picture img = new Picture(base);
    Element match = null;
    if (success) {
      base.show(1);
      match = Do.find(img, base);
      success &= match.isMatch() && 0.99 < match.getScore() &&
              0 == match.x && 0 == match.y &&
              match.w == (int) base.w && match.h == (int) base.h;
    }
    if (success) {
      result = match.toString();
      base.showMatch(1);
    }
    result = end() + result;
    assert success;
  }

  @Test
  public void test_042_findImageInOtherImage() {
    currentTest = "test_042_findImageInOtherImage";
    boolean success = Do.setBundlePath(mavenRoot, "Images");
    result = "Not Found";
    start();
    Picture target = new Picture(imageNameDefault);
    success &= target.isValid();
    Picture base = new Picture("shot-tile");
    success &= base.isValid();
    Element element = null;
    if (success) {
      base.show(1);
      element = Do.find(target, base);
      success &= element.isMatch();
    }
    if (success) {
      result = element.toString();
    }
    result = end() + result;
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
  public void test_057_saveCapturePartOfDefaultScreen() {
    currentTest = "test_057_saveCapturePartOfDefaultScreen";
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
  public void test_058_basicStory() {
    currentTest = "test_058_basicStory";
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
  public void test_080_basicsObserve() {
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
    pEdges = Finder.detectEdges(pBase);
    pBase.show();
    pEdges.show();
    pBase = new Picture("gui-button");
    pEdges = Finder.detectEdges(pBase);
    pBase.show();
    pEdges.show();
  }

  @Test
  public void test_091_changeDetectionBasic() {
    currentTest = "test_091_changeDetectionBasic";
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
  public void test_130_toJSONbasic() {
    currentTest = "test_130_toJSONbasic";
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
    log.p("asJSON: %s", scr.getScreen().toJSON());
    Element eScr = new Element(scr.toJSON());
  }

  @Test
  public void test_200_popat() {
    currentTest = "test_200_popat";
    boolean assertVal = true;
    if (!SX.onTravisCI() && log.isGlobalLevel(log.TRACE)) {
      Do.popat(300, 300);
      Do.popup("Use mouse to click OK", "testing popat");
      Element loc = Do.at();
      result = String.format("clicked at (%d, %d)", loc.x, loc.y);
      assertVal = loc.x > 300 && loc.x < 450;
    } else {
      result = "TravisCI or NonInteractive: not testing";
    }
    assert assertVal;
  }

  @Test
  public void test_300_oldAPI_Basic() {
    currentTest = "test_300_oldAPI_Basic";
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

  @Test
  public void test_300_oldAPI_Region() {
    currentTest = "test_300_oldAPI_Region";
    result = "old API: Region";
    Region region = new Region(100, 100, 100, 100);
    assert region.isValid() && region instanceof Region : "new Region(100, 100, 100, 100)";
    region = Region.create(100, 100, 100, 100);
    assert region.isValid() && region instanceof Region : "Region.create(100, 100, 100, 100)";
    region = Region.create(-200, -200, 100, 100);
    assert !region.isValid() && region instanceof Region : "Region.create(-200, -200, 100, 100)";
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
