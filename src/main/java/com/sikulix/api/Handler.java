/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

public class Handler {
  private static SXLog log = SX.getLogger("SX.Handler");

  public void run(Event e) {
    log.error("Event handler not implemented");
    showEvent(e);
  }

  public void showEvent(Event e) {
    String msg = "---------- Event:\n" +
            "key = %d\n" +
            "when = %d\n" +
            "what = %s\n" +
            "where = %s\n" +
            "match = %s\n" +
            "---------";
    log.p(msg, e.getKey(), e.getWhen(), e.getWhat(), e.getWhere(), e.getWhere().getLastMatch());
  }
}
