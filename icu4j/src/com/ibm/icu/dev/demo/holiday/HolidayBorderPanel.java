/*
 *******************************************************************************
 * Copyright (C) 1997-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.demo.holiday;

import java.awt.*;

/**
 * Various graphical borders. The border itself is a Panel so that it can
 * contain other Components (i.e. it borders something). You use the
 * HolidayBorderPanel like any other Panel: you set the layout that you prefer and
 * add Components to it. Beware that a null layout does not obey the insets
 * of the panel so if you use null layouts, adjust your measurements to
 * handle the border by calling insets().
 *
 * @author  Andy Clark, Taligent Inc.
 * @version 1.0
 */
public class HolidayBorderPanel extends Panel {
    // Constants

    /** Solid border. */
    public final static int SOLID = 0;
    /** A raised border. */
    public final static int RAISED = 1;
    /** A lowered border. */
    public final static int LOWERED = 2;
    /** An etched in border. */
    public final static int IN = 3;
    /** An etched out border. */
    public final static int OUT = 4;

    /** Left alignment. */
    public final static int LEFT = 0;
    /** Center alignment. */
    public final static int CENTER = 1;
    /** Right alignment. */
    public final static int RIGHT = 2;

    /** Default style (IN). */
    public final static int DEFAULT_STYLE = IN;
    /** Default thickness (10). */
    public final static int DEFAULT_THICKNESS = 10;
    /** Default thickness for solid borders (4). */
    public final static int DEFAULT_SOLID_THICKNESS = 4;
    /** Default thickness for raised borders (2). */
    public final static int DEFAULT_RAISED_THICKNESS = 2;
    /** Default thickness for lowered borders (2). */
    public final static int DEFAULT_LOWERED_THICKNESS = 2;
    /** Default thickness for etched-in borders (10). */
    public final static int DEFAULT_IN_THICKNESS = 10;
    /** Default thickness for etched-out borders (10). */
    public final static int DEFAULT_OUT_THICKNESS = 10;
    /** Default gap between border and contained component (5). */
    public final static int DEFAULT_GAP = 5;
    /** Default color (black). Applies to SOLID and etched borders. */
    public final static Color DEFAULT_COLOR = Color.black;

    /** Default font (TimesRoman,PLAIN,14). Only applies to etched borders. */
    public final static Font DEFAULT_FONT = new Font("TimesRoman", Font.PLAIN, 14);
    /** Default alignment (LEFT). Only applies to etched borders. */
    public final static int DEFAULT_ALIGNMENT = LEFT;

    // Data
    private int style;
    private int thickness;
    private int gap;
    private Color color;

    private Font font;
    private String text;
    private int alignment;

    /**
     * Constructor. Makes default border.
     */
    public HolidayBorderPanel() {

        // initialize data
        style       = DEFAULT_STYLE;
        thickness   = DEFAULT_THICKNESS;
        gap         = DEFAULT_GAP;
        color       = DEFAULT_COLOR;

        text        = null;
        font        = DEFAULT_FONT;
        alignment   = DEFAULT_ALIGNMENT;

        }

    /**
     * Constructor. Makes an etched IN border with given text caption.
     *
     * @param text  Text caption
     */
    public HolidayBorderPanel(String text) {
        this();

        style = IN;
        this.text = text;
        }

    /**
     * Constructor. Makes SOLID border with color and thickness given.
     *
     * @param color     The color for the border.
     * @param thickness The thickness of the border.
     */
    public HolidayBorderPanel(Color color, int thickness) {
        this();

        style = SOLID;
        this.color = color;
        this.thickness = thickness;
        }

    /**
     * Constructor. Makes a border of the given style with the default
     * thickness for that style.
     *
     * @param style The style for this border.
     */
    public HolidayBorderPanel(int style) {
        this();

        // set thickness appropriate to this style
        int thickness;
        switch (style) {
            case SOLID: thickness = DEFAULT_SOLID_THICKNESS; break;
            case RAISED: thickness = DEFAULT_RAISED_THICKNESS; break;
            case LOWERED: thickness = DEFAULT_LOWERED_THICKNESS; break;
            case IN: thickness = DEFAULT_IN_THICKNESS; break;
            case OUT: thickness = DEFAULT_OUT_THICKNESS; break;
            default:
                thickness = DEFAULT_THICKNESS;
            }

        this.style = style;
        this.thickness = thickness;
        }

