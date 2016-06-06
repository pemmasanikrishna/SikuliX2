/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */
package com.sikulix.core;


import com.sikulix.api.Image;

import java.awt.Rectangle;
import java.awt.Color;

/**
 * INTERNAL USE <br>
 * function template for (alternative) Robot implementations
 */
public interface IRobot {
   enum KeyMode {
      PRESS_ONLY, RELEASE_ONLY, PRESS_RELEASE
   };
   void keyDown(String keys);
   void keyUp(String keys);
   void keyDown(int code);
   void keyUp(int code);
   void keyUp();
   void pressModifiers(int modifiers);
   void releaseModifiers(int modifiers);
   void typeChar(char character, KeyMode mode);
   void typeKey(int key);
   void mouseMove(int x, int y);
   void mouseDown(int buttons);
   int mouseUp(int buttons);
   void mouseWheel(int wheelAmt);
   Image captureScreen(Rectangle rect);
   void waitForIdle();
   void delay(int ms);
   void setAutoDelay(int ms);
   Color getColorAt(int x, int y);
   void cleanup();
   boolean isRemote();
}

