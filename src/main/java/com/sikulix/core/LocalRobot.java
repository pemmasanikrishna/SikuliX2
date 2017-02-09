/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */
package com.sikulix.core;

import com.sikulix.api.Element;
import com.sikulix.api.Picture;

import java.awt.Robot;
import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class LocalRobot extends Robot implements IRobot {

  private static SXLog log = SX.getLogger("SX.LocalRobot");

  //<editor-fold desc="housekeeping">
  final static int MAX_DELAY = 60000;
  private static int heldButtons = 0;
  private static String heldKeys = "";
  private static final ArrayList<Integer> heldKeyCodes = new ArrayList<Integer>();
  public static int stdAutoDelay = 0;
  public static int stdDelay = 10;

  public LocalRobot() throws AWTException {
    super();
    setAutoDelay(stdAutoDelay);
    setAutoWaitForIdle(false);
  }
  //</editor-fold>

  //<editor-fold desc="Mouse">
  @Override
  public void mouseDown(int buttons) {
    if (heldButtons != 0) {
      log.error("mouseDown: buttons still pressed - using all", buttons, heldButtons);
      heldButtons |= buttons;
    } else {
      heldButtons = buttons;
    }
    doMouseDown(heldButtons);
  }

  private void doMouseDown(int buttons) {
    Element.fakeHighlight(true);
    setAutoDelay(stdAutoDelay);
    setAutoWaitForIdle(false);
    pause(100);
    Element.fakeHighlight(false);
    pause(100);
    mousePress(buttons);
    if (stdAutoDelay == 0) {
      pause(stdDelay);
    }
  }

  @Override
  public int mouseUp(int buttons) {
    if (buttons == 0) {
      doMouseUp(heldButtons);
      heldButtons = 0;
    } else {
      doMouseUp(buttons);
      heldButtons &= ~buttons;
    }
    return heldButtons;
  }

  private void doMouseUp(int buttons) {
    setAutoDelay(stdAutoDelay);
    setAutoWaitForIdle(false);
    mouseRelease(buttons);
    if (stdAutoDelay == 0) {
      pause(stdDelay);
    }
  }

  public void pause(int ms) {
    if (ms < 0) {
      return;
    }
    while (ms > MAX_DELAY) {
      super.delay(MAX_DELAY);
      ms -= MAX_DELAY;
    }
    super.delay(ms);
  }
  //</editor-fold>

  //<editor-fold desc="Screen">
  @Override
  public Picture captureScreen(Rectangle rect) {
    BufferedImage bImg = createScreenCapture(rect);
    log.trace("captureScreen: [%d,%d, %dx%d]",
            rect.x, rect.y, rect.width, rect.height);
    return new Picture(bImg);
  }

  @Override
  public Color getColorAt(int x, int y) {
    return getPixelColor(x, y);
  }
  //</editor-fold>

  //<editor-fold desc="Keys">
  @Override
  public void pressModifiers(int modifiers) {
    if (Device.hasModifier(modifiers, Device.Modifier.SHIFT)) {
      doKeyPress(KeyEvent.VK_SHIFT);
    }
    if (Device.hasModifier(modifiers, Device.Modifier.CTRL)) {
      doKeyPress(KeyEvent.VK_CONTROL);
    }
    if (Device.hasModifier(modifiers, Device.Modifier.ALT)) {
      doKeyPress(KeyEvent.VK_ALT);
    }
    if (Device.hasModifier(modifiers, Device.Modifier.META)) {
      if (SX.isWindows()) {
        doKeyPress(KeyEvent.VK_WINDOWS);
      } else {
        doKeyPress(KeyEvent.VK_META);
      }
    }
  }

  @Override
  public void releaseModifiers(int modifiers) {
    if (Device.hasModifier(modifiers, Device.Modifier.SHIFT)) {
      doKeyRelease(KeyEvent.VK_SHIFT);
    }
    if (Device.hasModifier(modifiers, Device.Modifier.CTRL)) {
      doKeyRelease(KeyEvent.VK_CONTROL);
    }
    if (Device.hasModifier(modifiers, Device.Modifier.ALT)) {
      doKeyRelease(KeyEvent.VK_ALT);
    }
    if (Device.hasModifier(modifiers, Device.Modifier.META)) {
      if (SX.isWindows()) {
        doKeyRelease(KeyEvent.VK_WINDOWS);
      } else {
        doKeyRelease(KeyEvent.VK_META);
      }
    }
  }

  @Override
  public void keyDown(String keys) {
    if (keys != null && !"".equals(keys)) {
      for (int i = 0; i < keys.length(); i++) {
        if (heldKeys.indexOf(keys.charAt(i)) == -1) {
          log.trace("press: " + keys.charAt(i));
          typeChar(keys.charAt(i), KeyMode.PRESS_ONLY);
          heldKeys += keys.charAt(i);
        }
      }
    }
  }

  @Override
  public void keyDown(int code) {
    if (!heldKeyCodes.contains(code)) {
      doKeyPress(code);
      heldKeyCodes.add(code);
    }
  }

  private void doKeyPress(int keyCode) {
    setAutoDelay(stdAutoDelay);
    setAutoWaitForIdle(false);
    keyPress(keyCode);
    if (stdAutoDelay == 0) {
      pause(stdDelay);
    }
  }

  @Override
  public void keyUp(String keys) {
    if (keys != null && !"".equals(keys)) {
      for (int i = 0; i < keys.length(); i++) {
        int pos;
        if ((pos = heldKeys.indexOf(keys.charAt(i))) != -1) {
          log.trace("release: " + keys.charAt(i));
          typeChar(keys.charAt(i), KeyMode.RELEASE_ONLY);
          heldKeys = heldKeys.substring(0, pos)
                  + heldKeys.substring(pos + 1);
        }
      }
    }
  }

  @Override
  public void keyUp(int code) {
    if (heldKeyCodes.contains(code)) {
      doKeyRelease(code);
      heldKeyCodes.remove((Object) code);
    }
  }

  @Override
  public void keyUp() {
    keyUp(heldKeys);
    for (int code : heldKeyCodes) {
      keyUp(code);
    }
  }

  private void doKeyRelease(int keyCode) {
    setAutoDelay(stdAutoDelay);
    setAutoWaitForIdle(false);
    keyRelease(keyCode);
    if (stdAutoDelay == 0) {
      pause(stdDelay);
    }
  }
  //</editor-fold>

  //<editor-fold desc="type">
  @Override
  public void typeChar(char character, KeyMode mode) {
    log.trace("Robot: doType: %s ( %d )",
            KeyEvent.getKeyText(Device.toJavaKeyCode(character)[0]),
            Device.toJavaKeyCode(character)[0]);
    doType(mode, Device.toJavaKeyCode(character));
  }

  @Override
  public void typeKey(int key) {
    log.trace("Robot: doType: %s ( %d )", KeyEvent.getKeyText(key), key);
    if (SX.isMac()) {
      if (key == Device.toJavaKeyCodeFromText("#N.")) {
        doType(KeyMode.PRESS_ONLY, Device.toJavaKeyCodeFromText("#C."));
        doType(KeyMode.PRESS_RELEASE, key);
        doType(KeyMode.RELEASE_ONLY, Device.toJavaKeyCodeFromText("#C."));
        return;
      } else if (key == Device.toJavaKeyCodeFromText("#T.")) {
        doType(KeyMode.PRESS_ONLY, Device.toJavaKeyCodeFromText("#C."));
        doType(KeyMode.PRESS_ONLY, Device.toJavaKeyCodeFromText("#A."));
        doType(KeyMode.PRESS_RELEASE, key);
        doType(KeyMode.RELEASE_ONLY, Device.toJavaKeyCodeFromText("#A."));
        doType(KeyMode.RELEASE_ONLY, Device.toJavaKeyCodeFromText("#C."));
        return;
      } else if (key == Device.toJavaKeyCodeFromText("#X.")) {
        key = Device.toJavaKeyCodeFromText("#T.");
        doType(KeyMode.PRESS_ONLY, Device.toJavaKeyCodeFromText("#A."));
        doType(KeyMode.PRESS_RELEASE, key);
        doType(KeyMode.RELEASE_ONLY, Device.toJavaKeyCodeFromText("#A."));
        return;
      }
    }
    doType(KeyMode.PRESS_RELEASE, key);
  }

  private void doType(KeyMode mode, int... keyCodes) {
    if (mode == KeyMode.PRESS_ONLY) {
      for (int i = 0; i < keyCodes.length; i++) {
        doKeyPress(keyCodes[i]);
      }
    } else if (mode == KeyMode.RELEASE_ONLY) {
      for (int i = 0; i < keyCodes.length; i++) {
        doKeyRelease(keyCodes[i]);
      }
    } else {
      for (int i = 0; i < keyCodes.length; i++) {
        doKeyPress(keyCodes[i]);
      }
      for (int i = 0; i < keyCodes.length; i++) {
        doKeyRelease(keyCodes[i]);
      }
    }
  }
  //</editor-fold>
}
