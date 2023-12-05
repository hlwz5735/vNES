package com.hlwz5735.vnes;

import com.hlwz5735.vnes.gui.vNES;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("vNES");
            frame.setSize(512, 480);
            frame.setLocation(400, 300);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);

            vNES mainPanel = new vNES();
            frame.add(mainPanel);
            mainPanel.init();
            new Thread(mainPanel).start();
        });
    }
}
