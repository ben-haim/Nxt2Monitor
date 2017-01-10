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
import org.ScripterRon.Nxt2API.IdentifierException;
import org.ScripterRon.Nxt2API.Nxt;
import org.ScripterRon.Nxt2API.Response;
import org.ScripterRon.Nxt2API.Transaction;
import org.ScripterRon.Nxt2API.TransactionType;
import org.ScripterRon.Nxt2API.Utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;

/**
 * Display the transactions in a block
 */
public class TransactionDialog extends JDialog implements ActionListener {

    /** Transaction table column names */
    private static final String[] columnNames = {
        "Transaction ID", "Type", "Sender", "Recipient", "Amount", "Fee", "Chain"};

    /** Transaction table column classes */
    private static final Class<?>[] columnClasses = {
        String.class, String.class, String.class, String.class, Long.class, Long.class, String.class};

    /** Transaction table column types */
    private static final int[] columnTypes = {
        SizedTable.ID, SizedTable.TYPE, SizedTable.ID, SizedTable.ID, SizedTable.AMOUNT,
        SizedTable.AMOUNT, SizedTable.CHAIN};

    /** Transaction table model */
    private final TransactionTableModel tableModel;

    /** Transaction table */
    private final JTable table;

    /** Transaction table popup menu */
    private final JPopupMenu tablePopup;

    /**
     * Create the dialog
     *
     * @param       parent          Parent frame
     * @param       transactions    Block transactions
     */
    public TransactionDialog(JFrame parent, List<Response> transactions) {
        super(parent, "Block Transactions", Dialog.ModalityType.DOCUMENT_MODAL);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        TableMouseListener mouseListener = new TableMouseListener();
        //
        // Create the transaction table
        //
        tablePopup = new PopupMenu(this, new String[] {"View Transaction", "view transaction"});
        tableModel = new TransactionTableModel(columnNames, columnClasses, transactions);
        table = new SizedTable(tableModel, columnTypes);
        table.setRowSorter(new TableRowSorter<>(tableModel));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setPreferredScrollableViewportSize(new Dimension(900, 400));
        table.addMouseListener(mouseListener);
        JScrollPane scrollPane = new JScrollPane(table);
        //
        // Create the table pane
        //
        JPanel tablePane = new JPanel();
        tablePane.setBackground(Color.WHITE);
        tablePane.setLayout(new BoxLayout(tablePane, BoxLayout.Y_AXIS));
        tablePane.add(Box.createVerticalStrut(25));
        tablePane.add(scrollPane);
        tablePane.add(Box.createVerticalStrut(25));
        //
        // Create the buttons (Done)
        //
        JPanel buttonPane = new ButtonPane(this, 10, new String[] {"Done", "done"});
        buttonPane.setBackground(Color.WHITE);
        //
        // Set up the content pane
        //
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setOpaque(true);
        contentPane.setBackground(Color.WHITE);
        contentPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPane.add(tablePane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.SOUTH);
        setContentPane(contentPane);
    }

