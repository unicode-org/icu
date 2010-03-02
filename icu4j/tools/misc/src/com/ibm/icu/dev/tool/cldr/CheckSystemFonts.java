/*
 **********************************************************************
 * Copyright (c) 2009-2010, Google, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package com.ibm.icu.dev.tool.cldr;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeMapIterator;
import com.ibm.icu.dev.test.util.Tabber.HTMLTabber;
import com.ibm.icu.dev.test.util.UnicodeMap.Composer;
import com.ibm.icu.dev.test.util.XEquivalenceClass.SetMaker;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;


public class CheckSystemFonts {
    
    static String outputDirectoryName;
    static Set<String> SKIP_SHAPES = new HashSet<String>();
    
    public static void main(String[] args) throws IOException {
        System.out.println("Arguments:\t" + Arrays.asList(args));
        if (args.length < 2) {
            throw new IllegalArgumentException("Need command-line args:" +
                    "\n\t\tfont-name-regex" +
                    "\n\t\toutput-directory"
                    );
        }
        Matcher nameMatcher = Pattern.compile(args[0], Pattern.CASE_INSENSITIVE).matcher("");
        outputDirectoryName = args[1].trim();
        File outputDirectory = new File(outputDirectoryName);
        if (!outputDirectory.isDirectory()) {
            throw new IllegalArgumentException("2nd arg must be valid directory");
        }
        loadSkipShapes();

        Map<UnicodeSet,Set<String>> data = new TreeMap<UnicodeSet, Set<String>>();
        Map<String, Font> fontMap = new TreeMap<String, Font>();
        getFontData(nameMatcher, data, fontMap);
        
        showInvisibles();
        showSameGlyphs();

        UnicodeMap<Set<String>> map = showEquivalentCoverage(data);

        showRawCoverage(data);

        Map<Set<String>, String> toShortName = showRawCoverage(map);

        showFullCoverage(map, toShortName);
    }

    private static void loadSkipShapes() {
        try {
            BufferedReader in = BagFormatter.openUTF8Reader(outputDirectoryName, "skip_fonts.txt");
            while (true) {
                String line = in.readLine();
                if (line == null) break;
                String[] fonts = line.trim().split("\\s+");
                for (String font : fonts) {
                    SKIP_SHAPES.add(font);
                }
            }
            in.close();
        } catch (IOException e) {
            System.err.println("Couldn't open:\t" + outputDirectoryName + "/" + "skip_fonts.txt");
        }
    }


    private static final Collator English = Collator.getInstance();

    static {
        English.setStrength(Collator.SECONDARY);
    }

    public static final UnicodeSet DONT_CARE = new UnicodeSet("[[:cn:][:co:][:cs:]]").freeze();
    public static final UnicodeSet COVERAGE = new UnicodeSet(DONT_CARE).complement().freeze();

    private static final Comparator<String> SHORTER_FIRST = new Comparator<String>() {
        public int compare(String n1, String n2) {
            int result = n1.length() - n2.length();
            if (result != 0) return result;
            return n1.compareTo(n2);
        }
    };

    private static final Comparator<UnicodeSet> LONGER_SET_FIRST = new Comparator<UnicodeSet>() {
        public int compare(UnicodeSet n1, UnicodeSet n2) {
            int result = n1.size() - n2.size();
            if (result != 0) return -result;
            return n1.compareTo(n2);
        }
    };

    private static final Comparator<Collection> SHORTER_COLLECTION_FIRST = new Comparator<Collection>() {
        public int compare(Collection n1, Collection n2) {
            int result = n1.size() - n2.size();
            if (result != 0) return result;
            return UnicodeSet.compare(n1, n2);
        }
    };

    private static final HashSet SKIP_TERMS = new HashSet(Arrays.asList("black", "blackitalic", "bold", "boldit", "bolditalic", "bolditalicmt", "boldmt",
            "boldob", "boldoblique", "boldslanted", "book", "bookitalic", "condensed", "condensedblack", "condensedbold", "condensedextrabold",
            "condensedlight", "condensedmedium", "extracondensed", "extralight", "heavy", "italic", "italicmt", "light", "lightit", "lightitalic", "medium",
            "mediumitalic", "oblique", "regular", "roman", "semibold", "semibolditalic", "shadow", "slanted", "ultrabold", "ultralight", "ultralightitalic"
    ));

    private static Composer<Set<String>> composer = new Composer<Set<String>>() {
        Map<R2<Set<String>, Set<String>>,Set<String>> cache = new HashMap<R2<Set<String>, Set<String>>,Set<String>>();
        public Set<String> compose(int codePoint, String string, Set<String> a, Set<String> b) {
            return a == null ? b
                    : b == null ? null 
                            : intern(a,b);
        }
        private Set<String> intern(Set<String> a, Set<String> b) {
            R2<Set<String>, Set<String>> row = Row.of(a, b);
            Set<String> result = cache.get(row);
            if (result == null) {
                result = new TreeSet<String>(English);
                result.addAll(a);
                result.addAll(b);
                cache.put(row, result);
            }
            return result;
        }
    };


    private static void showFullCoverage(UnicodeMap<Set<String>> map, Map<Set<String>, String> toShortName) throws IOException {
        System.out.println("\n***COVERAGE:\t" + map.keySet().size() + "\n");
        PrintWriter out = BagFormatter.openUTF8Writer(outputDirectoryName, "coverage.txt");

        for (UnicodeMapIterator<String> it = new UnicodeMapIterator<String>(map); it.nextRange();) {
            String codes = "U+" + Utility.hex(it.codepoint);
            String names = UCharacter.getExtendedName(it.codepoint);
            if (it.codepointEnd != it.codepoint) {
                codes += "..U+" + Utility.hex(it.codepointEnd);
                names += ".." + UCharacter.getExtendedName(it.codepointEnd);
            }
            out.println(codes + "\t" + toShortName.get(map.get(it.codepoint)) + "\t" + names);
        }

        UnicodeSet missing = new UnicodeSet(COVERAGE).removeAll(map.keySet());
        out.println("\nMISSING:\t" + missing.size() + "\n");

        UnicodeMap<String> missingMap = new UnicodeMap<String>();
        for (UnicodeSetIterator it = new UnicodeSetIterator(missing); it.next();) {
            missingMap.put(it.codepoint, UScript.getName(UScript.getScript(it.codepoint)) + "-" + getShortAge(it.codepoint));
        }

        Set<String> sorted = new TreeSet<String>(English);
        sorted.addAll(missingMap.values());
        for (String value : sorted) {
            UnicodeSet items = missingMap.getSet(value);
            for (UnicodeSetIterator it = new UnicodeSetIterator(items); it.nextRange();) {
                String codes = "U+" + Utility.hex(it.codepoint);
                String names = UCharacter.getExtendedName(it.codepoint);
                if (it.codepointEnd != it.codepoint) {
                    codes += "..U+" + Utility.hex(it.codepointEnd);
                    names += ".." + UCharacter.getExtendedName(it.codepointEnd);
                }
                out.println(codes + "\t" + value + "\t" + names);
            }
            out.println();
        }
        out.close();
    }

    private static Map<Set<String>, String> showRawCoverage(UnicodeMap<Set<String>> map) throws IOException {
        System.out.println("\n***COMBO NAMES\n");
        PrintWriter out = BagFormatter.openUTF8Writer(outputDirectoryName, "combo_names.txt");

        int count = 0;
        Map<Set<String>, String> toShortName = new HashMap<Set<String>, String>();
        TreeSet<Set<String>> sortedValues = new TreeSet<Set<String>>(SHORTER_COLLECTION_FIRST);
        sortedValues.addAll(map.values());
        for (Set<String> value : sortedValues) {
            String shortName = "combo" + count++;
            Set<String> contained = getLargestContained(value, toShortName.keySet());
            String valueName;
            if (contained != null) {
                Set<String> remainder = new TreeSet<String>();
                remainder.addAll(value);
                remainder.removeAll(contained);
                valueName = toShortName.get(contained) + " + " + remainder;
            } else {
                valueName = value.toString();
            }
            toShortName.put(value, shortName);
            out.println(shortName + "\t" + valueName);
        }
        out.close();
        return toShortName;
    }

    private static void showRawCoverage(Map<UnicodeSet, Set<String>> data) throws IOException {
        System.out.println("\n***RAW COVERAGE (bridging unassigned)\n");
        PrintWriter out = BagFormatter.openUTF8Writer(outputDirectoryName, "raw_coverage.txt");

        for (UnicodeSet s : data.keySet()) {
            Set<String> nameSet = data.get(s);
            String name = nameSet.iterator().next();
            UnicodeSet bridged = new UnicodeSet(s).addBridges(DONT_CARE);
            out.println(name + "\t" + s.size() + "\t" + bridged);
        }
        out.close();
    }

    private static UnicodeMap<Set<String>> showEquivalentCoverage(Map<UnicodeSet, Set<String>> data) throws IOException {
        System.out.println("\n***EQUIVALENT COVERAGE\n");
        PrintWriter out = BagFormatter.openUTF8Writer(outputDirectoryName, "equiv_coverage.txt");

        UnicodeMap<Set<String>> map = new UnicodeMap<Set<String>>();

        Map<String,Set<String>> nameToSingleton = new HashMap<String,Set<String>>();

        for (UnicodeSet s : data.keySet()) {
            Set<String> nameSet = data.get(s);
            String name = nameSet.iterator().next();
            //System.out.println(s);
            Set<String> temp2 = nameToSingleton.get(name);
            if (temp2 == null) {
                temp2 = new TreeSet<String>(English);
                temp2.add(name);
            }
            map.composeWith(s, temp2, composer);
            if (nameSet.size() > 1) {
                TreeSet<String> temp = new TreeSet<String>(English);
                temp.addAll(nameSet);
                temp.remove(name);
                out.println(name + "\t" + temp);
            }
        }
        out.close();
        return map;
    }

    private static void showSameGlyphs() throws IOException {
        System.out.println("\n***Visual Equivalences");
        PrintWriter out = BagFormatter.openUTF8Writer(outputDirectoryName, "same_glyphs.txt");
        PrintWriter out2 = BagFormatter.openUTF8Writer(outputDirectoryName, "same_glyphs.html");
        out2.println("<html><head>");
        out2.println("<meta content=\"text/html; charset=utf-8\" http-equiv=Content-Type></HEAD>");
        out2.println("<link rel='stylesheet' href='index.css' type='text/css'>");
        out2.println("</head><body><table>");
        HTMLTabber tabber = new HTMLTabber();

        out2.println(tabber.process("Code1\tCode2\tNFC1\tNFC1\tCh1\tCh1\tCh1/F\tCh2/F\tName1\tName2\tFonts"));
        tabber.setParameters(0, "class='c'");
        tabber.setParameters(1, "class='c'");
        tabber.setParameters(2, "class='nf'");
        tabber.setParameters(3, "class='nf'");
        tabber.setParameters(4, "class='p'");
        tabber.setParameters(5, "class='p'");
        //tabber.setParameters(6, "class='q'");
        //tabber.setParameters(7, "class='q'");
        tabber.setParameters(8, "class='n'");
        tabber.setParameters(9, "class='n'");
        tabber.setParameters(10, "class='f'");

        for (R2<Integer,Integer> sample : equivalences.keySet()) {
            final Set<String> reasonSet = equivalences.get(sample);
            String reasons = reasonSet.toString();
            if (reasons.length() > 100) reasons = reasons.substring(0,100) + "...";
            final Integer codepoint1 = sample.get0();
            final Integer codepoint2 = sample.get1();
            
            out.println("U+" + Utility.hex(codepoint1) + "\t" + "U+" + Utility.hex(codepoint2)
                    + "\t" + showNfc(codepoint1) + "\t" + showNfc(codepoint2)
                    + "\t" + showChar(codepoint1, false) + "\t" + showChar(codepoint2, false)
                    + "\t" + UCharacter.getExtendedName(codepoint1) + "\t" + UCharacter.getExtendedName(codepoint2)
                    + "\t" + reasons);
            String line = "U+" + Utility.hex(codepoint1) + "\t" + "U+" + Utility.hex(codepoint2)
                    + "\t" + showNfc(codepoint1) + "\t" + showNfc(codepoint2)
                    + "\t" + showChar(codepoint1, false) + "\t" + showChar(codepoint2, true)
                    + "\t" + showChar(codepoint1, false) + "\t" + showChar(codepoint2, true)
                    + "\t" + UCharacter.getExtendedName(codepoint1) + "\t" + UCharacter.getExtendedName(codepoint2)
                    + "\t" + reasons;
            
            String fonts = "class='q' style='font-family:";
            int maxCount = 5;
            for (String font : reasonSet) {
                if (maxCount != 5) {
                    fonts += ",";
                }
                fonts += font;
                --maxCount;
                if (maxCount <= 0) break;
            }
            fonts += "'";
            tabber.setParameters(6, fonts);
            tabber.setParameters(7, fonts);
            out2.println(tabber.process(line));
        }
        out2.println("</table></body>");
        out2.close();
        out.close();
    }

    private static void showInvisibles() throws IOException {
        System.out.println("\n***Invisibles Equivalences");
        PrintWriter out = BagFormatter.openUTF8Writer(outputDirectoryName, "invisibles.txt");
        for (String sample : invisibles) {
            String reasons = invisibles.get(sample).toString();
            if (reasons.length() > 100) reasons = reasons.substring(0,100) + "...";
            int codepoint = sample.codePointAt(0);
            out.println("U+" + Utility.hex(sample)
                    + "\t" + showChar(codepoint, false)
                    + "\t" + showNfc(codepoint)
                    + "\t" + UCharacter.getExtendedName(codepoint)
                    + "\t" + reasons);

        }
        out.close();
    }

    private static void getFontData(Matcher nameMatcher, Map<UnicodeSet, Set<String>> data, Map<String, Font> fontMap) {
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font[] fonts = env.getAllFonts();
        for (Font font : fonts) {
            if (!font.isPlain()) {
                continue;
            }
            String name = font.getName();
            int lastDash = name.lastIndexOf('-');
            String term = lastDash < 0 ? "" : name.substring(lastDash+1).toLowerCase();
            if (SKIP_TERMS.contains(term)) {
                continue;
            }
            if (nameMatcher != null && !nameMatcher.reset(name).find()) {
                continue;
            }
            fontMap.put(name,font);
        }
        for (String name : fontMap.keySet()) {
            Font font = fontMap.get(name);
            System.out.println(name);
            UnicodeSet coverage = getCoverage(font);
            Set<String> sameFonts = data.get(coverage);
            if (sameFonts == null) {
                data.put(coverage, sameFonts = new TreeSet<String>(SHORTER_FIRST));
            } else {
                System.out.println("\tNote: same coverage as " + sameFonts.iterator().next());
            }
            sameFonts.add(name);
        }
    }
    
    static Comparator<Integer> NFCLower = new Comparator<Integer>() {
        public int compare(Integer o1, Integer o2) {
            boolean n1 = Normalizer.isNormalized(o1, Normalizer.NFC, 0);
            boolean n2 = Normalizer.isNormalized(o2, Normalizer.NFC, 0);
            if (n1 != n2) return n1 ? -1 : 1;
            n1 = Normalizer.isNormalized(o1, Normalizer.NFKC, 0);
            n2 = Normalizer.isNormalized(o2, Normalizer.NFKC, 0);
            if (n1 != n2) return n1 ? -1 : 1;
            return o1.compareTo(o2);
        } 
    };
    
    static Comparator<R2<Integer,Integer>> NFCLowerR2 = new Comparator<R2<Integer,Integer>>() {
        public int compare(R2<Integer, Integer> o1, R2<Integer, Integer> o2) {
            int diff = NFCLower.compare(o1.get0(), o2.get0());
            if (diff != 0) return diff;
            return NFCLower.compare(o1.get1(), o2.get1());
        }
    };

    private static String showNfc(int codepoint) {
        return Normalizer.isNormalized(codepoint, Normalizer.NFC, 0) ? "" 
                : Normalizer.isNormalized(codepoint, Normalizer.NFKC, 0) ? "!C" : "!K";
    }

    private static String showChar(Integer item, boolean html) {
        return rtlProtect(UTF16.valueOf(item), html);
    }
    static UnicodeSet RTL = new UnicodeSet("[[:bc=R:][:bc=AL:][:bc=AN:]]").freeze();
    static UnicodeSet CONTROLS = new UnicodeSet("[[:cc:][:Zl:][:Zp:]]").freeze();
    static UnicodeSet INVISIBLES = new UnicodeSet("[:di:]").freeze();
    static final char LRM = '\u200E';

    private static String rtlProtect(String source, boolean html) {
        if (CONTROLS.containsSome(source)) {
            source = "";
        } else if (INVISIBLES.containsSome(source)) {
            source = "";
        } else if (RTL.containsSome(source) || source.startsWith("\"")) {
            source = LRM + source + LRM;
        }
        return html ? TransliteratorUtilities.toHTML.transform(source) : source;
    }


    private static Set<String> getLargestContained(Set<String> value, Collection<Set<String>> collection) {
        Set<String> best = null;
        for (Set<String> set : collection) {
            if (best != null && best.size() > set.size()) {
                continue;
            }
            if (value.containsAll(set)) {
                best = set;
            }
        }
        return best;
    }

    private static String getShortAge(int i) {
        String age = UCharacter.getAge(i).toString();
        return age.substring(0,age.indexOf('.',age.indexOf('.') + 1));
    }

    static SetMaker setMaker = new SetMaker() {
        public Set make() {
            return new TreeSet();
        } 
    };

    static UnicodeMap<Set<String>> invisibles = new UnicodeMap();
    static Map<R2<Integer,Integer>, Set<String>> equivalences = new TreeMap<R2<Integer,Integer>, Set<String>>(NFCLowerR2);
//    static Set<String> SKIP_SHAPES = new HashSet<String>(Arrays.asList(
//            "MT-Extra",
//            "JCsmPC",
//            "DFKaiShu-SB-Estd-BF",
//            "LiGothicMed",
//            "LiHeiPro",
//            "LiSongPro",
//            "LiSungLight",
//            "PMingLiU",
//            "SIL-Hei-Med-Jian",
//            "SIL-Kai-Reg-Jian",
//            "CharcoalCY",
//            "GenevaCY",
//            "HelveticaCYBoldOblique",
//            "HelveticaCYOblique",
//            "HelveticaCYPlain",
//            "HoeflerText-Ornaments",
//            "Apple-Chancery",
//            "MSReferenceSpecialty",
//            "Stencil",
//            "Hooge0555",
//            "Hooge0556",
//            "Desdemona",
//            "EccentricStd",
//            "EngraversMT",
//            "MesquiteStd",
//            "RosewoodStd-Fill",
//            "Stencil",
//            "StencilStd",
//            "Osaka",
//            "Osaka-Mono",
//            "Kroeger0455",
//            "Kroeger0456",
//            "Uni0563",
//            "Uni0564",
//            "Code2001",
//            "AppleSymbols",
//            "AppleGothic", 
//            "AppleMyungjo",
//            "JCkg",
//            "MalithiWeb",
//            "JCfg"
//    ));

    // bug on Mac: http://forums.sun.com/thread.jspa?threadID=5209611
    private static UnicodeSet getCoverage(Font font) {
        String name = font.getFontName();
        boolean skipShapes = SKIP_SHAPES.contains(name);
        UnicodeSet result = new UnicodeSet();
        final FontRenderContext fontRenderContext = new FontRenderContext(null, false, false);
        char[] array = new char[1];
        char[] array2 = new char[2];
        Map<Rectangle2D,Map<Shape,UnicodeSet>> boundsToData = new TreeMap<Rectangle2D,Map<Shape,UnicodeSet>>(ShapeComparator);
        for (UnicodeSetIterator it = new UnicodeSetIterator(COVERAGE); it.next();) {
            if (font.canDisplay(it.codepoint)) {
                char[] temp;
                if (it.codepoint <= 0xFFFF) {
                    array[0] = (char) it.codepoint;
                    temp = array;
                } else {
                    Character.toChars(it.codepoint, array2, 0);
                    temp = array2;
                }

                GlyphVector glyphVector = font.createGlyphVector(fontRenderContext, temp);
                int glyphCode = glyphVector.getGlyphCode(0);
                boolean validchar = (glyphCode > 0);
                if (!validchar) continue;

                result.add(it.codepoint);

                if (skipShapes) continue;
                Shape shape = glyphVector.getOutline();
                if (isInvisible(shape)) {
                    Set<String> set = invisibles.get(it.codepoint);
                    if (set == null) {
                        invisibles.put(it.codepoint, set = new TreeSet<String>());
                    }
                    set.add(name);
                } else {
                    Rectangle2D bounds = glyphVector.getVisualBounds();
                    Map<Shape, UnicodeSet> map = boundsToData.get(bounds);
                    if (map == null) {
                        boundsToData.put(bounds, map = new TreeMap<Shape,UnicodeSet>(ShapeComparator));
                    }
                    UnicodeSet set = map.get(shape);
                    if (set == null) {
                        map.put(shape, set = new UnicodeSet());
                    }
                    if (false && set.size() != 0) {
                        System.out.println("Adding " + Utility.hex(it.codepoint) + "\t" + UTF16.valueOf(it.codepoint) +  "\tto " + set.toPattern(false));
                    }
                    set.add(it.codepoint);
                }
            }
        }
        //System.out.println(result.size() + "\t" + result);
        for (Rectangle2D bounds : boundsToData.keySet()) {
            Map<Shape, UnicodeSet> map = boundsToData.get(bounds);
            for (Shape shape : map.keySet()) {
                UnicodeSet set = map.get(shape);
                set.removeAll(CONTROLS);
                if (set.size() != 1) {
                    //System.out.println(set.toPattern(false));
                    for (UnicodeSetIterator it = new UnicodeSetIterator(set); it.next();) {
                        for (UnicodeSetIterator it2 = new UnicodeSetIterator(set); it2.next();) {
                            int cp = it.codepoint;
                            int cp2 = it2.codepoint;
                            if (cp >= cp2) continue;
                            R2<Integer, Integer> r = Row.of(cp, cp2);
                            Set<String> reasons = equivalences.get(r);
                            if (reasons == null) {
                                equivalences.put(r, reasons = new TreeSet());
                            }
                            reasons.add(name);
                        }
                    }
                }
            }
        }
        return result.freeze();
    }

    static Comparator<Rectangle2D> RectComparator = new Comparator<Rectangle2D>() {

        public int compare(Rectangle2D r1, Rectangle2D r2) {
            int diff;
            if (0 != (diff = compareDiff(r1.getX(),r2.getX()))) return diff;
            if (0 != (diff = compareDiff(r1.getY(),r2.getY()))) return diff;
            if (0 != (diff = compareDiff(r1.getWidth(),r2.getWidth()))) return diff;
            if (0 != (diff = compareDiff(r1.getHeight(),r2.getHeight()))) return diff;
            return 0;
        }

    };

    static final AffineTransform IDENTITY = new AffineTransform();

    static boolean isInvisible(Shape shape) {
        return shape.getPathIterator(IDENTITY).isDone();
    }

    static Comparator<Shape> ShapeComparator = new Comparator<Shape>() {
        float[] coords1 = new float[6];
        float[] coords2 = new float[6];

        public int compare(Shape s1, Shape s2) {
            int diff;
            PathIterator p1 = s1.getPathIterator(IDENTITY);
            PathIterator p2 = s2.getPathIterator(IDENTITY);
            while (true) {
                if (p1.isDone()) {
                    return p2.isDone() ? 0 : -1;
                } else if (p2.isDone()) {
                    return 1;
                }
                int t1 = p1.currentSegment(coords1);
                int t2 = p2.currentSegment(coords2);
                diff = t1 - t2;
                if (diff != 0) return diff;
                /*
                 * SEG_MOVETO and SEG_LINETO types returns one point,
                 * SEG_QUADTO returns two points,
                 * SEG_CUBICTO returns 3 points
                 * and SEG_CLOSE does not return any points.
                 */
                switch (t1) {
                case PathIterator.SEG_CUBICTO: 
                    if (0 != (diff = compareDiff(coords1[5],coords2[5]))) return diff;
                    if (0 != (diff = compareDiff(coords1[4],coords2[4]))) return diff;
                case PathIterator.SEG_QUADTO: 
                    if (0 != (diff = compareDiff(coords1[3],coords2[3]))) return diff;
                    if (0 != (diff = compareDiff(coords1[2],coords2[2]))) return diff;
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO: 
                    if (0 != (diff = compareDiff(coords1[1],coords2[1]))) return diff;
                    if (0 != (diff = compareDiff(coords1[0],coords2[0]))) return diff;
                case PathIterator.SEG_CLOSE: break;
                default: throw new IllegalArgumentException();
                }
                p1.next();
                p2.next();
            }
        }
    };

    private static int compareDiff(float f, float g) {
        return f < g ? -1 : f > g ? 1 : 0;
    }
    private static int compareDiff(double f, double g) {
        return f < g ? -1 : f > g ? 1 : 0;
    }
}
