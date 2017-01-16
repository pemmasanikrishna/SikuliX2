/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package org.sikuli.script;

import com.sikulix.api.Do;
import com.sikulix.api.Element;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import java.util.Iterator;
import java.util.List;

public class Region extends Element {
  private static eType eClazz = eType.REGION;
  private static SXLog log = SX.getLogger("SX." + eClazz.toString());

  protected void setClazz() {
    clazz = eClazz;
  }

  protected void copy(Element elem) {
    super.copy(elem);
  }

  protected void initAfter() {
    initName(eClazz);
  }

  public <PSI> Match find(PSI target) throws FindFailed {
    if (getAutoWaitTimeout() > 0) {
      return wait(target, getAutoWaitTimeout());
    }
    Do.find(target, this);
    if (hasMatch()) {
      return (Match) getLastMatch();
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
    Do.wait(target, this, timeout);
    if (hasMatch()) {
      return (Match) getLastMatch();
    }
    throw new FindFailed(String.format("%s in %s", target, this));
  }

  public <PSI> Match exists(PSI target) {
    return exists(target, getAutoWaitTimeout());
  }

  public <PSI> Match exists(PSI target, double timeout) {
    Do.wait(target, this, timeout);
    return (Match) getLastMatch();
  }

  public <PSI> boolean waitVanish(PSI target) {
    return waitVanish(target, getAutoWaitTimeout());
  }

  public <PSI> boolean waitVanish(PSI target, double timeout) {
    Do.wait(target, this, timeout);
    return hasVanish();
  }

  public <PSI> Iterator<Match> findAll(PSI target) throws FindFailed {
    List<Element> matches = Do.findAll(target);
    if (matches.size() == 0) {
      throw new FindFailed(String.format("%s in %s", target, this));
    }
    return new IteratorMatch(matches);
  }

  private class IteratorMatch implements Iterator<Match> {

    List<Element> matches = null;

    public IteratorMatch(List<Element> matches) {
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
}
