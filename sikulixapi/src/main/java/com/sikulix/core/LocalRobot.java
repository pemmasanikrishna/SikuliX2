/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */
package com.sikulix.core;

import com.sikulix.api.Image;
import com.sikulix.api.Keys;

import java.awt.Robot;
import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class LocalRobot extends Robot implements IRobot {

  private static SXLog log = SX.getLogger("SX.LocalRobot");

  final static int MAX_DELAY = 60000;
  private static int heldButtons = 0;
  private static String heldKeys = "";
  private static final ArrayList<Integer> heldKeyCodes = new ArrayList<Integer>();
  public static int stdAutoDelay = 0;
  public static int stdDelay = 10;

  @Override
  public boolean isRemote() {
    return false;
  }

  public LocalRobot() throws AWTException {
    super();
    setAutoDelay(stdAutoDelay);
    setAutoWaitForIdle(false);
  }

  private void doMouseMove(int x, int y) {
    mouseMove(x, y);
  }

  private void doMouseDown(int buttons) {
    Visual.fakeHighlight(true);
    setAutoDelay(stdAutoDelay);
    setAutoWaitForIdle(false);
    delay(100);
    Visual.fakeHighlight(false);
    delay(100);
    mousePress(buttons);
    if (stdAutoDelay == 0) {
      delay(stdDelay);
    }
  }

  private void doMouseUp(int buttons) {
    setAutoDelay(stdAutoDelay);
    setAutoWaitForIdle(false);
    mouseRelease(buttons);
    if (stdAutoDelay == 0) {
      delay(stdDelay);
    }
  }

  private void doKeyPress(int keyCode) {
    setAutoDelay(stdAutoDelay);
    setAutoWaitForIdle(false);
    keyPress(keyCode);
    if (stdAutoDelay == 0) {
      delay(stdDelay);
    }
  }

  private void doKeyRelease(int keyCode) {
    setAutoDelay(stdAutoDelay);
    setAutoWaitForIdle(false);
    keyRelease(keyCode);
    if (stdAutoDelay == 0) {
      delay(stdDelay);
    }
  }

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

  @Override
  public void delay(int ms) {
    if (ms < 0) {
      return;
    }
    while (ms > MAX_DELAY) {
      super.delay(MAX_DELAY);
      ms -= MAX_DELAY;
    }
    super.delay(ms);
  }

  @Override
  public Image captureScreen(Rectangle rect) {
    BufferedImage bImg = createScreenCapture(rect);
    log.trace("RobotDesktop: captureScreen: [%d,%d, %dx%d]",
            rect.x, rect.y, rect.width, rect.height);
    return new Image(bImg, rect);
  }

  public BufferedImage captureRegion(Rectangle rect) {
    BufferedImage img = createScreenCapture(rect);
    return img;
  }

  @Override
  public Color getColorAt(int x, int y) {
    return getPixelColor(x, y);
  }

  @Override
  public void pressModifiers(int modifiers) {
    if (Keys.hasModifier(modifiers, Keys.Modifier.SHIFT)) {
      doKeyPress(KeyEvent.VK_SHIFT);
    }
    if (Keys.hasModifier(modifiers, Keys.Modifier.CTRL)) {
      doKeyPress(KeyEvent.VK_CONTROL);
    }
    if (Keys.hasModifier(modifiers, Keys.Modifier.ALT)) {
      doKeyPress(KeyEvent.VK_ALT);
    }
    if (Keys.hasModifier(modifiers, Keys.Modifier.META)) {
      if (SX.isWindows()) {
        doKeyPress(KeyEvent.VK_WINDOWS);
      } else {
        doKeyPress(KeyEvent.VK_META);
      }
    }
  }

  @Override
  public void releaseModifiers(int modifiers) {
    if (Keys.hasModifier(modifiers, Keys.Modifier.SHIFT)) {
      doKeyRelease(KeyEvent.VK_SHIFT);
    }
    if (Keys.hasModifier(modifiers, Keys.Modifier.CTRL)) {
      doKeyRelease(KeyEvent.VK_CONTROL);
    }
    if (Keys.hasModifier(modifiers, Keys.Modifier.ALT)) {
      doKeyRelease(KeyEvent.VK_ALT);
    }
    if (Keys.hasModifier(modifiers, Keys.Modifier.META)) {
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

  @Override
  public void typeChar(char character, KeyMode mode) {
    log.trace("Robot: doType: %s ( %d )",
            KeyEvent.getKeyText(Keys.toJavaKeyCode(character)[0]),
            Keys.toJavaKeyCode(character)[0]);
    doType(mode, Keys.toJavaKeyCode(character));
  }

  @Override
  public void typeKey(int key) {
    log.trace("Robot: doType: %s ( %d )", KeyEvent.getKeyText(key), key);
    if (SX.isMac()) {
      if (key == Keys.toJavaKeyCodeFromText("#N.")) {
        doType(KeyMode.PRESS_ONLY, Keys.toJavaKeyCodeFromText("#C."));
        doType(KeyMode.PRESS_RELEASE, key);
        doType(KeyMode.RELEASE_ONLY, Keys.toJavaKeyCodeFromText("#C."));
        return;
      } else if (key == Keys.toJavaKeyCodeFromText("#T.")) {
        doType(KeyMode.PRESS_ONLY, Keys.toJavaKeyCodeFromText("#C."));
        doType(KeyMode.PRESS_ONLY, Keys.toJavaKeyCodeFromText("#A."));
        doType(KeyMode.PRESS_RELEASE, key);
        doType(KeyMode.RELEASE_ONLY, Keys.toJavaKeyCodeFromText("#A."));
        doType(KeyMode.RELEASE_ONLY, Keys.toJavaKeyCodeFromText("#C."));
        return;
      } else if (key == Keys.toJavaKeyCodeFromText("#X.")) {
        key = Keys.toJavaKeyCodeFromText("#T.");
        doType(KeyMode.PRESS_ONLY, Keys.toJavaKeyCodeFromText("#A."));
        doType(KeyMode.PRESS_RELEASE, key);
        doType(KeyMode.RELEASE_ONLY, Keys.toJavaKeyCodeFromText("#A."));
        return;
      }
    }
    doType(KeyMode.PRESS_RELEASE, key);
  }

  @Override
  public void cleanup() {
  }
}
