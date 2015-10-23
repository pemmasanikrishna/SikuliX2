/*
 * Copyright 2010-2014, Sikuli.org, sikulix.com
 * Released under the MIT License.
 *
 * modified RaiMan
 */
package org.sikuli.script;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfInt;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.sikuli.util.Debug;
import org.sikuli.util.FileManager;
import org.sikuli.util.Settings;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

/**
 * This class hides the complexity behind image names given as string.
 * <br>Image does not have public nor protected constructors: use create()
 * <br>It's companion is {@link ImagePath} that maintains a list of places, where image files are
 * loaded from.<br>
 * Another companion {@link ImageGroup} will allow to look at images in a folder as a
 * group.<br>
 * An Image object:<br>
 * - has a name, either given or taken from the basename<br>
 * - keeps it's content in memory as CV-Mat in a configurable cache avoiding reload from source<br>
 * - remembers, where it was found when searched the last time<br>
 * - can be sourced from the filesystem, from jars, from the web and from other
 * in memory images <br>
 * - has features for basic image manipulation and presentation<br>
 */
public class Image {

  static RunTime runTime = RunTime.get();

//<editor-fold defaultstate="collapsed" desc="logging">
  private static final int lvl = 3;
  private static final Logger logger = LogManager.getLogger("SX.Image");

