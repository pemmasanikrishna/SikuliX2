/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.sikulix.scripting.JythonHelper;
import com.sikulix.scripting.Runner;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class SXCommands {

  private static SXLog log = SX.getLogger("SXCommands");
  private static int lvl = SXLog.DEBUG;

  public static void logCmd(String cmd, Object... args) {
		String msg = cmd + ": ";
		if (args.length == 0) {
			log.log(lvl, msg + "no-args");
		} else {
			for (int i = 0; i < args.length; i++) {
				msg += "%s ";
			}
			log.log(lvl, msg, args);
		}
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
      File fJarFound = SX.asExtension(fpJar);
      if (fJarFound != null) {
        fpJarFound = fJarFound.getAbsolutePath();
        SX.addClassPath(fpJarFound);
      }
    }
    if (fpJarFound != null && fpJarImagePath != null) {
      SX.addImagePath(fpJarFound, fpJarImagePath);
    }
    return fpJarFound;
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

  public static void popup(String message) {
    popup(message, "Sikuli");
  }

  public static void popup(String message, String title) {
    JOptionPane.showMessageDialog(null, message, title, JOptionPane.PLAIN_MESSAGE);
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

  public static void popError(String message) {
    popError(message, "Sikuli");
  }

  public static void popError(String message, String title) {
    JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
  }

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

  public static String run(String cmdline) {
    return run(new String[]{cmdline});
  }
  
  public static String run(String[] cmd) {
    log.terminate(1, "run: not implemented");
    return "";
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
        jython = SXCommands.doCompileJythonFolder(jython, entry);
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
      Content.deleteFileOrFolder(fTarget);
      fTarget.mkdirs();
      if (!fTarget.exists()) {
        log.log(-1, "compileJythonFolder: target folder not available\n%", fTarget);
        return false;
      }
      File fSource = new File(fpSource);
      if (!fSource.exists()) {
        log.log(-1, "compileJythonFolder: source folder not available\n", fSource);
        return false;
      }
      if (fTarget.equals(fSource)) {
        log.log(-1, "compileJythonFolder: target folder cannot be the same as the source folder");
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
    log.log(lvl, "buildJarFromFolder: \nfrom Folder: %s\nto Jar: %s", sourceFolder, targetJar);
    File fJar = new File(targetJar);
    if (!fJar.getParentFile().exists()) {
      log.log(-1, "buildJarFromFolder: parent folder of Jar not available");
      return false;
    }
    File fSrc = new File(sourceFolder);
    if (!fSrc.exists() || !fSrc.isDirectory()) {
      log.log(-1, "buildJarFromFolder: source folder not available");
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

}
