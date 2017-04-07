/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.sikulix.api.Mouse;

public class Device {

  protected static SXLog log = SX.getLogger("SX.Device");

  protected Device device = null;
  protected String deviceName = "Device";

  protected boolean isMouse = false;
  protected boolean isKeys = false;
  protected boolean isLocalScreen = false;
  protected boolean isOtherScreen = false;


  private boolean inUse = false;
  private boolean keep = false;
  protected Object owner = null;
  private boolean blocked = false;
  private boolean suspended = false;

  //<editor-fold desc="*** Construction">
  public Device() {}
  //</editor-fold>

  //<editor-fold desc="*** Callback">
  private ObserverCallBack callback = null;
  private boolean shouldRunCallback = false;
  private boolean shouldTerminate = false;

  public void setShouldTerminate() {
    shouldTerminate = true;
    log.debug("setShouldTerminate: request issued");
  }

  public boolean isShouldRunCallback() {
    return shouldRunCallback;
  }

  public void setShouldRunCallback(boolean shouldRunCallback) {
    this.shouldRunCallback = shouldRunCallback;
  }

  private void checkShouldRunCallback() {
    if (shouldRunCallback && callback != null) {
      callback.happened(new ObserveEvent(ObserveEvent.Type.GENERIC));
      if (shouldTerminate) {
        shouldTerminate = false;
        throw new AssertionError("aborted by Sikulix.GenericDeviceCallBack");
      }
    }
  }

  /**
   * set a callback function for handling Device events <br>
   * in case of event the user provided callBack.happened is called
   *
   * @param givenCallBack
   */
  public void setCallback(Object givenCallBack) {
    if (givenCallBack != null) {
      callback = new ObserverCallBack(givenCallBack, ObserveEvent.Type.GENERIC);
    } else {
      callback = null;
    }
  }
  //</editor-fold>

  //<editor-fold desc="*** get state">
  protected boolean isInUse() {
    return inUse;
  }
  protected boolean isSuspended() {
    return suspended;
  }
  protected boolean isBlocked() {
    return blocked;
  }
	private boolean isNotLocal(Object owner) {
		if (owner instanceof SXElement) {
      return ((SXElement) owner).isSpecial();
    }
		return false;
	}
  //</editor-fold>

  //<editor-fold desc="*** block globally">
  /**
   * to block the device globally <br>
   * only the contained device methods without owner will be granted
   *
   * @return success
   */
  public boolean block() {
    return block(null);
  }

  /**
   * to block the device globally for the given owner <br>
   * only the contained mouse methods having the same owner will be granted
   *
   * @param owner Object
   * @return success
   */
  public boolean block(Object owner) {
    if (use(owner)) {
      blocked = true;
      return true;
    } else {
      return false;
    }
  }

  /**
   * free the mouse globally after a block()
   *
   * @return success (false means: not blocked currently)
   */
  public boolean unblock() {
    return unblock(null);
  }

  /**
   * free the mouse globally for this owner after a block(owner)
   *
   * @param ownerGiven Object
   * @return success (false means: not blocked currently for this owner)
   */
	public boolean unblock(Object ownerGiven) {
		if (ownerGiven == null) {
			ownerGiven = device;
		} else if (isNotLocal(ownerGiven)) {
			return false;
		}
		if (blocked && owner == ownerGiven) {
			blocked = false;
			let(ownerGiven);
			return true;
		}
		return false;
	}
  //</editor-fold>

  //<editor-fold desc="*** coordinate usage">
  public boolean use() {
    return use(null);
  }

  public synchronized boolean use(Object owner) {
    if (owner == null) {
      owner = device;
		} else if (isNotLocal(owner)) {
			return false;
		}
    if ((blocked || inUse) && this.owner == owner) {
      return true;
    }
    while (inUse) {
      try {
        wait();
      } catch (InterruptedException e) {
      }
    }
    if (!inUse) {
      inUse = true;
      if (isMouse) {
        Mouse.get().checkLastPos();
        checkShouldRunCallback();
        if (shouldTerminate) {
          shouldTerminate = false;
          throw new AssertionError(String.format("Device: %s: termination after return from callback", deviceName));
        }
      }
      keep = false;
      this.owner = owner;
      log.trace("%s: use start: %s", deviceName, owner);
      return true;
    }
    log.error("use synch problem at start: %s", owner);
    return false;
  }

  public synchronized boolean keep(Object ownerGiven) {
    if (ownerGiven == null) {
      ownerGiven = this;
		} else if (isNotLocal(ownerGiven)) {
			return false;
		}
    if (inUse && owner == ownerGiven) {
      keep = true;
      log.trace("%s: use keep: %s", deviceName, ownerGiven);
      return true;
    }
    return false;
  }

  public boolean let() {
    return let(null);
  }

  public synchronized boolean let(Object owner) {
    if (owner == null) {
      owner = this;
		} else if (isNotLocal(owner)) {
			return false;
		}
    if (inUse && this.owner == owner) {
      if (keep) {
        keep = false;
        return true;
      }
      if (isMouse) {
        Mouse.get().setLastPos();
      }
      inUse = false;
      this.owner = null;
      notify();
      log.trace("%s: use stop: %s", deviceName, owner);
      return true;
    }
    return false;
  }
  //</editor-fold>

  public static void delay(int time) {
    if (time == 0) {
      return;
    }
    if (time < 60) {
      time = time * 1000;
    }
    try {
      Thread.sleep(time);
    } catch (InterruptedException e) {
    }
  }
}
