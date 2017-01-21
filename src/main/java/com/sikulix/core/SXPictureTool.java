package com.sikulix.core;

import com.sikulix.api.Do;
import com.sikulix.api.Element;
import com.sikulix.api.Picture;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.awt.event.KeyEvent.VK_ESCAPE;

public class SXPictureTool {

  //<editor-fold desc="housekeeping">
  private static final SXLog log = SX.getLogger("SX.PictureTool");

  private Picture scr = null;
  private Element rect = null;
  private List<Element> lastRects = new ArrayList<>();
  private int maxRevert = 10;
  private Picture shot;
  boolean dirty = false;

  private JFrame box = null;
  private Container pBox = null;
  private BufferedImage shotDisplayed = null;
  private Dimension dimShotDisplayed = null;
  private JLabel content = new JLabel() {
    @Override
    public void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2d = (Graphics2D) g.create();
      drawSelection(g2d);
    }
  };

  private JLabel status = new JLabel();
  private int statusFontHeight = 16;
  private int statusLineWidht = 2;
  private int statusHeight = statusFontHeight + 4 + statusLineWidht;

  private int zoomStep = 10;
  private float wFactor = 0;
  private float hFactor = 0;

  final private int TOP = 0;
  final private int LEFT = 1;
  final private int BOTTOM = 2;
  final private int RIGHT = 3;
  final private int ALL = 4;
  final private int COUNTERCLOCKWISE = 5;
  final private int CLOCKWISE = 6;
  final private int OPPOSITE = 7;
  private int activeSide = TOP;
  private int activesideSaved = TOP;
  private boolean activeSideAll = false;
  private String[] activeSides = new String[]{"TOP", "LEFT", "BOTTOM", "RIGHT", "ALL"};
  private boolean shouldKeepAll = false;
  private boolean running = false;

  private int borderThickness = 5;
  private  int minWidthHeight = 5;

  MouseEvent lastDrag = null;
  MouseEvent dragStart = null;
  MouseEvent dragCurrent = null;

  public boolean isRunning() {
    return running;
  }

  public void waitFor() {
    while (isRunning()) {
      SX.pause(1);
    }
  }

  public SXPictureTool(Element rect) {
    this(0, rect);
  }

  public SXPictureTool(int scrID, Element rect) {
    running = true;
    this.rect = rect;
    //aspect = ((float) rect.w) / rect.h;
    scr = new Element(scrID).capture();
    box = new JFrame();
    box.setUndecorated(true);
    box.setResizable(false);
    if (!log.isGlobalLevel(log.TRACE)) {
      box.setAlwaysOnTop(true);
    }

    //<editor-fold desc="*** Key Mouse handler">
    box.addKeyListener(new KeyAdapter() {
      @Override
      public void keyTyped(KeyEvent e) {
        super.keyTyped(e);
        myKeyTyped(e);
      }

      @Override
      public void keyReleased(KeyEvent e) {
        super.keyReleased(e);
        myKeyReleased(e);
      }
    });
    box.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseReleased(MouseEvent e) {
        if (getActiveSide() == ALL && SX.isNotNull(dragStart)) {
          Element start = getPos(dragStart);
          Element end = getPos(dragCurrent);
          int w = Math.abs(end.x - start.x);
          int h = Math.abs(end.y - start.y);
          if (w > minWidthHeight && h > minWidthHeight) {
            pushRect();
            int rx = getRect().x;
            int ry = getRect().y;
            getRect().change(new Element(rx + Math.min(start.x, end.x), ry + Math.min(start.y, end.y), w , h));
          }
          dragStart = null;
          dragCurrent = null;
          content.repaint();
          resizeToFrame();
        }
        lastDrag = null;
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        super.mouseClicked(e);
        myMouseClicked(e);
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        super.mouseEntered(e);
        myMouseEntered(e);
      }

      @Override
      public void mouseExited(MouseEvent e) {
        super.mouseExited(e);
        myMouseExited(e);
      }
    });
    box.addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseDragged(MouseEvent e) {
        super.mouseDragged(e);
        myMouseMoved(e);
        if (getActiveSide() == ALL) {
          if (SX.isNull(lastDrag)) {
            dragStart = e;
          }
          dragCurrent = e;
          content.repaint();
        } else {
          resizeSelection(e);
        }
        lastDrag = e;
      }

      @Override
      public void mouseMoved(MouseEvent e) {
        super.mouseMoved(e);
        myMouseMoved(e);
      }
    });
    box.addMouseWheelListener(new MouseWheelListener() {
      @Override
      public void mouseWheelMoved(MouseWheelEvent e) {
        if (getActiveSide() != ALL) {
          if (e.getWheelRotation() < 0) {
            zoomIn();
          } else {
            zoomOut();
          }
        }
      }
    });
    //</editor-fold>

    pBox = box.getContentPane();
    pBox.setLayout(new BoxLayout(pBox, BoxLayout.Y_AXIS));
    pBox.setBackground(Color.white);
    status.setOpaque(true);
    status.setBackground(Color.white);
    status.setFont(new Font(Font.MONOSPACED, Font.PLAIN, statusFontHeight));
    status.setText("    ,     ");
    status.setBorder(BorderFactory.createMatteBorder(0, statusLineWidht, statusLineWidht, statusLineWidht, Color.black));
    resizeToFrame();
    content.setBorder(coloredSide(TOP));
    pBox.add(content);
    pBox.add(status);
    box.pack();
    box.setLocation((int) ((scr.w - box.getWidth()) / 2), (int) (scr.h - box.getHeight()) / 2);
    new Thread(new ClickHandler()).start();
    box.setVisible(true);
  }

  public Element getRect() {
    return rect;
  }
  //</editor-fold>

  //<editor-fold desc="mouse exit/enter/click">
  int exited = -1;

  private void myMouseMoved(MouseEvent e) {
    Element elem = getPos(new Element(e.getX(), e.getY()));
    updateStatus(elem);
  }

  private Element mousePos = new Element();

  private void updateStatus() {
    updateStatus(null);
  }

  private void updateStatus(Element elem) {
    if (!SX.isNull(elem)) {
      mousePos = elem;
    }
    status.setText(String.format("%s%04d, %04d [%04d, %04d %04dx%04d] %s ",
            dirty ? "*" : "",
            rect.x + mousePos.x, rect.y + mousePos.y, rect.x, rect.y, rect.w, rect.h, getImageName()));
  }

  private void myMouseEntered(MouseEvent e) {
    if (!isDragging()) {
      int side = whichSide(e.getX() - borderThickness, e.getY() - borderThickness);
      if (side == exited) {
        log.trace("mouseEntered: %s", activeSides[side]);
        exited = -1;
        content.setBorder(coloredSide(side));
        box.repaint();
      }
    }
  }

  private void myMouseExited(MouseEvent e) {
    if (!isDragging()) {
      exited = whichSide(e.getX() - borderThickness, e.getY() - borderThickness);
      log.trace("mouseExited: %s ", activeSides[exited]);
    } else {
      zoomOut();
    }
  }

  private int whichSide(int x, int y) {
    if (isMiddle(box.getWidth(), x) && isLow(box.getHeight(), y)) return TOP;
    if (isMiddle(box.getHeight(), y) && isLow(box.getWidth(), x)) return LEFT;
    if (isMiddle(box.getWidth(), x) && isHigh(box.getHeight(), y)) return BOTTOM;
    if (isMiddle(box.getHeight(), y) && isHigh(box.getWidth(), x)) return RIGHT;
    return TOP;
  }

  private boolean isLow(int range, int point) {
    return point < range / 4;
  }

  private boolean isMiddle(int range, int point) {
    int lower = range / 4;
    int upper = range - lower;
    return point > lower && point < upper;
  }

  private boolean isHigh(int range, int point) {
    return point > range - range / 4;
  }

  private void myMouseClicked(MouseEvent e) {
    String doubleClick = e.getClickCount() > 1 ? "Double" : "";
    log.trace("mouse%sClicked: %d,%d", doubleClick, e.getX(), e.getY());
    if (!SX.isSet(doubleClick)) {
      if ((!activeSideAll && e.getButton() < 2) || activeSideAll)
        setClickStatusClicked(new Element(e.getX(), e.getY(), e.getButton()));
    } else {
      setClickStatusDoubleClicked();
    }
  }

  //<editor-fold desc="clickHandler">
  List<Element> listElement = new ArrayList<>();
  List<Element> clickStatus = Collections.synchronizedList(listElement);

  synchronized void setClickStatusClicked(Element element) {
    if (!clickStatus.get(0).isClicked()) {
      clickStatus.get(0).setClick(element);
      clickStatus.set(1, new Element());
    }
  }

  synchronized void setClickStatusDoubleClicked() {
    if (!clickStatus.get(1).isClicked()) {
      clickStatus.set(1, clickStatus.get(0));
      clickStatus.set(0, new Element());
    }
  }

  synchronized void resetClickStatus() {
    SX.pause(0.3);
    clickStatus.set(0, new Element());
    clickStatus.set(1, new Element());
  }

  protected class ClickHandler implements Runnable {
    @Override
    public void run() {
      clickStatus.add(new Element());
      clickStatus.add(new Element());
      Element clicked = null;
      log.trace("ClickHandler: started");
      while (isRunning()) {
        if (clickStatus.get(0).isClicked()) {
          SX.pause(0.3);
          if (clickStatus.get(0).isClicked()) {
            clicked = clickStatus.get(0).getClick();
            log.trace("action: crop at (%d,%d) from %s", clicked.x, clicked.y, getActiveSideText());
            crop(clicked);
            resetClickStatus();
            rect.resetClick();
          }
        } else if (clickStatus.get(1).isClicked()) {
          clicked = clickStatus.get(1).getClick();
          log.trace("action: center at (%d,%d) %s", clicked.x, clicked.y, rect);
          center(clicked);
          resetClickStatus();
          rect.resetClick();
        }
      }
      log.trace("ClickHandler: ended");
    }
  }
  //</editor-fold>
  //</editor-fold>

  //<editor-fold desc="key typed">
  private void myKeyTyped(KeyEvent e) {
    boolean shouldQuit = false;
    if (e.CHAR_UNDEFINED != e.getKeyChar()) {
      String sKey = "" + e.getKeyChar();
      if ("+".equals("" + e.getKeyChar())) {
        zoomIn();
        log.trace("action: zoom-in to %s", rect);
      } else if ("-".equals("" + e.getKeyChar())) {
        zoomOut();
        log.trace("action: zoom-out to %s", rect);
      } else if ("s".equals("" + e.getKeyChar())) {
        log.trace("action: save request");
        dirty = false;
      } else if ("f".equals("" + e.getKeyChar())) {
        Runnable find = new Runnable() {
          @Override
          public void run() {
            log.trace("action: find: %s", scr.getLastMatch());
            Do.find(scr.getSub(rect), scr);
            SXHighlight hl = new SXHighlight(scr);
            hl.add(scr.getLastMatch());
            hl.on();
          }
        };
        new Thread(find).start();
      } else if ("t".equals("" + e.getKeyChar())) {
        log.trace("action: set target");
        dirty = true;
      } else if ("a".equals("" + e.getKeyChar())) {
        log.trace("action: set similarity");
        dirty = true;
      } else if ("n".equals("" + e.getKeyChar())) {
        log.trace("action: change name");
        String newName = Do.input("Change image name", getImageName(), "NativeCapture::ImageName");
        if (!SX.isNotSet(newName) && !imgName.equals(newName)) {
          imgName = newName;
          dirty = true;
          updateStatus();
        }
      } else if ("i".equals("" + e.getKeyChar())) {
        Do.popup(imgInfo(), "PictureTool::Information");
        log.trace("action: show info");
      } else if ("h".equals("" + e.getKeyChar())) {
        log.trace("action: show help");
      } else if ("q".equals("" + e.getKeyChar())) {
        log.trace("action: quit request");
        shouldQuit = true;
      } else if ("z".equals("" + e.getKeyChar())) {
        if (lastRects.size() > 0) {
          log.trace("action: revert crop from %s to %s", rect, lastRects.get(0));
          rect = lastRects.remove(0);
          resizeToFrame();
        }
      } else if (e.getKeyChar() == VK_ESCAPE) {
        dirty = false;
        sKey = "#ESCAPE";
        shouldQuit = true;
      }
      log.trace("keyTyped: %s", sKey);
      if (shouldQuit) {
        quit();
      }
    }
  }

  private void quit() {
    if (dirty) {
      log.error("image not saved yet");
    }
    box.dispose();
    running = false;
    SX.pause(1);
  }
  //</editor-fold>

  //<editor-fold desc="key released (special keys)">
  private void resizeSelection(MouseEvent e) {
    if (SX.isNotNull(lastDrag)) {
      if (activeSide == TOP || activeSide == BOTTOM) {
        if (e.getY() > lastDrag.getY()) {
          keyReleasedHandler(KeyEvent.VK_UP, 10);
        } else if (e.getY() < lastDrag.getY()) {
          keyReleasedHandler(KeyEvent.VK_DOWN, 10);
        }
      } else if (activeSide == LEFT || activeSide == RIGHT) {
        if (e.getX() > lastDrag.getX()) {
          keyReleasedHandler(KeyEvent.VK_RIGHT, 10);
        } else if (e.getX() < lastDrag.getX()) {
          keyReleasedHandler(KeyEvent.VK_LEFT, 10);
        }
      }
    }
  }

  private void myKeyReleased(KeyEvent e) {
    int code = e.getKeyCode();
    if (e.CHAR_UNDEFINED != e.getKeyChar()) {
    } else {
      String cName = keyReleasedHandler(code, 1);
      log.trace("keyReleased: %s (%d) %s %s %s", e.getKeyText(code), code, cName,
              activeSides[activeSide], activeSides[activesideSaved]);
    }
  }

  private String keyReleasedHandler(int code, int step) {
    String cName = "";
    Element newRect = null;
    switch (code) {
      case KeyEvent.VK_DOWN:
        cName = "#DOWN";
        if (activeSide == TOP) {
          newRect = new Element(rect.x, rect.y + step, rect.w, rect.h - step);
        } else if (activeSide == BOTTOM) {
          newRect = new Element(rect.x, rect.y, rect.w, rect.h + step);
        } else if (activeSide == LEFT || activeSide == RIGHT) {
          newRect = new Element(rect.x, rect.y - step, rect.w, rect.h);
        } else if (activeSide == ALL) {
          newRect = new Element(rect.x + step, rect.y + step, rect.w - 2 * step, rect.h - 2 * step);
        }
        break;
      case KeyEvent.VK_UP:
        cName = "#UP";
        if (activeSide == TOP) {
          newRect = new Element(rect.x, rect.y - step, rect.w, rect.h + step);
        } else if (activeSide == BOTTOM) {
          newRect = new Element(rect.x, rect.y, rect.w, rect.h - step);
        } else if (activeSide == LEFT || activeSide == RIGHT) {
          newRect = new Element(rect.x, rect.y + step, rect.w, rect.h);
        } else if (activeSide == ALL) {
          newRect = new Element(rect.x - step, rect.y - step, rect.w + 2 * step, rect.h + 2 * step);
        }
        break;
      case KeyEvent.VK_LEFT:
        cName = "#LEFT";
        if (activeSide == LEFT) {
          newRect = new Element(rect.x - step, rect.y, rect.w + step, rect.h);
        } else if (activeSide == RIGHT) {
          newRect = new Element(rect.x, rect.y, rect.w - step, rect.h);
        } else if (activeSide == TOP || activeSide == BOTTOM) {
          newRect = new Element(rect.x - step, rect.y, rect.w, rect.h);
        } else if (activeSide == ALL) {
          newRect = new Element(rect.x - step, rect.y - step, rect.w + 2 * step, rect.h + 2 * step);
        }
        break;
      case KeyEvent.VK_RIGHT:
        cName = "#RIGHT";
        if (activeSide == LEFT) {
          newRect = new Element(rect.x + step, rect.y, rect.w - step, rect.h);
        } else if (activeSide == RIGHT) {
          newRect = new Element(rect.x, rect.y, rect.w + step, rect.h);
        } else if (activeSide == TOP || activeSide == BOTTOM) {
          newRect = new Element(rect.x + step, rect.y, rect.w, rect.h);
        } else if (activeSide == ALL) {
          newRect = new Element(rect.x + step, rect.y + step, rect.w - 2 * step, rect.h - 2 * step);
        }
        break;
      case KeyEvent.VK_SHIFT:
        cName = "#SHIFT";
        content.setBorder(coloredSide(CLOCKWISE));
        box.pack();
        break;
      case KeyEvent.VK_CONTROL:
        cName = "#CTRL";
        content.setBorder(coloredSide(COUNTERCLOCKWISE));
        box.pack();
        break;
      case KeyEvent.VK_ALT:
        cName = "#ALT";
        content.setBorder(coloredSide(OPPOSITE));
        box.pack();
        break;
      case KeyEvent.VK_META:
        cName = "#META";
        content.setBorder(coloredSide(ALL));
        box.pack();
        break;
    }
    if (!SX.isNull(newRect)) {
      log.info("action: crop %s from %s to %s", cName, getActiveSideText(), newRect);
      rect.change(newRect);
      resizeToFrame();
    }
    return cName;
  }

  private Border coloredSide(int side) {
    return coloredSide(side, Color.RED, Color.GREEN);
  }

  private Border coloredSide(int side, Color outerCol, Color innerCol) {
    Border inner = null;
    Border outer = null;
    if (side == ALL) {
      if (!activeSideAll) {
        activesideSaved = activeSide;
        activeSideAll = true;
        activeSide = ALL;
        return BorderFactory.createMatteBorder(borderThickness, borderThickness,
                borderThickness, borderThickness, innerCol);
      }
    }
    if (activeSide == ALL) {
      if (!shouldKeepAll) {
        activeSide = activesideSaved;
        activeSideAll = false;
      }
      shouldKeepAll = false;
    } else {
      if (side == COUNTERCLOCKWISE) {
        activeSide = ++activeSide > RIGHT ? TOP : activeSide;
      } else if (side == CLOCKWISE) {
        activeSide = --activeSide < TOP ? RIGHT : activeSide;
      } else if (side == OPPOSITE) {
        activeSide = activeSide + 2;
        if (activeSide > RIGHT) {
          activeSide -= 4;
        }
      } else {
        activeSide = side;
      }
    }
    switch (activeSide) {
      case TOP:
        inner = BorderFactory.createMatteBorder(borderThickness, 0, 0, 0, innerCol);
        outer = BorderFactory.createMatteBorder(0, borderThickness, borderThickness, borderThickness, outerCol);
        break;
      case LEFT:
        inner = BorderFactory.createMatteBorder(0, borderThickness, 0, 0, innerCol);
        outer = BorderFactory.createMatteBorder(borderThickness, 0, borderThickness, borderThickness, outerCol);
        break;
      case BOTTOM:
        inner = BorderFactory.createMatteBorder(0, 0, borderThickness, 0, innerCol);
        outer = BorderFactory.createMatteBorder(borderThickness, borderThickness, 0, borderThickness, outerCol);
        break;
      case RIGHT:
        inner = BorderFactory.createMatteBorder(0, 0, 0, borderThickness, innerCol);
        outer = BorderFactory.createMatteBorder(borderThickness, borderThickness, borderThickness, 0, outerCol);
        break;
      case ALL:
        return BorderFactory.createMatteBorder(borderThickness, borderThickness,
                borderThickness, borderThickness, innerCol);
    }
    return new CompoundBorder(outer, inner);
  }

  private String getActiveSideText() {
    return activeSides[activeSide];
  }

  private int getActiveSide() {
    return activeSide;
  }
  //</editor-fold>

  //<editor-fold desc="handle selection">
  private void pushRect() {
    lastRects.add(0, new Element(rect));
    if (lastRects.size() > maxRevert) {
      lastRects.remove(maxRevert);
    }
  }

  Element getPos(Element elem) {
    return new Element((int) ((elem.x - borderThickness) / resizeFactor),
            (int) ((elem.y - borderThickness) / resizeFactor), elem.w);
  }

  Element getPos(MouseEvent evt) {
    return new Element((int) ((evt.getX() - borderThickness) / resizeFactor),
            (int) ((evt.getY() - borderThickness) / resizeFactor));
  }

  int halfCenteredWidth = 150;

  void crop(Element clicked) {
    pushRect();
    clicked = getPos(clicked);
    int x = clicked.x;
    int y = clicked.y;
    int button = clicked.w;
    if (TOP == activeSide) {
      rect.h -= y;
      rect.y += y;
    } else if (BOTTOM == activeSide) {
      rect.h -= rect.h - y;
    } else if (LEFT == activeSide) {
      rect.w -= x;
      rect.x += x;
    } else if (RIGHT == activeSide) {
      rect.w -= rect.w - x;
    } else if (ALL == activeSide && button < 2) {
      rect.h -= y;
      rect.y += y;
      rect.w -= x;
      rect.x += x;
      rect.w = rect.h = Math.min(Math.min(2 * halfCenteredWidth, Math.min(rect.x, scr.w - rect.x)),
              Math.min(2 * halfCenteredWidth, Math.min(rect.y, scr.h - rect.y)));
      shouldKeepAll = true;
    } else if (ALL == activeSide && button > 1) {
      rect.w -= rect.w - x;
      rect.h -= rect.h - y;
    }
    exited = -1;
    dirty = true;
    resizeToFrame();
  }

  void center(Element clicked) {
    pushRect();
    clicked = getPos(clicked);
    int x = rect.x + clicked.x;
    rect.x = Math.max(0, x - halfCenteredWidth);
    int y = rect.y + clicked.y;
    rect.y = Math.max(0, y - halfCenteredWidth);
    rect.w = rect.h = 2 * Math.min(Math.min(halfCenteredWidth, Math.min(x, scr.w - x)),
            Math.min(halfCenteredWidth, Math.min(y, scr.h - y)));
    exited = -1;
    dirty = true;
    resizeToFrame();
  }

  private void zoomOut() {
    int stepX, stepY;
    stepX = Math.max(1, rect.w / 10);
    stepY = Math.max(1, rect.h / 10);
    rect.x -= stepX;
    rect.w += 2 * stepX;
    rect.y -= stepY;
    rect.h += 2 * stepY;
    dirty = true;
    resizeToFrame();
  }

  private void zoomIn() {
    int stepX, stepY;
    stepX = Math.max(1, rect.w / 10);
    stepY = Math.max(1, rect.h / 10);
    rect.x += stepX;
    rect.w -= 2 * stepX;
    rect.y += stepY;
    rect.h -= 2 * stepY;
    dirty = true;
    resizeToFrame();
  }

  private void checkSelection() {
    rect.intersect(scr);
    Element minimum = rect.getCenter();
    minimum.grow();
    if (rect.w < minWidthHeight || rect.h < minWidthHeight) {
      rect.change(minimum);
    }
  }

  double resizeFactor = 10;

  private void resizeToFrame() {
    checkSelection();
    shot = scr.getSub(rect);
    double wFactor = rect.w / (scr.w * 0.85f);
    double hFactor = rect.h / (scr.h * 0.85f);
    resizeFactor = 1 / Math.max(wFactor, hFactor);
    resizeFactor = Math.min(resizeFactor, 10);
    BufferedImage img = shot.resize(resizeFactor).get();
    Dimension dim = new Dimension(img.getWidth() + 2 * borderThickness,
            img.getHeight() + 2 * borderThickness + statusHeight);
    content.setIcon(new ImageIcon(img));
    if (!isDragging()) {
      content.setBorder(coloredSide(activeSide));
    }
    pBox.setPreferredSize(dim);
    box.pack();
    box.setLocation((int) ((scr.w - dim.getWidth()) / 2), (int) (scr.h - dim.getHeight()) / 2);
    updateStatus();
  }

  private void drawSelection(Graphics2D g2d) {
    if (SX.isNotNull(dragStart)) {
      g2d.setStroke(new BasicStroke(2));
      g2d.setColor(Color.blue);
      int x = dragStart.getX();
      int y = dragStart.getY();
      int x2 = dragCurrent.getX();
      int y2 = dragCurrent.getY();
      int w = x2 - x;
      int h = y2 - y;
      int crossVx1 = x + w / 2;
      int crossVy1 = y;
      int crossVx2 = crossVx1;
      int crossVy2 = y + h;
      int crossHx1 = x;
      int crossHy1 = y + h / 2;
      int crossHx2 = x + w;
      int crossHy2 = crossHy1;
      g2d.drawLine(x, y, x + w, y);
      g2d.drawLine(x + w, y, x + w, y + h);
      g2d.drawLine(x + w, y + h, x, y + h);
      g2d.drawLine(x, y + h, x, y);
      g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
              0, new float[]{3, 2}, 0));
      g2d.drawLine(crossVx1, crossVy1, crossVx2, crossVy2);
      g2d.drawLine(crossHx1, crossHy1, crossHx2, crossHy2);
    }
  }

  private boolean isDragging() {
    return SX.isNotNull(dragStart);
  }
  //</editor-fold>

  //<editor-fold desc="image attributes">
  private File imgPath = null;

  public File getImgPath() {
    File path = imgPath;
    if (SX.isNull(imgPath)) {
      path = new File(SX.getSXSTORE());
    }
    return path;
  }

  private String imgName = "";

  private String getImageName() {
    String name = imgName;
    if (SX.isNotSet(imgName)) {
      name = "NameNotSet";
    }
    return name;
  }

  private File getImageFile() {
    String name = getImageName();
    return new File(getImgPath(), name + ".png");
  }

  private String imgInfo() {
    return String.format("ImagePath: %s\n" +
                    "ImageName: %s\n" +
                    "Selection: %s",
            getImgPath(), getImageFile().getName(), rect);
  }
  //</editor-fold>
}
