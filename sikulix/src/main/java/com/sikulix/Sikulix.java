/*
 * Copyright (c) 2016 - sikulix.com - License MIT
 */

package com.sikulix;

import org.gjt.sp.jedit.jEdit;

import java.util.Properties;

public class Sikulix {
  public static void main(String[] args) {
    Properties props = System.getProperties();
    String fpUserHome = System.getProperty("user.home");
    String fpSikulixStore = fpUserHome + "/Library/Application Support/Sikulix";
    String fpExtensionsJEdit = fpSikulixStore + "/Extensions/jEdit";
    System.setProperty("jedit.home", fpExtensionsJEdit);
    System.setProperty("awt.useSystemAAFontSettings", "on");
    System.setProperty("swing.aatext", "true");
    String[] argsJEdit = {"-noserver", "-nobackground"};
    jEdit.main(argsJEdit);
  }
}
