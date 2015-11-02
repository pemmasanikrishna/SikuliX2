package org.sikuli.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Observer {
  
  private Observer() {}
  
  private static List<Observer> observers = Collections.synchronizedList(new ArrayList<Observer>());
  
  private Region region = null;
  
  private List<ObserveEvent> events = Collections.synchronizedList(new ArrayList<ObserveEvent>());
  private List<ObserveEvent> eventsHappend = Collections.synchronizedList(new ArrayList<ObserveEvent>());
  private List<ObserveEvent> eventsActive = Collections.synchronizedList(new ArrayList<ObserveEvent>());
  
  public Observer(Region reg) {
    region = reg;
    observers.add(this);
  }

  public boolean hasObservers() {
    return events.size() > 0;
  }

  public boolean isObserving() {
    return eventsActive.size() > 0;
  }

  public boolean hasEvents() {
    return eventsHappend.size() > 0;
  }

  public ObserveEvent getEvent() {
    if (hasEvents()) {
      return events.remove(0);
    }
    return null;
  }

  public ObserveEvent[] getEvents() {
    ObserveEvent[] evnts = new ObserveEvent[0];
    if (hasEvents()) {
       evnts = eventsHappend.toArray(new ObserveEvent[0]);
       eventsHappend.clear();
    }
    return evnts;
  }
  
  public void pause(ObserveEvent event) {
    eventsActive.remove(event);
    event.setActive(false);
  }  

  public boolean restart(ObserveEvent event) {
    eventsActive.remove(event);
    if (eventsActive.add(event)) {
      event.setActive(true);
      return true;
    }
    return false;
  }
}
