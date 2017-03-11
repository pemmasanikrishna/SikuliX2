package org.sikuli.script;

import com.sikulix.api.Element;

import java.awt.*;

public interface IScreen {

  public boolean isOtherScreen();
  public int getID();
  public Rectangle getRect();
  public int getX();
  public int getY();
  public int getW();
  public int getH();
  public Rectangle getBounds();
  public Element asElement();
}
