/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.util;

import com.sikulix.api.*;
import com.sikulix.core.*;
import org.opencv.core.Mat;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.awt.event.KeyEvent.VK_ESCAPE;

public class Tool {

  //<editor-fold desc="housekeeping">
  private static final SXLog log = SX.getLogger("SX.Tool");

  //<editor-fold desc="fields">
  JFrame intro = new JFrame();
  private Picture base = null;
  private Element rect = null;
  boolean isImage = false;
  private List<Element> lastRects = new ArrayList<>();
  private int maxRevert = 10;
  private Picture shot;
  int shotDisplayedW = 0;
  int shotDisplayedH = 0;
  boolean dirty = false;

  private JFrame box = null;
  private Container pBox = null;
  private JLabel content = new JLabel() {
    @Override
    public void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2d = (Graphics2D) g.create();
      if (getActiveSide() == ALL) {
        drawSelection(g2d);
      } else {
        drawCrossHair(g2d);
      }
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
  final private int NONE = 5;
  final private int COUNTERCLOCKWISE = 6;
  final private int CLOCKWISE = 7;
  final private int OPPOSITE = 8;
  private int activeSide = NONE;
  private int activeSideSaved = TOP;
  private String[] activeSides = new String[]{"TOP", "LEFT", "BOTTOM", "RIGHT", "ALL", "NONE"};
  private boolean running = false;

  private int borderThickness = 5;
  private int minWidthHeight = 5;
  private int whichSideMargin = 10;

  MouseEvent lastDrag = null;
  MouseEvent dragStart = null;
  MouseEvent dragCurrent = null;

  int dragButton = 1;

  public void setDragButton(int dragButton) {
    this.dragButton = dragButton;
  }

  String bundlePath = "";
  String appName = "";

  int scrW = 0;
  int scrH = 0;
  int scrID = 0;

  public boolean isRunning() {
    return running;
  }
  //</editor-fold>

  public Tool() {
    bundlePath = SX.getOption("Tool.bundlePath");
    if (bundlePath.isEmpty()) {
      String userwork = SX.getUSERWORK();
      if (new File(userwork, "pom.xml").exists()) {
        bundlePath = new File(userwork, "src/main/resources/Images").getAbsolutePath();
      } else {
        bundlePath = SX.getFolder(SX.getSXSTORE(), "Images").getAbsolutePath();
      }
      log.trace("%s", userwork);
    }
    intro = new JFrame();
    intro.addKeyListener(new KeyAdapter() {
      @Override
      public void keyTyped(KeyEvent e) {
        boolean shouldQuit = false;
        super.keyTyped(e);
        String sKey = "" + e.getKeyChar();
        if (e.getKeyChar() == VK_ESCAPE) {
          shouldQuit = true;
        } else {
          if (!"pocabq".contains(sKey)) {
            log.trace("keyTyped: %s", sKey);
            return;
          }
          if ("p".equals("" + e.getKeyChar())) {
            log.trace("action: bundlePath");
            actionBundlePath(intro);
          } else if ("o".equals("" + e.getKeyChar())) {
            log.trace("action: open");
            actionOpen(intro);
          } else if ("c".equals("" + e.getKeyChar())) {
            log.trace("action: capture");
            intro.setVisible(false);
            SX.pause(0.5);
            actionCapture();
          } else if ("a".equals("" + e.getKeyChar())) {
            intro.setVisible(false);
            appName = Do.input("name an application to focus", "SikulixTool::SwitchApp", intro);
            if (SX.isNotNull(appName)) {
              new Window(appName).toFront();
            }
            SX.pause(2);
            intro.setVisible(true);
            Do.popup("click ok to continue", intro);
          } else if ("b".equals("" + e.getKeyChar())) {
            if (Do.popAsk("now going to background:" +
                            "\n- use ctrl-alt-2 to capture" +
                            "\n- use ctrl-alt-1 for back to foreground",
                    "SikulixTool::ToBackground", intro)) {
              actionToBackground();
            }
          } else if ("q".equals("" + e.getKeyChar())) {
            shouldQuit = true;
          }
        }
        if (shouldQuit) {
          log.trace("action: quit requested");
          actionQuit();
        }
      }
    });
    intro.setUndecorated(true);
    intro.setAlwaysOnTop(true);
    intro.setResizable(false);
    if (!log.isGlobalLevel(log.TRACE)) {
      intro.setAlwaysOnTop(true);
    }
    Container introPane = intro.getContentPane();
    introPane.setLayout(new BoxLayout(introPane, BoxLayout.Y_AXIS));
    introPane.setBackground(Color.white);
    String aText = "<html>" +
            "<h1>&nbsp;&nbsp;&nbsp;&nbsp;SikuliX Tool</h1>" +
            "<hr><br>" +
            "&nbsp;type a key for an action:" +
            "<br><hr><br>" +
            "&nbsp;&nbsp;p - set bundle path" +
            "<br>" +
            "&nbsp;&nbsp;o - open an image file" +
            "<br>" +
            "&nbsp;&nbsp;c - capture screen image" +
            "<br>" +
            "&nbsp;&nbsp;a - focus an application" +
            "<br>" +
            "&nbsp;&nbsp;b - tool to background" +
            "<br><br><hr><br>" +
            "&nbsp;ESC or q - to quit the tool" +
            "<br>";
    JLabel introText = getNewLabel(320, 300, aText);
    introText.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
    introText.setBorder(BorderFactory.createLineBorder(Color.magenta, 2));
    introPane.add(introText);
    intro.pack();
    Dimension introSize = intro.getMinimumSize();
    Element centered = new Element(introSize).getCentered(Do.onMain());
    intro.setLocation(centered.x, centered.y);
    initBox();
    intro.setVisible(true);
  }

  private JLabel getNewLabel(final int width, final int height, String text) {
    JLabel jLabel = new JLabel(text) {
      @Override
      public Dimension getPreferredSize() {
        return new Dimension(width, height);
      }

      @Override
      public Dimension getMinimumSize() {
        return new Dimension(width, height);
      }

      @Override
      public Dimension getMaximumSize() {
        return new Dimension(width, height);
      }
    };
    jLabel.setVerticalAlignment(SwingConstants.CENTER);
    jLabel.setHorizontalAlignment(SwingConstants.CENTER);
    return jLabel;
  }

  private void initBox() {
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
          if (e.getButton() > 1) {
            start.at(start.x - w / 2, start.y - h / 2);
            end.at(start.x + w, start.y + h);
          }
          if (w > minWidthHeight && h > minWidthHeight) {
            pushRect();
            int rx = rect.x;
            int ry = rect.y;
            rect.change(new Element(rx + Math.min(start.x, end.x), ry + Math.min(start.y, end.y), w, h));
          }
          dragStart = null;
          dragCurrent = null;
          content.repaint();
          resizeToFrame();
        }
        lastDrag = null;
        dragButton = 1;
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
        setDragButton(e.getButton());
        myMouseMoved(e);
        myMouseDragged(e);
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
    pBox.add(content);
    pBox.add(status);
    running = true;
    new Thread(new ClickHandler()).start();
    scrW = Do.onMain().w;
    scrH = Do.onMain().h;
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
            rect.x + mousePos.x, rect.y + mousePos.y, rect.x, rect.y, rect.w, rect.h, base.getName()));
  }
  //</editor-fold>

  //<editor-fold desc="mouse click/move/drag">
  private void myMouseClicked(MouseEvent e) {
    String doubleClick = e.getClickCount() > 1 ? "Double" : "";
    log.trace("mouse%sClicked: %d,%d", doubleClick, e.getX(), e.getY());
    if (!SX.isSet(doubleClick)) {
      setClickStatusClicked(new Element(e.getX(), e.getY(), e.getButton()));
    } else {
      setClickStatusDoubleClicked();
    }
  }

  int exited = -1;

  private void myMouseMoved(MouseEvent e) {
    Element elem = getPos(new Element(e.getX(), e.getY()));
    updateStatus(elem);
  }

  private void myMouseDragged(MouseEvent e) {
    if (getActiveSide() == ALL) {
      if (SX.isNull(lastDrag)) {
        dragStart = e;
      }
      dragCurrent = e;
      content.repaint();
    } else {
      dragSelection(e);
    }
    lastDrag = e;
  }

  private void dragSelection(MouseEvent e) {
    int step = 1;
    if (SX.isNotNull(lastDrag) && exited < 0) {
      if (activeSide == NONE) {
        if (rect.w > halfCenteredWidth && rect.h > halfCenteredWidth) {
          step = 10;
        }
        Element newRect = null;
        if (e.getY() > lastDrag.getY()) {
          if (e.getX() > lastDrag.getX()) {
            newRect = new Element(rect.x - step, rect.y - step, rect.w, rect.h);
          } else if (e.getX() > lastDrag.getX()) {
            newRect = new Element(rect.x + step, rect.y - step, rect.w, rect.h);
          } else {
            newRect = new Element(rect.x, rect.y - step, rect.w, rect.h);
          }
        } else if (e.getY() < lastDrag.getY()) {
          if (e.getX() > lastDrag.getX()) {
            newRect = new Element(rect.x - step, rect.y + step, rect.w, rect.h);
          } else if (e.getX() > lastDrag.getX()) {
            newRect = new Element(rect.x + step, rect.y + step, rect.w, rect.h);
          } else {
            newRect = new Element(rect.x, rect.y + step, rect.w, rect.h);
          }
        } else {
          if (e.getX() > lastDrag.getX()) {
            newRect = new Element(rect.x - step, rect.y, rect.w, rect.h);
          } else {
            newRect = new Element(rect.x + step, rect.y, rect.w, rect.h);
          }
        }
        if (SX.isNotNull(newRect)) {
          log.info("action: move %s", getActiveSideText());
          int newRectW = newRect.w;
          int newRectH = newRect.h;
          newRect.intersect(base);
          if (newRect.w == newRectW && newRect.h == newRectH) {
            rect.change(newRect);
            resizeToFrame();
          }
        }
      }
    }
  }
  //</editor-fold>

  //<editor-fold desc="mouse enter/exit">
  private void myMouseEntered(MouseEvent e) {
//    if (!isDragging() && activeSide != NONE && activeSide != ALL) {
//      int side = whichSide(e.getX() - borderThickness, e.getY() - borderThickness);
//      if (side == exited) {
//        log.trace("mouseEntered: %s", activeSides[side]);
//        content.setBorder(coloredSide(side));
//        box.repaint();
//      }
//    }
    exited = -1;
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
    return point < range / whichSideMargin;
  }

  private boolean isMiddle(int range, int point) {
    int lower = range / whichSideMargin;
    int upper = range - lower;
    return point > lower && point < upper;
  }

  private boolean isHigh(int range, int point) {
    return point > range - range / whichSideMargin;
  }
  //</editor-fold>

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

  //<editor-fold desc="key typed">
  private void myKeyTyped(KeyEvent e) {
    boolean shouldQuit = false;
    String allowed = " +-acrposftmnihqz";
    if (e.CHAR_UNDEFINED != e.getKeyChar()) {
      String sKey = "" + e.getKeyChar();
      if (e.getKeyChar() == VK_ESCAPE) {
        log.trace("action: quit request");
        sKey = "#";
        shouldQuit = true;
      } else {
        if (!allowed.contains(sKey)) {
          log.trace("keyTyped: %s", sKey);
          return;
        }
        if (" ".equals("" + e.getKeyChar())) {
          if (activeSide == NONE) {
            activeSide = activeSideSaved;
          } else {
            activeSideSaved = activeSide;
            activeSide = NONE;
          }
          content.setBorder(coloredSide(activeSide));
          box.pack();
        } else if ("+".equals("" + e.getKeyChar())) {
          zoomIn();
          log.trace("action: zoom-in to %s", rect);
        } else if ("-".equals("" + e.getKeyChar())) {
          zoomOut();
          log.trace("action: zoom-out to %s", rect);
        } else if ("r".equals("" + e.getKeyChar())) {
          log.trace("action: reset");
          actionReset();
        } else if ("p".equals("" + e.getKeyChar())) {
          log.trace("action: bundlePath");
          actionBundlePath(box);
        } else if ("o".equals("" + e.getKeyChar())) {
          log.trace("action: open");
          actionOpen(box);
        } else if ("c".equals("" + e.getKeyChar())) {
          log.trace("action: capture");
          actionCapture();
        } else if ("s".equals("" + e.getKeyChar())) {
          log.trace("action: save");
          actionSave();
        } else if ("f".equals("" + e.getKeyChar())) {
          log.trace("action: find: %s", rect);
          actionFind();
        } else if ("a".equals("" + e.getKeyChar())) {
          log.trace("action: findAll: %s", rect);
          actionFindAll();
        } else if ("t".equals("" + e.getKeyChar())) {
          log.trace("action: set target");
          actionTarget();
        } else if ("m".equals("" + e.getKeyChar())) {
          log.trace("action: set similarity");
          actionSimilar();
        } else if ("n".equals("" + e.getKeyChar())) {
          log.trace("action: change name");
          actionName();
        } else if ("i".equals("" + e.getKeyChar())) {
          log.trace("action: show info");
          actionInfo();
        } else if ("h".equals("" + e.getKeyChar())) {
          log.trace("action: show help");
          actionHelp();
        } else if ("z".equals("" + e.getKeyChar())) {
          log.trace("action: revert");
          actionRevert();
        } else if ("q".equals("" + e.getKeyChar())) {
          log.trace("action: quit request");
          shouldQuit = true;
        }
      }
      if (shouldQuit) {
        if (dirty) {
          if (!Do.popAsk("discard not saved image?", "SikuliX Tool::SaveImage", box)) {
            actionSave();
          }
          dirty = false;
        }
        box.setVisible(false);
        if (!inBackground) {
          intro.setVisible(true);
        } else {
          if (!Do.popAsk("still want to run in background?" +
                          "\n- use ctrl-alt-2 to capture" +
                          "\n- use ctrl-alt-1 for back to foreground",
                  "SikulixTool::ToBackground", intro)) {
            inBackground = false;
            intro.setVisible(true);
          }
        }
      }
    }
  }
  //</editor-fold>

  //<editor-fold desc="actions">
  String hotKeyCapture = "";
  String hotKeyCaptureFinal = "";
  String hotKeyCaptureDefault = "ctrl alt 2";
  HotkeyCallback hotkeyCaptureListener = new HotkeyCallback() {
    @Override
    public void hotkeyPressed(HotkeyEvent e) {
      if (inBackground) {
        log.trace("Hotkey: %s (action capture)", e);
        actionCapture();
      }
    }
  };

  String hotkeyForeground = "";
  String hotkeyForegroundFinal = "";
  String hotkeyForegroundDefault = "ctrl alt 1";
  HotkeyCallback hotkeyForegroundListener = new HotkeyCallback() {
    @Override
    public void hotkeyPressed(HotkeyEvent e) {
      if (inBackground) {
        log.trace("Hotkey: %s (back to foreground)", e);
        inBackground = false;
        intro.setVisible(true);
      }
    }
  };

  private boolean inBackground = false;

  private void actionToBackground() {
    if (!inBackground && SX.isNotSet(hotkeyForegroundFinal)) {
      hotkeyForeground = hotkeyForegroundDefault;
      hotkeyForegroundFinal = HotkeyManager.get().addHotkey(hotkeyForegroundListener, hotkeyForeground);
      if (SX.isSet(hotkeyForegroundFinal)) {
        hotKeyCapture = hotKeyCaptureDefault;
        hotKeyCaptureFinal = HotkeyManager.get().addHotkey(hotkeyCaptureListener, hotKeyCapture);
        if (SX.isSet(hotKeyCaptureFinal)) {
          inBackground = true;
          intro.setVisible(false);
        }
      }
    } else {
      inBackground = true;
      intro.setVisible(false);
    }
  }

  private void actionCapture() {
    box.setVisible(false);
    base = new Element(scrID).capture();
    isImage = false;
    activeSide = ALL;
    resetBox();
    dirty = true;
  }

  private void actionReset() {
    resetBox();
  }

  private void resetBox() {
    box.setVisible(false);
    rect = new Element(this.base);
    dirty = false;
    resizeToFrame();
    box.setVisible(true);
  }

  private void actionFind() {
    Runnable find = new Runnable() {
      @Override
      public void run() {
        box.setVisible(false);
        Picture where = base;
        Element what = base.getSub(rect);
        if (isImage) {
          SX.pause(0.3);
          where = new Element(scrID).capture();
        }
        Do.find(what, where);
        where.showMatch();
        box.setVisible(true);
      }
    };
    new Thread(find).start();
  }

  private void actionFindAll() {
    Runnable find = new Runnable() {
      @Override
      public void run() {
        box.setVisible(false);
        Picture where = base;
        Element what = base.getSub(rect);
        if (isImage) {
          SX.pause(0.3);
          where = new Element(scrID).capture();
        }
        Do.findAll(what, where);
        where.showMatches();
        box.setVisible(true);
      }
    };
    new Thread(find).start();
  }

  private void actionQuit() {
    if (dirty) {
      log.error("image not saved yet");
    }
    log.trace("waiting for subs to end");
    SX.setOption("Tool.bundlePath", bundlePath);
    SX.saveOptions();
    if (SX.isNotNull(box)) {
      box.dispose();
    }
    intro.dispose();
    running = false;
    HotkeyManager.get().stop();
    SX.pause(1);
  }

  private boolean actionBundlePath(JFrame frame) {
    String result = selectPath("Select BundlePath", frame);
    boolean success = false;
    if (SX.isSet(result)) {
      bundlePath = result;
      success = true;
    }
    log.trace("new bundlePath: %s", result);
    return success;
  }

  private String selectPath(String title, JFrame frame) {
    String path = null;
    File fPath = FileChooser.folder(frame, title);
    if (SX.isNotNull(fPath)) {
      path = fPath.getAbsolutePath();
    }
    return path;
  }

  private void actionOpen(JFrame frame) {
    frame.setVisible(false);
    File fImage = FileChooser.loadImage(frame, new File(bundlePath));
    if (SX.isNotNull(fImage)) {
      String result = fImage.getAbsolutePath();
      log.trace("new image: %s", result);
      base = new Picture(result);
      if (base.isValid()) {
        rect = new Element(base);
        isImage = true;
        resetBox();
        if (!bundlePath.equals(fImage.getParent()) && Do.popAsk(fImage.getParent() +
                "\nUse this folder as bundlepath?", "SikuliX Tool::BundlePath", box)) {
          bundlePath = fImage.getParent();
        }
        updateStatus();
        return;
      }
    }
    frame.setVisible(true);
  }

  private void actionSave() {
    boolean shouldSelectPath = false;
    String savePath = bundlePath;
    if (SX.isNotSet(savePath)) {
      shouldSelectPath = true;
    } else {
      if (!Do.popAsk(savePath +
              "\nSave to this folder (bundle path)?", "SikuliX Tool::ImageSave", box)) {
        shouldSelectPath = true;
      }
    }
    if (shouldSelectPath) {
      savePath = selectPath("Select a Directory/Folder", box);
      if (SX.isNotSet(savePath)) {
        return;
      }
    }
    if (!base.hasName()) {
      actionName();
    }
    if (!base.hasName()) {
      return;
    }
    if (SX.existsImageFile(savePath, base.getName())) {
      if (!Do.popAsk(new File(savePath, base.getName()).getAbsolutePath() +
              "\nOverwrite image file?", "SikuliX Tool::ImageSave", box)) {
        return;
      }
    }
    if (shot.save(base.getName(), savePath)) {
      if (!bundlePath.equals(savePath) && Do.popAsk(savePath +
              "\nUse this folder as bundlepath?", "SikuliX Tool::BundlePath", box)) {
        bundlePath = savePath;
      }
      dirty = false;
      updateStatus();
    }
  }

  private void actionTarget() {
    dirty = true;
  }

  private void actionSimilar() {
    dirty = true;
  }

  private void actionName() {
    String newName = Do.input("Change image name", "SikuliX Tool::ImageName",
            (base.hasName() ? base.getName() : ""), box);
    if (SX.isSet(newName) && !newName.equals(base.getName())) {
      base.setName(newName);
      dirty = true;
      updateStatus();
    }
  }

  private void actionInfo() {
    Do.popup(imgInfo(), "PictureTool::Information", box);
  }

  private void actionHelp() {
  }

  private void actionRevert() {
    if (lastRects.size() > 0) {
      log.trace("action: revert crop from %s to %s", rect, lastRects.get(0));
      rect = lastRects.remove(0);
      resizeToFrame();
    }
  }
  //</editor-fold>

  //<editor-fold desc="key released (special keys)">
  private void myKeyReleased(KeyEvent e) {
    int code = e.getKeyCode();
    if (e.CHAR_UNDEFINED != e.getKeyChar()) {
    } else {
      String cName = keyReleasedHandler(code, 1);
      log.trace("keyReleased: %s (%d) %s %s %s", e.getKeyText(code), code, cName,
              activeSides[activeSide], activeSides[activeSideSaved]);
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
        if (activeSide == ALL) {
          activeSide = activeSideSaved;
        } else {
          activeSideSaved = activeSide;
          activeSide = ALL;
        }
        cName = "#META";
        content.setBorder(coloredSide(activeSide));
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
  //</editor-fold>

  //<editor-fold desc="coloredSide">
  private Border coloredSide(int side) {
    return coloredSide(side, Color.RED, Color.GREEN);
  }

  private Border coloredSide(int side, Color outerCol, Color innerCol) {
    Border inner = null;
    Border outer = null;
    if (side == NONE) {
      activeSide = NONE;
      return BorderFactory.createLineBorder(outerCol, borderThickness);
    } else if (side == ALL) {
      activeSide = ALL;
      return BorderFactory.createLineBorder(innerCol, borderThickness);
    } else if (side == COUNTERCLOCKWISE) {
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
    }
    exited = -1;
    dirty = true;
    resizeToFrame();
  }

  void center(Element clicked) {
    pushRect();
    clicked = getPos(clicked);
    int x = rect.x + clicked.x;
    int y = rect.y + clicked.y;
    if (activeSide > RIGHT) {
      rect.x = Math.max(0, x - halfCenteredWidth * base.w / base.h);
      rect.y = Math.max(0, y - halfCenteredWidth);
      rect.w = 2 * Math.min(halfCenteredWidth * base.w / base.h, Math.min(x, base.w - x));
      rect.h = 2 * Math.min(halfCenteredWidth, Math.min(y, base.h - y));
    } else {
      rect.x = x - rect.w / 2;
      rect.y = y - rect.y / 2;
    }
    exited = -1;
    dirty = true;
    resizeToFrame();
  }

  private void zoomOut() {
    int stepX, stepY;
    stepX = stepY = 1;
    if (rect.w > halfCenteredWidth && rect.h > halfCenteredWidth) {
      stepX = Math.max(1, rect.w / 10);
      stepY = Math.max(1, rect.h / 10);
    }
    rect.x -= stepX;
    rect.w += 2 * stepX;
    rect.y -= stepY;
    rect.h += 2 * stepY;
    dirty = true;
    resizeToFrame();
  }

  private void zoomIn() {
    int stepX, stepY;
    stepX = stepY = 1;
    if (rect.w > halfCenteredWidth && rect.h > halfCenteredWidth) {
      stepX = Math.max(1, rect.w / 10);
      stepY = Math.max(1, rect.h / 10);
    }
    rect.x += stepX;
    rect.w -= 2 * stepX;
    rect.y += stepY;
    rect.h -= 2 * stepY;
    dirty = true;
    resizeToFrame();
  }

  private void checkSelection() {
    rect.intersect(base);
    Element minimum = rect.getCenter().grow();
    if (rect.w < minWidthHeight || rect.h < minWidthHeight) {
      rect.change(minimum);
    }
  }

  double resizeFactor = 10;

  private void resizeToFrame() {
    checkSelection();
    shot = base.getSub(rect);
    if (activeSide > RIGHT) {
      double wFactor = rect.w / (scrW * 0.85f);
      double hFactor = rect.h / (scrH * 0.85f);
      resizeFactor = 1 / Math.max(wFactor, hFactor);
      resizeFactor = Math.min(resizeFactor, 10);
    }
    BufferedImage img = getResizedAsBufferedImage(shot, resizeFactor);
    shotDisplayedW = img.getWidth();
    shotDisplayedH = img.getHeight();
    Dimension dim = new Dimension(img.getWidth() + 2 * borderThickness,
            img.getHeight() + 2 * borderThickness + statusHeight);
    content.setIcon(new ImageIcon(img));
    content.setBorder(coloredSide(activeSide));
    pBox.setPreferredSize(dim);
    box.pack();
    box.setLocation((int) ((scrW - dim.getWidth()) / 2), (int) (scrH - dim.getHeight()) / 2);
    updateStatus();
  }

  private BufferedImage getResizedAsBufferedImage(Picture img, double factor) {
    BufferedImage bImg = null;
    Mat resizedMat = img.getResizedMat(factor);
    bImg = Picture.getBufferedImage(resizedMat);
    return bImg;
  }

  private void drawSelection(Graphics2D g2d) {
    if (SX.isNotNull(dragStart)) {
      g2d.setStroke(new BasicStroke(2));
      g2d.setColor(Color.blue);
      int x, y, x2, y2, w, h;
      int crossVx1, crossVy1, crossVx2, crossVy2, crossHx1, crossHy1, crossHx2, crossHy2;
      if (dragButton > 1) {
        int xD = (dragCurrent.getX() - dragStart.getX()) / 2;
        int yD = (dragCurrent.getY() - dragStart.getY()) / 2;
        x = dragStart.getX() - xD;
        y = dragStart.getY() - yD;
        x2 = dragStart.getX() + xD;
        y2 = dragStart.getY() + yD;
      } else {
        x = dragStart.getX();
        y = dragStart.getY();
        x2 = dragCurrent.getX();
        y2 = dragCurrent.getY();
      }
      w = x2 - x;
      h = y2 - y;
      g2d.drawLine(x, y, x + w, y);
      g2d.drawLine(x + w, y, x + w, y + h);
      g2d.drawLine(x + w, y + h, x, y + h);
      g2d.drawLine(x, y + h, x, y);
      crossVx1 = x + w / 2;
      crossVy1 = y;
      crossVx2 = crossVx1;
      crossVy2 = y + h;
      crossHx1 = x;
      crossHy1 = y + h / 2;
      crossHx2 = x + w;
      crossHy2 = crossHy1;
      g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
              0, new float[]{3, 2}, 0));
      g2d.drawLine(crossVx1, crossVy1, crossVx2, crossVy2);
      g2d.drawLine(crossHx1, crossHy1, crossHx2, crossHy2);
    }
  }

  private void drawCrossHair(Graphics2D g2d) {
    g2d.setColor(Color.red);
    g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
            0, new float[]{3, 2}, 0));
    g2d.drawLine(shotDisplayedW / 2 + borderThickness, borderThickness,
            shotDisplayedW / 2 + borderThickness, shotDisplayedH);
    g2d.drawLine(borderThickness, shotDisplayedH / 2 + borderThickness,
            shotDisplayedW, shotDisplayedH / 2 + borderThickness);

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

//  private String imgName = "";

  private String getImageName() {
    if (base.hasName()) {
      return base.getName();
    }
    return "";
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