  private static void log(int level, String message, Object... args) {
    if (Debug.is(lvl)) {
      message = String.format(message, args).replaceFirst("\\n", "\n          ");
      if (level == lvl) {
        logger.debug(message, args);
      } else if (level > lvl) {
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

  public static void terminate(int retval, String message, Object... args) {
    logger.fatal(String.format(" *** terminating: " + message, args));
    System.exit(retval);
  }
//</editor-fold>

  private static List<Image> images = Collections.synchronizedList(new ArrayList<Image>());
  private static Map<URL, Image> imageFiles = Collections.synchronizedMap(new HashMap<URL, Image>());

//<editor-fold defaultstate="collapsed" desc="Caching">
  private static int KB = 1024;
  private static int MB = KB * KB;
  
  private static long currentMemory = 0;
  
  private static synchronized long currentMemoryChange(long size, long max) {
    long maxMemory = max;
    if (max < 0) {
      maxMemory = Settings.getImageCache() * MB;
      currentMemory += size;
    }
    if (currentMemory > maxMemory) {
      Image first;
      while (images.size() > 0 && currentMemory > maxMemory) {
        first = images.remove(0);
        first.mat = null;
        currentMemory -= first.msize;
      }
      if (maxMemory == 0) {
        currentMemory = 0;
      } else {
        currentMemory = Math.max(0, currentMemory);
      }
    }
    if (size < 0) {
      currentMemory = Math.max(0, currentMemory);
    }
    return currentMemory;
  }
  
  private static long currentMemoryUp(long size) {
    return currentMemoryChange(size, -1);
  }
  
  private static long currentMemoryDown(long size) {
    currentMemory -= size;
    currentMemory = Math.max(0, currentMemory);
    return currentMemoryChange(-size, -1);
  }
  
  private static long currentMemoryDownUp(int sizeOld, int sizeNew) {
    currentMemoryDown(sizeOld);
    return currentMemoryUp(sizeNew);
  }
  
  private static boolean isCaching() {
    return Settings.getImageCache() > 0;
  }
  
  public static void clearCache(int maxSize) {
    currentMemoryChange(0, maxSize);
  }
  
  /**
   * Print the current state of the cache
   */
  public static void dump() {
    dump(0);
  }

  /**
   * Print the current state of the cache, verbosity depends on debug level
   * @param lvl debug level used here
   */
  public static void dump(int lvl) {
    log(lvl, "--- start of Image dump ---");
    ImagePath.dump(lvl);
    log(lvl, "ImageFiles entries: %d", imageFiles.size());
    Iterator<Map.Entry<URL, Image>> it = imageFiles.entrySet().iterator();
    Map.Entry<URL, Image> entry;
    while (it.hasNext()) {
      entry = it.next();
      log(lvl, entry.getKey().toString());
    }
    if (Settings.getImageCache() == 0) {
      log(lvl, "Cache state: switched off!");
    } else {
      log(lvl, "Cache state: Max %d MB (entries: %d  used: %d %% %d KB)",
              Settings.getImageCache(), images.size(),
              (int) (100 * currentMemory / (Settings.getImageCache() * MB)), (int) (currentMemory / KB));
    }
    log(lvl, "--- end of Image dump ---");
  }
  
  /**
   * clears all caches (should only be needed for debugging)
   */
  public static void reset() {
    clearCache(0);
    imageFiles.clear();
  }
  
  /**
   * INTERNAL USE: IDE: to get rid of cache entries at script save, close or
   * save as
   *
   * @param bundlePath absolute path for an image set in this folder
   */
  public static void purge(String bundlePath) {
    if (imageFiles.isEmpty() || ImagePath.getPaths().get(0) == null) {
      return;
    }
    URL pathURL = FileManager.makeURL(bundlePath);
    if (!ImagePath.getPaths().get(0).pathURL.equals(pathURL)) {
      log(-1, "purge: not current bundlepath: " + pathURL);
      return;
    }
    purge(pathURL);
  }

  public static void purge(ImagePath.PathEntry path) {
    if (path == null) {
      return;
    }
    purge(path.pathURL);
  }

  public static synchronized void purge(URL pathURL) {
    List<Image> imagePurgeList = new ArrayList<Image>();
    URL imgURL;
    Image img;
    Iterator<Map.Entry<URL, Image>> it = imageFiles.entrySet().iterator();
    Map.Entry<URL, Image> entry;
    while (it.hasNext()) {
      entry = it.next();
      imgURL = entry.getKey();
      if (imgURL.toString().startsWith(pathURL.toString())) {
        img = entry.getValue();
        imagePurgeList.add(img);
        it.remove();
      }
    }
    if (!imagePurgeList.isEmpty()) {
      Iterator<Image> bit = images.iterator();
      while (bit.hasNext()) {
        img = bit.next();
        if (imagePurgeList.contains(img)) {
          bit.remove();
          currentMemoryDown(img.msize);
        }
      }
    }
  }
//</editor-fold>

  protected static String getValidImageFilename(String fname) {
    String validEndings = ".png.jpg.jpeg.tiff.bmp";
    String defaultEnding = ".png";
    int dot = fname.lastIndexOf(".");
    String ending = defaultEnding;
    if (dot > 0) {
      ending = fname.substring(dot);
      if (validEndings.contains(ending.toLowerCase())) {
        return fname;
      }
    } else {
      fname += ending;
      return fname;
    }
    return "";
  }
  
//<editor-fold defaultstate="collapsed" desc="fileURL">
  private URL fileURL = null;

  public URL getURL() {
    return fileURL;
  }
  
  /**
   * @return the image's absolute filename or null if jar, http or in memory
   * image
   */
  public String getFilename() {
    if (fileURL != null && "file".equals(fileURL.getProtocol())) {
      return new File(fileURL.getPath()).getAbsolutePath();
    } else {
			return null;
		}
  }
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="CV-Mat">
  private Mat mat = null;

  private int msize = 0;
  private int mwidth = -1;
  private int mheight = -1;
  
  public Mat getMat() {
    return mat;
  }
  
  public static Mat makeMat(BufferedImage bImg) {
    Mat aMat = null;
    if (bImg.getType() == BufferedImage.TYPE_INT_RGB) {
//      log(lvl, "makeMat: INT_RGB (%dx%d)", bImg.getWidth(), bImg.getHeight());
      int[] data = ((DataBufferInt) bImg.getRaster().getDataBuffer()).getData();
      ByteBuffer byteBuffer = ByteBuffer.allocate(data.length * 4); 
      IntBuffer intBuffer = byteBuffer.asIntBuffer();
      intBuffer.put(data);
      aMat = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC4);
      aMat.put(0, 0, byteBuffer.array());
      Mat oMatBGR = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC3);
      Mat oMatA = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC1);
      List<Mat> mixIn = new ArrayList<Mat>(Arrays.asList(new Mat[]{aMat}));
      List<Mat> mixOut = new ArrayList<Mat>(Arrays.asList(new Mat[]{oMatA, oMatBGR}));
      //A 0 - R 1 - G 2 - B 3 -> A 0 - B 1 - G 2 - R 3 
      Core.mixChannels(mixIn, mixOut, new MatOfInt(0, 0, 1, 3, 2, 2, 3, 1));
      return oMatBGR;
    } else if (bImg.getType() == BufferedImage.TYPE_3BYTE_BGR) {
      log(-1, "makeMat: 3BYTE_BGR (%dx%d)", bImg.getWidth(), bImg.getHeight());
    } else {
      log(-1, "makeMat: not supported: Type: %d (%dx%d)", 
              bImg.getType(), bImg.getWidth(), bImg.getHeight());      
    }
    terminate(1, "not supported for this: makeMat()");
    return aMat;
  }
    
  public int getWidth() {
    if (isValid()) {
      return mwidth;
    } else {
      return -1;
    }
  }

