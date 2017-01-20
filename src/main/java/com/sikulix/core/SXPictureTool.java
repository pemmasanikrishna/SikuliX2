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
  private JLabel content = new JLabel();
  private JLabel mousePos = new JLabel();

  private float aspect = 1;
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
  private String[] activeSides = new String[]{"TOP", "LEFT", "BOTTOM", "RIGHT", "ALL"};
  private boolean running = false;

  private int borderThickness = 5;

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
    //</editor-fold>

    box.addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseDragged(MouseEvent e) {
        super.mouseDragged(e);
      }

      @Override
      public void mouseMoved(MouseEvent e) {
        super.mouseMoved(e);
        Element elem = getPos(new Element(e.getX(), e.getY()));
        Element rect = getRect();
        mousePos.setText(String.format("%04d, %04d (%04d, %04d) [%04d, %04d %04dx%04d]",
                elem.x, elem.y, rect.x + elem.x, rect.y + elem.y, rect.x, rect.y, rect.w, rect.h));
      }
    });


    pBox = box.getContentPane();
    pBox.setLayout(new BoxLayout(pBox, BoxLayout.Y_AXIS));
    pBox.setBackground(Color.white);
    mousePos.setOpaque(true);
    mousePos.setBackground(Color.white);
    mousePos.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 20));
    mousePos.setText("    ,     ");
    resizeToFrame();
    dirty = false;
    content.setBorder(coloredSide(TOP));
    pBox.add(content);
    pBox.add(mousePos);
    box.pack();
    box.setLocation((int) ((scr.w - box.getWidth()) / 2), (int) (scr.h - box.getHeight()) / 2);
    new Thread(new ClickHandler()).start();
    box.setVisible(true);
  }

  public Element getRect() {
    return rect;
  }
  //</editor-fold>

  //<editor-fold desc="mouse exit/enter">
  int exited = -1;

  private void myMouseEntered(MouseEvent e) {
    int side = whichSide(e.getX() - borderThickness, e.getY() - borderThickness);
    if (side == exited) {
      log.trace("mouseEntered: %s", activeSides[side]);
      exited = -1;
      content.setBorder(coloredSide(side));
      box.repaint();
    }
  }

  private void myMouseExited(MouseEvent e) {
    exited = whichSide(e.getX() - borderThickness, e.getY() - borderThickness);
    log.trace("mouseExited: %s ", activeSides[exited]);
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
  //</editor-fold>

  //<editor-fold desc="mouse click">
  private void myMouseClicked(MouseEvent e) {
    String doubleClick = e.getClickCount() > 1 ? "Double" : "";
    log.trace("mouse%sClicked: %d,%d", doubleClick, e.getX(), e.getY());
    lastRects.add(0, new Element(rect));
    if (lastRects.size() > maxRevert) {
      lastRects.remove(maxRevert);
    }
    if (!SX.isSet(doubleClick)) {
      setClickStatusClicked(new Element(e.getX(), e.getY()));
    } else {
      setClickStatusDoubleClicked();
    }
  }

  Element getPos(Element elem) {
    return new Element((int) ((elem.x - borderThickness) / resizeFactor),
            (int) ((elem.y - borderThickness) / resizeFactor));
  }

  void crop(Element clicked) {
    clicked = getPos(clicked);
    int x = clicked.x;
    int y = clicked.y;
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
    } else if (ALL == activeSide) {
      rect.h -= y;
      rect.y += y;
      rect.w -= x;
      rect.x += x;
    }
    exited = -1;
    resizeToFrame();
  }

  void center(Element clicked) {
    int halfCenteredWidth = 150;
    clicked = getPos(clicked);
    int x = rect.x + clicked.x;
    rect.x = Math.max(0, x - halfCenteredWidth);
    int y = rect.y + clicked.y;
    rect.y = Math.max(0, y - halfCenteredWidth);
    rect.w = rect.h = 2 * Math.min(Math.min(halfCenteredWidth,Math.min(x, scr.w - x)),
            Math.min(halfCenteredWidth,Math.min(y, scr.h - y)));
    exited = -1;
    resizeToFrame();
  }

  double resizeFactor = 10;

  private void resizeToFrame() {
    shot = scr.getSub(rect);
    double wFactor = rect.w / (scr.w * 0.85f);
    double hFactor = rect.h / (scr.h * 0.85f);
    resizeFactor = 1 / Math.max(wFactor, hFactor);
    resizeFactor = Math.min(resizeFactor, 10);
    BufferedImage img = shot.resize(resizeFactor).get();
    Dimension dim = new Dimension(img.getWidth() + 2 * borderThickness,
            img.getHeight() + 2 * borderThickness + 30);
    content.setIcon(new ImageIcon(img));
    content.setBorder(coloredSide(activeSide));
    pBox.setPreferredSize(dim);
    box.pack();
    box.setLocation((int) ((scr.w - dim.getWidth()) / 2),(int) (scr.h - dim.getHeight()) / 2);
    dirty = true;
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
      while (true) {
        if (clickStatus.get(0).isClicked()) {
          SX.pause(0.3);
          if (clickStatus.get(0).isClicked()) {
            clicked = clickStatus.get(0).getClick();
            log.trace("action: crop at (%d,%d) from %s", clicked.x, clicked.y, getActiveSide());
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

  private void zoomOut() {
    int stepX, stepY;
    stepX = Math.max(1, rect.w / 10);
    stepY = Math.max(1, rect.h / 10);
    rect.x -= stepX;
    rect.w += 2 * stepX;
    rect.y -= stepY;
    rect.h += 2 * stepY;
    rect = scr.intersection(rect);
    resizeToFrame();
  }

  private void zoomIn() {
    int stepX, stepY;
    Element minimum = rect.getCenter();
    minimum.grow();
    stepX = Math.max(1, rect.w / 10);
    stepY = Math.max(1, rect.h / 10);
    rect.x += stepX;
    rect.w -= 2 * stepX;
    rect.y += stepY;
    rect.h -= 2 * stepY;
    if (rect.w < 5 || rect.h < 5) {
      rect.change(minimum);
    }
    resizeToFrame();
  }

  private void quit() {
    if (dirty) {
      log.error("image not saved yet");
    }
    box.dispose();
    running = false;
  }
  //</editor-fold>

  //<editor-fold desc="key released (special keys)">
  private void myKeyReleased(KeyEvent e) {
    int code = e.getKeyCode();
    if (e.CHAR_UNDEFINED != e.getKeyChar()) {
    } else {
      String cName = "";
      Element newRect = null;
      switch (code) {
        case KeyEvent.VK_DOWN:
          cName = "#DOWN";
          if (activeSide == TOP) {
            newRect = new Element(rect.x, rect.y + 1, rect.w, rect.h - 1);
          } else if (activeSide == BOTTOM) {
            newRect = new Element(rect.x, rect.y, rect.w, rect.h + 1);
          } else if (activeSide == LEFT || activeSide == RIGHT) {
            newRect = new Element(rect.x, rect.y - 1, rect.w, rect.h);
          }
          break;
        case KeyEvent.VK_UP:
          cName = "#UP";
          if (activeSide == TOP) {
            newRect = new Element(rect.x, rect.y - 1, rect.w, rect.h + 1);
          } else if (activeSide == BOTTOM) {
            newRect = new Element(rect.x, rect.y, rect.w, rect.h - 1);
          } else if (activeSide == LEFT || activeSide == RIGHT) {
            newRect = new Element(rect.x, rect.y + 1, rect.w, rect.h);
          }
          break;
        case KeyEvent.VK_LEFT:
          cName = "#LEFT";
          if (activeSide == LEFT) {
            newRect = new Element(rect.x - 1, rect.y, rect.w + 1, rect.h);
          } else if (activeSide == RIGHT) {
            newRect = new Element(rect.x, rect.y, rect.w - 1, rect.h);
          } else if (activeSide == TOP || activeSide == BOTTOM) {
            newRect = new Element(rect.x - 1, rect.y, rect.w, rect.h);
          }
          break;
        case KeyEvent.VK_RIGHT:
          cName = "#RIGHT";
          if (activeSide == LEFT) {
            newRect = new Element(rect.x + 1, rect.y, rect.w - 1, rect.h);
          } else if (activeSide == RIGHT) {
            newRect = new Element(rect.x, rect.y, rect.w + 1, rect.h);
          } else if (activeSide == TOP || activeSide == BOTTOM) {
            newRect = new Element(rect.x + 1, rect.y, rect.w, rect.h);
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
        log.info("action: crop %s from %s to %s", cName, getActiveSide(), newRect);
        rect = newRect;
        resizeToFrame();
      }
      log.trace("keyReleased: %s (%d) %s", e.getKeyText(code), code, cName);
    }
  }

  private Border coloredSide(int side) {
    return coloredSide(side, Color.RED, Color.GREEN);
  }

  private Border coloredSide(int side, Color outerCol, Color innerCol) {
    Border inner = null;
    Border outer = null;
    if (side == ALL) {
      activeSide = ALL;
      return BorderFactory.createMatteBorder(borderThickness, borderThickness, borderThickness, borderThickness, innerCol);
    }
    if (activeSide == ALL) {
      activeSide = TOP;
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
    }
    return new CompoundBorder(outer, inner);
  }

  private String getActiveSide() {
    return activeSides[activeSide];
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

  private String imgName = null;

  private String getImageName() {
    String name = imgName;
    if (SX.isNotSet(imgName)) {
      name = "";
    }
    return name;
  }

  private File getImageFile() {
    String name = imgName;
    if (SX.isNotSet(imgName)) {
      name = "NameNotSet";
    }
    return new File(getImgPath(), name + ".png");
  }

  private String imgInfo() {
    return String.format("ImagePath: %s\n" +
                    "ImageName: %s\n" +
                    "Region: %s",
            getImgPath(), getImageFile().getName(), rect);
  }
  //</editor-fold>
}
