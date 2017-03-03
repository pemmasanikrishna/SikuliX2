/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package org.sikuli.script;

import com.sikulix.api.Element;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

public class Match extends Region {
  private static SXLog log = SX.getLogger("API.MATCH");

  public  Match(Element elem) {
    x = elem.x;
    y = elem.y;
    w = elem.w;
    h = elem.h;
    setScore(elem.getScore());
    setTarget(elem.getTarget());
  }

}
