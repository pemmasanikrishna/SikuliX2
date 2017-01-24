/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.sikulix.api.Do;
import com.sikulix.api.Element;
import com.sikulix.api.Picture;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.*;

public class SXShow {

  static SXLog log;

  static {
    log = SX.getLogger("SX.Show");
    log.isSX();
    log.on(SXLog.INFO);
  }

  private static Color lineColor = Color.red;
  private static int lineThickness = 5;
  private static float darker_factor = 0.85f;
  private static int showTime = 2;

  private JFrame frame = new JFrame();
  private Element story = new Element();
  private java.util.List<Element> elements = new ArrayList<>();
  private BufferedImage storyImg = null;

  public SXShow() {
    this(Do.on());
  }

  public SXShow(Element story) {
    frame.setUndecorated(true);
    frame.setAlwaysOnTop(true);
    JPanel panel = new JPanel() {
      public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.drawImage(storyImg, null, 0, 0);
        g2d.setStroke(new BasicStroke(lineThickness));
        for (Element element : elements) {
          drawHighlight(g2d, getStory(), element);
        }
        g2d.dispose();
      }
    };
    Element location = new Element();
    Element onElement = Do.on();
    if (story.w < onElement.w || story.h < onElement.h) {
      location = story.getCentered(onElement, new Element(-lineThickness));
    }
    panel.setPreferredSize(new Dimension(story.w, story.h));
    frame.add(panel);
    frame.pack();
    frame.setLocation(location.x, location.y);
    log.trace("init: %s", story);
    this.story = story;
  }

  //<editor-fold desc="painting">
  private Element getStory() {
    return story;
  }

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

  //<editor-fold desc="showing">
  public void start() {
    if (story.isValid()) {
//      RescaleOp darkOverlay = new RescaleOp(darker_factor, 0, null);
      if (story.isPicture()) {
//        storyImg = darkOverlay.filter(((Picture) story).get(), null);
        storyImg = ((Picture) story).get();
      } else {
        storyImg = Do.on().capture(story).get();
      }
      frame.setVisible(true);
    }
  }

  public void stop() {
    frame.dispose();
  }

  public void show() {
    show(Do.on(), showTime);
  }

  public void show(long time) {
    show(Do.on(), time);
  }

  private void show(Element story, long time) {
    start();
    SX.pause(time);
    stop();
  }
  //</editor-fold>

  //<editor-fold desc="handling">
  public SXShow add(Element elem) {
    return add(elem, SX.isNull(elem.getHighlightColor()) ? lineColor : elem.getHighlightColor());
  }

  public SXShow add(Element elem, Color lineColor) {
    Element hlElem = new Element(elem);
    hlElem.setScore(elem.getScore());
    hlElem.setHighlightColor(lineColor);
    hlElem.setShowTime(elem.getShowTime() > 0 ? elem.getShowTime() : showTime);
    elements.add(elem);
    return this;
  }
  //</editor-fold>
}
