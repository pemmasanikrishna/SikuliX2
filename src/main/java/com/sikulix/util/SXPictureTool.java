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
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class SXPictureTool {

  private static final SXLog log = SX.getLogger("SX.PictureTool");

  private Picture scr = null;
  private Element rect = null;
  private Picture shot;

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
  private int activeSide = TOP;
  private String[] activeSides = new String[]{"TOP", "LEFT", "BOTTOM", "RIGHT"};
  private boolean running = false;

  public boolean isRunning() {
    return running;
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
    });
    //</editor-fold>
    box.setAlwaysOnTop(true);
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

  private void myMouseClicked(MouseEvent e) {
    log.trace("mouseClicked: %d,%d", e.getX(), e.getY());
    crop(e.getX(), e.getY());
    log.trace("action: crop from %s to %s", getActiveSide(), rect);
    resizeToFrame();
  }

  private void myKeyTyped(KeyEvent e) {
    if (e.CHAR_UNDEFINED != e.getKeyChar()) {
      log.trace("keyTyped: %s", e.getKeyChar());
      if ("+".equals("" + e.getKeyChar())) {
        Element newRect = zoomIn(rect);
        log.trace("action: zoom-in to %s", newRect);
        resizeToFrame();
      } else if ("-".equals("" + e.getKeyChar())) {
        Element newRect = zoomOut(rect, scr);
        log.trace("action: zoom-out to %s", newRect);
        resizeToFrame();
      } else if ("s".equals("" + e.getKeyChar())) {
        log.trace("action: save request");
        dirty = false;
      } else if ("f".equals("" + e.getKeyChar())) {
        log.trace("action: find request");
        box.setVisible(false);
        new Element(rect).highlight(3);
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
        if (dirty) {
          log.error("image not saved yet");
        }
        box.dispose();
        running = false;
      }
    }
  }

  private void myKeyReleased(KeyEvent e) {
    int code = e.getKeyCode();
    if (e.CHAR_UNDEFINED != e.getKeyChar()) {
    } else {
      String cName = "";
      Element newRect = null;
      switch (code) {
        case KeyEvent.VK_DOWN:
          cName = "DOWN";
          if (activeSide == TOP) {
            newRect = new Element(rect.x, rect.y + 1, rect.w, rect.h - 1);
          } else if (activeSide == BOTTOM) {
            newRect = new Element(rect.x, rect.y, rect.w, rect.h + 1);
          } else if (activeSide == LEFT || activeSide == RIGHT) {
            newRect = new Element(rect.x, rect.y - 1, rect.w, rect.h);
          }
          break;
        case KeyEvent.VK_UP:
          cName = "UP";
          if (activeSide == TOP) {
            newRect = new Element(rect.x, rect.y - 1, rect.w, rect.h + 1);
          } else if (activeSide == BOTTOM) {
            newRect = new Element(rect.x, rect.y, rect.w, rect.h - 1);
          } else if (activeSide == LEFT || activeSide == RIGHT) {
            newRect = new Element(rect.x, rect.y + 1, rect.w, rect.h);
          }
          break;
        case KeyEvent.VK_LEFT:
          cName = "LEFT";
          if (activeSide == LEFT) {
            newRect = new Element(rect.x - 1, rect.y, rect.w + 1, rect.h);
          } else if (activeSide == RIGHT) {
            newRect = new Element(rect.x, rect.y, rect.w - 1, rect.h);
          } else if (activeSide == TOP || activeSide == BOTTOM) {
            newRect = new Element(rect.x - 1, rect.y, rect.w, rect.h);
          }
          break;
        case KeyEvent.VK_RIGHT:
          cName = "RIGHT";
          if (activeSide == LEFT) {
            newRect = new Element(rect.x + 1, rect.y, rect.w - 1, rect.h);
          } else if (activeSide == RIGHT) {
            newRect = new Element(rect.x, rect.y, rect.w + 1, rect.h);
          } else if (activeSide == TOP || activeSide == BOTTOM) {
            newRect = new Element(rect.x + 1, rect.y, rect.w, rect.h);
          }
          break;
        case KeyEvent.VK_SHIFT:
          cName = "SHIFT";
          break;
        case KeyEvent.VK_CONTROL:
          cName = "CTRL";
          content.setBorder(coloredSide(-1));
          box.pack();
          break;
        case KeyEvent.VK_ALT:
          cName = "ALT";
          break;
        case KeyEvent.VK_META:
          cName = "META";
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

  private void resizeToFrame() {
    content.setIcon(new ImageIcon(getShot()));
    content.setBorder(coloredSide(activeSide));
    box.pack();
    box.setLocation((int) ((scr.w - shotDisplayed.getWidth()) / 2), (int) (scr.h - shotDisplayed.getHeight()) / 2);
    int wf = box.getWidth();
    int hf = box.getHeight();
    wFactor = (1f + wf) / rect.w;
    hFactor = (1f + hf) / rect.h;
    dirty = true;
  }

  private void crop(int newX, int newY) {
    int x = (int) (newX / wFactor);
    int y = (int) (newY / hFactor);
    if (TOP == activeSide) {
      rect.h -= y;
      rect.y += y;
    } else if (BOTTOM == activeSide) {
      rect.h -= rect.h - y;
      rect.h++;
    } else if (LEFT == activeSide) {
      rect.h -= y;
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
      return BorderFactory.createMatteBorder(3, 3, 3, 3, outerCol);
    }
    if (side < 0) {
      side = ++activeSide > RIGHT ? TOP : activeSide;
      activeSide = side;
    }
    switch (side) {
      case TOP:
        inner = BorderFactory.createMatteBorder(3, 0, 0, 0, innerCol);
        outer = BorderFactory.createMatteBorder(0, 3, 3, 3, outerCol);
        break;
      case LEFT:
        inner = BorderFactory.createMatteBorder(0, 3, 0, 0, innerCol);
        break;
      case BOTTOM:
        inner = BorderFactory.createMatteBorder(0, 0, 3, 0, innerCol);
        break;
      case RIGHT:
        inner = BorderFactory.createMatteBorder(0, 0, 0, 3, innerCol);
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

  boolean dirty = true;

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
