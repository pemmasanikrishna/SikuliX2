/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.sikulix.api.Picture;
//import org.sikuli.basics.PreferencesUser;
import com.sikulix.util.SplashFrame;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.Charset;
import java.security.CodeSource;
import java.util.*;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Content {

  private static SXLog log = SX.getLogger("SX.Content");
  private static int lvl = SXLog.DEBUG;

  public static void start() {
    log.trace("!start: class init");
  }

  public static final int DOWNLOAD_BUFFER_SIZE = 153600;
  private static SplashFrame _progress = null;
  private static final String EXECUTABLE = "#executable";

  public static int tryGetFileSize(URL aUrl) {
    HttpURLConnection conn = null;
    try {
      if (getProxy() != null) {
        conn = (HttpURLConnection) aUrl.openConnection(getProxy());
      } else {
        conn = (HttpURLConnection) aUrl.openConnection();
      }
      conn.setConnectTimeout(30000);
      conn.setReadTimeout(30000);
      conn.setRequestMethod("HEAD");
      conn.getInputStream();
      return conn.getContentLength();
    } catch (Exception ex) {
      return 0;
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
  }

	public static int isUrlUseabel(String sURL) {
		try {
			return isUrlUseabel(new URL(sURL));
		} catch (Exception ex) {
			return -1;
		}
	}
	
	public static int isUrlUseabel(URL aURL) {
    HttpURLConnection conn = null;
		try {
//			HttpURLConnection.setFollowRedirects(false);
	    if (getProxy() != null) {
    		conn = (HttpURLConnection) aURL.openConnection(getProxy());
      } else {
    		conn = (HttpURLConnection) aURL.openConnection();
      }
//			con.setInstanceFollowRedirects(false);
			conn.setRequestMethod("HEAD");
			int retval = conn.getResponseCode();
//				HttpURLConnection.HTTP_BAD_METHOD 405
//				HttpURLConnection.HTTP_NOT_FOUND 404
			if (retval == HttpURLConnection.HTTP_OK) {
				return 1;
			} else if (retval == HttpURLConnection.HTTP_NOT_FOUND) {
				return 0;
			} else if (retval == HttpURLConnection.HTTP_FORBIDDEN) {
				return 0;
			} else {
				return -1;
			}
		} catch (Exception ex) {
			return -1;
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
	}

  // ******************************* Proxy ****************************

  static String proxyName = "";
  static String proxyIP = "";
  static InetAddress proxyAddress = null;
  static String proxyPort = "";
  static boolean proxyChecked = false;
  static Proxy sxProxy = null;

  public static Proxy getProxy() {
    Proxy proxy = sxProxy;
    if (!proxyChecked) {
      String phost = proxyName;
      String padr = proxyIP;
      String pport = proxyPort;
      InetAddress a = null;
      int p = -1;
      if (phost != null) {
        a = getProxyAddress(phost);
      }
      if (a == null && padr != null) {
        a = getProxyAddress(padr);
      }
      if (a != null && pport != null) {
        p = getProxyPort(pport);
      }
      if (a != null && p > 1024) {
        proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(a, p));
        log.debug("Proxy defined: %s : %d", a.getHostAddress(), p);
      }
      proxyChecked = true;
      sxProxy = proxy;
    }
    return proxy;
  }

  public static boolean setProxy(String pName, String pPort) {
    InetAddress a = null;
    String host = null;
    String adr = null;
    int p = -1;
    if (pName != null) {
      a = getProxyAddress(pName);
      if (a == null) {
        a = getProxyAddress(pName);
        if (a != null) {
          adr = pName;
        }
      } else {
        host = pName;
      }
    }
    if (a != null && pPort != null) {
      p = getProxyPort(pPort);
    }
    if (a != null && p > 1024) {
      log.debug("Proxy stored: %s : %d", a.getHostAddress(), p);
      proxyChecked = true;
      proxyName = host;
      proxyIP = adr;
      proxyPort = pPort;
      sxProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(a, p));
////TODO options
//      PreferencesUser prefs = PreferencesUser.getInstance();
//      prefs.put("ProxyName", (host == null ? "" : host));
//      prefs.put("ProxyIP", (adr == null ? "" : adr));
//      prefs.put("ProxyPort", ""+p);
      return true;
    }
    return false;
  }

  /**
   * download a file at the given url to a local folder
   *
   * @param url a valid url
   * @param localPath the folder where the file should go (will be created if necessary)
   * @return the absolute path to the downloaded file or null on any error
   */
  public static String downloadURL(URL url, String localPath) {
    String[] path = url.getPath().split("/");
    String filename = path[path.length - 1];
    String targetPath = null;
    int srcLength = 1;
    int srcLengthKB = 0;
    int done;
    int totalBytesRead = 0;
    File fullpath = new File(localPath);
    if (fullpath.exists()) {
      if (fullpath.isFile()) {
        log.error("download: target path must be a folder:\n%s", localPath);
        fullpath = null;
      }
    } else {
      if (!fullpath.mkdirs()) {
        log.error("download: could not create target folder:\n%s", localPath);
        fullpath = null;
      }
    }
    if (fullpath != null) {
      srcLength = tryGetFileSize(url);
      srcLengthKB = (int) (srcLength / 1024);
      if (srcLength > 0) {
        log.debug("Downloading %s having %d KB", filename, srcLengthKB);
			} else {
        log.debug("Downloading %s with unknown size", filename);
			}
			fullpath = new File(localPath, filename);
			targetPath = fullpath.getAbsolutePath();
			done = 0;
			if (_progress != null) {
				_progress.setProFile(filename);
				_progress.setProSize(srcLengthKB);
				_progress.setProDone(0);
				_progress.setVisible(true);
			}
			InputStream reader = null;
      FileOutputStream writer = null;
			try {
				writer = new FileOutputStream(fullpath);
				if (getProxy() != null) {
					reader = url.openConnection(getProxy()).getInputStream();
				} else {
					reader = url.openConnection().getInputStream();
				}
				byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
				int bytesRead = 0;
				long begin_t = (new Date()).getTime();
				long chunk = (new Date()).getTime();
				while ((bytesRead = reader.read(buffer)) > 0) {
					writer.write(buffer, 0, bytesRead);
					totalBytesRead += bytesRead;
					if (srcLength > 0) {
						done = (int) ((totalBytesRead / (double) srcLength) * 100);
					} else {
						done = (int) (totalBytesRead / 1024);
					}
					if (((new Date()).getTime() - chunk) > 1000) {
						if (_progress != null) {
							_progress.setProDone(done);
						}
						chunk = (new Date()).getTime();
					}
				}
				writer.close();
				log.debug("downloaded %d KB to:\n%s", (int) (totalBytesRead / 1024), targetPath);
				log.debug("download time: %d", (int) (((new Date()).getTime() - begin_t) / 1000));
			} catch (Exception ex) {
				log.error("problems while downloading\n%s", ex);
				targetPath = null;
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException ex) {
					}
				}
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException ex) {
					}
				}
			}
      if (_progress != null) {
        if (targetPath == null) {
          _progress.setProDone(-1);
        } else {
          if (srcLength <= 0) {
            _progress.setProSize((int) (totalBytesRead / 1024));
          }
          _progress.setProDone(100);
        }
        _progress.closeAfter(3);
        _progress = null;
      }
    }
    if (targetPath == null) {
      fullpath.delete();
    }
    return targetPath;
  }

  /**
   * download a file at the given url to a local folder
   *
   * @param url a string representing a valid url
   * @param localPath the folder where the file should go (will be created if necessary)
   * @return the absolute path to the downloaded file or null on any error
   */
  public static String downloadURL(String url, String localPath) {
    URL urlSrc = null;
    try {
      urlSrc = new URL(url);
    } catch (MalformedURLException ex) {
      log.error("download: bad URL: " + url);
      return null;
    }
    return downloadURL(urlSrc, localPath);
  }

  public static String downloadURL(String url, String localPath, JFrame progress) {
    _progress = (SplashFrame) progress;
    return downloadURL(url, localPath);
  }

  public static String downloadURLtoString(String src) {
    URL url = null;
    try {
      url = new URL(src);
    } catch (MalformedURLException ex) {
      log.error("download to string: bad URL:\n%s", src);
      return null;
    }
    return downloadURLtoString(url);
  }

  public static String downloadURLtoString(URL uSrc) {
    String content = "";
    InputStream reader = null;
    log.debug("download to string from:\n%s,", uSrc);
    try {
      if (getProxy() != null) {
        reader = uSrc.openConnection(getProxy()).getInputStream();
      } else {
        reader = uSrc.openConnection().getInputStream();
      }
      byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
      int bytesRead = 0;
      while ((bytesRead = reader.read(buffer)) > 0) {
        content += (new String(Arrays.copyOfRange(buffer, 0, bytesRead), Charset.forName("utf-8")));
      }
    } catch (Exception ex) {
      log.error("problems while downloading\n" + ex.getMessage());
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException ex) {
        }
      }
    }
    return content;
  }

  public static String downloadScriptToString(URL url) {
    return downloadFileToString(url, true);
  }

  public static String downloadFileToString(URL url) {
    return downloadFileToString(url, true);
  }

  public static String downloadFileToString(URL url, boolean silent) {
    HttpURLConnection httpConn = null;
    String content = "";
    try {
      httpConn = (HttpURLConnection) url.openConnection();
      if (httpConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
        InputStream inputStream = httpConn.getInputStream();
        int bytesRead = -1;
        byte[] buffer = new byte[Content.DOWNLOAD_BUFFER_SIZE];
        while ((bytesRead = inputStream.read(buffer)) != -1) {
          content += (new String(Arrays.copyOfRange(buffer, 0, bytesRead), Charset.forName("utf-8")));
        }
        inputStream.close();
        log.trace("downloadFileToString: %s (%s)", url, httpConn.getContentType());
      } else {
        if (silent) {
          log.trace("downloadFileToString: (HTTP:%d) %s", httpConn.getResponseCode(), url);
        } else {
          log.error("downloadFileToString: (HTTP:%d) %s", httpConn.getResponseCode(), url);
        }
      }
      httpConn.disconnect();
    } catch (IOException e) {
      if (silent) {
        log.trace("downloadFileToString: (%s) %s", e.getMessage(), url);
      } else {
        log.error("downloadFileToString: (%s) %s", e.getMessage(), url);
      }
    }
    return content;
  }

  /**
   * open the given url in the standard browser
   *
   * @param url string representing a valid url
   * @return false on error, true otherwise
   */
  public static boolean openURL(String url) {
    try {
      URL u = new URL(url);
      Desktop.getDesktop().browse(u.toURI());
    } catch (Exception ex) {
      log.error("show in browser: bad URL: " + url);
      return false;
    }
    return true;
  }

  public static File createTempDir(String path) {
    File fTempDir = new File(SX.getTEMP(), path);
    log.debug("createTempDir:\n%s", fTempDir);
    if (!fTempDir.exists()) {
      fTempDir.mkdirs();
    } else {
      Content.resetFolder(fTempDir);
    }
    if (!fTempDir.exists()) {
      log.error("createTempDir: not possible: %s", fTempDir);
      return null;
    }
    return fTempDir;
  }

  public static File createTempDir() {
    File fTempDir = createTempDir("tmp-" + SX.getRandomInt() + ".sikuli");
    if (null != fTempDir) {
      fTempDir.deleteOnExit();
    }
    return fTempDir;
  }

  public static void deleteTempDir(String path) {
    if (!deleteFileOrFolder(path)) {
      log.error("deleteTempDir: not possible");
    }
  }

  public static boolean deleteFileOrFolder(File fPath, FileFilter filter) {
    return doDeleteFileOrFolder(fPath, filter);
	}

  public static boolean deleteFileOrFolder(File fPath) {
    return doDeleteFileOrFolder(fPath, null);
	}

  public static boolean deleteFileOrFolder(String fpPath, FileFilter filter) {
    if (fpPath.startsWith("#")) {
      fpPath = fpPath.substring(1);
    } else {
  		log.debug("deleteFileOrFolder: %s\n%s", (filter == null ? "" : "filtered: "), fpPath);
    }
    return doDeleteFileOrFolder(new File(fpPath), filter);
	}

  public static boolean deleteFileOrFolder(String fpPath) {
    if (fpPath.startsWith("#")) {
      fpPath = fpPath.substring(1);
    } else {
  		log.debug("deleteFileOrFolder:\n%s", fpPath);
    }
    return doDeleteFileOrFolder(new File(fpPath), null);
  }

  public static void resetFolder(File fPath) {
		log.debug("resetFolder:\n%s", fPath);
    doDeleteFileOrFolder(fPath, null);
    fPath.mkdirs();
  }

  private static boolean doDeleteFileOrFolder(File fPath, FileFilter filter) {
    if (fPath == null) {
      return false;
    }
    File aFile;
    String[] entries;
    boolean somethingLeft = false;
    if (fPath.exists() && fPath.isDirectory()) {
      entries = fPath.list();
      for (int i = 0; i < entries.length; i++) {
        aFile = new File(fPath, entries[i]);
        if (filter != null && !filter.accept(aFile)) {
          somethingLeft = true;
          continue;
        }
        if (aFile.isDirectory()) {
          if (!doDeleteFileOrFolder(aFile, filter)) {
            return false;
          }
        } else {
          try {
            aFile.delete();
          } catch (Exception ex) {
            log.error("deleteFile: not deleted:\n%s\n%s", aFile, ex);
            return false;
          }
        }
      }
    }
    // deletes intermediate empty directories and finally the top now empty dir
    if (!somethingLeft && fPath.exists()) {
      try {
        fPath.delete();
      } catch (Exception ex) {
        log.error("deleteFolder: not deleted:\n" + fPath.getAbsolutePath() + "\n" + ex.getMessage());
        return false;
      }
    }
    return true;
  }

  public static void traverseFolder(File fPath, FileFilter filter) {
    if (fPath == null) {
      return;
    }
    File aFile;
    String[] entries;
    if (fPath.isDirectory()) {
      entries = fPath.list();
      for (int i = 0; i < entries.length; i++) {
        aFile = new File(fPath, entries[i]);
        if (filter != null) {
          filter.accept(aFile);
        }
        if (aFile.isDirectory()) {
          traverseFolder(aFile, filter);
        }
      }
    }
  }
  
  public static File createTempFile(String suffix) {
    return createTempFile(suffix, null);
  }

  public static File createTempFile(String suffix, String path) {
    String temp1 = "sikuli-";
    String temp2 = "." + suffix;
    File fpath = new File(SX.getTEMP());
    if (path != null) {
      fpath = new File(path);
    }
    try {
      fpath.mkdirs();
      File temp = File.createTempFile(temp1, temp2, fpath);
      temp.deleteOnExit();
      String fpTemp = temp.getAbsolutePath();
      if (!fpTemp.endsWith(".script")) {
        log.debug("tempfile create:\n%s", temp.getAbsolutePath());
      }
      return temp;
    } catch (IOException ex) {
      log.error("createTempFile: IOException: %s\n%s", ex.getMessage(),
              fpath + File.separator + temp1 + "12....56" + temp2);
      return null;
    }
  }

  public static String saveTmpImage(BufferedImage img) {
    return saveTmpImage(img, null, "png");
  }

  public static String saveTmpImage(BufferedImage img, String typ) {
    return saveTmpImage(img, null, typ);
  }

  public static String saveTmpImage(BufferedImage img, String path, String typ) {
    File tempFile;
    boolean success;
    try {
      tempFile = createTempFile(typ, path);
      if (tempFile != null) {
        success = ImageIO.write(img, typ, tempFile);
        if (success) {
          return tempFile.getAbsolutePath();
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
  
  public static String saveTimedImage(BufferedImage img) {
    return saveTimedImage(img, Picture.getBundlePath(), null);
  }

  public static String saveTimedImage(BufferedImage img, String path) {
    return saveTimedImage(img, path, null);
  }
  
  public static String saveTimedImage(BufferedImage img, String path, String name) {
    SX.pause(0.01f);
    File fImage = new File(path, String.format("%s-%d.png", name, new Date().getTime()));
    try {
      ImageIO.write(img, "png", fImage);
    } catch (Exception ex) {
      return "";
    }
    return fImage.getAbsolutePath();
  }

  public static boolean unzip(String inpZip, String target) {
    return unzip(new File(inpZip), new File(target));
  }

  public static boolean unzip(File fZip, File fTarget) {
    String fpZip = null;
    String fpTarget = null;
    log.debug("unzip: from: %s\nto: %s", fZip, fTarget);
    try {
      fpZip = fZip.getCanonicalPath();
      if (!new File(fpZip).exists()) {
        throw new IOException();
      }
    } catch (IOException ex) {
      log.error("unzip: source not found:\n%s\n%s", fpZip, ex);
      return false;
    }
    try {
      fpTarget = fTarget.getCanonicalPath();
      deleteFileOrFolder(fpTarget);
      new File(fpTarget).mkdirs();
      if (!new File(fpTarget).exists()) {
        throw new IOException();
      }
    } catch (IOException ex) {
      log.error("unzip: target cannot be created:\n%s\n%s", fpTarget, ex);
      return false;
    }
    ZipInputStream inpZip = null;
    ZipEntry entry = null;
    try {
      final int BUF_SIZE = 2048;
      inpZip = new ZipInputStream(new BufferedInputStream(new FileInputStream(fZip)));
      while ((entry = inpZip.getNextEntry()) != null) {
        if (entry.getName().endsWith("/") || entry.getName().endsWith("\\")) {
          new File(fpTarget, entry.getName()).mkdir();
          continue;
        }
        int count;
        byte data[] = new byte[BUF_SIZE];
        File outFile = new File(fpTarget, entry.getName());
        File outFileParent = outFile.getParentFile();
        if (! outFileParent.exists()) {
          outFileParent.mkdirs();
        }
        FileOutputStream fos = new FileOutputStream(outFile);
        BufferedOutputStream dest = new BufferedOutputStream(fos, BUF_SIZE);
        while ((count = inpZip.read(data, 0, BUF_SIZE)) != -1) {
          dest.write(data, 0, count);
        }
        dest.close();
      }
    } catch (Exception ex) {
      log.error("unzip: not possible: source:\n%s\ntarget:\n%s\n(%s)%s",
          fpZip, fpTarget, entry.getName(), ex);
      return false;
    } finally {
      try {      
        inpZip.close();
      } catch (IOException ex) {
        log.error("unzip: closing source:\n%s\n%s", fpZip, ex);
      }
    }
    return true;
  }

  public static boolean xcopy(File fSrc, File fDest) {
    if (fSrc == null || fDest == null) {
      return false;
    }
    try {
      doXcopy(fSrc, fDest, null);
    } catch (Exception ex) {
      log.debug("xcopy from: %s\nto: %s\n%s", fSrc, fDest, ex);
      return false;
    }
    return true;
	}

  public static boolean xcopy(File fSrc, File fDest, FileFilter filter) {
    if (fSrc == null || fDest == null) {
      return false;
    }
    try {
      doXcopy(fSrc, fDest, filter);
    } catch (Exception ex) {
      log.debug("xcopy from: %s\nto: %s\n%s", fSrc, fDest, ex);
      return false;
    }
    return true;
	}

  public static void xcopy(String src, String dest) throws IOException {
		doXcopy(new File(src), new File(dest), null);
	}

  public static void xcopy(String src, String dest, FileFilter filter) throws IOException {
		doXcopy(new File(src), new File(dest), filter);
	}

  private static void doXcopy(File fSrc, File fDest, FileFilter filter) throws IOException {
    if (fSrc.getAbsolutePath().equals(fDest.getAbsolutePath())) {
      return;
    }
    if (fSrc.isDirectory()) {
			if (filter == null || filter.accept(fSrc)) {
				if (!fDest.exists()) {
					fDest.mkdirs();
				}
				String[] children = fSrc.list();
				for (String child : children) {
					if (child.equals(fDest.getName())) {
						continue;
					}
					doXcopy(new File(fSrc, child), new File(fDest, child), filter);

				}
			}
		} else {
			if (filter == null || filter.accept(fSrc)) {
				if (fDest.isDirectory()) {
					fDest = new File(fDest, fSrc.getName());
				}
				InputStream in = new FileInputStream(fSrc);
				OutputStream out = new FileOutputStream(fDest);
				// Copy the bits from instream to outstream
				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				in.close();
				out.close();
			}
		}
  }

  private static String makeFileListString;
  private static String makeFileListPrefix;

  public static String makeFileList(File path, String prefix) {
    makeFileListPrefix = prefix;
    return makeFileListDo(path, true);
  }

  private static String makeFileListDo(File path, boolean starting) {
    String x;
    if (starting) {
      makeFileListString = "";
    }
    if (!path.exists()) {
      return makeFileListString;
    }
    if (path.isDirectory()) {
      String[] fcl = path.list();
      for (String fc : fcl) {
        makeFileListDo(new File(path, fc), false);
      }
    } else {
      x = path.getAbsolutePath();
      if (!makeFileListPrefix.isEmpty()) {
        x = x.replace(makeFileListPrefix, "").replace("\\", "/");
        if (x.startsWith("/")) {
          x = x.substring(1);
        }
      }
      makeFileListString += x + "\n";
    }
    return makeFileListString;
  }

  /**
   * Copy a file *src* to the path *dest* and check if the file name conflicts. If a file with the
   * same name exists in that path, rename *src* to an alternative name.
	 * @param src source file
	 * @param dest destination path
	 * @return the destination file if ok, null otherwise
	 * @throws IOException on failure
   */
  public static File smartCopy(String src, String dest) throws IOException {
    File fSrc = new File(src);
    String newName = fSrc.getName();
    File fDest = new File(dest, newName);
    if (fSrc.equals(fDest)) {
      return fDest;
    }
    while (fDest.exists()) {
      newName = getAltFilename(newName);
      fDest = new File(dest, newName);
    }
    xcopy(src, fDest.getAbsolutePath());
    if (fDest.exists()) {
      return fDest;
    }
    return null;
  }

  public static String convertStreamToString(InputStream is) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    StringBuilder sb = new StringBuilder();
    String line;
    try {
      while ((line = reader.readLine()) != null) {
        sb.append(line).append("\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        is.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return sb.toString();
  }

  public static String getAltFilename(String filename) {
    int pDot = filename.lastIndexOf('.');
    int pDash = filename.lastIndexOf('-');
    int ver = 1;
    String postfix = filename.substring(pDot);
    String name;
    if (pDash >= 0) {
      name = filename.substring(0, pDash);
      ver = Integer.parseInt(filename.substring(pDash + 1, pDot));
      ver++;
    } else {
      name = filename.substring(0, pDot);
    }
    return name + "-" + ver + postfix;
  }

  public static boolean exists(String path) {
    File f = new File(path);
    return f.exists();
  }

  public static void mkdir(String path) {
    File f = new File(path);
    if (!f.exists()) {
      f.mkdirs();
    }
  }

  public static String getName(String filename) {
    File f = new File(filename);
    return f.getName();
  }

  public static String slashify(String path, Boolean isDirectory) {
    if (path != null) {
      if (path.contains("%")) {
        try {
          path = URLDecoder.decode(path, "UTF-8");
        } catch (Exception ex) {
					log.debug("slashify: decoding problem with %s\nwarning: filename might not be useable.", path);
        }
      }
      if (File.separatorChar != '/') {
        path = path.replace(File.separatorChar, '/');
      }
      if (isDirectory != null) {
        if (isDirectory) {
          if (!path.endsWith("/")) {
            path = path + "/";
          }
        } else if (path.endsWith("/")) {
          path = path.substring(0, path.length() - 1);
        }
      }
			if (path.startsWith("./")) {
				path = path.substring(2);
			}
      return path;
    } else {
      return "";
    }
  }

	public static String normalize(String filename) {
		return slashify(filename, false);
	}

	public static String normalizeAbsolute(String filename, boolean withTrailingSlash) {
    filename = slashify(filename, false);
    String jarSuffix = "";
    int nJarSuffix;
    if (-1 < (nJarSuffix = filename.indexOf(".jar!/"))) {
      jarSuffix = filename.substring(nJarSuffix + 4);
      filename = filename.substring(0, nJarSuffix + 4);
    }
    File aFile = new File(filename);
    try {
      filename = aFile.getCanonicalPath();
      aFile = new File(filename);
    } catch (Exception ex) {
    }
    String fpFile = aFile.getAbsolutePath();
    if (!fpFile.startsWith("/")) {
      fpFile = "/" + fpFile;
    }
		return slashify(fpFile + jarSuffix, withTrailingSlash);
	}

	public static boolean isFilenameDotted(String name) {
		String nameParent = new File(name).getParent();
		if (nameParent != null && nameParent.contains(".")) {
			return true;
		}
		return false;
	}

  /**
   * Returns the directory that contains the images used by the ScriptRunner.
   *
   * @param scriptFile The file containing the script.
   * @return The directory containing the images.
   */
  public static File resolveImagePath(File scriptFile) {
    if (!scriptFile.isDirectory()) {
      return scriptFile.getParentFile();
    }
    return scriptFile;
  }

  public static URL makeURL(String fName) {
    return makeURL(fName, "file");
  }

  public static URL makeURL(String fName, String type) {
    try {
      if ("file".equals(type)) {
        fName = normalizeAbsolute(fName, false);
        if (!fName.startsWith("/")) {
          fName = "/" + fName;
        }
      }
			if ("jar".equals(type)) {
				if (!fName.contains("!/")) {
					fName += "!/";
				}
				return new URL("jar:" + fName);
			} else if ("file".equals(type)) {
        File aFile = new File(fName);
        if (aFile.exists() && aFile.isDirectory()) {
          if (!fName.endsWith("/")) {
            fName += "/";
          }
        }
      }
      return new URL(type, null, fName);
    } catch (MalformedURLException ex) {
      return null;
    }
  }

  public static URL makeURL(URL path, String fName) {
    URL url = null;
    fName = slashify(fName, false);
    if (fName.startsWith("/")) {
      fName = fName.substring(1);
    }
    try {
			if ("file".equals(path.getProtocol())) {
				url = new URL("file:" + new File(path.getFile(), fName).getAbsolutePath());
			} else if ("jar".equals(path.getProtocol())) {
				String jp = path.getPath();
				if (!jp.endsWith("/")) {
          jp += "/";
        }
				url = new URL("jar:" + jp + fName);
			} else if (path.getProtocol().startsWith("http")) {
			  url = new URL(path.toString() + "/" + fName);
      }
    } catch (MalformedURLException ex) {
      log.error("makeURL: %s for %s + %s", ex.getMessage(), path, fName);
    }
    return url;
  }

  public static URL getURLForContentFromURL(URL uRes, String fName) {
    URL aURL = null;
    if ("jar".equals(uRes.getProtocol())) {
      return makeURL(uRes, fName);
    } else if ("file".equals(uRes.getProtocol())) {
      aURL = makeURL(new File(slashify(uRes.getPath(), false), slashify(fName, false)).getPath(), uRes.getProtocol());
    } else if (uRes.getProtocol().startsWith("http")) {
      String sRes = uRes.toString();
			if (!sRes.endsWith("/")) {
				sRes += "/";
			}
			try {
				aURL = new URL(sRes + fName);
				if (1 == isUrlUseabel(aURL)) {
					return aURL;
				} else {
					return null;
				}
			} catch (MalformedURLException ex) {
				return null;
			}
    }
    try {
      if (aURL != null) {
        aURL.getContent();
        return aURL;
      }
    } catch (IOException ex) {
      return null;
    }
    return aURL;
  }

	public static boolean checkJarContent(String jarPath, String jarContent) {
		URL jpu = makeURL(jarPath, "jar");
		if (jpu != null && jarContent != null) {
			jpu = makeURL(jpu, jarContent);
		}
		if (jpu != null) {
			try {
			  jpu.getContent();
				return true;
			} catch (IOException ex) {
        ex.getMessage();
			}
		}
		return false;
	}

  public static int getPort(String p) {
    int port;
    int pDefault = 50000;
    if (p != null) {
      try {
        port = Integer.parseInt(p);
      } catch (NumberFormatException ex) {
        return -1;
      }
    } else {
      return pDefault;
    }
    if (port < 1024) {
      port += pDefault;
    }
    return port;
  }

  public static int getProxyPort(String p) {
    int port;
    int pDefault = 8080;
    if (p != null) {
      try {
        port = Integer.parseInt(p);
      } catch (NumberFormatException ex) {
        return -1;
      }
    } else {
      return pDefault;
    }
    return port;
  }

  public static String getAddress(String arg) {
    try {
      if (arg == null) {
        return InetAddress.getLocalHost().getHostAddress();
      }
      return InetAddress.getByName(arg).getHostAddress();
    } catch (UnknownHostException ex) {
      return null;
    }
  }

  public static InetAddress getProxyAddress(String arg) {
    try {
      return InetAddress.getByName(arg);
    } catch (UnknownHostException ex) {
      return null;
    }
  }

  public static void zip(String path, String outZip) throws IOException, FileNotFoundException {
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outZip));
    zipDir(path, zos);
    zos.close();
  }

  private static void zipDir(String dir, ZipOutputStream zos) throws IOException {
    File zipDir = new File(dir);
    String[] dirList = zipDir.list();
    byte[] readBuffer = new byte[1024];
    int bytesIn;
    for (int i = 0; i < dirList.length; i++) {
      File f = new File(zipDir, dirList[i]);
      if (f.isFile()) {
        FileInputStream fis = new FileInputStream(f);
        ZipEntry anEntry = new ZipEntry(f.getName());
        zos.putNextEntry(anEntry);
        while ((bytesIn = fis.read(readBuffer)) != -1) {
          zos.write(readBuffer, 0, bytesIn);
        }
        fis.close();
      }
    }
  }

	public static void deleteNotUsedImages(String bundle, Set<String> usedImages) {
		File scriptFolder = new File(bundle);
		if (!scriptFolder.isDirectory()) {
			return;
		}
		String path;
		for (File image : scriptFolder.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						if ((name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg"))) {
							if (!name.startsWith("_")) {
                return true;
              }
						}
						return false;
					}
				})) {
			if (!usedImages.contains(image.getName())) {
				log.debug("Content: delete not used: %s", image.getName());
				image.delete();
			}
		}
	}

	public static boolean isBundle(String dir) {
		return dir.endsWith(".sikuli");
	}

//  public static IResourceLoader getNativeLoader(String name, String[] args) {
//    if (nativeLoader != null) {
//      return nativeLoader;
//    }
//    IResourceLoader nl = null;
//    ServiceLoader<IResourceLoader> loader = ServiceLoader.load(IResourceLoader.class);
//    Iterator<IResourceLoader> resourceLoaderIterator = loader.iterator();
//    while (resourceLoaderIterator.hasNext()) {
//      IResourceLoader currentLoader = resourceLoaderIterator.next();
//      if ((name != null && currentLoader.getName().toLowerCase().equals(name.toLowerCase()))) {
//        nl = currentLoader;
//        nl.init(args);
//        break;
//      }
//    }
//    if (nl == null) {
//      log0(-1, "Fatal error 121: Could not load any NativeLoader!");
//      (121);
//    } else {
//      nativeLoader = nl;
//    }
//    return nativeLoader;
//  }
//
  public static String getJarParentFolder() {
    CodeSource src = Content.class.getProtectionDomain().getCodeSource();
    String jarParentPath = "--- not known ---";
    String RunningFromJar = "Y";
    if (src.getLocation() != null) {
      String jarPath = src.getLocation().getPath();
      if (!jarPath.endsWith(".jar")) RunningFromJar = "N";
      jarParentPath = Content.slashify((new File(jarPath)).getParent(), true);
    } else {
      SX.terminate(101, "Fatal Error 101: Not possible to access the jar files!");
    }
    return RunningFromJar + jarParentPath;
  }

  public static String getJarPath(Class cname) {
    CodeSource src = cname.getProtectionDomain().getCodeSource();
    if (src.getLocation() != null) {
      return new File(src.getLocation().getPath()).getAbsolutePath();
    }
    return "";
  }

  public static String getJarName(Class cname) {
		String jp = getJarPath(cname);
		if (jp.isEmpty()) {
			return "";
		}
		return new File(jp).getName();
  }

  public static boolean writeStringToFile(String text, String path) {
    return writeStringToFile(text, new File(path));
  }

  public static boolean writeStringToFile(String text, File fPath) {
    PrintStream out = null;
    try {
      out = new PrintStream(new FileOutputStream(fPath));
      out.print(text);
    } catch (Exception e) {
      log.error("writeStringToFile: did not work: " + fPath + "\n" + e.getMessage());
    }
    if (out != null) {
      out.close();
      return true;
    }
    return false;
  }

  public static String readFileToString(File fPath) {
    try {
      return doRreadFileToString(fPath);
    } catch (Exception ex) {
      return "";
    }
  }

  private static String doRreadFileToString(File fPath) throws IOException {
    StringBuilder result = new StringBuilder();
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(fPath));
      char[] buf = new char[1024];
      int r = 0;
      while ((r = reader.read(buf)) != -1) {
        result.append(buf, 0, r);
      }
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
    return result.toString();
  }

  public static boolean packJar(String folderName, String jarName, String prefix) {
    jarName = Content.slashify(jarName, false);
    if (!jarName.endsWith(".jar")) {
      jarName += ".jar";
    }
    folderName = Content.slashify(folderName, true);
    if (!(new File(folderName)).isDirectory()) {
      log.error("packJar: not a directory or does not exist: " + folderName);
      return false;
    }
    try {
      File dir = new File((new File(jarName)).getAbsolutePath()).getParentFile();
      if (dir != null) {
        if (!dir.exists()) {
          dir.mkdirs();
        }
      } else {
        throw new Exception("workdir is null");
      }
      log.debug("packJar: %s from %s in workDir %s", jarName, folderName, dir.getAbsolutePath());
      if (!folderName.startsWith("http://") && !folderName.startsWith("https://")) {
        folderName = "file://" + (new File(folderName)).getAbsolutePath();
      }
      URL src = new URL(folderName);
      JarOutputStream jout = new JarOutputStream(new FileOutputStream(jarName));
      addToJar(jout, new File(src.getFile()), prefix);
      jout.close();
    } catch (Exception ex) {
      log.error("packJar: " + ex.getMessage());
      return false;
    }
    log.debug("packJar: completed");
    return true;
  }

  public static boolean buildJar(String targetJar, String[] jars,
          String[] files, String[] prefixs, Content.JarFileFilter filter) {
    boolean logShort = false;
    if (targetJar.startsWith("#")) {
      logShort = true;
      targetJar = targetJar.substring(1);
      log.debug("buildJar: %s", new File(targetJar).getName());
    } else {
      log.debug("buildJar:\n%s", targetJar);
    }
    try {
      JarOutputStream jout = new JarOutputStream(new FileOutputStream(targetJar));
      ArrayList done = new ArrayList();
      for (int i = 0; i < jars.length; i++) {
        if (jars[i] == null) {
          continue;
        }
        if (logShort) {
          log.debug("buildJar: adding: %s", new File(jars[i]).getName());
        } else {
          log.debug("buildJar: adding:\n%s", jars[i]);
        }
        BufferedInputStream bin = new BufferedInputStream(new FileInputStream(jars[i]));
        ZipInputStream zin = new ZipInputStream(bin);
        for (ZipEntry zipentry = zin.getNextEntry(); zipentry != null; zipentry = zin.getNextEntry()) {
          if (filter == null || filter.accept(zipentry, jars[i])) {
            if (!done.contains(zipentry.getName())) {
              jout.putNextEntry(zipentry);
              if (!zipentry.isDirectory()) {
                bufferedWrite(zin, jout);
              }
              done.add(zipentry.getName());
              log.trace("adding: %s", zipentry.getName());
            }
          }
        }
        zin.close();
        bin.close();
      }
      if (files != null) {
        for (int i = 0; i < files.length; i++) {
					if (files[i] == null) {
						continue;
					}
          if (logShort) {
            log.debug("buildJar: adding %s at %s", new File(files[i]).getName(), prefixs[i]);
          } else {
            log.debug("buildJar: adding %s at %s", files[i], prefixs[i]);
          }
         addToJar(jout, new File(files[i]), prefixs[i]);
        }
      }
      jout.close();
    } catch (Exception ex) {
      log.error("buildJar: %s", ex);
      return false;
    }
    log.debug("buildJar: completed");
    return true;
  }

  /**
   * unpack a jar file to a folder
   * @param jarName absolute path to jar file
   * @param folderName absolute path to the target folder
   * @param del true if the folder should be deleted before unpack
   * @param strip true if the path should be stripped
   * @param filter to select specific content
   * @return true if success,  false otherwise
   */
  public static boolean unpackJar(String jarName, String folderName, boolean del, boolean strip,
          Content.JarFileFilter filter) {
    jarName = Content.slashify(jarName, false);
    if (!jarName.endsWith(".jar")) {
      jarName += ".jar";
    }
    if (!new File(jarName).isAbsolute()) {
      log.error("unpackJar: jar path not absolute");
      return false;
    }
    if (folderName == null) {
      folderName = jarName.substring(0, jarName.length() - 4);
    } else if (!new File(folderName).isAbsolute()) {
      log.error("unpackJar: folder path not absolute");
      return false;
    }
    folderName = Content.slashify(folderName, true);
    ZipInputStream in;
    BufferedOutputStream out;
    try {
      if (del) {
        Content.deleteFileOrFolder(folderName);
      }
      in = new ZipInputStream(new BufferedInputStream(new FileInputStream(jarName)));
      log.debug("unpackJar: %s to %s", jarName, folderName);
      boolean isExecutable;
      int n;
      File f;
      for (ZipEntry z = in.getNextEntry(); z != null; z = in.getNextEntry()) {
        if (filter == null || filter.accept(z, null)) {
          if (z.isDirectory()) {
            (new File(folderName, z.getName())).mkdirs();
          } else {
            n = z.getName().lastIndexOf(EXECUTABLE);
            if (n >= 0) {
              f = new File(folderName, z.getName().substring(0, n));
              isExecutable = true;
            } else {
              f = new File(folderName, z.getName());
              isExecutable = false;
            }
            if (strip) {
              f = new File(folderName, f.getName());
            } else {
              f.getParentFile().mkdirs();
            }
            out = new BufferedOutputStream(new FileOutputStream(f));
            bufferedWrite(in, out);
            out.close();
            if (isExecutable) {
              f.setExecutable(true, false);
            }
          }
        }
      }
      in.close();
    } catch (Exception ex) {
      log.error("unpackJar: " + ex.getMessage());
      return false;
    }
    log.debug("unpackJar: completed");
    return true;
  }

  private static void addToJar(JarOutputStream jar, File dir, String prefix) throws IOException {
    File[] content;
    prefix = prefix == null ? "" : prefix;
    if (dir.isDirectory()) {
      content  = dir.listFiles();
      for (int i = 0, l = content.length; i < l; ++i) {
        if (content[i].isDirectory()) {
          jar.putNextEntry(new ZipEntry(prefix + (prefix.equals("") ? "" : "/") + content[i].getName() + "/"));
          addToJar(jar, content[i], prefix + (prefix.equals("") ? "" : "/") + content[i].getName());
        } else {
          addToJarWriteFile(jar, content[i], prefix);
        }
      }
    } else {
      addToJarWriteFile(jar, dir, prefix);
    }
  }

  private static void addToJarWriteFile(JarOutputStream jar, File file, String prefix) throws IOException {
    if (file.getName().startsWith(".")) {
      return;
    }
    String suffix = "";
//TODO buildjar: suffix EXECUTABL
//    if (file.canExecute()) {
//      suffix = EXECUTABLE;
//    }
    jar.putNextEntry(new ZipEntry(prefix + (prefix.equals("") ? "" : "/") + file.getName() + suffix));
    FileInputStream in = new FileInputStream(file);
    bufferedWrite(in, jar);
    in.close();
  }

  public static File[] getScriptFile(File fScriptFolder) {
    if (fScriptFolder == null) {
      return null;
    }
    String scriptName;
    String scriptType = "";
    String fpUnzippedSkl = null;
    File[] content = null;

    if (fScriptFolder.getName().endsWith(".skl") || fScriptFolder.getName().endsWith(".zip")) {
      fpUnzippedSkl = Content.unzipSKL(fScriptFolder.getAbsolutePath());
      if (fpUnzippedSkl == null) {
        return null;
      }
      scriptType = "sikuli-zipped";
      fScriptFolder = new File(fpUnzippedSkl);
    }

    int pos = fScriptFolder.getName().lastIndexOf(".");
    if (pos == -1) {
      scriptName = fScriptFolder.getName();
      scriptType = "sikuli-plain";
    } else {
      scriptName = fScriptFolder.getName().substring(0, pos);
      scriptType = fScriptFolder.getName().substring(pos + 1);
    }

    boolean success = true;
    if (!fScriptFolder.exists()) {
      if ("sikuli-plain".equals(scriptType)) {
        fScriptFolder = new File(fScriptFolder.getAbsolutePath() + ".sikuli");
        if (!fScriptFolder.exists()) {
          success = false;
        }
      } else {
        success = false;
      }
    }
    if (!success) {
      log.error("Not a valid Sikuli script project:\n%s", fScriptFolder.getAbsolutePath());
      return null;
    }
    if (scriptType.startsWith("sikuli")) {
      content = fScriptFolder.listFiles(new FileFilterScript(scriptName + "."));
      if (content == null || content.length == 0) {
        log.error("Script project %s \n has no script file %s.xxx", fScriptFolder, scriptName);
        return null;
      }
    } else if ("jar".equals(scriptType)) {
      log.error("Sorry, script projects as jar-files are not yet supported;");
      //TODO try to load and run as extension
      return null; // until ready
    }
    return content;
  }

  public static List<String> extractTessData(File folder) {
    List<String> files = new ArrayList<String>();

    String tessdata = "/sikulixtessdata";
    URL uContentList = SX.sxGlobalClassReference.getResource(tessdata + "/" + SX.fpContent);
    if (uContentList != null) {
      files = doResourceListWithList(tessdata, files, null);
      if (files.size() > 0) {
        files = doExtractToFolderWithList(tessdata, folder, files);
      }
    } else {
      files = extractResourcesToFolder("/sikulixtessdata", folder, null);
    }
    return (files.size() == 0 ? null : files);
  }

  /**
   * export all resource files from the given subtree on classpath to the given folder retaining the subtree<br>
   * to export a specific file from classpath lock extractResourceToFile or extractResourceToString
   *
   * @param fpRessources path of the subtree relative to root
   * @param fFolder      folder where to export (if null, only list - no export)
   * @param filter       implementation of interface FilenameFilter or null for no filtering
   * @return the filtered list of files (compact sikulixcontent format)
   */
  public static List<String> extractResourcesToFolder(String fpRessources, File fFolder, FilenameFilter filter) {
    List<String> content = null;
    content = resourceList(fpRessources, filter);
    if (content == null) {
      return null;
    }
    if (fFolder == null) {
      return content;
    }
    return doExtractToFolderWithList(fpRessources, fFolder, content);
  }

  public static List<String> doExtractToFolderWithList(String fpRessources, File fFolder, List<String> content) {
    int count = 0;
    int ecount = 0;
    String subFolder = "";
    if (content != null && content.size() > 0) {
      for (String eFile : content) {
        if (eFile == null) {
          continue;
        }
        if (eFile.endsWith("/")) {
          subFolder = eFile.substring(0, eFile.length() - 1);
          continue;
        }
        if (!subFolder.isEmpty()) {
          eFile = new File(subFolder, eFile).getPath();
        }
        if (extractResourceToFile(fpRessources, eFile, fFolder)) {
          log.trace("extractResourceToFile done: %s", eFile);
          count++;
        } else {
          ecount++;
        }
      }
    }
    if (ecount > 0) {
      log.debug("files exported: %d - skipped: %d from %s to:\n%s", count, ecount, fpRessources, fFolder);
    } else {
      log.debug("files exported: %d from: %s to:\n%s", count, fpRessources, fFolder);
    }
    return content;
  }

  /**
   * store a resource found on classpath to a file in the given folder with same filename
   *
   * @param inPrefix a subtree found in classpath
   * @param inFile   the filename combined with the prefix on classpath
   * @param outDir   a folder where to export
   * @return success
   */
  public static boolean extractResourceToFile(String inPrefix, String inFile, File outDir) {
    return extractResourceToFile(inPrefix, inFile, outDir, "");
  }

  /**
   * store a resource found on classpath to a file in the given folder
   *
   * @param inPrefix a subtree found in classpath
   * @param inFile   the filename combined with the prefix on classpath
   * @param outDir   a folder where to export
   * @param outFile  the filename for export
   * @return success
   */
  public static boolean extractResourceToFile(String inPrefix, String inFile, File outDir, String outFile) {
    InputStream aIS;
    FileOutputStream aFileOS;
    String content = inPrefix + "/" + inFile;
    try {
      content = SX.isWindows() ? content.replace("\\", "/") : content;
      if (!content.startsWith("/")) {
        content = "/" + content;
      }
      aIS = (InputStream) SX.sxGlobalClassReference.getResourceAsStream(content);
      if (aIS == null) {
        throw new IOException("resource not accessible");
      }
      File out = outFile.isEmpty() ? new File(outDir, inFile) : new File(outDir, inFile);
      if (!out.getParentFile().exists()) {
        out.getParentFile().mkdirs();
      }
      aFileOS = new FileOutputStream(out);
      copy(aIS, aFileOS);
      aIS.close();
      aFileOS.close();
    } catch (Exception ex) {
      log.error("extractResourceToFile: %s (%s)", content, ex);
      return false;
    }
    return true;
  }

  /**
   * store the content of a resource found on classpath in the returned string
   *
   * @param inPrefix a subtree from root found in classpath (leading /)
   * @param inFile   the filename combined with the prefix on classpath
   * @return file content in UTF-8 encoding
   */
  public static String extractResourceToString(String inPrefix, String inFile) {
    return extractResourceToString(inPrefix, inFile, "");
  }

    /**
     * store the content of a resource found on classpath in the returned string
     *
     * @param inPrefix a subtree from root found in classpath (leading /)
     * @param inFile   the filename combined with the prefix on classpath
     * @param encoding
     * @return file content
     */
  public static String extractResourceToString(String inPrefix, String inFile, String encoding) {
    InputStream aIS = null;
    String out = null;
    String content = inPrefix + "/" + inFile;
    if (!content.startsWith("/")) {
      content = "/" + content;
    }
    try {
      content = SX.isWindows() ? content.replace("\\", "/") : content;
      aIS = (InputStream) SX.sxGlobalClassReference.getResourceAsStream(content);
      if (aIS != null) {
        if (encoding == null || encoding.isEmpty()) {
          encoding = "UTF-8";
          out = new String(copy(aIS), "UTF-8");
        } else {
          out = new String(copy(aIS), encoding);
        }
        aIS.close();
      }
      aIS = null;
    } catch (Exception ex) {
      log.error("extractResourceToString error: %s from: %s (%s)", encoding, content, ex);
    }
    try {
      if (aIS != null) {
        aIS.close();
      }
    } catch (Exception ex) {
    }
    return out;
  }

  public static URL resourceLocation(String folderOrFile) {
    log.debug("resourceLocation: (%s) %s", SX.sxGlobalClassReference, folderOrFile);
    if (!folderOrFile.startsWith("/")) {
      folderOrFile = "/" + folderOrFile;
    }
    return SX.sxGlobalClassReference.getResource(folderOrFile);
  }

  public static List<String> resourceList(String folder, FilenameFilter filter) {
    log.debug("resourceList: enter");
    List<String> files = new ArrayList<String>();
    if (!folder.startsWith("/")) {
      folder = "/" + folder;
    }
    URL uFolder = resourceLocation(folder);
    if (uFolder == null) {
      log.debug("resourceList: not found: %s", folder);
      return files;
    }
    try {
      uFolder = new URL(uFolder.toExternalForm().replaceAll(" ", "%20"));
    } catch (Exception ex) {
    }
    URL uContentList = SX.sxGlobalClassReference.getResource(folder + "/" + SX.fpContent);
    if (uContentList != null) {
      return doResourceListWithList(folder, files, filter);
    }
    File fFolder = null;
    try {
      fFolder = new File(uFolder.toURI());
      log.debug("resourceList: having folder:\n%s", fFolder);
      String sFolder = normalizeAbsolute(fFolder.getPath(), false);
      if (":".equals(sFolder.substring(2, 3))) {
        sFolder = sFolder.substring(1);
      }
      files.add(sFolder);
      files = doResourceListFolder(new File(sFolder), files, filter);
      files.remove(0);
      return files;
    } catch (Exception ex) {
      if (!"jar".equals(uFolder.getProtocol())) {
        log.debug("resourceList:\n%s", folder);
        log.error("resourceList: URL neither folder nor jar:\n%s", ex);
        return null;
      }
    }
    String[] parts = uFolder.getPath().split("!");
    if (parts.length < 2 || !parts[0].startsWith("file:")) {
      log.debug("resourceList:\n%s", folder);
      log.error("resourceList: not a valid jar URL:\n" + uFolder.getPath());
      return null;
    }
    String fpFolder = parts[1];
    log.debug("resourceList: having jar:\n%s", uFolder);
    return doResourceListJar(uFolder, fpFolder, files, filter);
  }

  public static List<String> doResourceListFolder(File fFolder, List<String> files, FilenameFilter filter) {
    int localLevel = lvl + 1;
    String subFolder = "";
    if (fFolder.isDirectory()) {
      if (!pathEquals(fFolder.getPath(), files.get(0))) {
        subFolder = fFolder.getPath().substring(files.get(0).length() + 1).replace("\\", "/") + "/";
        if (filter != null && !filter.accept(new File(files.get(0), subFolder), "")) {
          return files;
        }
      } else {
        log.trace("scanning folder:\n%s", fFolder);
        subFolder = "/";
        files.add(subFolder);
      }
      String[] subList = fFolder.list();
      for (String entry : subList) {
        File fEntry = new File(fFolder, entry);
        if (fEntry.isDirectory()) {
          files.add(fEntry.getAbsolutePath().substring(1 + files.get(0).length()).replace("\\", "/") + "/");
          doResourceListFolder(fEntry, files, filter);
          files.add(subFolder);
        } else {
          if (filter != null && !filter.accept(fFolder, entry)) {
            continue;
          }
          log.trace("from %s adding: %s", (subFolder.isEmpty() ? "." : subFolder), entry);
          files.add(fEntry.getAbsolutePath().substring(1 + fFolder.getPath().length()));
        }
      }
    }
    return files;
  }

  public static List<String> doResourceListWithList(String folder, List<String> files, FilenameFilter filter) {
    String content = extractResourceToString(folder, SX.fpContent, "");
    String[] contentList = content.split(content.indexOf("\r") != -1 ? "\r\n" : "\n");
    if (filter == null) {
      files.addAll(Arrays.asList(contentList));
    } else {
      for (String fpFile : contentList) {
        if (filter.accept(new File(fpFile), "")) {
          files.add(fpFile);
        }
      }
    }
    return files;
  }

  public static List<String> doResourceListJar(URL uJar, String fpResource, List<String> files, FilenameFilter filter) {
    int localLevel = lvl + 1;
    ZipInputStream zJar;
    String fpJar = uJar.getPath().split("!")[0];
    String fileSep = "/";
    if (!fpJar.endsWith(".jar")) {
      return files;
    }
    log.trace("scanning jar:\n%s", uJar);
    fpResource = fpResource.startsWith("/") ? fpResource.substring(1) : fpResource;
    File fFolder = new File(fpResource);
    File fSubFolder = null;
    ZipEntry zEntry;
    String subFolder = "";
    boolean skip = false;
    try {
      zJar = new ZipInputStream(new URL(fpJar).openStream());
      while ((zEntry = zJar.getNextEntry()) != null) {
        if (zEntry.getName().endsWith("/")) {
          continue;
        }
        String zePath = zEntry.getName();
        if (zePath.startsWith(fpResource)) {
          if (fpResource.length() == zePath.length()) {
            files.add(zePath);
            return files;
          }
          String zeName = zePath.substring(fpResource.length() + 1);
          int nSep = zeName.lastIndexOf(fileSep);
          String zefName = zeName.substring(nSep + 1, zeName.length());
          String zeSub = "";
          if (nSep > -1) {
            zeSub = zeName.substring(0, nSep + 1);
            if (!subFolder.equals(zeSub)) {
              subFolder = zeSub;
              fSubFolder = new File(fFolder, subFolder);
              skip = false;
              if (filter != null && !filter.accept(fSubFolder, "")) {
                skip = true;
                continue;
              }
              files.add(zeSub);
            }
            if (skip) {
              continue;
            }
          } else {
            if (!subFolder.isEmpty()) {
              subFolder = "";
              fSubFolder = fFolder;
              files.add("/");
            }
          }
          if (filter != null && !filter.accept(fSubFolder, zefName)) {
            continue;
          }
          files.add(zefName);
          log.trace("from %s adding: %s", (zeSub.isEmpty() ? "." : zeSub), zefName);
        }
      }
    } catch (Exception ex) {
      log.error("doResourceListJar: %s", ex);
      return files;
    }
    return files;
  }

  public static void copy(InputStream in, OutputStream out) throws IOException {
    byte[] tmp = new byte[8192];
    int len;
    while (true) {
      len = in.read(tmp);
      if (len <= 0) {
        break;
      }
      out.write(tmp, 0, len);
    }
    out.flush();
  }

  public static byte[] copy(InputStream inputStream) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int length = 0;
    while ((length = inputStream.read(buffer)) != -1) {
      baos.write(buffer, 0, length);
    }
    return baos.toByteArray();
  }

  /**
   * export all resource files from the given subtree in given jar to the given folder retaining the subtree
   *
   * @param aJar         absolute path to an existing jar or a string identifying the jar on classpath (no leading /)
   * @param fpRessources path of the subtree or file relative to root
   * @param fFolder      folder where to export (if null, only list - no export)
   * @param filter       implementation of interface FilenameFilter or null for no filtering
   * @return the filtered list of files (compact sikulixcontent format)
   */
  public static List<String> extractResourcesToFolderFromJar(String aJar, String fpRessources, File fFolder, FilenameFilter filter) {
    List<String> content = new ArrayList<String>();
    File faJar = new File(aJar);
    URL uaJar = null;
    fpRessources = slashify(fpRessources, false);
    if (faJar.isAbsolute()) {
      if (!faJar.exists()) {
        log.error("extractResourcesToFolderFromJar: does not exist: %s", faJar);
        return null;
      }
      try {
        uaJar = new URL("jar", null, "file:" + aJar);
        log.info("%s", uaJar);
      } catch (MalformedURLException ex) {
        log.error("extractResourcesToFolderFromJar: bad URL for: %s", faJar);
        return null;
      }
    } else {
      uaJar = fromClasspath(aJar);
      if (uaJar == null) {
        log.error("extractResourcesToFolderFromJar: not on classpath: %s", aJar);
        return null;
      }
      try {
        String sJar = "file:" + uaJar.getPath() + "!/";
        uaJar = new URL("jar", null, sJar);
      } catch (MalformedURLException ex) {
        log.error("extractResourcesToFolderFromJar: bad URL for: %s", uaJar);
        return null;
      }
    }
    content = doResourceListJar(uaJar, fpRessources, content, filter);
    if (fFolder == null) {
      return content;
    }
    copyFromJarToFolderWithList(uaJar, fpRessources, content, fFolder);
    return content;
  }

  /**
   * write the list as it is produced by calling extractResourcesToFolder to the given file with system line
   * separator<br>
   * non-compact format: every file with full path
   *
   * @param folder path of the subtree relative to root with leading /
   * @param target the file to write the list (if null, only list - no file)
   * @param filter implementation of interface FilenameFilter or null for no filtering
   * @return success
   */
  public static String[] resourceListAsFile(String folder, File target, FilenameFilter filter) {
    String content = resourceListAsString(folder, filter);
    if (content == null) {
      log.error("resourceListAsFile: did not work: %s", folder);
      return null;
    }
    if (target != null) {
      try {
        deleteFileOrFolder(target.getAbsolutePath());
        target.getParentFile().mkdirs();
        PrintWriter aPW = new PrintWriter(target);
        aPW.write(content);
        aPW.close();
      } catch (Exception ex) {
        log.error("resourceListAsFile: %s:\n%s", target, ex);
      }
    }
    return content.split(System.getProperty("line.separator"));
  }

  /**
   * write the list as it is produced by calling extractResourcesToFolder to the given file with system line
   * separator<br>
   * compact sikulixcontent format
   *
   * @param folder       path of the subtree relative to root with leading /
   * @param targetFolder the folder where to store the file sikulixcontent (if null, only list - no export)
   * @param filter       implementation of interface FilenameFilter or null for no filtering
   * @return success
   */
  public static String[] resourceListAsSXContent(String folder, File targetFolder, FilenameFilter filter) {
    List<String> contentList = resourceList(folder, filter);
    if (contentList == null) {
      log.error("resourceListAsSikulixContent: did not work: %s", folder);
      return null;
    }
    File target = null;
    String arrString[] = new String[contentList.size()];
    try {
      PrintWriter aPW = null;
      if (targetFolder != null) {
        target = new File(targetFolder, SX.fpContent);
        deleteFileOrFolder(target);
        target.getParentFile().mkdirs();
        aPW = new PrintWriter(target);
      }
      int n = 0;
      for (String line : contentList) {
        arrString[n++] = line;
        if (targetFolder != null) {
          aPW.println(line);
        }
      }
      if (targetFolder != null) {
        aPW.close();
      }
    } catch (Exception ex) {
      log.error("resourceListAsFile: %s:\n%s", target, ex);
    }
    return arrString;
  }

  /**
   * write the list as it is produced by calling extractResourcesToFolder to the given file with system line
   * separator<br>
   * compact sikulixcontent format
   *
   * @param aJar         absolute path to an existing jar or a string identifying the jar on classpath (no leading /)
   * @param folder       path of the subtree relative to root with leading /
   * @param targetFolder the folder where to store the file sikulixcontent (if null, only list - no export)
   * @param filter       implementation of interface FilenameFilter or null for no filtering
   * @return success
   */
  public static String[] resourceListAsSXContentFromJar(String aJar, String folder, File targetFolder, FilenameFilter filter) {
    List<String> contentList = extractResourcesToFolderFromJar(aJar, folder, null, filter);
    if (contentList == null || contentList.size() == 0) {
      log.error("resourceListAsSikulixContentFromJar: did not work: %s", folder);
      return null;
    }
    File target = null;
    String arrString[] = new String[contentList.size()];
    try {
      PrintWriter aPW = null;
      if (targetFolder != null) {
        target = new File(targetFolder, SX.fpContent);
        deleteFileOrFolder(target);
        target.getParentFile().mkdirs();
        aPW = new PrintWriter(target);
      }
      int n = 0;
      for (String line : contentList) {
        arrString[n++] = line;
        if (targetFolder != null) {
          aPW.println(line);
        }
      }
      if (targetFolder != null) {
        aPW.close();
      }
    } catch (Exception ex) {
      log.error("resourceListAsFile: %s:\n%s", target, ex);
    }
    return arrString;
  }

  /**
   * write the list produced by calling extractResourcesToFolder to the returned string with system line separator<br>
   * non-compact format: every file with full path
   *
   * @param folder path of the subtree relative to root with leading /
   * @param filter implementation of interface FilenameFilter or null for no filtering
   * @return the resulting string
   */
  public static String resourceListAsString(String folder, FilenameFilter filter) {
    return resourceListAsString(folder, filter, null);
  }

  /**
   * write the list produced by calling extractResourcesToFolder to the returned string with given separator<br>
   * non-compact format: every file with full path
   *
   * @param folder    path of the subtree relative to root with leading /
   * @param filter    implementation of interface FilenameFilter or null for no filtering
   * @param separator to be used to separate the entries
   * @return the resulting string
   */
  public static String resourceListAsString(String folder, FilenameFilter filter, String separator) {
    List<String> aList = resourceList(folder, filter);
    if (aList == null) {
      return null;
    }
    if (separator == null) {
      separator = System.getProperty("line.separator");
    }
    String out = "";
    String subFolder = "";
    if (aList != null && aList.size() > 0) {
      for (String eFile : aList) {
        if (eFile == null) {
          continue;
        }
        if (eFile.endsWith("/")) {
          subFolder = eFile.substring(0, eFile.length() - 1);
          continue;
        }
        if (!subFolder.isEmpty()) {
          eFile = new File(subFolder, eFile).getPath();
        }
        out += eFile.replace("\\", "/") + separator;
      }
    }
    return out;
  }

  public static boolean copyFromJarToFolderWithList(URL uJar, String fpRessource, List<String> files, File fFolder) {
    if (files == null || files.isEmpty()) {
      log.debug("copyFromJarToFolderWithList: list of files is empty");
      return false;
    }
    String fpJar = uJar.getPath().split("!")[0];
    if (!fpJar.endsWith(".jar")) {
      return false;
    }
    log.trace("scanning jar:\n%s", uJar);
    fpRessource = fpRessource.startsWith("/") ? fpRessource.substring(1) : fpRessource;

    String subFolder = "";

    int maxFiles = files.size() - 1;
    int nFiles = 0;

    ZipEntry zEntry;
    ZipInputStream zJar;
    String zPath;
    int prefix = fpRessource.length();
    fpRessource += !fpRessource.isEmpty() ? "/" : "";
    String current = "/";
    boolean shouldStop = false;
    try {
      zJar = new ZipInputStream(new URL(fpJar).openStream());
      while ((zEntry = zJar.getNextEntry()) != null) {
        zPath = zEntry.getName();
        if (zPath.endsWith("/")) {
          continue;
        }
        while (current.endsWith("/")) {
          if (nFiles > maxFiles) {
            shouldStop = true;
            break;
          }
          subFolder = current.length() == 1 ? "" : current;
          current = files.get(nFiles++);
          if (!current.endsWith("/")) {
            current = fpRessource + subFolder + current;
            break;
          }
        }
        if (shouldStop) {
          break;
        }
        if (zPath.startsWith(current)) {
          if (zPath.length() == fpRessource.length() - 1) {
            log.error("extractResourcesToFolderFromJar: only ressource folders allowed - lock filter");
            return false;
          }
          log.trace("copying: %s", zPath);
          File out = new File(fFolder, zPath.substring(prefix));
          if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
          }
          FileOutputStream aFileOS = new FileOutputStream(out);
          copy(zJar, aFileOS);
          aFileOS.close();
          if (nFiles > maxFiles) {
            break;
          }
          current = files.get(nFiles++);
          if (!current.endsWith("/")) {
            current = fpRessource + subFolder + current;
          }
        }
      }
      zJar.close();
    } catch (Exception ex) {
      log.error("doResourceListJar: %s", ex);
      return false;
    }
    return true;
  }

  private static class FileFilterScript implements FilenameFilter {
    private String _check;
    public FileFilterScript(String check) {
      _check = check;
    }
    @Override
    public boolean accept(File dir, String fileName) {
      return fileName.startsWith(_check);
    }
  }

  public static String unzipSKL(String fpSkl) {
    File fSkl = new File(fpSkl);
    if (!fSkl.exists()) {
      log.error("unzipSKL: file not found: %s", fpSkl);
    }
    String name = fSkl.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    File fSikuliDir = Content.createTempDir(name + ".sikuli");
    if (null != fSikuliDir) {
      fSikuliDir.deleteOnExit();
      Content.unzip(fSkl, fSikuliDir);
    }
    if (null == fSikuliDir) {
      log.error("unzipSKL: not possible for:\n%s", fpSkl);
      return null;
    }
    return fSikuliDir.getAbsolutePath();
  }

  public interface JarFileFilter {
    public boolean accept(ZipEntry entry, String jarname);
  }

  public interface FileFilter {
    public boolean accept(File entry);
  }

  public static String extractResourceAsLines(String src) {
    String res = null;
    ClassLoader cl = Content.class.getClassLoader();
    InputStream isContent = cl.getResourceAsStream(src);
    if (isContent != null) {
      res = "";
      String line;
      try {
        BufferedReader cnt = new BufferedReader(new InputStreamReader(isContent));
        line = cnt.readLine();
        while (line != null) {
          res += line + "\n";
          line = cnt.readLine();
        }
        cnt.close();
      } catch (Exception ex) {
        log.error("extractResourceAsLines: %s\n%s", src, ex);
      }
    }
    return res;
  }

  public static boolean extractResource(String src, File tgt) {
    ClassLoader cl = Content.class.getClassLoader();
    InputStream isContent = cl.getResourceAsStream(src);
    if (isContent != null) {
      try {
        log.trace("extractResource: %s to %s", src, tgt);
        tgt.getParentFile().mkdirs();
        OutputStream osTgt = new FileOutputStream(tgt);
        bufferedWrite(isContent, osTgt);
        osTgt.close();
      } catch (Exception ex) {
        log.error("extractResource:\n%s", src, ex);
        return false;
      }
    } else {
      return false;
    }
    return true;
  }

  public class oneFileFilter implements FilenameFilter {

    String aFile;

    public oneFileFilter(String aFileGiven) {
      aFile = aFileGiven;
    }

    @Override
    public boolean accept(File dir, String name) {
      if (name.contains(aFile)) {
        return true;
      }
      return false;
    }
  }

  private static synchronized void bufferedWrite(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = new byte[1024 * 512];
    int read;
    while (true) {
      read = in.read(buffer);
      if (read == -1) {
        break;
      }
      out.write(buffer, 0, read);
    }
    out.flush();
  }

	/**
	 * compares to path strings using java.io.File.equals()
	 * @param path1 string
	 * @param path2 string
	 * @return true if same file or folder
	 */
	public static boolean pathEquals(String path1, String path2) {
    File f1 = new File(path1);
    File f2 = new File(path2);
    boolean isEqual = f1.equals(f2);
    return isEqual;
  }

  //<editor-fold desc="*** java class path">
  public static boolean addClassPath(String jarOrFolder) {
    URL uJarOrFolder = Content.makeURL(jarOrFolder);
    if (!new File(jarOrFolder).exists()) {
      log.debug("addToClasspath: does not exist - not added:\n%s", jarOrFolder);
      return false;
    }
    if (isOnClasspath(uJarOrFolder)) {
      return true;
    }
    log.debug("addToClasspath:\n%s", uJarOrFolder);
    Method method;
    URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
    Class sysclass = URLClassLoader.class;
    try {
      method = sysclass.getDeclaredMethod("addURL", new Class[]{URL.class});
      method.setAccessible(true);
      method.invoke(sysLoader, new Object[]{uJarOrFolder});
    } catch (Exception ex) {
      log.error("Did not work: %s", ex.getMessage());
      return false;
    }
    storeClassPath();
    return true;
  }

  private static List<URL> storeClassPath() {
    URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
    return Arrays.asList(sysLoader.getURLs());
  }

  public static void dumpClassPath() {
    dumpClassPath(null);
  }

  public static void dumpClassPath(String filter) {
    filter = filter == null ? "" : filter;
    log.p("*** classpath dump %s", filter);
    String sEntry;
    filter = filter.toUpperCase();
    int n = 0;
    for (URL uEntry : storeClassPath()) {
      sEntry = uEntry.getPath();
      if (!filter.isEmpty()) {
        if (!sEntry.toUpperCase().contains(filter)) {
          n++;
          continue;
        }
      }
      log.p("%3d: %s", n, sEntry);
      n++;
    }
    log.p("*** classpath dump end");
  }

  public static String isOnClasspath(String fpName, boolean isJar) {
    File fMatch = null;
    List<URL> classPath = storeClassPath();
    int n = -1;
    String sEntry = "";
    for (URL entry : classPath) {
      n++;
      sEntry = entry.toString();
      if (sEntry.contains(".jdk")) {
        continue;
      }
      if (isJar && !sEntry.endsWith(".jar")) {
        continue;
      }
      log.debug("%2d: %s", n, entry);
      if (SX.getFile(entry.getPath()).toString().contains(fpName)) {
        fMatch = new File(entry.getPath());
      }
    }
    return fMatch.toString();
  }

  public static String isJarOnClasspath(String artefact) {
    return isOnClasspath(artefact, true);
  }

  public static String isOnClasspath(String artefact) {
    return isOnClasspath(artefact, false);
  }

  public static URL fromClasspath(String artefact) {
    artefact = Content.slashify(artefact, false).toUpperCase();
    URL cpe = null;
    for (URL entry : storeClassPath()) {
      String sEntry = Content.slashify(new File(entry.getPath()).getPath(), false);
      if (sEntry.toUpperCase().contains(artefact)) {
        return entry;
      }
    }
    return cpe;
  }

  public static boolean isOnClasspath(URL path) {
    for (URL entry : storeClassPath()) {
      if (new File(path.getPath()).equals(new File(entry.getPath()))) {
        return true;
      }
    }
    return false;
  }
  //</editor-fold>
}
