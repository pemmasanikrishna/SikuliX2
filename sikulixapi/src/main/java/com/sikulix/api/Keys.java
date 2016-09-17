/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.core.Device;

import java.awt.event.InputEvent;

/**
 * Main pupose is to coordinate the keyboard usage among threads <br>
 * At any one time, the keyboard has one owner (usually a Region object) <br>
 * who exclusively uses the keyboard, all others wait for the keyboard to be free again <br>
 * if more than one possible owner is waiting, the next owner is uncertain <br>
 * It is detected, when the keyboard is usaed externally of the workflow, which can be used for appropriate actions
 * (e.g. pause a script) <br>
 * the keyboard can be blocked for a longer time, so only this owner can use the keyboard (like some transactional
 * processing) <br>
 * Currently deadlocks and infinite waits are not detected, but should not happen ;-) <br>
 * Contained are methods to use the keyboard (click, press, release) as is<br>
 * The keys are specified completely and only as string either litterally by their keynames<br>
 */
public class Keys extends Device {

  private static SXLog log = SX.getLogger("SX.Keys");
  private static int lvl = SXLog.DEBUG;

  private static Keys keys = null;

  private Keys() {
  }

  public static Keys get() {
    if (keys == null) {
      keys = new Keys();
      keys.init();
    }
    return keys;
  }

  public void init() {
    //TODO Keys init
    isKeys = true;
    log.debug("init done");
  }

  public class Modifier {
    public static final int CTRL = InputEvent.CTRL_MASK;
    public static final int SHIFT = InputEvent.SHIFT_MASK;
    public static final int ALT = InputEvent.ALT_MASK;
    public static final int ALTGR = InputEvent.ALT_GRAPH_MASK;
    public static final int META = InputEvent.META_MASK;
    public static final int CMD = InputEvent.META_MASK;
    public static final int WIN = 64;
  }

  public static boolean hasModifier(int mods, int mkey) {
    return (mods & mkey) != 0;
  }

  public static int[] toJavaKeyCode(char character) {
    //TODO implement toJavaKeyCode
    return new int[0];
  }

  public static int toJavaKeyCodeFromText(String s) {
    //TODO implement toJavaKeyCodeFromText
    return 0;
  }

}
