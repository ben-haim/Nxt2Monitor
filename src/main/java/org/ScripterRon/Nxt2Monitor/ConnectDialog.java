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

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 * Display the server connection dialog
 */
public class ConnectDialog extends JDialog implements ActionListener {

    /** Server list field */
    private final JComboBox<String> serverField;

    /** Server connection */
    private Connection serverConnection = null;

    /**
     * Create the dialog
     *
     * @param       parent          Parent frame
     */
    public ConnectDialog(JFrame parent) {
        super(parent, "Server Connect", Dialog.ModalityType.DOCUMENT_MODAL);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        //
        // Create the address field
        //
        String[] serverList = new String[Main.connections.size()];
        int index = 0;
        for (Connection connection : Main.connections)
            serverList[index++] = connection.toString();
        serverField = new JComboBox<>(serverList);
        serverField.setEditable(true);
        serverField.setSelectedIndex(-1);
        serverField.setPreferredSize(new Dimension(340, 25));
        JPanel serverPane = new JPanel();
        serverPane.add(new JLabel("Server  ", JLabel.RIGHT));
        serverPane.add(serverField);
        //
        // Create the buttons (Connect, Cancel)
        //
        JPanel buttonPane = new ButtonPane(this, 10, new String[] {"Connect", "connect"},
                                                     new String[] {"Cancel", "cancel"});
        //
        // Set up the content pane
        //
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setOpaque(true);
        contentPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        contentPane.add(serverPane);
        contentPane.add(Box.createVerticalStrut(15));
        contentPane.add(buttonPane);
        setContentPane(contentPane);
    }

    /**
     * Show the server connect dialog
     *
     * @param       parent              Parent frame
     * @return                          Server connection or null
     */
    public static Connection showDialog(JFrame parent) {
        Connection connection = null;
        try {
            ConnectDialog dialog = new ConnectDialog(parent);
            dialog.pack();
            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);
            connection = dialog.serverConnection;
        } catch (Exception exc) {
            Main.log.error("Exception while displaying dialog", exc);
            Main.logException("Exception while displaying dialog", exc);
        }
        return connection;
    }

    /**
     * Action performed (ActionListener interface)
     *
     * @param   ae              Action event
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        //
        // "connect"    - Connect to the server
        // "cancel"     - Cancel the connect request
        //
        try {
            String action = ae.getActionCommand();
            switch (action) {
                case "connect":
                    String connect = (String)serverField.getSelectedItem();
                    if (connect != null)
                        connect = connect.trim();
                    if (connect == null || connect.length()==0) {
                        JOptionPane.showMessageDialog(this, "You must select a server connection",
                                                      "Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        serverConnection = new Connection(connect);
                        if (serverConnection.getPort() == -1)
                            serverConnection.setPort(Main.apiPort);
                        if (serverConnection.getAdminPW().length() == 0)
                            serverConnection.setAdminPW(Main.adminPW);
                        setVisible(false);
                        dispose();
                    }
                    break;
                case "cancel":
                    setVisible(false);
                    dispose();
                    break;
            }
        } catch (IllegalArgumentException exc) {
            JOptionPane.showMessageDialog(this, "Invalid server connection", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception exc) {
            Main.log.error("Exception while processing action event", exc);
            Main.logException("Exception while processing action event", exc);
        }
    }
}
