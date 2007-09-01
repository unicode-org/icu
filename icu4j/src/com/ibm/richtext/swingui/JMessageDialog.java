/*
 * (C) Copyright IBM Corp. 1998-2007.  All Rights Reserved.
 *
 * The program is provided "as is" without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 */
package com.ibm.richtext.swingui;

import java.awt.Color;
import java.awt.Container;
import java.awt.CardLayout;

import javax.swing.JFrame;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.ibm.richtext.styledtext.MConstText;

import com.ibm.richtext.textpanel.JTextPanel;
import com.ibm.richtext.textpanel.TextPanelSettings;

/**
 * MessageDialog is a simple Frame which displays a styled
 * text message in a TextPanel.
 * The text in the message is not selectable or editable.
 * @see MConstText
 * @see JTextPanel
 */
public final class JMessageDialog extends JFrame {

    /**
     * For serialization
     */
    private static final long serialVersionUID = 5012952859760456427L;

    //static final String COPYRIGHT =
    //            "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    /**
    * Create a new MessageDialog.
    * @param title the dialog's title
    * @param message the text which will appear in the dialog
    */
    public JMessageDialog(String title, MConstText message)
    {
        super(title);

        Container content = getContentPane();
        content.setLayout(new CardLayout());
        TextPanelSettings settings = JTextPanel.getDefaultSettings();
        settings.setScrollable(false);
        settings.setSelectable(false);
        JTextPanel panel = new JTextPanel(settings, message, null);

        panel.setBackground(Color.black);
        content.add("Center", panel);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                setVisible(false);
                dispose();
            }
        });

        setSize(450,320);
    }
}
