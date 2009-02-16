/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.locale;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.util.InvalidLocaleException;

public final class LocaleExtension {

    private String _canonical;

    private transient TreeMap/*<String,String>*/ _extensions;
    private transient String _privuse;
    private transient TreeMap/*<String,String>*/ _keywords;

    private static final LocaleObjectPool/*<String,LocaleExtension>*/ EXTENSIONPOOL =
        new LocaleObjectPool/*<String,LocaleExtension>*/();

    public static final LocaleExtension EMPTY_EXTENSION = new LocaleExtension("");

    private static final String LOCALESEP = "_";
    private static final String LOCALESINGLETON = "u";
    private static final String PRIVUSE = "x";

    private static final int MINLEN = 3; // minium length of string representation "x_?"

    private LocaleExtension(String canonical) {
        _canonical = canonical;
    }

    public static LocaleExtension get(String extstr) throws InvalidLocaleException {
        if (extstr == null || extstr.length() == 0) {
            return EMPTY_EXTENSION;
        }
        // Convert to lower case
        extstr = AsciiUtil.toLowerString(extstr);
        LocaleExtension singleton = (LocaleExtension)EXTENSIONPOOL.get(extstr);
        if (singleton == null) {
            LocaleExtension locext = getCanonical(extstr);
            // Try to get from the pool with the canonicalized string
            singleton = (LocaleExtension)EXTENSIONPOOL.get(locext.toString());
            if (singleton == null) {
                singleton = (LocaleExtension)EXTENSIONPOOL.register(locext.toString(), locext);
            }
        }
        return singleton;
    }

    /*
     * This method is package local and used by LocaleBuilder.
     * LocaleBuilder stores singleton extensions including locale
     * keywords in a Map structure, so we do not need to parse plain
     * extension string again.
     */
    static LocaleExtension get(TreeMap/*<String,String>*/ extensions, String privuse) {
        boolean hasExtensions = (extensions != null && extensions.size() > 0);
        boolean hasPrivuse = (privuse != null && privuse.length() > 0);

        if (!hasExtensions && !hasPrivuse) {
            return EMPTY_EXTENSION;
        }

        StringBuffer buf = new StringBuffer();
        if (hasExtensions) {
            mapToLocaleExtensionString(extensions, buf);
        }
        if (hasPrivuse) {
            // prepend x_
            privuse = PRIVUSE + LOCALESEP + privuse;
            if (buf.length() > 0) {
                buf.append(LOCALESEP);
            }
            buf.append(privuse);
        }
        String extstr = buf.toString();

        // Check if the same LocaleExtension is available in the pool
        LocaleExtension singleton = (LocaleExtension)EXTENSIONPOOL.get(extstr);
        if (singleton == null) {
            // create a new one
            singleton = new LocaleExtension(extstr.intern());
            singleton._extensions = extensions;
            singleton._privuse = privuse;
            if (singleton._extensions != null) {
                String kwdstr = (String)singleton._extensions.get(LOCALESINGLETON);
                if (kwdstr != null) {
                    singleton._keywords = parseKeywordSubtags(kwdstr, LOCALESEP);
                }
            }
            singleton = (LocaleExtension)EXTENSIONPOOL.register(singleton._canonical, singleton);
        }

        return singleton;
    }

