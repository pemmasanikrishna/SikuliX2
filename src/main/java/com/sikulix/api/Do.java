/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.*;
//import com.sikulix.scripting.JythonHelper;
//import com.sikulix.scripting.SXRunner;
import com.sikulix.util.FileChooser;
import org.sikuli.script.Key;

import javax.swing.*;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Implements the top level features as static methods to allow the usage somehow like "commands".<br>
 * Usage: Do.something(arg1, arg2, ...)<br>
 */
public class Do {

  private static SXLog log = SX.getLogger("SX.Do");
  private static String klazz = Do.class.getName();

  //<editor-fold desc="SX Do popat">
  private static Point locPopAt = null;

  public static Element popat(Element at) {
    locPopAt = new Point(at.getCenter().x, at.getCenter().y);
    return new Element(locPopAt);
  }

  public static Element popat(Rectangle at) {
    locPopAt = new Point(at.x + (int) (at.width / 2), at.y + (int) (at.height / 2));
    return new Element(locPopAt);
  }

  public static Element popat(int atx, int aty) {
    locPopAt = new Point(atx, aty);
    return new Element(locPopAt);
  }

  public static Element popat() {
    locPopAt = getLocPopAt();
    return new Element(locPopAt);
  }

  private static Point getLocPopAt() {
    Rectangle screen0 = Do.getDevice().getMonitor();
    if (null == screen0) {
      return null;
    }
    return new Point((int) screen0.getCenterX(), (int) screen0.getCenterY());
  }

  private static JFrame popLocation() {
    if (null == locPopAt) {
      locPopAt = getLocPopAt();
      if (null == locPopAt) {
        return null;
      }
    }
    JFrame anchor = new JFrame();
    anchor.setAlwaysOnTop(true);
    anchor.setUndecorated(true);
    anchor.setSize(1, 1);
    anchor.setLocation(locPopAt);
    anchor.setVisible(true);
    return anchor;
  }
  //</editor-fold>

  //<editor-fold desc="SX Do input one line, password">

  /**
   * request user's input as one line of text <br>
   * with hidden = true: <br>
   * the dialog works as password input (input text hidden as bullets) <br>
   * take care to destroy the return value as soon as possible (internally the password is deleted on return)
   *
   * @param msg
   * @param preset
   * @param title
   * @param hidden
   * @return the text entered
   */
  public static String input(String msg, String preset, String title, boolean hidden) {
    JFrame anchor = popLocation();
    String ret = "";
    if (!hidden) {
      if ("".equals(title)) {
        title = "Sikuli input request";
      }
      ret = (String) JOptionPane.showInputDialog(anchor, msg, title,
              JOptionPane.PLAIN_MESSAGE, null, null, preset);
    } else {
      preset = "";
      JTextArea tm = new JTextArea(msg);
      tm.setColumns(20);
      tm.setLineWrap(true);
      tm.setWrapStyleWord(true);
      tm.setEditable(false);
      tm.setBackground(new JLabel().getBackground());
      JPasswordField pw = new JPasswordField(preset);
      JPanel pnl = new JPanel();
      pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
      pnl.add(pw);
      pnl.add(Box.createVerticalStrut(10));
      pnl.add(tm);
      int retval = JOptionPane.showConfirmDialog(anchor, pnl, title, JOptionPane.OK_CANCEL_OPTION);
      if (0 == retval) {
        char[] pwc = pw.getPassword();
        for (int i = 0; i < pwc.length; i++) {
          ret = ret + pwc[i];
          pwc[i] = 0;
        }
      }
    }
    if (anchor != null) {
      anchor.dispose();
    }
    return ret;
  }

  public static String input(String msg, String title, boolean hidden) {
    return input(msg, "", title, hidden);
  }

  public static String input(String msg, boolean hidden) {
    return input(msg, "", "", hidden);
  }

  public static String input(String msg, String preset, String title) {
    return input(msg, preset, title, false);
  }

  public static String input(String msg, String preset) {
    return input(msg, preset, "", false);
  }

  public static String input(String msg) {
    return input(msg, "", "", false);
  }
  //</editor-fold>

