package com.sikulix.util.animation;

public class AnimatorLinear extends AnimatorTimeBased {

  public AnimatorLinear(float beginVal, float endVal, long totalMS) {
    super(new AnimatorLinearInterpolation(beginVal, endVal, totalMS));
  }
}