    /**
     * Show the block transactions dialog
     *
     * @param       parent              Parent frame
     * @param       blockId             Block identifier
     */
    public static void showDialog(JFrame parent, String blockId) {
        try {
            //
            // Get the block transactions
            //
            Response block = Nxt.getBlock(blockId, true);
            List<Response> blockTransactions = block.getObjectList("transactions");
            List<Response> transactions = new ArrayList<>();
            for (Response tx : blockTransactions) {
                if (tx.getInt("type") == -1) {
                    Response attachment = tx.getObject("attachment");
                    Chain chain = Nxt.getChain(attachment.getInt("chain"));
                    List<String> hashList = attachment.getStringList("childTransactionFullHashes");
                    for (String hash : hashList) {
                        transactions.add(Nxt.getTransaction(Utils.parseHexString(hash), chain));
                    }
                } else {
                    transactions.add(tx);
                }
            }
            //
            // Display the dialog
            //
            TransactionDialog dialog = new TransactionDialog(parent, transactions);
            dialog.pack();
            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);
        } catch (IOException exc) {
            Main.log.error("Unable to get block transactions", exc);
            Main.logException("Unable to get block transactions", exc);
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
        // "done"               - Done displaying block transactions
        // "view transaction"   - Show transaction details
        //
        try {
            String action = ae.getActionCommand();
            switch (action) {
                case "done":
                    setVisible(false);
                    dispose();
                    break;
                case "view transaction":
                    int row = table.getSelectedRow();
                    if (row >= 0) {
                        row = table.convertRowIndexToModel(row);
                        Transaction tx = tableModel.getTransaction(row);
                        JOptionPane.showMessageDialog(this, tx.toString(), "Transaction Details",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                    break;
            }
        } catch (Exception exc) {
            Main.log.error("Exception while processing action event", exc);
            Main.logException("Exception while processing action event", exc);
        }
    }

    /**
     * Mouse listener for the transaction table
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
                JTable popupTable = (JTable)event.getSource();
                if (popupTable == table) {
                    int row = table.rowAtPoint(event.getPoint());
                    if (row >= 0 && !table.isRowSelected(row))
                        table.changeSelection(row, 0, false, false);
                        tablePopup.show(event.getComponent(), event.getX(), event.getY());
                }
            }
        }
    }

    /**
     * Transaction table model
     */
    private class TransactionTableModel extends AbstractTableModel {

        /** Column names */
        private final String[] columnNames;

        /** Column classes */
        private final Class<?>[] columnClasses;

        /** Block transactions */
        private List<Transaction> transactions;

        /**
         * Create the transaction table model
         *
         * @param       columnName          Column names
         * @param       columnClasses       Column classes
         * @param       blockTransactions   Block transactions
         */
        public TransactionTableModel(String[] columnNames, Class<?>[] columnClasses,
                                     List<Response> blockTransactions) {
            super();
            if (columnNames.length != columnClasses.length)
                throw new IllegalArgumentException("Number of names not same as number of classes");
            this.columnNames = columnNames;
            this.columnClasses = columnClasses;
            try {
                this.transactions = Transaction.processTransactions(blockTransactions);
            } catch (Exception exc) {
                Main.log.error("Unable to process block transactions", exc);
                Main.logException("Unable to process block transactions", exc);
                this.transactions = new ArrayList<>(0);
            }
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
            return transactions.size();
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
            if (row >= transactions.size())
                throw new IndexOutOfBoundsException("Table row "+row+" is not valid");
            Object value;
            Transaction tx = transactions.get(row);
            Chain chain = tx.getChain();
            //
            // Get the value for the requested cell
            //
            switch (column) {
                case 0:                                 // Transaction ID
                    value = Utils.idToString(tx.getId());
                    break;
                case 1:                                 // Type
                    value = tx.getTransactionType().getName();
                    break;
                case 2:                                 // Sender
                    value = Utils.getAccountRsId(tx.getSenderId());
                    break;
                case 3:                                 // Recipient
                    if (tx.getRecipientId() != 0)
                        value = Utils.getAccountRsId(tx.getRecipientId());
                    else
                        value = "";
                    break;
                case 4:                                 // Amount
                    value = new BigDecimal(tx.getAmount(), MathContext.DECIMAL128)
                                .movePointLeft(chain.getDecimals());
                    break;
                case 5:                                 // Fee
                    value = new BigDecimal(tx.getFee(), MathContext.DECIMAL128)
                                .movePointLeft(chain.getDecimals());
                    break;
                case 6:                                 // Chain
                    value = chain.getName();
                    break;
                default:
                    throw new IndexOutOfBoundsException("Table column "+column+" is not valid");
            }
            return value;
        }

        /**
         * Get the transaction for the specified row
         *
         * @param   row                 Table row
         * @return                      Transaction
         */
        public Transaction getTransaction(int row) {
            return transactions.get(row);
        }
    }
}