  //<editor-fold desc="SX Do multiline input">

  /**
   * Shows a dialog request to enter text in a multiline text field <br>
   * Though not all text might be visible, everything entered is delivered with the returned text <br>
   * The main purpose for this feature is to allow pasting text from somewhere preserving line breaks <br>
   *
   * @param msg   the message to display.
   * @param title the title for the dialog (default: Sikuli input request)
   * @param lines the maximum number of lines visible in the text field (default 9)
   * @param width the maximum number of characters visible in one line (default 20)
   * @return The user's input including the line breaks.
   */
  public static String inputText(String msg, String title, int lines, int width) {
    return inputText(msg, title, lines, width, "");
  }

  public static String inputText(String msg, String title, int lines, int width, String text) {
    width = Math.max(20, width);
    lines = Math.max(9, lines);
    if ("".equals(title)) {
      title = "Sikuli input request";
    }
    JTextArea ta = new JTextArea("");
    int w = width * ta.getFontMetrics(ta.getFont()).charWidth('m');
    int h = (int) (lines * ta.getFontMetrics(ta.getFont()).getHeight());
    ta.setPreferredSize(new Dimension(w, h));
    ta.setMaximumSize(new Dimension(w, 2 * h));
    ta.setText(text);
    JScrollPane sp = new JScrollPane(ta);
    sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
    JTextArea tm = new JTextArea(msg);
    tm.setColumns(width);
    tm.setLineWrap(true);
    tm.setWrapStyleWord(true);
    tm.setEditable(false);
    tm.setBackground(new JLabel().getBackground());
    JPanel pnl = new JPanel();
    pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
    pnl.add(sp);
    pnl.add(Box.createVerticalStrut(10));
    pnl.add(tm);
    pnl.add(Box.createVerticalStrut(10));
    JFrame anchor = popLocation();
    int ret = JOptionPane.showConfirmDialog(anchor, pnl, title, JOptionPane.OK_CANCEL_OPTION);
    if (anchor != null) {
      anchor.dispose();
    }
    if (0 == ret) {
      return ta.getText();
    } else {
      return "";
    }
  }
  //</editor-fold>

  //<editor-fold desc="SX Do popup, popAsk, popError">
  public static void popup(String message) {
    popup(message, "Sikuli");
  }

  public static void popup(String message, String title) {
    if (SX.isHeadless()) {
      log.error("running headless: [%s] popup(%s)", title, message);
    } else {
      JFrame anchor = popLocation();
      JOptionPane.showMessageDialog(anchor, message, title, JOptionPane.PLAIN_MESSAGE);
      if (anchor != null) {
        anchor.dispose();
      }
    }
  }

  public static boolean popAsk(String msg) {
    return popAsk(msg, null);
  }

  public static boolean popAsk(String msg, String title) {
    if (title == null) {
      title = "... something to decide!";
    }
    JFrame anchor = popLocation();
    int ret = JOptionPane.showConfirmDialog(anchor, msg, title, JOptionPane.YES_NO_OPTION);
    if (anchor != null) {
      anchor.dispose();
    }
    if (ret == JOptionPane.CLOSED_OPTION || ret == JOptionPane.NO_OPTION) {
      return false;
    }
    return true;
  }

  public static void popError(String message) {
    popError(message, "Sikuli");
  }

  public static void popError(String message, String title) {
    JFrame anchor = popLocation();
    JOptionPane.showMessageDialog(anchor, message, title, JOptionPane.ERROR_MESSAGE);
    if (anchor != null) {
      anchor.dispose();
    }
  }
  //</editor-fold>

  //<editor-fold desc="SX Do popSelect, popFile">
  public static String popSelect(String msg, String[] options, String preset) {
    return popSelect(msg, null, options, preset);
  }

  public static String popSelect(String msg, String[] options) {
    if (options.length == 0) {
      return "";
    }
    return popSelect(msg, null, options, options[0]);
  }

  public static String popSelect(String msg, String title, String[] options) {
    if (options.length == 0) {
      return "";
    }
    return popSelect(msg, title, options, options[0]);
  }