    /**
     * Constructor. Makes border with given style and thickness.
     *
     * @param style     The style for this border.
     * @param thickness The thickness for this border.
     */
    public HolidayBorderPanel(int style, int thickness) {
        this();

        this.style = style;
        this.thickness = thickness;
        }

    /**
     * Returns the insets of this panel..
     */
    public Insets getInsets() {
        int adjustment = 0;

        // adjust for text string
        if (style == IN || style == OUT) {
            if (text != null && text.length() > 0) {
                try {
                    // set font and get info
                    int height = getGraphics().getFontMetrics(font).getHeight();
                    if (height > thickness)
                        adjustment = height - thickness;
                    }
                catch (Exception e) {
                    // nothing: just in case there is no graphics context
                    //   at the beginning.
                    System.out.print("");
                    }
                }
            }

        // return appropriate insets
        int dist = thickness + gap;
        return new Insets(dist + adjustment, dist, dist, dist);
        }

    /**
     * Sets the style of the border
     *
     * @param style The new style.
     */
    public HolidayBorderPanel setStyle(int style) {

        // set the style and re-layout the panel
        this.style = style;
        doLayout();
        repaint();

        return this;
        }

    /**
     * Gets the style of the border
     */
    public int getStyle() {

        return style;
        }

    /**
     * Sets the thickness of the border.
     *
     * @param thickness The new thickness
     */
    public HolidayBorderPanel setThickness(int thickness) {

        if (thickness > 0) {
            this.thickness = thickness;
            doLayout();
            repaint();
            }

        return this;
        }

    /**
     * Gets the thickness of the border.
     */
    public int getThickness() {

        return thickness;
        }

    /**
     * Sets the gap between the border and the contained Component.
     *
     * @param gap The new gap, in pixels.
     */
    public HolidayBorderPanel setGap(int gap) {

        if (gap > -1) {
            this.gap = gap;
            doLayout();
            repaint();
            }

        return this;
        }

    /**
     * Gets the gap between the border and the contained Component.
     */
    public int getGap() {

        return gap;
        }

    /**
     * Sets the current color for SOLID borders and the caption text
     * color for etched borders.
     *
     * @param color The new color.
     */
    public HolidayBorderPanel setColor(Color color) {

        this.color = color;
        if (style == SOLID || style == IN || style == OUT)
            repaint();

        return this;
        }

    /**
     * Gets the current color for SOLID borders and the caption
     * text color for etched borders.
     */
    public Color getColor() {

        return color;
        }

    /**
     * Sets the font. Only applies to etched borders.
     */
    public HolidayBorderPanel setTextFont(Font font) {

        // set font
        if (font != null) {
            this.font = font;
            if (style == IN || style == OUT) {
                doLayout();
                repaint();
                }
            }

        return this;
        }

    /**
     * Gets the font of the text. Only applies to etched borders.
     */
    public Font getTextFont() {

        return font;
        }

    /**
     * Sets the text. Only applies to etched borders.
     *
     * @param text  The new text.
     */
    public HolidayBorderPanel setText(String text) {

        this.text = text;
        if (style == IN || style == OUT) {
            doLayout();
            repaint();
            }

        return this;
        }

    /**
     * Gets the text. Only applies to etched borders.
     */
    public String getText() {

        return text;
        }

    /**
     * Sets the text alignment. Only applies to etched borders.
     *
     * @param alignment The new alignment.
     */
    public HolidayBorderPanel setAlignment(int alignment) {

        this.alignment = alignment;
        if (style == IN || style == OUT) {
            doLayout();
            repaint();
            }

        return this;
        }

    /**
     * Gets the text alignment.
     */
    public int getAlignment() {

        return alignment;
        }