  public int getHeight() {
    if (isValid()) {
      return mheight;
    } else {
      return -1;
    }
  }
  
  public Dimension getSize() {
    return new Dimension(mwidth, mheight);
  }

	private int getKB() {
    if (mat == null) {
      return 0;
    }
		return (int) msize / KB;
	}

  public BufferedImage get() {
    BufferedImage bimg = null;
    terminate(1, "notImplemented: BufferedImage get()");
    return bimg;
  }
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="ImageGroup">
  private ImageGroup group = null;
  
  /**
   *
   * @return true if image is in an ImageGroup
   */
  public boolean hasImageGroup() {
    return group != null;
  }

  /**
   *
   * @return the current ImageGroup
   */
  public ImageGroup getGroup() {
    return group;
  }

  /**
   * set the ImageGroup this image should belong to
   *
   * @param group ImageGroup
   */
  public void setGroup(ImageGroup group) {
    this.group = group;
  }
//</editor-fold>
  
//<editor-fold defaultstate="collapsed" desc="isAbsolute">
  private boolean imageIsAbsolute = false;
  /**
   *
   * @return true if image was given with absolute filepath
   */
  public boolean isAbsolute() {
    return imageIsAbsolute;
  }
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="isBundled">
  private boolean imageIsBundled = false;

  /**
   * image is contained in a bundle (.sikuli)
   * @return true/false
   */
  public boolean isBundled() {
    return imageIsBundled;
  }
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="lastSeen">
  private Rectangle lastSeen = null;
  private double lastScore = 0.0;

  /**
   * if the image was already found before
   *
   * @return the rectangle where it was found
   */
  public Rectangle getLastSeen() {
    return lastSeen;
  }

  /**
   * if the image was already found before
   *
   * @return the similarity score
   */
  public double getLastSeenScore() {
    return lastScore;
  }

  /**
   * Internal Use: set the last seen info after a find
   *
   * @param lastSeen Match
   * @param sim SimilarityScore
   */
  protected Image setLastSeen(Rectangle lastSeen, double sim) {
    this.lastSeen = lastSeen;
    this.lastScore = sim;
    if (group != null) {
      group.addImageFacts(this, lastSeen, sim);
    }
    return this;
  }
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="Raster">
  private int rows = 0;
  private int cols = 0;
  private int rowH = 0;
  private int colW = 0;
  private int rowHdiff = 0;
  private int colWdiff = 0;
  
  public boolean isRasterFilling() {
    return (rowHdiff < rowH) && (colWdiff < colW);
  }
  
  public Dimension getRasterMargin() {
    return new Dimension(rowHdiff, colWdiff);
  }
  
/**
   * store info: this image is divided vertically into n even rows <br>
   * a preparation for using getRow()
   *
   * @param n number of rows
   * @return the top row
   */
  public Image setRows(int n) {
    return setRaster(n, 0);
  }

  /**
   * store info: this image is divided horizontally into n even columns <br>
   * a preparation for using getCol()
   *
   * @param n number of Columns
   * @return the leftmost column
   */
  public Image setCols(int n) {
    return setRaster(0, n);
  }

  /**
   *
   * @return number of eventually defined rows in this image or 0
   */
  public int getRows() {
    return rows;
  }

  /**
   *
   * @return height of eventually defined rows in this image or 0
   */
  public int getRowH() {
    return rowH;
  }

  /**
   *
   * @return number of eventually defined columns in this image or 0
   */
  public int getCols() {
    return cols;
  }

  /**
   *
   * @return width of eventually defined columns in this image or 0
   */
  public int getColW() {
    return colW;
  }

  /**
   * store info: this image is divided into a raster of even cells <br>
   * a preparation for using getCell()
   *
   * @param r number of rows
   * @param c number of columns
   * @return the top left cell
   */
  public Image setRaster(int r, int c) {
    rows = r;
    cols = c;
    if (r > 0) {
      rowH = (int) (getSize().height / r);
      rowHdiff = getSize().height - r * rowH;
    }
    if (c > 0) {
      colW = (int) (getSize().width / c);
      colWdiff = getSize().width - c * colW;
    }
    return getCell(0, 0);
  }

  /**
   * get the specified row counting from 0, if rows or raster are setup <br>negative
   * counts reverse from the end (last = -1) <br>values outside range are 0 or last
   * respectively
   *
   * @param r row number
   * @return the row as new image or the image itself, if no rows are setup
   */
  public Image getRow(int r) {
    if (rows == 0) {
      return this;
    }
    if (r < 0) {
      r = rows + r;
    }
    r = Math.max(0, r);
    r = Math.min(r, rows - 1);
    return getSub(0, r * rowH, getSize().width, rowH);
  }

