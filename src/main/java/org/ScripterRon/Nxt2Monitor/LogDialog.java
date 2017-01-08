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
import java.util.List;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

/**
 * Dialog to display the server log
 */
public class LogDialog extends JDialog implements ActionListener {

    /**
     * Create the dialog
     *
     * @param   parent              Parent frame
     */
    public LogDialog(JFrame parent) {
        super(parent, "View Log", Dialog.ModalityType.DOCUMENT_MODAL);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        //
        // Create the message field
        //
        JTextArea logField = new JTextArea(50, 120);
        logField.setLineWrap(false);
        JScrollPane scrollPane = new JScrollPane(logField);
        //
        // Load the log messages
        //
        try {
            List<String> messages = Nxt.getLog(1000, Main.adminPW);
            messages.forEach((logmsg) -> {
                String msg = logmsg.trim();
                StringBuilder sb = new StringBuilder(msg.length());
                for (int i=0; i<msg.length(); i++) {
                    int ch = msg.codePointAt(i);
                    if (Character.isValidCodePoint(ch)) {
                        if (Character.isWhitespace(ch)) {
                            sb.appendCodePoint(ch);
                        } else {
                            Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
                            if (!Character.isISOControl(ch) && block!=null && block!=Character.UnicodeBlock.SPECIALS)
                                sb.appendCodePoint(ch);
                            else
                                sb.append('!');
                        }
                        if (Character.isSupplementaryCodePoint(ch))
                            i++;
                    } else {
                        sb.append('!');
                    }
                }
                sb.append('\n');
                logField.append(sb.toString());
            });
        } catch (IOException exc) {
            Main.log.error("Unable to get server log messages", exc);
            Main.logException("Unable to get server log messages", exc);
        }
        //
        // Create the buttons (Done)
        //
        JPanel buttonPane = new ButtonPane(this, 10, new String[] {"Done", "done"});
        //
        // Set up the content pane
        //
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setOpaque(true);
        contentPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        contentPane.add(scrollPane);
        contentPane.add(Box.createVerticalStrut(15));
        contentPane.add(buttonPane);
        setContentPane(contentPane);
    }

    /**
     * Show the log message dialog
     *
     * @param       parent              Parent frame
     */
    public static void showDialog(JFrame parent) {
        try {
            LogDialog dialog = new LogDialog(parent);
            dialog.pack();
            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);
        } catch (Exception exc) {
            Main.log.error("Exception while displaying dialog", exc);
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
        // "done"       - All done
        //
        try {
            String action = ae.getActionCommand();
            switch (action) {
                case "done":
                    setVisible(false);
                    dispose();
                    break;
            }
        } catch (Exception exc) {
            Main.log.error("Exception while processing action event", exc);
            Main.logException("Exception while processing action event", exc);
        }
    }
}
