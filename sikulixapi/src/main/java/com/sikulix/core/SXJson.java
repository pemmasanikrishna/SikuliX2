/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import org.json.JSONArray;
import org.json.JSONObject;

public class SXJson {

  static SXLog log;

  static {
    log = SX.getLogger("SXJson");
    log.isSX();
    log.on(SXLog.TRACE);
  }

  JSONObject theJsonObject = null;
  JSONArray theJsonArray = null;

  public SXJson() {
    theJsonObject = new JSONObject(JSONObject.NULL);
  }

  public SXJson(String strJson) {
    try {
      theJsonObject = new JSONObject(strJson);
    } catch (Exception e) {
      log.error("SXJson(String): not possible: %s", e.getMessage());
    }
  }

  public SXJson(JSONObject objJson) {
    theJsonObject = objJson;
  }

  public SXJson(Object bean) {
    try {
      theJsonObject = new JSONObject(bean);
    } catch (Exception e) {
      log.error("SXJson(bean): not possible: %s", e.getMessage());
    }
  }

  public static JSONObject makeBean(Object bean) {
    return new SXJson(bean).theJsonObject;
  }

  public static SXJson asArray(Object toArray) {
    SXJson theJson = new SXJson();
    try {
      if (toArray instanceof String) {
        theJson.setArray(new JSONArray((String) toArray));
      } else {
        JSONArray anArray = new JSONArray(toArray);
        theJson.setArray(anArray);
      }
    } catch (Exception e) {
      log.error("JSONArray: not possible: %s", e.getMessage());
    }
    return theJson;
  }

  private SXJson setArray(JSONArray theArray) {
    theJsonArray = theArray;
    return this;
  }

  public JSONArray getArray() {
    return theJsonArray;
  }

  public boolean isArray() {
    return SX.isNotNull(theJsonArray);
  }

}
