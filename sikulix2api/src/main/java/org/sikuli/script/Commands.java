package org.sikuli.script;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.lang.reflect.Method;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.sikuli.util.Debug;
import org.sikuli.util.hotkey.HotkeyManager;
import org.sikuli.util.PreferencesUser;
import org.sikuli.util.Settings;
import org.sikuli.script.FindFailed;
import org.sikuli.script.ImagePath;
import org.sikuli.script.Location;
import org.sikuli.script.Match;
import org.sikuli.script.Mouse;
import org.sikuli.script.Observing;
import org.sikuli.script.Pattern;
import org.sikuli.script.Region;
import org.sikuli.script.RunTime;
import org.sikuli.script.Runner;
import org.sikuli.script.Screen;
import org.sikuli.script.Sikulix;
import org.sikuli.util.FileManager;
import org.sikuli.util.JythonHelper;
import org.sikuli.util.visual.ScreenHighlighter;

public class Commands {

  private static RunTime rt = RunTime.get();

  private static int lvl = 3;
  
  private static void log(int level, String message, Object... args) {
    Debug.logx(level, "Commands: " + message, args);
  }
	private static void logCmd(String cmd, Object... args) {
		String msg = cmd + ": ";
		if (args.length == 0) {
			log(lvl, msg + "no-args");
		} else {
			for (int i = 0; i < args.length; i++) {
				msg += "%s ";
			}
			log(lvl, msg, args);
		}
	}

  public static void p(String msg, Object... args) {
    System.out.println(String.format(msg, args));
  }

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
    if (!hidden) {
      if ("".equals(title)) {
        title = "Sikuli input request";
      }
      return (String) JOptionPane.showInputDialog(null, msg, title, JOptionPane.PLAIN_MESSAGE, null, null, preset);
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
      if (0 == JOptionPane.showConfirmDialog(null, pnl, title, JOptionPane.OK_CANCEL_OPTION)) {
        char[] pwc = pw.getPassword();
        String pwr = "";
        for (int i = 0; i < pwc.length; i++) {
          pwr = pwr + pwc[i];
          pwc[i] = 0;
        }
        return pwr;
      } else {
        return "";
      }
    }
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

  /**
   * add a jar to the scripting environment<br>
   * Jython: added to sys.path<br>
   * JRuby: not yet supported<br>
   * JavaScript: not yet supported<br>
   * if no scripting active (API usage), jar is added to classpath if available
   * @param fpJar absolute path to a jar (relative: searched according to Extension concept,
   * but first on sys.path)
   * @return the absolute path to the jar or null, if not available
   */
  public static String load(String fpJar) {
    return load(fpJar, null);
  }

