package org.sikuli.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Observer {

  private Observer() {}
  
  private static List<Observer> observers = Collections.synchronizedList(new ArrayList<Observer>());
  
  private Region region = null;
  private Finder finder = new Finder();
  private Finder.Found found = new Finder.Found(finder);
  private boolean stopped = true;
  
  private List<ObserveEvent> events = Collections.synchronizedList(new ArrayList<ObserveEvent>());
  private List<ObserveEvent> eventsActive = Collections.synchronizedList(new ArrayList<ObserveEvent>());
  
  public Observer(Region reg) {
    region = reg;
    observers.add(this);
  }
  
  public boolean init() {
    return true;
  }

  protected void stop() {
    stopped = true;
  }
  
  protected boolean isStopped() {
    return stopped;
  }

  protected static void cleanUp() {
    for (Observer observer : observers) {
      if (!observer.isStopped()) {
        observer.region.stopObserver();
      }
    }
    boolean allStopped = false;
    while (!allStopped) {
      allStopped = true;
      for (Observer observer : observers) {
        allStopped &= observer.isStopped();
      }
    }
  }
  
  public boolean hasObservers() {
    return events.size() > 0;
  }

  public boolean isObserving() {
    for (ObserveEvent evt : events) {
      if (evt.isActive()) {
        return true;
      }
    }
    return false;
  }
  
  public boolean run() {
    eventsActive.clear();
    stopped = false;
    for (int i = 0; i < events.size(); i++) {
      ObserveEvent evt = events.get(i);
      if (evt.isActiveFind()) {
        evt.reset();
        eventsActive.add(evt);
      }
    }
    finder.setBase(region);
    found.events = eventsActive.toArray(new ObserveEvent[0]);
    finder.findAny(found);
    for (ObserveEvent evt : eventsActive) {
      if (evt.hasHappened() && evt.hasCallback()) {
        evt.getCallback().happened(evt);
      }
    }
    if (stopped) {
      return false;
    }
    stopped = true;
    return true;
  }
  
  public boolean hasEvents() {
    for (ObserveEvent evt : events) {
      if (evt.hasHappened()) {
        return true;
      }
    }
    return false;
  }

  public ObserveEvent[] getEvents() {
    List<ObserveEvent> evnts = new ArrayList<ObserveEvent>(0);
    for (int i = 0; i < events.size(); i++) {
      ObserveEvent evt = events.get(i);
      if (evt.hasHappened()) {
       evnts.add(evt);
      }
    }
    return evnts.toArray(new ObserveEvent[0]);
  }
}
