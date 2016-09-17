/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.IDE;

import com.sikulix.core.Content;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.api.Commands;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
//import org.gjt.sp.jedit.jEdit;

public class Sikulix extends SX {

  static SXLog log = null;

  public static void main(String[] args) {
    log = getLogger("SX.IDE", args);
    File fExtensionsJEdit = SX.getFileExists(SX.getSXEDITOR(), "jedit");
    boolean jeditAvailable = true;
    File fJEditJar = null;
    if (SX.isNull(fExtensionsJEdit)) {
      jeditAvailable = false;
    }
    if (jeditAvailable) {
      fJEditJar = SX.getFileExists(SX.getSXEDITOR(), "jedit-" + SX.getOption("sxjedit") + ".jar");
      if (SX.isNull(fJEditJar)) {
        jeditAvailable = false;
      }
    }
    if (jeditAvailable) {
      Content.addClassPath(fJEditJar.getAbsolutePath());
      Method mMain = null;
      try {
        Class cJEdit = Class.forName("org.gjt.sp.jedit.jEdit");
        mMain = cJEdit.getDeclaredMethod("main", (new String[]{}).getClass());
      } catch (Exception e) {
        log.error("org.gjt.sp.jedit.jEdit::main not on classpath");
        System.exit(1);
      }
      System.setProperty("jedit.home", fExtensionsJEdit.getAbsolutePath());
      System.setProperty("awt.useSystemAAFontSettings", "on");
      System.setProperty("swing.aatext", "true");
      String[] argsJEdit = {"-noserver", "-nobackground"};
      try {
        mMain.invoke(null, new Object[]{argsJEdit});
      } catch (Exception e) {
        log.error("org.gjt.sp.jedit.jEdit::main not working (%s)", e.getMessage());
        System.exit(1);
      }
    } else {
      Commands.popError("jEdit 5.3.0 is not available/n" +
              "Download the jEdit package from:\n" +
              "http://download.sikulix.com/SXEditor.zip\n" +
              "and unzip in folder <SikulixAppData>/Extensions\n" +
              "so you then have a folder: <SikulixAppData>/Extensions/SXEditor", "SikuliX2::IDE");
      Commands.input("Copy the URL and paste in a browser:",
              "http://download.sikulix.com/SXEditor.zip", "SikuliX2::IDE");
    }
  }
}
