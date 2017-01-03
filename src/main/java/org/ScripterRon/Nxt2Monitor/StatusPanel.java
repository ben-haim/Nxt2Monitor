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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

/**
 * This is the status panel for the main window.  It displays information about the current
 * block chain and peer connections
 */
public class StatusPanel extends JPanel implements ActionListener, Runnable {

    /** Peer connection states */
    enum State {
        NOT_CONNECTED(0),
        CONNECTED(1),
        DISCONNECTED(2);

        private static final Map<Integer, State> codeMap = new HashMap<>();
        static {
            for (State state : values()) {
                codeMap.put(state.code, state);
            }
        }

        private final int code;

        private State(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static State fromCode(int code) {
            State state = codeMap.get(code);
            return (state != null ? state : State.NOT_CONNECTED);
        }
    }

    /** Transaction types */
    public static final Map<Integer, Map<Integer, String>> transactionTypes = new HashMap<>();

    /** Chains */
    public static final Map<Integer, String> chains = new HashMap<>();

    /** Block status table column names */
    private static final String[] blockColumnNames = {
        "Date", "Height", "Block", "Version", "Tx Count", "Generator"};

    /** Block status table column classes */
    private static final Class<?>[] blockColumnClasses = {
        Date.class, Integer.class, String.class, Integer.class, Integer.class, String.class};

    /** Block status table column types */
    private static final int[] blockColumnTypes = {
        SizedTable.DATE, SizedTable.INTEGER, SizedTable.ID, SizedTable.INTEGER, SizedTable.INTEGER, SizedTable.ID};

    /** Block status table model */
    private final BlockTableModel blockTableModel;

    /** Block table pop-up menu */
    private final JPopupMenu blockTablePopup;

    /** Block status table */
    private final JTable blockTable;

    /** Block status scroll pane */
    private final JScrollPane blockScrollPane;

    /** Connection table column names */
    private static final String[] connectionColumnNames = {
        "Address", "Version", "Platform", "Services", "Status"};

    /** Connection table column classes */
    private static final Class<?>[] connectionColumnClasses = {
        String.class, String.class, String.class, String.class, String.class};

    /** Connection table column types */
    private static final int[] connectionColumnTypes = {
        SizedTable.ADDRESS, SizedTable.APPLICATION, SizedTable.APPLICATION, SizedTable.SERVICES, SizedTable.STATUS};

    /** Connection table model */
    private final ConnectionTableModel connectionTableModel;

    /** Connection table pop-up menu */
    private final JPopupMenu connectionTablePopup;

    /** Connection table */
    private final JTable connectionTable;

    /** Connection scroll pane */
    private final JScrollPane connectionScrollPane;

    /** NRS node field */
    private final JLabel nodeField;

    /** Chain height field */
    private final JLabel chainHeightField;

    /** Connections field */
    private final JLabel connectionsField;

    /** Status update shutdown */
    private boolean shutdown = false;

    /** Status update peer */
    private Response statusPeer;

    /** Status update block */
    private Response statusBlock;

    /** Status update identifier */
    private long statusId;

    /** Nxt epoch (milliseconds since January 1, 1970) */
    private long epochBeginning;

    /** Event registration token */
    private long eventToken;

