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

import java.util.Date;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.*;

/**
 * The SizedTable class is a JTable with column sizes based on the column data types
 */
public final class SizedTable extends JTable {

    /** Date column */
    public static final int DATE = 1;

    /** Integer column */
    public static final int INTEGER = 2;

    /** Long column */
    public static final int LONG = 3;

    /** Identifier column */
    public static final int ID = 4;

    /** Status column */
    public static final int STATUS = 5;

    /** IP address column */
    public static final int ADDRESS = 6;

    /** Application column */
    public static final int APPLICATION = 7;

    /** Services column */
    public static final int SERVICES = 8;

    /** Name column */
    public static final int NAME = 9;

    /** Thread identifier column */
    public static final int TID = 10;

    /** Identity hash column */
    public static final int HASH = 11;

    /** Amount column */
    public static final int AMOUNT = 12;

    /** Type column */
    public static final int TYPE = 13;

    /** Event column */
    public static final int EVENT = 14;

    /** Chain column */
    public static final int CHAIN = 15;

    /**
     * Create a new sized table
     *
     * @param       tableModel      The table model
     * @param       columnTypes     Array of column types
     */
    public SizedTable(TableModel tableModel, int[] columnTypes) {
        //
        // Create the table
        //
        super(tableModel);
        //
        // Set the cell renderers and column widths
        //
        Component component;
        TableCellRenderer renderer;
        TableColumn column;
        TableColumnModel columnModel = getColumnModel();
        TableCellRenderer headRenderer = getTableHeader().getDefaultRenderer();
        if (headRenderer instanceof DefaultTableCellRenderer) {
            DefaultTableCellRenderer defaultRenderer = (DefaultTableCellRenderer)headRenderer;
            defaultRenderer.setHorizontalAlignment(JLabel.CENTER);
        }
        int columnCount = tableModel.getColumnCount();
        if (columnCount > columnTypes.length)
            throw new IllegalArgumentException("More columns than column types");
        for (int i=0; i<columnCount; i++) {
            Object value = null;
            column = columnModel.getColumn(i);
            switch (columnTypes[i]) {
                case DATE:
                    column.setCellRenderer(new DateRenderer());
                    value = new Date();
                    break;
                case INTEGER:                                       // 6 characters
                case TID:
                    value = "mmmmmn";
                    break;
                case LONG:                                          // 12 characters
                    value = "mmmmmmmmmmmm";
                    break;
                case ID:                                            // 32 characters
                    column.setCellRenderer(new StringRenderer(JLabel.CENTER));
                    value = "nnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnn";
                    break;
                case STATUS:                                        // 16 character
                case APPLICATION:
                case SERVICES:
                    column.setCellRenderer(new StringRenderer(JLabel.CENTER));
                    value = "Mmmmmmmmmmmmmmmm";
                    break;
                case CHAIN:                                         // 5 characters
                    column.setCellRenderer(new StringRenderer(JLabel.CENTER));
                    value = "Mnnnn";
                    break;
                case TYPE:                                          // 24 characters
                    column.setCellRenderer(new StringRenderer(JLabel.CENTER));
                    value = "Mmmmmmmmmmmmmmmmmmmmmmmm";
                    break;
                case EVENT:                                         // 32 characters
                    column.setCellRenderer(new StringRenderer(JLabel.CENTER));
                    value = "Mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm";
                    break;
                case ADDRESS:                                       // IP address
                    column.setCellRenderer(new StringRenderer(JLabel.RIGHT));
                    value = "[nnnn:nnnn:nnnn:nnnn:nnnn:nnnn:nnnn:nnnn]:nnnnn";
                    break;
                case NAME:                                          // 48 characters
                    column.setCellRenderer(new StringRenderer(JLabel.RIGHT));
                    value = "mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm";
                    break;
                case HASH:                                          // 9 characters
                    value = "mmmmmmmmm";
                    break;
                case AMOUNT:                                        // nnnnnn.nnnn
                    column.setCellRenderer(new AmountRenderer());
                    value = 1234567891234L;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported column type "+columnTypes[i]);
            }
            component = headRenderer.getTableCellRendererComponent(this, tableModel.getColumnName(i),
                                                                   false, false, 0, i);
            int headWidth = component.getPreferredSize().width;
            renderer = column.getCellRenderer();
            if (renderer == null)
                renderer = getDefaultRenderer(tableModel.getColumnClass(i));
            component = renderer.getTableCellRendererComponent(this, value, false, false, 0, i);
            int cellWidth = component.getPreferredSize().width;
            column.setPreferredWidth(Math.max(headWidth+20, cellWidth+30));
        }
        //
        // Resize all column proportionally
        //
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    }
}
