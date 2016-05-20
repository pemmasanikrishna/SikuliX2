/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package org.sikuli.script;

import com.sikulix.core.Visual;

import java.awt.*;

class LocationSX extends com.sikulix.api.Location {
  public LocationSX() {
    init(0, 0, 0, 0);
  }

  public LocationSX(int x, int y) {
    init(x, y, 0, 0);
  }

  public LocationSX(Visual vis) {
    init(vis.x, vis.y, 0, 0);
  }

  public LocationSX(Point p) {
    init(p);
  }

  public LocationSX(double x, double y) {
    init((int) x, (int) y, 0, 0);
  }

  protected static Location create(Location loc, int x, int y) {
    LocationSX newLoc = new LocationSX(x, y);
    if (loc.isOtherScreen()) {
      ((Location) newLoc).setOtherScreen(loc.getScreen());
    }
    return (Location) newLoc;
  }

  protected static Location create(Location loc, LocationSX newLoc) {
    return create(loc, newLoc.x, newLoc.y);
  }
}