    /**
     * Create the status panel
     */
    public StatusPanel() {
        super(new BorderLayout());
        setOpaque(true);
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        JPanel tablePane = new JPanel();
        tablePane.setLayout(new BoxLayout(tablePane, BoxLayout.Y_AXIS));
        tablePane.setBackground(Color.WHITE);
        TableMouseListener mouseListener = new TableMouseListener();
        //
        // Create the connection table
        //
        connectionTablePopup = new PopupMenu(this, new String[] {"Copy Address", "copy address"},
                                                   new String[] {"Blacklist Peer", "blacklist peer"});
        connectionTableModel = new ConnectionTableModel(connectionColumnNames, connectionColumnClasses);
        connectionTable = new SizedTable(connectionTableModel, connectionColumnTypes);
        connectionTable.setRowSorter(new TableRowSorter<>(connectionTableModel));
        connectionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        connectionTable.addMouseListener(mouseListener);
        connectionScrollPane = new JScrollPane(connectionTable);
        tablePane.add(Box.createGlue());
        tablePane.add(new JLabel("<html><h3>Peer Connections</h3></html>"));
        tablePane.add(connectionScrollPane);
        //
        // Create the block status table
        //
        blockTablePopup = new PopupMenu(this, new String[] {"Copy Block ID", "copy block id"},
                                              new String[] {"Copy Generator ID", "copy generator id"},
                                              new String[] {"Show Transactions", "show transactions"});
        blockTableModel = new BlockTableModel(blockColumnNames, blockColumnClasses);
        blockTable = new SizedTable(blockTableModel, blockColumnTypes);
        blockTable.setRowSorter(new TableRowSorter<>(blockTableModel));
        blockTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        blockTable.addMouseListener(mouseListener);
        blockScrollPane = new JScrollPane(blockTable);
        tablePane.add(Box.createGlue());
        tablePane.add(new JLabel("<html><h3>Recent Blocks</h3></html>"));
        tablePane.add(blockScrollPane);
        tablePane.add(Box.createGlue());
        //
        // Create the status pane containing the NRS node, chain height
        // and number of peer connections
        //
        nodeField = new JLabel("", JLabel.CENTER);
        chainHeightField = new JLabel("", JLabel.CENTER);
        connectionsField = new JLabel("", JLabel.CENTER);

        JPanel statusPane = new JPanel();
        statusPane.setLayout(new BoxLayout(statusPane, BoxLayout.Y_AXIS));
        statusPane.setOpaque(true);
        statusPane.setBackground(Color.WHITE);
        statusPane.add(nodeField);
        statusPane.add(chainHeightField);
        statusPane.add(connectionsField);
        statusPane.add(Box.createVerticalStrut(20));
        //
        // Set up the content pane
        //
        add(statusPane, BorderLayout.NORTH);
        add(tablePane, BorderLayout.CENTER);
    }

    /**
     * Action performed (ActionListener interface)
     *
     * @param       ae              Action event
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        //
        // "blacklist peer"             - Blacklist inbound peer
        // "copy address"               - Copy peer address
        // "copy block id"              - Copy the block identifier
        // "copy generator id"          - Copy the generator identifier
        // "show transactions"          - Show block transactions
        //
        try {
            String action = ae.getActionCommand();
            StringSelection sel;
            Clipboard cb;
            int row;
            switch (action) {
                case "blacklist peer":
                    row = connectionTable.getSelectedRow();
                    if (row >= 0) {
                        row = connectionTable.convertRowIndexToModel(row);
                        String address = (String)connectionTableModel.getValueAt(row, 0);
                        Request.blacklistPeer(address);
                    }
                    break;
                case "copy address":
                    row = connectionTable.getSelectedRow();
                    if (row >= 0) {
                        row = connectionTable.convertRowIndexToModel(row);
                        sel = new StringSelection((String)connectionTableModel.getValueAt(row, 0));
                        cb = Toolkit.getDefaultToolkit().getSystemClipboard();
                        cb.setContents(sel, null);
                    }
                    break;
                case "copy block id":
                    row = blockTable.getSelectedRow();
                    if (row >= 0) {
                        row = blockTable.convertRowIndexToModel(row);
                        sel = new StringSelection((String)blockTableModel.getValueAt(row, 2));
                        cb = Toolkit.getDefaultToolkit().getSystemClipboard();
                        cb.setContents(sel, null);
                    }
                    break;
                case "copy generator id":
                    row = blockTable.getSelectedRow();
                    if (row >= 0) {
                        row = blockTable.convertRowIndexToModel(row);
                        sel = new StringSelection((String)blockTableModel.getValueAt(row, 5));
                        cb = Toolkit.getDefaultToolkit().getSystemClipboard();
                        cb.setContents(sel, null);
                    }
                    break;
                case "show transactions":
                    row = blockTable.getSelectedRow();
                    if (row >= 0) {
                        row = blockTable.convertRowIndexToModel(row);
                        String blockId = (String)blockTableModel.getValueAt(row, 2);
                        TransactionDialog.showDialog(Main.mainWindow, blockId);
                    }
                    break;
            }
        } catch (IOException exc) {
            Main.logException("I/O exception while processing Nxt API request",exc);
        } catch (Exception exc) {
            Main.log.error("Exception while processing action event", exc);
            Main.logException("Exception while processing action event", exc);
        }
    }

    /**
     * Update the node status
     */
    private void updateStatus() {
        Response block = blockTableModel.getChainHead();
        nodeField.setText(String.format("<html><b>NRS node: [%s]:%d</b></html>",
                                        Main.serverConnection.getHost(),
                                        Main.serverConnection.getPort()));
        chainHeightField.setText(String.format("<html><b>Chain height: %d</b></html>",
                                        block.getInt("height")));
        connectionsField.setText(String.format("<html><b>Peer connections: %d</b></html>",
                                        connectionTableModel.getActiveCount()));
    }