    private static LocaleExtension getCanonical(String extstr) throws InvalidLocaleException {
        if (extstr == null || extstr.length() == 0) {
            return EMPTY_EXTENSION;
        }
        if (extstr.length() < MINLEN) {
            throw new InvalidLocaleException("Locale extension string '" + extstr + "' is too short.");
        }
        extstr = AsciiUtil.toLowerString(extstr);

        TreeMap/*<String,String>*/ extensions = null;
        TreeMap/*<String,String>*/ keywords = null;
        String ext = null;  // extensions part
        String prv = null;  // private use part

        if (extstr.charAt(0) == PRIVUSE.charAt(0)) {
            if (extstr.charAt(1) == LOCALESEP.charAt(0)) {
                prv = extstr.substring(2).intern();
            } else {
                throw new InvalidLocaleException("Locale extension string '" + extstr
                        + "' must start with a singleton segment.");
            }
        } else {
            int idx = extstr.indexOf(LOCALESEP + PRIVUSE + LOCALESEP);
            if (idx == -1) {
                ext = extstr;
            } else {
                ext = extstr.substring(0, idx);
                prv = extstr.substring(idx + 3).intern();
            }
        }

        if (ext != null) {
            //String[] subtags = ext.split(LOCALESEP);
            String[] subtags = Utility.split(ext, LOCALESEP.charAt(0));

            String letter = subtags[0];
            if (letter.length() != 1) {
                throw new InvalidLocaleException("Locale extension string '" + extstr
                        + "' must start with a singleton segment.");
            }

            extensions = new TreeMap/*<String,String>*/();
            StringBuffer buf = new StringBuffer();
            boolean inLocaleKeywords = false;
            String kwkey = null;

            for (int i = 1; i < subtags.length; i++) {
                if (subtags[i].length() == 0) {
                    throw new InvalidLocaleException("Locale extension string '" + extstr
                            + "' contains an empty segment.");
                }
                if (subtags[i].length() == 1) {
                    // next extension singleton
                    if (extensions.containsKey(subtags[i])) {
                        throw new InvalidLocaleException("Locale extension string '" + extstr
                                + "' contains multiple extensions: " + subtags[i]);
                    }

                    // write out the previous extension
                    if (inLocaleKeywords) {
                        if (kwkey != null) {
                            throw new InvalidLocaleException("Locale extension string '" + extstr
                                    + "' contains a malformed locale keywords: " + kwkey);
                        }
                        // creating a single string including locale keyword key/type pairs
                        mapToLocaleExtensionString(keywords, buf);
                        inLocaleKeywords = false;
                    }
                    if (buf.length() == 0) {
                        throw new InvalidLocaleException("Locale extension string '" + extstr
                                + "' contains an empty extension value.");
                    }
                    extensions.put(letter.intern(), buf.toString().intern());

                    // preparation for next extension
                    if (subtags[i].equals(LOCALESINGLETON)) {
                        keywords = new TreeMap/*<String,String>*/();
                        inLocaleKeywords = true;
                    }
                    letter = subtags[i];
                    buf.setLength(0);
                    continue;
                }

                if (inLocaleKeywords) {
                    if (kwkey == null) {
                        kwkey = subtags[i];
                    } else {
                        keywords.put(kwkey.intern(), subtags[i].intern());
                        kwkey = null;
                    }
                } else {
                    // append an extension subtag
                    if (buf.length() > 0) {
                        buf.append(LOCALESEP);
                    }
                    buf.append(subtags[i]);
                }
            }

            // process the last extension
            if (inLocaleKeywords) {
                if (kwkey != null) {
                    throw new InvalidLocaleException("Locale extension string '" + extstr
                            + "' contains a malformed locale keywords: " + kwkey);
                }
                // creating a single string including locale keyword key/type pairs
                mapToLocaleExtensionString(keywords, buf);
            }
            if (buf.length() == 0) {
                throw new InvalidLocaleException("Locale extension string '" + extstr
                        + "' contains an empty extension value.");
            }
            extensions.put(letter.intern(), buf.toString().intern());
        }

        // Reconstruct a locale extension string
        StringBuffer canonicalbuf = new StringBuffer();
        if (extensions != null) {
            Set/*<Map.Entry<String,String>>*/ entries = extensions.entrySet();
            //for (Map.Entry<String,String> entry : entries) {
            Iterator itr = entries.iterator();
            while (itr.hasNext()) {
                Map.Entry entry = (Map.Entry)itr.next();
                if (canonicalbuf.length() > 0) {
                    canonicalbuf.append(LOCALESEP);
                }
                canonicalbuf.append(entry.getKey());
                canonicalbuf.append(LOCALESEP);
                canonicalbuf.append(entry.getValue());
            }
        }
        if (prv != null) {
            if (canonicalbuf.length() > 0) {
                canonicalbuf.append(LOCALESEP);
            }
            canonicalbuf.append(PRIVUSE);
            canonicalbuf.append(LOCALESEP);
            canonicalbuf.append(prv);
        }

        // Finally, create an instance of LocaleExtension
        LocaleExtension le = new LocaleExtension(canonicalbuf.toString().intern());
        le._extensions = extensions;
        le._keywords = keywords;
        le._privuse = prv;

        return le;
    }

    public String toString() {
        return _canonical;
    }

    public int hashCode() {
        return _canonical.hashCode();
    }

    public String getPrivateUse() {
        return _privuse;
    }

    public Set/*<String>*/ getLocaleKeywordKeys() {
        if (_keywords != null) {
            return _keywords.keySet();
        }
        return null;
    }

    public boolean containsLocaleKeywordKey(String key) {
        if (_keywords != null) {
            return _keywords.containsKey(key);
        }
        return false;
    }

    public String getLocaleKeywordType(String key) {
        if (_keywords != null) {
            return (String)_keywords.get(key);
        }
        return null;
    }

    public Set/*<String>*/ getExtensionKeys() {
        if (_extensions != null) {
            return _extensions.keySet();
        }
        return null;
    }

    public boolean containsExtensionKey(String key) {
        if (_extensions != null) {
            return _extensions.containsKey(key);
        }
        return false;
    }

    public String getExtensionValue(String key) {
        if (_extensions != null) {
            return (String)_extensions.get(key);
        }
        return null;
    }

    static String mapToLocaleExtensionString(Map/*<String,String>*/ map, StringBuffer buf) {
      Set/*<Map.Entry<String,String>>*/ entries = map.entrySet();
      Iterator itr = entries.iterator();
      //for (Map.Entry<String,String> entry : entries) {
      while (itr.hasNext()) {
          Map.Entry entry = (Map.Entry)itr.next();
          if (buf.length() > 0) {
              buf.append(LOCALESEP);
          }
          buf.append(entry.getKey());
          buf.append(LOCALESEP);
          buf.append(entry.getValue());
      }
      return buf.toString();
    }

    static TreeMap/*<String,String>*/ parseKeywordSubtags(String text, String delim) {
        if (text == null || text.length() == 0) {
            return null;
        }
        //String[] subtags = AsciiUtil.toLowerString(text).split(delim);
        String[] subtags = Utility.split(AsciiUtil.toLowerString(text), delim.charAt(0));
        if ((subtags.length % 2) != 0) {
            // number of keyword subtags must be even
            return null;
        }

        TreeMap/*<String,String>*/ keywords = new TreeMap/*<String,String>*/();
        int idx = 0;
        while (idx < subtags.length) {
            String key = subtags[idx++];
            String type = subtags[idx++];

            if ((keywords.put(key.intern(), type.intern())) != null) {
                return null;
            }
        }
        return keywords;
    }
}
