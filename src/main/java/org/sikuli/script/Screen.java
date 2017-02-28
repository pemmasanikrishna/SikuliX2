/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package org.sikuli.script;

import com.sikulix.api.Do;
import com.sikulix.api.Element;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class Screen extends Region {
  private static SXLog log = SX.getLogger("SX.SCREEN");

  private int id = -1;
  private int curID = -1;

  public int getID() {
    return id;
  }

  public Screen() {
    init(Do.getDevice().getMonitor());
    id = 0;
  }

  public Screen(int id) {
    init(Do.getDevice().getMonitor(id));
    this.id = id;
  }

  public Screen(Element elem) {
    init(elem.getRectangle());
  }

  public String toString() {
    return String.format("Screen(%d,%d %dx%d #%d)", x, y, w, h, id);
  }

  public Screen getScreen() {
    return this;
  }

  private static List<Screen> screens = new ArrayList<>();

  public static Screen getScreen(int num) {
    if (screens.isEmpty()) {
      for (Rectangle monitor : Do.getDevice().getMonitors()) {
        screens.add(new Screen(new Element(monitor)));
      }
    }
    if (num > -1 && num < screens.size()) {
      return screens.get(num);
    } else {
      return screens.get(0);
    }
  }

  public String toStringPlus() {
    return " #" + id;
  }

  /**
   * show the current monitor setup
   */
  public static void showMonitors() {
    log.p("*** monitor configuration [ %s Screen(s)] ***", Do.getDevice().getNumberOfMonitors());
    log.p("*** Primary is Screen %d", Do.getDevice().getMainMonitorID());
    for (int i = 0; i < Do.getDevice().getNumberOfMonitors(); i++) {
      log.p("%d: %s", i, getScreen(i));
    }
    log.p("*** end monitor configuration ***");
  }

  /**
   * re-initialize the monitor setup (e.g. when it was changed while running)
   */
  public static void resetMonitors() {
    showMonitors();
    log.p("*** TRYING *** to reset the monitor configuration");
    Do.getDevice().resetMonitors();
    showMonitors();
  }

}
