/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.Content;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
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
    content = makeMat(bimg).submat(new Rect(rect.x, rect.y, rect.width, rect.height));
    init(0, 0, content.width(), content.height());
  }

  public Image(Mat mat) {
    this();
    if (SX.isNull(mat)) {
      mat = new Mat();
    }
    content = mat;
    init(0, 0, content.width(), content.height());
  }

  public Image(String fpImg) {
    this();
    setContent(fpImg);
    init(0, 0, content.width(), content.height());
  }

  public Image(URL url) {
    this();
    setContent(url);
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

  public String getURL() {
    return urlImg.toString();
  }
  //</editor-fold>

  //<editor-fold desc="*** get content">
  public Mat getContent() {
    return content;
  }

  private void setContent(String fpImg) {
    URL url = searchOnImagePath(fpImg);
    setContent(url);
  }

  private void setContent(URL url) {
    if (SX.isSet(url)) {
      urlImg = url;
      if (urlImg != null) {
        String urlProto = urlImg.getProtocol();
        if (urlProto.equals("file")) {
          File imgFile = new File(urlImg.getPath());
          content = Imgcodecs.imread(imgFile.getAbsolutePath());
        } else {
          try {
            content = makeMat(ImageIO.read(urlImg));
          } catch (IOException e) {
            log.error("load(): %s for %s", e.getMessage(), urlImg);
          }
        }
        if (isValid()) {
          setAttributes();
          log.debug("get: loaded: (%dx%s) %s", content.width(), content.height(), urlImg);
        } else {
          log.error("get: not loaded: %s", urlImg);
        }
      }
    }
  }

  public Image reset() {
    if (isValid()) {
      setContent(urlImg);
    }
    return this;
  }

  private final int resizeMinDownSample = 12;
  private double resizeFactor;
  private boolean plainColor = false;
  private boolean blackColor = false;
  private boolean whiteColor = false;
  private int[] meanColor = null;
  private double minThreshhold = 1.0E-5;

  public boolean isPlainColor() {
    return isValid() && plainColor;
  }

  public boolean isBlack() {
    return isValid() && blackColor;
  }

  public boolean isWhite() {
    return isValid() && blackColor;
  }

  public Color getMeanColor() {
    return new Color(meanColor[2], meanColor[1], meanColor[0]);
  }

  public boolean isMeanColorEqual(Color otherMeanColor) {
    Color col = getMeanColor();
    int r = (col.getRed() - otherMeanColor.getRed()) *  (col.getRed() - otherMeanColor.getRed());
    int g = (col.getGreen() - otherMeanColor.getGreen()) *  (col.getGreen() - otherMeanColor.getGreen());
    int b = (col.getBlue() - otherMeanColor.getBlue()) *  (col.getBlue() - otherMeanColor.getBlue());
    return Math.sqrt(r + g + b) < minThreshhold;
  }

  public double getResizeFactor() {
    return isValid() ? resizeFactor : 1;
  }

  private void setAttributes() {
    plainColor = false;
    blackColor = false;
    resizeFactor = Math.min(((double) content.width()) / resizeMinDownSample,
            ((double) content.height()) / resizeMinDownSample);
    resizeFactor = Math.max(1.0, resizeFactor);
    MatOfDouble pMean = new MatOfDouble();
    MatOfDouble pStdDev = new MatOfDouble();
    Core.meanStdDev(content, pMean, pStdDev);
    double sum = 0.0;
    double[] arr = pStdDev.toArray();
    for (int i = 0; i < arr.length; i++) {
      sum += arr[i];
    }
    if (sum < minThreshhold) {
      plainColor = true;
    }
    sum = 0.0;
    arr = pMean.toArray();
    meanColor = new int[arr.length];
    for (int i = 0; i < arr.length; i++) {
      meanColor[i] = (int) arr[i];
      sum += arr[i];
    }
    if (sum < minThreshhold && plainColor) {
      blackColor = true;
    }

    whiteColor = isMeanColorEqual(Color.WHITE);
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
        fURL = Content.makeURL(path, fname);
        if ("file".equals(proto)) {
          if (new File(fURL.getPath()).exists()) {
            break;
          }
        } else if ("jar".equals(proto) || proto.startsWith("http")) {
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

  //<editor-fold desc="*** content/show">
  final static String PNG = "png";
  final static String dotPNG = "." + PNG;

  protected static Mat makeMat(BufferedImage bImg) {
    Mat aMat = new Mat();
    if (bImg.getType() == BufferedImage.TYPE_INT_RGB) {
      log.trace("makeMat: INT_RGB (%dx%d)", bImg.getWidth(), bImg.getHeight());
      int[] data = ((DataBufferInt) bImg.getRaster().getDataBuffer()).getData();
      ByteBuffer byteBuffer = ByteBuffer.allocate(data.length * 4);
      IntBuffer intBuffer = byteBuffer.asIntBuffer();
      intBuffer.put(data);
      aMat = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC4);
      aMat.put(0, 0, byteBuffer.array());
      Mat oMatBGR = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC3);
      Mat oMatA = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC1);
      java.util.List<Mat> mixIn = new ArrayList<Mat>(Arrays.asList(new Mat[]{aMat}));
      java.util.List<Mat> mixOut = new ArrayList<Mat>(Arrays.asList(new Mat[]{oMatA, oMatBGR}));
      //A 0 - R 1 - G 2 - B 3 -> A 0 - B 1 - G 2 - R 3
      Core.mixChannels(mixIn, mixOut, new MatOfInt(0, 0, 1, 3, 2, 2, 3, 1));
      return oMatBGR;
    } else if (bImg.getType() == BufferedImage.TYPE_3BYTE_BGR) {
      log.error("makeMat: 3BYTE_BGR (%dx%d)", bImg.getWidth(), bImg.getHeight());
    } else if (bImg.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
      log.trace("makeMat: TYPE_4BYTE_ABGR (%dx%d)", bImg.getWidth(), bImg.getHeight());
      byte[] data = ((DataBufferByte) bImg.getRaster().getDataBuffer()).getData();
      aMat = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC4);
      aMat.put(0, 0, data);
      Mat oMatBGR = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC3);
      Mat oMatA = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC1);
      java.util.List<Mat> mixIn = new ArrayList<Mat>(Arrays.asList(new Mat[]{aMat}));
      java.util.List<Mat> mixOut = new ArrayList<Mat>(Arrays.asList(new Mat[]{oMatA, oMatBGR}));
      //A 0 - R 1 - G 2 - B 3 -> A 0 - B 1 - G 2 - R 3
      Core.mixChannels(mixIn, mixOut, new MatOfInt(0, 0, 1, 1, 2, 2, 3, 3));
      return oMatBGR;
    } else {
      log.error("makeMat: Type not supported: %d (%dx%d)",
              bImg.getType(), bImg.getWidth(), bImg.getHeight());
    }
    return aMat;
  }

  private static BufferedImage getBufferedImage(Mat mat, String type) {
    BufferedImage bImg = null;
    MatOfByte bytemat = new MatOfByte();
    if (SX.isNull(mat)) {
      mat = new Mat();
    }
    Imgcodecs.imencode(type, mat, bytemat);
    byte[] bytes = bytemat.toArray();
    InputStream in = new ByteArrayInputStream(bytes);
    try {
      bImg = ImageIO.read(in);
    } catch (IOException ex) {
      log.error("getBufferedImage: %s error(%s)", mat, ex.getMessage());
    }
    return bImg;
  }

  public void show() {
//    show((int) SX.getOptionNumber("DefaultHighlightTime"));
    show(3);
  }

  public void show(int time) {
    show(getBufferedImage(content, dotPNG), time);
  }

  private static void show(Mat mat, int time) {
    show(getBufferedImage(mat, dotPNG), time);
  }

  private static void show(BufferedImage bImg, int time) {
    JFrame frImg = new JFrame();
    frImg.setAlwaysOnTop(true);
    frImg.setResizable(false);
    frImg.setUndecorated(true);
    frImg.setLocation(100, 100);
    frImg.setSize(bImg.getWidth(), bImg.getHeight());
    Container cp = frImg.getContentPane();
    cp.add(new JLabel(new ImageIcon(bImg)), BorderLayout.CENTER);
    frImg.pack();
    frImg.setVisible(true);
    SX.pause(time);
    frImg.dispose();
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="*** helpers">
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
    Imgcodecs.imwrite(fName.getAbsolutePath(), content);
    return fpName;
  }
//</editor-fold>
}
