package org.sikuli.script;

import java.util.Iterator;

public class AFinder implements Iterator<Match> {
  
  public void destroy() {}
  
  @Override
  public boolean hasNext() {
    return true;
  }
  
  @Override
  public Match next() {
    return null;
  }
  
  public String find(String string) {
    return null;
  }

  public String find(Pattern pattern) {
    return null;
  }

  public String find(Image image) {
    return null;
  }

  public String findText(String text) {
    return null;
  }
  
  public String findAll(String string) {
    return null;
  }

  public String findAll(Pattern pattern) {
    return null;
  }

  public String findAll(Image image) {
    return null;
  }

  public String findAllText(String text) {
    return null;
  }

  protected void setScreenImage(ScreenImage simg) {}

  protected void setRepeating() {}

  protected void findRepeat() {}
  
  protected void findAllRepeat() {}

  public void setFindTimeout(double t) {}
}