    /**
     * Shutdown server status updates
     */
    public void shutdown() {
        shutdown = true;
        //
        // Cancel our event listener (this will cause the event wait to complete)
        //
        try {
            List<String> eventList = new ArrayList<>();
            Request.eventRegister(eventList, eventToken, false, true);
        } catch (IOException exc) {
            Main.log.error("Unable to cancel event listener", exc);
            Main.logException("Unable to cancel event listener", exc);
        }
    }

    /**
     * Start the Nxt event handler
     */
    public void startEventHandler() {
        Thread eventThread = new Thread(this, "Nxt Event Handler");
        eventThread.setDaemon(true);
        eventThread.start();
    }

    /**
     * Process server events
     */
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        //
        // Get the initial server status
        //
        try {
            //
            // Get the Nxt constants
            //
            Response constants = Request.getConstants();
            epochBeginning = constants.getLong("epochBeginning");
            //
            // Get the chains
            //
            Set<Map.Entry<String, Object>> chainSet = constants.getObject("chains").entrySet();
            chainSet.forEach(entry -> {
                chains.put(((Long)entry.getValue()).intValue(), entry.getKey());
            });
            //
            // Get the transaction types
            //
            Set<Map.Entry<String, Object>> typeSet = constants.getObject("transactionTypes").entrySet();
            typeSet.forEach(entry -> {
                int type = Integer.valueOf(entry.getKey());
                Map<String, Object> subtypes = (Map<String, Object>)((Map<String, Object>)entry.getValue()).get("subtypes");
                Set<Map.Entry<String, Object>> subtypeSet = subtypes.entrySet();
                Map<Integer, String> transactionSubtypes = new HashMap<>();
                subtypeSet.forEach(subentry -> {
                    int subtype = Integer.valueOf(subentry.getKey());
                    String name = (String)((Map<String, Object>)subentry.getValue()).get("name");
                    transactionSubtypes.put(subtype, name);
                });
                transactionTypes.put(type, transactionSubtypes);
            });
            //
            // Add the last 25 blocks to the block table
            //
            List<Response> blockList = Request.getBlocks(0, 24, false);
            //
            // Add connected peers to the connection table
            //
            List<Response> peerList = Request.getPeers(State.CONNECTED.name());
            //
            // Update the status panel
            //
            SwingUtilities.invokeAndWait(() -> {
                blockTableModel.blocksAdded(blockList);
                connectionTableModel.peersAdded(peerList);
                updateStatus();
            });
            //
            // Register our events
            //
            List<String> eventList = new ArrayList<>();
            eventList.add("Peer.ADD_ACTIVE_PEER");
            eventList.add("Peer.CHANGE_ACTIVE_PEER");
            eventList.add("Peer.CHANGE_ANNOUNCED_ADDRESS");
            eventList.add("Peer.BLACKLIST");
            eventList.add("Peer.UNBLACKLIST");
            eventList.add("Block.BLOCK_PUSHED");
            eventList.add("Block.BLOCK_POPPED");
            Response eventResponse = Request.eventRegister(eventList, 0, false, false);
            eventToken = eventResponse.getLong("token");
        } catch (InterruptedException | InvocationTargetException exc) {
            Main.logException("Unable to perform status update", exc);
            Main.log.error("Unable to perform status update", exc);
            shutdown = true;
        } catch (IOException exc) {
            Main.log.error("Unable to get server state", exc);
            Main.logException("Unable to get server state", exc);
            shutdown = true;
        } catch (Exception exc) {
            Main.log.error("Exception while getting server state", exc);
            Main.logException("Exception while getting server state", exc);
            shutdown = true;
        }
        //
        // Process server events
        //
        Response peer;
        List<String> pendingConnections = new ArrayList<>();
        while (!shutdown) {
            try {
                //
                // Wait for an event
                //
                List<Event> eventList = Request.eventWait(eventToken, 60);
                if (shutdown)
                    break;
                //
                // Add pending connections to the event list
                //
                if (!pendingConnections.isEmpty()) {
                    pendingConnections.forEach(address ->
                        eventList.add(new Event("Peer.ADD_ACTIVE_PEER", address)));
                    pendingConnections.clear();
                }
                //
                // Process the events
                //
                for (Event event : eventList) {
                    String eventId = event.getIds().get(0);
                    switch (event.getName()) {
                        case "Peer.ADD_ACTIVE_PEER":
                            statusPeer = Request.getPeer(eventId);
                            if (State.fromCode(statusPeer.getInt("state")) == State.CONNECTED) {
                                SwingUtilities.invokeAndWait(() ->
                                        connectionTableModel.peerAdded(statusPeer, pendingConnections));
                            }
                            break;
                        case "Peer.CHANGE_ACTIVE_PEER":
                        case "Peer.CHANGE_ANNOUNCED_ADDRESS":
                            peer = Request.getPeer(eventId);
                            statusPeer = connectionTableModel.getPeer(eventId);
                            if (statusPeer != null) {
                                SwingUtilities.invokeAndWait(() -> connectionTableModel.peerUpdated(
                                            statusPeer.getString("address"),
                                            State.fromCode(statusPeer.getInt("state")) == State.CONNECTED ?
                                                    State.DISCONNECTED : State.CONNECTED,
                                            false));
                            } else {
                                statusPeer = peer;
                                if (State.fromCode(statusPeer.getInt("state")) == State.CONNECTED)
                                    SwingUtilities.invokeAndWait(() ->
                                            connectionTableModel.peerAdded(statusPeer, pendingConnections));
                            }
                            break;
                        case "Peer.BLACKLIST":
                            statusPeer = connectionTableModel.getPeer(eventId);
                            if (statusPeer != null) {
                                SwingUtilities.invokeAndWait(() -> connectionTableModel.peerUpdated(
                                            statusPeer.getString("address"), State.DISCONNECTED, true));
                            }
                            break;
                        case "Peer.UNBLACKLIST":
                            statusPeer = connectionTableModel.getPeer(eventId);
                            if (statusPeer != null) {
                                SwingUtilities.invokeAndWait(() -> connectionTableModel.peerUpdated(
                                            statusPeer.getString("address"),
                                            State.fromCode(statusPeer.getInt("state")), false));
                            }
                            break;
                        case "Block.BLOCK_PUSHED":
                            try {
                                statusBlock = Request.getBlock(eventId, false);
                                SwingUtilities.invokeAndWait(() -> blockTableModel.blockAdded(statusBlock));
                            } catch (IOException exc) {
                                Main.log.error("Unable to get block", exc);
                            }
                            break;
                        case "Block.BLOCK_POPPED":
                            statusId = Utils.stringToId(eventId);
                            SwingUtilities.invokeAndWait(() -> blockTableModel.blockRemoved(statusId));
                            break;
                    }
                }
                SwingUtilities.invokeAndWait(() -> updateStatus());
            } catch (InterruptedException | InvocationTargetException exc) {
                Main.log.error("Unable to perform status update", exc);
                Main.logException("Unable to perform status update", exc);
                shutdown = true;
            } catch (IOException exc) {
                Main.log.error("Unable to process server event", exc);
                Main.logException("Unable to process server event", exc);
                shutdown = true;
            } catch (Exception exc) {
                Main.log.error("Exception while processing server event", exc);
                Main.logException("Exception while processing server event", exc);
            }
        }
    }

