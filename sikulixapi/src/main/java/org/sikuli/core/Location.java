/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package org.sikuli.core;

public class Location extends Visual {
  private static String logStamp = "Location";

  Location() {
    init(logStamp, 0, 0, -1, -1);
  }

  Location(int x, int y) {
    init(logStamp, x, y, -1, -1);
  }
}
