/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.util;

import com.sikulix.api.Commands;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import org.sikuli.util.PreferencesUser;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

//import java.io.FilenameFilter;

public class FileChooser {

  private static SXLog log = SX.getLogger("SX.FileChooser");

  static final int FILES = JFileChooser.FILES_ONLY;
  static final int DIRS = JFileChooser.DIRECTORIES_ONLY;
  static final int DIRSANDFILES = JFileChooser.FILES_AND_DIRECTORIES;
  static final int SAVE = FileDialog.SAVE;
  static final int LOAD = FileDialog.LOAD;
  Frame _parent;
  boolean accessingAsFile = false;
  String fileType = "";

  public FileChooser(Frame parent) {
    _parent = parent;
  }

  public FileChooser(Frame parent, String type) {
    _parent = parent;
    fileType = type;
  }

  public FileChooser(Frame parent, boolean accessingAsFile) {
    _parent = parent;
    this.accessingAsFile = accessingAsFile;
  }

  public File show(String title) {
    File ret = showFileChooser(title, LOAD, DIRSANDFILES);
    return ret;
  }

  public File load() {
    String type = "Sikuli Script (*.sikuli, *.skl)";
    String title = "Open a Sikuli Script";
    File ret = showFileChooser(title, LOAD, DIRSANDFILES, new SikulixFileFilter(type, "o"));
    return ret;
  }

  public File save() {
    String type = "Sikuli Script (*.sikuli)";
    String title = "Save a Sikuli Script";
    File ret = showFileChooser(title, SAVE, DIRS, new SikulixFileFilter(type, "s"));
    return ret;
  }

  public File export() {
    String type = "Sikuli packed Script (*.skl)";
    String title = "Export as Sikuli packed Script";
    File ret = showFileChooser(title, SAVE, FILES, new SikulixFileFilter(type, "e"));
    return ret;
  }

  public File loadImage() {
    File ret = showFileChooser("Load Image File", LOAD, FILES,
            new FileNameExtensionFilter("Image files (jpg, png)", "jpg", "jpeg", "png"));
    return ret;
  }

  private File showFileChooser(String title, int mode, int selectionMode, Object... filters) {
    String last_dir = PreferencesUser.getInstance().get("LAST_OPEN_DIR", "");
    log.debug("showFileChooser: %s at %s", title.split(" ")[0], last_dir);
    JFileChooser fchooser = null;
    File fileChoosen = null;
    while (true) {
      fchooser = new JFileChooser();
      if (!last_dir.isEmpty()) {
        fchooser.setCurrentDirectory(new File(last_dir));
      }
      fchooser.setSelectedFile(null);
      if (SX.isMac() && selectionMode == DIRS) {
        selectionMode = DIRSANDFILES;
      }
      fchooser.setFileSelectionMode(selectionMode);
      fchooser.setDialogTitle(title);
      String btnApprove = "Select";
      if (mode == FileDialog.SAVE) {
        fchooser.setDialogType(JFileChooser.SAVE_DIALOG);
        btnApprove = "Save";
      }
      boolean shouldTraverse = false;
      if (filters.length == 0) {
        fchooser.setAcceptAllFileFilterUsed(true);
        shouldTraverse = true;
      } else {
        fchooser.setAcceptAllFileFilterUsed(false);
        for (Object filter : filters) {
          if (filter instanceof SikulixFileFilter) {
            fchooser.addChoosableFileFilter((SikulixFileFilter) filter);
          } else {
            fchooser.setFileFilter((FileNameExtensionFilter) filter);
            shouldTraverse = true;
          }
        }
      }
      if (shouldTraverse && SX.isMac()) {
        fchooser.putClientProperty("JFileChooser.packageIsTraversable", "always");
      }

      if (fchooser.showDialog(_parent, btnApprove) != JFileChooser.APPROVE_OPTION) {
        return null;
      }
      fileChoosen = fchooser.getSelectedFile();
      if (mode == FileDialog.LOAD) {
        if (SX.isNotSet(fileType) && !isValidScript(fileChoosen)) {
          // folders must contain a valid scriptfile
          Commands.popError("Folder not a valid SikuliX script\nTry again.");
          last_dir = fileChoosen.getAbsolutePath();
          continue;
        }
        break;
      }
    }
    PreferencesUser.getInstance().put("LAST_OPEN_DIR", fileChoosen.getParent());
    return fileChoosen;
  }

  private boolean isValidScript(File f) {
    String[] endings = new String[]{".py", ".rb", ".js"};
    String fName = f.getName();
    if (fName.endsWith(".skl")) {
      return true;
    }
    if (fName.endsWith(".sikuli")) {
      fName = fName.substring(0, fName.length() - 7);
    }
    boolean valid = false;
    for (String ending : endings) {
      if (new File(f, fName + ending).exists()) {
        return true;
      }
    }
    return false;
  }

  private static boolean isExt(String fName, String givenExt) {
    int i = fName.lastIndexOf('.');
    if (i > 0) {
      if (fName.substring(i + 1).toLowerCase().equals(givenExt)) {
        return true;
      }
    }
    return false;
  }

  class SikulixFileFilter extends FileFilter {

    private String _type, _desc;

    public SikulixFileFilter(String desc, String type) {
      _type = type;
      _desc = desc;
    }

    @Override
    public boolean accept(File f) {
      if ("o".equals(_type) && (isExt(f.getName(), "sikuli") || isExt(f.getName(), "skl"))) {
        return true;
      }
      if ("s".equals(_type) && isExt(f.getName(), "sikuli")) {
        return true;
      }
      if ("e".equals(_type)) {
        if (isExt(f.getName(), "skl")) {
          return true;
        }
        if (SX.isMac() && isExt(f.getName(), "sikuli")) {
          return false;
        }
      }
      if (f.isDirectory()) {
        return true;
      }
      return false;
    }

    @Override
    public String getDescription() {
      return _desc;
    }
  }
}
