/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/demo/translit/Demo.java,v $ 
 * $Date: 2002/05/25 15:20:10 $ 
 * $Revision: 1.15 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.demo.translit;
import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import com.ibm.icu.dev.demo.impl.*;
import com.ibm.icu.lang.*;
import com.ibm.icu.text.*;

/**
 * A frame that allows the user to experiment with keyboard
 * transliteration.  This class has a main() method so it can be run
 * as an application.  The frame contains an editable text component
 * and uses keyboard transliteration to process keyboard events.
 *
 * <p>Copyright (c) IBM Corporation 1999.  All rights reserved.
 *
 * @author Alan Liu
 * @version $RCSfile: Demo.java,v $ $Revision: 1.15 $ $Date: 2002/05/25 15:20:10 $
 */
public class Demo extends Frame {

    static final boolean DEBUG = false;
    static final String START_TEXT = "(cut,\u03BA\u03C5\u03C4,\u05D0,\u3042,\u4E80,\u091A\u0941\u0924\u094D)";

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
        text.setText(START_TEXT);
        add(text);

        setSize(width, height);
        setTransliterator("Latin-Greek", null);
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
        int[] sizes = {9, 10, 12, 14, 18, 24, 36, 48, 72};
        for (int i = 0; i < sizes.length; ++i) {
            MenuItem mItem = new MenuItem("" + sizes[i]);
            mItem.addActionListener(new SizeActionListener(sizes[i]));
            sizeMenu.add(mItem);
        }
        mbar.add(sizeMenu);
        
        translit = null;
        
        mbar.add(translitMenu = new Menu("Transliterator"));
        
