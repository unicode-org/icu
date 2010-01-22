/*
 *******************************************************************************
 * Copyright (C) 1996-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.demo.rbnf;

import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.text.BreakIterator;
import java.text.ParsePosition;
import java.util.Locale;
import com.ibm.icu.dev.demo.impl.*;
import com.ibm.icu.text.RuleBasedNumberFormat;

public class RbnfDemo extends DemoApplet {
    /**
     * For serialization
     */
    private static final long serialVersionUID = -9119861296873763536L;

    /**
     * Puts a copyright in the .class file
     */
//    private static final String copyrightNotice
//        = "Copyright \u00a91997-1998 IBM Corp.  All rights reserved.";

    /*
     * code to run the demo as an application
     */
    public static void main(String[] argv) {
        new RbnfDemo().showDemo();
    }

    protected Dimension getDefaultFrameSize(DemoApplet applet, Frame f) {
        return new Dimension(430,270);
    }

    protected Frame createDemoFrame(DemoApplet applet) {
        final Frame window = new Frame("Number Spellout Demo");
        window.setSize(800, 600);
        window.setLayout(new BorderLayout());

        Panel mainPanel = new Panel();
        mainPanel.setLayout(new GridLayout(1,2));

        commentaryField = new TextArea("", 0, 0, TextArea.SCROLLBARS_VERTICAL_ONLY);
        commentaryField.setSize(800, 50);
        commentaryField.setText(RbnfSampleRuleSets.sampleRuleSetCommentary[0]);
        commentaryField.setEditable(false);
        commentaryField.setFont(new Font("Helvetica", Font.PLAIN, 14));

        spelloutFormatter = new RuleBasedNumberFormat(RbnfSampleRuleSets.usEnglish, Locale.US);
        spelloutFormatter.setLenientParseMode(lenientParse);
        populateRuleSetMenu();
        numberFormatter = new DecimalFormat("#,##0.##########");
        parsePosition = new ParsePosition(0);
        theNumber = 0;

        numberField = new TextField();
        numberField.setFont(new Font("Serif", Font.PLAIN, 24));
        textField = new DemoTextFieldHolder();
        textField.setFont(new Font("Serif", Font.PLAIN, 24));
        rulesField = new DemoTextFieldHolder();
        rulesField.setFont(new Font("Serif", Font.PLAIN, 14));
        lenientParseButton = new Checkbox("Lenient parse", lenientParse);

        numberField.addTextListener(new TextListener() {
            public void textValueChanged(TextEvent e) {
                if (!numberFieldHasFocus)
                    return;

                String fieldText = ((TextComponent)(e.getSource())).getText();
                parsePosition.setIndex(0);
                Number temp = numberFormatter.parse(fieldText, parsePosition);
                if (temp == null || parsePosition.getIndex() == 0) {
                    theNumber = 0;
                    textField.setText("PARSE ERROR");
                }
                else {
                    theNumber = temp.doubleValue();
                    textField.setText(spelloutFormatter.format(theNumber, ruleSetName));
                }
            }
        } );

        numberField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                numberFieldHasFocus = false;
                numberField.setText(numberFormatter.format(theNumber));
            }

            public void focusGained(FocusEvent e) {
                numberFieldHasFocus = true;
                numberField.selectAll();
            }
        } );

        textField.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == '\t') {
                    String fieldText = ((TextComponent)(e.getSource())).getText();
                    parsePosition.setIndex(0);
                    theNumber = spelloutFormatter.parse(fieldText, parsePosition)
                                        .doubleValue();
                    if (parsePosition.getIndex() == 0) {
                        theNumber = 0;
                        numberField.setText("PARSE ERROR");
                        textField.selectAll();
                    }
                    else if (parsePosition.getIndex() < fieldText.length()) {
                        textField.select(parsePosition.getIndex(), fieldText.length());
                        numberField.setText(numberFormatter.format(theNumber));
                    }
                    else {
                        textField.selectAll();
                        numberField.setText(numberFormatter.format(theNumber));
                    }
                    e.consume();
                }
            }
        } );

        textField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                String fieldText = ((TextComponent)(e.getSource())).getText();
                parsePosition.setIndex(0);
                theNumber = spelloutFormatter.parse(fieldText, parsePosition)
                                .doubleValue();
                if (parsePosition.getIndex() == 0)
                    numberField.setText("PARSE ERROR");
                else
                    numberField.setText(numberFormatter.format(theNumber));
                textField.setText(textField.getText()); // textField.repaint() didn't work right
            }

            public void focusGained(FocusEvent e) {
                textField.selectAll();
            }
        } );

        rulesField.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == '\t') {
                    String fieldText = ((TextComponent)(e.getSource())).getText();
                    if (formatterMenu.getSelectedItem().equals("Custom") || !fieldText.equals(
                                    RbnfSampleRuleSets.sampleRuleSets[formatterMenu.getSelectedIndex()])) {
                        try {
                            RuleBasedNumberFormat temp = new RuleBasedNumberFormat(fieldText);
                            temp.setLenientParseMode(lenientParse);
                            populateRuleSetMenu();
                            spelloutFormatter = temp;
                            customRuleSet = fieldText;
                            formatterMenu.select("Custom");
                            commentaryField.setText(RbnfSampleRuleSets.
                                sampleRuleSetCommentary[RbnfSampleRuleSets.
                                sampleRuleSetCommentary.length - 1]);
                            redisplay();
                        }
                        catch (Exception x) {
                            textField.setText(x.toString());
                        }
                    }
                    e.consume();
                }
            }
        } );

        rulesField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                String fieldText = ((TextComponent)(e.getSource())).getText();
                if (formatterMenu.getSelectedItem().equals("Custom") || !fieldText.equals(
                                RbnfSampleRuleSets.sampleRuleSets[formatterMenu.getSelectedIndex()])) {
                    try {
                        RuleBasedNumberFormat temp = new RuleBasedNumberFormat(fieldText);
                        temp.setLenientParseMode(lenientParse);
                        populateRuleSetMenu();
                        spelloutFormatter = temp;
                        customRuleSet = fieldText;
                        formatterMenu.select("Custom");
                        redisplay();
                    }
                    catch (Exception x) {
                        textField.setText(x.toString());
                    }
                }
                rulesField.setText(rulesField.getText()); // rulesField.repaint() didn't work right
            }
        } );

        lenientParseButton.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                lenientParse = lenientParseButton.getState();
                spelloutFormatter.setLenientParseMode(lenientParse);
            }
        } );

        numberField.setText(numberFormatter.format(theNumber));
        numberField.selectAll();
        textField.setText(spelloutFormatter.format(theNumber, ruleSetName));

        Panel leftPanel = new Panel();
        leftPanel.setLayout(new BorderLayout());
        Panel panel = new Panel();
        panel.setLayout(new BorderLayout());
        Panel panel1 = new Panel();
        panel1.setLayout(new GridLayout(3, 1));
        panel1.add(new Panel());
        panel1.add(numberField, "Center");
        panel1.add(lenientParseButton);
        panel.add(panel1, "Center");
        Panel panel2 = new Panel();
        panel2.setLayout(new GridLayout(3, 3));
        Button button = new Button("+100");
        button.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                roll(100);
            }
        } );
        panel2.add(button);
        button = new Button("+10");
        button.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                roll(10);
            }
        } );
        panel2.add(button);
        button = new Button("+1");
        button.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                roll(1);
            }
        } );
        panel2.add(button);
        button = new Button("<");
        button.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                theNumber *= 10;
                redisplay();
            }
        } );
        panel2.add(button);
        panel2.add(new Panel());
        button = new Button(">");
        button.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                theNumber /= 10;
                redisplay();
            }
        } );
        panel2.add(button);
        button = new Button("-100");
        button.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                roll(-100);
            }
        } );
        panel2.add(button);
        button = new Button("-10");
        button.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                roll(-10);
            }
        } );
        panel2.add(button);
        button = new Button("-1");
        button.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                roll(-1);
            }
        } );
        panel2.add(button);
        panel.add(panel2, "East");
        leftPanel.add(panel, "North");
        leftPanel.add(textField, "Center");

        Panel rightPanel = new Panel();
        rightPanel.setLayout(new BorderLayout());
        formatterMenu = new Choice();
        for (int i = 0; i < RbnfSampleRuleSets.sampleRuleSetNames.length; i++)
            formatterMenu.addItem(RbnfSampleRuleSets.sampleRuleSetNames[i]);
        formatterMenu.addItem("Custom");
        formatterMenu.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                Choice source = (Choice)(e.getSource());
                int item = source.getSelectedIndex();
                Locale locale = RbnfSampleRuleSets.sampleRuleSetLocales[item];

                commentaryField.setText(RbnfSampleRuleSets.
                                sampleRuleSetCommentary[item]);

                if (locale != null && (locale.getLanguage().equals("iw")
                        || locale.getLanguage().equals("ru") || locale.getLanguage().equals("ja")
                        || locale.getLanguage().equals("el")
                        || locale.getLanguage().equals("zh"))) {
                    textField.togglePanes(false);
                    rulesField.togglePanes(false);
                }
                else {
                    textField.togglePanes(true);
                    rulesField.togglePanes(true);
                }

                makeNewSpelloutFormatter();
                redisplay();
            }
        } );

        ruleSetMenu = new Choice();
        populateRuleSetMenu();

        ruleSetMenu.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                ruleSetName = ruleSetMenu.getSelectedItem();
                redisplay();
            }
        } );

        Panel menuPanel = new Panel();
        menuPanel.setLayout(new GridLayout(1, 2));
        menuPanel.add(formatterMenu);
        menuPanel.add(ruleSetMenu);
        rightPanel.add(menuPanel, "North");

        rulesField.setText(RbnfSampleRuleSets.sampleRuleSets[formatterMenu.getSelectedIndex()]);
        rightPanel.add(rulesField, "Center");

        mainPanel.add(leftPanel);
        mainPanel.add(rightPanel);

        window.add(mainPanel, "Center");
        window.add(commentaryField, "South");

        window.doLayout();
        window.show();
        final DemoApplet theApplet = applet;
        window.addWindowListener(
                new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                        setVisible(false);
                        window.dispose();

                        if (theApplet != null) {
                            theApplet.demoClosed();
                        } else System.exit(0);
                    }
                } );
        return window;
    }

    void roll(int delta) {
        theNumber += delta;
        redisplay();
    }

    void redisplay() {
        numberField.setText(numberFormatter.format(theNumber));
        textField.setText(spelloutFormatter.format(theNumber, ruleSetName));
    }

    void makeNewSpelloutFormatter() {
        int item = formatterMenu.getSelectedIndex();
        String formatterMenuItem = formatterMenu.getSelectedItem();

        if (formatterMenuItem.equals("Custom")) {
            rulesField.setText(customRuleSet);
            spelloutFormatter = new RuleBasedNumberFormat(customRuleSet);
        }
        else {
            rulesField.setText(RbnfSampleRuleSets.sampleRuleSets[item]);

            Locale locale = RbnfSampleRuleSets.sampleRuleSetLocales[item];
            if (locale == null)
                locale = Locale.getDefault();

            spelloutFormatter = new RuleBasedNumberFormat(RbnfSampleRuleSets.
                            sampleRuleSets[item], locale);
        }
        spelloutFormatter.setLenientParseMode(lenientParse);
        populateRuleSetMenu();
    }

    void populateRuleSetMenu() {
        String[] ruleSetNames = spelloutFormatter.getRuleSetNames();

        if (ruleSetMenu != null) {
            ruleSetMenu.removeAll();
            for (int i = 0; i < ruleSetNames.length; i++)
                ruleSetMenu.addItem(ruleSetNames[i]);

            ruleSetName = ruleSetMenu.getSelectedItem();
        }
        else
            ruleSetName = ruleSetNames[0];
    }

