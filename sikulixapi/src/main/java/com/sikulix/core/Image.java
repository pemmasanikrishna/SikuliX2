/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

public class Image extends Visual {

  private static vType vClazz = vType.IMAGE;
  private static SXLog log = SX.getLogger(vClazz.toString());

  public Image() {
    clazz = vClazz;
    init(0, 0, 0, 0);
  }
}
