package com.hlwz5735.vnes;

import com.hlwz5735.vnes.gui.NesPanel;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            final JFrame frame = new JFrame("vNES");
            frame.setSize(512, 480);
            frame.setLocation(400, 300);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);

            NesPanel mainPanel = new NesPanel();
            frame.setContentPane(mainPanel);
            mainPanel.init();
            new Thread(mainPanel).start();
        });
    }
}