    /**
     * Repaints the border.
     *
     * @param g The graphics context.
     */
    public void paint(Graphics g) {

        // get current dimensions
        Dimension size = getSize();
        int width = size.width;
        int height = size.height;

        // set colors
        Color light = getBackground().brighter().brighter().brighter();
        Color dark = getBackground().darker().darker().darker();

        // Draw border
        switch (style) {
            case RAISED:    // 3D Border (in or out)
            case LOWERED:
                Color topleft = null;
                Color bottomright = null;

                // set colors
                if (style == RAISED) {
                    topleft = light;
                    bottomright = dark;
                    }
                else {
                    topleft = dark;
                    bottomright = light;
                    }

                // draw border
                g.setColor(topleft);
                for (int i = 0; i < thickness; i++) {
                    g.drawLine(i, i, width - i - 2, i);
                    g.drawLine(i, i + 1, i, height - i - 1);
                    }
                g.setColor(bottomright);
                for (int i = 0; i < thickness; i++) {
                    g.drawLine(i + 1, height - i - 1, width - i - 1, height - i - 1);
                    g.drawLine(width - i - 1, i, width - i - 1, height - i - 2);
                    }
                break;

            case IN:    // Etched Border (in or out)
            case OUT:
                int adjust1 = 0;
                int adjust2 = 0;

                // set font and get info
                Font oldfont = g.getFont();
                g.setFont(font);
                FontMetrics fm = g.getFontMetrics();
                int ascent = fm.getAscent();

                // set adjustment
                if (style == IN)
                    adjust1 = 1;
                else
                    adjust2 = 1;

                // Calculate adjustment for text
                int adjustment = 0;
                if (text != null && text.length() > 0) {
                    if (ascent > thickness)
                        adjustment = (ascent - thickness) / 2;
                    }

                // The adjustment is there so that we always draw the
                // light rectangle first. Otherwise, your eye picks up
                // the discrepancy where the light rect. passes over
                // the darker rect.
                int x = thickness / 2;
                int y = thickness / 2 + adjustment;
                int w = width - thickness - 1;
                int h = height - thickness - 1 - adjustment;

                // draw rectangles
                g.setColor(light);
                g.drawRect(x + adjust1, y + adjust1, w, h);
                g.setColor(dark);
                g.drawRect(x + adjust2, y + adjust2, w, h);

                // draw text, if applicable
                if (text != null && text.length() > 0) {
                    // calculate drawing area
                    int fontheight = fm.getHeight();
                    int strwidth = fm.stringWidth(text);

                    int textwidth = width - 2 * (thickness + 5);
                    if (strwidth > textwidth)
                        strwidth = textwidth;

                    // calculate offset for alignment
                    int offset;
                    switch (alignment) {
                        case CENTER:
                            offset = (width - strwidth) / 2;
                            break;
                        case RIGHT:
                            offset = width - strwidth - thickness - 5;
                            break;
                        case LEFT:
                        default: // assume left alignment if invalid
                            offset = thickness + 5;
                            break;
                        }

                    // clear drawing area and set clipping region
                    g.clearRect(offset - 5, 0, strwidth  + 10, fontheight);
                    g.clipRect(offset, 0, strwidth, fontheight);

                    // draw text
                    g.setColor(color);
                    g.drawString(text, offset, ascent);

                    // restore old clipping area
                    g.clipRect(0, 0, width, height);
                    }

                g.setFont(oldfont);
                break;

            case SOLID:
            default: // assume SOLID
                g.setColor(color);
                for (int i = 0; i < thickness; i++)
                    g.drawRect(i, i, width - 2 * i - 1, height - 2 * i - 1);
            }

        }

    /**
     * Returns the settings of this HolidayBorderPanel instance as a string.
     */
    public String toString() {
        StringBuffer str = new StringBuffer("HolidayBorderPanel[");

        // style
        str.append("style=");
        switch (style) {
            case SOLID: str.append("SOLID"); break;
            case RAISED: str.append("RAISED"); break;
            case LOWERED: str.append("LOWERED"); break;
            case IN: str.append("IN"); break;
            case OUT: str.append("OUT"); break;
            default: str.append("unknown");
            }
        str.append(",");

        // thickness
        str.append("thickness=");
        str.append(thickness);
        str.append(",");

        // gap
        str.append("gap=");
        str.append(gap);
        str.append(",");

        // color
        str.append(color);
        str.append(",");

        // font
        str.append(font);
        str.append(",");

        // text
        str.append("text=");
        str.append(text);
        str.append(",");

        // alignment
        str.append("alignment=");
        switch (alignment) {
            case LEFT: str.append("LEFT"); break;
            case CENTER: str.append("CENTER"); break;
            case RIGHT: str.append("RIGHT"); break;
            default: str.append("unknown");
            }

        str.append("]");

        return str.toString();
        }

    }

