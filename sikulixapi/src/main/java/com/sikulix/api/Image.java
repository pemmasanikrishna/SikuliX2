/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.Content;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.core.SXElement;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.List;

public class Image extends Element {

  private static eType eClazz = eType.IMAGE;
  private static SXLog log = SX.getLogger("SX." + eClazz.toString());

  //<editor-fold desc="*** construction">
  public Image() {
  }

  protected void setClazz() {
    clazz = eClazz;
  }

  protected void copy(Element elem) {
    content = elem.content;
    urlImg = elem.urlImg;
  }

  public Image(BufferedImage bimg) {
    this();
    content = makeMat(bimg);
    init(0, 0, content.width(), content.height());
  }

  public Image(BufferedImage bimg, Rectangle rect) {
    this();
    content = makeMat(bimg);
    init(rect.x, rect.y, content.width(), content.height());
  }

  public Image(Mat mat) {
    this();
    content = mat;
    init(0, 0, content.width(), content.height());
  }

  public Image(String fpImg) {
    this();
    content = get(fpImg);
    init(0, 0, content.width(), content.height());
  }

  public Image(URL url) {
    this();
    content = get(url);
    init(0, 0, content.width(), content.height());
  }

  public Image(Element elem) {
    super(elem);
    copy(elem);
  }

  public Image(Element elem, double score) {
    super(elem, score);
  }

  public Image(Element elem, double score, Element off) {
    super(elem, score, off);
  }

  public Image(Element elem, Element off) {
    super(elem, off);
  }

  /**
   * @return true if the Element is useable and/or has valid content
   */
  @Override
  public boolean isValid() {
    return !content.empty();
  }

  public boolean isText() {
    //TODO implement isText()
    return false;
  }
  //</editor-fold>

  //<editor-fold desc="*** get content">
  private Mat get(String fpImg) {
    URL url = getURL(fpImg);
    return get(url);
  }

  private Mat get(URL url) {
    Mat mContent = new Mat();
    if (SX.isSet(url)) {
      urlImg = url;
      if (isCaching()) {
        mContent = imageFiles.get(urlImg);
        if (SX.isNull(mContent)) {
          mContent = new Mat();
        }
      }
      if (mContent.empty()) {
        mContent = get();
      }
      if (isCaching() && !mContent.empty()) {
        changeCache(true, urlImg, mContent);
      }
    }
    return mContent;
  }

  private URL getURL(String fpImg) {
    URL url = searchOnImagePath(fpImg);
    return url;
  }

  private Mat get() {
    Mat mContent = new Mat();
    if (urlImg != null) {
      File imgFile = new File(urlImg.getPath());
      mContent = Highgui.imread(imgFile.getAbsolutePath());
      if (!mContent.empty()) {
        log.debug("get: loaded: (%dx%s) %s", mContent.width(), mContent.height(), urlImg);
      } else {
        log.error("get: not loaded: %s", urlImg);
      }
    }
    return mContent;
  }

  public Image reset() {
    if (isCaching() && isValid()) {
      changeCache(false, urlImg, content);
    }
    content = get();
    if (isCaching() && !content.empty()) {
      changeCache(true, urlImg, content);
    }
    return this;
  }
  //</editor-fold>

  //<editor-fold desc="*** cache">
  private static final Map<URL, Mat> imageFiles = Collections.synchronizedMap(new HashMap<URL, Mat>());
  private static long currentMemory = 0;

  private static int ImageCache = 64;
  private static final int KB = 1024;
  private static final int MB = KB * KB;

  /**
   * set the maximum to be used for the {@link Image} cache
   * <br>the start up value is 64 (meaning MB)
   * <br>using 0 switches off caching and clears the cache in that moment
   *
   * @param max cache size in MB
   */
  public static void setImageCache(int max) {
    ImageCache = max;
    if (max == 0) {
      changeCache(null, null, null);
    }
  }

  private static boolean isCaching() {
    return ImageCache > 0;
  }

  private synchronized static void changeCache(Boolean upOrDown, URL urlImg, Mat mat) {
    if (SX.isNotSet(upOrDown) || ImageCache < currentMemory + getMatSize(mat)) {
      for (URL url : imageFiles.keySet()) {
        imageFiles.put(url, new Mat());
      }
      currentMemory = 0;
      if (SX.isNotSet(upOrDown)) {
        return;
      }
    }
    if (upOrDown) {
      imageFiles.put(urlImg, mat);
      currentMemory += getMatSize(mat);
    } else {
      imageFiles.put(urlImg, new Mat());
      currentMemory -= getMatSize(mat);
      currentMemory = currentMemory < 0 ? 0 : currentMemory;
    }
  }

  private static long getMatSize(Mat mat) {
    return mat.channels() * mat.width() * mat.height();
  }
  //</editor-fold>

  //<editor-fold desc="*** path">
  private static final List<URL> imagePath = Collections.synchronizedList(new ArrayList<URL>());

  private static void initPath() {
    if (imagePath.isEmpty()) {
      imagePath.add(SX.getFileURL(SX.getSXIMAGES()));
    }
  }

  public static void clearPath() {
    imagePath.clear();
  }

  public static boolean setBundlePath(Object... args) {
    initPath();
    if (args.length == 0) {
      imagePath.set(0, SX.getFileURL(SX.getSXIMAGES()));
      return true;
    }
    URL urlPath = SX.makeURL(args);
    if (SX.isSet(urlPath)) {
      imagePath.set(0, urlPath);
      return true;
    }
    return false;
  }

  public static boolean resetPath(Object... args) {
    imagePath.clear();
    if (args.length == 0) {
      initPath();
      return true;
    }
    return setBundlePath(args);
  }

  public static String getBundlePath() {
    initPath();
    return SX.makePath(imagePath.get(0));
  }

