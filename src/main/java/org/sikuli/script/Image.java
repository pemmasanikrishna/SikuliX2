package org.sikuli.script;

import com.sikulix.api.Picture;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

public class Image {
  private static SXLog log = SX.getLogger("SX.IMAGE");

  Picture image = null;

  public String toString() {
    return String.format("Image(%s)", name);
  }

  public Image(String name) {
    image = new Picture(name);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  private String name = "";
}
