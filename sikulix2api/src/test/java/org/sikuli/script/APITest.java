package org.sikuli.script;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.BeforeClass;

public class APITest {

  public static Screen s = null;

  @Test
  public void startRunTime() {
		System.setProperty("sikuli.Debug", "3");
		RunTime.get();
		Assert.assertTrue(true);
  }
}