    /**
     * Mouse listener for the connection and block tables
     */
    private class TableMouseListener extends MouseAdapter {

        /**
         * Mouse button released
         *
         * We will select the table row at the mouse pointer for a popup trigger event
         * if the row is not already selected.  We will then display the popup menu.
         * This allows the action listener to determine the row for the popup event.
         *
         * @param   event           Mouse event
         */
        @Override
        public void mouseReleased(MouseEvent event) {
            if (event.isPopupTrigger()) {
                JTable table = (JTable)event.getSource();
                int row = table.rowAtPoint(event.getPoint());
                if (row >= 0 && !table.isRowSelected(row))
                    table.changeSelection(row, 0, false, false);
                if (table == connectionTable)
                    connectionTablePopup.show(event.getComponent(), event.getX(), event.getY());
                else
                    blockTablePopup.show(event.getComponent(), event.getX(), event.getY());
            }
        }
    }

    /**
     * Table model for the block status table
     */
    private class BlockTableModel extends AbstractTableModel {

        /** Column names */
        private final String[] columnNames;

        /** Column classes */
        private final Class<?>[] columnClasses;

        /** Block list */
        private final List<Response> blockList = new LinkedList<>();

        /** Block map */
        private final Map<Long, Response> blockMap = new ConcurrentHashMap<>();

