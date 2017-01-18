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

public class SXHighlight extends JFrame {

  static SXLog log;

  static {
    log = SX.getLogger("SX.Capture");
    log.isSX();
    log.on(SXLog.INFO);
  }

  final static java.util.List<Element> highlights = new ArrayList<>();

  private Picture base = null;
  private BufferedImage baseDisplayed = null;

  public SXHighlight(Picture base) {
    super();
    this.base = base;
    setUndecorated(true);
    setAlwaysOnTop(true);
//    setBackground(new Color(255, 255, 255, 80));
    setBounds(base.getRectangle());
    init();
  }

  private void init() {
    RescaleOp op = new RescaleOp(0.6f, 0, null);
    baseDisplayed = op.filter(base.get(), null);
  }

  public void on() {
    setVisible(true);
    SX.pause(3);
    setVisible(false);
  }

  public void add(Element elem) {
    highlights.add(elem);
  }

  @Override
  public void paint(Graphics g) {
    if (base != null) {
      Graphics2D g2dWin = (Graphics2D) g;
      BufferedImage bi = new BufferedImage(base.w, base.h, baseDisplayed.getType());
      Graphics2D bfG2 = bi.createGraphics();
      bfG2.drawImage(baseDisplayed, 0, 0, this);
      g2dWin.drawImage(bi, 0, 0, this);
    }
  }

  public void stop() {
    dispose();
  }

}
