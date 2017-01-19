/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.util;

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

  final static java.util.List<Element> highlights = new ArrayList<>();

  private Picture base = null;
  private BufferedImage baseDisplayed = null;
  private JFrame frame = new JFrame();

  public SXHighlight(Picture base) {
    this.base = base;
    frame.setUndecorated(true);
    frame.setAlwaysOnTop(true);
//    setBackground(new Color(255, 255, 255, 80));
    init();
  }

  private void init() {
    JPanel panel = new JPanel() {
      public boolean isOptimizedDrawingEnabled() {
        return false;
      }
    };
    JLabel label = new JLabel();
    label.setIcon(new ImageIcon(base.get()));
    LayoutManager overlay = new OverlayLayout(panel);
    panel.setLayout(overlay);
    panel.add(label);
    frame.add(panel);
    frame.pack();
    frame.setLocation(0, 0);
  }

  public void on() {
    frame.setVisible(true);
    SX.pause(2);
    frame.setVisible(false);
  }

  public void add(Element elem) {
    highlights.add(elem);
  }

  public void stop() {
    frame.dispose();
  }

}
