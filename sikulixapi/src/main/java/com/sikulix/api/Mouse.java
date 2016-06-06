/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */
package com.sikulix.api;

import com.sikulix.core.*;
import com.sikulix.util.Settings;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * Main pupose is to coordinate the mouse usage among threads <br>
 * At any one time, the mouse has one owner (usually a Region object) <br>
 * who exclusively uses the mouse, all others wait for the mouse to be free again <br>
 * if more than one possible owner is waiting, the next owner is uncertain <br>
 * It is detected, when the mouse is moved external from the workflow, which can be used for
 * appropriate actions (e.g. pause a script) <br>
 * the mouse can be blocked for a longer time, so only this owner can use the mouse (like some
 * transactional processing) <br>
 * Currently deadlocks and infinite waits are not detected, but should not happen ;-) <br>
 * Contained are methods to use the mouse (click, move, button down/up) as is
 */
public class Mouse extends Device {

  private static SXLog log = SX.getLogger("SX.Mouse");
  private static int lvl = SXLog.DEBUG;

  private static Mouse mouse = null;

  private Location lastPos = null;

  private int MouseMovedIgnore = 0;
  private int MouseMovedShow = 1;
  private int MouseMovedPause = 2;
  private int MouseMovedAction = 3;
  private int mouseMovedResponse = MouseMovedIgnore;
  private boolean MouseMovedHighlight = true;


  private Location mousePos;
  private boolean clickDouble;
  private int buttons;
  private int beforeWait;
  private int innerWait;
  private int afterWait;

  public static final int LEFT = InputEvent.BUTTON1_MASK;
  public static final int MIDDLE = InputEvent.BUTTON2_MASK;
  public static final int RIGHT = InputEvent.BUTTON3_MASK;
  public static final int WHEEL_UP = -1;
  public static int WHEEL_DOWN = 1;
  public static final int WHEEL_STEP_DELAY = 50;

  private Mouse() {
  }

  public static Mouse get() {
    if (mouse == null) {
      mouse = new Mouse();
      mouse.init();
    }
    return mouse;
  }

  public void init() {
    //TODO Mouse init
    isMouse = true;
    move(at());
    lastPos = null;
    log.debug("init done");
  }

  public Location getLocation() {
    PointerInfo mp = MouseInfo.getPointerInfo();
    if (mp != null) {
      return new Location(MouseInfo.getPointerInfo().getLocation());
    } else {
      log.error("Mouse: not possible to get mouse position (PointerInfo == null)");
      return null;
    }
  }

  public void setLastPos() {
    lastPos = getLocation();
  }

  public void checkLastPos() {
    if (lastPos == null) {
      return;
    }
    Location pos = getLocation();
    if (pos != null && (lastPos.x != pos.x || lastPos.y != pos.y)) {
      log.debug("moved externally: now (%d,%d) was (%d,%d) (mouseMovedResponse %d)",
              pos.x, pos.y, lastPos.x, lastPos.y, mouseMovedResponse);
      if (mouseMovedResponse > 0) {
        if (MouseMovedHighlight) {
          showMousePos(pos.getPoint());
        }
      }
      if (mouseMovedResponse == MouseMovedPause) {
        while (pos.x > 0 && pos.y > 0) {
          delay(500);
          pos = getLocation();
          if (MouseMovedHighlight) {
            showMousePos(pos.getPoint());
          }
        }
        if (pos.x < 1) {
          return;
        }
        SX.terminate(1, "Terminating in MouseMovedResponse = Pause");
      }
      if (mouseMovedResponse == MouseMovedAction) {
//TODO implement 3
//        if (mouseMovedCallback != null) {
//          mouseMovedCallback.happened(new ObserveEvent(ObserveEvent.Type.GENERIC, lastPos, new Location(pos)));
//          if (shouldTerminate) {
//            shouldTerminate = false;
//            throw new AssertionError("aborted by Sikulix.MouseMovedCallBack");
//          }
//        }
      }
    }
  }

  private static void showMousePos(Point pos) {
    Location lPos = new Location(pos);
    Region inner = lPos.grow(20).highlight();
    delay(500);
    lPos.grow(40).highlight(1);
    delay(500);
    inner.highlight();
  }

  public Location at() {
    return getLocation();
  }

  public void reset() {
    if (mouse == null) {
      return;
    }
    unblock(owner);
    let(owner);
    let(owner);
    up();
    mouseMovedResponse = MouseMovedIgnore;
    mouseMovedCallback = null;
		callback = null;
    lastPos = null;
  }

