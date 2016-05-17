/*
 * Copyright 2010-2014, Sikuli.org, sikulix.com
 * Released under the MIT License.
 *
 * modified RaiMan
 */
package org.sikuli.script;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import java.awt.Point;

/**
 * A point like AWT.Point using global coordinates (x, y).
 * hence modifications might move location out of
 * any screen (not checked as is done with region)
 */
public class Location extends LocationSX implements Comparable<Location> {

  private static final SXLog log = SX.getLogger("API.Location");

  private IScreen otherScreen = null;

  //<editor-fold desc="Construction">
  /**
   * to allow calculated x and y that might not be integers
   *
   * @param x column
   * @param y row
   *          truncated to the integer part
   */
  public Location(double x, double y) {
    init((int) x, (int) y, 0, 0);
  }

  /**
   * a new point at the given coordinates
   *
   * @param x column
   * @param y row
   */
  public Location(int x, int y) {
    init(x, y, 0, 0);
  }

  /**
   * duplicates the point
   *
   * @param loc other Location
   */
  public Location(Location loc) {
    init(loc.x, loc.y, 0, 0);
    if (loc.isOtherScreen()) {
      otherScreen = loc.getScreen();
    }
  }

  /**
   * create from AWT point
   *
   * @param point a Point
   */
  public Location(Point point) {
    init(point.x, point.y, 0, 0);
  }

  /**
   * sets the coordinates to the given values (moves it)
   *
   * @param x new x
   * @param y new y
   * @return this
   */
  public Location setLocation(int x, int y) {
    at(x, y);
    return this;
  }

  /**
   * sets the coordinates to the given values (moves it)
   *
   * @param x new x might be non-int
   * @param y new y might be non-int
   * @return this
   */
  public Location setLocation(double x, double y) {
    at((int) x, (int) y);
    return this;
  }
  //</editor-fold>

  //<editor-fold desc="on which screen">
  /**
   * Returns null, if outside of any screen and not contained in a non-Desktop Screen instance (e.g. remote screen)<br>
   * subsequent actions WILL crash if not tested for null return
   *
   * @return the screen, that contains the given point
   */
  public IScreen getScreen() {
    int monitor = getContainingScreenNumber();
    if (monitor < 0) {
      log.error("Location: outside any screen (%s, %s) - subsequent actions might not work as expected", x, y);
      return null;
    }
    return Screen.getScreen(monitor);
  }

  /**
   * INTERNAL USE
   * reveals wether the containing screen is a DeskTopScreen or not
   *
   * @return null if DeskTopScreen
   */
  public boolean isOtherScreen() {
    return (otherScreen != null);
  }

  /**
   * INTERNAL USE
   * identifies the point as being on a non-desktop-screen
   *
   * @param scr Screen
   * @return this
   */
  public Location setOtherScreen(IScreen scr) {
    otherScreen = scr;
    return this;
  }

  /**
   * INTERNAL USE
   * identifies the point as being on a non-desktop-screen
   * if this is true for the given location
   *
   * @return this
   */
  //</editor-fold>

  /**
   * the offset of given point to this Location
   *
   * @param loc the other Location
   * @return relative offset
   */
  public Location getOffset(Location loc) {
    return create(this, loc.x - x, loc.y - y);
  }

  //<editor-fold desc="grow">
  /**
   * create a region with this point as center and the given size
   *
   * @param w the width
   * @param h the height
   * @return the new region
   */
  public Region grow(int w, int h) {
    return Region.grow(this, w, h);
  }

  /**
   * create a region with this point as center and the given size
   *
   * @param wh the width and height
   * @return the new region
   */
  public Region grow(int wh) {
    return grow(wh, wh);
  }

  /**
   * create a region with a corner at this point<br>as specified with x y<br> 0 0 top left<br>
   * 0 1 bottom left<br> 1 0 top right<br> 1 1 bottom right<br>
   *
   * @param CREATE_X_DIRECTION == 0 is left side !=0 is right side, see {@link Region#CREATE_X_DIRECTION_LEFT}, {@link Region#CREATE_X_DIRECTION_RIGHT}
   * @param CREATE_Y_DIRECTION == 0 is top side !=0 is bottom side, see {@link Region#CREATE_Y_DIRECTION_TOP}, {@link Region#CREATE_Y_DIRECTION_BOTTOM}
   * @param w                  the width
   * @param h                  the height
   * @return the new region
   */
  public Region grow(int CREATE_X_DIRECTION, int CREATE_Y_DIRECTION, int w, int h) {
    return Region.create(this, CREATE_X_DIRECTION, CREATE_Y_DIRECTION, w, h);
  }
  //</editor-fold>

  //<editor-fold desc="move">
  /**
   * moves the point the given amounts in the x and y direction, might be negative <br>might move
   * point outside of any screen, not checked
   *
   * @param dx x offset
   * @param dy y offset
   * @return the location itself modified
   * @deprecated use {@link #translate(int, int)}
   */
  @Deprecated
  public Location moveFor(int dx, int dy) {
    x += dx;
    y += dy;
    return this;
  }

  /**
   * convenience: like awt point
   *
   * @param dx x offset
   * @param dy y offset
   * @return the location itself modified
   */
  public Location translate(int dx, int dy) {
    translate((Integer) dx, (Integer) dy);
    return this;
  }

