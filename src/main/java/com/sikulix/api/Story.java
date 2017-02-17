/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.*;

public class Story {

  static SXLog log;

  static {
    log = SX.getLogger("SX.STORY");
    log.isSX();
    log.on(SXLog.INFO);
  }

  //<editor-fold desc="housekeeping">
  private static float darker_factor = 0.85f;
  private static int showTime = (int) SX.getOptionNumber("SXShow.showTime", 2);

  private Element storyBackground = new Element();
  private java.util.List<Element> elements = new ArrayList<>();
  private java.util.List<Symbol> activeElements = new ArrayList<>();
  private Symbol activeElement = null;
  private boolean shouldClose = false;
  private boolean canDrag = false;
  private boolean isDragging = false;
  private Element dragStart = null;
  private Element storyTopLeft = null;
  private Element storyTopLeftStart = null;

  private BufferedImage storyImg = null;

  public Story() {
    this(Do.on());
  }

  public Story(Element background, int... times) {
    storyBackground = background;
    if (times.length > 0) {
      pauseAfter = times[0];
    }
    if (times.length > 1) {
      pauseBefore = times[1];
    }
    waitForFrame = 0;
    String contentLoaded = "with content";
    if (storyBackground.isSymbol()) {
      storyImg = plainBackground(storyBackground);
      storyTopLeft = storyBackground.getCentered(borderThickness);
      add(storyBackground);
      if (storyBackground.isSymbol()) {
        canDrag = true;
      }
      addBorder();
    } else if (storyBackground.isPicture()) {
      storyImg = storyBackground.get();
    } else {
      storyImg = storyBackground.capture().get();
      contentLoaded = "captured";
    }
    log.trace("init: %s: %s", contentLoaded, storyBackground);
  }
  //</editor-fold>

  //<editor-fold desc="attributes">
  private boolean withBorder = false;

  public void setBorder() {
    withBorder = true;
  }

  private boolean shouldAddBorder = false;

  public void addBorder() {
    shouldAddBorder = true;
  }

  private static Color borderColor = Color.green;

  public static Color getBorderColor() {
    return borderColor;
  }

  public static void setBorderColor(Color borderColor) {
    Story.borderColor = borderColor;
  }

  private static int borderThickness = 6;

  public static int getBorderThickness() {
    return borderThickness;
  }

  public static void setBorderThickness(int borderThickness) {
    Story.borderThickness = borderThickness;
  }

  private static Color lineColor = Color.red;

  public static Color getLineColor() {
    return lineColor;
  }

  public static void setLineColor(Color lineColor) {
    Story.lineColor = lineColor;
  }

  private int lineThickness = 3;
  private static int defaultlineThickness = 3;

  public int getLineThickness() {
    return lineThickness;
  }

  public void setLineThickness(int lineThickness) {
    this.lineThickness = lineThickness;
  }

  private static Color labelColor = new Color(200, 200, 200);

  public static Color getLabelColor() {
    return labelColor;
  }

  public static void setLabelColor(Color labelColor) {
    Story.labelColor = labelColor;
  }

  int pauseAfter = 0;
  int pauseBefore = 0;
  //</editor-fold>

  //<editor-fold desc="showing">
  public Story start() {
    log.trace("starting");
    SX.pause(pauseBefore);
    showThread = new ShowIt();
    showThread.running = true;
//    new Thread(showThread).start();
//    SX.pause(waitForFrame);
    SwingUtilities.invokeLater(showThread);
    while (!showThread.isVisible()) {
      SX.pause(0.3);
    }
    if (pauseAfter > 0) {
      new Thread(new ShowStop(showThread, pauseAfter)).start();
    }
    return this;
  }

  public void stop() {
    showThread.stop();
    log.trace("stopping");
  }

  public void show() {
    show(showTime);
  }

  public void show(long time) {
    waitForFrame = 0;
    start();
    showThread.running = true;
    long end = new Date().getTime() + time * 1000;
    while (new Date().getTime() < end) {
      SX.pause(0.5);
      if (!showThread.running) {
        return;
      }
    }
    stop();
  }

  public boolean isRunning() {
    return showThread.running;
  }
  //</editor-fold>

  //<editor-fold desc="story frame">
  private ShowIt showThread = null;

  private class ShowIt implements Runnable {
    private Object shouldStop = new Object();
    public boolean running = false;
    private JFrame frame = new JFrame();

