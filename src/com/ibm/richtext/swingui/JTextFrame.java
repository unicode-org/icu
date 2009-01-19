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

import com.ibm.richtext.textpanel.MTextPanel;
import com.ibm.richtext.textpanel.JTextPanel;
import com.ibm.richtext.styledtext.MConstText;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.UIManager;

import java.awt.Container;

/**
 * JTextFrame is a JFrame containing an editable JTextPanel, a set of standard
 * menus, and a JTabRuler.  This class can be used as-is, but is
 * primarily intended to be a simple example of how to use the other classes
 * in this package.
 * @see com.ibm.richtext.textpanel.JTextPanel
 * @see SwingMenuBuilder
 * @see JTabRuler
 */
public final class JTextFrame extends JFrame {

    /**
     * For serialization
     */
    private static final long serialVersionUID = -1026126723995559230L;
    //static final String COPYRIGHT =
    //            "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private JTextPanel fTextPanel;

    /**
     * Create a new JTextFrame with no text, no title, 
     * and a private clipboard.
     */
    public JTextFrame() {
        
        super();
        init(null, Toolkit.getDefaultToolkit().getSystemClipboard());
    }
    
    /**
     * Create a new JTextFrame with no text and the given title.
     * The JTextPanel will use a private clipboard.
     * @param title the title of this Frame
     */
    public JTextFrame(String title) {
        
        super(title);
        init(null, Toolkit.getDefaultToolkit().getSystemClipboard());
    }
    
    /**
     * Create a new JTextFrame with the given text and title, whose
     * TextPanel will use the given clipboard.
     * @param text the initial text in the TextPanel.  If null the
     *      TextPanel will initially be empty
     * @param title the title of this Frame
     * @param clipboard the Clipboard which the TextPanel will use.
     *      If null the TextPanel will use a private Clipboard
     */
    public JTextFrame(MConstText text,
                     String title,
                     Clipboard clipboard) {

        super(title);
        init(text, clipboard);
    }

    private void init(MConstText text, Clipboard clipboard) {
        
        fTextPanel = new JTextPanel(text, clipboard);

        JTabRuler tabRuler = new JTabRuler(14, 10, fTextPanel);

        createMenus();

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(fTextPanel, "Center");
        contentPane.add(tabRuler, "North");
        pack();
    }

    private void createMenus() {

        JMenuBar menuBar = new JMenuBar();

        SwingMenuBuilder.getInstance().createMenus(menuBar, fTextPanel, this);
        
        setJMenuBar(menuBar);
    }

    /**
     * Return the MTextPanel in this frame.
     */
    public MTextPanel getTextPanel() {

        return fTextPanel;
    }
    
    public static void main(String[] args) {
        
        String laf = UIManager.getSystemLookAndFeelClassName();
        if (args.length == 1) {
            if (args[0].equals("cp")) {
                laf = UIManager.getCrossPlatformLookAndFeelClassName();
            }
        }
                
        try {
            UIManager.setLookAndFeel(laf);
        }
        catch(Throwable th) {
            th.printStackTrace();
        }
        JTextFrame frame = new JTextFrame();
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.setSize(550, 700);
        frame.show();
    }
}
