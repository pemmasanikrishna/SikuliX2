/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.image;

import com.sikulix.api.Commands;
import com.sikulix.core.NativeHookCallback;
import com.sikulix.api.Mouse;
import com.sikulix.core.NativeHook;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import org.jnativehook.NativeInputEvent;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.mouse.NativeMouseEvent;
import org.sikuli.script.Region;
import org.sikuli.script.Screen;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

public class SXCapture {

  static SXLog log;

  static {
    log = SX.getLogger("SXCapture");
    log.isSX();
    log.on(SXLog.INFO);
  }

  final static java.util.List<Object> shot = new ArrayList<Object>();
  static int cbCaptureKey = NativeKeyEvent.VC_SHIFT_L;
  static int cbpCaptureKey = NativeKeyEvent.VC_CONTROL_L;
  static NativeHook hook = null;

  public SXCapture() {
    init(NativeHook.start());
  }

  public static void init(NativeHook theHook) {
    hook = theHook;

    //<editor-fold desc="Capture Callback Selection">
    NativeHookCallback cbCapture = new NativeHookCallback() {
      public void callback(NativeInputEvent evt) {
        if (hook.startGesture()) {
          shot.clear();
          shot.add(0, SXCapture.getScreenImage(hook));
          log.info("Gesture started");
        } else if (hook.stopGesture()) {
          log.info("Gesture stopped");
          final java.util.List<Object> events = new ArrayList<Object>();
          events.addAll(hook.getQueue());
          if (events.size() > 0) {
            Runnable doit = new Runnable() {
              public void run() {
                int x = Integer.MAX_VALUE;
                int y = Integer.MAX_VALUE;
                int w = -1;
                int h = -1;
                for (Object e : events) {
                  NativeMouseEvent event = (NativeMouseEvent) e;
                  if (SX.isNull(e)) continue;
                  x = Math.min(x, event.getX());
                  y = Math.min(y, event.getY());
                  w = Math.max(w, event.getX());
                  h = Math.max(h, event.getY());
                }
                if (x < Integer.MAX_VALUE && y < Integer.MAX_VALUE && w > x && h > y) {
                  w = w - x;
                  h = h - y;
                  try {
                    SXCapture.make(hook, x, y, w, h, shot.get(0));
                  } catch (Exception ex) {
                    log.error("NativeCapture.make: %s", ex);
                    hook.allowGesture();
                  }
                } else {
                  hook.allowGesture();
                }
              }

            };
            new Thread(doit).start();
          } else {
            hook.allowGesture();
          }
        }
      }
    };
    hook.addCallback(cbCaptureKey, cbCapture);
    //</editor-fold>

    //<editor-fold desc="Capture Callback Point">

    NativeHookCallback cbpCapture = new NativeHookCallback() {
      public void callback(NativeInputEvent evt) {
        if (hook.stopGesture()) {
          log.info("Gesture stopped for point");
          final java.util.List<NativeInputEvent> events = new ArrayList<NativeInputEvent>();
          NativeInputEvent event = hook.getQueueLast();
          if (!SX.isNull(event)) {
            events.add(event);
            Runnable doit = new Runnable() {
              public void run() {
                NativeMouseEvent mevt = (NativeMouseEvent) events.get(0);
                try {
                  SXCapture.make(hook, mevt.getX(), mevt.getY(), 0, 0, shot.get(0));
                } catch (Exception ex) {
                  log.error("NativeCapture.makeFromPoint: %s", ex);
                  hook.allowGesture();
                }
              }

            };
            new Thread(doit).start();
          } else {
            hook.allowGesture();
          }
        }
      }
    };
    hook.addCallback(cbpCaptureKey, cbpCapture);
    //</editor-fold>
  }

  public static Object getScreenImage(NativeHook hook) {
    BufferedImage bImg = new Screen(Mouse.at().getContainingScreenNumber()).capture().getImage();
    return bImg;
  }