    @Override
    public void run() {
      frame.setUndecorated(true);
      frame.setAlwaysOnTop(false);
      frame.addMouseListener(new MouseInputAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          super.mousePressed(e);
          Element loc = new Element(e.getPoint());
          loc.setName("MousePosition");
          log.trace("pressed at: %s", loc);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
          super.mouseReleased(e);
          Element loc = new Element(e.getPoint());
          loc.setName("MousePosition");
          log.trace("released at: %s", loc);
          activeElement = null;
          if (!isDragging) {
            if (activeElements.size() > 0) {
              for (Symbol symbol : activeElements) {
                if (symbol.contains(loc)) {
                  activeElement = symbol;
                }
                log.trace("clicked active symbol: %s", getClickedSymbol());
              }
            }
            stop();
          } else {
            if (SX.isNotNull(storyTopLeftStart)) {
              int xStart = storyTopLeftStart.x + dragStart.x;
              int yStart = storyTopLeftStart.y + dragStart.y;
              int xEnd = storyTopLeftStart.x + loc.x;
              int yEnd = storyTopLeftStart.y + loc.y;
              frame.setLocation(storyTopLeftStart.x + xEnd - xStart, storyTopLeftStart.y + yEnd - yStart);
              frame.repaint();
              SX.pause(0.5);
            }
            isDragging = false;
            dragStart = null;
          }
        }
      });
      frame.addMouseMotionListener(new MouseMotionAdapter() {
        @Override
        public void mouseDragged(MouseEvent e) {
          super.mouseDragged(e);
          if (!canDrag) {
            return;
          }
          Element loc = new Element(e.getPoint());
          if (!isDragging) {
            isDragging = true;
            dragStart = loc;
            storyTopLeftStart = new Element(storyTopLeft.x, storyTopLeft.y);
          } else {
            int xStart = storyTopLeft.x + dragStart.x;
            int yStart = storyTopLeft.y + dragStart.y;
            int xEnd = storyTopLeft.x + loc.x;
            int yEnd = storyTopLeft.y + loc.y;
            storyTopLeft.x += xEnd - xStart;
            storyTopLeft.y += yEnd - yStart;
            frame.setLocation(storyTopLeft.x, storyTopLeft.y);
            frame.repaint();
          }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
          super.mouseMoved(e);
        }
      });
      JPanel panel = new JPanel() {
        @Override
        public void paintComponent(Graphics g) {
          super.paintComponent(g);
          Graphics2D g2d = (Graphics2D) g.create();
          Graphics2D bg = (Graphics2D) storyImg.getGraphics().create();
          for (Element element : elements) {
            drawElement(bg, storyBackground, element);
          }
          drawBorder(g2d, 0, 0, storyBackground.w, storyBackground.h, borderThickness, borderColor);
          g2d.drawImage(storyImg, borderThickness, borderThickness, null);
          g2d.dispose();
        }
      };
      boolean hasBorder = withBorder || shouldAddBorder;
      Element location = new Element();
      Element onElement = Do.on();
      panel.setPreferredSize(new Dimension(storyBackground.w + borderThickness * 2,
              storyBackground.h + borderThickness * 2 ));
      if (storyBackground.w < onElement.w || storyBackground.h < onElement.h) {
        location = storyBackground.getCentered(onElement, new Element(-borderThickness));
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
    }

    public void stop() {
//      synchronized (shouldStop) {
//        shouldStop.notify();
//      }
      frame.dispose();
      running = false;
      //SX.pause(0.5);
    }

    //<editor-fold desc="painting">
    public boolean isVisible() {
      return frame.isVisible();
    }

    public Element whereShowing() {
      return new Element(frame.getLocation().x, frame.getLocation().y, frame.getWidth(), frame.getHeight());
    }

