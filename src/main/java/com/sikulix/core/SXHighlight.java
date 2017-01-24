/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.sikulix.api.Element;
import com.sikulix.api.Picture;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.*;
import java.util.List;

public class SXHighlight {

  static SXLog log;

  static {
    log = SX.getLogger("SX.Capture");
    log.isSX();
    log.on(SXLog.INFO);
  }

  private static java.util.List<Element> currentHighlights = new ArrayList<>();
  private static Map<Element, java.util.List<Element>> highlights = new HashMap<>();

  private static BufferedImage baseImg = null;
  private static JFrame frame = new JFrame();
  private static Color lineColor = null;
  private static int lineThickness = 5;
  private static float darker_factor = 0.85f;
  private static int showTime = 2;

  private static SXHighlight instance = null;

  private SXHighlight() {
  }

  public static SXHighlight forElement(Element base) {
    if (SX.isNull(instance)) {
      instance = new SXHighlight();
      frame.setUndecorated(true);
      frame.setAlwaysOnTop(true);
      JPanel panel = new JPanel() {
        public void paintComponent(Graphics g) {
          Element currentBase = null;
          for (Element base : highlights.keySet()) {
            currentBase = base;
            if (highlights.get(base).equals(currentHighlights)) {
              break;
            }
          }
          super.paintComponent(g);
          Graphics2D g2d = (Graphics2D) g.create();
          g2d.drawImage(baseImg, null, 0, 0);
          g2d.setStroke(new BasicStroke(lineThickness));
          for (Element highlight : currentHighlights) {
            drawHighlight(g2d, currentBase, highlight);
          }
          g2d.dispose();
        }
      };
      Element location = new Element();
      if (base.w < SX.getMain().w || base.h < SX.getMain().h) {
        location = base.getCentered(SX.getMain(), new Element(-lineThickness));
      }
      panel.setPreferredSize(new Dimension(base.w, base.h));
      frame.add(panel);
      frame.pack();
      frame.setLocation(location.x, location.y);
      log.trace("init: %s", base);
    }
    addBase(base);
    return instance;
  }

  private static List<Element> addBase(Element base) {
    if (!highlights.containsKey(base)) {
      highlights.put(base, new ArrayList<Element>());
    }
    return highlights.get(base);
  }

  //<editor-fold desc="painting">
  private static void drawHighlight(Graphics2D g2d, Element currentBase, Element elem) {
    g2d.setColor(SX.isNull(lineColor) ? elem.getHighlightColor() : lineColor);
    int x = elem.x - currentBase.x - lineThickness;
    int y = elem.y - currentBase.y - lineThickness;
    int w = elem.w + 2 * lineThickness;
    int h = elem.h + 2 * lineThickness;
    g2d.setStroke(new BasicStroke(lineThickness));
    g2d.drawLine(x, y, x + w, y);
    g2d.drawLine(x + w, y, x + w, y + h);
    g2d.drawLine(x + w, y + h, x, y + h);
    g2d.drawLine(x, y + h, x, y);
    g2d.setColor(Color.white);
    int rectW = 50;
    int rectH = 20;
    int rectX = x;
    int rectY = y - lineThickness - rectH;
    int margin = 2;
    int fontSize = rectH - margin * 2;
    g2d.fillRect(rectX, rectY, rectW, rectH);
    g2d.setColor(Color.black);
    g2d.setFont(new Font(Font.DIALOG, Font.BOLD, fontSize));
    double hlScore = elem.isMatch() ? Math.min(elem.getScore(), 0.9999) : 0;
    g2d.drawString(String.format("%05.2f", 100 * hlScore), rectX + margin, rectY + fontSize);
  }

  public Color getLineColor() {
    return lineColor;
  }

  public void setLineColor(Color lineColor) {
    this.lineColor = lineColor;
  }

  public int getLineThickness() {
    return lineThickness;
  }

  public void setLineThickness(int lineThickness) {
    this.lineThickness = lineThickness;
  }
  //</editor-fold>

  //<editor-fold desc="handling">
  public void on() {
    if (!highlights.containsKey(SX.getMain())) {
      on(SX.getMain());
    }
  }

  public void on(Element base) {
    if (highlights.containsKey(base)) {
      RescaleOp darkOverlay = new RescaleOp(darker_factor, 0, null);
      if (base.isPicture()) {
        baseImg = darkOverlay.filter(((Picture) base).get(), null);
      } else {
        baseImg = SX.getMain().capture(base).get();
      }
    }
  }

  private static void show(long time) {
    frame.setVisible(true);
    SX.pause(2);
    frame.setVisible(false);
  }

  public void add(Element elem) {
    add(elem, elem.getHighlightColor());
  }

  public void add(Element elem, Color highlightColor) {
    if (!highlights.containsKey(SX.getMain())) {
      on(SX.getMain());
    }
    add(SX.getMain(), elem, highlightColor);
  }

  public void add(Element base, Element elem) {
    add(base, elem, elem.getHighlightColor());
  }

  public void add(Element base, Element elem, Color highlightColor) {
    Element hlElem = new Element(elem);
    hlElem.setScore(elem.getScore());
    hlElem.setHighlightColor(highlightColor);
    hlElem.setShowTime(elem.getShowTime() > 0 ? elem.getShowTime() : showTime);
    addBase(base).add(elem);
  }

  public void stop() {
    frame.dispose();
  }
  //</editor-fold>

}