  public static String popSelect(String msg, String title, String[] options, String preset) {
    if (title == null || "".equals(title)) {
      title = "... something to select!";
    }
    if (options.length == 0) {
      return "";
    }
    if (preset == null) {
      preset = options[0];
    }
    JFrame anchor = popLocation();
    String ret = (String) JOptionPane.showInputDialog(anchor, msg, title,
            JOptionPane.PLAIN_MESSAGE, null, options, preset);
    if (anchor != null) {
      anchor.dispose();
    }
    return ret;
  }

  public static String popFile(String title) {
    popat(Do.getDevice().getMonitor());
    JFrame anchor = popLocation();
    File ret = FileChooser.load(anchor);
    popat();
    if (SX.isNull(ret)) {
      return "";
    }
    return ret.getAbsolutePath();
  }
  //</editor-fold>

  //<editor-fold desc="SX Do compile Jython, build a jar ">

  //<editor-fold desc="TODO ScriptingHelper">
  /**
   * add a jar to the scripting environment<br>
   * Jython: added to sys.path<br>
   * JRuby: not yet supported<br>
   * JavaScript: not yet supported<br>
   * if no scripting active (API usage), jar is added to classpath if available
   *
   * @param fpJar absolute path to a jar (relative: searched according to Extension concept,
   *              but first on sys.path)
   * @return the absolute path to the jar or null, if not available
   */
//  public static String load(String fpJar) {
//    return load(fpJar, null);
//  }

  /**
   * add a jar to the scripting environment or to classpath<br>
   * Jython: added to sys.path<br>
   * JRuby: only added to classpath<br>
   * JavaScript: only added to classpath<br>
   * if no scripting is active (API usage), jar is added to classpath if available<br>
   * additionally: fpJar/fpJarImagePath is added to ImagePath (not checked)
   *
   * @param fpJar          absolute path to a jar (relative: searched according to Extension concept,
   *                       but first on sys.path)
   * @param fpJarImagePath path relative to jar root inside jar
   * @return the absolute path to the jar or null, if not available
   */
/*
  public static String load(String fpJar, String fpJarImagePath) {
    JythonHelper jython = JythonHelper.get();
    String fpJarFound = null;
    if (jython != null) {
      File aFile = jython.existsSysPathJar(fpJar);
      if (aFile != null) {
        fpJar = aFile.getAbsolutePath();
      }
      fpJarFound = jython.load(fpJar);
    } else {
      File fJarFound = SX.asExtension(fpJar);
      if (fJarFound != null) {
        fpJarFound = fJarFound.getAbsolutePath();
        Content.addClassPath(fpJarFound);
      }
    }
    if (fpJarFound != null && fpJarImagePath != null) {
      addImagePath(fpJarFound, fpJarImagePath);
    }
    return fpJarFound;
  }
*/
  /**
   * the foo.py files in the given source folder are compiled to JVM-ByteCode-classfiles foo$py.class
   * and stored in the target folder (thus securing your code against changes).<br>
   * A folder structure is preserved. All files not ending as .py will be copied also.
   * The target folder might then be packed to a jar using buildJarFromFolder.<br>
   * Be aware: you will get no feedback about any compile problems,
   * so make sure your code compiles error free. Currently there is no support for running such a jar,
   * it can only be used with load()/import, but you might provide a simple script that does load()/import
   * and then runs something based on available functions in the jar code.
   *
   * @param fpSource absolute path to a folder/folder-tree containing the stuff to be copied/compiled
   * @param fpTarget the folder that will contain the copied/compiled stuff (folder is first deleted)
   * @return false if anything goes wrong, true means should have worked
   */

/*
  public static boolean compileJythonFolder(String fpSource, String fpTarget) {
    JythonHelper jython = JythonHelper.get();
    if (jython != null) {
      File fTarget = new File(fpTarget);
      Content.deleteFileOrFolder(fTarget);
      fTarget.mkdirs();
      if (!fTarget.exists()) {
        log.error("compileJythonFolder: target folder not available\n%", fTarget);
        return false;
      }
      File fSource = new File(fpSource);
      if (!fSource.exists()) {
        log.error("compileJythonFolder: source folder not available\n", fSource);
        return false;
      }
      if (fTarget.equals(fSource)) {
        log.error("compileJythonFolder: target folder cannot be the same as the source folder");
        return false;
      }
      Content.xcopy(fSource, fTarget);
      if (!jython.exec("import compileall")) {
        return false;
      }
      jython = doCompileJythonFolder(jython, fTarget);
      Content.traverseFolder(fTarget, new CompileJythonFilter(jython));
    }
    return false;
  }

  private static JythonHelper doCompileJythonFolder(JythonHelper jython, File fSource) {
    String fpSource = Content.slashify(fSource.getAbsolutePath(), false);
    if (!jython.exec(String.format("compileall.compile_dir(\"%s\"," + "maxlevels = 0, quiet = 1)", fpSource))) {
      return null;
    }
    for (File aFile : fSource.listFiles()) {
      if (aFile.getName().endsWith(".py")) {
        aFile.delete();
      }
    }
    return jython;
  }

  private static class CompileJythonFilter implements Content.FileFilter {

    JythonHelper jython = null;

    public CompileJythonFilter(JythonHelper jython) {
      this.jython = jython;
    }

    @Override
    public boolean accept(File entry) {
      if (jython != null && entry.isDirectory()) {
        jython = Do.doCompileJythonFolder(jython, entry);
      }
      return false;
    }
  }
*/
  //</editor-fold>

