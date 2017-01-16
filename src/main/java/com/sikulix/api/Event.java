/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

public class Event {
  private static SXLog log = SX.getLogger("SX.SXEvent");
  private static String klazz = Event.class.getName();

  public enum TYPE {
    NOTSET, FINDFAILED, IMAGEMISSING, ONAPPEAR, ONVANISH, ONCHANGE, GENERIC
  }

  public enum REACTION {
    NOTSET, ABORT, SKIP, RETRY, PROMPT, CAPTURE, HANDLE
  }

  TYPE type = TYPE.NOTSET;
  REACTION reaction = REACTION.NOTSET;
  Element elem = null;
  Handler handler = null;

  private Event() {
  }

  public Event(TYPE type, REACTION reaction) {
    if (TYPE.FINDFAILED.equals(type) || TYPE.IMAGEMISSING.equals(type)) {
      this.type = type;
      if (!REACTION.HANDLE.equals(reaction)) {
        this.reaction = reaction;
      }
    }
  }

  public Event(TYPE type, Handler handler) {
    this.type = type;
    this.reaction = REACTION.HANDLE;
    this.handler = handler;
  }

  public Event(TYPE type, Element elem, Handler handler) {
    if (isOnEvent()) {
      this.type = type;
      this.reaction = REACTION.HANDLE;
      this.elem = elem;
      this.handler = handler;
    }
  }

  public boolean isValid() {
    return !TYPE.NOTSET.equals(type);
  }

  public boolean isOnEvent() {
    return type.toString().startsWith("ON");
  }

  public Event handle() {
    if (SX.isNotNull(handler)) {
      handler.handle(this);
    }
    return this;
  }

}