  public static String[] getPath(String filter) {
    String[] sPaths = new String[imagePath.size()];
    int n = 0;
    String sPath;
    for (URL uPath : imagePath) {
      sPath = uPath.toString();
      if (SX.isSet(filter) && !sPath.contains(filter)) {
        continue;
      }
      sPaths[n++] = sPath;
    }
    return sPaths;
  }

  public static String[] getPath() {
    return getPath("");
  }

  public static int hasPath(String filter) {
    if (SX.isNotSet(filter)) return -1;
    int n = 0;
    String sPath;
    for (URL uPath : imagePath) {
      sPath = SX.makePath(uPath);
      if (sPath.contains(filter)) {
        return n;
      }
      n++;
    }
    return -1;
  }

  public static String getPath(int n) {
    if (n < 0 || n > imagePath.size() - 1) {
      n = 0;
    }
    return SX.makePath(imagePath.get(n));
  }

  public static boolean setPath(int n, String fpPath) {
    if (n < 0 || n > imagePath.size() - 1) {
      return false;
    }
    URL urlPath = SX.makeURL(fpPath);
    if (SX.isSet(urlPath)) {
      imagePath.set(n, urlPath);
      return true;
    }
    return false;
  }

  public static String removePath(int n) {
    if (n < 0 || n > imagePath.size() - 1) {
      n = 0;
    }
    return SX.makePath(imagePath.get(n));
  }

  public static int addPath(Object... args) {
    initPath();
    if (args.length == 0) {
      return -1;
    }
    URL urlPath = SX.makeURL(args);
    if (SX.isSet(urlPath)) {
      imagePath.add(urlPath);
      return imagePath.size() - 1;
    }
    return -1;
  }

  public static int removePath(Object... args) {
    //TODO implment removePath()
    URL url = null;
    return -1;
  }

  public static int insertPath(Object... args) {
    //TODO implment insertPath()
    URL url = null;
    return -1;
  }

  public static int changePath(Object... args) {
    //TODO implment changePath()
    URL url = null;
    return -1;
  }

  /**
   * try to find the given relative image file name on the image path<br>
   * starting from entry 0, the first found existence is taken<br>
   * absolute file names are checked for existence
   *
   * @param fname relative or absolute filename
   * @return a valid URL or null if not found/exists
   */
  public static URL searchOnImagePath(String fname) {
    fname = getValidName(fname);
    URL fURL = null;
    String proto = "";
    fname = Content.normalize(fname);
    if (new File(fname).isAbsolute()) {
      if (new File(fname).exists()) {
        fURL = Content.makeURL(fname);
      }
    } else {
      initPath();
      for (URL path : imagePath) {
        if (path == null) {
          continue;
        }
        proto = path.getProtocol();
        if ("file".equals(proto)) {
          fURL = Content.makeURL(path, fname);
          if (new File(fURL.getPath()).exists()) {
            break;
          }
        } else if ("jar".equals(proto) || proto.startsWith("http")) {
          fURL = Content.getURLForContentFromURL(path, fname);
          if (fURL != null) {
            break;
          }
        } else {
          log.error("find: URL not supported: " + path);
          return fURL;
        }
        fURL = null;
      }
    }
    if (fURL == null) {
      log.error("find: does not exist: " + fname);
    }
    return fURL;
  }

  /**
   * image file types supported by OpenCV highgui.imgread<br>
   * Windows bitmaps - *.bmp, *.dib (always supported) <br>
   * JPEG files - *.jpeg, *.jpg, *.jpe (see the *Notes* section)<br>
   * JPEG 2000 files - *.jp2 (see the *Notes* section) <br>
   * Portable Network Graphics - *.png (see the *Notes* section) <br>
   * Portable image format - *.pbm, *.pgm, *.ppm (always supported) <br>
   * Sun rasters - *.sr, *.ras (always supported) <br>
   * TIFF files - *.tiff, *.tif (see the *Notes* section)
   * @param name an image file name
   * @return the name optionally .png added if no ending
   */
  public static String getValidName(String name) {
    String validEndings = ".bmp.dib.jpeg.jpg.jpe.jp2.png.pbm.pgm.ppm.sr.ras.tiff.tif";
    String validName = name;
    String[] parts = validName.split("\\.");
    if (parts.length == 1) {
      log.trace("getValidName: supposing PNG: %s", name);
      validName += ".png";
    } else {
      String ending = "." + parts[parts.length - 1];
      if (validEndings.indexOf(ending) == -1) {
        log.error("getValidName: image file ending %s not supported: %s", ending, name);
      }
    }
    return validName;
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="*** helpers">
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
    resizeFactor = Math.min(((double) content.width()) / resizeMinDownSample,
            ((double) content.height()) / resizeMinDownSample);
    resizeFactor = Math.max(1.0, resizeFactor);

    MatOfDouble pMean = new MatOfDouble();
    MatOfDouble pStdDev = new MatOfDouble();
    Core.meanStdDev(content, pMean, pStdDev);
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
   *
   * @param factor resize factor
   * @return a new inMemory Image
   */
  public Image resize(float factor) {
    Image img = new Image();
    if (isValid()) {
      Mat newMat = new Mat();
      Size newS = new Size(w * factor, h * factor);
      Imgproc.resize(content, newMat, newS, 0, 0, Imgproc.INTER_AREA);
      img = new Image(newMat);
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
    if (isValid()) {
      img = new Image(content.submat(new Rect(x, y, w, h)));
    }
    return img;
  }

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

  public String save(String name) {
    String fpName = getValidImageFilename("_" + name);
    File fName = new File(getBundlePath(), fpName);
    Highgui.imwrite(fName.getAbsolutePath(), content);
    return fpName;
  }
//</editor-fold>
}
