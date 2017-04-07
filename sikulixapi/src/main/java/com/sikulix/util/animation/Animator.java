/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */
package com.sikulix.util.animation;

/**
 * INTERNAL USE
 * allows to implement timed animations (e.g. mouse move)
 */
public interface Animator {

  public float step();

  public boolean running();
}