    private void drawElement(Graphics2D g2d, Element currentBase, Element elem) {
      if (elem.isSymbol()) {
        Symbol symbol = (Symbol) elem;
        if (symbol.isRectangle() || symbol.isCircle() || symbol.isButton()) {
          g2d.setColor(symbol.getColor());
          int stroke = symbol.getLine();
          g2d.setStroke(new BasicStroke(stroke));
          Element topLeft = new Element(symbol.x, symbol.y);
          if (topLeft.x < 0 || topLeft.y < 0) {
            topLeft = symbol.getCentered(currentBase);
          }
          if (SX.isNotNull(symbol.getOver())) {
            topLeft = symbol.getCentered(symbol.getOver());
          }
          if (symbol.isRectangle() || symbol.isButton()) {
            g2d.drawRect(topLeft.x, topLeft.y, symbol.w, symbol.h);
            if (SX.isNotNull(symbol.getFillColor())) {
              g2d.setColor(symbol.getFillColor());
              g2d.fillRect(topLeft.x + stroke / 2, topLeft.y + stroke / 2,
                      symbol.w - 2 * (stroke / 2), symbol.h - 2 * (stroke / 2));
            }
          } else if (symbol.isCircle()) {
            g2d.drawArc(topLeft.x, topLeft.y, symbol.w, symbol.h, 0, 360);
            if (SX.isNotNull(symbol.getFillColor())) {
              g2d.setColor(symbol.getFillColor());
              g2d.fillArc(topLeft.x + stroke / 2, topLeft.y + stroke / 2,
                      symbol.w - 2 * (stroke / 2), symbol.h - 2 * (stroke / 2), 0, 360);
            }
          }
          symbol.x = topLeft.x;
          symbol.y = topLeft.y;
        }
      } else {
        drawBorder(g2d, elem.x - currentBase.x, elem.y - currentBase.y, elem.w, elem.h,
                elem.getHighLightLine(), elem.getLineColor());
        if (elem.isMatch()) {
          g2d.setColor(labelColor);
          int rectW = 50;
          int rectH = 20;
          int rectX = elem.x - currentBase.x;
          int rectY = elem.y - currentBase.y - elem.getHighLightLine() - rectH;
          if (rectY < 30) {
            rectY += elem.h + rectH + 3 * elem.getHighLightLine();
          }
          int margin = 2;
          int fontSize = rectH - margin * 2;
          g2d.fillRect(rectX, rectY, rectW, rectH);
          g2d.setColor(Color.black);
          g2d.setFont(new Font(Font.DIALOG, Font.BOLD, fontSize));
          double hlScore = elem.isMatch() ? Math.min(elem.getScore(), 0.9999) : 0;
          g2d.drawString(String.format("%05.2f", 100 * hlScore), rectX + margin, rectY + fontSize);
        }
      }
    }

    private void drawBorder(Graphics2D g2d, int x, int y, int w, int h, int thickness, Color color) {
      x = Math.max(0 + thickness / 2, x - thickness / 2);
      y = Math.max(0 + thickness / 2, y - thickness / 2);
      w = w + thickness;
      h = h + thickness;
      g2d.setStroke(new BasicStroke(thickness));
      g2d.setColor(color);
      g2d.drawLine(x, y, x + w, y);
      g2d.drawLine(x + w, y, x + w, y + h);
      g2d.drawLine(x + w, y + h, x, y + h);
      g2d.drawLine(x, y + h, x, y);
    }
    //</editor-fold>
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

  public void setWaitForFrame() {
    this.waitForFrame = 2;
  }

  public void resetWaitForFrame() {
    this.waitForFrame = 0;
  }

  private int waitForFrame = 0;

  //</editor-fold>

  //<editor-fold desc="handling">
  private BufferedImage plainBackground(Element elem) {
    BufferedImage bImg = new BufferedImage(elem.w, elem.h, BufferedImage.TYPE_3BYTE_BGR);
    Graphics2D graphics = bImg.createGraphics();
    graphics.setPaint(new Color(255, 255, 255));
    graphics.fillRect(0, 0, bImg.getWidth(), bImg.getHeight());
    return bImg;
  }

  public Element whereShowing() {
    return showThread.whereShowing();
  }

  public Symbol getClickedSymbol() {
    Symbol clicked = new Symbol(activeElement);
    clicked.at(whereShowing());
    return clicked;
  }

  public boolean hasClickedSymbol() {
    return SX.isNotNull(activeElement);
  }

  public Story add(Element elem) {
    if (elem.isSymbol()) {
      elements.add(elem);
      if (((Symbol) elem).isActive()) {
        activeElements.add((Symbol) elem);
      }
      return this;
    }
    return add(elem, SX.isNull(elem.getLineColor()) ? lineColor : elem.getLineColor());
  }

  public Story add(Element elem, Color lineColor) {
    Element element = new Element(elem);
    element.setScore(elem.getScore());
    element.setLineColor(lineColor);
    element.setShowTime(elem.getShowTime() > 0 ? elem.getShowTime() : showTime);
    elements.add(elem);
    return this;
  }
  //</editor-fold>
}
