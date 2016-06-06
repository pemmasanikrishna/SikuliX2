/*
 * Copyright 2010-2014, Sikuli.org, sikulix.com
 * Released under the MIT License.
 *
 * modified RaiMan
 */
package org.sikuli.script;

import java.awt.Rectangle;

import com.sikulix.util.Settings;

/**
 * holds the result of a find operation, is itself the region on the screen,
 * where the image was found and hence inherits all methods from {@link Region}.
 * <br>
 * attributes:<br> the match score (0 ... 1.0)<br> the click target (e.g.
 * from {@link Pattern})<br> a ref to the image used for search<br>or the text used for
 * find text<br>and elapsed times for debugging
 */
public class Match extends Region implements Comparable<Match> {

  private double simScore = 0.0;
  private Location target = null;
  private Image image = null;
  private String ocrText = null;
  private long lastSearchTime = -1;
  private long lastFindTime = -1;
  private int index = -1;
  private boolean onScreen = true;
  
  public void setOnScreen(boolean state) {
    onScreen = state;
  }
  
  public boolean isOnScreen() {
    return onScreen;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

	/**
	 * INTERNAL USE
	 * set the elapsed times from search
	 * @param ftime time
	 * @param stime time
	 */
	public void setTimes(long ftime, long stime) {
    lastFindTime = ftime;
    lastSearchTime = stime;
  }

	/**
	 *
	 * @return this Match's actual waiting time from last successful find
	 */
	public long getTime() {
		return lastFindTime;
	}

  /**
   * INTERNAL USE
   */
  protected Match() {}
  
  /**
   * create a copy of Match object<br>
   * to e.g. set another TargetOffset for same match
   *
   * @param m other Match
   */
  public Match(Match m) {
    init(m.x, m.y, m.w, m.h, m.getScreen());
    copy(m);
  }

	/**
	 * create a Match from a region with given SimScore
	 * @param reg Region
	 * @param sc SimScore
	 */
	public Match(Region reg, double sc) {
    init(reg.x, reg.y, reg.w, reg.h, reg.getScreen());
    simScore = sc;
  }

	/**
	 * create a Match from a region with given SimScore
	 * @param reg Region
	 */
	public Match(Region reg) {
    init(reg.x, reg.y, reg.w, reg.h, reg.getScreen());
  }
  
	public Match(Rectangle reg, double score) {
    init(reg.x, reg.y, reg.width, reg.height, null);
    simScore = score;
  }

  private Match(Match m, IScreen parent) {
    init(m.x, m.y, m.w, m.h, parent);
    copy(m);
  }

  /**
   * internally used constructor by TextRecognizer.listText()
   *
   * @param x x
   * @param y y
   * @param w width
   * @param h height
   * @param Score SimScore
   * @param parent Screen
   * @param text given text
   */
  protected Match(int x, int y, int w, int h, double Score, IScreen parent, String text) {
    init(x, y, w, h, parent);
    simScore = Score;
    ocrText = text;
  }

  public Match(int x, int y, int w, int h, double Score) {
    init(x, y, w, h, null);
    simScore = Score;
    ocrText = null;
  }

  private Match(int _x, int _y, int _w, int _h, double score, IScreen _parent) {
    init(_x, _y, _w, _h, _parent);
    simScore = score;
  }

//  /**
//   * internally used constructor used by findX image
//   *
//   * @param f
//   * @param _parent
//   */
//  protected Match(FindResult f, IScreen _parent) {
//    init(f.getX(), f.getY(), f.getW(), f.getH(), _parent);
//    simScore = f.getScore();
//  }

  private void init(int X, int Y, int W, int H, IScreen parent) {
    x = X;
    y = Y;
    w = W;
    h = H;
    setScreen(parent);
  }

  private void copy(Match m) {
    simScore = m.simScore;
    ocrText = m.ocrText;
    image = m.image;
    target = null;
    if (m.target != null) {
      target = new Location(m.target);
    }
    lastFindTime = m.lastFindTime;
    lastSearchTime = m.lastSearchTime;
  }

  /**
   * the match score
   *
   * @return a decimal value between 0 (no match) and 1 (exact match)
   */
  public double getScore() {
    return simScore;
  }

  /**
   * {@inheritDoc}
   * @return the point defined by target offset (if set) or the center
   */
  @Override
  public Location getTarget() {
    if (target != null) {
      return target;
    }
    return getCenter();
  }

  /**
	 * sets the click target by offset relative to the center
   *
   * @param offset as a Location
   */
  public void setTargetOffset(Location offset) {
    target = new Location(getCenter());
    target.translate(offset.x, offset.y);
  }

  /**
   * like {@link Pattern#targetOffset(int, int) Pattern.targetOffset}
	 * sets the click target relative to the center
   * @param x x offset
   * @param y y offset
   */
  public void setTargetOffset(int x, int y) {
    setTargetOffset(new Location(x,y));
  }

  /**
   * convenience - same as {@link Pattern#getTargetOffset()}
   *
   * @return the relative offset to the center
   */
  public Location getTargetOffset() {
    return target == null ? getCenter() : target;
  }

  /**
   * set the image after finding with success
   * @param img Image
   */
  protected void setImage(Image img) {
    image = img;
    if (Settings.Highlight) {
      highlight(Settings.DefaultHighlightTime);
    }
  }

  /**
   * get the image used for searching
   * @return image or null
   */
  public Image getImage() {
    return image;
  }

  /**
   * get the filename of the image used for searching
   * @return filename
   */
  public String getImageFilename() {
    return image.getFilename();
  }

  /**
   *
   * @return the text used for searching
   */
  public String getText() {
    return ocrText;
  }

  @Override
  public int compareTo(Match m) {
    if (simScore != m.simScore) {
      return simScore < m.simScore ? -1 : 1;
    }
    if (x != m.x) {
      return x - m.x;
    }
    if (y != m.y) {
      return y - m.y;
    }
    if (w != m.w) {
      return w - m.w;
    }
    if (h != m.h) {
      return h - m.h;
    }
    if (equals(m)) {
      return 0;
    }
    return -1;
  }

  @Override
  public boolean equals(Object oThat) {
    if (this == oThat) {
      return true;
    }
    if (!(oThat instanceof Match)) {
      return false;
    }
    Match that = (Match) oThat;
    return x == that.x && y == that.y && w == that.w && h == that.h
            && Math.abs(simScore - that.simScore) < 1e-5 && getTarget().equals(that.getTarget());
  }

  @Override
  public String toString() {
    String starget = "";
    Location c = getCenter();
    if (target != null) {
      if (!c.equals(target)) {
        starget = String.format(" [%d,%d]", target.x, target.y);
      }
    } else {
      starget = "";
    }
    String sScreen = "";
    if (getScreen().getID() != 0) {
      String.format("@S(%s)",(getScreen()== null || !onScreen) ? "?" : getScreen().getID());
    }
    String sScore = String.format("%%%.4f", simScore*100).replace(",", ".");
    return String.format("M[%d,%d %dx%d %s%s]%s", x, y, w, h, sScore, starget, sScreen);
  }

	@Override
	public String toJSON() {
    String sScore = String.format("%f", simScore).replace(",", ".");
		String strTarget = target == null ? ", []" : String.format(", [%d, %d]", target.x, target.y);
		return String.format("[\"M\", [%d, %d, %d, %d], %s%s]", x, y, w, h, sScore, strTarget);
	}

	/**
	 * for fromJSON
	 * @param tx
	 * @param ty
	 */
	public void setTarget(int tx, int ty) {
		target = new Location(tx, ty);
	}
}
