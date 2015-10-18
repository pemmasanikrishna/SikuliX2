/*
 * Copyright 2010-2014, Sikuli.org, sikulix.com
 * Released under the MIT License.
 *
 * modified RaiMan
 */
package org.sikuli.script;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.imgproc.Imgproc;
import org.sikuli.util.Debug;
import org.sikuli.util.FileManager;
import org.sikuli.util.Settings;
import org.sikuli.natives.Vision;
import org.opencv.highgui.Highgui;

/**
 * This class hides the complexity behind image names given as string.
 * <br>Image does not have public nor protected constructors: use create()
 * <br>It's companion is {@link ImagePath} that maintains a list of places, where image files are
 * loaded from.<br>
 * Another companion {@link ImageGroup} will allow to look at images in a folder as a
 * group.<br>
 * An Image object:<br>
 * - has a name, either given or taken from the basename<br>
 * - keeps it's in memory buffered image in a configurable cache avoiding reload
 * from source<br>
 * - remembers, where it was found when searched the last time<br>
 * - can be sourced from the filesystem, from jars, from the web and from other
 * in memory images <br>
 * - will have features for basic image manipulation and presentation<br>
 * - contains the stuff to communicate with the underlying OpenCV based search
 * engine <br>
 *
 * This class maintains<br>
 * - a list of all images ever loaded in this session with their source
 * reference and a ref to the image object<br>
 * - a list of all images currently having their content in memory (buffered
 * image) (managed as a configurable cache)<br>
 * The caching can be configured using {@link Settings#setImageCache(int)}
 */
public class Image {

  static RunTime runTime = RunTime.get();

//<editor-fold defaultstate="collapsed" desc="logging">
  private static final int lvl = 3;
  private static final Logger logger = LogManager.getLogger("SX.Image");

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

  private void logp(String message, Object... args) {
    System.out.println(String.format(message, args));
  }

  public void terminate(int retval, String message, Object... args) {
    logger.fatal(String.format(" *** terminating: " + message, args));
    System.exit(retval);
  }
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="Caching">
  private static List<Image> images = Collections.synchronizedList(new ArrayList<Image>());
  private static Map<URL, Image> imageFiles = Collections.synchronizedMap(new HashMap<URL, Image>());
  private static Map<String, URL> imageNames = Collections.synchronizedMap(new HashMap<String, URL>());
  private static int KB = 1024;
  private static int MB = KB * KB;
  private final static String isBImg = "__BufferedImage__";
  
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
  
  public static void reload(String fpImage) {
    URL imgURL = FileManager.makeURL(fpImage);
    if (imageFiles.containsKey(imgURL)) {
      Image image = imageFiles.get(imgURL);
      int sizeOld = image.msize;
      if (image.load()) {
        currentMemoryDownUp(sizeOld, image.msize);
      }
    }
  }
  
  /**
   * purge the given image file's in memory image data and remove it from cache.
   * @param imgName name of an image
   */
  public static void unCacheBundledImage(String imgName) {
    URL imgURL = imageNames.get(imgName);
    if (imgURL != null) {
      unCacheImage(imgURL);
    }
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
    log(lvl, "ImageNames entries: %d", imageNames.size());
    Iterator<Map.Entry<String, URL>> nit = imageNames.entrySet().iterator();
    Map.Entry<String, URL> name;
    while (nit.hasNext()) {
      name = nit.next();
      log(lvl, "%s %d KB (%s)", new File(name.getKey()).getName(),
							imageFiles.get(name.getValue()).getKB(), name.getValue());
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
    imageNames.clear();
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
    List<String> imageNamePurgeList = new ArrayList<String>();
    URL imgURL;
    Image img;
    log(lvl, "purge: ImagePath: %s", pathURL.getPath());
    Iterator<Map.Entry<URL, Image>> it = imageFiles.entrySet().iterator();
    Map.Entry<URL, Image> entry;
    while (it.hasNext()) {
      entry = it.next();
      imgURL = entry.getKey();
      if (imgURL.toString().startsWith(pathURL.toString())) {
        log(lvl + 1, "purge: URL: %s", imgURL.toString());
        img = entry.getValue();
        imagePurgeList.add(img);
        imageNamePurgeList.add(img.imageName);
        it.remove();
      }
    }
    if (!imagePurgeList.isEmpty()) {
      Iterator<Image> bit = images.iterator();
      while (bit.hasNext()) {
        img = bit.next();
        if (imagePurgeList.contains(img)) {
          bit.remove();
          log(lvl + 1, "purge: bimg: %s", img);
          currentMemoryDown(img.msize);
        }
      }
    }
    for (String name : imageNamePurgeList) {
      imageNames.remove(name);
    }
  }
//</editor-fold>
  
//<editor-fold defaultstate="collapsed" desc="imageName">
  private String imageName = null;

  public String getName() {
    return imageName;
  }

  public Image setImageName(String imageName) {
    this.imageName = imageName;
    return this;
  }

//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="fileURL">
  private URL fileURL = null;
  private String imageAsFile = null;

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
  
  private static String getNameFromURL(URL fURL) {
    //TODO add handling for http
    if ("jar".equals(fURL.getProtocol())) {
      int n = fURL.getPath().lastIndexOf(".jar!/");
      int k = fURL.getPath().substring(0, n).lastIndexOf("/");
      if (n > -1) {
        return "JAR:" + fURL.getPath().substring(k + 1, n) + fURL.getPath().substring(n + 5);
      }
    }
    return "???:" + fURL.getPath();
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
      log(lvl, "makeMat: INT_RGB (%dx%d)", bImg.getWidth(), bImg.getHeight());
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
      log(lvl, "makeMat: 3BYTE_BGR (%dx%d)", bImg.getWidth(), bImg.getHeight());
      log(-1, "not supported");
    } else {
      log(lvl, "makeMat: not supported: Type: %d (%dx%d)", 
              bImg.getType(), bImg.getWidth(), bImg.getHeight());      
    }
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
    return bimg;
  }
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="ImageGroup">
  private ImageGroup group = null;

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
  
//<editor-fold defaultstate="collapsed" desc="isText">
  private boolean imageIsText = false;

