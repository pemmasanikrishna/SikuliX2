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
public class Location extends com.sikulix.api.Location  {

  private static final SXLog log = SX.getLogger("API.Location");

  private IScreen otherScreen = null;

  //<editor-fold desc="Construction">
  private Location() {}

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
   * @return false if DeskTopScreen
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
  //</editor-fold>

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

}