  /**
   * get the specified column counting from 0, if columns or raster are setup<br>
   * negative counts reverse from the end (last = -1) <br>values outside range are 0
   * or last respectively
   *
   * @param c column number
   * @return the column as new image or the image itself, if no columns are
   * setup
   */
  public Image getCol(int c) {
    if (cols == 0) {
      return this;
    }
    if (c < 0) {
      c = cols + c;
    }
    c = Math.max(0, c);
    c = Math.min(c, cols - 1);
    return getSub(c * colW, 0, colW, getSize().height);
  }

  /**
   * get the specified cell counting from (0, 0), if a raster is setup <br>
   * negative counts reverse from the end (last = -1) <br>values outside range are 0
   * or last respectively
   *
	 * @param r row number
   * @param c column number
   * @return the cell as new image or the image itself, if no raster is setup
   */
  public Image getCell(int r, int c) {
    if (rows == 0) {
      return getCol(c);
    }
    if (cols == 0) {
      return getRow(r);
    }
    if (rows == 0 && cols == 0) {
      return this;
    }
    if (r < 0) {
      r = rows - r;
    }
    if (c < 0) {
      c = cols - c;
    }
    r = Math.max(0, r);
    r = Math.min(r, rows - 1);
    c = Math.max(0, c);
    c = Math.min(c, cols - 1);
    return getSub(c * colW, r * rowH, colW, rowH);
  }
//</editor-fold>
  
//<editor-fold defaultstate="collapsed" desc="toString">
  public String getImageName() {
    if (isInMemory) {
      return ("isInMemory");
    } else {
      return new File(fileURL.getPath()).getName();
    }
  }
  
  @Override
  public String toString() {
    return String.format("I[%s (%dx%d)%s]",
            getImageName(), mwidth, mheight,
            (lastSeen == null ? "" : String.format(" at(%d,%d) %%%.2f",
                    lastSeen.x, lastSeen.y, (int) (lastScore*100))));
  }
  
  public String toJSON(boolean withLastSeen) {
    return String.format("[\"I\", \"%s\", %d, %d%s]",
            fileURL, mwidth, mheight,
            (withLastSeen && lastSeen != null) ? ", " + new Match(lastSeen, lastScore).toJSON() : "");
  }
  
  public String toJSON() {
    return toJSON(true);
  }
//</editor-fold>
  
//<editor-fold defaultstate="collapsed" desc="Constructors">
  public boolean isValid() {
    return fileURL != null && mat != null;
  }

  public boolean isUseable() {
    return isValid() || isInMemory;
  }
  
  private boolean hasText = false;
  public boolean isText() {
    return hasText;
  }

  private Image() {
  }
  
  private Image(URL fURL) {
    fileURL = fURL;
  }
  
  public Image(Object img) {
    terminate(1, "Image(Object)");
  }

  public static Image get(Object source) {
    Image img = new Image();
    if (null == source) {
      return img;
    }
    URL fURL = null;
    if (source instanceof URL) {
      fURL = (URL) source; 
    } else if (source instanceof String) {
      fURL = ImagePath.find((String) source);
    }
    if (null != fURL) {
      img = imageFiles.get(fURL);
      if (img == null) {
        img = new Image(fURL);
        img.load();
      } else {
        log(lvl, "get: reused: %s (%dx%d)", img.getImageName(), img.getWidth(), img.getHeight());        
      }
    }
    return img;
  }
  
  private boolean load() {
    boolean success = true;
    if (fileURL != null) {
      mat = null;
      File imgFile = new File(fileURL.getPath());
      mat = Highgui.imread(imgFile.getAbsolutePath());
      if (mat.empty()) {
        mat = null;
        success = false;
      }
      if (success) {
        if (!imageFiles.containsKey(fileURL)) {
          imageFiles.put(fileURL, this);
        }
        mwidth = mat.width();
        mheight = mat.height();
        msize = mat.channels() * mwidth * mheight;;
        String msg = String.format("load: loaded: (%dx%s)\n%s", mwidth, mheight, fileURL);
        if (isCaching()) {
          int maxMemory = Settings.getImageCache() * MB;
          currentMemoryUp(msize);
          images.add(this);
          msg = String.format("load: cached: (%dx%d) #%d %%%d of %dMB)\n%s",
              mwidth, mheight, images.size(),
              (int) (100 * currentMemory / maxMemory), (int) (maxMemory / MB), fileURL);
        }
        log(lvl, "%s", msg);
        checkProbe();
      } else {
        log(-1, "load: not loaded - taken as text:\n%s", fileURL);
        hasText = true;
      }
    }
    return success;
  }
//</editor-fold>
  
//<editor-fold defaultstate="collapsed" desc="inMemory BImg, SImg, Region">
  private boolean isInMemory = false;
  