        /**
         * Create the table model
         *
         * @param       columnName          Column names
         * @param       columnClasses       Column classes
         */
        public BlockTableModel(String[] columnNames, Class<?>[] columnClasses) {
            super();
            this.columnNames = columnNames;
            this.columnClasses = columnClasses;
        }

        /**
         * Get the number of columns in the table
         *
         * @return                  The number of columns
         */
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        /**
         * Get the column class
         *
         * @param       column      Column number
         * @return                  The column class
         */
        @Override
        public Class<?> getColumnClass(int column) {
            return columnClasses[column];
        }

        /**
         * Get the column name
         *
         * @param       column      Column number
         * @return                  Column name
         */
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        /**
         * Get the number of rows in the table
         *
         * @return                  The number of rows
         */
        @Override
        public int getRowCount() {
            return blockList.size();
        }

        /**
         * Get the value for a cell
         *
         * @param       row         Row number
         * @param       column      Column number
         * @return                  Returns the object associated with the cell
         */
        @Override
        public Object getValueAt(int row, int column) {
            if (row >= blockList.size())
                return null;
            Object value;
            Response block = blockList.get(row);
            //
            // Get the value for the requested cell
            //
            switch (column) {
                case 0:                             // Date
                    value = new Date(block.getLong("timestamp") * 1000 + epochBeginning);
                    break;
                case 1:                             // Height
                    value = block.getInt("height");
                    break;
                case 2:                             // Block identifier
                    value = block.getString("block");
                    break;
                case 3:                             // Block version
                    value = block.getInt("version");
                    break;
                case 4:                             // Block transaction count
                    value = block.getInt("numberOfTransactions");
                    break;
                case 5:                             // Block generator
                    value = block.getString("generatorRS");
                    break;
                default:
                    throw new IndexOutOfBoundsException("Table column "+column+" is not valid");
            }
            return value;
        }

        /**
         * New blocks have been added to the block chain
         *
         * @param   blocks          Block list
         */
        public void blocksAdded(List<Response> blocks) {
            blocks.forEach(block -> {
                blockList.add(block);
                blockMap.put(block.getId("block"), block);
            });
            fireTableDataChanged();
        }

        /**
        * A new block has been added to the block chain
        *
        * @param    block           New block
        */
        public void blockAdded(Response block) {
            blockList.add(0, block);
            blockMap.put(block.getId("block"), block);
            fireTableRowsInserted(0, 0);
        }

        /**
         * An existing block has been removed from the block chain
         *
         * @param   blockId         Block identifier
         */
        public void blockRemoved(long blockId) {
            Response block = blockMap.remove(blockId);
            if (block != null) {
                int row = blockList.indexOf(block);
                if (row >= 0) {
                    blockList.remove(row);
                    fireTableRowsDeleted(row, row);
                }
            }
        }

        /**
         * Return the block chain head
         *
         * @return                  Block chain head
         */
        public Response getChainHead() {
            return blockList.get(0);
        }

        /**
         * Return the block for the specified table row
         *
         * @param   row             Table row
         * @return                  Block
         */
        public Response getBlock(int row) {
            return blockList.get(row);
        }
    }

    /**
     * Table model for the connections table
     */
    private class ConnectionTableModel extends AbstractTableModel {

        /** Column names */
        private final String[] columnNames;

        /** Column classes */
        private final Class<?>[] columnClasses;

        /** Connection list */
        private final List<Response> connectionList = new LinkedList<>();

        /** Connection map */
        private final Map<String, Response> connectionMap = new ConcurrentHashMap<>();

        /** Active connection count */
        private int activeCount = 0;

        /**
         * Create the table model
         *
         * @param       columnName          Column names
         * @param       columnClasses       Column classes
         */
        public ConnectionTableModel(String[] columnNames, Class<?>[] columnClasses) {
            super();
            this.columnNames = columnNames;
            this.columnClasses = columnClasses;
        }

        /**
         * Get the number of columns in the table
         *
         * @return                  The number of columns
         */
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        /**
         * Get the column class
         *
         * @param       column      Column number
         * @return                  The column class
         */
        @Override
        public Class<?> getColumnClass(int column) {
            return columnClasses[column];
        }

        /**
         * Get the column name
         *
         * @param       column      Column number
         * @return                  Column name
         */
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        /**
         * Get the number of rows in the table
         *
         * @return                  The number of rows
         */
        @Override
        public int getRowCount() {
            return connectionList.size();
        }

