/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/demo/translit/Demo.java,v $ 
 * $Date: 2001/11/21 00:53:05 $ 
 * $Revision: 1.6 $
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
 * @version $RCSfile: Demo.java,v $ $Revision: 1.6 $ $Date: 2001/11/21 00:53:05 $
 */
public class Demo extends Frame {

    static final boolean DEBUG = false;

    Transliterator translit = null;
    String fontName = "Arial Unicode MS";
    int fontSize = 36;
    
    

    /*
    boolean compound = false;
    Transliterator[] compoundTranslit = new Transliterator[MAX_COMPOUND];
    static final int MAX_COMPOUND = 128;
    int compoundCount = 0;
    */

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
        Font font = new Font(fontName, Font.PLAIN, fontSize);
        text.setFont(font);
        text.setSize(width, height);
        text.setVisible(true);
        text.setText("\u03B1\u05D0\u3042\u4E80");
        add(text);

        setSize(width, height);
        setTransliterator("Any-Null");
        
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
/*
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
*/
        /*
        translitMenu.add(translitItem = noTranslitItem =
                         new CheckboxMenuItem(NO_TRANSLITERATOR, true));
        noTranslitItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                // Can't uncheck None -- any action here sets None to true
                setNoTransliterator();
            }
        });

        translitMenu.addSeparator();
        */

/*
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
       */

        /*
        for (Enumeration e=getSystemTransliteratorNames().elements();
             e.hasMoreElements(); ) {
            String s = (String) e.nextElement();
            translitMenu.add(citem = new CheckboxMenuItem(s));
            citem.addItemListener(setTransliteratorListener);
        }
        */
        
        Menu fontMenu = new Menu("Font");
        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (int i = 0; i < fonts.length; ++i) {
            MenuItem mItem = new MenuItem(fonts[i]);
            mItem.addActionListener(new FontActionListener(fonts[i]));
            fontMenu.add(mItem);
        }
        mbar.add(fontMenu);
        
        Menu sizeMenu = new Menu("Size");
        for (double i = 9; i < 100; i = i * 4/3) {
            MenuItem mItem = new MenuItem("" + (int)i);
            mItem.addActionListener(new SizeActionListener((int)i));
            sizeMenu.add(mItem);
        }
        mbar.add(sizeMenu);
        
        translit = null;
        
        mbar.add(translitMenu = new Menu("Transliterator"));
        
        translitMenu.add(mitem = new MenuItem("Convert Selection"));
        mitem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleBatchTransliterate(translit);
            }
        });
        
        translitMenu.add(mitem = new MenuItem("Invert Selection"));
        mitem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleBatchTransliterate(translit.getInverse());
            }
        });
        
        translitMenu.add(mitem = new MenuItem("Flush"));
        mitem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                text.flush();
            }
        });
        
        translitMenu.add(historyMenu = new Menu("History"));
        
        historyMenu.add(mitem = new MenuItem("Inverse"));
        mitem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Transliterator inv = translit.getInverse();
                if (inv == null) {
                    getToolkit().beep(); // LIU: Add audio feedback of NOP
                    return;
                } 
                setTransliterator(inv.getID());
            }
        });
        
        
        translitMenu.addSeparator();
        
        Iterator sources = add(new TreeSet(), Transliterator.getAvailableSources()).iterator();
        while(sources.hasNext()) {
            String source = (String) sources.next();
            Iterator targets = add(new TreeSet(), Transliterator.getAvailableTargets(source)).iterator();
            Menu targetMenu = new Menu(source);
            while(targets.hasNext()) {
                String target = (String) targets.next();
                Set variantSet = add(new TreeSet(), Transliterator.getAvailableVariants(source, target));
                if (variantSet.size() < 2) {
                    mitem = new MenuItem(target);
                    mitem.addActionListener(new TransliterationListener(source + "-" + target));
                    targetMenu.add(mitem);
                } else {
                    Iterator variants = variantSet.iterator();
                    Menu variantMenu = new Menu(target);
                    while(variants.hasNext()) {
                        String variant = (String) variants.next();
                        mitem = new MenuItem(variant == "" ? "<default>" : variant);
                        mitem.addActionListener(new TransliterationListener(source + "-" + target + "/" + variant));
                        variantMenu.add(mitem);
                    }
                    targetMenu.add(variantMenu);
                }
            }
            translitMenu.add(targetMenu);
        }
    }
    
    Menu historyMenu;
    Map historyMap = new HashMap();
    CheckboxMenuItem currentHistory = new CheckboxMenuItem();
    
    void setTransliterator(String name) {
        System.out.println("Got: " + name);
        translit = Transliterator.getInstance(name);
        text.setTransliterator(translit);
        
        addHistory(translit, true);
        
        Transliterator inv = translit.getInverse();
        if (inv != null) {
            addHistory(inv, false);
        }
    }
    
    void addHistory(Transliterator translit, boolean makeSelected) {
        String name = translit.getID();
        CheckboxMenuItem cmi = (CheckboxMenuItem) historyMap.get(name);
        if (!currentHistory.equals(cmi)) {
            if (makeSelected) currentHistory.setState(false);
            if (cmi == null) {
                cmi = new CheckboxMenuItem(translit.getDisplayName(name));
                cmi.addItemListener(new TransliterationListener(name));
                historyMenu.add(cmi);
                historyMap.put(name, cmi);
            }
            if (makeSelected) cmi.setState(true);
            currentHistory = cmi;
        }
    }
    
    class TransliterationListener implements ActionListener, ItemListener {
        String name;
        public TransliterationListener(String name) {
            this.name = name;
        }
        public void actionPerformed(ActionEvent e) {
            setTransliterator(name);
        }
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == e.SELECTED) {
                setTransliterator(name);
            } else {
                setTransliterator("Any-Null");
            }
        }
    }
    
    class FontActionListener implements ActionListener {
        String name;
        public FontActionListener(String name) {
            this.name = name;
        }
        public void actionPerformed(ActionEvent e) {
            System.out.println("Font: " + name);
            fontName = name;
            text.setFont(new Font(fontName, Font.PLAIN, fontSize));
        }
    }
    
    class SizeActionListener implements ActionListener {
        int size;
        public SizeActionListener(int size) {
            this.size = size;
        }
        public void actionPerformed(ActionEvent e) {
            System.out.println("Size: " + size);
            fontSize = size;
            text.setFont(new Font(fontName, Font.PLAIN, fontSize));
        }
    }
    
    Set add(Set s, Enumeration enum) {
        while(enum.hasMoreElements()) {
            s.add(enum.nextElement());
        }
        return s;
    }

    /**
     * Get a sorted list of the system transliterators.
     */
     /*
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
    */

/*
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
*/
/*
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
*/
/*
    private void handleSetTransliterator(String name) {
        translit = decodeTranslitItem(name);
        text.setTransliterator(translit);
    }
    */

    /**
     * Decode a menu item that looks like <translit name>.
     */
     /*
    private static Transliterator decodeTranslitItem(String name) {
        return (name.equals(NO_TRANSLITERATOR))
            ? null : Transliterator.getInstance(name);
    }
    */

    private void handleBatchTransliterate(Transliterator translit) {
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
