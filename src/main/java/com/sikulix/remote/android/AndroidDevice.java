/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.remote.android;

import com.sikulix.api.Element;
import com.sikulix.api.Picture;
import com.sikulix.core.IDevice;

import java.awt.Rectangle;

public class AndroidDevice implements IDevice{
  @Override
  public IDevice start(Object... args) {
    return null;
  }

  @Override
  public void stop() {

  }

  @Override
  public boolean isValid() {
    return false;
  }

  @Override
  public int getNumberOfMonitors() {
    return 0;
  }

  @Override
  public Rectangle getMonitor(int... id) {
    return null;
  }

  @Override
  public Rectangle getAllMonitors() {
    return null;
  }

  @Override
  public int getMonitorID() {
    return 0;
  }

  @Override
  public int getMonitorID(int id) {
    return 0;
  }

  @Override
  public void resetMonitors() {

  }

  @Override
  public Rectangle[] getMonitors() {
    return new Rectangle[0];
  }

  @Override
  public int getContainingMonitorID(Element element) {
    return 0;
  }

  @Override
  public Element getContainingMonitor(Element element) {
    return null;
  }

  @Override
  public Element click(Element loc) {
    return null;
  }

  @Override
  public Element doubleClick(Element loc) {
    return null;
  }

  @Override
  public Element rightClick(Element loc) {
    return null;
  }

  @Override
  public Element click(Action action) {
    return null;
  }

  @Override
  public Element click(Element loc, Action action) {
    return null;
  }

  @Override
  public Element dragDrop(Element from, Element to, Object... times) {
    return null;
  }

  @Override
  public void keyStart() {

  }

  @Override
  public void keyStop() {

  }

  @Override
  public void key(Action action, Object key) {

  }

  /**
   * move the mouse from the current position to the offset given by the parameters
   *
   * @param xoff horizontal offset (&lt; 0 left, &gt; 0 right)
   * @param yoff vertical offset (&lt; 0 up, &gt; 0 down)
   * @return the new mouseposition as Element (might be invalid)
   */
  @Override
  public Element move(int xoff, int yoff) {
    return null;
  }

  /**
   * move the mouse to the target of given Element (default center)
   *
   * @param loc
   * @return the new mouseposition as Element (might be invalid)
   */
  @Override
  public Element move(Element loc) {
    return null;
  }

  /**
   * @return the current mouseposition as Element (might be invalid)
   */
  @Override
  public Element at() {
    return null;
  }

  @Override
  public void button(Action action) {

  }

  @Override
  public void wheel(Action action, int steps) {

  }

  @Override
  public Picture capture(Object... args) {
    return null;
  }
}
