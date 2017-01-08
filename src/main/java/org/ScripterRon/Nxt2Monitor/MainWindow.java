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

import org.ScripterRon.Nxt2API.Chain;
import org.ScripterRon.Nxt2API.Nxt;
import org.ScripterRon.Nxt2API.Response;
import org.ScripterRon.Nxt2API.Utils;

import java.io.IOException;
import java.util.List;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * This is the main application window
 */
public final class MainWindow extends JFrame implements ActionListener {

    /** Main window is minimized */
    private boolean windowMinimized = false;

    /** Main window status panel */
    private StatusPanel statusPanel;

    /**
     * Create the application window
     */
    public MainWindow() {
        //
        // Create the frame
        //
        super("Nxt2 Node Monitor");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        //
        // Position the window using the saved position from the last time
        // the program was run
        //
        int frameX = 320;
        int frameY = 10;
        String propValue = Main.properties.getProperty("window.main.position");
        if (propValue != null) {
            int sep = propValue.indexOf(',');
            frameX = Integer.parseInt(propValue.substring(0, sep));
            frameY = Integer.parseInt(propValue.substring(sep+1));
        }
        setLocation(frameX, frameY);
        //
        // Size the window using the saved size from the last time
        // the program was run
        //
        int frameWidth = 640;
        int frameHeight = 580;
        propValue = Main.properties.getProperty("window.main.size");
        if (propValue != null) {
            int sep = propValue.indexOf(',');
            frameWidth = Math.max(frameWidth, Integer.parseInt(propValue.substring(0, sep)));
            frameHeight = Math.max(frameHeight, Integer.parseInt(propValue.substring(sep+1)));
        }
        setPreferredSize(new Dimension(frameWidth, frameHeight));
        //
        // Create the application menu bar
        //
        JMenuBar menuBar = new JMenuBar();
        menuBar.setOpaque(true);
        menuBar.setBackground(new Color(230,230,230));
        //
        // Add the "File" menu to the menu bar
        //
        // The "File" menu contains "Connect" and "Exit"
        //
        menuBar.add(new Menu(this, "File", new String[] {"Connect Server", "connect server"},
                                           new String[] {"Exit", "exit"}));
        //
        // Add the "View" menu to the menu bar
        //
        // The "View" menu contains "Account Ledger", "Forging Generators", "Server Log" and "Server Trace"
        //
        menuBar.add(new Menu(this, "View", new String[] {"Bundlers", "view bundlers"},
                                           new String[] {"Forging Generators", "view forging"},
                                           new String[] {"Server Log", "view log"}));
        //
        // Add the "Action" menu to the menu bar
        //
        // The "Action" menu contains "Blacklist Peer", "Connect Peer" and "Set Logging"
        //
        menuBar.add(new Menu(this, "Action", new String[] {"Blacklist Peer", "blacklist peer"},
                                             new String[] {"Connect Peer", "connect peer"},
                                             new String[] {"Set Logging", "set logging"}));
        //
        // Add the "Help" menu to the menu bar
        //
        // The "Help" menu contains "About"
        //
        menuBar.add(new Menu(this, "Help", new String[] {"About", "about"}));
        //
        // Add the menu bar to the window frame
        //
        setJMenuBar(menuBar);
        //
        // Set up the status pane
        //
        statusPanel = new StatusPanel();
        setContentPane(statusPanel);
        //
        // Receive WindowListener events
        //
        addWindowListener(new ApplicationWindowListener());
        //
        // Start the Nxt event handler
        //
        statusPanel.startEventHandler();
    }