  /**
   * changes the locations x and y value to the given values (moves it) <br>might move point
   * outside of any screen, not checked
   *
   * @param X new x
   * @param Y new y
   * @return the location itself modified
   * @deprecated use {@link #move(int, int)}
   */
  @Deprecated
  public Location moveTo(int X, int Y) {
    return move(X, Y);
  }

  /**
   * convenience: like awt point
   *
   * @param X new x
   * @param Y new y
   * @return the location itself modified
   */
  public Location move(int X, int Y) {
    at(X, Y);
    return this;
  }
  //</editor-fold>

  //<editor-fold desc="new locations">
  /**
   * creates a point at the given offset, might be negative <br>might create a point outside of
   * any screen, not checked
   *
   * @param dx x offset
   * @param dy y offset
   * @return new location
   */
  public Location offset(int dx, int dy) {
    return create(this, (LocationSX) offset((Integer) dx, (Integer) dy));
  }

  /**
   * creates a point at the given offset, might be negative <br>might create a point outside of
   * any screen, not checked
   *
   * @param loc offset given as Location
   * @return new location
   */
  public Location offset(Location loc) {
    return create(this, (LocationSX) offset(loc.x, loc.y));
  }

  /**
   * creates a point at the given offset to the left, might be negative <br>might create a point
   * outside of any screen, not checked
   *
   * @param dx x offset
   * @return new location
   */
  public Location left(int dx) {
    return create(this, (LocationSX) left((Integer) dx));
  }

  /**
   * creates a point at the given offset to the right, might be negative <br>might create a point
   * outside of any screen, not checked
   *
   * @param dx x offset
   * @return new location
   */
  public Location right(int dx) {
    return create(this, (LocationSX) right((Integer) dx));
  }

  /**
   * creates a point at the given offset above, might be negative <br>might create a point outside
   * of any screen, not checked
   *
   * @param dy y offset
   * @return new location
   */
  public Location above(int dy) {
    return create(this, (LocationSX) above((Integer) dy));
  }

  /**
   * creates a point at the given offset below, might be negative <br>might create a point outside
   * of any screen, not checked
   *
   * @param dy y offset
   * @return new location
   */
  public Location below(int dy) {
    return create(this, (LocationSX) below((Integer) dy));
  }

  @Deprecated
  public Location copyTo(int scrID) {
    return copyTo(Screen.getScreen(scrID));
  }

  @Deprecated
  public Location copyTo(IScreen screen) {
    IScreen s = getScreen();
    s = (s == null ? Screen.getPrimaryScreen() : s);
    Location o = create(this, s.getBounds().getLocation().x, s.getBounds().getLocation().y);
    Location n = create(this, screen.getBounds().getLocation().x, screen.getBounds().getLocation().y);
    return create(this, n.x + x - o.x, n.y + y - o.y);
  }
  //</editor-fold>

  //<editor-fold desc="mouse">
  /**
   * Move the mouse to this location point
   *
   * @return this
   */
  public Location hover() {
    Mouse.move(this);
    return this;
  }

  /**
   * Move the mouse to this location point and click left
   *
   * @return this
   */
  public Location click() {
    Mouse.click(this, "L");
    return this;
  }

  /**
   * Move the mouse to this location point and double click left
   *
   * @return this
   */
  public Location doubleClick() {
    Mouse.click(this, "LD");
    return this;
  }

  /**
   * Move the mouse to this location point and click right
   *
   * @return this
   */
  public Location rightClick() {
    Mouse.click(this, "R");
    return this;
  }
  //</editor-fold>

  //<editor-fold desc="helper">
  @Override
  public boolean equals(Object oThat) {
    if (this == oThat) {
      return true;
    }
    if (!(oThat instanceof Location)) {
      return false;
    }
    Location that = (Location) oThat;
    return x == that.x && y == that.y;
  }

  /**
   * {@inheritDoc}
   *
   * @param loc other Location
   * @return -1 if given point is more above and/or left, 1 otherwise (0 is equal)
   */
  @Override
  public int compareTo(Location loc) {
    if (equals(loc)) {
      return 0;
    }
    if (loc.x > x) {
      return 1;
    } else if (loc.x == x) {
      if (loc.y > y) {
        return 1;
      }
    }
    return -1;
  }

  /**
   * {@inheritDoc}
   *
   * @return the description
   */
  @Override
  public String toString() {
    String sScreen = "";
    if (getScreen().getID() != 0) {
      String.format("@S(%s)", (getScreen() == null) ? "?" : getScreen().getID());
    }
    return String.format("L[%d,%d]%s", x, y, sScreen);
  }

  public String toJSON() {
    return String.format("[\"L\", [%d, %d]]", x, y);
  }

  // to avoid NPE for points outside any screen
  protected IRobot getRobotForPoint(String action) {
    if (getScreen() == null) {
      log.error("Point %s outside any screen not useable for %s", this, action);
      return null;
    }
    if (!getScreen().isOtherScreen()) {
      getScreen().showTarget(this);
    }
    return Screen.getMouseRobot();
//    return getScreen().getRobot();
  }
  //</editor-fold>
}
