/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

public class Screen extends Visual {

  private static vType vClazz = vType.SCREEN;
  private static SXLog log = SX.getLogger(vClazz.toString());
  //clazz = vClazz;

  private int sNum = 0;

  public Screen() {
    clazz = vClazz;
    init(0, 0, 0, 0);
  }

  public Screen(int n) {
    clazz = vClazz;
    sNum = n;
    init(0, 0, 0, 0);
  }
}
