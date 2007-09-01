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
package com.ibm.richtext.awtui;

import com.ibm.richtext.textpanel.MTextPanel;
import com.ibm.richtext.textpanel.TextPanel;
import com.ibm.richtext.styledtext.MConstText;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.MenuBar;
import java.awt.Toolkit;

import java.awt.datatransfer.Clipboard;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * TextFrame is a Frame containing an editable TextPanel, a set of standard
 * menus, and a TabRuler.  This class can be used as-is, but is
 * primarily intended to be a simple example of how to use the other classes
 * in this package.
 * @see com.ibm.richtext.textpanel.TextPanel
 * @see AwtMenuBuilder
 * @see TabRuler
 */
public final class TextFrame extends Frame {

    /**
     * For serialization
     */
    private static final long serialVersionUID = 8436822743736641732L;
    //static final String COPYRIGHT =
    //            "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private TextPanel fTextPanel;

    /**
     * Create a new TextFrame with no text and no title.
     */
    public TextFrame() {
        
        super();
        init(null, Toolkit.getDefaultToolkit().getSystemClipboard());
    }
    
    /**
     * Create a new TextFrame with no text and the given title.
     * @param title the title of this Frame
     */
    public TextFrame(String title) {
        
        super(title);
        init(null, Toolkit.getDefaultToolkit().getSystemClipboard());
    }
    
    /**
     * Create a new TextFrame with the given text and title, whose
     * TextPanel will use the given clipboard.
     * @param text the initial text in the TextPanel.  If null the
     *      TextPanel will initially be empty
     * @param title the title of this Frame
     * @param clipboard the Clipboard which the TextPanel will use.
     *      If null the TextPanel will use a private Clipboard
     */
    public TextFrame(MConstText text,
                     String title,
                     Clipboard clipboard) {

        super(title);
        init(text, clipboard);
    }

    private void init(MConstText text, Clipboard clipboard) {
        
        fTextPanel = new TextPanel(text, clipboard);

        TabRuler tabRuler = new TabRuler(14, 10, fTextPanel);

        createMenus();

        setLayout(new BorderLayout());
        add(fTextPanel, "Center");
        add(tabRuler, "North");
        pack();
    }

    private void createMenus() {

        MenuBar menuBar = new MenuBar();

        AwtMenuBuilder.getInstance().createMenus(menuBar, fTextPanel, this);

        setMenuBar(menuBar);
    }

    /**
     * Return the MTextPanel in this frame.
     */
    public MTextPanel getTextPanel() {

        return fTextPanel;
    }
    
    public static void main(String[] args) {
        
        TextFrame frame = new TextFrame();
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.setSize(550, 700);
        frame.show();
    }
}