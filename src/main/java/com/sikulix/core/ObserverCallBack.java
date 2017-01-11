/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

//import com.sikulix.scripting.JythonHelper;

import java.lang.reflect.Method;
import java.util.EventListener;

/**
 * Use this class to implement call back methods for the Region observers
 * onAppear, onVanish and onChange. <br>
 * by overriding the contained empty method happened(ObserveEvent e)<br>
 * (deprecated: appeared, vanished and changed)
 * <pre>
 * example:
 * aRegion.onAppear(anImage,
 *   new ObserverCallBack() {
 *     happened(ObserveEvent e) {
 *       // do something
 *     }
 *   }
 * );
 * </pre>
 when the image appears, your above call back appeared() will be called
 see {@link ObserveEvent} about the features available in the callback function
 */
public class ObserverCallBack implements EventListener {

	private static SXLog log = SX.getLogger("SX.ObserverCallBack");

  Object callback = null;
  ObserveEvent.Type obsType = ObserveEvent.Type.GENERIC;
  Object scriptRunner = null;
  String scriptRunnerType = null;
  Method doSomethingSpecial = null;

	public ObserverCallBack(Object callback, ObserveEvent.Type obsType) {
//TODO implement ScriptingHelper
		this.callback = callback;
		this.obsType = obsType;
		if (callback.getClass().getName().contains("org.python")) {
			scriptRunnerType = "jython";
			scriptRunner = null; //JythonHelper.get();
		} else {
			try {
				if (callback.getClass().getName().contains("org.jruby")) {
					scriptRunnerType = "jruby";
				}
				if (scriptRunnerType != null) {
					Class Scripting = Class.forName("org.sikuli.scriptrunner.ScriptingSupport");
					Method getRunner = Scripting.getMethod("getRunner",
									new Class[]{String.class, String.class});
					scriptRunner = getRunner.invoke(Scripting, new Object[]{null, scriptRunnerType});
					if (scriptRunner != null) {
						doSomethingSpecial = scriptRunner.getClass().getMethod("doSomethingSpecial",
										new Class[]{String.class, Object[].class});
					}
				} else {
					log.error("ObserverCallBack: no valid callback: %s", callback);
				}
			} catch (Exception ex) {
				log.error("ObserverCallBack: %s init: ScriptRunner not available for %s", obsType, scriptRunnerType);
				scriptRunner = null;
			}
		}
	}

  public void appeared(ObserveEvent e) {
    if (scriptRunner != null && ObserveEvent.Type.APPEAR.equals(obsType)) {
      run(e);
    }
  }

  public void vanished(ObserveEvent e) {
    if (scriptRunner != null && ObserveEvent.Type.VANISH.equals(obsType)) {
      run(e);
    }
  }

  public void changed(ObserveEvent e) {
    if (scriptRunner != null && ObserveEvent.Type.CHANGE.equals(obsType)) {
      run(e);
    }
  }

  public void happened(ObserveEvent e) {
    if (scriptRunner != null && ObserveEvent.Type.GENERIC.equals(obsType)) {
      run(e);
    }
  }

  private void run(ObserveEvent e) {
    boolean success = true;
		Object[] args = new Object[] {callback, e};
		if (scriptRunnerType == "jython") {
//			success = ((JythonHelper) scriptRunner).runObserveCallback(args);
		} else {
			String msg = "IScriptRunner: doSomethingSpecial returned false";
			try {
				if (scriptRunner != null) {
					success = (Boolean) doSomethingSpecial.invoke(scriptRunner, new Object[]{"runObserveCallback", args});
				}
			} catch (Exception ex) {
				success = false;
				msg = ex.getMessage();
			}
			if (!success) {
				log.error("ObserverCallBack: problem with scripting handler: %s\n%s\n%s", scriptRunner, callback, msg);
			}
		}
  }

  public ObserverCallBack() {
  }
}
