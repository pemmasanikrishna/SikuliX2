/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.sikulix.api.Element;
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

  public static class ElementFlat {

    int x = 0;
    int y = 0;
    int w = 0;
    int h = 0;

    ElementFlat lastMatch = null;
    double score = -1;

    int[] target = null;

    Element.eType clazz = Element.eType.ELEMENT;

    String name = null;

    public ElementFlat(Element element) {
      clazz = element.getType();
      x = element.x;
      y = element.y;
      w = element.w;
      h = element.h;
      if (element.hasName()) {
        name = element.getName();
      }
      if (element.isRectangle()) {
        if (element.hasMatch()) {
          Element match = element.getLastMatch();
          lastMatch = new ElementFlat(match);
          lastMatch.score = match.getScore();
          lastMatch.target = new int[]{match.getTarget().x, match.getTarget().y};
        }
      }
      target = new int[]{element.getTarget().x, element.getTarget().x};
    }

    public String getType() {
      return clazz.toString();
    }

    public int getX() {
      return x;
    }

    public int getY() {
      return y;
    }

    public int getW() {
      return w;
    }

    public int getH() {
      return h;
    }

    public String getName() { return name; }

    public ElementFlat getLastMatch() {
      return lastMatch;
    }

    public Double getScore() {
      return score;
    }

    public int[] getTarget() {
      return target;
    }
  }

  public static JSONObject makeElement(Element elem) {
    return new SXJson(new ElementFlat(elem)).theJsonObject;
  }

  public static JSONObject makeObject(String strJson) {
    return new SXJson(strJson).theJsonObject;
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

//TODO Base64
//  private String toBase64(Mat image) {
//    MatOfByte mobImage = new MatOfByte();
//    Highgui.imencode("png", image, mobImage);
//    byte[] bImage = mobImage.toArray();
//    String b64Image = Base64.getEncoder().encodeToString(bImage);
//    return b64Image;
//  }
//
//  private Mat fromBase64(String image) {
//    byte[] bImage = Base64.getDecoder().decode(image);
//    Mat mImage = new Mat();
//    mImage.put(0,0,bImage);
//    return Highgui.imdecode(mImage, CV_LOAD_IMAGE_COLOR);
//  }
}
