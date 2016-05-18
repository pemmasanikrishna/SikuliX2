/*
 * Copyright 2010-2014, Sikuli.org, sikulix.com
 * Released under the MIT License.
 *
 * modified RaiMan
 */
package org.sikuli.script;

import com.sikulix.core.SX;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * to define a more complex search target<br>
 * - non-standard minimum similarity <br>
 * - click target other than center <br>
 * - image as in-memory image
 */
public class Pattern {
  
  static RunTime runTime = RunTime.getRunTime();
  
  private static double exactAs = 0.99f;
  
  public static void setExactAs(double sim) {
    exactAs = sim;
  }

  private Image image = null;
  private double similarity = SX.MinSimilarity;
  private Location offset = new Location(0, 0);
  private int waitAfter = 0;
  private Type type = Type.IMAGE;
  private String text = ""; 
  
  /**
   * defines the Pattern type<br>
   * IMAGE the default 
   * TEXT a text should be searched using OCR
   */
  public enum Type {
    TEXT, IMAGE
  }

  /**
   * creates empty Pattern object at least setFilename() or setBImage() must be used before the
   * Pattern object is ready for anything
   */
  public Pattern() {
  }

  /**
   * create a new Pattern from another (attribs are copied)
   *
   * @param p other Pattern
   */
  public Pattern(Pattern p) {
    image = p.getImage();
    similarity = p.similarity;
    offset.x = p.offset.x;
    offset.y = p.offset.y;
  }

  /**
   * create a Pattern with given image<br>
   *
   * @param img Image
   */
  public Pattern(Image img) {
		image = img;
  }

	/**
   * create a Pattern based on an image file name<br>
   *
   * @param fpImg image filename
   */
  public Pattern(String fpImg) {
    image = Image.get(fpImg);
  }

	/**
   * create a Pattern as given type<br>
   *
   * @param str meaning depends on type
   */
  public Pattern(String str, Type ptype) {
    if (ptype == Type.TEXT) {
      text = str;
      type = ptype;
    } else {
      image = Image.get(str);
    };
  }

  /**
   * Pattern from a Java resource (Object.class.getResource)
   *
	 * @param url image file URL
   */
  public Pattern(URL url) {
    image = Image.get(url);
  }

  /**
   * A Pattern from a BufferedImage
   *
   * @param bimg BufferedImage
   */
  public Pattern(BufferedImage bimg) {
    image = new Image(bimg);
  }

  /**
   * A Pattern from a ScreenImage
   *
   * @param simg ScreenImage
   */
  public Pattern(ScreenImage simg) {
    image = new Image(simg);
  }

  /**
   * check wether the image is valid
   *
   * @return true if image is useable
   */
  public boolean isValid() {
    return image.isUseable();
  }

  /**
   * set a new image for this pattern
   *
   * @param fileName image filename
   * @return the Pattern itself
   */
  public Pattern setFilename(String fileName) {
    image = Image.get(fileName);
    return this;
  }

  /**
   * set a new image for this pattern
   *
   * @param fileURL image file URL
   * @return the Pattern itself
   */
  public Pattern setFilename(URL fileURL) {
    image = Image.get(fileURL);
    return this;
  }

  /**
   * set a new image for this pattern
   *
   * @param img Image
   * @return the Pattern itself
   */
  public Pattern setFilename(Image img) {
    image = img;
    return this;
  }

  /**
   * the current image's absolute filepath
   * <br>will return null, if image is in jar or in web
   * <br>use getFileURL in this case
   *
   * @return might be null
   */
  public String getFilename() {
    return image.getFilename();
  }

  /**
   * the current image's URL
   *
   * @return might be null
   */
  public URL getFileURL() {
    return image.getURL();
  }

  /**
   * sets the minimum Similarity to use with findX
   *
   * @param sim value 0 to 1
   * @return the Pattern object itself
   */
  public Pattern similar(double sim) {
    similarity = sim;
    return this;
  }

  /**
   * sets the minimum Similarity to 0.99 which means exact match
   *
   * @return the Pattern object itself
   */
  public Pattern exact() {
    similarity = exactAs;
    return this;
  }

  /**
   *
   * @return the current minimum similarity
   */
  public double getSimilar() {
    return this.similarity;
  }

  /**
   * set the offset from the match's center to be used with mouse actions
   *
   * @param dx x offset
   * @param dy y offset
   * @return the Pattern object itself
   */
  public Pattern targetOffset(int dx, int dy) {
    offset.x = dx;
    offset.y = dy;
    return this;
  }

  /**
   * set the offset from the match's center to be used with mouse actions
   *
   * @param loc Location
   * @return the Pattern object itself
   */
  public Pattern targetOffset(Location loc) {
    offset.x = loc.x;
    offset.y = loc.y;
    return this;
  }

  /**
   *
   * @return the current offset
   */
  public Location getTargetOffset() {
    return offset;
  }

  /**
   * ONLY FOR INTERNAL USE! Might vanish without notice!
   *
   * @return might be null
   */
  public BufferedImage getBImage() {
    return image.get();
  }

  /**
   * ONLY FOR INTERNAL USE! Might vanish without notice!
   *
   * @param bimg BufferedImage
   * @return the Pattern object itself
   */
  public Pattern setBImage(BufferedImage bimg) {
    image = new Image(bimg);
    return this;
  }

  /**
   * sets the Pattern's image
   *
   * @param img Image
   * @return the Pattern object itself
   */
  public Pattern setImage(Image img) {
    image = img;
    return this;
  }

  /**
   * get the Pattern's image
   *
	 * @return Image
   */
  public Image getImage() {
    return image;
  }

  /**
   * set the seconds to wait, after this pattern is acted on
   *
   * @param secs seconds
   */
  public void setTimeAfter(int secs) {
    waitAfter = secs;
  }

  /**
	 * <br>TODO: Usage to be implemented!
   * get the seconds to wait, after this pattern is acted on
	 * @return time in seconds
   */
  public int getTimeAfter() {
    return waitAfter;
  }
  
  public String getText() {
    if (Type.TEXT.equals(type)) {
      return "T[" + text.trim() + "]";
    }
    return toString();
  }

  @Override
  public String toString() {
    String off = "";
    if (offset.x != 0 || offset.y != 0) {
      off = " (" + offset.x + "," + offset.y + ")";
    }
    String size = "";
    if (image != null) {
      size = String.format("(%dx%d)", image.getWidth(), image.getHeight());
    }
    String ret = String.format("P[%s%s%s%%%.2f%s]",
            image.getImageName(), (isValid() ? "" : "???"), size,
            similarity * 100, off);
    return ret;
  }

  public String toJSON(boolean withLastSeen) {
    String off = "";
    if (offset.x != 0 || offset.y != 0) {
      off = " (" + offset.x + "," + offset.y + ")";
    }
    String ret = String.format("[\"P\", %s, %d%s]",
            (isValid() ? image.toJSON(withLastSeen) : "null"), (int) (similarity * 10000), off);
    return ret;
  }
  
    public String toJSON() {
      return toJSON(true);
    }

}
