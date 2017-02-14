/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package org.sikuli.script;

import com.sikulix.api.Element;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

public class Match extends Region {
  private static SXLog log = SX.getLogger("SX.MATCH");

  double score = -1;
  Location target = null;

  public  Match(Element elem) {
    x = elem.x;
    y = elem.y;
    w = elem.w;
    h = elem.h;
    score = elem.getScore();
    target = new Location(elem.getTarget().x, elem.getTarget().y);
  }

}
