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

import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * PopupMenu creates a JPopupMenu containing one or more menu items
 */
public class PopupMenu extends JPopupMenu {

    /**
     * Create a JPopupMenu from an item list.  Each item is a String array containing
     * the item label and the item action.
     *
     * @param       listener            Menu action listener
     * @param       items               One or more menu items
     */
    public PopupMenu(ActionListener listener, String[]... items) {
        super();
        for (String[] item : items) {
            JMenuItem menuItem = new JMenuItem(item[0]);
            menuItem.setActionCommand(item[1]);
            menuItem.addActionListener(listener);
            add(menuItem);
        }
    }
}
