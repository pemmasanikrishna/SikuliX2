/*
 * Copyright 2010-2014, Sikuli.org, sikulix.com
 * Released under the MIT License.
 *
 * modified RaiMan
 */
package org.sikuli.script;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;


public class Pattern extends com.sikulix.api.Pattern {

  private static SXLog log = SX.getLogger("API.Pattern");

  private Pattern() {
  }

  //TODO org.sikuli.script.Pattern: Constructor not implemented
  public Pattern(Object obj) {
    SX.terminate(1, "org.sikuli.script.Pattern: Constructor not implemented");
  }
}
