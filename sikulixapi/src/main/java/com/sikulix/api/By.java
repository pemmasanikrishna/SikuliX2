/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

/**
 * to be like Selenium (to be implemented!)
 */
//TODO implement By
public class By {

  static Element id(String id) {
    return name(id);
  }

  static Element name(String name) {
    return new Element();
  }

  static Element pattern(Target pattern) {
    return new Element();
  }

  static Element text(String text) {
    return new Element();
  }

  static Element all(Object... objs) {
    return new Element();
  }

  static Element any(Object... objs) {
    return new Element();
  }
}
