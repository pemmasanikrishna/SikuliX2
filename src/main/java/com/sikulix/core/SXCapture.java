/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.sikulix.api.Do;
import com.sikulix.api.Element;
import com.sikulix.core.NativeHook;
import com.sikulix.core.NativeHookCallback;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import org.jnativehook.NativeInputEvent;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.mouse.NativeMouseEvent;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

public class SXCapture extends JFrame {

  static SXLog log;

  static {
    log = SX.getLogger("SX.Capture");
    log.isSX();
    log.on(SXLog.INFO);
  }

  final static java.util.List<Object> shot = new ArrayList<Object>();
  static int cbCaptureKey = NativeKeyEvent.VC_SHIFT_L;
  static int cbpCaptureKey = NativeKeyEvent.VC_CONTROL_L;

  public SXCapture() {
    super();
    setUndecorated(true);
    setAlwaysOnTop(true);
    setBackground(new Color(255, 255, 255, 80));
    setBounds(Do.onMain().getRectangle());
    setVisible(true);
    init();
  }

  private void init() {
  }

  public void stop() {
    dispose();
  }

}