  /**
   * build a jar on the fly at runtime from a folder.<br>
   * special for Jython: if the folder contains a __init__.py on first level,
   * the folder will be copied to the jar root (hence preserving module folders)
   *
   * @param targetJar    absolute path to the created jar (parent folder must exist, jar is overwritten)
   * @param sourceFolder absolute path to a folder, the contained folder structure
   *                     will be copied to the jar root level
   * @return
   */
  public static boolean buildJarFromFolder(String targetJar, String sourceFolder) {
    log.debug("buildJarFromFolder: \nfrom Folder: %s\nto Jar: %s", sourceFolder, targetJar);
    File fJar = new File(targetJar);
    if (!fJar.getParentFile().exists()) {
      log.error("buildJarFromFolder: parent folder of Jar not available");
      return false;
    }
    File fSrc = new File(sourceFolder);
    if (!fSrc.exists() || !fSrc.isDirectory()) {
      log.error("buildJarFromFolder: source folder not available");
      return false;
    }
    String prefix = null;
    if (new File(fSrc, "__init__.py").exists() || new File(fSrc, "__init__$py.class").exists()) {
      prefix = fSrc.getName();
      if (prefix.endsWith("_")) {
        prefix = prefix.substring(0, prefix.length() - 1);
      }
    }
    return Content.buildJar(targetJar, new String[]{null}, new String[]{sourceFolder}, new String[]{prefix}, null);
  }
  //</editor-fold>

  //<editor-fold desc="SX Do ImagePath handling">
  public static boolean setBundlePath(Object... args) {
    return Picture.setBundlePath(args);
  }

  public static String getBundlePath() {
    return Picture.getBundlePath();
  }

  public static void addImagePath(Object... args) {
    Picture.addPath(args);
  }

  public static void removeImagePath(Object... args) {
    Picture.removePath(args);
  }

  public static String[] getImagePath() {
    return Picture.getPath();
  }
  //</editor-fold>

  //<editor-fold desc="SX Do Clipboard">

  /**
   * @return clipboard content
   */
  public static String getClipboard() {
    Transferable content = null;
    try {
      content = SXClipboard.getSystemClipboard().getContents(null);
    } catch (Exception ex) {
      log.error("getClipboard: clipboard not available:\n%s", ex.getMessage());
    }
    if (content != null) {
      try {
        if (content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
          return (String) content.getTransferData(DataFlavor.stringFlavor);
        }
      } catch (UnsupportedFlavorException ex) {
        log.error("getClipboard: UnsupportedFlavorException: %s", content);
      } catch (IOException ex) {
        log.error("getClipboard: IOException:\n%s", ex.getMessage());
      }
    }
    return "";
  }

