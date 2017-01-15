/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package org.sikuli.script;

import com.sikulix.api.Element;
import com.sikulix.api.Target;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

public class Pattern extends Target {
  private static eType eClazz = eType.PATTERN;
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
