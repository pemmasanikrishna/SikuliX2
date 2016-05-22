/*
 * Copyright (c) 2016 - sikulix.com - License MIT
 */

package com.sikulix;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import org.gjt.sp.jedit.jEdit;

import java.util.Properties;

public class Sikulix extends SX {

  static SXLog log = null;

  public static void main(String[] args) {
    log = getLogger("SX.IDE", args);

    // TODO only works in project context (export resources)
    //String fpExtensionsJEdit = SX.getFolder(SX.getSXEDITOR(), "jEdit").getAbsolutePath();
    String fpExtensionsJEdit = SX.getFolder(SX.fSxProject,
            "sikulix/target/classes/SXEditor/jEdit").getAbsolutePath();
    System.setProperty("jedit.home", fpExtensionsJEdit);
    System.setProperty("awt.useSystemAAFontSettings", "on");
    System.setProperty("swing.aatext", "true");
    String[] argsJEdit = {"-noserver", "-nobackground"};
    jEdit.main(argsJEdit);
  }
}