    /**
     * Action performed (ActionListener interface)
     *
     * @param       ae              Action event
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        //
        // "about"              - Display information about this program
        // "blacklist peer"     - Blacklist a peer
        // "connect peer"       - Connect to a peer
        // "connect server"     - Connect to a different server
        // "exit"               - Exit the program
        // "set logging"        - Set server logging
        // "view bundlers"      - View bundlers
        // "view forging"       - View forging generators
        // "view log"           - View the server log
        //
        try {
            String action = ae.getActionCommand();
            switch (action) {
                case "blacklist peer":
                    blacklistPeer();
                    break;
                case "connect peer":
                    connectPeer();
                    break;
                case "connect server":
                    connectServer();
                    break;
                case "set logging":
                    LoggingDialog.showDialog(this);
                    break;
                case "view bundlers":
                    viewBundlers();
                    break;
                case "view forging":
                    viewForging();
                    break;
                case "view log":
                    LogDialog.showDialog(this);
                    break;
                case "exit":
                    exitProgram();
                    break;
                case "about":
                    aboutNxtMonitor();
                    break;
            }
        } catch (Exception exc) {
            Main.log.error("Exception while processing action event", exc);
            Main.logException("Exception while processing action event", exc);
        }
    }

    /**
     * Connect to a different server
     */
    private void connectServer() {
        Connection connection = ConnectDialog.showDialog(Main.mainWindow);
        if (connection != null) {
            statusPanel.shutdown();
            SwingUtilities.invokeLater(() -> {
                Main.serverConnection = connection;
                try {
                    Nxt.init(Main.serverConnection.getHost(), Main.serverConnection.getPort(), Main.useSSL);
                } catch (IOException exc) {
                    Main.log.error("Unable to switch to new server", exc);
                    Main.logException("Unable to switch to new server", exc);
                    exitProgram();
                }
                statusPanel = new StatusPanel();
                setContentPane(statusPanel);
                revalidate();
                statusPanel.startEventHandler();
            });
        }
    }

    /**
     * Connect a peer
     */
    private void connectPeer() {
        String address = JOptionPane.showInputDialog(this, "Enter the announced address for the peer",
                                                     "Announced Address", JOptionPane.QUESTION_MESSAGE);
        if (address != null && address.length() > 0) {
            try {
                Nxt.addPeer(address, Main.adminPW);
            } catch (IOException exc) {
                Main.log.error("Unable to connect peer", exc);
                Main.logException("Unable to connect peer", exc);
            }
        }
    }

    /**
     * Blacklist a peer
     */
    private void blacklistPeer() {
        String address = JOptionPane.showInputDialog(this, "Enter the announced address for the peer",
                                                     "Announced Address", JOptionPane.QUESTION_MESSAGE);
        if (address != null && address.length() > 0) {
            try {
                Nxt.blacklistPeer(address, Main.adminPW);
            } catch (IOException exc) {
                Main.log.error("Unable to blacklist peer", exc);
                Main.logException("Unable to blacklist peer", exc);
            }
        }
    }

