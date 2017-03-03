package com.sikulix.util;

import com.sikulix.api.Do;
import com.sikulix.api.Element;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class SIDE {

  private static SXLog log = SX.getLogger("SX.SIDE");

  static List<Object[]> lines = new ArrayList<>();
  static int selected = 0;
  static JTextField fldStatus = new JTextField("status");

  static {
    for (int n = 0; n < 20; n++) {
      lines.add(new Object[] {"click", "parameters", null, null, null});
    }
  }

  private static int iButtonText = 0;
  private static int iParamText = 1;
  private static int iButton = 2;
  private static int iParam = 3;
  private static int iLineNo = 4;

  private static JButton getButton(int lineNo) {
    return (JButton) lines.get(lineNo - 1)[iButton];
  }

  private static void setButton(int lineNo, JButton button) {
    lines.get(lineNo - 1)[iButton] = button;
  }

  private static JTextField getParam(int lineNo) {
    return (JTextField) lines.get(lineNo - 1)[iParam];
  }

  private static void setParam(int lineNo, JTextField param) {
    lines.get(lineNo - 1)[iParam] = param;
  }

  private static JLabel getLineNo(int lineNo) {
    return (JLabel) lines.get(lineNo - 1)[iLineNo];
  }

  private static void setLineNo(int lineNo, JLabel label) {
    lines.get(lineNo - 1)[iLineNo] = label;
  }

  private static void addComponentsToPane(Container pane) {
    pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
    int n = 1;
    for(Object line[] : lines) {
      addALine(pane, n, line);
      n++;
    }
  }

  private static void addALine(Container container, int lineNo, Object... args) {
    JPanel line = new JPanel();
    line.setLayout(new BoxLayout(line, BoxLayout.LINE_AXIS));
    line.add(Box.createRigidArea(new Dimension(5, 0)));

    final JLabel lblLineNo = new JLabel(String.format("%d", lineNo));
    lblLineNo.setPreferredSize(new Dimension(20, 30));
    lblLineNo.setBackground(Color.white);
    lblLineNo.setOpaque(true);
    lblLineNo.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        super.mouseClicked(e);
        int lineNo = Integer.parseInt(lblLineNo.getText());
        if (selected > 0) {
          getLineNo(selected).setBackground(Color.white);
        }
        selected = lineNo;
        getLineNo(selected).setBackground(Color.cyan);
        fldStatus.setText(String.format("(%d) %s", selected, getParam(selected).getText()));
      }
    });
    line.add(lblLineNo);

    JButton btnAction = new JButton((String) args[iButtonText]);
    line.add(btnAction);

    JTextField txtParam = new JTextField((String) args[iParamText], 50);
    txtParam.setEditable(false);
    line.add(txtParam);

    line.setAlignmentX(Component.LEFT_ALIGNMENT);
    container.add(line);
    setButton(lineNo, btnAction);
    setParam(lineNo, txtParam);
    setLineNo(lineNo, lblLineNo);
  }

  private static void createAndShowGUI() {
    JFrame frame = new JFrame("BoxLayoutDemo");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    Container contentPane = frame.getContentPane();
    contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

    JPanel pLines = new JPanel();
    addComponentsToPane(pLines);
    JScrollPane scrollPane = new JScrollPane(pLines);
    scrollPane.setLayout(new ScrollPaneLayout());
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.setPreferredSize(new Dimension(800, Math.min((1 + lines.size()) * 30, (int) (Do.on().h * 0.8))));
    contentPane.add(scrollPane);

    JPanel footer = new JPanel();
    footer.setLayout(new BoxLayout(footer, BoxLayout.LINE_AXIS));
    footer.add(new JButton("+"));
    footer.add(fldStatus);
    contentPane.add(footer);

    frame.pack();
    Dimension size = frame.getSize();
    Element centered = new Element(frame.getSize()).getCentered();
    frame.setLocation(centered.x, centered.y);
    frame.setVisible(true);
  }

  public static void main(String[] args) {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        createAndShowGUI();
      }
    });
  }
}
