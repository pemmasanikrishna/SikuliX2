/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.sikulix.api.Do;
import com.sikulix.api.Element;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

public class SXShow {

  static SXLog log;

  static {
    log = SX.getLogger("SX.Show");
    log.isSX();
    log.on(SXLog.INFO);
  }

  private static float darker_factor = 0.85f;
  private static int showTime = (int) SX.getOptionNumber("SXShow.showTime", 2);

  private JFrame frame = new JFrame();
  private Element story = new Element();
  private java.util.List<Element> elements = new ArrayList<>();

  private BufferedImage storyImg = null;

  public SXShow() {
    this(Do.on());
  }

  JPanel panel = new JPanel() {
    @Override
    public void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2d = (Graphics2D) g.create();
      if (withBorder) {
        drawBorder(g2d, 0, 0, story.w, story.h, borderThickness, borderColor);
        g2d.drawImage(storyImg, borderThickness, borderThickness, null);
      } else {
        Graphics2D bg = (Graphics2D) storyImg.getGraphics().create();
        for (Element element : elements) {
          drawHighlight(bg, story, element);
        }
        g2d.drawImage(storyImg, 0, 0, null);
      }
      g2d.dispose();
    }
  };

  private static Color borderColor = Color.green;
  private static int borderThickness = 5;
  private static Color lineColor = Color.red;
  private static int lineThickness = 3;
  private boolean withBorder = false;

  public void setBorder() {
    withBorder = true;
  }

  int pauseAfter = 0;
  int pauseBefore = 0;

  public SXShow(Element story, int... times) {
    this.story = story;
    if (times.length > 0) {
      pauseAfter = times[0];
    }
    if (times.length > 1) {
      pauseBefore = times[1];
    }
    waitForFrame = 0;
    String contentLoaded = "with content";
    if (story.hasContent()) {
      storyImg = story.get();
    } else {
      storyImg = Do.on().capture(story).get();
      contentLoaded = "captured";
    }
    frame.setUndecorated(true);
    frame.setAlwaysOnTop(false);
    log.trace("init: %s: %s", contentLoaded, story);
  }

  //<editor-fold desc="painting">
  private static void drawHighlight(Graphics2D g2d, Element currentBase, Element elem) {
    drawBorder(g2d, elem.x - currentBase.x, elem.y - currentBase.y, elem.w, elem.h, lineThickness,
            SX.isNull(lineColor) ? elem.getHighlightColor() : lineColor);
    g2d.setColor(Color.white);
    int rectW = 50;
    int rectH = 20;
    int rectX = elem.x - currentBase.x;
    int rectY = elem.y - currentBase.y - lineThickness - rectH;
    int margin = 2;
    int fontSize = rectH - margin * 2;
    g2d.fillRect(rectX, rectY, rectW, rectH);
    g2d.setColor(Color.black);
    g2d.setFont(new Font(Font.DIALOG, Font.BOLD, fontSize));
    double hlScore = elem.isMatch() ? Math.min(elem.getScore(), 0.9999) : 0;
    g2d.drawString(String.format("%05.2f", 100 * hlScore), rectX + margin, rectY + fontSize);
  }

  private static void drawBorder(Graphics2D g2d, int x, int y, int w, int h, int thickness, Color color) {
    x = Math.max(0 + thickness/2, x - thickness/2);
    y = Math.max(0 + thickness/2, y - thickness/2);
    w = w + (int) (1.5 * thickness);
    h = h + (int) (1.5 * thickness);
    g2d.setStroke(new BasicStroke(thickness));
    g2d.setColor(color);
    g2d.drawLine(x, y, x + w, y);
    g2d.drawLine(x + w, y, x + w, y + h);
    g2d.drawLine(x + w, y + h, x, y + h);
    g2d.drawLine(x, y + h, x, y);
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
  private ShowIt showThread = null;

  private class ShowIt implements Runnable {
    private Object shouldStop = new Object();

    @Override
    public void run() {
      Element location = new Element();
      Element onElement = Do.on();
      panel.setPreferredSize(new Dimension(story.w + (withBorder ? borderThickness * 2 : 0),
              story.h + (withBorder ? borderThickness * 2 : 0)));
      if (story.w < onElement.w || story.h < onElement.h) {
        location = story.getCentered(onElement, (withBorder ? new Element(-borderThickness) : null));
      }
      Container contentPane = frame.getContentPane();
      contentPane.setLayout(new OverlayLayout(contentPane));
      contentPane.add(panel);
      JLabel ovl = new JLabel();
      ovl.setPreferredSize(new Dimension(1, 1));
      contentPane.add(ovl);
      frame.pack();
      frame.setLocation(location.x, location.y);
      frame.setVisible(true);
      try {
        synchronized (shouldStop) {
          shouldStop.wait();
        }
      } catch (InterruptedException e) {
      }
      frame.dispose();
    }

    public void stop() {
      synchronized (shouldStop) {
        shouldStop.notify();
      }
    }
  }

  public void setWaitForFrame() {
    this.waitForFrame = 2;
  }

  public void resetWaitForFrame() {
    this.waitForFrame = 0;
  }

  private int waitForFrame = 0;

  public SXShow start() {
    log.trace("starting");
    SX.pause(pauseBefore);
    showThread = new ShowIt();
    new Thread(showThread).start();
    SX.pause(waitForFrame);
    if (pauseAfter > 0) {
      new Thread(new ShowStop(showThread, pauseAfter)).start();
    }
    return this;
  }

  public void stop() {
    showThread.stop();
    log.trace("stopping");
  }

  private class ShowStop implements Runnable {
    private ShowIt showThread = null;
    private int waitTime;

    public ShowStop(ShowIt showThread, int waitTime) {
      this.showThread = showThread;
      this.waitTime = waitTime;
    }
    @Override
    public void run() {
      SX.pause(waitTime);
      showThread.stop();
    }
  }

  public void show() {
    show(Do.on(), showTime);
  }

  public void show(long time) {
    show(Do.on(), time);
  }

  private void show(Element story, long time) {
    waitForFrame = 0;
    start();
    SX.pause(time);
    stop();
  }

  public Element whereShowing() {
    return new Element(frame.getLocation().x, frame.getLocation().y, frame.getWidth(), frame.getHeight());
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