  /**
   * @param text to set Clipboard content
   */
  public static void setClipboard(String text) {
    SXClipboard.putText(SXClipboard.PLAIN, SXClipboard.UTF8, SXClipboard.CHAR_BUFFER, text);
  }

  private static class SXClipboard {

    public static final TextType HTML = new TextType("text/html");
    public static final TextType PLAIN = new TextType("text/plain");

    public static final Charset UTF8 = new Charset("UTF-8");
    public static final Charset UTF16 = new Charset("UTF-16");
    public static final Charset UNICODE = new Charset("unicode");
    public static final Charset US_ASCII = new Charset("US-ASCII");

    public static final TransferType READER = new TransferType(Reader.class);
    public static final TransferType INPUT_STREAM = new TransferType(InputStream.class);
    public static final TransferType CHAR_BUFFER = new TransferType(CharBuffer.class);
    public static final TransferType BYTE_BUFFER = new TransferType(ByteBuffer.class);

    private SXClipboard() {
    }

    /**
     * Dumps a given text (either String or StringBuffer) into the Clipboard, with a default MIME type
     */
    public static void putText(CharSequence data) {
      StringSelection copy = new StringSelection(data.toString());
      getSystemClipboard().setContents(copy, copy);
    }

    /**
     * Dumps a given text (either String or StringBuffer) into the Clipboard with a specified MIME type
     */
    public static void putText(TextType type, Charset charset, TransferType transferType, CharSequence data) {
      String mimeType = type + "; charset=" + charset + "; class=" + transferType;
      TextTransferable transferable = new TextTransferable(mimeType, data.toString());
      getSystemClipboard().setContents(transferable, transferable);
    }

    public static java.awt.datatransfer.Clipboard getSystemClipboard() {
      return Toolkit.getDefaultToolkit().getSystemClipboard();
    }

    private static class TextTransferable implements Transferable, ClipboardOwner {
      private String data;
      private DataFlavor flavor;

      public TextTransferable(String mimeType, String data) {
        flavor = new DataFlavor(mimeType, "Text");
        this.data = data;
      }

      @Override
      public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{flavor, DataFlavor.stringFlavor};
      }

      @Override
      public boolean isDataFlavorSupported(DataFlavor flavor) {
        boolean b = this.flavor.getPrimaryType().equals(flavor.getPrimaryType());
        return b || flavor.equals(DataFlavor.stringFlavor);
      }

      @Override
      public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (flavor.isRepresentationClassInputStream()) {
          return new StringReader(data);
        } else if (flavor.isRepresentationClassReader()) {
          return new StringReader(data);
        } else if (flavor.isRepresentationClassCharBuffer()) {
          return CharBuffer.wrap(data);
        } else if (flavor.isRepresentationClassByteBuffer()) {
          return ByteBuffer.wrap(data.getBytes());
        } else if (flavor.equals(DataFlavor.stringFlavor)) {
          return data;
        }
        throw new UnsupportedFlavorException(flavor);
      }

