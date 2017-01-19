/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.sikulix.api.Element;
import com.sikulix.api.Picture;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import org.jnativehook.keyboard.NativeKeyEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.ArrayList;

public class SXHighlight {

  static SXLog log;

  static {
    log = SX.getLogger("SX.Capture");
    log.isSX();
    log.on(SXLog.INFO);
  }

  private java.util.List<Element> highlights = new ArrayList<>();

  private Picture base = null;
  private BufferedImage baseImg = null;
  private BufferedImage baseDisplayed = null;
  private JFrame frame = new JFrame();

  public SXHighlight(Picture base) {
    this.base = base;
    float darker_factor = 0.85f;
    RescaleOp op = new RescaleOp(darker_factor, 0, null);
    baseImg = op.filter(base.get(), null);
    frame.setUndecorated(true);
    frame.setAlwaysOnTop(true);
    init();
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

  private Color lineColor = null;
  private int lineThickness = 3;
  private Element location = new Element();

  private void init() {
    JPanel panel = new JPanel() {
      public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.drawImage(baseImg, null, 0, 0);
        g2d.setStroke(new BasicStroke(lineThickness));
        for (Element highlight : highlights ) {
          drawHighlight(g2d, highlight);
        }
        g2d.dispose();
      }
    };
    if (base.w < SX.getMain().w || base.h < SX.getMain().h) {
      location = base.getCentered(SX.getMain(), new Element(-lineThickness));
    }
    panel.setPreferredSize(new Dimension(base.w, base.h));
    frame.add(panel);
    frame.pack();
    frame.setLocation(location.x, location.y);
    log.trace("init: %s", base);
  }

  private void drawHighlight(Graphics2D g2d, Element elem) {
    g2d.setColor(SX.isNull(lineColor) ? elem.getHighlightColor() : lineColor);
    int x = elem.x - base.x - lineThickness;
    int y = elem.y - base.y - lineThickness;
    int w = elem.w + 2 * lineThickness;
    int h = elem.h + 2 * lineThickness;
    g2d.drawLine(x, y, x+w, y);
    g2d.drawLine(x + w, y, x + w, y + h);
    g2d.drawLine(x + w, y + h, x, y + h);
    g2d.drawLine(x, y + h , x, y);
  }

  public void on() {
    frame.setVisible(true);
    SX.pause(2);
    frame.setVisible(false);
  }

  public void add(Element elem) {
    add(elem, elem.getHighlightColor());
  }

  public void add(Element elem, Color highlightColor) {
    Element hlElem = new Element(elem);
    hlElem.setScore(elem.getScore());
    hlElem.setHighlightColor(highlightColor);
    highlights.add(elem);
  }

  public void stop() {
    frame.dispose();
  }

}
