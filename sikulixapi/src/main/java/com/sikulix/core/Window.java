/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

public class Window extends Visual {

  private static vType vClazz = vType.WINDOW;
  private static SXLog log = SX.getLogger("SX." + vClazz.toString());
  //clazz = vClazz;

  public Window() {
    clazz = vClazz;
    init(0, 0, 0, 0);
  }
}
