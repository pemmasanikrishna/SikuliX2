/*
 * Copyright 2015-2016, SikulixUtil.com
 * Released under the MIT License.
 */
package org.sikuli.util.visual;

import java.awt.Container;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class SplashFrame extends JFrame {

  private static JFrame splash = null;
  private static long start = 0;

  private JLabel lbl, txt;
  private Container pane;
  private int proSize;
  private int fw, fh;

  public SplashFrame(String type) {
    init(new String[]{type});
  }

  public SplashFrame(String[] args) {
    init(args);
  }
  private void init(String[] args) {
    setResizable(false);
    setUndecorated(true);
    pane = getContentPane();

    if ("download".equals(args[0])) {
      pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
      pane.add(new JLabel(" "));
      lbl = new JLabel("");
      lbl.setAlignmentX(CENTER_ALIGNMENT);
      pane.add(lbl);
      pane.add(new JLabel(" "));
      txt = new JLabel("... waiting");
      txt.setAlignmentX(CENTER_ALIGNMENT);
      pane.add(txt);
      fw = 350;
      fh = 80;
    }

    pack();
    setSize(fw, fh);
    setLocationRelativeTo(null);
    setVisible(true);
  }

  public void setProFile(String proFile) {
    lbl.setText("Downloading: " + proFile);
  }

  public void setProSize(int proSize) {
    this.proSize = proSize;
  }

  public void setProDone(int done) {
    if (done < 0) {
      txt.setText(" ..... failed !!!");
    } else if (proSize > 0) {
      txt.setText(done + " % out of " + proSize + " KB");
    } else {
      txt.setText(done + " KB out of ??? KB");
    }
    repaint();
  }

  public void closeAfter(int secs) {
    try {
      Thread.sleep(secs*1000);
    } catch (InterruptedException ex) {
    }
    setVisible(false);
  }
}
