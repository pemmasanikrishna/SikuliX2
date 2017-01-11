/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

//import org.sikuli.script.Key;
//import org.sikuli.util.Debug;
//import org.sikuli.util.PreferencesUser;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import com.tulskiy.keymaster.common.Provider;

/**
 * Singleton class to bind hotkeys to hotkey listeners
 */
public abstract class HotkeyManager {

  private static SXLog log = SX.getLogger("SX.HotkeyManager");

  private static HotkeyManager _instance = null;
  private static Map<Integer, Integer> hotkeys;
  private static Map<Integer, Integer> hotkeysGlobal;
  private static final String HotkeyTypeCapture = "Capture";
  private static int HotkeyTypeCaptureKey;
  private static int HotkeyTypeCaptureMod;
  private static final String HotkeyTypeAbort = "Abort";
  private static int HotkeyTypeAbortKey;
  private static int HotkeyTypeAbortMod;

  public static HotkeyManager getInstance() {
    if (_instance == null) {
      hotkeys = new HashMap<Integer, Integer>();
      hotkeysGlobal = new HashMap<Integer, Integer>();
      Provider hotkeys = Provider.getCurrentProvider(false);
    }
    return _instance;
  }

  /**
   * remove all hotkeys
   */
  public static void reset() {
    if (_instance == null || hotkeys.isEmpty()) {
      return;
    }
    log.debug("HotkeyManager: reset - removing all defined hotkeys.");
    boolean res;
    int[] hk = new int[hotkeys.size()];
    int i = 0;
    for (Integer k : hotkeys.keySet()) {
      //res = _instance._removeHotkey(k, hotkeys.get(k));
      res = true;
      if (!res) {
        log.error("HotkeyManager: reset: failed to remove hotkey: %s %s",
                getKeyModifierText(hotkeys.get(k)), getKeyCodeText(k));
        hk[i++] = -1;
      } else {
        hk[i++] = k;
        log.debug("removed (%d, %d)" , k, hotkeys.get(k));
      }
    }
    for (int k : hk) {
      if (k == -1) {
        continue;
      }
      hotkeys.remove(k);
    }
  }

  private static String getKeyCodeText(int key) {
    return KeyEvent.getKeyText(key).toUpperCase();
  }

  private int getCaptureHotkey() {
    return 0;
  }

  private int getCaptureHotkeyModifiers() {
    return 0;
  }

  private int getStopHotkey() {
    return 0;
  }

  private int getStopHotkeyModifiers() {
    return 0;
  }

  private static String getKeyModifierText(int modifiers) {
    String txtMod = KeyEvent.getKeyModifiersText(modifiers).toUpperCase();
    if (SX.isMac()) {
      txtMod = txtMod.replace("META", "CMD");
      txtMod = txtMod.replace("WINDOWS", "CMD");
    } else {
      txtMod = txtMod.replace("META", "WIN");
      txtMod = txtMod.replace("WINDOWS", "WIN");
    }
    return txtMod;
  }

  public String getHotKeyText(String hotkeyType) {
    String key = "";
    String mod = "";
    if (hotkeyType == HotkeyTypeCapture) {
      key = getKeyCodeText(getCaptureHotkey());
      mod = getKeyModifierText(getCaptureHotkeyModifiers());
    } else if (hotkeyType == HotkeyTypeAbort) {
      key = getKeyCodeText(getStopHotkey());
      mod = getKeyModifierText(getStopHotkeyModifiers());
    } else {
      log.error("HotkeyManager: getHotKeyText: HotkeyType %s not supported", hotkeyType);
    }
    return mod + " " + key;
  }

  /**
   * install a hotkey listener for a global hotkey (capture, abort, ...)
   * @param hotkeyType a type string
   * @param callback HotkeyListener
   * @return success
   */
  public boolean addHotkey(String hotkeyType, HotkeyListener callback) {
    if (hotkeyType == HotkeyTypeCapture) {
      HotkeyTypeCaptureKey = getCaptureHotkey();
      HotkeyTypeCaptureMod = getCaptureHotkeyModifiers();
      return installHotkey(HotkeyTypeCaptureKey, HotkeyTypeCaptureMod, callback, hotkeyType);
    } else if (hotkeyType == HotkeyTypeAbort) {
      HotkeyTypeAbortKey = getStopHotkey();
      HotkeyTypeAbortMod = getStopHotkeyModifiers();
      return installHotkey(HotkeyTypeAbortKey, HotkeyTypeAbortMod, callback, hotkeyType);
    } else {
      log.error("HotkeyManager: addHotkey: HotkeyType %s not supported", hotkeyType);
      return false;
    }
  }

