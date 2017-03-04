/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.LocalDevice;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
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

  private java.util.List<ShowElement> elements = new ArrayList<>();
  private java.util.List<Symbol> activeElements = new ArrayList<>();
  private Symbol activeElement = null;
  private boolean shouldClose = false;
  private boolean canDrag = false;
  private boolean isDragging = false;
  private Element dragStart = null;
  private Element lastDrag = null;
  private Element currentDrag = null;
  private Element storyTopLeft = null;
  private LocalDevice localDevice;

  private Element storyBackground = new Element();
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
      canDrag = true;
      //addBorder();
    } else if (storyBackground.hasContent()) {
      storyImg = storyBackground.get();
    } else {
      storyImg = storyBackground.capture().get();
      contentLoaded = "captured";
    }
    localDevice = new LocalDevice().start();
    log.trace("init: %s: %s", contentLoaded, storyBackground);
  }
  //</editor-fold>

  //<editor-fold desc="attributes">
  private boolean withBorder = true;

  public void setBorder() {
    withBorder = true;
  }

  private boolean shouldAddBorder = false;

  public void addBorder() {
    shouldAddBorder = true;
  }

  private static Color defaultBorderColor = Color.green;
  private Color borderColor = defaultBorderColor;

  public Color getBorderColor() {
    return borderColor;
  }

  public void setBorderColor(Color borderColor) {
    this.borderColor = borderColor;
  }

  private static int defaultBorderThickness = 6;
  private int borderThickness = defaultBorderThickness;

  public int getBorderThickness() {
    return borderThickness;
  }

  public void setBorderThickness(int borderThickness) {
    this.borderThickness = borderThickness;
  }

  protected static Color defaultlineColor = Color.red;
  private static int defaultLineThickness = 3;

  private Color labelColor = new Color(200, 200, 200);

  public Color getLabelColor() {
    return labelColor;
  }

  public void setLabelColor(Color labelColor) {
    this.labelColor = labelColor;
  }

  int pauseAfter = 0;
  int pauseBefore = 0;
  //</editor-fold>

  //<editor-fold desc="showing">
  public Story start() {
    log.trace("starting");
    showThread = new ShowIt(pauseBefore);
    showThread.running = true;
    SwingUtilities.invokeLater(showThread);
    if (pauseBefore == 0) {
      while (!showThread.isVisible()) {
        SX.pause(0.3);
      }
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

  public void waitForEnd() {
    while (isRunning()) SX.pause(0.3);
  }

  public boolean isRunning() {
    return showThread.running;
  }
  //</editor-fold>

  //<editor-fold desc="story frame">
  private ShowIt showThread = null;

  private class ShowIt implements Runnable {
    public boolean running = false;
    private JFrame frame = new JFrame();
    private int storyWidth, storyHeight;
    private int waitBefore = 0;

    public ShowIt() {
    }

    public ShowIt(int waitBefore) {
      this.waitBefore = waitBefore;
    }

    private String logStoryFrame() {
      return String.format("[%d,%d %dx%d]", storyTopLeft.x, storyTopLeft.y, storyWidth, storyHeight);
    }

    @Override
    public void run() {
      frame.setUndecorated(true);
      frame.setAlwaysOnTop(false);

      frame.addMouseListener(new MouseInputAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          super.mousePressed(e);
          if (canDrag) {
            Element loc = new Element(e.getPoint());
            log.trace("pressed at: (%d,%d) story: %s", loc.x, loc.y, logStoryFrame());
            dragStart = loc;
          }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
          super.mouseReleased(e);
          Element loc = new Element(e.getPoint());
          log.trace("released at: (%d,%d) story: %s", loc.x, loc.y, logStoryFrame());
          activeElement = null;
          if (!isDragging) {
            if (activeElements.size() > 0) {
              for (Symbol symbol : activeElements) {
                if (symbol.contains(loc)) {
                  activeElement = symbol;
                }
                log.trace("clicked active symbol: %s story: %s", getClickedSymbol(), logStoryFrame());
              }
            }
            stop();
          } else {
            currentDrag = localDevice.at();
            storyTopLeft.x = currentDrag.x - dragStart.x;
            storyTopLeft.y = currentDrag.y - dragStart.y;
            frame.setLocation(storyTopLeft.x, storyTopLeft.y);
            frame.repaint();
            log.trace("end dragging at: (%d,%d) story: %s", loc.x, loc.y, logStoryFrame());
            SX.pause(0.5);
          }
          dragStart = null;
          isDragging = false;
        }
      });

      frame.addMouseMotionListener(new MouseMotionAdapter() {
        @Override
        public void mouseDragged(MouseEvent e) {
          super.mouseDragged(e);
          if (!canDrag) {
            return;
          }
          String dragState = "continue";
          if (!isDragging) {
            isDragging = true;
            lastDrag = dragStart;
            dragState = "start";
          }
          currentDrag = localDevice.at();
          storyTopLeft.x = currentDrag.x - dragStart.x;
          storyTopLeft.y = currentDrag.y - dragStart.y;
//          currentDrag = new Element(e.getPoint());
//          storyTopLeft.x += (currentDrag.x - dragStart.x);
//          storyTopLeft.y += (currentDrag.y - dragStart.y);
          frame.setLocation(storyTopLeft.x, storyTopLeft.y);
          frame.repaint();
          log.trace("%s dragging at (%d,%d) story: %s", dragState, currentDrag.x, currentDrag.y, logStoryFrame());
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
          for (ShowElement element : elements) {
            drawElement(bg, storyBackground, element);
          }
          if (withBorder) {
            drawBorderInside(g2d, 0, 0, storyBackground.w, storyBackground.h, borderThickness, borderColor);
            g2d.drawImage(storyImg, borderThickness, borderThickness, null);
          } else {
            g2d.drawImage(storyImg, 0, 0, null);
          }
          g2d.dispose();
        }
      };
      boolean hasBorder = withBorder || shouldAddBorder;
      Element location = new Element();
      Element onElement = Do.on();
      panel.setPreferredSize(new Dimension(storyBackground.w + borderThickness * 2,
              storyBackground.h + borderThickness * 2));
      if (panel.getPreferredSize().getWidth() < onElement.w &&
              panel.getPreferredSize().getHeight() < onElement.h) {
        location = storyBackground.getCentered(onElement, new Element(-borderThickness));
      } else {
        panel.setPreferredSize(new Dimension(storyBackground.w, storyBackground.h));
        withBorder = false;
      }
      Container contentPane = frame.getContentPane();
      contentPane.setLayout(new OverlayLayout(contentPane));
      contentPane.add(panel);
      JLabel ovl = new JLabel();
      ovl.setPreferredSize(new Dimension(1, 1));
      contentPane.add(ovl);
      frame.pack();
      frame.setLocation(location.x, location.y);
      storyWidth = (int) panel.getPreferredSize().getWidth();
      storyHeight = (int) panel.getPreferredSize().getHeight();
      if (waitBefore > 0) {
        new Thread(new ShowWaitBefore(frame, waitBefore)).start();
      } else {
        frame.setVisible(true);
        log.trace("Story frame visible");
      }
    }

    public void stop() {
      if (running) {
        frame.dispose();
        running = false;
        log.trace("Story frame disposed");
      }
    }

    //<editor-fold desc="painting">
    public boolean isVisible() {
      return frame.isVisible();
    }

    public Element whereShowing() {
      return new Element(frame.getLocation().x, frame.getLocation().y, frame.getWidth(), frame.getHeight());
    }

    private void drawElement(Graphics2D g2d, Element currentBase, ShowElement showElement) {
      Element elem = showElement.getWhat();
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
        drawBorderAround(g2d, elem.x - currentBase.x, elem.y - currentBase.y, elem.w, elem.h,
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

    private int BORDERINSIDE = 0;
    private int BORDERAROUND = 1;

    private void drawBorderInside(Graphics2D g2d, int x, int y, int w, int h, int thickness, Color color) {
      drawBorder(BORDERINSIDE, g2d, x, y, w, h, thickness, color);
    }

    private void drawBorderAround(Graphics2D g2d, int x, int y, int w, int h, int thickness, Color color) {
      drawBorder(BORDERAROUND, g2d, x, y, w, h, thickness, color);
    }

    private void drawBorder(int whereBorder, Graphics2D g2d, int x, int y, int w, int h, int thickness, Color color) {
      if (whereBorder == BORDERINSIDE) {
        x = 0 + thickness / 2;
        y = 0 + thickness / 2;
      }
      if (whereBorder == BORDERAROUND) {
        x = x - thickness / 2;
        y = y - thickness / 2;
      }
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
      log.trace("Story pause after visible before stop: %d", waitTime);
      SX.pause(waitTime);
      showThread.stop();
    }
  }

  private class ShowWaitBefore implements Runnable {
    private Frame frame = null;
    private int waitTime;

    public ShowWaitBefore(Frame frame, int waitTime) {
      this.frame = frame;
      this.waitTime = waitTime;
    }

    @Override
    public void run() {
      log.trace("Story pause before visible after start: %d", waitTime);
      SX.pause(waitTime);
      frame.setVisible(true);
      log.trace("Story frame visible");
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
      elements.add(new ShowElement(elem));
      if (((Symbol) elem).isActive()) {
        activeElements.add((Symbol) elem);
      }
      return this;
    }
    return add(elem, SX.isNull(elem.getLineColor()) ? defaultlineColor : elem.getLineColor());
  }

  public Story add(Element elem, Color lineColor) {
    Element element = new Element(elem);
    element.setScore(elem.getScore());
    element.setLineColor(lineColor);
    element.setShowTime(elem.getShowTime() > 0 ? elem.getShowTime() : showTime);
    elements.add(new ShowElement(elem));
    return this;
  }

  private class ShowElement {
    Element what = null;
    Element where = null;

    public ShowElement(Element what) {
      this.what = what;
    }

    public Element getWhat() { return what; }

    //<editor-fold desc="housekeeping">
    private Color lineColor = defaultlineColor;

    public Color getLineColor() {
      return lineColor;
    }

    public void setLineColor(Color lineColor) {
      this.lineColor = lineColor;
    }

    private int lineThickness = defaultLineThickness;

    public int getLineThickness() {
      return lineThickness;
    }

    public void setLineThickness(int lineThickness) {
      this.lineThickness = lineThickness;
    }
    //</editor-fold>
  }
  //</editor-fold>
}