        /**
         * Get the value for a cell
         *
         * @param       row         Row number
         * @param       column      Column number
         * @return                  Returns the object associated with the cell
         */
        @Override
        public Object getValueAt(int row, int column) {
            if (row >= connectionList.size())
                return null;
            Object value = null;
            Response peer = connectionList.get(row);
            switch (column) {
                case 0:                         // Network address
                    value = peer.getString("announcedAddress");
                    if (((String)value).isEmpty())
                        value = peer.getString("address");
                    break;
                case 1:                         // Application version
                    value = peer.getString("application") + " " + peer.getString("version");
                    break;
                case 2:                         // Platform
                    value = peer.getString("platform");
                    break;
                case 3:                         // Services
                    List<String> services = peer.getStringList("services");
                    StringBuilder builder = new StringBuilder(32);
                    services.forEach((service) -> {
                        if (builder.length() > 0)
                            builder.append(",");
                        builder.append(service);
                    });
                    value = builder.toString();
                    break;
                case 4:                         // Status
                    if (peer.getBoolean("blacklisted")) {
                        value = "Blacklisted";
                    } else {
                        State state = State.fromCode(peer.getInt("state"));
                        switch (state) {
                            case CONNECTED:
                                value = "Connected";
                                break;
                            case DISCONNECTED:
                                value = "Disconnected";
                                break;
                            default:
                                value = "Not connected";
                        }
                    }
                    break;
            }
            return value;
        }

        /**
         * New peers have been added
         *
         * @param   peerList        Peers to add
         */
        public void peersAdded(List<Response> peerList) {
            peerList.stream()
                    .filter(peer -> connectionMap.get(peer.getString("address")) == null &&
                                    !peer.getString("version").isEmpty())
                    .forEach(peer -> {
                        connectionList.add(peer);
                        connectionMap.put(peer.getString("address"), peer);
                        if (State.fromCode(peer.getInt("state")) == State.CONNECTED)
                            activeCount++;
            });
            fireTableDataChanged();
        }
        /**
         * A new peer has been added
         *
         * @param   peer                Peer to add
         * @param   pendingConnections  List of pending connections
         */
        public void peerAdded(Response peer, List<String> pendingConnections) {
            String address = peer.getString("address");
            if (peer.getString("version").isEmpty()) {
                if (!pendingConnections.contains(address))
                    pendingConnections.add(address);
                return;
            }
            State peerState = State.fromCode(peer.getInt("state"));
            Response mapPeer = connectionMap.get(address);
            if (mapPeer == null) {
                connectionList.add(peer);
                connectionMap.put(address, peer);
                if (peerState == State.CONNECTED)
                    activeCount++;
                fireTableRowsInserted(connectionList.size()-1, connectionList.size()-1);
            } else {
                State mapState = State.fromCode(mapPeer.getInt("state"));
                if (mapState == State.CONNECTED && peerState != State.CONNECTED)
                    activeCount--;
                else if (mapState != State.CONNECTED && peerState == State.CONNECTED)
                    activeCount++;
                int row = connectionList.indexOf(mapPeer);
                connectionList.set(row, peer);
                connectionMap.put(address, peer);
                fireTableRowsUpdated(row, row);
            }
        }

        /**
         * An existing peer has been updated
         *
         * @param   address         Peer network address
         * @param   state           New peer state
         * @param   blaocklisted    TRUE if the peer is blacklisted
         */
        public void peerUpdated(String address, State state, boolean blacklisted) {
            Response peer = connectionMap.get(address);
            if (peer != null) {
                State peerState = State.fromCode(peer.getInt("state"));
                if (peerState == State.CONNECTED && state != State.CONNECTED)
                    activeCount--;
                else if (peerState != State.CONNECTED && state == State.CONNECTED)
                    activeCount++;
                int row = connectionList.indexOf(peer);
                peer.getObjectMap().put("state", (long)state.getCode());
                peer.getObjectMap().put("blacklisted", blacklisted);
                fireTableRowsUpdated(row, row);
            }
        }

        /**
         * Return an existing peer
         *
         * @param   address         Peer network address
         * @return                  Peer or null if peer not found
         */
        public Response getPeer(String address) {
            return connectionMap.get(address);
        }

        /**
         * Return the active connection count
         *
         * @return                  Active connection count
         */
        public int getActiveCount() {
            return activeCount;
        }
    }
}
