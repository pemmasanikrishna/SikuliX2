/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

public abstract class HotkeyListener {

  /**
   * Override this to implement your own hotkey handler.
   *
   * @param e HotkeyEvent
   */
  abstract public void hotkeyPressed(HotkeyEvent e);

  /**
   * INTERNAL USE: system specific handler implementation
   *
   * @param e HotkeyEvent
   */
  public void invokeHotkeyPressed(final HotkeyEvent e) {
    Thread hotkeyThread = new Thread() {
      @Override
      public void run() {
        hotkeyPressed(e);
      }
    };
    hotkeyThread.start();
  }
}