  /**
   * add a jar to the scripting environment or to classpath<br>
   * Jython: added to sys.path<br>
   * JRuby: only added to classpath<br>
   * JavaScript: only added to classpath<br>
   * if no scripting is active (API usage), jar is added to classpath if available<br>
   * additionally: fpJar/fpJarImagePath is added to ImagePath (not checked)
   * @param fpJar absolute path to a jar (relative: searched according to Extension concept,
   * but first on sys.path)
   * @param fpJarImagePath path relative to jar root inside jar
   * @return the absolute path to the jar or null, if not available
   */
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
      File fJarFound = rt.asExtension(fpJar);
      if (fJarFound != null) {
        fpJarFound = fJarFound.getAbsolutePath();
        rt.addToClasspath(fpJarFound);
      }
    }
    if (fpJarFound != null && fpJarImagePath != null) {
      ImagePath.addJar(fpJarFound, fpJarImagePath);
    }
    return fpJarFound;
  }

  /**
   * build a jar on the fly at runtime from a folder.<br>
   * special for Jython: if the folder contains a __init__.py on first level,
   * the folder will be copied to the jar root (hence preserving module folders)
   * @param targetJar absolute path to the created jar (parent folder must exist, jar is overwritten)
   * @param sourceFolder absolute path to a folder, the contained folder structure
   * will be copied to the jar root level
   * @return
   */
  public static boolean buildJarFromFolder(String targetJar, String sourceFolder) {
    log(lvl, "buildJarFromFolder: \nfrom Folder: %s\nto Jar: %s", sourceFolder, targetJar);
    File fJar = new File(targetJar);
    if (!fJar.getParentFile().exists()) {
      log(-1, "buildJarFromFolder: parent folder of Jar not available");
      return false;
    }
    File fSrc = new File(sourceFolder);
    if (!fSrc.exists() || !fSrc.isDirectory()) {
      log(-1, "buildJarFromFolder: source folder not available");
      return false;
    }
    String prefix = null;
    if (new File(fSrc, "__init__.py").exists() || new File(fSrc, "__init__$py.class").exists()) {
      prefix = fSrc.getName();
      if (prefix.endsWith("_")) {
        prefix = prefix.substring(0, prefix.length() - 1);
      }
    }
    return FileManager.buildJar(targetJar, new String[]{null}, new String[]{sourceFolder}, new String[]{prefix}, null);
  }

  private static JythonHelper doCompileJythonFolder(JythonHelper jython, File fSource) {
    String fpSource = FileManager.slashify(fSource.getAbsolutePath(), false);
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

  public static boolean popAsk(String msg) {
    return popAsk(msg, null);
  }

  public static boolean popAsk(String msg, String title) {
    if (title == null) {
      title = "... something to decide!";
    }
    int ret = JOptionPane.showConfirmDialog(null, msg, title, JOptionPane.YES_NO_OPTION);
    if (ret == JOptionPane.CLOSED_OPTION || ret == JOptionPane.NO_OPTION) {
      return false;
    }
    return true;
  }

//<editor-fold defaultstate="collapsed" desc="User prefs">
  private static final String prefNonSikuli = "nonSikuli_";
  
  /**
   * store a key-value-pair in Javas persistent preferences storage that is used by SikuliX to save settings and
   * information between IDE sessions<br>
   * this allows, to easily make some valuable information persistent
   *
   * @param key name of the item
   * @param value item content
   */
  public static void prefStore(String key, String value) {
    PreferencesUser.getInstance().put(prefNonSikuli + key, value);
  }

  /**
   * permanently remove the previously stored key-value-pair having the given key from Javas persistent preferences
   * storage that is used by SikuliX to save settings and information between IDE sessions<br>
   *
   * @param key name of the item to permanently remove
   * @return the item content that would be returned by prefLoad(key)
   */
  public static String prefRemove(String key) {
    String val = prefLoad(key);
    PreferencesUser.getInstance().remove(prefNonSikuli + key);
    return val;
  }
  
  /**
   * permanently remove all previously stored key-value-pairs (by prefsStore()) from Javas persistent preferences
   * storage that is used by SikuliX to save settings and information between IDE sessions<br>
   */
  public static void prefRemove() {
    PreferencesUser.getInstance().removeAll(prefNonSikuli);
  }
  
  /**
   * retrieve the value of a previously stored a key-value-pair from Javas persistent preferences storage that is used
   * by SikuliX to save settings and information between IDE sessions<br>
   *
   * @param key name of the item
   * @return the item content or empty string if not stored yet
   */
  public static String prefLoad(String key) {
    return PreferencesUser.getInstance().get(prefNonSikuli + key, "");
  }
  
  /**
   * retrieve the value of a previously stored a key-value-pair from Javas persistent preferences storage that is used
   * by SikuliX to save settings and information between IDE sessions<br>
   *
   * @param key name of the item
   * @param value the item content or the given value if not stored yet (default)
   * @return the item content or the given default
   */
  public static String prefLoad(String key, String value) {
    return PreferencesUser.getInstance().get(prefNonSikuli + key, value);
  }

  public static boolean importPrefs(String path) {
    return true;
  }
  
  public static boolean exportPrefs(String path) {
    return true;
  }
//</editor-fold>
  
//<editor-fold defaultstate="collapsed" desc="run stuff on a commandline">
  public static String run(String cmdline) {
    return run(new String[]{cmdline});
  }
  
  public static String run(String[] cmd) {
    return rt.runcmd(cmd);
  }
//</editor-fold>
  
  public static void popError(String message) {
    popError(message, "Sikuli");
  }

  public static void popError(String message, String title) {
    JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
  }

  private static class CompileJythonFilter implements FileManager.FileFilter {
    
    JythonHelper jython = null;
    
    public CompileJythonFilter(JythonHelper jython) {
      this.jython = jython;
    }

    @Override
    public boolean accept(File entry) {
      if (jython != null && entry.isDirectory()) {
        jython = Commands.doCompileJythonFolder(jython, entry);
      }
      return false;
    }
  }

  /**
   * the foo.py files in the given source folder are compiled to JVM-ByteCode-classfiles foo$py.class
   * and stored in the target folder (thus securing your code against changes).<br>
   * A folder structure is preserved. All files not ending as .py will be copied also.
   * The target folder might then be packed to a jar using buildJarFromFolder.<br>
   * Be aware: you will get no feedback about any compile problems,
   * so make sure your code compiles error free. Currently there is no support for running such a jar,
   * it can only be used with load()/import, but you might provide a simple script that does load()/import
   * and then runs something based on available functions in the jar code.
   * @param fpSource absolute path to a folder/folder-tree containing the stuff to be copied/compiled
   * @param fpTarget the folder that will contain the copied/compiled stuff (folder is first deleted)
   * @return false if anything goes wrong, true means should have worked
   */
  public static boolean compileJythonFolder(String fpSource, String fpTarget) {
    JythonHelper jython = JythonHelper.get();
    if (jython != null) {
      File fTarget = new File(fpTarget);
      FileManager.deleteFileOrFolder(fTarget);
      fTarget.mkdirs();
      if (!fTarget.exists()) {
        log(-1, "compileJythonFolder: target folder not available\n%", fTarget);
        return false;
      }
      File fSource = new File(fpSource);
      if (!fSource.exists()) {
        log(-1, "compileJythonFolder: source folder not available\n", fSource);
        return false;
      }
      if (fTarget.equals(fSource)) {
        log(-1, "compileJythonFolder: target folder cannot be the same as the source folder");
        return false;
      }
      FileManager.xcopy(fSource, fTarget);
      if (!jython.exec("import compileall")) {
        return false;
      }
      jython = doCompileJythonFolder(jython, fTarget);
      FileManager.traverseFolder(fTarget, new CompileJythonFilter(jython));
    }
    return false;
  }

//<editor-fold defaultstate="collapsed" desc="terminating">
  public static void endNormal(int retVal) {
    log(lvl, "endNormal: %d", retVal);
    cleanUp(retVal);
    System.exit(retVal);
  }
  
  public static void endWarning(int retVal) {
    log(lvl, "endWarning: %d", retVal);
    cleanUp(retVal);
    System.exit(retVal);
  }
  
  public static void endError(int retVal) {
    log(lvl, "endError: %d", retVal);
    cleanUp(retVal);
    System.exit(retVal);
  }
  
  private static void terminate(int retVal, String msg, Object... args) {
    p(msg, args);
    System.exit(retVal);
  }
  
  public static void terminate(int retVal) {
    Debug.error("***** Terminating SikuliX after a fatal error" + (retVal == 0 ? "*****\n" : " %d *****\n") + "It makes no sense to continue!\n" + "If you do not have any idea about the error cause or solution, run again\n" + "with a Debug level of 3. You might paste the output to the Q&A board.", retVal);
    cleanUp(0);
    System.exit(retVal);
  }
  
  public static void cleanUp(int n) {
    log(lvl, "cleanUp: %d", n);
    ScreenHighlighter.closeAll();
    Observing.cleanUp();
    Mouse.reset();
    Screen.getPrimaryScreen().getRobot().keyUp();
    HotkeyManager.reset();
  }
//</editor-fold>
  
  /**
   * Shows a dialog request to enter text in a multiline text field <br>
   * Though not all text might be visible, everything entered is delivered with the returned text <br>
   * The main purpose for this feature is to allow pasting text from somewhere preserving line breaks <br>
   *
   * @param msg the message to display.
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
    if (0 == JOptionPane.showConfirmDialog(null, pnl, title, JOptionPane.OK_CANCEL_OPTION)) {
      return ta.getText();
    } else {
      return "";
    }
  }

  public static void popup(String message) {
    popup(message, "Sikuli");
  }

  public static void popup(String message, String title) {
    JOptionPane.showMessageDialog(null, message, title, JOptionPane.PLAIN_MESSAGE);
  }

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
    return (String) JOptionPane.showInputDialog(null, msg, title, JOptionPane.PLAIN_MESSAGE, null, options, preset);
  }
	int i = 0;

  private static Region scr = new Screen();
  private static Region scrSaved = null;
  
  private static RunTime runTime = RunTime.get();

  /**
   *
   * @return true if we are on Java 8+
   */
  public static boolean isNashorn() {
    return runTime.isJava8();
  }

  /**
   * INTERNAL USE: call interface for JavaScript to be used with predefined functions
   * @param function the function's name
   * @param args the parameters
   * @return the object returned by the function or null
   */
  public static Object call(String function, Object... args) {
    Method m = null;
    Object retVal = null;
    int count = 0;
    for (Object aObj : args) {
      if (aObj == null || aObj.getClass().getName().endsWith("Undefined")) {
        break;
      }
			if (aObj instanceof String && ((String) aObj).contains("undefined")) {
				break;
			}
      count++;
    }
    Object[] newArgs = new Object[count];
    for(int n = 0; n < count; n++) {
      newArgs[n] = args[n];
    }
    try {
      m = Commands.class.getMethod(function, Object[].class);
      retVal = m.invoke(null, (Object) newArgs);
    } catch (Exception ex) {
      m = null;
    }
    return retVal;
  }
  
  public static Object run(Object... args) {
    String script = args[0].toString();
    String scriptArgs[] = new String[args.length - 1];
    if (scriptArgs.length > 0) {
      for (int i = 1; i < args.length; i++) {
        scriptArgs[i-1] = args[i].toString();
      }
    }
    return Runner.run(script, scriptArgs);
  }  

  public static Object circle(Object args) {
    return 0;
  }  
  
//<editor-fold defaultstate="collapsed" desc="conversions">
  private static boolean isNumber(Object aObj) {
    if (aObj instanceof Integer || aObj instanceof Long || aObj instanceof Float || aObj instanceof Double) {
      return true;
    }
    return false;
  }
  
  private static int getInteger(Object aObj, int deflt) {
    Integer val = deflt;
    if (aObj instanceof Integer || aObj instanceof Long) {
      val = (Integer) aObj;
    }
    if (aObj instanceof Float) {
      val = Math.round((Float) aObj);
    }
    if (aObj instanceof Double) {
      val = (int) Math.round((Double) aObj);
    }
    return val;
  }
  
  private static int getInteger(Object aObj) {
    return getInteger(aObj, 0);
  }
  
  private static double getNumber(Object aObj, Double deflt) {
    Double val = deflt;
    if (aObj instanceof Integer) {
      val = 0.0 + (Integer) aObj;
    } else if (aObj instanceof Long) {
      val = 0.0 + (Long) aObj;
    } else if (aObj instanceof Float) {
      val = 0.0 + (Float) aObj;
    } else if (aObj instanceof Double) {
      val = (Double) aObj;
    }
    return val;
  }
  
  private static double getNumber(Object aObj) {
    return getNumber(aObj, 0.0);
  }
//</editor-fold>
  
//<editor-fold defaultstate="collapsed" desc="use/use1">

  /**
   * all following undotted function calls will use the given screen or region 
   * until this is changed by a later use()<br>
   * -- no args: use Screen(0) (this is the default after start)<br>
   * -- a number: use Screen(number), Screen(0) if not valid<br>
   * -- a region: use the given region<br>
   * @param args
   * @return the used region
   */
    public static Region use(Object... args) {
    logCmd("use", args);
    scrSaved = null;
    return usex(args);
  }
  
  /**
   * same as use(), but only affects the next processed undotted function
   * after that the use() active before is restored
   * @param args see use()
   * @return the used region
   */
  public static Region use1(Object... args) {
    logCmd("use1", args);
    scrSaved = scr;
    return usex(args);
  }
  
  /**
   * INTERNAL USE: restore a saved use() after a use1() 
   */
  public static void restoreUsed() {
    if (scrSaved != null) {
      scr = scrSaved;
      scrSaved = null;
      log(lvl, "restored: %s", scr);
    }
  }
  
  private static Region usex(Object... args) {
    int len = args.length;
    int nScreen = -1;
    if (len == 0 || len > 1) {
      scr = new Screen();
      return scr;
    }
    nScreen = getInteger(args[0], -1);
    if (nScreen > -1) {
      scr = new Screen(nScreen);
    } else {
      Object oReg = args[0];
      if (oReg instanceof Region) {
        scr = (Region) oReg;
      }
    }
    return scr;
  }
//</editor-fold>
  
//<editor-fold defaultstate="collapsed" desc="wait/waitVanish/exists">

  /**
   * wait for the given visual to appear within the given wait time<br>
   * args [String|Pattern|Double, [Double, [Float]]] (max 3 args)<br>
   * arg1: String/Pattern to search or double time to wait (rest ignored)<br>
   * arg2: time to wait in seconds<br>
   * arg3: minimum similarity to use for search (overwrites Pattern setting)<br>
   * @param args
   * @return the match or throws FindFailed
   * @throws FindFailed
   */
    public static Match wait(Object... args) throws FindFailed {
    logCmd("wait", args);
    Object[] realArgs = waitArgs(args);
    return waitx((String) realArgs[0], (Pattern) realArgs[1], (Double) realArgs[2], (Float) realArgs[3]);
  }
  
  private static Match waitx(String image, Pattern pimage, double timeout, float score) throws FindFailed {
    Object aPattern = null;
    if (image != null) {
      if (score > 0) {
        aPattern = new Pattern(image).similar(score);
      } else {
        aPattern = image;
      }
    } else if (pimage != null) {
      aPattern = pimage;
    }
    if (aPattern != null) {
      if (timeout > -1.0) {
        return scr.wait(aPattern, timeout);
      }
      return scr.wait(aPattern);
    }
    return null;
  }
  
  private static Object[] waitArgs(Object... args) {
    int len = args.length;
    String image = "";
    float score = 0.0f;
    double timeout = -1.0f;
    boolean argsOK = true;
    Object[] realArgs = new Object[] {null, null, (Double) (-1.0), (Float) 0f};
    if (len == 0 || len > 3) {
      argsOK = false;
    } else {
      Object aObj = args[0];
      if (aObj == null) {
        return realArgs;
      }
      if (isJSON(aObj)) {
        aObj = fromJSON(aObj);
      }
      if (aObj instanceof String) {
        realArgs[0] = aObj;
      } else if (aObj instanceof Pattern) {
        realArgs[1] = aObj;
        if (len > 1 && isNumber(args[1])) {
          realArgs[2] = (Double) getNumber(args[1]);
        }
      } else if (isNumber(aObj)) {
        scr.wait(getNumber(aObj));
        return null;
      } else {
        argsOK = false;
      }
    }
    if (argsOK && len > 1 && realArgs[1] == null) {
      if (len > 2 && isNumber(args[2])) {
        score = (float) getNumber(args[2]) / 100.0f;
        if (score < 0.7) {
          score = 0.7f;
        } else if (score > 0.99) {
          score = 0.99f;
        }
      }
      if (score > 0.0f) {
        realArgs[3] = (Float) score;
      }
      if (len > 1 && isNumber(args[1])) {
        realArgs[2] = (Double) getNumber(args[1]);
      }
    }
    if (!argsOK) {
      throw new UnsupportedOperationException(
              "Commands.wait: parameters: String/Pattern:image, float:timeout, int:score");
    }
    return realArgs;
  }
  
  /**
   * wait for the given visual to vanish within the given wait time 
   * @param args see wait()
   * @return true if not there from beginning or vanished within wait time, false otherwise
   */
  public static boolean waitVanish(Object... args) {
    logCmd("waitVanish", args);
    Object aPattern;
    Object[] realArgs = waitArgs(args);
    String image = (String) realArgs[0];
    Pattern pimage = (Pattern) realArgs[1];
    double timeout = (Double) realArgs[2];
    float score = (Float) realArgs[3];
    if (image != null) {
      if (score > 0) {
        aPattern = new Pattern(image).similar(score);
      } else {
        aPattern = image;
      }
    } else {
      aPattern = pimage;
    }
    if (timeout > -1.0) {
      return scr.waitVanish(aPattern, timeout);
    }
    return scr.waitVanish(aPattern);
  }
  
  /**
   * wait for the given visual to appear within the given wait time 
   * @param args see wait()
   * @return the match or null if not found within wait time (no FindFailed exception)
   */
  public static Match exists(Object... args) {
    logCmd("exists", args);
    Match match = null;
    Object[] realArgs = waitArgs(args);
    if ((Double) realArgs[2] < 0.0) {
      realArgs[2] = 0.0;
    }
    try {
      match = waitx((String) realArgs[0], (Pattern) realArgs[1], (Double) realArgs[2], (Float) realArgs[3]);
    } catch (Exception ex) {
      return null;
    }
    return match;
  }
//</editor-fold>
  
//<editor-fold defaultstate="collapsed" desc="hover/click/doubleClick/rightClick">

  /**
   * move the mouse to the given location with a given offset<br>
   * 3 parameter configurations:<br>
   * --1: wait for a visual and move mouse to it (args see wait())<br>
   * --2: move to the given region/location/match with a given offset<br>
   * --3: move to the given offset relative to the last match of the region in use
   * @param args
   * @return the evaluated location to where the mouse should have moved 
   */
    public static Location hover(Object... args) {
    logCmd("hover", args);
    return hoverx(args);
  }
  
  private static Location hoverx(Object... args) {
    int len = args.length;
    Match aMatch;
    if (len == 0 || args[0] == null) {
      Mouse.move(scr.checkMatch());
      return Mouse.at();
    }
    if (len < 4) {
      Object aObj = args[0];
      Location loc = null;
      if (isJSON(aObj)) {
        aObj = fromJSON(aObj);
      }
      if (aObj instanceof String || aObj instanceof Pattern) {
        try {
          aMatch = wait(args);
          Mouse.move(aMatch.getTarget());
        } catch (Exception ex) {
          Mouse.move(scr.checkMatch());
        }
        return Mouse.at();
      } else if (aObj instanceof Region) {
        loc = ((Region) aObj).getTarget();
      } else if (aObj instanceof Location) {
        loc = (Location) aObj;
      }
      if (len > 1) {
        if (isNumber(aObj) && isNumber(args[1])) {
          Mouse.move(scr.checkMatch().offset(getInteger(aObj), getInteger(args[1])));
          return Mouse.at();
        } else if (len == 3 && loc != null && isNumber(args[1]) && isNumber(args[2])) {
          Mouse.move(loc.offset(getInteger(args[1], 0), getInteger(args[2], 0)));
          return Mouse.at();
        }
      }
      if (loc != null) {
        Mouse.move(loc);
        return Mouse.at();
      }
    }
    Mouse.move(scr.checkMatch());
    return Mouse.at();
  }
  
  /**
   * move the mouse with hover() and click using the left button
   * @param args see hover()
   * @return the location, where the click was done
   */
  public static Location click(Object... args) {
    logCmd("click", args);
    hoverx(args);
    Mouse.click(null, "L");
    return Mouse.at();
  }
  
  /**
   * move the mouse with hover() and double click using the left button
   * @param args see hover()
   * @return the location, where the double click was done
   */
  public static Location doubleClick(Object... args) {
    logCmd("doubleClick", args);
    hoverx(args);
    Mouse.click(null, "LD");
    return Mouse.at();
  }
  
  /**
   * move the mouse with hover() and do a right click
   * @param args see hover()
   * @return the location, where the right click was done
   */
  public static Location rightClick(Object... args) {
    logCmd("rightClick", args);
    hoverx(args);
    Mouse.click(null, "R");
    return Mouse.at();
  }
//</editor-fold>
  
//<editor-fold defaultstate="collapsed" desc="type/write/paste">

  /**
   * just doing a currentRegion.paste(text) (see paste())
   * @param args only one parameter being a String
   * @return true if paste() returned 1, false otherwise
   */
  
  public static boolean paste(Object... args) {
    logCmd("paste", args);
    Object[] realArgs = typeArgs(args);
    return 0 < scr.paste((String) realArgs[0]);
  }
  
  /**
   * just doing a currentRegion.write(text) (see write())
   * @param args only one parameter being a String
   * @return true if write() returned 1, false otherwise
   */
  public static boolean write(Object... args) {
    logCmd("write", args);
    Object[] realArgs = typeArgs(args);
    return 0 < scr.write((String) realArgs[0]);
  }
  
  private static Object[] typeArgs(Object... args) {
    Object[] realArgs = new Object[] {null};
    if (! (args[0] instanceof String)) {
      throw new UnsupportedOperationException("Commands.type/paste/write: parameters: String:text");
    }
    realArgs[0] = args[0];
    return realArgs;
  }
//</editor-fold>
  
//<editor-fold defaultstate="collapsed" desc="JSON support">

  /**
   * check wether the given object is in JSON format as ["ID", ...]
   * @param aObj
   * @return true if object is in JSON format, false otherwise
   */
    public static boolean isJSON(Object aObj) {
    if (aObj instanceof String) {
      return ((String) aObj).startsWith("[\"");
    }
    return false;
  }
  
  /**
   * experimental: create the real object from the given JSON<br>
   * take care: content length not checked if valid (risk for index out of bounds)<br>
   * planned to be used with a socket/RPC based interface to any framework (e.g. C#)
   * Region ["R", x, y, w, h]<br>
   * Location ["L", x, y]<br>
   * Match ["M", x, y, w, h, score, offx, offy]<br>
   * Screen ["S", x, y, w, h, id]<br>
   * Pattern ["P", "imagename", score, offx, offy]<br>
   * These real objects have a toJSON(), that returns these JSONs<br>
   * @param aObj
   * @return the real object or the given object if it is not one of these JSONs
   */
  public static Object fromJSON(Object aObj) {
    if (!isJSON(aObj)) {
      return aObj;
    }
    Object newObj = null;
    String[] json = ((String) aObj).split(",");
    String last = json[json.length-1];
    if (!last.endsWith("]")) {
      return aObj;
    } else {
      json[json.length-1] = last.substring(0, last.length()-1);
    }
    String oType = json[0].substring(2,3);
    if (!"SRML".contains(oType)) {
      return aObj;
    }
    if ("S".equals(oType)) {
      aObj = new Screen(intFromJSON(json, 5));
      ((Screen) aObj).setRect(rectFromJSON(json));
    } else if ("R".equals(oType)) {
      newObj = new Region(rectFromJSON(json));
    } else if ("M".equals(oType)) {
      double score = dblFromJSON(json, 5)/100;
      newObj = new Match(new Region(rectFromJSON(json)), score);
      ((Match) newObj).setTarget(intFromJSON(json, 6), intFromJSON(json, 7));
    } else if ("L".equals(oType)) {
      newObj = new Location(locFromJSON(json));
    } else if ("P".equals(oType)) {
      newObj = new Pattern(json[1]);
      ((Pattern) newObj).similar(fltFromJSON(json, 2));
      ((Pattern) newObj).targetOffset(intFromJSON(json, 3), intFromJSON(json, 4));
    }
    return newObj;
  }
  
  private static Rectangle rectFromJSON(String[] json) {
    int[] vals = new int[4];
    for (int n = 1; n < 5; n++) {
      try {
        vals[n-1] = Integer.parseInt(json[n].trim());
      } catch (Exception ex) {
        vals[n-1] = 0;
      }
    }
    return new Rectangle(vals[0], vals[1], vals[2], vals[3]);
  }
  
  private static Point locFromJSON(String[] json) {
    int[] vals = new int[2];
    for (int n = 1; n < 3; n++) {
      try {
        vals[n-1] = Integer.parseInt(json[n].trim());
      } catch (Exception ex) {
        vals[n-1] = 0;
      }
    }
    return new Point(vals[0], vals[1]);
  }
  
  private static int intFromJSON(String[] json, int pos) {
    try {
      return Integer.parseInt(json[pos].trim());
    } catch (Exception ex) {
      return 0;
    }
  }
  
  private static float fltFromJSON(String[] json, int pos) {
    try {
      return Float.parseFloat(json[pos].trim());
    } catch (Exception ex) {
      return 0;
    }
  }
  
  private static double dblFromJSON(String[] json, int pos) {
    try {
      return Double.parseDouble(json[pos].trim());
    } catch (Exception ex) {
      return 0;
    }
  }
//</editor-fold>
}