    /**
     * View the server forging status
     */
    private void viewForging() {
        try {
            List<Response> generators = Nxt.getForging(Main.adminPW);
            StringBuilder sb = new StringBuilder(1000);
            int count = 0;
            for (Response generator : generators) {
                if (count == 10) {
                    sb.append("\n    ...More");
                    break;
                }
                String line = String.format("Generator %s: Time to hit %,d seconds",
                                            generator.getString("accountRS"), generator.getLong("remaining"));
                if (count > 0)
                    sb.append("\n");
                sb.append(line);
                count++;
            }
            if (count == 0)
                JOptionPane.showMessageDialog(this, "Server is not forging",
                                              "Generators", JOptionPane.INFORMATION_MESSAGE);
            else
                JOptionPane.showMessageDialog(this, sb.toString(), "Generators", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException exc) {
            Main.log.error("Unable to get forging status", exc);
            Main.logException("Unable to get forging status", exc);
        }
    }

    /**
     * View server bundler status
     */
    private void viewBundlers() {
        try {
            List<Response> bundlers = Nxt.getBundlers(Main.adminPW);
            StringBuilder sb = new StringBuilder(1000);
            int count = 0;
            for (Response bundler : bundlers) {
                if (count == 10) {
                    sb.append("\n    ...More");
                    break;
                }
                Chain chain = Nxt.getChain(bundler.getInt("chain"));
                String line = String.format("%s bundler %s: Rate %s, Overpay %s, Limit %s",
                        chain.getName(), bundler.getString("bundlerRS"),
                        Utils.nqtToString(bundler.getLong("minRateNQTPerFXT"), chain.getDecimals()),
                        Utils.nqtToString(bundler.getLong("overpayFQTPerFXT"), 8),
                        Utils.nqtToString(bundler.getLong("totalFeesLimitFQT"), 8));
                if (count > 0)
                    sb.append("\n");
                sb.append(line);
                count++;
            }
            if (count == 0)
                JOptionPane.showMessageDialog(this, "Server is not bundline",
                                              "Bundlers", JOptionPane.INFORMATION_MESSAGE);
            else
                JOptionPane.showMessageDialog(this, sb.toString(), "Bundlers", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException exc) {
            Main.log.error("Unable to get bundler status", exc);
            Main.logException("Unable to get bundler status", exc);
        }
    }

    /**
     * Exit the application
     */
    private void exitProgram() {
        //
        // Stop status updates
        //
        statusPanel.shutdown();
        //
        // Remember the current window position and size unless the window
        // is minimized
        //
        if (!windowMinimized) {
            Point p = Main.mainWindow.getLocation();
            Dimension d = Main.mainWindow.getSize();
            Main.properties.setProperty("window.main.position", p.x+","+p.y);
            Main.properties.setProperty("window.main.size", d.width+","+d.height);
        }
        //
        // Shutdown and exit
        //
        Main.shutdown();
    }

    /**
     * Display information about the NxtMonitor application
     */
    private void aboutNxtMonitor() {
        StringBuilder info = new StringBuilder(256);
        info.append(String.format("<html>%s Version %s<br>",
                    Main.applicationName, Main.applicationVersion));

        info.append("<br>User name: ");
        info.append(System.getProperty("user.name"));

        info.append("<br>Home directory: ");
        info.append(System.getProperty("user.home"));

        info.append("<br><br>OS: ");
        info.append(System.getProperty("os.name"));

        info.append("<br>OS version: ");
        info.append(System.getProperty("os.version"));

        info.append("<br>OS patch level: ");
        info.append(System.getProperty("sun.os.patch.level"));

        info.append("<br><br>Java vendor: ");
        info.append(System.getProperty("java.vendor"));

        info.append("<br>Java version: ");
        info.append(System.getProperty("java.version"));

        info.append("<br>Java home directory: ");
        info.append(System.getProperty("java.home"));

        info.append("<br>Java class path: ");
        info.append(System.getProperty("java.class.path"));

        info.append("<br><br>Current Java memory usage: ");
        info.append(String.format("%,.3f MB", (double)Runtime.getRuntime().totalMemory()/(1024.0*1024.0)));

        info.append("<br>Maximum Java memory size: ");
        info.append(String.format("%,.3f MB", (double)Runtime.getRuntime().maxMemory()/(1024.0*1024.0)));

        info.append("</html>");
        JOptionPane.showMessageDialog(this, info.toString(), "About Nxt2 Node Monitor",
                                      JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Listen for window events
     */
    private class ApplicationWindowListener extends WindowAdapter {

        /**
         * Create the window listener
         */
        public ApplicationWindowListener() {
        }

        /**
         * Window has gained the focus (WindowListener interface)
         *
         * @param       we              Window event
         */
        @Override
        public void windowGainedFocus(WindowEvent we) {
        }

        /**
         * Window has been minimized (WindowListener interface)
         *
         * @param       we              Window event
         */
        @Override
        public void windowIconified(WindowEvent we) {
            windowMinimized = true;
        }

        /**
         * Window has been restored (WindowListener interface)
         *
         * @param       we              Window event
         */
        @Override
        public void windowDeiconified(WindowEvent we) {
            windowMinimized = false;
        }

        /**
         * Window is closing (WindowListener interface)
         *
         * @param       we              Window event
         */
        @Override
        public void windowClosing(WindowEvent we) {
            try {
                exitProgram();
            } catch (Exception exc) {
                Main.logException("Exception while closing application window", exc);
            }
        }
    }
}
