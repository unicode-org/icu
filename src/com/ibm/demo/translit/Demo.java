/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/demo/translit/Attic/Demo.java,v $ 
 * $Date: 2000/03/10 03:47:44 $ 
 * $Revision: 1.4 $
 *
 *****************************************************************************************
 */
package com.ibm.demo.translit;
import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import com.ibm.text.components.*;
import com.ibm.text.*;

/**
 * A frame that allows the user to experiment with keyboard
 * transliteration.  This class has a main() method so it can be run
 * as an application.  The frame contains an editable text component
 * and uses keyboard transliteration to process keyboard events.
 *
 * <p>Copyright (c) IBM Corporation 1999.  All rights reserved.
 *
 * @author Alan Liu
 * @version $RCSfile: Demo.java,v $ $Revision: 1.4 $ $Date: 2000/03/10 03:47:44 $
 */
public class Demo extends Frame {

    static final boolean DEBUG = false;

    Transliterator translit = null;

    boolean compound = false;
    Transliterator[] compoundTranslit = new Transliterator[MAX_COMPOUND];
    static final int MAX_COMPOUND = 128;
    int compoundCount = 0;

    TransliteratingTextComponent text = null;

    Menu translitMenu;
    CheckboxMenuItem translitItem;
    CheckboxMenuItem noTranslitItem;

    static final String NO_TRANSLITERATOR = "None";

    private static final String COPYRIGHT =
        "\u00A9 IBM Corporation 1999. All rights reserved.";

    public static void main(String[] args) {
        Frame f = new Demo(600, 200);
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        f.setVisible(true);
    }

	public Demo(int width, int height) {
        super("Transliteration Demo");

        initMenus();

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                handleClose();
            }
        });
        
        text = new TransliteratingTextComponent();
        Font font = new Font("serif", Font.PLAIN, 48);
        text.setFont(font);
        text.setSize(width, height);
        text.setVisible(true);
        text.setText("\u03B1\u05D0\u3042\u4E80");
        add(text);

        setSize(width, height);
    }

    private void initMenus() {
        MenuBar mbar;
        Menu menu;
        MenuItem mitem;
        CheckboxMenuItem citem;
        
        setMenuBar(mbar = new MenuBar());
        mbar.add(menu = new Menu("File"));
        menu.add(mitem = new MenuItem("Quit"));
        mitem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleClose();
            }
        });

        final ItemListener setTransliteratorListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                CheckboxMenuItem item = (CheckboxMenuItem) e.getSource();
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                    // Don't let the current transliterator be deselected.
                    // Just reselect it.
                    item.setState(true);
                } else if (compound) {
                    // Adding an item to a compound transliterator
                    handleAddToCompound(item.getLabel());
                } else if (item != translitItem) {
                    // Deselect previous choice.  Don't need to call
                    // setState(true) on new choice.
                    translitItem.setState(false);
                    translitItem = item;
                    handleSetTransliterator(item.getLabel());
                }
            }
        };

        translit = null;
        mbar.add(translitMenu = new Menu("Transliterator"));
        translitMenu.add(translitItem = noTranslitItem =
                         new CheckboxMenuItem(NO_TRANSLITERATOR, true));
        noTranslitItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                // Can't uncheck None -- any action here sets None to true
                setNoTransliterator();
            }
        });

        translitMenu.addSeparator();

        translitMenu.add(citem = new CheckboxMenuItem("Compound"));
        citem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                CheckboxMenuItem item = (CheckboxMenuItem) e.getSource();
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                    // If compound gets deselected, then select NONE
                    setNoTransliterator();
                } else if (!compound) {
                    // Switching from non-compound to compound
                    translitItem.setState(false);
                    translitItem = item;
                    translit = null;
                    compound = true;
                    compoundCount = 0;
                    for (int i=0; i<MAX_COMPOUND; ++i) {
                        compoundTranslit[i] = null;
                    }
                }
            }
        });
      
        translitMenu.addSeparator();

        for (Enumeration e=getSystemTransliteratorNames().elements();
             e.hasMoreElements(); ) {
            String s = (String) e.nextElement();
            translitMenu.add(citem = new CheckboxMenuItem(s));
            citem.addItemListener(setTransliteratorListener);
        }

        mbar.add(menu = new Menu("Batch"));
        menu.add(mitem = new MenuItem("Transliterate Selection"));
        mitem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleBatchTransliterate();
            }
        });
    }

    /**
     * Get a sorted list of the system transliterators.
     */
    private static Vector getSystemTransliteratorNames() {
        Vector v = new Vector();
        for (Enumeration e=Transliterator.getAvailableIDs();
             e.hasMoreElements(); ) {
            v.addElement(e.nextElement());
        }
        // Insertion sort, O(n^2) acceptable for small n
        for (int i=0; i<(v.size()-1); ++i) {
            String a = (String) v.elementAt(i);
            for (int j=i+1; j<v.size(); ++j) {
                String b = (String) v.elementAt(j);
                if (a.compareTo(b) > 0) {
                    v.setElementAt(b, i);
                    v.setElementAt(a, j);
                    a = b;
                }
            }
        }
        return v;
    }

    private void setNoTransliterator() {
        translitItem = noTranslitItem;
        noTranslitItem.setState(true);
        handleSetTransliterator(noTranslitItem.getLabel());
        compound = false;
        for (int i=0; i<translitMenu.getItemCount(); ++i) {
            MenuItem it = translitMenu.getItem(i);
            if (it != noTranslitItem && it instanceof CheckboxMenuItem) {
                ((CheckboxMenuItem) it).setState(false);
            }
        }
    }

    private void handleAddToCompound(String name) {
        if (compoundCount < MAX_COMPOUND) {
            compoundTranslit[compoundCount] = decodeTranslitItem(name);
            ++compoundCount;
            Transliterator t[] = new Transliterator[compoundCount];
            System.arraycopy(compoundTranslit, 0, t, 0, compoundCount);
            translit = new CompoundTransliterator(t);
            text.setTransliterator(translit);
        }
    }

    private void handleSetTransliterator(String name) {
        translit = decodeTranslitItem(name);
        text.setTransliterator(translit);
    }

    /**
     * Decode a menu item that looks like <translit name>.
     */
    private static Transliterator decodeTranslitItem(String name) {
        return (name.equals(NO_TRANSLITERATOR))
            ? null : Transliterator.getInstance(name);
    }

    private void handleBatchTransliterate() {
        if (translit == null) {
            return;
        }

        int start = text.getSelectionStart();
        int end = text.getSelectionEnd();
        ReplaceableString s =
            new ReplaceableString(text.getText().substring(start, end));

        StringBuffer log = null;
        if (DEBUG) {
            log = new StringBuffer();
            log.append('"' + s.toString() + "\" (start " + start +
                       ", end " + end + ") -> \"");
        }

        translit.transliterate(s);
        String str = s.toString();

        if (DEBUG) {
            log.append(str + "\"");
            System.out.println("Batch " + translit.getID() + ": " + log.toString());
        }

        text.replaceRange(str, start, end);
        text.select(start, start + str.length());
    }

    private void handleClose() {
        dispose();
    }
}
