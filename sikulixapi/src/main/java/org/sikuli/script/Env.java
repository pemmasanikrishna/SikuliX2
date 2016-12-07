/*
 * Copyright 2010-2014, Sikuli.org, sikulix.com
 * Released under the MIT License.
 *
 * modified RaiMan
 */
package org.sikuli.script;

import com.sikulix.api.Element;
import com.sikulix.core.SX;

/**
 * features moved to other classes, details below with the methods
 */
public class Env {

  /**
   * @return where we store Sikuli specific data
   * @deprecated use getSXAPP()
   */
  public static String getSikuliDataPath() {
    return SX.getSXAPP();
  }

  /**
   * @return version
   * @deprecated use getVersion()
   */
  @Deprecated
  public static String getSikuliVersion() {
    return SX.getSXVERSION();
  }

  /**
   * @return current Location
   * @deprecated use at() instead
   */
  @Deprecated
  public static Location getMouseLocation() {
    return new Location(SX.at());
  }

  /**
   * @return version (java: os.version)
   * @deprecated use getSystemVersion
   */
  @Deprecated
  public static String getOSVersion() {
    return SX.getSYSTEMVERSION();
  }

  /**
   * @deprecated use getSystem()
   */
  @Deprecated
  public static String getOS() {
    return SX.getSYSTEMVERSION();
	}
}
