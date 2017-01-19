package com.sikulix.util;

import com.sikulix.api.Do;
import com.sikulix.api.Element;
import com.sikulix.api.Picture;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.awt.event.KeyEvent.VK_ESCAPE;

public class SXPictureTool {

  private static final SXLog log = SX.getLogger("SX.PictureTool");

  private Picture scr = null;
  private Element rect = null;
  private List<Element> lastRects = new ArrayList<>();
  private int maxRevert = 10;
  private Picture shot;
  boolean dirty = false;

  private JFrame box = null;
  private BufferedImage shotDisplayed = null;
  private JLabel content = new JLabel();

  private float aspect = 0;
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
  private String[] activeSides = new String[]{"TOP", "LEFT", "BOTTOM", "RIGHT"};
  private boolean running = false;
  private boolean ignoreMouseEnter = true;

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
    aspect = ((float) rect.w) / rect.h;
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
    JPanel pBox = new JPanel() {
      public boolean isOptimizedDrawingEnabled() {
        return false;
      }
    };
    LayoutManager overlay = new OverlayLayout(pBox);
    pBox.setLayout(overlay);
    resizeToFrame();
    dirty = false;
    content.setBorder(coloredSide(TOP));
    pBox.add(content);
    box.add(pBox);
    box.pack();
    box.setLocation((int) ((scr.w - box.getWidth()) / 2), (int) (scr.h - box.getHeight()) / 2);
    box.setVisible(true);
  }

  private Element displayBox() {
    Element elem = new Element(0, 0, shotDisplayed.getWidth(), shotDisplayed.getHeight());
    return elem.setName("BOX");
  }

  private void myMouseEntered(MouseEvent e) {
    if (ignoreMouseEnter) {
      ignoreMouseEnter = false;
      return;
    }
    log.trace("mouseEntered: %d,%d %s", e.getX(), e.getY(), displayBox());
    int side = whichSide(e.getX() - borderThickness, e.getY() - borderThickness);
    content.setBorder(coloredSide(side));
    box.repaint();
  }

  private int whichSide(int x, int y) {
    if (isMiddle(displayBox().w, x) && isLow(displayBox().h, y)) return TOP;
    if (isMiddle(displayBox().h, y) && isLow(displayBox().w, x)) return LEFT;
    if (isMiddle(displayBox().w, x) && isHigh(displayBox().h, y)) return BOTTOM;
    if (isMiddle(displayBox().h, y) && isHigh(displayBox().w, x)) return RIGHT;
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

  private void myMouseExited(MouseEvent e) {
    //log.trace("mouseExited: %d,%d %s", e.getX(), e.getY(), new Element(box.getBounds()).setName("BOX"));
  }

  private void myMouseClicked(MouseEvent e) {
    log.trace("mouseClicked: %d,%d", e.getX(), e.getY());
    crop(e.getX(), e.getY());
    log.trace("action: crop from %s to %s", getActiveSide(), rect);
    resizeToFrame();
  }

  private void myKeyTyped(KeyEvent e) {
    boolean shouldQuit = false;
    if (e.CHAR_UNDEFINED != e.getKeyChar()) {
      String sKey = "" + e.getKeyChar();
      if ("+".equals("" + e.getKeyChar())) {
        rect = zoomIn(rect);
        log.trace("action: zoom-in to %s", rect);
        resizeToFrame();
      } else if ("-".equals("" + e.getKeyChar())) {
        rect = zoomOut(rect, scr);
        log.trace("action: zoom-out to %s", rect);
        resizeToFrame();
      } else if ("s".equals("" + e.getKeyChar())) {
        log.trace("action: save request");
        dirty = false;
      } else if ("f".equals("" + e.getKeyChar())) {
        box.setVisible(false);
        Do.find(getCapture(), scr);
        log.trace("action: find: %s", scr.getLastMatch());
        SXHighlight hl = new SXHighlight(scr);
        hl.add(scr.getLastMatch());
        hl.on();
        box.setVisible(true);
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
        Do.popup(imgInfo(), "NativeCapture::Information");
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
  }

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

  private BufferedImage getShot() {
    shot = scr.getSub(rect);
    double wFactor = rect.w / (scr.w * 0.9f);
    double hFactor = rect.h / (scr.h * 0.9f);
    double factor = 1 / Math.max(wFactor, hFactor);
    factor = Math.min(factor, 10);
    shotDisplayed = shot.resize(factor).get();
    return shotDisplayed;
  }

  public Picture getCapture() {
    return scr.getSub(rect);
  }

  private void resizeToFrame() {
    content.setIcon(new ImageIcon(getShot()));
    content.setBorder(coloredSide(activeSide));
    box.pack();
    box.setLocation((int) ((scr.w - shotDisplayed.getWidth()) / 2), (int) (scr.h - shotDisplayed.getHeight()) / 2);
    dirty = true;
  }

  private void crop(int newX, int newY) {
    lastRects.add(0, new Element(rect));
    if (lastRects.size() > maxRevert) {
      lastRects.remove(maxRevert);
    }
    int wf = box.getWidth();
    int hf = box.getHeight();
    wFactor = (1f + wf) / rect.w;
    hFactor = (1f + hf) / rect.h;
    int x = (int) (newX / wFactor);
    int y = (int) (newY / hFactor);
    if (TOP == activeSide) {
      rect.h -= y;
      rect.y += y;
    } else if (BOTTOM == activeSide) {
      rect.h -= rect.h - y;
      rect.h++;
    } else if (LEFT == activeSide) {
      rect.w -= x;
      rect.x += x;
    } else if (RIGHT == activeSide) {
      rect.w -= rect.w - x;
      rect.w++;
    }
  }

  private Element zoomOut(Element in, Element max) {
    Element out = new Element(in);
    int toAdd;
    if (aspect <= 1) {
      out.x -= 1;
      out.w += 2;
      toAdd = (int) (1.0f / aspect);
      out.y -= toAdd;
      out.h += 2 * toAdd;
    } else {
      out.y -= 1;
      out.h += 2;
      toAdd = (int) aspect;
      out.x -= toAdd;
      out.w += 2 * toAdd;
    }
    out = max.intersection(out);
    return out;
  }

  private Element zoomIn(Element in) {
    Element out = new Element(in);
    int toAdd;
    if (aspect <= 1) {
      out.x += 1;
      out.w -= 2;
      toAdd = (int) (1.0f / aspect);
      out.y += toAdd;
      out.h -= 2 * toAdd;
    } else {
      out.y += 1;
      out.h -= 2;
      toAdd = (int) aspect;
      out.x += toAdd;
      out.w -= 2 * toAdd;
    }
    if (out.w < 5 || out.h < 5) {
      return in;
    }
    return out;
  }

  private Border coloredSide(int side) {
    return coloredSide(side, Color.RED, Color.GREEN);
  }

  private Border coloredSide(int side, Color outerCol, Color innerCol) {
    Border inner = null;
    Border outer = null;
    if (side == ALL) {
      return BorderFactory.createMatteBorder(borderThickness, borderThickness, borderThickness, borderThickness, outerCol);
    }
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

  private static BufferedImage resize(BufferedImage bImg, float factor) {
    int type;
    type = bImg.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : bImg.getType();
    int width = (int) (bImg.getWidth() * factor);
    int height = (int) (bImg.getHeight() * factor);
    BufferedImage resizedImage = new BufferedImage(width, height, type);
    Graphics2D g = resizedImage.createGraphics();
    g.drawImage(bImg, 0, 0, width, height, null);
    g.dispose();
    return resizedImage;
  }

}
