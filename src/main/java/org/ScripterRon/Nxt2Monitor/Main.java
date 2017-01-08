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
import org.ScripterRon.Nxt2API.NxtException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.LogManager;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Nxt2Monitor will display the status of a Nxt2 peer node
 */
public class Main {

    /** Logger instance */
    public static final Logger log = LoggerFactory.getLogger("org.ScripterRon.Nxt2Monitor");

    /** File separator */
    public static String fileSeparator;

    /** Line separator */
    public static String lineSeparator;

    /** User home */
    public static String userHome;

    /** Operating system */
    public static String osName;

    /** Application identifier */
    public static String applicationID;

    /** Application name */
    public static String applicationName;

    /** Application version */
    public static String applicationVersion;

    /** Application properties */
    public static Properties properties;

    /** Data directory */
    public static String dataPath;

    /** Application properties file */
    private static File propFile;

    /** Main application window */
    public static MainWindow mainWindow;

    /** Server connections */
    public static List<Connection> connections = new ArrayList<>();

    /** Current server connection */
    public static Connection serverConnection;

    /** Default API port */
    public static int apiPort = 7876;

    /** Default administrator password */
    public static String adminPW = "dummy-password";

    /** Use HTTPS connections */
    public static boolean useSSL = true;

    /** Application lock file */
    private static RandomAccessFile lockFile;

    /** Application lock */
    private static FileLock fileLock;

    /** Deferred exception text */
    private static String deferredText;

    /** Deferred exception */
    private static Throwable deferredException;