//    private Frame demoWindow = null;

    private TextComponent numberField;
    private DemoTextFieldHolder textField;
    private DemoTextFieldHolder rulesField;
    private TextComponent commentaryField;
    private Checkbox lenientParseButton;

    private boolean numberFieldHasFocus = true;

    private RuleBasedNumberFormat spelloutFormatter;
    private DecimalFormat numberFormatter;
    private ParsePosition parsePosition;

    private boolean lenientParse = true;

    private double theNumber = 0;
//    private boolean canEdit = true;

    private Choice formatterMenu;
    private Choice ruleSetMenu;
    private String ruleSetName;

    private String customRuleSet = "NO RULES!";
}

class DemoTextField extends Component {
    /**
     * For serialization
     */
    private static final long serialVersionUID = -7947090021239472658L;
    public DemoTextField() {
    }

    public void setText(String text) {
        this.text = text;
        this.repaint();
    }

    public String getText() {
        return text;
    }

    public void paint(Graphics g) {
        Font font = getFont();
        FontMetrics fm = g.getFontMetrics();
        g.setFont(font);
        String txt = getText();
        BreakIterator bi = BreakIterator.getLineInstance();
        bi.setText(txt);
        int lineHeight = fm.getHeight();
        int width = getSize().width;
        int penY = fm.getAscent();
        int lineStart = 0;
        int tempLineEnd = bi.first();
        int lineEnd = 0;
        int maxLineEnd = 0;
        totalHeight = 0;

        while (lineStart < txt.length()) {
            maxLineEnd = txt.indexOf('\n', lineStart);
            if (maxLineEnd == -1)
                maxLineEnd = Integer.MAX_VALUE;
            while (tempLineEnd != BreakIterator.DONE && fm.stringWidth(txt.substring(
                            lineStart, tempLineEnd)) < width) {
                lineEnd = tempLineEnd;
                tempLineEnd = bi.next();
            }
            if (lineStart >= lineEnd) {
                if (tempLineEnd == BreakIterator.DONE)
                    lineEnd = txt.length();
                else
                    lineEnd = tempLineEnd;
            }
            if (lineEnd > maxLineEnd)
                lineEnd = maxLineEnd;
            g.drawString(txt.substring(lineStart, lineEnd), 0, penY);
            penY += lineHeight;
            totalHeight += lineHeight;
            lineStart = lineEnd;
            if (lineStart < txt.length() && txt.charAt(lineStart) == '\n')
                ++lineStart;
        }
    }

/*
    public Dimension getPreferredSize() {
        Dimension size = getParent().getSize();
        return new Dimension(size.width, totalHeight);
    }
*/

