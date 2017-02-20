package com.sikulix.core;

import com.sikulix.api.Element;
import com.sikulix.api.Picture;

import java.awt.*;

public abstract class IDevice {

  public static enum Action {
    LEFT, LEFTDOWN, LEFTUP, LEFTDOUBLE,
    RIGHT, RIGHTDOWN, RIGHTUP, RIGHTDOUBLE,
    MIDDLE, MIDDLEDOWN, MIDDLEUP, MIDDLEDOUBLE,
    UP, DOWN
  }

  public abstract IDevice start(Object... args);
  public abstract void stop();

  public abstract int getNumberOfMonitors();
  public abstract Rectangle getMonitor(int... id);
  public abstract Rectangle getAllMonitors();
  public abstract int getMainMonitorID();
  public abstract void resetMonitors();
  public abstract Element click(Element loc);
  public abstract Element doubleClick(Element loc);
  public abstract Element rightClick(Element loc);
  public abstract Element click(Action action);
  public abstract Element click(Element loc, Action action);
  public abstract Element dragDrop(Element from, Element to, Object... times);

  /**
   * move the mouse from the current position to the offset given by the parameters
   *
   * @param xoff horizontal offset (&lt; 0 left, &gt; 0 right)
   * @param yoff vertical offset (&lt; 0 up, &gt; 0 down)
   * @return the new mouseposition as Element (might be invalid)
   */
  public abstract Element move(int xoff, int yoff);

  /**
   * move the mouse to the target of given Element (default center)
   *
   * @param loc
   * @return the new mouseposition as Element (might be invalid)
   */
  public abstract Element move(Element loc);

  /**
   * @return the current mouseposition as Element (might be invalid)
   */
  public abstract Element at();

  public abstract void button(Action action);

  public abstract void wheel(Action action, int steps);

  public abstract Picture capture(Element what);
}