  public static BufferedImage resize(BufferedImage bImg, float factor) {
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

  public static void make(NativeHook hook, int x, int y, int w, int h, Object shot) {
    Screen scr = new Screen(Mouse.at().getContainingScreenNumber());
    Rectangle toCapture = null;
    if (w == 0 && h == 0) {
      toCapture = new Rectangle(x - 50, y - 15, 100, 30);
    } else {
      toCapture = new Rectangle(x, y, w, h);
    }
    BufferedImage img = getCapture(scr, toCapture, shot);
    if (!SX.isNull(img)) {
      log.info("%d,%d %dx%d on(%s)", toCapture.x, toCapture.y, toCapture.width, toCapture.height, scr);
      MakeCapture mc = new MakeCapture(hook, scr, toCapture, img, shot);
    } else {
      log.error("%d,%d %dx%d on(%s)", toCapture.x, toCapture.y, toCapture.width, toCapture.height, scr);
    }
  }

  private static BufferedImage getCapture(Screen scr, Rectangle r, Object shot) {
    float wFactor = r.width / (scr.w * 0.9f);
    float hFactor = r.height / (scr.h * 0.9f);
    float factor = 1 / Math.max(wFactor, hFactor);
    factor = Math.min(factor, 10);
    return resize(((BufferedImage) shot).getSubimage(r.x, r.y, r.width, r.height), factor);
  }

  private static class MakeCapture extends MouseAdapter implements WindowListener, KeyListener {

    private NativeHook hook = null;
    private Screen scr = null;
    private BufferedImage shot = null;
    private Rectangle rect = null;
    private JFrame box = null;
    private JLabel content = null;
    private float aspect = 0;
    private float wFactor = 0;
    private float hFactor = 0;
    private int xOff = 0;
    private int yOff = 0;

    final private int TOP = 0;
    final private int LEFT = 1;
    final private int BOTTOM = 2;
    final private int RIGHT = 3;
    final private int ALL = 4;
    private int activeSide = TOP;
    private String[] activeSides = new String[]{"TOP", "LEFT", "BOTTOM", "RIGHT"};

    public MakeCapture(NativeHook hook, Screen scr, Rectangle rect, BufferedImage img, Object shot) {
      this.hook = hook;
      this.scr = scr;
      this.rect = rect;
      aspect = ((float) rect.width) / rect.height;
      this.shot = (BufferedImage) shot;
      init(img);
    }

    private void init(BufferedImage img) {
      box = new JFrame();
      box.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      box.setResizable(false);
      box.addWindowListener(this);
      box.addKeyListener(this);
      box.addMouseListener(this);
      box.setAlwaysOnTop(true);
      JPanel pBox = new JPanel() {
        public boolean isOptimizedDrawingEnabled() {
          return false;
        }
      };
      LayoutManager overlay = new OverlayLayout(pBox);
      pBox.setLayout(overlay);
      content = new JLabel(new ImageIcon(img));
      content.setBorder(coloredSide(TOP, Color.RED));
      pBox.add(content);
      box.add(pBox);
      box.pack();
      box.setLocation((int) ((scr.w - box.getWidth()) / 2), (int) (scr.h - box.getHeight()) / 2);
      box.setVisible(true);
      zoom(rect);
    }

    private Rectangle zoomOut(Rectangle in, Rectangle max) {
      Rectangle out = new Rectangle(in);
      int toAdd;
      if (aspect <= 1) {
        out.x -= 1;
        out.width += 2;
        toAdd = (int) (1.0f / aspect);
        out.y -= toAdd;
        out.height += 2 * toAdd;
      } else {
        out.y -= 1;
        out.height += 2;
        toAdd = (int) aspect;
        out.x -= toAdd;
        out.width += 2 * toAdd;
      }
      out = max.intersection(out);
      return out;
    }

    private Rectangle zoomIn(Rectangle in) {
      Rectangle out = new Rectangle(in);
      int toAdd;
      if (aspect <= 1) {
        out.x += 1;
        out.width -= 2;
        toAdd = (int) (1.0f / aspect);
        out.y += toAdd;
        out.height -= 2 * toAdd;
      } else {
        out.y += 1;
        out.height -= 2;
        toAdd = (int) aspect;
        out.x += toAdd;
        out.width -= 2 * toAdd;
      }
      if (out.width < 5 || out.height < 5) {
        return in;
      }
      return out;
    }

    private void zoom(Rectangle newRect) {
      rect = new Rectangle(newRect);
      BufferedImage img = getCapture(scr, rect, shot);
      content.setIcon(new ImageIcon(img));
      content.setBorder(coloredSide(activeSide));
      box.pack();
      box.setLocation((int) ((scr.w - img.getWidth()) / 2), (int) (scr.h - img.getHeight()) / 2);
      int wf = box.getWidth();
      int hf = box.getHeight();
      int wc = content.getWidth();
      int hc = content.getHeight();
      xOff = wf - wc;
      yOff = hf - hc;
      wFactor = (1f + wc) / rect.width;
      hFactor = (1f + hc) / rect.height;
      dirty = true;
    }

    private Border coloredSide(int side) {
      return coloredSide(side, Color.RED);
    }

    private Border coloredSide(int side, Color col) {
      if (side == ALL) {
        return BorderFactory.createMatteBorder(3, 3, 3, 3, col);
      }
      if (side < 0) {
        side = ++activeSide > RIGHT ? TOP : activeSide;
        activeSide = side;
      }
      switch (side) {
        case TOP:
          return BorderFactory.createMatteBorder(3, 0, 0, 0, col);
        case LEFT:
          return BorderFactory.createMatteBorder(0, 3, 0, 0, col);
        case BOTTOM:
          return BorderFactory.createMatteBorder(0, 0, 3, 0, col);
        case RIGHT:
          return BorderFactory.createMatteBorder(0, 0, 0, 3, col);
      }
      return BorderFactory.createMatteBorder(3, 0, 0, 0, col);
    }

    private String getActiveSide() {
      return activeSides[activeSide];
    }

    private String rectangleToString(Rectangle r) {
      return String.format("[%d,%d %dx%d]", r.x, r.y, r.width, r.height);
    }

    private Rectangle crop(Rectangle in, int newX, int newY) {
      Rectangle out = new Rectangle(in);
      int x = (int) (newX / wFactor);
      int y = (int) (newY / hFactor);
      if (TOP == activeSide) {
        out.height -= y;
        out.y += y;
      } else if (BOTTOM == activeSide) {
        out.height -= out.height - y;
        out.height++;
      } else if (LEFT == activeSide) {
        out.width -= y;
        out.x += x;
      } else if (RIGHT == activeSide) {
        out.width -= out.width - x;
        out.width++;
      }
      return out;
    }

    public void mouseClicked(MouseEvent e) {
      log.trace("mouseClicked: %d,%d", e.getX(), e.getY());
      Rectangle newRect = crop(rect, e.getX() - xOff, e.getY() - yOff);
      log.trace("action: crop from %s to %s", getActiveSide(), rectangleToString(newRect));
      zoom(newRect);
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
              getImgPath(), getImageFile().getName(), rectangleToString(rect));
    }

    boolean dirty = true;

    public void keyTyped(KeyEvent e) {
      if (e.CHAR_UNDEFINED != e.getKeyChar()) {
        log.trace("keyTyped: %s", e.getKeyChar());
        if ("+".equals("" + e.getKeyChar())) {
          Rectangle newRect = zoomIn(rect);
          log.trace("action: zoom-in to %s", rectangleToString(newRect));
          zoom(newRect);
        } else if ("-".equals("" + e.getKeyChar())) {
          Rectangle newRect = zoomOut(rect, scr.getBounds());
          log.trace("action: zoom-out to %s", rectangleToString(newRect));
          zoom(newRect);
        } else if ("s".equals("" + e.getKeyChar())) {
          log.info("action: save request");
          dirty = false;
        } else if ("f".equals("" + e.getKeyChar())) {
          log.info("action: find request");
          box.setVisible(false);
          new Region(rect).highlight(3);
          box.setVisible(true);
        } else if ("t".equals("" + e.getKeyChar())) {
          log.info("action: set target");
          dirty = true;
        } else if ("a".equals("" + e.getKeyChar())) {
          log.info("action: set similarity");
          dirty = true;
        } else if ("n".equals("" + e.getKeyChar())) {
          log.info("action: change name");
          String newName = Commands.input("Change image name", getImageName(), "NativeCapture::ImageName");
          if (!SX.isNotSet(newName) && !imgName.equals(newName)) {
            imgName = newName;
            dirty = true;
          }
        } else if ("i".equals("" + e.getKeyChar())) {
          Commands.popup(imgInfo(), "NativeCapture::Information");
          log.info("action: show info");
        } else if ("h".equals("" + e.getKeyChar())) {
          log.info("action: show help");
        } else if ("q".equals("" + e.getKeyChar())) {
          log.info("action: quit request");
          if (dirty) {
            log.error("image not saved yet");
          }
          box.dispatchEvent(new WindowEvent(box, WindowEvent.WINDOW_CLOSING));
          hook.stop();
        }
      }
    }

    public void keyReleased(KeyEvent e) {
      int code = e.getKeyCode();
      if (e.CHAR_UNDEFINED != e.getKeyChar()) {
      } else {
        String cName = "";
        Rectangle newRect = null;
        switch (code) {
          case KeyEvent.VK_DOWN:
            cName = "DOWN";
            if (activeSide == TOP) {
              newRect = new Rectangle(rect.x, rect.y + 1, rect.width, rect.height - 1);
            } else if (activeSide == BOTTOM) {
              newRect = new Rectangle(rect.x, rect.y, rect.width, rect.height + 1);
            } else if (activeSide == LEFT || activeSide == RIGHT) {
              newRect = new Rectangle(rect.x, rect.y - 1, rect.width, rect.height);
            }
            break;
          case KeyEvent.VK_UP:
            cName = "UP";
            if (activeSide == TOP) {
              newRect = new Rectangle(rect.x, rect.y - 1, rect.width, rect.height + 1);
            } else if (activeSide == BOTTOM) {
              newRect = new Rectangle(rect.x, rect.y, rect.width, rect.height - 1);
            } else if (activeSide == LEFT || activeSide == RIGHT) {
              newRect = new Rectangle(rect.x, rect.y + 1, rect.width, rect.height);
            }
            break;
          case KeyEvent.VK_LEFT:
            cName = "LEFT";
            if (activeSide == LEFT) {
              newRect = new Rectangle(rect.x - 1, rect.y, rect.width + 1, rect.height);
            } else if (activeSide == RIGHT) {
              newRect = new Rectangle(rect.x, rect.y, rect.width - 1, rect.height);
            } else if (activeSide == TOP || activeSide == BOTTOM) {
              newRect = new Rectangle(rect.x - 1, rect.y, rect.width, rect.height);
            }
            break;
          case KeyEvent.VK_RIGHT:
            cName = "RIGHT";
            if (activeSide == LEFT) {
              newRect = new Rectangle(rect.x + 1, rect.y, rect.width - 1, rect.height);
            } else if (activeSide == RIGHT) {
              newRect = new Rectangle(rect.x, rect.y, rect.width + 1, rect.height);
            } else if (activeSide == TOP || activeSide == BOTTOM) {
              newRect = new Rectangle(rect.x + 1, rect.y, rect.width, rect.height);
            }
            break;
          case KeyEvent.VK_SHIFT:
            cName = "SHIFT";
            break;
          case KeyEvent.VK_CONTROL:
            cName = "CTRL";
            content.setBorder(coloredSide(-1, Color.RED));
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
          log.info("action: crop %s from %s to %s", cName, getActiveSide(), rectangleToString(newRect));
          zoom(newRect);
        }
        log.trace("keyReleased: %s (%d) %s", e.getKeyText(code), code, cName);
      }
    }

    public void windowClosing(WindowEvent e) {
      hook.allowGesture();
      log.trace("Window about to be closed");
    }

    //<editor-fold desc="WindowEvents not used">
    public void windowOpened(WindowEvent e) {

    }

    public void windowClosed(WindowEvent e) {
      log.trace("Window is disposed");
    }

    public void windowIconified(WindowEvent e) {

    }

    public void windowDeiconified(WindowEvent e) {

    }

    public void windowActivated(WindowEvent e) {

    }

    public void windowDeactivated(WindowEvent e) {

    }
    //</editor-fold>

    //<editor-fold desc="KeyEvents notused">
    public void keyPressed(KeyEvent e) {
      log.trace("keyPressed: %s", e.paramString());
    }
    //</editor-fold>

  }

}