    /**
     * Handles program initialization
     *
     * @param   args                Command-line arguments
     */
    public static void main(String[] args) {
        try {
            fileSeparator = System.getProperty("file.separator");
            lineSeparator = System.getProperty("line.separator");
            userHome = System.getProperty("user.home");
            osName = System.getProperty("os.name").toLowerCase();
            //
            // Process command-line options
            //
            dataPath = System.getProperty("nxt.datadir");
            if (dataPath == null) {
                if (osName.startsWith("win"))
                    dataPath = userHome+"\\Appdata\\Roaming\\Nxt2Monitor";
                else if (osName.startsWith("linux"))
                    dataPath = userHome+"/.Nxt2Monitor";
                else if (osName.startsWith("mac os"))
                    dataPath = userHome+"/Library/Application Support/Nxt2Monitor";
                else
                    dataPath = userHome+"/Nxt2Monitor";
            }
            //
            // Create the data directory if it doesn't exist
            //
            File dirFile = new File(dataPath);
            if (!dirFile.exists())
                dirFile.mkdirs();
            //
            // Open the application lock file
            //
            lockFile = new RandomAccessFile(dataPath+fileSeparator+".lock", "rw");
            fileLock = lockFile.getChannel().tryLock();
            if (fileLock == null) {
                JOptionPane.showMessageDialog(null, "Nxt2Monitor is already running", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
            //
            // Initialize the logging properties from 'logging.properties'
            //
            File logFile = new File(dataPath+fileSeparator+"logging.properties");
            if (logFile.exists()) {
                FileInputStream inStream = new FileInputStream(logFile);
                LogManager.getLogManager().readConfiguration(inStream);
            }
            //
            // Use the brief logging format
            //
            BriefLogFormatter.init();
            //
            // Process configuration file options
            //
            processConfig();
            //
            // Get the application build properties
            //
            Class<?> mainClass = Class.forName("org.ScripterRon.Nxt2Monitor.Main");
            try (InputStream classStream = mainClass.getClassLoader().getResourceAsStream("META-INF/application.properties")) {
                if (classStream == null)
                    throw new IllegalStateException("Application build properties not found");
                Properties applicationProperties = new Properties();
                applicationProperties.load(classStream);
                applicationID = applicationProperties.getProperty("application.id");
                applicationName = applicationProperties.getProperty("application.name");
                applicationVersion = applicationProperties.getProperty("application.version");
            }
            log.info(String.format("%s Version %s", applicationName, applicationVersion));
            log.info(String.format("Application data path: %s", dataPath));
            //
            // Load the saved application properties
            //
            propFile = new File(dataPath+fileSeparator+"Nxt2Monitor.properties");
            properties = new Properties();
            if (propFile.exists()) {
                try (FileInputStream in = new FileInputStream(propFile)) {
                    properties.load(in);
                }
            }
            //
            // Initialize the server connection list
            //
            if (connections.isEmpty()) {
                connections.add(new Connection("localhost", apiPort, adminPW));
            } else {
                connections.forEach((connection) -> {
                    if (connection.getPort() == -1)
                        connection.setPort(apiPort);
                    if (connection.getAdminPW().length() == 0)
                        connection.setAdminPW(adminPW);
                });
            }
            serverConnection = connections.get(0);
            //
            // Initialize the Nxt API library
            //
            Nxt.init(serverConnection.getHost(), serverConnection.getPort(), useSSL);
            //
            // Start the GUI
            //
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            javax.swing.SwingUtilities.invokeLater(() -> createAndShowGUI());
        } catch (Exception exc) {
            logException("Exception during program initialization", exc);
        }
    }

    /**
     * Create and show our application GUI
     *
     * This method is invoked on the AWT event thread to avoid timing
     * problems with other window events
     */
    private static void createAndShowGUI() {
        //
        // Use the normal window decorations as defined by the look-and-feel
        // schema
        //
        JFrame.setDefaultLookAndFeelDecorated(true);
        //
        // Create the main application window
        //
        mainWindow = new MainWindow();
        //
        // Show the application window
        //
        mainWindow.pack();
        mainWindow.setVisible(true);
    }

    /**
     * Shutdown and exit
     */
    public static void shutdown() {
        //
        // Save the application properties
        //
        saveProperties();
        //
        // Close the application lock file
        //
        try {
            fileLock.release();
            lockFile.close();
        } catch (IOException exc) {
        }
        //
        // All done
        //
        System.exit(0);
    }

    /**
     * Save the application properties
     */
    public static void saveProperties() {
        try {
            try (FileOutputStream out = new FileOutputStream(propFile)) {
                properties.store(out, "Nxt2Monitor Properties");
            }
        } catch (IOException exc) {
            Main.logException("Exception while saving application properties", exc);
        }
    }

    /**
     * Process the configuration file
     *
     * @throws      IllegalArgumentException    Invalid configuration option
     * @throws      IOException                 Unable to read configuration file
     */
    private static void processConfig() throws IOException, IllegalArgumentException {
        //
        // Use the defaults if there is no configuration file
        //
        File configFile = new File(dataPath+Main.fileSeparator+"Nxt2Monitor.conf");
        if (!configFile.exists())
            return;
        //
        // Process the configuration file
        //
        try (BufferedReader in = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line=in.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0 || line.charAt(0) == '#')
                    continue;
                int sep = line.indexOf('=');
                if (sep < 1)
                    throw new IllegalArgumentException(String.format("Invalid configuration option: %s", line));
                String option = line.substring(0, sep).trim().toLowerCase();
                String value = line.substring(sep+1).trim();
                switch (option) {
                    case "connect":
                        connections.add(new Connection(value));
                        break;
                    case "apiport":
                        apiPort = Integer.valueOf(value);
                        break;
                    case "adminpw":
                        adminPW = value;
                        break;
                    case "usessl":
                        useSSL = Boolean.valueOf(value);
                        break;
                    default:
                        throw new IllegalArgumentException(String.format("Invalid configuration option: %s", line));
                }
            }
        }
    }

    /**
     * Display a dialog when an exception occurs.
     *
     * @param       text        Text message describing the cause of the exception
     * @param       exc         The Java exception object
     */
    public static void logException(String text, Throwable exc) {
        if (SwingUtilities.isEventDispatchThread()) {
            StringBuilder string = new StringBuilder(512);
            //
            // Display our error message
            //
            string.append("<html><b>");
            string.append(text);
            string.append("</b><br>");
            //
            // Display the exception object
            //
            string.append("<b>");
            if (exc instanceof NxtException)
                string.append(exc.getMessage());
            else
                string.append(exc.toString());
            string.append("</b><br><br>");
            //
            // Display the stack trace
            //
            if (!(exc instanceof NxtException)) {
                StackTraceElement[] trace = exc.getStackTrace();
                int count = 0;
                for (StackTraceElement elem : trace) {
                    string.append(elem.toString());
                    string.append("<br>");
                    if (++count == 25)
                        break;
                }
            }
            string.append("</html>");
            JOptionPane.showMessageDialog(mainWindow, string, "Error", JOptionPane.ERROR_MESSAGE);
        } else if (deferredException == null) {
            deferredText = text;
            deferredException = exc;
            try {
                javax.swing.SwingUtilities.invokeAndWait(() -> {
                    Main.logException(deferredText, deferredException);
                    deferredException = null;
                    deferredText = null;
                });
            } catch (Exception logexc) {
                log.error("Unable to log exception during program initialization");
            }
        }
    }
}
