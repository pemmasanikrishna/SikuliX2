/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package org.sikuli.script;

import com.sikulix.api.Element;
import com.sikulix.core.LocalDevice;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

public class Screen extends Region {
  private static eType eClazz = eType.SCREEN;
  private static SXLog log = SX.getLogger("SX." + eClazz.toString());

  protected void setClazz() {
    clazz = eClazz;
  }

  protected void copy(Element elem) {
    super.copy(elem);
  }

  protected void initAfter() {
    initName(eClazz);
  }

  private int id = -1;

  public Screen() {
    init(SX.getSXLOCALDEVICE().getMonitor());
    id = 0;
    initAfter();
  }

  public Screen(int id) {
    init(SX.getSXLOCALDEVICE().getMonitor(id));
    this.id = isOn();
    initAfter();
  }

  public String toStringPlus() {
    return " #" + id;
  }

  /**
   * show the current monitor setup
   */
  public static void showMonitors() {
    log.p("*** monitor configuration [ %s Screen(s)] ***", SX.getSXLOCALDEVICE().getNumberOfMonitors());
    log.p("*** Primary is Screen %d", SX.getSXLOCALDEVICE().getMainMonitorID());
    for (int i = 0; i < SX.getSXLOCALDEVICE().getNumberOfMonitors(); i++) {
      log.p("%d: %s", i, new Screen(i));
    }
    log.p("*** end monitor configuration ***");
  }

  /**
   * re-initialize the monitor setup (e.g. when it was changed while running)
   */
  public static void resetMonitors() {
    showMonitors();
    log.p("*** TRYING *** to reset the monitor configuration");
    SX.getSXLOCALDEVICE().resetMonitors();
    showMonitors();
  }

}
