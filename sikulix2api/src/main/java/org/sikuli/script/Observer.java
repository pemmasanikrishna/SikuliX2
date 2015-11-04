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
  
  private List<ObserveEvent> events = Collections.synchronizedList(new ArrayList<ObserveEvent>());
  private List<ObserveEvent> eventsActive = Collections.synchronizedList(new ArrayList<ObserveEvent>());
  
  public Observer(Region reg) {
    region = reg;
    observers.add(this);
  }
  
  public boolean init() {
    return true;
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
    return true;
  }
  
  public void stop() {
    
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