  /**
   * current setting what to do if mouse is moved outside Sikuli's mouse protection
   *
   * @return current setting see {@link #setMouseMovedAction(int)}
   */
  public int getMouseMovedResponse() {
    return mouseMovedResponse;
  }

  /**
   * what to do if mouse is moved outside Sikuli's mouse protection <br>
   * - Mouse.MouseMovedIgnore (0) ignore it (default) <br>
   * - Mouse.MouseMovedShow (1) show and ignore it <br>
   * - Mouse.MouseMovedPause (2) show it and pause until user says continue <br>
   * (2 not implemented yet - 1 is used)
   *
   * @param movedAction value
   */
  public void setMouseMovedAction(int movedAction) {
    if (movedAction > -1 && movedAction < 3) {
      mouseMovedResponse = movedAction;
      mouseMovedCallback = null;
      log.debug("setMouseMovedAction: %d", mouseMovedResponse);
    }
  }

  /**
   * what to do if mouse is moved outside Sikuli's mouse protection <br>
   * only 3 is honored:<br>
   * in case of event the user provided callBack.happened is called
   *
   * @param callBack ObserverCallBack
   */
  public void setMouseMovedCallback(Object callBack) {
    if (callBack != null) {
      mouseMovedResponse = 3;
      mouseMovedCallback = new ObserverCallBack(callBack, ObserveEvent.Type.GENERIC);
    }
  }

  /**
   *
   * @param state
   */
  public void setMouseMovedHighlight(boolean state) {
    MouseMovedHighlight = state;
}

  /**
   * check if mouse was moved since last mouse action
   *
   * @return true/false
   */
  public boolean hasMoved() {
    Location pos = getLocation();
    if (lastPos.x != pos.x || lastPos.y != pos.y) {
      return true;
    }
    return false;
  }

  /**
   * to click (left, right, middle - single or double) at the given location using the given button
   * only useable for local screens
   *
   * timing parameters: <br>
   * - one value <br>
   * &lt; 0 wait before mouse down <br>
   * &gt; 0 wait after mouse up <br>
   * - 2 or 3 values 1st wait before mouse down <br>
   * 2nd wait after mouse up <br>
   * 3rd inner wait (milli secs, cut to 1000): pause between mouse down and up (Settings.ClickDelay)
   *
   * wait before and after: &gt; 59 taken as milli secs - &lt; are seconds
   *
   * @param loc where to click
   * @param action L,R,M left, right, middle - D means double click
   * @param args timing parameters
   * @return the location
   */
  public Location click(Location loc, String action, Integer... args) {
    if (isSuspended() || loc.isSpecial()) {
      return null;
    }
    getArgsClick(loc, action, args);
    use();
    Device.delay(mouse.beforeWait);
    Settings.ClickDelay = mouse.innerWait / 1000;
    click(loc, buttons, 0, clickDouble, null);
    Device.delay(mouse.afterWait);
    let();
    return loc;
  }

  private void getArgsClick(Location loc, String action, Integer... args) {
    mouse.mousePos = loc;
    mouse.clickDouble = false;
    action = action.toUpperCase();
    if (action.contains("D")) {
      mouse.clickDouble = true;
    }
    mouse.buttons = 0;
    if (action.contains("L")) {
      mouse.buttons += LEFT;
    }
    if (action.contains("M")) {
      mouse.buttons += MIDDLE;
    }
    if (action.contains("R")) {
      mouse.buttons += RIGHT;
    }
    if (mouse.buttons == 0) {
      mouse.buttons = LEFT;
    }
    mouse.beforeWait = 0;
    mouse.innerWait = 0;
    mouse.afterWait = 0;
    if (args.length > 0) {
      if (args.length == 1) {
        if (args[0] < 0) {
          mouse.beforeWait = -args[0];
        } else {
          mouse.afterWait = args[0];
        }
      }
      mouse.beforeWait = args[0];
      if (args.length > 1) {
        mouse.afterWait = args[1];
        if (args.length > 2) {
          mouse.innerWait = args[2];
        }
      }
    }
  }

