/*
 * Copyright 2010-2014, Sikuli.org, sikulix.com
 * Released under the MIT License.
 *
 * modified RaiMan
 */
package org.sikuli.script;

import com.sikulix.core.ExFindFailed;

/**
 * implements the SikuliX FindFailed exception class
 * and defines constants and settings for the feature FindFailedResponse
 */
public class FindFailed extends ExFindFailed {

  public FindFailed(String message) {
    super(message);
  }
}
