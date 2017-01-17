/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.util;

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

public class SXCapture {

  static SXLog log;

  static {
    log = SX.getLogger("SX.Capture");
    log.isSX();
    log.on(SXLog.INFO);
  }

  final static java.util.List<Object> shot = new ArrayList<Object>();
  static int cbCaptureKey = NativeKeyEvent.VC_SHIFT_L;
  static int cbpCaptureKey = NativeKeyEvent.VC_CONTROL_L;

  JFrame frm = null;

  public SXCapture() {
    frm = new JFrame();
    frm.setUndecorated(true);
    frm.setAlwaysOnTop(true);
    frm.setBackground(new Color(255, 255, 255, 50));
    frm.setBounds(SX.getMain().getRectangle());
    frm.setVisible(true);
    init();
  }

  private void init() {
  }

}
