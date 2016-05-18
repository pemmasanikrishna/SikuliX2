/*
 * Copyright 2010-2014, Sikuli.org, sikulix.com
 * Released under the MIT License.
 *
 * modified RaiMan
 */
package org.sikuli.script;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sikulix.core.SX;
import org.apache.logging.log4j.LogManager;
import org.sikuli.util.Debug;

/**
 * provides information about the observed event being in the {@link ObserverCallBack}
 */
public class ObserveEvent {

  //<editor-fold defaultstate="collapsed" desc="logging">
  private static final int lvl = 3;
  private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger("SX.ObserveEvent");

  private static void log(int level, String message, Object... args) {
    if (Debug.is(lvl) || level < 0) {
      message = String.format(message, args).replaceFirst("\\n", "\n          ");
      if (level == lvl) {
        logger.debug(message, args);
      } else if (level > lvl) {
        logger.trace(message, args);
      } else if (level == -1) {
        logger.error(message, args);
      } else {
        logger.info(message, args);
      }
    }
  }

  private static void logp(String message, Object... args) {
    System.out.println(String.format(message, args));
  }

  public static void terminate(int retval, String message, Object... args) {
    logger.fatal(String.format(" *** terminating: " + message, args));
    System.exit(retval);
  }
  
  private long started = 0;
  
  private void start() {
    started = new Date().getTime();
  }

  private long end() {
    return end("");
  }

  private long end(String message) {
    long ended = new Date().getTime();
    long diff = ended - started;
    if (!message.isEmpty()) {
      logp("[time] %s: %d msec", message, diff);
    }
    started = ended;
    return diff;
  }
//</editor-fold>

  public enum Type {
    APPEAR, VANISH, CHANGE, GENERIC
  }

  /**
   * the event's type as ObserveEvent.APPEAR, .VANISH, .CHANGE
   */
  private Type type;
  private long time;
  private Region region = null;
  private Pattern pattern = null;
  private ObserverCallBack callback = null;
  private Object[] vals = null;
  private int minChanged = SX.ObserveMinChangedPixels;
  
  private long repeatAfter = 0;
  private int count = 0;
  private boolean active = true;
 
  private List<Match> matchOrChanges = new ArrayList<Match>();

  private ObserveEvent() {
  }

  public ObserveEvent(Region reg, Type type, Object targetOrMinChanges, ObserverCallBack callback) {
    region = reg;
    this.type = type;
    time = new Date().getTime();
    init(type, callback, targetOrMinChanges);
  }
  
  public ObserveEvent(Type type, ObserverCallBack callback, Object... args) {
    this.type = type;
    this.callback = callback;
    init(args);
  }

  public ObserveEvent(Type type, Object... args) {
    this.type = type;
    init(args);
  }
  
  private void init(Object... args) {
    if (type == Type.GENERIC) {
      vals = args;
    } else if (type == Type.CHANGE) {
      if (args.length > 0) {
        minChanged = (int) args[0];
      }
    } else {
      if (args.length > 0) {
        try {
          pattern = Finder.evalTarget(args[0]);
        } catch (IOException ex) {
          log(-1, "init: file not found: %s", args[0]);
        }
      }
    }
  }
  
  /**
   * get the observe event type
   * @return a string containing either APPEAR, VANISH, CHANGE or GENERIC
   */
  public String getType() {
    return type.toString();
  }
  
  /**
   *
   * @return wether this event is currently ready to be observed
   */
  public boolean isActive() {
    return active;
  }
  
  /**
   *
   * @param state the intended active state
   */
  public void setActive(boolean state) {
    active = state;
  }

  protected boolean isActiveFind() {
    return isActive() && (isAppear() || isVanish());
  }

  /**
   * check the observe event type
   * @return true if it is APPEAR, false otherwise
   */
  public boolean isAppear() {
    return type == Type.APPEAR;
  }

 /**
   * check the observe event type
   * @return true if it is VANISH, false otherwise
   */
   public boolean isVanish() {
    return type == Type.VANISH;
  }

 /**
   * check the observe event type
   * @return true if it is CHANGE, false otherwise
   */
   public boolean isChange() {
    return type == Type.CHANGE;
  }

 /**
   * check the observe event type
   * @return true if it is GENERIC, false otherwise
   */
   public boolean isGeneric() {
    return type == Type.GENERIC;
  }
   
  public boolean hasCallback() {
    return callback != null;
  }
  
  public ObserverCallBack getCallback() {
    return callback;
  } 

  /**
   *
   * @return this event's observer's region
   */
  public Region getRegion() {
    return region;
  }

  public boolean hasHappened() {
    if (matchOrChanges.size() > 0) { 
      if (isChange()) {
        return matchOrChanges.get(0) == null;
      }
      return true;
    };
    return false;
  }

  /**
   *
   * @return the observed match (APEAR, VANISH)
   */
  public Match getMatch() {
    if ((isAppear() || isVanish()) && hasHappened()) {
      return matchOrChanges.get(0);
    }
    return null;
  }

  protected void setMatch(Match m) {
    matchOrChanges.clear();
    matchOrChanges.add(0, m);
  }
  
  protected void setHappened() {
    matchOrChanges.clear();
    matchOrChanges.add(0, new Match());    
  }

  public void reset() {
    matchOrChanges.clear();
  }
    
  /**
   *
   * @return a list of observed changes as matches (CHANGE)
   */
  public List<Match> getChanges() {
    if (isChange() && hasHappened()) {
      return matchOrChanges;
    }
    return null;
  }

  protected void setChanges(List<Match> c) {
    if (c != null) {
      matchOrChanges.clear();
      matchOrChanges.addAll(c);
    }
  }

  /**
   *
   * @return the used pattern for this event's observing
   */
  public Pattern getPattern() {
    return pattern;
  }

  public long getTime() {
    return time;
  }

  // get and reset
  protected long repeat() {
    long ra = repeatAfter;
    repeatAfter = 0;
    return ra;
  }

  /**
   * tell the observer to repeat this event's observe action after given time in secs
   * after returning from this handler (APPEAR, VANISH)
   * @param secs seconds
   */
  public void repeat(long secs) {
    repeatAfter = secs;
  }

  /**
   * @return the number how often this event has already been triggered until now
   */
  public int getCount() {
    return count;
  }

  protected int setCount() {
    return count++;
  }

  /**
   * stops the observer
   */
  public void stopObserver() {
    region.stopObserver();
  }

  /**
   * stops the observer and prints the given text
   * @param text text
   */
  public void stopObserver(String text) {
    region.stopObserver(text);
  }

  @Override
  public String toString() {
    String sCB = "";
    if (null != callback) {
      sCB = "(withCallback)";
    }
    if (type == Type.CHANGE) {
      return String.format("Event(%s) on: %s min: %s (#%d) count: %d %s", 
          type, region, minChanged, matchOrChanges.size(), getCount());
    } else if (type == Type.GENERIC) {
      return String.format("Event(%s) happened: %s %s", type, (hasHappened() ? "yes" : "no"), sCB);
    } else {
      return String.format("Event(%s) on: %s with: %s\nmatch: %s count: %d %s", 
          type, region, pattern, (matchOrChanges.size() > 0 ? matchOrChanges.get(0) : "none"), getCount());
    }
  }
}
