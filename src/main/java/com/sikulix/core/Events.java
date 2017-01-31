/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.sikulix.api.Element;
import com.sikulix.api.Event;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Events {
  private static SXLog log = SX.getLogger("SX.Events");

  private static Events instance = null;
  private ConcurrentHashMap<Element, ConcurrentHashMap<Long, Event>> elements = new ConcurrentHashMap<>();

  private boolean running = false;

  EventLoop eventLoop = null;

  private Events() {
  }

  private static Events get() {
    if (SX.isNull(instance)) {
      instance = new Events();
    }
    return instance;
  }

  public static void startObserving() {
    if (!get().running) {
      if (SX.isNull(get().eventLoop)) {
        get().eventLoop = new EventLoop();
        new Thread(get().eventLoop).start();
      }
    }
    get().running = true;
    log.trace("startObserving");
  }

  public static void pause() {
    if (isRunning()) {
      get().running = false;
    }
  }

  public static void stopObserving() {
    log.trace("stop requested");
    if (isRunning()) {
      get().running = false;
    }
    waitForEventLoopToFinish();
  }

  private static boolean isRunning() {
    return get().running;
  }

  private static ConcurrentHashMap<Long, Event> put(Element where) {
    get().elements.put(where, new ConcurrentHashMap<Long, Event>());
    return get().elements.get(where);
  }

  public static void add(Element where, Collection<Event> evts) {
    ConcurrentHashMap<Long, Event> events = put(where);
    for (Event evt : evts) {
      Long key = evt.setKey(evt.getWhen());
      events.put(key, evt);
    }
  }

  public static void remove(Element elem) {
    get().elements.remove(elem);
  }

  private static ConcurrentHashMap<Long, Event> getEventList(Element where) {
    ConcurrentHashMap<Long, Event> events = get().elements.get(where);
    return events;
  }

  public static boolean hasEvents(Element where) {
    return SX.isNotNull(get().elements.get(where));
  }

  public static int getEventCount(Element where) {
    if (SX.isNotNull(getEventList(where))) {
      return getEventList(where).size();
    }
    return 0;
  }

  private static List<Event> getEvents(Element where) {
    List<Event> events = new ArrayList<>();
    events.addAll(getEventList(where).values());
    Collections.sort(events);
    return events;
  }

  public static boolean hasEvent(Element what, Element where) {
    if (SX.isNotNull(getEventList(where))) {
      for (Event evt : getEventList(where).values()) {
        if (evt.isFor(what)) {
          return true;
        }
      }
    }
    return false;
  }

  public static Event getEvent(Element what, Element where) {
    Event evt = null;
    if (SX.isNotNull(getEventList(where))) {
      for (Event e : getEventList(where).values()) {
        if (e.isFor(what)) {
          evt = e;
          break;
        }
      }
    }
    return evt;
  }

  public static boolean hasHappened(Element where) {
    return countHappened(where) > 0;
  }

  private static int countHappened(Element where) {
    int count = 0;
    if (hasEvents(where)) {
      for (Event e : getEvents(where)) {
        if (e.getWhen() > where.getObserveStart()) {
          count++;
        }
      }
    }
    return count;
  }

  public static Event nextHappened(Element where) {
    Event evt = null;
    if (SX.isNotNull(getEventList(where))) {
      if (getEventList(where).size() > 0) {
        evt = getEvents(where).get(0);
        getEventList(where).remove(evt.getKey());
      }
    }
    return evt;
  }

  private static void waitForEventLoopToFinish() {
    log.trace("waitForEventLoopToFinish: start");
    while (get().eventLoop.isLooping()) {
      SX.pause(0.5);
    }
    log.trace("waitForEventLoopToFinish: end");
  }

  private static class EventLoop implements Runnable {

    boolean running = true;

    public boolean isLooping() {
      return running;
    }

    @Override
    public void run() {
      log.p("EventLoop: started for %d elements", get().elements.size());
      while (true) {
        for (Element where : get().elements.keySet().toArray(new Element[0])) {
          if (where.isObserving()) {
            log.p("EventLoop: observing starting for %s with %d events",
                    where, getEventList(where).size());
            for (Event evt : getEvents(where)) {
              evt.setWhen(new Date().getTime());
              log.p("observing: %s", evt);
            }
          }
        }
        SX.pause(1);
        if (!isRunning()) {
          break;
        }
      }
      running = false;
      log.trace("EventLoop: stopped");
    }
  }
}
