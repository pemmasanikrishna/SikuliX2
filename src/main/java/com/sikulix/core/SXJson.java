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

  public static class SXElementFlat {

    int x = 0;
    int y = 0;
    int w = 0;
    int h = 0;

    SXElementFlat lastMatch = null;
    double score = 0;

    int[] target = null;

    SXElement.eType clazz = SXElement.eType.ELEMENT;

    public SXElementFlat(Element vis) {
      clazz = vis.getType();
      x = vis.x;
      y = vis.y;
      w = vis.w;
      h = vis.h;
      if (vis.isRectangle()) {
        Element match = vis.getLastMatch();
        if (SX.isNotNull(match)) {
          lastMatch = new SXElementFlat(match);
        }
      }
      score = vis.getScore();
      target = new int[]{vis.getTarget().x, vis.getTarget().x};
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

    public SXElementFlat getLastMatch() {
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
    return new SXJson(new SXElementFlat(elem)).theJsonObject;
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
