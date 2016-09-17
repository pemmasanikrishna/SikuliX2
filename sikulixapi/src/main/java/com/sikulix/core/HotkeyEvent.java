/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;


public class HotkeyEvent {
   public int keyCode;
   public int modifiers;

   public HotkeyEvent(int code_, int mod_){
      init(code_, mod_);
   }

   void init(int code_, int mod_){
      keyCode = code_;
      modifiers = mod_;
   }
}