  /**
   *
   * @return true if the given image name did not give a valid image so it might
   * be text to search
   */
  public boolean isText() {
    return imageIsText;
  }

  /**
   * wether this image's name should be taken as text
   * @param val
   */
  public Image setIsText(boolean val) {
    imageIsText = val;
    return this;
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

  private void setIsAbsolute(boolean val) {
    imageIsAbsolute = val;
  }

//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="isBundled">
  private boolean imageIsBundled = false;

  /**
   * mark this image as being contained in a bundle
   * @param imageIsBundled
   */
  public Image setIsBundled(boolean imageIsBundled) {
    this.imageIsBundled = imageIsBundled;
    return this;
  }

  /**
   * INTERNAL USE: image is contained in a bundle (.sikuli)
   * @return true/false
   */
  public boolean isBundled() {
    return imageIsBundled;
  }

//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="isPattern">
  private boolean imageIsPattern = false;

  /**
   * true if this image contains pattern aspects<br>
   * only useable with the new ImageFinder
   * @return true if yes, false otherwise
   */
  public boolean isPattern() {
    return imageIsPattern;
  }

  public Image setIsPattern(boolean imageIsPattern) {
    this.imageIsPattern = imageIsPattern;
    return this;
  }

//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="waitAfter">
  private int waitAfter;

  /**
   * Get the value of waitAfter
   *
   * @return the value of waitAfter
   */
  public int getWaitAfter() {
    return waitAfter;
  }

  /**
   * Set the value of waitAfter
   *
   * @param waitAfter new value of waitAfter
   */
  public Image setWaitAfter(int waitAfter) {
    this.waitAfter = waitAfter;
    return this;
  }
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="offset">
  private Location offset = new Location(0, 0);

  /**
   * Get the value of offset
   *
   * @return the value of offset
   */
  public Location getOffset() {
    return offset;
  }

  /**
   * Set the value of offset
   *
   * @param offset new value of offset
   */
  public Image setOffset(Location offset) {
    this.offset = offset;
    return this;
  }
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="similarity">
  private float similarity = (float) Settings.MinSimilarity;

  /**
   * Get the value of similarity
   *
   * @return the value of similarity
   */
  public float getSimilarity() {
    return similarity;
  }

  /**
   * Set the value of similarity
   *
   * @param similarity new value of similarity
   */
  public Image setSimilarity(float similarity) {
    this.similarity = similarity;
    return this;
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
  private int rowHd = 0;
  private int colWd = 0;
  
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
      rowHd = getSize().height - r * rowH;
    }
    if (c > 0) {
      colW = (int) (getSize().width / c);
      colWd = getSize().width - c * colW;
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
  
  private boolean beSilent = false;

  @Override
  public String toString() {
    return String.format("I[%s (%dx%d)%s]",
            (imageName != null ? imageName : "__UNKNOWN__"), mwidth, mheight,
            (lastSeen == null ? "" : String.format(" at(%d,%d) %%%.2f", 
                    lastSeen.x, lastSeen.y, (int) (lastScore*100))));
  }

//<editor-fold defaultstate="collapsed" desc="Constructors">
  public boolean isValid() {
    return fileURL != null || imageName.contains(isBImg);
  }

  public boolean isUseable() {
    return isValid() || imageIsPattern;
  }

  private Image() {
  }
  
  public Image(String imgName) {
    init(imgName, null, false);
  }
  
  public static Image get(String imgName) {
    URL imgURL = imageNames.get(imgName);
    if (null == imgURL) {
      return null;
    }
    return imageFiles.get(imgURL);
  }
  
  /**
   * create a new image from the given url <br>
   * file ending .png is added if missing <br>
   * filename: ...url-path.../name[.png] is loaded from the url and and cached
   * <br>
   * already loaded image with same url is reused (reference) and taken from
   * cache
   *
   * @param url image file URL
   * @return the image
   */
  public Image(URL imgURL) {
    init(null, imgURL, false);
  }
  
  public static Image get(URL imgURL) {
    return imageFiles.get(imgURL);
  }
  
  public Image(Region reg) {
    
  }
  
  /**
   * create a new Image with Pattern aspects from an existing Pattern
   * @param p a Pattern
   * @return the new Image
   */
  public Image(Pattern pat) {
    init(null, pat.getFileURL(), false);
  }
  
  /**
   * create a new Image from another Image
   * @param img
   */
  public Image(Image img) {
    init(null, img.getURL(), false);
  }
  
  /**
   * create a new image from a buffered image<br>
   * giving it a reference name for acess, printout and logging <br>
   *
   * @param img BufferedImage
   * @param name unique reference name
   */
  public Image(BufferedImage img, String name) {
    imageName = isBImg;
    if (name != null) {
      imageName += name;
    }
    mwidth = img.getWidth();
    mheight = img.getHeight();
		log(lvl, "BufferedImage: (%d, %d)%s", mwidth, mheight,
						(name == null ? "" : " with name: " + name));
    terminate(1, "not supported: Image(BufferedImage img, String name)");
  }

  /**
   * create a new image from a Sikuli ScreenImage (captured)<br>
   * giving it a reference name for access, printout and logging <br>
   *
   * @param img ScreenImage
   * @param name unique reference name
   */
  public Image(ScreenImage img, String name) {
    this(img.getImage(), name);
  }

  /**
   * create a new Image from a valid object
   * @param obj String, Pattern, Region or other Image
   * @return Image or null
   */
  protected static Image createFromObject(Object obj) {
    if (obj instanceof String) {
      return new Image((String) obj);
    } else if (obj instanceof Image) {
      return new Image((Image) obj);
    }  else if (obj instanceof Pattern) {
      return new Image((Pattern) obj);
    }  else if (obj instanceof Region) {
      return new Image((Region) obj);
    }
    return null;
  }
  
  private void init(String imgName, URL imgURL, boolean silent) {
    if (imgName == null || imgName.isEmpty()) {
      return;
    }
    if (imgURL == null) {
      get(imgName, imgURL);
    } else {
      imageName = imgName;
      fileURL= imgURL;
    }
    if (ImagePath.isImageBundled(fileURL)) {
      imageIsBundled = true;
      imageName = new File(imageName).getName();
    }
    beSilent = silent;
  }
  
  private boolean get(String fName, URL imgURL) {
    boolean success = true;
    if (fName == null || fName.isEmpty()) {
      return success;
    }
    fName = FileManager.slashify(fName, false);
    Image img = null;
    URL fURL = null;
    String fileName = Settings.getValidImageFilename(fName);
    if (fileName.isEmpty()) {
      log(-1, "not a valid image type: " + fName);
      fileName = fName;
    }
    File imgFile = new File(fileName);
    if (imgFile.isAbsolute()) {
      if (imgFile.exists()) {
        fURL = FileManager.makeURL(fileName);
      }
    } else {
      fURL = imageNames.get(fileName);
      if (fURL == null) {
        fURL = ImagePath.find(fileName);
      }
    }
    if (fURL != null) {
      fileURL = fURL;
      imageName = fileName;
      img = imageFiles.get(fileURL);
      if (img != null && null == imageNames.get(img.imageName)) {
        imageNames.put(img.imageName, fileURL);
      }
    }
    if (img == null) {
      success = load();
    } else {
      if (img.getMat() != null) {
        log(3, "reused: %s (%s)", img.imageName, img.fileURL);
      } else {
        success = load();
      }
    }
    imageIsAbsolute = imgFile.isAbsolute();
    return success;
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
          imageNames.put(imageName, fileURL);
        }
        mwidth = mat.width();
        mheight = mat.height();
        msize = mat.channels() * mwidth * mheight;;
        log(lvl, "loaded: %s\n%s", imageName, fileURL);
        if (isCaching()) {
          int maxMemory = Settings.getImageCache() * MB;
          currentMemoryUp(msize);
          images.add(this);
          log(lvl, "cached: %s (%d KB) (# %d KB %d -- %d %% of %d MB)",
              imageName, getKB(),
              images.size(), (int) (currentMemory / KB),
              (int) (100 * currentMemory / maxMemory), (int) (maxMemory / MB));
        }
      } else {
        log(-1, "invalid! not loaded! %s", fileURL);
      }
    }
    return success;
  }
//</editor-fold>
  
  /**
   * resize the loaded image with factor using Graphics2D.drawImage
   * @param factor resize factor
   * @return a new BufferedImage resized (width*factor, height*factor)
   */
  public Image resize(float factor) {
    int type;
    BufferedImage bufimg = get();
    type = bufimg.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : bufimg.getType();
    int width = (int) (getSize().getWidth() * factor);
    int height = (int) (getSize().getHeight() * factor);
    BufferedImage resizedImage = new BufferedImage(width, height, type);
    Graphics2D g = resizedImage.createGraphics();
    g.drawImage(bufimg, 0, 0, width, height, null);
    g.dispose();
    return new Image(this, "resized_" + getName());
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
    return new Image(this, "sub_" + getName());
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
  
  private Image(Image img, String imgName) {
    terminate(1, "not supported: new Image(Image, name)");
  }
}
