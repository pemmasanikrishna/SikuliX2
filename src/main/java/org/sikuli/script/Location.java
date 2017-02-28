/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package org.sikuli.script;

import com.sikulix.api.Do;
import com.sikulix.api.Element;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import java.awt.*;

public class Location extends Element {
  private static SXLog log = SX.getLogger("SX.LOCATION");

  public int x = 0;
  public int y = 0;

  public Screen getScreen() {
    Rectangle r;
    int numScreens = Do.getDevice().getNumberOfMonitors();
    for (int i = 0; i < numScreens; i++) {
      r = Do.getDevice().getMonitor(i);
      if (r.contains(x, y)) {
        return Screen.getScreen(i);
      }
    }
    log.error("Location: outside any screen (%s, %s) - subsequent actions might not work as expected", x, y);
    return null;
  }

  public Location(int x, int y) {
    this.x = x;
    this.y = y;
  }

}
