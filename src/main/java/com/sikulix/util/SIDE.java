package com.sikulix.util;

import com.sikulix.api.Do;
import com.sikulix.api.Element;

import java.awt.*;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;

public class SIDE {

  public static void addComponentsToPane(Container pane) {
    pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

    addAButton("find", pane);
    addAButton("click", pane);
    addAButton("+", pane);
  }

  private static void addAButton(String text, Container container) {
    JButton button = new JButton(text);
    button.setAlignmentX(Component.LEFT_ALIGNMENT);
    container.add(button);
  }

  private static void createAndShowGUI() {
    //Create and set up the window.
    JFrame frame = new JFrame("BoxLayoutDemo");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    //Set up the content pane.
    addComponentsToPane(frame.getContentPane());

    //Display the window.
    frame.pack();
    Dimension size = frame.getSize();
    new Element(frame.getSize()).getCentered();
    frame.setVisible(true);
  }

  public static void main(String[] args) {
    //Schedule a job for the event-dispatching thread:
    //creating and showing this application's GUI.
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        createAndShowGUI();
      }
    });
  }
}
