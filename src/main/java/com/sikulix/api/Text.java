/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.core.TextFinder;

/**
 * implements the API for text search and OCR features<br>
 * holds all settings for a text feature processing<br>
 * and the results of this processing<br>
 * uses Core.TextFinder which implements the text features using Tesseract (via Tess4J)
 *
 */
public class Text extends Element {

  private static final SXLog log = SX.getLogger("SX.Text");

  private String searchText = null;
  private String ocrText = null;

  public Text(Object... settings) {
    //TODO what settings and how to process/store
  }

  public Text(String text) {
    searchText = text;
  }

  public Text(String text, Object... settings) {
    this(settings);
    searchText = text;
  }

  public static Text find(String text, Element where) {
    return new Text(text).find(where);
  }

  public Text find(Element where) {
    TextFinder finder = new TextFinder();
    //TODO search the text and fill lastMatch
    return this;
  }

  public static Text findAll(String text, Element where) {
    return new Text(text).findAll(where);
  }

  public Text findAll(Element where) {
    TextFinder finder = new TextFinder();
    //TODO search the text and fill lastMatches
    return this;
  }

  public static Text read(Element where, Object... settings) {
    return new Text(settings).read(where);
  }

  public Text read(Element where) {
    TextFinder finder = new TextFinder();
    //TODO read the text according to the given settings and fill ocrText
    return this;
  }

}