  /**
   * install a hotkey listener.
   *
   * @param key key character (class Key)
   * @param modifiers modifiers flag
   * @param callback HotkeyListener
   * @return true if success. false otherwise.
   */
  public boolean addHotkey(char key, int modifiers, HotkeyListener callback) {
    return addHotkey("" + key, modifiers, callback);
  }

  /**
   * install a hotkey listener.
   *
   * @param key key character (class Key)
   * @param modifiers modifiers flag
   * @param callback HotkeyListener
   * @return true if success. false otherwise.
   */
  public boolean addHotkey(String key, int modifiers, HotkeyListener callback) {
    int[] keyCodes = toJavaKeyCode(key.toLowerCase());
    int keyCode = keyCodes[0];
    return installHotkey(keyCode, modifiers, callback, "");
  }

  private int[] toJavaKeyCode(String key) {
    return new int[]{0};
  }

  private boolean installHotkey(int key, int mod, HotkeyListener callback, String hotkeyType) {
    boolean res;
    String txtMod = getKeyModifierText(mod);
    String txtCode = getKeyCodeText(key);
    log.info("HotkeyManager: add %s Hotkey: %s %s (%d, %d)" , hotkeyType, txtMod, txtCode, key, mod);
    boolean checkGlobal = true;
    for (Integer k : hotkeys.keySet()) {
      if (k == key && mod == hotkeys.get(key)) {
        //res = _instance._removeHotkey(key, hotkeys.get(key));
        res = true;
        if (!res) {
          log.error("HotkeyManager: addHotkey: failed to remove already defined hotkey");
          return false;
        } else {
          checkGlobal = false;
        }
      }
    }
    if (checkGlobal) {
      for (Integer kg : hotkeysGlobal.keySet()) {
        if (kg == key && mod == hotkeysGlobal.get(key)) {
          log.error("HotkeyManager: addHotkey: ignored: trying to redefine a global hotkey");
          return false;
        }
      }
    }
    //res = _instance._addHotkey(key, mod, callback);
    res = true;
    if (res) {
      if (hotkeyType.isEmpty()) {
        hotkeys.put(key, mod);
      } else {
        hotkeysGlobal.put(key, mod);
      }
    } else {
      log.error("HotkeyManager: addHotkey: failed");
    }
    return res;
  }

	/**
	 * remove a hotkey by type (not supported yet)
	 * @param hotkeyType capture, abort, ...
	 * @return success
	 */
	public boolean removeHotkey(String hotkeyType) {
    if (hotkeyType == HotkeyTypeCapture) {
      return uninstallHotkey(HotkeyTypeCaptureKey, HotkeyTypeCaptureMod, hotkeyType);
    } else if (hotkeyType == HotkeyTypeAbort) {
      return uninstallHotkey(HotkeyTypeAbortKey, HotkeyTypeAbortMod, hotkeyType);
    } else {
      log.error("HotkeyManager: removeHotkey: using HotkeyType as %s not supported yet", hotkeyType);
      return false;
    }
  }

  /**
   * remove a hotkey and uninstall a hotkey listener.
   *
   * @param key key character (class Key)
   * @param modifiers modifiers flag
   * @return true if success. false otherwise.
   */
  public boolean removeHotkey(char key, int modifiers) {
    return removeHotkey("" + key, modifiers);
  }

  /**
   * uninstall a hotkey listener.
   *
   * @param key key string (class Key)
   * @param modifiers modifiers flag
   * @return true if success. false otherwise.
   */
  public boolean removeHotkey(String key, int modifiers) {
    int[] keyCodes = toJavaKeyCode(key.toLowerCase());
    int keyCode = keyCodes[0];
    return uninstallHotkey(keyCode, modifiers, "");
  }

  private boolean uninstallHotkey(int key, int mod, String hotkeyType) {
    boolean res;
    String txtMod = getKeyModifierText(mod);
    String txtCode = getKeyCodeText(key);
    log.info("HotkeyManager: remove %s Hotkey: %s %s (%d, %d)" , hotkeyType, txtMod, txtCode, key, mod);
    //res = _instance._removeHotkey(key, mod);
    res = true;
    if (res) {
      hotkeys.remove(key);
    } else {
      log.error("HotkeyManager: removeHotkey: failed");
    }
    return res;
  }

  abstract public void cleanUp();
}