  private int click(Location loc, int buttons, Integer modifiers, boolean dblClick, Region region) {
    if (modifiers == null) {
      modifiers = 0;
    }
    boolean shouldMove = true;
    if (loc == null) {
      shouldMove = false;
      loc = at();
    }
    IRobot r = null;
    r = Screen.getMouseRobot();
    if (r == null) {
      return 0;
    }
    use(region);
    log.info("%s", getClickMsg(loc, buttons, modifiers, dblClick));
    if (shouldMove) {
      r.smoothMove(loc);
    }
    r.pressModifiers(modifiers);
    int pause = Settings.ClickDelay > 1 ? 1 : (int) (Settings.ClickDelay * 1000);
    Settings.ClickDelay = 0.0;
    if (dblClick) {
      r.mouseDown(buttons);
      r.mouseUp(buttons);
      r.mouseDown(buttons);
      r.mouseUp(buttons);
    } else {
      r.mouseDown(buttons);
      r.delay(pause);
      r.mouseUp(buttons);
    }
    r.releaseModifiers(modifiers);
    r.waitForIdle();
    let(region);
    return 1;
  }

  private static String getClickMsg(Location loc, int buttons, int modifiers, boolean dblClick) {
    String msg = "";
    if (modifiers != 0) {
      msg += KeyEvent.getKeyModifiersText(modifiers) + "+";
    }
    if (buttons == InputEvent.BUTTON1_MASK && !dblClick) {
      msg += "CLICK";
    }
    if (buttons == InputEvent.BUTTON1_MASK && dblClick) {
      msg += "DOUBLE CLICK";
    }
    if (buttons == InputEvent.BUTTON3_MASK) {
      msg += "RIGHT CLICK";
    } else if (buttons == InputEvent.BUTTON2_MASK) {
      msg += "MID CLICK";
    }
    msg += " on " + loc;
    return msg;
  }

  /**
   * move the mouse to the given location (local and remote)
   *
   * @param loc Location
   * @return 1 for success, 0 otherwise
   */
  public static int move(org.sikuli.script.Location loc) {
    return move(loc, null);
  }

  public static int move(Visual loc) {
    //TODO ajust for com.sikulix.core.Location
    return 0;
  }

  /**
	 * move the mouse from the current position to the offset position given by the parameters
	 * @param xoff horizontal offset (&lt; 0 left, &gt; 0 right)
	 * @param yoff vertical offset (&lt; 0 up, &gt; 0 down)
   * @return 1 for success, 0 otherwise
	 */
  public static int move(int xoff, int yoff) {
    return move(at().offset(xoff, yoff));
  }

  public int move(Location loc, Region region) {
    if (isSuspended()) {
      return 0;
    }
    if (loc != null) {
      IRobot r = null;
      r = Screen.getMouseRobot();
      if (r == null) {
        return 0;
      }
      if (!r.isRemote()) {
        use(region);
      }
      r.smoothMove(loc);
      r.waitForIdle();
      if (!r.isRemote()) {
        let(region);
      }
      return 1;
    }
    return 0;
  }

  /**
   * press and hold the given buttons {@link Button}
   *
   * @param buttons value
   */
  public static void down(int buttons) {
    down(buttons, null);
  }

  protected void down(int buttons, Region region) {
    if (isSuspended()) {
      return;
    }
    use(region);
    Screen.getMouseRobot().mouseDown(buttons);
  }

  /**
   * release all buttons
   *
   */
  public void up() {
    up(0, null);
  }

  /**
   * release the given buttons {@link Button}
   *
   * @param buttons (0 releases all buttons)
   */
  public void up(int buttons) {
    up(buttons, null);
  }

  protected void up(int buttons, Region region) {
    if (isSuspended()) {
      return;
    }
    Screen.getMouseRobot().mouseUp(buttons);
    if (region != null) {
      let(region);
    }
  }

  /**
   * move mouse using mouse wheel in the given direction the given steps <br>
   * the result is system dependent
   *
   * @param direction {@link Button}
   * @param steps value
   */
  public void wheel(int direction, int steps) {
    wheel(direction, steps, null);
  }

  protected void wheel(int direction, int steps, Region region) {
    wheel(direction,steps,region, WHEEL_STEP_DELAY);
  }
    
  protected void wheel(int direction, int steps, Region region, int stepDelay) {
    if (isSuspended()) {
      return;
    }
    IRobot r = Screen.getMouseRobot();
    use(region);
    log.debug("Region: wheel: %s steps: %d",
            (direction == WHEEL_UP ? "WHEEL_UP" : "WHEEL_DOWN"), steps);
    for (int i = 0; i < steps; i++) {
      r.mouseWheel(direction);
      r.delay(stepDelay);
    }
    let(region);
  }
}
