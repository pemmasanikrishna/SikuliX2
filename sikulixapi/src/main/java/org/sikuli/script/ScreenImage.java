/*
 * Copyright 2010-2014, Sikuli.org, sikulix.com
 * Released under the MIT License.
 *
 * modified RaiMan
 */
package org.sikuli.script;

import org.sikuli.util.Debug;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.Date;
import javax.imageio.ImageIO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

/**
 * stores a BufferedImage usually ceated by screen capture,
 * the screen rectangle it was taken from and
 * the filename, where it is stored as PNG (only if requested)
 */
public class ScreenImage {

	public int x = 0, y = 0, w = 0, h = 0;
	private Rectangle _roi = new Rectangle(0,0,0,0);
	private BufferedImage _img = null;
	private String _filename = null;
  private Mat _mat = null;
  private Region _reg = null;

//<editor-fold defaultstate="collapsed" desc="logging">
  private static final int lvl = 3;
  private static final Logger logger = LogManager.getLogger("SX.ScreenImage");

  private static void log(int level, String message, Object... args) {
    if (Debug.is(lvl)) {
      message = String.format(message, args).replaceFirst("\\n", "\n          ");
      if (level == 3) {
        logger.debug(message, args);
      } else if (level > 3) {
        logger.trace(message, args);
      } else if (level == -1) {
        logger.error(message, args);
      } else {
        logger.info(message, args);
      }
    }
  }

  private static void logp(String message, Object... args) {
    System.out.println(String.format(message, args));
  }

  private static void terminate(int retval, String message, Object... args) {
    logger.fatal(String.format(" *** terminating: " + message, args));
    System.exit(retval);
  }
  
  private static long started = 0;
  
  private static void start() {
    started = new Date().getTime();
  }
    
  private static long end() {
    return end("");
  }
  
  private static long end(String message) {
    long ended = new Date().getTime();
    long diff = ended - started;
    if (!message.isEmpty()) {
      logp("[time] %s: %d msec", message, diff);
    }
    started = ended;
    return diff;
  }
  
//</editor-fold>
  
  private ScreenImage() {}
  
	/**
	 * create ScreenImage from given region as CV-Mat
	 *
	 * @param roi the rectangle it was taken from
	 * @param img the BufferedImage
	 */
	public ScreenImage(Region aReg) {
    _reg = aReg;
    init(aReg.getRect(), null);
  }
  
  /**
	 * create ScreenImage from given image as CV-Mat
	 *
	 * @param roi the rectangle it was taken from
	 * @param img the BufferedImage
	 */
	public ScreenImage(Rectangle roi, BufferedImage img) {
    init(roi, img);
  }
  
  private void init(Rectangle roi, BufferedImage img) {
    if (img == null) {
      _img = ((RobotDesktop) _reg.getScreen().getRobot()).captureRegion(_reg.getRect());
      ((Screen) _reg.getScreen()).setlastScreenImage(this);
    } else {
      _img = img;
    }
		_roi = roi;
    _mat = Image.makeMat(_img);
		x = (int) roi.getX();
		y = (int) roi.getY();
		w = (int) roi.getWidth();
		h = (int) roi.getHeight();
    _filename = null;
	}
  
  public Mat getMat() {
    return _mat;
  }
  
	/**
	 *
	 * @return the stored image in memory
	 */
	public BufferedImage getImage() {
		return _img;
	}

	/**
	 *
	 * @return the screen rectangle, the iamge was created from
	 */
	public Rectangle getROI() {
		return _roi;
	}
  
  public int width() {
    return w;
  }

  public int height() {
    return h;
  }
  
  public ScreenImage getSub(Rectangle sub) {
    if (!_roi.contains(sub)) {
      return this;
    }
    BufferedImage img = _img.getSubimage(sub.x - x, sub.y - y, sub.width, sub.height);
    return new ScreenImage(sub, img);
  }

	/**
	 * creates the PNG tempfile only when needed.
	 *
	 * @return absolute path to stored tempfile
	 * @throws IOException if not found
	 * @deprecated use getFile() instead
	 */
	@Deprecated
	public String getFilename() throws IOException {
		return getFile();
	}

  /**
   * use save() instead
   * @return absolute path to stored file
   */
  @Deprecated
  public String getFile() {
    return save();
  }
  
	/**
	 * stores the image as PNG file in the standard temp folder
	 * with a created filename (sikuliximage-timestamp.png)
	 * if not yet stored before
	 *
	 * @return absolute path to stored file
	 */
  public String save() {
    File fImage = new File(RunTime.get().fpBaseTempPath, 
            String.format("sikuliximage-%d.png", new Date().getTime()));
    return saveImage(fImage, false);
  }
  
	/**
   * use save() instead
	 *
	 * @param path valid path string
	 * @return absolute path to stored file
	 */
  @Deprecated
  public String getFile(String path) {
    return save(path);
  }

	/**
	 * stores the image as PNG file in the given path
	 * with a created filename (sikuliximage-timestamp.png)
	 *
	 * @param path valid path string (if null or empty temp is used)
	 * @return absolute path to stored file
	 */
  public String save(String path) {
    if (path == null || path.isEmpty()) {
      return save();
    }
    File fImage = new File(path, 
            String.format("sikuliximage-%d.png", new Date().getTime()));
    return saveImage(fImage, false);
  }

	/**
   * use save() instead
	 *
	 * @param path valid path string (if null or empty temp is used)
	 * @param name filename (.png is added if not present)
	 * @return absolute path to stored file
	 */
  @Deprecated
	public String getFile(String path, String name) {
		return save(path, name);
	}

	/**
	 * stores the image as PNG file in the given path
	 * with a created filename (givenName-timestamp.png)
	 *
	 * @param path valid path string
	 * @return absolute path to stored file
	 */
  public String save(String path, String name) {
    if (path == null || path.isEmpty()) {
      path = RunTime.get().fpBaseTempPath;
    }
    if (name == null || name.isEmpty()) {
      name = String.format("sikuliximage-%d.png", new Date().getTime());
    } else if (!name.endsWith(".png")) {
			name += ".png";
		}
    File fImage = new File(path, name);
    return saveImage(fImage, false);
  }

	public String saveInBundle(String name) {
    if (!name.endsWith(".png")) {
			name += ".png";
		}
    if (!name.startsWith("_")) {
			name = "_" + name;
		}
    File fImage = new File(name);
    fImage = new File(ImagePath.getBundlePath(), name);
    return saveImage(fImage, true);
  }
  
  private String saveImage(File fImage, boolean inBundle) {
    String feature = inBundle ? "saveInBundle" : "save";
    try {
      if (!createFile(fImage)) {
        log(-1, "%s Mat: did not work: %s", feature, fImage.getName());
        return null;
      }
    } catch (IOException iOException) {
      log(-1, "%s: did not work: %s", feature, fImage.getName());
      return null;
    }
    return fImage.getAbsolutePath();
  }
  
	// store image to given path if not yet stored
	private boolean createFile(File fImage) throws IOException {
		String filename = fImage.getAbsolutePath();
    boolean success = true;
		if (_filename == null || !filename.equals(_filename) || fImage.getName().startsWith("_")) {
      if (_mat != null) {
        success = Highgui.imwrite(fImage.getAbsolutePath(), _mat);
      } else {
        ImageIO.write(_img, "png", fImage);
      }
			if (success) {
        _filename = filename;
      }
		}
    return success;
	}

  public String saveLastScreenImage(File fPath) {
    return save(fPath.getAbsolutePath(), "sikulixlastshot");
  }
}
