/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import org.opencv.core.Mat;

import java.awt.Color;

public class Symbol extends Element {

  private static eType eClazz = eType.SYMBOL;
  private static SXLog log = SX.getLogger("SX." + eClazz.toString());

  //<editor-fold desc="*** construction">
  protected void setClazz() {
    clazz = eClazz;
  }

  protected void copy(Element elem) {
    if (elem.hasContent()) {
      setContent(elem.getContent().clone());
    } else {
      setContent();
    }
    urlImg = elem.urlImg;
    setName(elem.getName());
    setAttributes();
  }

  protected void initAfter() {
    initName(eClazz);
  }

  private void setAttributes() {

  }

  public enum Type {
    RECTANGLE, CIRCLE, LINE, IMAGE, TEXT, BUTTON;
  }

  public boolean isActive() {
    return Type.BUTTON.equals(type);
  }

  public Symbol() {}

  public Symbol(int w, int h) {
    this();
    init(-1, -1, w, h);
  }

  public static Symbol button(int w, int h) {
    return new Symbol(w, h).setType(Type.BUTTON).setName();
  }

  public static Symbol rectangle(int w, int h) {
    return new Symbol(w, h).setName();
  }

  public static Symbol square(int w) {
    return new Symbol(w, w).setName();
  }

  public static Symbol circle(int diameter) {
    return new Symbol(diameter, diameter).setType(Type.CIRCLE).setName();
  }

  public static Symbol ellipse(int w, int h) {
    return new Symbol(w, h).setType(Type.CIRCLE).setName();
  }

  public Symbol setType(Type type) {
    this.type = type;
    return this;
  }

  private Type type = Type.RECTANGLE;

  public Symbol setName(String name) {
    super.setName(name);
    return this;
  }

  public Symbol setName() {
    return setName(this.type.toString());
  }

  @Override
  public boolean isRectangle() {
    return Type.RECTANGLE.equals(type) || Type.BUTTON.equals(type);
  }

  public boolean isButton() {
    return Type.BUTTON.equals(type);
  }

  public boolean isCircle() {
    return Type.CIRCLE.equals(type);
  }

  private Color fillColor = null;

  public Color getFillColor() {
    return fillColor;
  }

  public Symbol fill() {
    fillColor = getLineColor();
    return this;
  }

  public Symbol fill(Color color) {
    fillColor = color;
    return this;
  }

  private int line = getHighLightLine();

  public int getLine() {
    return line;
  }

  public Symbol setLine(int line) {
    this.line = line;
    return this;
  }

  @Override
  public Element getTarget() {
    return target;
  }

  @Override
  public Color getColor() {
    return color;
  }

  public Symbol setColor(Color color) {
    this.color = color;
    return  this;
  }

  private Color color = Color.lightGray;

  public Symbol at(int x, int y) {
    super.at(x, y);
    return this;
  }

  private Element elementOver = null;

  public Symbol over(Element elem) {
    elementOver = elem;
    return this;
  }

  public Element getOver() {
    return elementOver;
  }
}
