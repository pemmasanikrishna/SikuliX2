/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package org.sikuli.script;

import com.sikulix.api.Element;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

public class Match extends Region {
  private static eType eClazz = eType.MATCH;
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
}
