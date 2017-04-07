/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.util.animation;

public class AnimatorLinearInterpolation extends AnimatorTimeValueFunction {

  float _stepUnit;

  public AnimatorLinearInterpolation(float beginVal, float endVal, long totalTime) {
    super(beginVal, endVal, totalTime);
    _stepUnit = (endVal - beginVal) / (float) totalTime;
  }

  @Override
  public float getValue(long t) {
    if (t > _totalTime) {
      return _endVal;
    }
    return _beginVal + _stepUnit * t;
  }
}

