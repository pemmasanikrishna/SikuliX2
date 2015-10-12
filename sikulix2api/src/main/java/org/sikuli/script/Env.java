/*
 * Copyright 2010-2014, Sikuli.org, sikulix.com
 * Released under the MIT License.
 *
 * modified RaiMan
 */
package org.sikuli.script;

import org.sikuli.util.hotkey.HotkeyManager;
import org.sikuli.util.hotkey.HotkeyListener;
import org.sikuli.zdeprecated.natives.OSUtil;
import org.sikuli.basics.Settings;
import org.sikuli.zdeprecated.natives.SysUtil;

/**
 * features moved to other classes, details below with the methods
 * @deprecated
 */
@Deprecated
public class Env {

  /**
   *
   * @return where we store Sikuli specific data
   * @deprecated use Settings. ... instead
   */
  @Deprecated
  public static String getSikuliDataPath() {
    return Settings.getSikuliDataPath();
  }

  /**
   * @return version
   * @deprecated use Settings.SikuliVersion
   */
  @Deprecated
  public static String getSikuliVersion() {
    return RunTime.get().SikulixVersion;
  }
  
  /**
   * @return current Location
   * @deprecated use {@link Mouse#at()} instead
   */
  @Deprecated
  public static Location getMouseLocation() {
    return Mouse.at();
  }

  @Deprecated
  public static OSUtil getOSUtil() {
    return SysUtil.getOSUtil();
  }

  /**
   * @return version (java: os.version)
   * @deprecated use Settings. ... instead
   */
  @Deprecated
  public static String getOSVersion() {
    return Settings.getOSVersion();
  }

  /**
   * @deprecated use Command features --- see docs
   */
  @Deprecated
  public static void getOS() throws Exception{
    throw new NoSuchMethodException("no longer supported --- see docs");
	}

  /**
   * @return true/false
   * @deprecated use Command features --- see docs
   */
  @Deprecated
  public static boolean isWindows() {
    return RunTime.get().runningWindows;
  }

  /**
   * @return true/false
   * @deprecated use Command features --- see docs
   */
  @Deprecated
  public static boolean isLinux() {
    return RunTime.get().runningLinux;
  }

  /**
   * @return true/false
   * @deprecated use Command features --- see docs
   */
  @Deprecated
  public static boolean isMac() {
    return RunTime.get().runningMac;
  }

  /**
   * @return path seperator : or ;
   * @deprecated use Settings.getPathSeparator() ... instead
   */
  @Deprecated
  public static String getSeparator() {
    return Settings.getPathSeparator();
  }

  /**
   *
   * @return content
   * @deprecated use App. ... instead
   */
  @Deprecated
  public static String getClipboard() {
    return App.getClipboard();
  }

  /**
   * set content
   *
   * @param text text
   * @deprecated use App. ... instead
   */
  @Deprecated
  public static void setClipboard(String text) {
		App.setClipboard(text);
	}

  /**
   * get the lock state of the given key
   * @param key respective key specifier according class Key
   * @return true/false
   * @deprecated use Key. ... instead
   */
  @Deprecated
  public static boolean isLockOn(char key) {
    return Key.isLockOn(key);
  }

  /**
   *
   * @return System dependent key
   * @deprecated use Key. ... instead
   */
  @Deprecated
  public static int getHotkeyModifier() {
    return Key.getHotkeyModifier();
  }

  /**
   *
   * @param key respective key specifier according class Key
   * @param modifiers respective key specifier according class KeyModifiers
   * @param listener a HotKeyListener instance
   * @return true if ok, false otherwise
   * @deprecated use Key. ... instead
   */
  @Deprecated
  public static boolean addHotkey(String key, int modifiers, HotkeyListener listener) {
    return Key.addHotkey(key, modifiers, listener);
  }

  /**
   *
   * @param key respective key specifier according class Key
   * @param modifiers respective key specifier according class KeyModifiers
   * @param listener a HotKeyListener instance
   * @return true if ok, false otherwise
   * @deprecated use Key. ... instead
   */
  @Deprecated
  public static boolean addHotkey(char key, int modifiers, HotkeyListener listener) {
    return Key.addHotkey(key, modifiers, listener);
  }

  /**
   *
   * @param key respective key specifier according class Key
   * @param modifiers respective key specifier according class KeyModifiers
   * @return true if ok, false otherwise
   * @deprecated use Key. ... instead
   */
  @Deprecated
  public static boolean removeHotkey(String key, int modifiers) {
    return Key.removeHotkey(key, modifiers);
  }

  /**
   *
   * @param key respective key specifier according class Key
   * @param modifiers respective key specifier according class KeyModifiers
   * @return true if ok, false otherwise
   * @deprecated use Key. ... instead
   */
  @Deprecated
  public static boolean removeHotkey(char key, int modifiers) {
    return Key.removeHotkey(key, modifiers);
  }

//TODO where to use???
	public static void cleanUp() {
    HotkeyManager.getInstance().cleanUp();
  }
}