    private String text;
    private int totalHeight;
}

class DemoTextFieldHolder extends Panel {
    /**
     * For serialization
     */
    private static final long serialVersionUID = 7514498764062569858L;
    public DemoTextFieldHolder() {
        tf1 = new TextArea("", 0, 0, TextArea.SCROLLBARS_VERTICAL_ONLY);
        tf2 = new DemoTextField();
        sp = new ScrollPane();

        setLayout(new CardLayout());

        sp.add(tf2, "TextField1");
        sp.setVisible(false);
        add(tf1, "TestField2");
        add(sp, "ScrollPane");
    }

    public void addFocusListener(FocusListener l) {
        tf1.addFocusListener(l);
    }

    public void addKeyListener(KeyListener l) {
        tf1.addKeyListener(l);
    }

    public void setText(String text) {
        tf1.setText(text);
        tf2.setText(text);
    }

    public String getText() {
        return tf1.getText();
    }

    public void select(int start, int end) {
        tf1.select(start, end);
    }

    public void selectAll() {
        tf1.selectAll();
    }

    public void togglePanes(boolean canShowRealTextField) {
        if (canShowRealTextField != showingRealTextField) {
            CardLayout layout = (CardLayout)(getLayout());
            layout.next(this);
            showingRealTextField = canShowRealTextField;
        }
    }

    private TextArea tf1 = null;
    private DemoTextField tf2 = null;
    private ScrollPane sp = null;
    private boolean showingRealTextField = true;
}
