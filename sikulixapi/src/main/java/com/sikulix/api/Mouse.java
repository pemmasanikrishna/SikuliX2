/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */
package com.sikulix.api;

import com.sikulix.core.*;
import com.sikulix.util.Settings;
import com.sikulix.util.animation.Animator;
import com.sikulix.util.animation.AnimatorOutQuarticEase;
import com.sikulix.util.animation.AnimatorTimeBased;

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
      return new Location(getLocationPoint());
  }

  private Point getLocationPoint() {
    PointerInfo mp = MouseInfo.getPointerInfo();
    if (mp != null) {
      return mp.getLocation();
    } else {
      log.error("not possible to get mouse position (PointerInfo == null)");
      return new Point(0,0);
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
    //TODO implement showMousePos (Visual.highlight)
//    Location lPos = new Location(pos);
//    Region inner = lPos.grow(20).highlight();
//    delay(500);
//    lPos.grow(40).highlight(1);
//    delay(500);
//    inner.highlight();
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

  private int click(Location loc, int buttons, Integer modifiers, boolean dblClick, Visual vis) {
    if (modifiers == null) {
      modifiers = 0;
    }
    boolean shouldMove = true;
    if (loc == null) {
      shouldMove = false;
      loc = at();
    }
    IRobot robot = loc.getDeviceRobot();
    if (robot == null) {
      return 0;
    }
    Point pLoc = loc.getPoint();
    use(vis);
    log.info("%s", getClickMsg(loc, buttons, modifiers, dblClick));
    if (shouldMove) {
      smoothMove(pLoc, robot);
    }
    robot.pressModifiers(modifiers);
    int pause = Settings.ClickDelay > 1 ? 1 : (int) (Settings.ClickDelay * 1000);
    Settings.ClickDelay = 0.0;
    if (dblClick) {
      robot.mouseDown(buttons);
      robot.mouseUp(buttons);
      robot.mouseDown(buttons);
      robot.mouseUp(buttons);
    } else {
      robot.mouseDown(buttons);
      robot.delay(pause);
      robot.mouseUp(buttons);
    }
    robot.releaseModifiers(modifiers);
    robot.waitForIdle();
    let(vis);
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
   * move the mouse to the given location
   *
   * @param loc Location
   * @return 1 for success, 0 otherwise
   */
  public int move(Location loc) {
    return move(loc, null);
  }

  public int move(Visual vis) {
    return move(vis.getTarget(), null);
  }

  /**
	 * move the mouse from the current position to the offset position given by the parameters
	 * @param xoff horizontal offset (&lt; 0 left, &gt; 0 right)
	 * @param yoff vertical offset (&lt; 0 up, &gt; 0 down)
   * @return 1 for success, 0 otherwise
	 */
  public int move(int xoff, int yoff) {
    return move(at().offset(xoff, yoff));
  }

  public int move(Location loc, Visual vis) {
    if (isSuspended()) {
      return 0;
    }
    Point pLoc = null;
    if (loc != null) {
      pLoc = loc.getPoint();
      IRobot robot = loc.getDeviceRobot();
      if (robot == null) {
        return 0;
      }
      use(vis);
      smoothMove(pLoc, robot);
      let(vis);
      return 1;
    }
    return 0;
  }

  public void smoothMove(Point dest, IRobot robot) {
    smoothMove(getLocationPoint(), dest, (long) (Settings.MoveMouseDelay * 1000L), robot);
  }

  public void smoothMove(Point src, Point dest, long ms, IRobot robot) {
    int x = dest.x;
    int y = dest.y;
    log.trace("smoothMove (%.1f): (%d, %d) to (%d, %d)", Settings.MoveMouseDelay, src.x, src.y, x, y);
    if (ms == 0) {
      robot.mouseMove(x, y);
    } else {
      Animator aniX = new AnimatorTimeBased(
              new AnimatorOutQuarticEase(src.x, dest.x, ms));
      Animator aniY = new AnimatorTimeBased(
              new AnimatorOutQuarticEase(src.y, dest.y, ms));
      while (aniX.running()) {
        x = (int) aniX.step();
        y = (int) aniY.step();
        robot.mouseMove(x, y);
      }
    }
    robot.waitForIdle();
    PointerInfo mp = MouseInfo.getPointerInfo();
    Point pc;
    if (mp == null) {
      log.error("RobotDesktop: checkMousePosition: MouseInfo.getPointerInfo invalid after move to (%d, %d)", x, y);
    } else {
      pc = mp.getLocation();
      if (pc.x != x || pc.y != y) {
        log.error("RobotDesktop: checkMousePosition: should be (%d, %d)\nbut after move is (%d, %d)"
                        + "\nPossible cause in case you did not touch the mouse while script was running:\n"
                        + " Mouse actions are blocked generally or by the frontmost application."
                        + (SX.isWindows() ? "\nYou might try to run the SikuliX stuff as admin." : ""),
                x, y, pc.x, pc.y);
      }
    }
  }

  /**
   * press and hold the given buttons {@link Button}
   *
   * @param buttons value
   */
  public void down(int buttons) {
    down(buttons, null);
  }

  public void down(int buttons, Visual vis) {
    if (isSuspended()) {
      return;
    }
    use(vis);
    vis.getDeviceRobot().mouseDown(buttons);
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

  public void up(int buttons, Visual vis) {
    if (isSuspended()) {
      return;
    }
    vis.getDeviceRobot().mouseUp(buttons);
    let(vis);
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

  public void wheel(int direction, int steps, Visual vis) {
    wheel(direction,steps,vis, WHEEL_STEP_DELAY);
  }
    
  public void wheel(int direction, int steps, Visual vis, int stepDelay) {
    if (isSuspended()) {
      return;
    }
    IRobot r = SX.isNull(vis) ? SX.getLocalRobot() : vis.getDeviceRobot();
    use(vis);
    log.debug("Region: wheel: %s steps: %d",
            (direction == WHEEL_UP ? "WHEEL_UP" : "WHEEL_DOWN"), steps);
    for (int i = 0; i < steps; i++) {
      r.mouseWheel(direction);
      r.delay(stepDelay);
    }
    let(vis);
  }
}