        translitMenu.add(convertSelectionItem = new MenuItem("Transliterate", 
            new MenuShortcut(KeyEvent.VK_K)));
        convertSelectionItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleBatchTransliterate(translit);
            }
        });
        
        translitMenu.add(swapSelectionItem = new MenuItem("Reverse", 
            new MenuShortcut(KeyEvent.VK_S)));
        swapSelectionItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	Transliterator inv;
            	try {
                	inv = translit.getInverse();
                } catch (Exception x) {
                	inv = new NullTransliterator();
                }
            	setTransliterator(inv.getID(), null);
            }
        });
        
        translitMenu.add(convertTypingItem = new MenuItem("No Typing Conversion",
            new MenuShortcut(KeyEvent.VK_T)));
        convertTypingItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!transliterateTyping) {
                    text.setTransliterator(translit);
                    convertTypingItem.setLabel("No Typing Conversion");
                } else {
                    text.flush();
                    text.setTransliterator(null);
                    convertTypingItem.setLabel("Convert Typing");
                }
                transliterateTyping = !transliterateTyping;
            }
        });
        
        translitMenu.add(historyMenu = new Menu("Recent"));
        
        helpDialog = new InfoDialog(this, "Simple Demo", "Instructions",
           "CTL A, X, C, V have customary meanings.\n"
         + "Arrow keys, delete and backspace work.\n"
         + "To get a character from its control point, type the hex, then hit CTL Q"
        );
        helpDialog.getArea().setEditable(false);
        
       
        Menu helpMenu;
        mbar.add(helpMenu = new Menu("Extras"));
        helpMenu.add(mitem = new MenuItem("Help"));
        mitem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                helpDialog.show();
            }
        });   
        
        hexDialog = new InfoDialog(this, "Hex Entry", "Use U+..., \\u..., \\x{...}, or &#x...;",
           "\\u00E1"
        );
        Button button = new Button("Insert");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String hexValue = hexDialog.getArea().getText();
                text.insertText(fromHex.transliterate(hexValue));
            }
        });
        hexDialog.getBottom().add(button);
        
        helpMenu.add(mitem = new MenuItem("Hex...", 
            new MenuShortcut(KeyEvent.VK_H)));
        mitem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                hexDialog.show();
            }
        });
        
        // Compound Transliterator
        
        compoundDialog = new InfoDialog(this, "Compound Transliterator", "",
           "[^\\u0000-\\u00FF] hex"
        );
        button = new Button("Set");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String compound = "";
                try {
                    compound = compoundDialog.getArea().getText();
                    setTransliterator(compound, null);
                } catch (RuntimeException ex) {
                    compoundDialog.getArea().setText(compound + "\n" + ex.getMessage());
                }
            }
        });
        compoundDialog.getBottom().add(button);
        
        translitMenu.add(mitem = new MenuItem("Multiple...", 
            new MenuShortcut(KeyEvent.VK_M)));
        mitem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                compoundDialog.show();
            }
        });
        
        // RuleBased Transliterator
        
        rulesDialog = new InfoDialog(this, "Rule-Based Transliterator", "",
           "([A-Z]) > &Hex($1) &Name($1);\r\n" 
            + "&Hex-Any($1) < ('\\' [uU] [a-fA-F0-9]*);\r\n" 
			+ "&Name-Any($1) < ('{' [^\\}]* '}');"
        );
        button = new Button("Set");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String compound = "";
                try {
                    compound = rulesDialog.getArea().getText();
                    String id = ruleId.getText();
                    setTransliterator(compound, id);
                } catch (RuntimeException ex) {
                    rulesDialog.getArea().setText(compound + "\n" + ex.getMessage());
                }
            }
        });
        rulesDialog.getBottom().add(button);
        ruleId = new TextField("test1", 20);
        Label temp = new Label(" Name:");
        rulesDialog.getBottom().add(temp);
        rulesDialog.getBottom().add(ruleId);
        
        
        translitMenu.add(mitem = new MenuItem("From Rules...", 
            new MenuShortcut(KeyEvent.VK_R)));
        mitem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                rulesDialog.show();
            }
        });
        
        // Flesh out the menu with the installed transliterators
        
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
    
    boolean transliterateTyping = true;
    Transliterator fromHex = Transliterator.getInstance("Hex-Any");
    InfoDialog helpDialog;
    InfoDialog hexDialog;
    InfoDialog compoundDialog;
    InfoDialog rulesDialog;
    TextField ruleId;
    MenuItem convertSelectionItem = null;
    MenuItem swapSelectionItem = null;
    MenuItem convertTypingItem = null;
    Menu historyMenu;
    Map historyMap = new HashMap();
    Set historySet = new TreeSet(new Comparator() {
            public int compare(Object a, Object b) {
                MenuItem aa = (MenuItem)a;
                MenuItem bb = (MenuItem)b;
                return aa.getLabel().compareTo(bb.getLabel());
            }
        });
        
    // ADD Factory since otherwise getInverse blows out
    static class DummyFactory implements Transliterator.Factory {
        static DummyFactory singleton = new DummyFactory();
        static HashMap m = new HashMap();

        // Since Transliterators are immutable, we don't have to clone on set & get
        static void add(String ID, Transliterator t) {
            m.put(ID, t);
            System.out.println("Registering: " + ID + ", " + t.toRules(true));
            Transliterator.registerFactory(ID, singleton);
        }
        public Transliterator getInstance(String ID) {
            return (Transliterator) m.get(ID);
        }
    }
    
    static {
    	AnyTransliterator at = new AnyTransliterator("Greek", null);
    	at.transliterate("(cat,\u03b1,\u0915)");
    	DummyFactory.add(at.getID(), at);
    	
    	at = new AnyTransliterator("Devanagari", null);
    	at.transliterate("(cat,\u03b1,\u0915)");
    	DummyFactory.add(at.getID(), at);
    	
    	at = new AnyTransliterator("Latin", null);
    	at.transliterate("(cat,\u03b1,\u0915)");
    	DummyFactory.add(at.getID(), at);
    	
    	if (false) {
        DummyFactory.add("Any-gif", Transliterator.createFromRules("gif", "'\\'u(..)(..) > '<img src=\"http://www.unicode.org/gifs/24/' $1 '/U' $1$2 '.gif\">';", Transliterator.FORWARD));    	
        DummyFactory.add("gif-Any", Transliterator.getInstance("Any-Null"));    	

        DummyFactory.add("Any-RemoveCurly", Transliterator.createFromRules("RemoveCurly", "[\\{\\}] > ;", Transliterator.FORWARD));    	
        DummyFactory.add("RemoveCurly-Any", Transliterator.getInstance("Any-Null"));
        
        System.out.println("Trying &hex");
        Transliterator t = Transliterator.createFromRules("hex2", "(.) > &hex($1);", Transliterator.FORWARD);
        System.out.println("Registering");
        DummyFactory.add("Any-hex2", t);    	
        
        System.out.println("Trying &gif");
        t = Transliterator.createFromRules("gif2", "(.) > &any-gif($1);", Transliterator.FORWARD);
        System.out.println("Registering");
        DummyFactory.add("Any-gif2", t);    
        }
    }
    
    
    void setTransliterator(String name, String id) {
        if (DEBUG) System.out.println("Got: " + name);
        if (id == null) {
        	translit = Transliterator.getInstance(name);
        } else {
            int pos = id.indexOf('-');
            String reverseId = "";
            if (pos < 0) {
            	reverseId = id + "-Any";
            	id = "Any-" + id;
            } else {
            	reverseId = id.substring(pos+1) + "-" + id.substring(0,pos);
            }
            
        	
        	translit = Transliterator.createFromRules(id, name, Transliterator.FORWARD);
        	if (DEBUG) System.out.println("***Forward Rules");
        	if (DEBUG) System.out.println(((RuleBasedTransliterator)translit).toRules(true));
            DummyFactory.add(id, translit);
            
        	Transliterator translit2 = Transliterator.createFromRules(reverseId, name, Transliterator.REVERSE);
        	if (DEBUG) System.out.println("***Backward Rules");
        	if (DEBUG) System.out.println(((RuleBasedTransliterator)translit2).toRules(true));
            DummyFactory.add(reverseId, translit2);
            
            Transliterator rev = translit.getInverse();
        	if (DEBUG) System.out.println("***Inverse Rules");
        	if (DEBUG) System.out.println(((RuleBasedTransliterator)rev).toRules(true));
            
        }
        text.flush();
        text.setTransliterator(translit);
        convertSelectionItem.setLabel(Transliterator.getDisplayName(translit.getID()));
        
        addHistory(translit);
        
        Transliterator inv;
        try {
        	inv = translit.getInverse();
        } catch (Exception ex) {
        	inv = null;
        }
        if (inv != null) {
            addHistory(inv);
            swapSelectionItem.setEnabled(true);
        } else {
            swapSelectionItem.setEnabled(false);
        }
    }
    
    void addHistory(Transliterator translit) {
        String name = translit.getID();
        MenuItem cmi = (MenuItem) historyMap.get(name);
        if (cmi == null) {
            cmi = new MenuItem(translit.getDisplayName(name));
            cmi.addActionListener(new TransliterationListener(name));
            historyMap.put(name, cmi);
            historySet.add(cmi);
            historyMenu.removeAll();
            Iterator it = historySet.iterator();
            while (it.hasNext()) {
                historyMenu.add((MenuItem)it.next());
            }
        }
    }
    
    class TransliterationListener implements ActionListener, ItemListener {
        String name;
        public TransliterationListener(String name) {
            this.name = name;
        }
        public void actionPerformed(ActionEvent e) {
            setTransliterator(name, null);
        }
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == e.SELECTED) {
                setTransliterator(name, null);
            } else {
                setTransliterator("Any-Null", null);
            }
        }
    }
    
    class FontActionListener implements ActionListener {
        String name;
        public FontActionListener(String name) {
            this.name = name;
        }
        public void actionPerformed(ActionEvent e) {
            if (DEBUG) System.out.println("Font: " + name);
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
            if (DEBUG) System.out.println("Size: " + size);
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
        helpDialog.dispose();
        dispose();
    }
    
    /*
    class InfoDialog extends Dialog {
        protected Button button;
        protected TextArea area;
        protected Dialog me;
        protected Panel bottom;
        
        public TextArea getArea() {
            return area;
        }
        
        public Panel getBottom() {
            return bottom;
        }
        
        InfoDialog(Frame parent, String title, String label, String message) {
            super(parent, title, false);
            me = this;
            this.setLayout(new BorderLayout());
            if (label.length() != 0) {
                this.add("North", new Label(label));
            }
            
            area = new TextArea(message, 8, 80, TextArea.SCROLLBARS_VERTICAL_ONLY);
            this.add("Center", area);
            
            button = new Button("Hide");
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    me.hide();
                }
            });
            bottom = new Panel();
            bottom.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
            bottom.add(button);
            this.add("South", bottom);
            this.pack();
            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    me.hide();
                }
            });
        }
    }
    */
}
