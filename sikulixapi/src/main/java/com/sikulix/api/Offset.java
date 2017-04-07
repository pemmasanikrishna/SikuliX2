/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.core.SXElement;

public class Offset extends SXElement {

  private static eType eClazz = eType.OFFSET;
  private static SXLog log = SX.getLogger("SX." + eClazz.toString());

  public Offset() {
    clazz = eClazz;
    init(0, 0, 0, 0);
  }

  public Offset(int x, int y) {
    this();
    init(x, y, 0, 0);
  }

  public Offset(SXElement elem) {
    this();
    init(elem.getCenter());
  }

  public Offset(SXElement elemFrom, SXElement elemTo) {
    this();
    init(elemTo.getCenter().x - elemFrom.getCenter().x, elemTo.getCenter().y - elemFrom.getCenter().y, 0, 0);
  }
}
