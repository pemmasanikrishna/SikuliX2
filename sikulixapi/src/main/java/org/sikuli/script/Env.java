/*
 * Copyright 2010-2014, Sikuli.org, sikulix.com
 * Released under the MIT License.
 *
 * modified RaiMan
 */
package org.sikuli.script;

import org.sikuli.core.SX;
import org.sikuli.core.Location;

/**
 * features moved to other classes, details below with the methods
 */
public class Env extends SX {

  /**
   * @return where we store Sikuli specific data
   * @deprecated use getAppDataPath()
   */
  public static String getSikuliDataPath() {
    return getAppDataPath();
  }

  /**
   * @return version
   * @deprecated use getVersion()
   */
  @Deprecated
  public static String getSikuliVersion() {
    return getVersion();
  }

  /**
   * @return current Location
   * @deprecated use at() instead
   */
  @Deprecated
  public static Location getMouseLocation() {
    return at();
  }

  /**
   * @return version (java: os.version)
   * @deprecated use getSystemVersion
   */
  @Deprecated
  public static String getOSVersion() {
    return getSystemVersion();
  }

  /**
   * @deprecated use getSystem()
   */
  @Deprecated
  public static String getOS() {
    return getSystem();
	}
}
