/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.util;

import com.sikulix.api.Do;
import com.sikulix.api.Element;
import com.sikulix.api.Picture;
import com.sikulix.core.NativeHook;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

public class SXMain {

  static {
    System.setProperty("sikulix.logging", "trace");
  }

  static SXLog log;

  static String stars = repeat("*", 50);

  public static String repeat(String str, int count) {
    return String.format("%0" + 50 + "d", 0).replace("0", "*");
  }

  private static void traceBlock(String message) {
    log.trace(stars);
    log.trace("*****   %s", message);
    log.trace(stars);
  }

  public static void main(String[] args) {
    log = SX.getLogger("SX.Main");
    log.trace("main: start: %s", "parameter");

    traceBlock("testing: native libraries");
    Do.setBaseClass();

    traceBlock("testing: NativeHook");
    if (!SX.isHeadless()) {
      NativeHook hook = NativeHook.start();
      SX.pause(1);
      hook.stop();
      SX.pause(1);
      log.trace("NativeHook works");
    } else {
      log.trace("headless: NativeHook not tested");
    }

    traceBlock("testing: bundlePath in jar");
    Do.setBundlePath("./Images");
    log.trace("bundlePath: %s", Do.getBundlePath());

    traceBlock("testing: load image from jar");
    Picture img = new Picture("sikulix2");
    boolean imgOK = img.hasContent();
    img.show(3);

    traceBlock("testing: find image in other image");
    Picture base = new Picture("shot-tile");
    boolean baseOK = base.hasContent();
    if (baseOK && imgOK) {
      Element element = new Element();
      element = Do.find(img, base);
      if (element.isMatch()) {
        base.showMatch();
      }
    }

    traceBlock("testing: find image on primary monitor");
    if (baseOK && imgOK) {
      Element element = new Element();
      base.showContent(3);
      element = Do.find(img);
      if (element.isMatch()) {
        SX.getMain().showMatch();
      }
    }
  }
}
