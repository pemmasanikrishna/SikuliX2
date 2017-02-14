/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package org.sikuli.script;

import com.sikulix.api.Do;
import com.sikulix.api.Element;
import com.sikulix.api.Picture;
import com.sikulix.core.LocalDevice;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import org.sikuli.basics.Debug;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Region {
  private static SXLog log = SX.getLogger("SX.REGION");

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public int getW() {
    return w;
  }

  public int getH() {
    return h;
  }

  /**
   * X-coordinate of the Region
   */
  public int x;
  /**
   * Y-coordinate of the Region
   */
  public int y;
  /**
   * Width of the Region
   */
  public int w;
  /**
   * Height of the Region
   */
  public int h;

  private Element regionElement;

  private IScreen scr;

  public IScreen getScreen() {
    return scr;
  }

  private boolean otherScreen = false;

  /**
   * INTERNAL USE: checks wether this region belongs to a non-Desktop screen
   *
   * @return true/false
   */
  public boolean isOtherScreen() {
    return otherScreen;
  }

  /**
   * INTERNAL USE: flags this region as belonging to a non-Desktop screen
   */
  public void setOtherScreen() {
    otherScreen = true;
  }

  public void setOtherScreen(IScreen aScreen) {
    scr = aScreen;
    setOtherScreen();
  }

  protected Rectangle regionOnScreen(IScreen screen) {
    if (screen == null) {
      return null;
    }
    // get intersection of Region and Screen
    Rectangle rect = screen.getRect().intersection(getRect());
    // no Intersection, Region is not on the Screen
    if (rect.isEmpty()) {
      return null;
    }
    return rect;
  }

  public static Screen getPrimaryScreen() {
    return (Screen) Screen.getScreen(0);
  }

  public void initScreen(IScreen iscr) {
    // check given screen first
    Rectangle rect, screenRect;
    IScreen screen, screenOn;
    if (iscr != null) {
      if (iscr.isOtherScreen()) {
        if (x < 0) {
          w = w + x;
          x = 0;
        }
        if (y < 0) {
          h = h + y;
          y = 0;
        }
        this.scr = iscr;
        this.otherScreen = true;
        return;
      }
      if (iscr.getID() > -1) {
        rect = regionOnScreen(iscr);
        if (rect != null) {
          x = rect.x;
          y = rect.y;
          w = rect.width;
          h = rect.height;
          this.scr = iscr;
          return;
        }
      } else {
        // is ScreenUnion
        return;
      }
    }
    // check all possible screens if no screen was given or the region is not on given screen
    // crop to the screen with the largest intersection
    screenRect = new Rectangle(0, 0, 0, 0);
    screenOn = null;
    boolean isVNC = false;
    //TODO VNCScreen
//    if (iscr == null) {
//      isVNC = scr instanceof VNCScreen;
//    } else {
//      isVNC = iscr instanceof VNCScreen;
//    }
    if (!isVNC) {
      for (int i = 0; i < SX.getSXLOCALDEVICE().getNumberOfMonitors(); i++) {
        screen = Screen.getScreen(i);
        rect = regionOnScreen(screen);
        if (rect != null) {
          if (rect.width * rect.height > screenRect.width * screenRect.height) {
            screenRect = rect;
            screenOn = screen;
          }
        }
      }
    } else {
//      for (int i = 0; i < VNCScreen.getNumberScreens(); i++) {
//        screen = VNCScreen.getScreen(i);
//        rect = regionOnScreen(screen);
//        if (rect != null) {
//          if (rect.width * rect.height > screenRect.width * screenRect.height) {
//            screenRect = rect;
//            screenOn = screen;
//          }
//        }
//      }
    }
    if (screenOn != null) {
      x = screenRect.x;
      y = screenRect.y;
      w = screenRect.width;
      h = screenRect.height;
      this.scr = screenOn;
    } else {
      // no screen found
      this.scr = null;
      log.error("Region(%d,%d,%d,%d) outside any screen - subsequent actions might not work as expected", x, y, w, h);
    }
  }

  //<editor-fold defaultstate="collapsed" desc="Constructors to be used with Jython">

  /**
   * Create a region with the provided coordinate / size and screen
   *
   * @param X            X position
   * @param Y            Y position
   * @param W            width
   * @param H            heigth
   * @param screenNumber The number of the screen containing the Region
   */
  public Region(int X, int Y, int W, int H, int screenNumber) {
    this(X, Y, W, H, Screen.getScreen(screenNumber));
  }

  /**
   * Create a region with the provided coordinate / size and screen
   *
   * @param X            X position
   * @param Y            Y position
   * @param W            width
   * @param H            heigth
   * @param parentScreen the screen containing the Region
   */
  public Region(int X, int Y, int W, int H, IScreen parentScreen) {
    this.x = X;
    this.y = Y;
    this.w = W > 1 ? W : 1;
    this.h = H > 1 ? H : 1;
    initScreen(parentScreen);
  }

  /**
   * Create a region with the provided coordinate / size
   *
   * @param X X position
   * @param Y Y position
   * @param W width
   * @param H heigth
   */
  public Region(int X, int Y, int W, int H) {
    this(X, Y, W, H, null);
  }

  /**
   * Create a region from a Rectangle
   *
   * @param r the Rectangle
   */
  public Region(Rectangle r) {
    this(r.x, r.y, r.width, r.height, null);
  }

  /**
   * Create a new region from another region<br>including the region's settings
   *
   * @param r the region
   */
  public Region(Region r) {
    init(r);
  }

  public void init(Region r) {
    x = r.x;
    y = r.y;
    w = r.w;
    h = r.h;
    scr = r.getScreen();
    otherScreen = r.isOtherScreen();
    autoWaitTimeout = r.autoWaitTimeout;
    findFailedResponse = r.findFailedResponse;
    throwException = r.throwException;
    waitScanRate = r.waitScanRate;
    observeScanRate = r.observeScanRate;
    repeatWaitTime = r.repeatWaitTime;
  }

  public void init(Rectangle r) {
    x = r.x;
    y = r.y;
    w = r.width;
    h = r.height;
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Quasi-Constructors to be used in Java">
  protected Region() {

  }

  protected Region(boolean isScreenUnion) {
    this.isScreenUnion = isScreenUnion;
  }

  public static Region create(int X, int Y, int W, int H) {
    return Region.create(X, Y, W, H, null);
  }

  private static Region create(int X, int Y, int W, int H, IScreen scr) {
    return new Region(X, Y, W, H, scr);
  }

  public static Region create(Location loc, int w, int h) {
    int _x = loc.x;
    int _y = loc.y;
    IScreen s = loc.getScreen();
    if (s == null) {
      _x = _y = 0;
      s = Screen.getPrimaryScreen();
    }
    return Region.create(_x, _y, w, h, s);
  }

  /**
   * Flag for the {@link #create(Location, int, int, int, int)} method. Sets the Location to be on the left corner of
   * the new Region.
   */
  public final static int CREATE_X_DIRECTION_LEFT = 0;
  /**
   * Flag for the {@link #create(Location, int, int, int, int)} method. Sets the Location to be on the right corner of
   * the new Region.
   */
  public final static int CREATE_X_DIRECTION_RIGHT = 1;
  /**
   * Flag for the {@link #create(Location, int, int, int, int)} method. Sets the Location to be on the top corner of the
   * new Region.
   */
  public final static int CREATE_Y_DIRECTION_TOP = 0;
  /**
   * Flag for the {@link #create(Location, int, int, int, int)} method. Sets the Location to be on the bottom corner of
   * the new Region.
   */
  public final static int CREATE_Y_DIRECTION_BOTTOM = 1;

  /**
   * create a region with a corner at the given point<br>as specified with x y<br> 0 0 top left<br> 0 1 bottom left<br>
   * 1 0 top right<br> 1 1 bottom right<br>
   *
   * @param loc                the refence point
   * @param create_x_direction == 0 is left side !=0 is right side
   * @param create_y_direction == 0 is top side !=0 is bottom side
   * @param w                  the width
   * @param h                  the height
   * @return the new region
   */
  public static Region create(Location loc, int create_x_direction, int create_y_direction, int w, int h) {
    int _x = loc.x;
    int _y = loc.y;
    IScreen s = loc.getScreen();
    if (s == null) {
      _x = _y = 0;
      s = Screen.getPrimaryScreen();
    }
    int X;
    int Y;
    int W = w;
    int H = h;
    if (create_x_direction == CREATE_X_DIRECTION_LEFT) {
      if (create_y_direction == CREATE_Y_DIRECTION_TOP) {
        X = _x;
        Y = _y;
      } else {
        X = _x;
        Y = _y - h;
      }
    } else {
      if (create_y_direction == CREATE_Y_DIRECTION_TOP) {
        X = _x - w;
        Y = _y;
      } else {
        X = _x - w;
        Y = _y - h;
      }
    }
    return Region.create(X, Y, W, H, s);
  }

  /**
   * create a region with a corner at the given point<br>as specified with x y<br> 0 0 top left<br> 0 1 bottom left<br>
   * 1 0 top right<br> 1 1 bottom right<br>same as the corresponding create method, here to be naming compatible with
   * class Location
   *
   * @param loc the refence point
   * @param x   ==0 is left side !=0 is right side
   * @param y   ==0 is top side !=0 is bottom side
   * @param w   the width
   * @param h   the height
   * @return the new region
   */
  public static Region grow(Location loc, int x, int y, int w, int h) {
    return Region.create(loc, x, y, w, h);
  }

  /**
   * Create a region from a Rectangle
   *
   * @param r the Rectangle
   * @return the new region
   */
  public static Region create(Rectangle r) {
    return Region.create(r.x, r.y, r.width, r.height, null);
  }

  /**
   * Create a region from a Rectangle on a given Screen
   *
   * @param r            the Rectangle
   * @param parentScreen the new parent screen
   * @return the new region
   */
  protected static Region create(Rectangle r, IScreen parentScreen) {
    return Region.create(r.x, r.y, r.width, r.height, parentScreen);
  }

  /**
   * Create a region from another region<br>including the region's settings
   *
   * @param r the region
   * @return then new region
   */
  public static Region create(Region r) {
    Region reg = Region.create(r.x, r.y, r.w, r.h, r.getScreen());
    reg.autoWaitTimeout = r.autoWaitTimeout;
    reg.findFailedResponse = r.findFailedResponse;
    reg.throwException = r.throwException;
    return reg;
  }

  /**
   * create a region with the given point as center and the given size
   *
   * @param loc the center point
   * @param w   the width
   * @param h   the height
   * @return the new region
   */
  public static Region grow(Location loc, int w, int h) {
    int _x = loc.x;
    int _y = loc.y;
    IScreen s = loc.getScreen();
    if (s == null) {
      _x = _y = 0;
      s = Screen.getPrimaryScreen();
    }
    int X = _x - (int) w / 2;
    int Y = _y - (int) h / 2;
    return Region.create(X, Y, w, h, s);
  }

  /**
   * create a minimal region at given point with size 1 x 1
   *
   * @param loc the point
   * @return the new region
   */
  public static Region grow(Location loc) {
    int _x = loc.x;
    int _y = loc.y;
    IScreen s = loc.getScreen();
    if (s == null) {
      _x = _y = 0;
      s = Screen.getPrimaryScreen();
    }
    return Region.create(_x, _y, 1, 1, s);
  }

  //</editor-fold>

  /**
   * Setting, how to react if an image is not found {@link FindFailed}
   */
  private FindFailedResponse findFailedResponse = FindFailed.defaultFindFailedResponse;
  private Object findFailedHandler = FindFailed.getFindFailedHandler();
  private Object imageMissingHandler = FindFailed.getImageMissingHandler();
  /**
   * Setting {@link Settings}, if exception is thrown if an image is not found
   */
  private boolean throwException = SX.isOption("Settings.ThrowException");
  /**
   * Default time to wait for an image {@link Settings}
   */
  double autoWaitTimeout = SX.getOptionNumber("Settings.AutoWaitTimeout");
  private float waitScanRate = (float) SX.getOptionNumber("Settings.WaitScanRate");
  /**
   * Flag, if an observer is running on this region {@link Settings}
   */
  private float observeScanRate = (float) SX.getOptionNumber("Settings.ObserveScanRate");
  private int repeatWaitTime = (int) SX.getOptionNumber("Settings.RepeatWaitTime");

  /**
   * The last found {@link Match} in the Region
   */
  private Match lastMatch = null;
  /**
   * The last found {@link Match}es in the Region
   */
  private Iterator<Match> lastMatches = null;
  private long lastSearchTime = -1;
  private long lastFindTime = -1;
  private boolean isScreenUnion = false;
  private boolean isVirtual = false;
  private long lastSearchTimeRepeat = -1;

  public double getAutoWaitTimeout() {
    return autoWaitTimeout;
  }

  public void setAutoWaitTimeout(double autoWaitTimeout) {
    this.autoWaitTimeout = autoWaitTimeout;
  }

  public boolean hasMatch() {
    return SX.isNotNull(lastMatch);
  }

  public Match getLastMatch() {
    return lastMatch;
  }

  public Rectangle getRect() {
    return new Rectangle(x, y, w, h);
  }

  public Rectangle getBounds() {
    return getRect();
  }

  public <PSI> Match find(PSI target) throws FindFailed {
    if (getAutoWaitTimeout() > 0) {
      return wait(target, getAutoWaitTimeout());
    }
    lastMatch = null;
    Element where = toElement(this);
    Do.find(toFindTarget(target), where);
    if (where.hasMatch()) {
      lastMatch = new Match(where.getLastMatch());
      return lastMatch;
    }
    throw new FindFailed(String.format("%s in %s", target, this));
  }

  public void wait(double timeout) {
    try {
      Thread.sleep((long) (timeout * 1000L));
    } catch (InterruptedException e) {
    }
  }

  public <PSI> Match wait(PSI target) throws FindFailed {
    if (target instanceof Float || target instanceof Double) {
      wait(0.0 + ((Double) target));
      return null;
    }
    return wait(target, getAutoWaitTimeout());
  }

  public <PSI> Match wait(PSI target, double timeout) throws FindFailed {
    lastMatch = null;
    Element where = toElement(this);
    Do.wait(toFindTarget(target), where, timeout);
    if (where.hasMatch()) {
      return new Match(where.getLastMatch());
    }
    throw new FindFailed(String.format("%s in %s", target, this));
  }

  public <PSI> Match exists(PSI target) {
    return exists(target, getAutoWaitTimeout());
  }

  public <PSI> Match exists(PSI target, double timeout) {
    lastMatch = null;
    Element where = toElement(this);
    Do.wait(toFindTarget(target), where, timeout);
    return new Match(where.getLastMatch());
  }

  public <PSI> boolean waitVanish(PSI target) {
    return waitVanish(target, getAutoWaitTimeout());
  }

  public <PSI> boolean waitVanish(PSI target, double timeout) {
    Element where = toElement(this);
    Do.wait(toFindTarget(target), this, timeout);
    return where.hasVanish();
  }

  private static Element toElement(Region reg) {
    return new Element(reg.x, reg.y, reg.w, reg.h);
  }

  private static <PSI> Object toFindTarget(PSI target) {
    if (target instanceof String) {
      return target;
    }
    if (target instanceof Image) {
      ((Image) target).getName();
    }
    if (target instanceof Pattern) {
      return ((Pattern) target).getName();
    }
    return new Element();
  }

  public <PSI> Iterator<Match> findAll(PSI target) throws FindFailed {
    List<Match> matches = new ArrayList<>();
    lastMatches = null;
    Element where = toElement(this);
    List<Element> matchElements = Do.findAll(toFindTarget(target), where);
    if (matchElements.size() == 0) {
      throw new FindFailed(String.format("%s in %s", target, this));
    }
    for (Element elem : matchElements) {
      matches.add(new Match(elem));
    }
    return new IteratorMatch(matches);
  }

  private class IteratorMatch implements Iterator<Match> {

    List<Match> matches = null;

    public IteratorMatch(List<Match> matches) {
      this.matches = matches;
    }

    @Override
    public boolean hasNext() {
      return matches.size() > 0;
    }

    @Override
    public Match next() {
      if (hasNext()) {
        return (Match) matches.remove(0);
      }
      return null;
    }

    @Override
    public void remove() {
    }
  }

  //<editor-fold defaultstate="collapsed" desc="spatial operators - new regions">

  /**
   * check if current region contains given region
   *
   * @param region the other Region
   * @return true/false
   */
  public boolean contains(Region region) {
    return getRect().contains(region.getRect());
  }

  /**
   * create a Location object, that can be used as an offset taking the width and hight of this Region
   *
   * @return a new Location object with width and height as x and y
   */
  public Location asOffset() {
    return new Location(w, h);
  }

  /**
   * create region with same size at top left corner offset
   *
   * @param loc use its x and y to set the offset
   * @return the new region
   */
  public Region offset(Location loc) {
    return Region.create(x + loc.x, y + loc.y, w, h, scr);
  }

  /**
   * create region with same size at top left corner offset
   *
   * @param x horizontal offset
   * @param y vertical offset
   * @return the new region
   */
  public Region offset(int x, int y) {
    return Region.create(this.x + x, this.y + y, w, h, scr);
  }

  private static int defaultPadding = (int) SX.getOptionNumber("Settings.DefaultPadding", 2);

  /**
   * create a region enlarged Settings.DefaultPadding pixels on each side
   *
   * @return the new region
   * @deprecated to be like AWT Rectangle API use grow() instead
   */
  @Deprecated
  public Region nearby() {
    return grow(defaultPadding, defaultPadding);
  }

  /**
   * create a region enlarged range pixels on each side
   *
   * @param range the margin to be added around
   * @return the new region
   * @deprecated to be like AWT Rectangle API use grow() instaed
   */
  @Deprecated
  public Region nearby(int range) {
    return grow(range, range);
  }

  /**
   * create a region enlarged n pixels on each side (n = Settings.DefaultPadding = 50 default)
   *
   * @return the new region
   */
  public Region grow() {
    return grow(defaultPadding, defaultPadding);
  }

  /**
   * create a region enlarged range pixels on each side
   *
   * @param range the margin to be added around
   * @return the new region
   */
  public Region grow(int range) {
    return grow(range, range);
  }

  /**
   * create a region enlarged w pixels on left and right side and h pixels at top and bottom
   *
   * @param w pixels horizontally
   * @param h pixels vertically
   * @return the new region
   */
  public Region grow(int w, int h) {
    Rectangle r = getRect();
    r.grow(w, h);
    return Region.create(r.x, r.y, r.width, r.height, scr);
  }

  /**
   * create a region enlarged l pixels on left and r pixels right side and t pixels at top side and b pixels a bottom
   * side. negative values go inside (shrink)
   *
   * @param l add to the left
   * @param r add to right
   * @param t add above
   * @param b add beneath
   * @return the new region
   */
  public Region grow(int l, int r, int t, int b) {
    return Region.create(x - l, y - t, w + l + r, h + t + b, scr);
  }

  /**
   * point middle on right edge
   *
   * @return point middle on right edge
   */
  public Location rightAt() {
    return rightAt(0);
  }

  /**
   * positive offset goes to the right. might be off current screen
   *
   * @param offset pixels
   * @return point with given offset horizontally to middle point on right edge
   */
  public Location rightAt(int offset) {
    return new Location(x + w + offset, y + h / 2);
  }

  /**
   * create a region right of the right side with same height. the new region extends to the right screen border<br>
   * use grow() to include the current region
   *
   * @return the new region
   */
  public Region right() {
    int distToRightScreenBorder = getScreen().getX() + getScreen().getW() - (getX() + getW());
    return right(distToRightScreenBorder);
  }

  /**
   * create a region right of the right side with same height and given width. negative width creates the right part
   * with width inside the region<br>
   * use grow() to include the current region
   *
   * @param width pixels
   * @return the new region
   */
  public Region right(int width) {
    int _x;
    if (width < 0) {
      _x = x + w + width;
    } else {
      _x = x + w;
    }
    return Region.create(_x, y, Math.abs(width), h, scr);
  }

  /**
   * @return point middle on left edge
   */
  public Location leftAt() {
    return leftAt(0);
  }

  /**
   * negative offset goes to the left <br>might be off current screen
   *
   * @param offset pixels
   * @return point with given offset horizontally to middle point on left edge
   */
  public Location leftAt(int offset) {
    return new Location(x + offset, y + h / 2);
  }

  /**
   * create a region left of the left side with same height<br> the new region extends to the left screen border<br> use
   * grow() to include the current region
   *
   * @return the new region
   */
  public Region left() {
    int distToLeftScreenBorder = getX() - getScreen().getX();
    return left(distToLeftScreenBorder);
  }

  /**
   * create a region left of the left side with same height and given width<br>
   * negative width creates the left part with width inside the region use grow() to include the current region <br>
   *
   * @param width pixels
   * @return the new region
   */
  public Region left(int width) {
    int _x;
    if (width < 0) {
      _x = x;
    } else {
      _x = x - width;
    }
    return Region.create(getScreen().getBounds().intersection(new Rectangle(_x, y, Math.abs(width), h)), scr);
  }

  /**
   * @return point middle on top edge
   */
  public Location aboveAt() {
    return aboveAt(0);
  }

  /**
   * negative offset goes towards top of screen <br>might be off current screen
   *
   * @param offset pixels
   * @return point with given offset vertically to middle point on top edge
   */
  public Location aboveAt(int offset) {
    return new Location(x + w / 2, y + offset);
  }

  /**
   * create a region above the top side with same width<br> the new region extends to the top screen border<br> use
   * grow() to include the current region
   *
   * @return the new region
   */
  public Region above() {
    int distToAboveScreenBorder = getY() - getScreen().getY();
    return above(distToAboveScreenBorder);
  }

  /**
   * create a region above the top side with same width and given height<br>
   * negative height creates the top part with height inside the region use grow() to include the current region
   *
   * @param height pixels
   * @return the new region
   */
  public Region above(int height) {
    int _y;
    if (height < 0) {
      _y = y;
    } else {
      _y = y - height;
    }
    return Region.create(getScreen().getBounds().intersection(new Rectangle(x, _y, w, Math.abs(height))), scr);
  }

  /**
   * @return point middle on bottom edge
   */
  public Location belowAt() {
    return belowAt(0);
  }

  /**
   * positive offset goes towards bottom of screen <br>might be off current screen
   *
   * @param offset pixels
   * @return point with given offset vertically to middle point on bottom edge
   */
  public Location belowAt(int offset) {
    return new Location(x + w / 2, y + h - offset);
  }

  /**
   * create a region below the bottom side with same width<br> the new region extends to the bottom screen border<br>
   * use grow() to include the current region
   *
   * @return the new region
   */
  public Region below() {
    int distToBelowScreenBorder = getScreen().getY() + getScreen().getH() - (getY() + getH());
    return below(distToBelowScreenBorder);
  }

  /**
   * create a region below the bottom side with same width and given height<br>
   * negative height creates the bottom part with height inside the region use grow() to include the current region
   *
   * @param height pixels
   * @return the new region
   */
  public Region below(int height) {
    int _y;
    if (height < 0) {
      _y = y + h + height;
    } else {
      _y = y + h;
    }
    return Region.create(x, _y, w, Math.abs(height), scr);
  }

  /**
   * create a new region containing both regions
   *
   * @param ur region to unite with
   * @return the new region
   */
  public Region union(Region ur) {
    Rectangle r = getRect().union(ur.getRect());
    return Region.create(r.x, r.y, r.width, r.height, scr);
  }

  /**
   * create a region that is the intersection of the given regions
   *
   * @param ir the region to intersect with like AWT Rectangle API
   * @return the new region
   */
  public Region intersection(Region ir) {
    Rectangle r = getRect().intersection(ir.getRect());
    return Region.create(r.x, r.y, r.width, r.height, scr);
  }

  //</editor-fold>

}
