/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

public class Handler {
  private static SXLog log = SX.getLogger("SX.Handler");

//  public Handler(Object handler) {
//    this.handler = handler;
//  }
//
//  public void handle(Event e) {
//    if (e.type.equals(Event.TYPE.ONAPPEAR)) {
//      appeared(e);
//    } else if (e.type.equals(Event.TYPE.ONVANISH)) {
//      vanished(e);
//    } else if (e.type.equals(Event.TYPE.ONCHANGE)) {
//      changed(e);
//    } else if (e.type.equals(Event.TYPE.FINDFAILED)) {
//      findFailed(e);
//    } else if (e.type.equals(Event.TYPE.IMAGEMISSING)) {
//      imageMissing(e);
//    }
//  }
//
//  public void appeared(Event e) {
//    if (SX.isNotNull(handler)) {
//      run(e);
//    }
//  }
//
//  public void vanished(Event e) {
//    if (SX.isNotNull(handler)) {
//      run(e);
//    }
//  }
//
//  public void changed(Event e) {
//    if (SX.isNotNull(handler)) {
//      run(e);
//    }
//  }
//
//  public void findFailed(Event e) {
//    if (SX.isNotNull(handler)) {
//      run(e);
//    }
//  }
//
//  public void imageMissing(Event e) {
//    if (SX.isNotNull(handler)) {
//      run(e);
//    }
//  }
//
//  public void generic(Event e) {
//    if (SX.isNotNull(handler)) {
//      run(e);
//    }
//  }

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