  private Image(Mat aMat) {
    mat = aMat;
    mwidth = mat.width();
    mheight = mat.height();
    msize = mat.channels() * mwidth * mheight;;
    isInMemory = true;
  }
    
  /**
   * create a new image with name as timestamp from a region's captured content<br>
   *
   * @param reg
   */
  public Image(Region reg) {
    this(reg.captureThis());
  }
  
  /**
   * create a new image with name as timestamp from a ScreenImage (captured before)<br>
   *
   * @param img ScreenImage
   */
  public Image(ScreenImage img) {
    this(img.getImage());
  }

  /**
   * create a new image with name as timestamp from a buffered image<br>
   *
   * @param bImg BufferedImage
   */
  public Image(BufferedImage bImg) {
    mat = makeMat(bImg);
    if (mat != null) {
      mwidth = mat.width();
      mheight = mat.height();
      msize = mat.channels() * mwidth * mheight;;
      isInMemory = true;
    }
  }
  
  public Image add() {
    if (isUseable()) {
      if (images.remove(this)) {
        currentMemoryDown(msize);
      }
      images.add(this);
    }
    return this;
  }

  public Image update() {
    if (isValid()) {
      if (images.remove(this)) {
        currentMemoryDown(msize);
      }
      if (load()) {
        currentMemoryUp(msize);
      }
    }
    return this;
  }

  public Image remove() {
    if (isUseable()) {
      
    }
    return this;
  }

  /**
   * purge the given image's in memory image data and remove it from cache.
   * @param imgURL URL of an image file
   */
  public static void unCacheImage(URL imgURL) {
    Image img = imageFiles.get(imgURL);
    if (img == null) {
      return;
    }
    img.mat = null;
    images.remove(img);
  }
//</editor-fold>
 
//<editor-fold defaultstate="collapsed" desc="inMemory features">
  private final int resizeMinDownSample = 12;
  private double resizeFactor;
  private boolean plainColor = false;
  private boolean blackColor = false;
  
  public boolean isPlainColor() {
    return plainColor;
  }
  
  public boolean isBlack() {
    return blackColor;
  }
  
  private void checkProbe() {
    resizeFactor = Math.min(((double) mwidth) / resizeMinDownSample, ((double) mheight) / resizeMinDownSample);
    resizeFactor = Math.max(1.0, resizeFactor);
    
    MatOfDouble pMean = new MatOfDouble();
    MatOfDouble pStdDev = new MatOfDouble();
    Core.meanStdDev(mat, pMean, pStdDev);
    double min = 1.0E-5;
    plainColor = false;
    double sum = 0.0;
    double[] arr = pStdDev.toArray();
    for (int i = 0; i < arr.length; i++) {
      sum += arr[i];
    }
    if (sum < min) {
      plainColor = true;
    }
    sum = 0.0;
    arr = pMean.toArray();
    for (int i = 0; i < arr.length; i++) {
      sum += arr[i];
    }
    if (sum < min && plainColor) {
      blackColor = true;
    }
  }
  
  public double getResizeFactor() {
    return resizeFactor;
  }
  
  /**
   * resize the image's CV-Mat by factor 
   * @param factor resize factor
   * @return a new inMemory Image
   */
  public Image resize(float factor) {
    Image img = new Image();
    if (isUseable()) {
      Mat newMat = new Mat();
      Size newS = new Size(mwidth * factor, mheight * factor);
      Imgproc.resize(mat, newMat, newS, 0, 0, Imgproc.INTER_AREA);
    }
    return img;
  }
  
  /**
   * create a sub image from this image
   *
   * @param x pixel column
   * @param y pixel row
   * @param w width
   * @param h height
   * @return the new image
   */
  public Image getSub(int x, int y, int w, int h) {
    Image img = new Image();
    if (isUseable()) {
      img = new Image(this.getMat().submat(new Rect(x, y, w, h)));
    }
    return img;
  }
  
  /**
   * create a sub image from this image
   *
   * @param part (the constants Region.XXX as used with {@link Region#get(int)})
   * @return the sub image
   */
  public Image getSub(int part) {
    Rectangle r = Region.getRectangle(new Rectangle(0, 0, getSize().width, getSize().height), part);
    return getSub(r.x, r.y, r.width, r.height);
  }
//</editor-fold>
}