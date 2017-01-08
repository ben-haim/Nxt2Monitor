/*
 * Copyright 2016 Ronald W Hoffman.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ScripterRon.Nxt2Monitor;

import org.ScripterRon.Nxt2API.Nxt;

import java.io.IOException;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 * Set the server log level and communications log mask
 */
public class LoggingDialog extends JDialog implements ActionListener {

    /** Log level field */
    private final JComboBox<String> levelField;

    /**
     * Create the dialog
     *
     * @param       parent          Parent frame
     */
    public LoggingDialog(JFrame parent) {
        super(parent, "Set Server Logging", Dialog.ModalityType.DOCUMENT_MODAL);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        //
        // Create the log level field
        //
        String[] levelList = new String[] {"DEBUG", "INFO", "WARN", "ERROR"};
        levelField = new JComboBox<>(levelList);
        levelField.setEditable(false);
        levelField.setSelectedIndex(1);
        JPanel levelPane = new JPanel();
        levelPane.add(levelField);
        levelPane.add(new JLabel(" Log level", JLabel.LEFT));
        //
        // Create the buttons (Set Logging, Cancel)
        //
        JPanel buttonPane = new ButtonPane(this, 10, new String[] {"Set Logging", "set logging"},
                                                     new String[] {"Cancel", "cancel"});
        //
        // Set up the content pane
        //
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setOpaque(true);
        contentPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        contentPane.add(levelPane);
        contentPane.add(Box.createVerticalStrut(15));
        contentPane.add(buttonPane);
        setContentPane(contentPane);
    }

    /**
     * Show the server logging dialog
     *
     * @param       parent              Parent frame
     */
    public static void showDialog(JFrame parent) {
        try {
            LoggingDialog dialog = new LoggingDialog(parent);
            dialog.pack();
            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);
        } catch (Exception exc) {
            Main.logException("Exception while displaying dialog", exc);
        }
    }

    /**
     * Action performed (ActionListener interface)
     *
     * @param   ae              Action event
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        //
        // "set logging" - Set the logging level
        // "cancel"      - Cancel the request
        //
        try {
            String action = ae.getActionCommand();
            switch (action) {
                case "set logging":
                    setLogging();
                    setVisible(false);
                    dispose();
                    break;
                case "cancel":
                    setVisible(false);
                    dispose();
                    break;
            }
        } catch (Exception exc) {
            Main.log.error("Exception while processing action event", exc);
            Main.logException("Exception while processing action event", exc);
        }
    }

    /**
     * Set server logging
     */
    private void setLogging() {
        String level = (String)levelField.getSelectedItem();
        try {
            Nxt.setLogging(level, Main.adminPW);
        } catch (IOException exc) {
            Main.log.error("Unable to set server logging", exc);
            Main.logException("Unable to set server logging", exc);
        }
    }
}