      @Override
      public void lostOwnership(java.awt.datatransfer.Clipboard clipboard, Transferable contents) {
      }
    }

    /**
     * Enumeration for the text type property in MIME types
     */
    public static class TextType {
      private String type;

      private TextType(String type) {
        this.type = type;
      }

      @Override
      public String toString() {
        return type;
      }
    }

    /**
     * Enumeration for the charset property in MIME types (UTF-8, UTF-16, etc.)
     */
    public static class Charset {
      private String name;

      private Charset(String name) {
        this.name = name;
      }

      @Override
      public String toString() {
        return name;
      }
    }

    /**
     * Enumeration for the transferScriptt type property in MIME types (InputStream, CharBuffer, etc.)
     */
    public static class TransferType {
      private Class dataClass;

      private TransferType(Class streamClass) {
        this.dataClass = streamClass;
      }

      public Class getDataClass() {
        return dataClass;
      }

      @Override
      public String toString() {
        return dataClass.getName();
      }
    }

  }
  //</editor-fold>

  //<editor-fold desc="SX Do Keys">

  /**
   * get the lock state of the given key
   *
   * @param key respective key specifier according class Key
   * @return true/false
   */
  public static boolean isLockOn(char key) {
    return Key.isLockOn(key);
  }

  /**
   * @return System dependent key
   */
  public static int getHotkeyModifier() {
    return Key.getHotkeyModifier();
  }

  /**
   * @param key       respective key specifier according class Key
   * @param modifiers respective key specifier according class KeyModifiers
   * @param listener  a HotKeyListener instance
   * @return true if ok, false otherwise
   */
  public static boolean addHotkey(String key, int modifiers, HotkeyListener listener) {
    return Key.addHotkey(key, modifiers, listener);
  }

  /**
   * @param key       respective key specifier according class Key
   * @param modifiers respective key specifier according class KeyModifiers
   * @param listener  a HotKeyListener instance
   * @return true if ok, false otherwise
   */
  public static boolean addHotkey(char key, int modifiers, HotkeyListener listener) {
    return Key.addHotkey(key, modifiers, listener);
  }

  /**
   * @param key       respective key specifier according class Key
   * @param modifiers respective key specifier according class KeyModifiers
   * @return true if ok, false otherwise
   */
  public static boolean removeHotkey(String key, int modifiers) {
    return Key.removeHotkey(key, modifiers);
  }

  /**
   * @param key       respective key specifier according class Key
   * @param modifiers respective key specifier according class KeyModifiers
   * @return true if ok, false otherwise
   */
  public static boolean removeHotkey(char key, int modifiers) {
    return Key.removeHotkey(key, modifiers);
  }
  //</editor-fold>

  //<editor-fold desc="SX Do run something">
  public static String run(String cmdline) {
    return run(new String[]{cmdline});
  }

  public static String run(String[] cmd) {
    log.terminate(1, "run: not implemented");
    return "";
  }

  public static Object run(Object... args) {
    String script = args[0].toString();
    String scriptArgs[] = new String[args.length - 1];
    if (scriptArgs.length > 0) {
      for (int i = 1; i < args.length; i++) {
        scriptArgs[i - 1] = args[i].toString();
      }
    }
    return null; //TODO SXRunner.run(script, scriptArgs);
  }

  public static String NL = "\n";

  public final static String runCmdError = "*****error*****";
  static String lastResult = "";

  /**
   * run a system command finally using Java::Runtime.getRuntime().exec(args) and waiting for completion
   *
   * @param cmd the command as it would be given on command line, quoting is preserved
   * @return the output produced by the command (sysout [+ "*** error ***" + syserr] if the syserr part is present, the
   * command might have failed
   */
  public static String runcmd(String cmd) {
    return runcmd(new String[]{cmd});
  }

  /**
   * run a system command finally using Java::Runtime.getRuntime().exec(args) and waiting for completion
   *
   * @param args the command as it would be given on command line splitted into the space devided parts, first part is
   *             the command, the rest are parameters and their values
   * @return the output produced by the command (sysout [+ "*** error ***" + syserr] if the syserr part is present, the
   * command might have failed
   */
  public static String runcmd(String args[]) {
    if (args.length == 0) {
      return "";
    }
    boolean silent = false;
    if (args.length == 1) {
      String separator = "\"";
      ArrayList<String> argsx = new ArrayList<String>();
      StringTokenizer toks;
      String tok;
      String cmd = args[0];
      if (SX.isWindows()) {
        cmd = cmd.replaceAll("\\\\ ", "%20;");
      }
      toks = new StringTokenizer(cmd);
      while (toks.hasMoreTokens()) {
        tok = toks.nextToken(" ");
        if (tok.length() == 0) {
          continue;
        }
        if (separator.equals(tok)) {
          continue;
        }
        if (tok.startsWith(separator)) {
          if (tok.endsWith(separator)) {
            tok = tok.substring(1, tok.length() - 1);
          } else {
            tok = tok.substring(1);
            tok += toks.nextToken(separator);
          }
        }
        argsx.add(tok.replaceAll("%20;", " "));
      }
      args = argsx.toArray(new String[0]);
    }
    if (args[0].startsWith("!")) {
      silent = true;
      args[0] = args[0].substring(1);
    }
    if (args[0].startsWith("#")) {
      String pgm = args[0].substring(1);
      args[0] = (new File(pgm)).getAbsolutePath();
      runcmd(new String[]{"chmod", "ugo+x", args[0]});
    }
    String result = "";
    String error = runCmdError + NL;
    boolean hasError = false;
    int retVal;
    try {
      Process process = Runtime.getRuntime().exec(args);
      BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
      BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
      String s;
      while ((s = stdInput.readLine()) != null) {
        if (!s.isEmpty()) {
          result += s + NL;
        }
      }
      if ((s = stdError.readLine()) != null) {
        hasError = true;
        if (!s.isEmpty()) {
          error += s + NL;
        }
      }
      process.waitFor();
      retVal = process.exitValue();
      process.destroy();
    } catch (Exception e) {
      log.fatal("error: " + e);
      result = String.format(error + "%s", e);
      retVal = 9999;
      hasError = true;
    }
    if (hasError) {
      result += error;
    }
    lastResult = result;
    return String.format("%d%s%s", retVal, NL, result);
  }

  public static String getLastCommandResult() {
    return lastResult;
  }
  //</editor-fold>

  //<editor-fold desc="Screen related">
  private static Element allMonitorsAsElement = null;

  /**
   * show the current monitor setup
   */
  public static void showMonitors() {
    log.p("*** monitor configuration [ %s Screen(s)] ***", Do.getDevice().getNumberOfMonitors());
    log.p("*** Primary is Screen %d", Do.getDevice().getMainMonitorID());
    for (int i = 0; i < Do.getDevice().getNumberOfMonitors(); i++) {
      log.p("Screen %d: %s", i, new Element(Do.getDevice().getMonitor(i)));
    }
    log.p("*** end monitor configuration ***");
  }

  /**
   * re-initialize the monitor setup (e.g. when it was changed while running)
   */
  public static void resetMonitors() {
    showMonitors();
    log.p("*** TRYING *** to reset the monitor configuration");
    Do.getDevice().resetMonitors();
    showMonitors();
  }

  public static Element on(int monitor) {
    return getScreenAsElement(monitor);
  }

  private static List<Element> screensAsElements = new ArrayList<>();

  private static Element getScreenAsElement(int monitor) {
    if (screensAsElements.size() == 0) {
      for (int i = 0; i < Do.getDevice().getNumberOfMonitors(); i++) {
        screensAsElements.add(new Element(Do.getDevice().getMonitor(i)));
      }
    }
    if (monitor > -1 && monitor < Do.getDevice().getNumberOfMonitors()) {
      return screensAsElements.get(monitor);
    } else {
      return screensAsElements.get(0);
    }
  }

  public static Element all() {
    if (SX.isNull(allMonitorsAsElement)) {
      allMonitorsAsElement = new Element(Do.getDevice().getAllMonitors());
    }
    return allMonitorsAsElement;
  }

  private static Element defaultScreenAsElement = null;
  private static Element getDefaultScreenAsElement() {
    if (SX.isNull(defaultScreenAsElement)) {
      defaultScreenAsElement = new Element(SX.getSXLOCALDEVICE().getMonitor().getBounds());
    }
    return defaultScreenAsElement;
  }

  private static Element defaultElement = null;
  private static Element getDefaultElement() {
    if (SX.isNull(defaultElement)) {
      defaultElement = getDefaultScreenAsElement();
    }
    return defaultElement;
  }

  public static Element on() {
    return getDefaultElement();
  }

  public static Element onMain() {
    return getDefaultScreenAsElement();
  }

  public static Element use() {
    defaultElement = getDefaultScreenAsElement();
    return defaultElement;
  }

  public static Element use(Element elem) {
    defaultElement = elem;
    return defaultElement;
  }

  public static Element use(int monitor) {
    defaultElement = getScreenAsElement(monitor);
    return defaultElement;
  }

  public static Picture capture() {
    return capture(defaultElement);
  }

  public static Picture capture(Element elem) {
    if (SX.isNull(elem)) {
      elem = defaultElement;
    }
    return elem.capture();
  }
  //</editor-fold>

  //<editor-fold desc="actions like find, wait, click">
  public static IDevice getDevice() {
    if (!Do.on().isSpecial()) {
      return SX.getSXLOCALDEVICE();
    } else {
      log.error("not implemented: non-local devices");
      return SX.getSXLOCALDEVICE();
    }
  }

  public static NativeHook getHook() {
    return SX.getSXLOCALDEVICE().getHook();
  }

  public static boolean isMouseposition(NativeHook hook, int x, int y) {
    if (SX.isNotNull(hook)) {
      Point mousePos = hook.getMousePosition();
      log.trace("hook mouse position: (%d, %d) should be (%d, %d)",
              mousePos.x, mousePos.y, x, y);
      return mousePos.x == x && mousePos.y == y;
    } else {
      Element mousePos = getDevice().at();
      log.trace("MouseInfo.getPointerInfo(): mouse position: (%d, %d) should be (%d, %d)",
              mousePos.x, mousePos.y, x, y);
      return mousePos.x == x && mousePos.y == y;
    }
  }

  public static Element at() {
    return getDevice().at();
  }

  public static Element click(Object... args) {
    log.trace("click: start");
    Element target = findForClick(Finder.CLICK, args);
    target.click();
    log.trace("click: end");
    return target;
  }

  public static Element doubleClick(Object... args) {
    log.trace("doubleClick: start");
    Element target = findForClick(Finder.DOUBLECLICK, args);
    target.doubleClick();
    log.trace("doubleClick: end");
    return target;
  }

  public static Element rightClick(Object... args) {
    log.trace("rightClick: start");
    Element target = findForClick(Finder.RIGHTCLICK, args);
    target.rightClick();
    log.trace("rightClick: end");
    return target;
  }

  public static Element hover(Object... args) {
    log.trace("hover: start");
    Element target = findForClick(Finder.HOVER, args);
    target.hover();
    log.trace("hover: end");
    return target;
  }

  private static Element findForClick(String type, Object... args) {
    Element match;
    if (args.length == 0) {
      match = Do.on();
    } else if (args.length == 1) {
      match = Finder.runFind(type, args);
    } else {
      match = Finder.runWait(type, args);
    }
    return match;
  }

  /**
   * @param args
   * @return
   */
  public static Element find(Object... args) {
    log.trace("find: start");
    Element match = Finder.runFind(Finder.FIND, args);
    log.trace("find: end");
    return match;
  }

  public static Element wait(Object... args) {
    log.trace("wait: start");
    Element match = Finder.runWait(Finder.WAIT, args);
    log.trace("wait: end");
    return match;
  }

  public static boolean exists(Object... args) {
    log.trace("exists: start");
    Element match = Finder.runWait(Finder.EXISTS, args);
    log.trace("exists: end");
    return match.isMatch();
  }

  public static boolean waitVanish(Object... args) {
    log.trace("waitVanish: start");
    boolean vanished = Finder.runWaitVanish(args);
    log.trace("waitVanish: end");
    return vanished;
  }

  //<editor-fold desc="findAll">
  public static List<Element> findAll(Object... args) {
    log.trace("findAll: start");
    List<Element> matches = Finder.runFindAll(args);
    log.trace("findAll: end");
    return matches;
  }

  public static boolean hasMatch() {
    return getDefaultElement().hasMatch();
  }

  public static Element getLastMatch() {
    return getDefaultElement().getLastMatch();
  }

  public static boolean hasVanish() {
    return getDefaultElement().hasVanish();
  }

  public static Element getLastVanish() {
    return getDefaultElement().getLastVanish();
  }

  public static boolean hasMatches() {
    return getDefaultElement().hasMatches();
  }

  public static List<Element> getLastMatches() {
    List<Element> matches = getDefaultElement().getLastMatches();
    if (matches.isEmpty()) {
      matches = null;
    }
    return matches;
  }
  //</editor-fold>
  //</editor-fold>
}
