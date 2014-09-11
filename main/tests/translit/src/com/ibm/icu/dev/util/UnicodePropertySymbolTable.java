/*
 *******************************************************************************
 * Copyright (C) 1996-2012, Google, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import com.ibm.icu.dev.util.UnicodeProperty.PatternMatcher;
import com.ibm.icu.impl.UnicodeRegex;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

/**
 * Allows for overriding the parsing of UnicodeSet property patterns.
 * <p>
 * WARNING: If this UnicodePropertySymbolTable is used with {@code UnicodeSet.setDefaultXSymbolTable}, and the
 * Unassigned characters (gc=Cn) are different than in ICU other than in ICU, you MUST call
 * {@code UnicodeProperty.ResetCacheProperties} afterwards. If you then call {@code UnicodeSet.setDefaultXSymbolTable}
 * with null to clear the value, you MUST also call {@code UnicodeProperty.ResetCacheProperties}.
 * 
 * @author markdavis
 */
public class UnicodePropertySymbolTable extends UnicodeSet.XSymbolTable {
    UnicodeRegex unicodeRegex;
    final UnicodeProperty.Factory factory;

    public UnicodePropertySymbolTable(UnicodeProperty.Factory factory) {
      unicodeRegex = new UnicodeRegex().setSymbolTable(this);
      this.factory = factory;
    }


    //    public boolean applyPropertyAlias0(String propertyName,
    //            String propertyValue, UnicodeSet result) {
    //      if (!propertyName.contains("*")) {
    //        return applyPropertyAlias(propertyName, propertyValue, result);
    //      }
    //      String[] propertyNames = propertyName.split("[*]");
    //      for (int i = propertyNames.length - 1; i >= 0; ++i) {
    //        String pname = propertyNames[i];
    //        
    //      }
    //      return null;
    //    }

    public boolean applyPropertyAlias(String propertyName,
            String propertyValue, UnicodeSet result) {
      boolean status = false;
      boolean invert = false;
      int posNotEqual = propertyName.indexOf('\u2260');
      int posColon = propertyName.indexOf(':');
      if (posNotEqual >= 0 || posColon >= 0) {
          if (posNotEqual < 0) posNotEqual = propertyName.length();
          if (posColon < 0) posColon = propertyName.length();
          int opPos = posNotEqual < posColon ? posNotEqual : posColon;
          propertyValue = propertyValue.length() == 0 ? propertyName.substring(opPos+1) 
                  : propertyName.substring(opPos+1) + "=" + propertyValue;
          propertyName = propertyName.substring(0,opPos);
          if (posNotEqual < posColon) {
              invert = true;
          }
      }
      if (propertyName.endsWith("!")) {
        propertyName = propertyName.substring(0, propertyName.length() - 1);
        invert = !invert;
      }
      propertyValue = propertyValue.trim();
      if (propertyValue.length() != 0) {
        status = applyPropertyAlias0(propertyName, propertyValue, result);
      } else {
        try {
          status = applyPropertyAlias0("gc", propertyName, result);
        } catch (Exception e) {};
        if (!status) {
          try {
            status = applyPropertyAlias0("sc", propertyName, result);
          } catch (Exception e) {};
          if (!status) {
            try {
              status = applyPropertyAlias0(propertyName, "Yes", result);
            } catch (Exception e) {};
            if (!status) {
              status = applyPropertyAlias0(propertyName, "", result);
            }
          }
        }
      }
      if (status && invert) {
        result.complement();
      }
      return status;
    }

    static final HashMap<String,String[]> GC_REMAP = new HashMap();
    {
        GC_REMAP.put("c", "Cc Cf Cn Co Cs".split(" "));
        GC_REMAP.put("other", GC_REMAP.get("c"));
        
        GC_REMAP.put("l", "Ll Lm Lo Lt Lu".split(" "));
        GC_REMAP.put("letter", GC_REMAP.get("l"));
        
        GC_REMAP.put("lc", "Ll Lt Lu".split(" "));
        GC_REMAP.put("casedletter", GC_REMAP.get("lc"));
        
        GC_REMAP.put("m", "Mc Me Mn".split(" "));
        GC_REMAP.put("mark", GC_REMAP.get("m"));
        
        GC_REMAP.put("n", "Nd Nl No".split(" "));
        GC_REMAP.put("number", GC_REMAP.get("n"));
        
        GC_REMAP.put("p", "Pc Pd Pe Pf Pi Po Ps".split(" "));
        GC_REMAP.put("punctuation", GC_REMAP.get("p"));
        GC_REMAP.put("punct", GC_REMAP.get("p"));
        
        GC_REMAP.put("s", "Sc Sk Sm So".split(" "));
        GC_REMAP.put("symbol", GC_REMAP.get("s"));
        
        GC_REMAP.put("z", "Zl Zp Zs".split(" "));
        GC_REMAP.put("separator", GC_REMAP.get("z"));
    }
    
    public boolean applyPropertyAlias0(String propertyName,
            String propertyValue, UnicodeSet result) {
      result.clear();
      UnicodeProperty prop = factory.getProperty(propertyName);
      String canonicalName = prop.getName();
      boolean isAge = UnicodeProperty.equalNames("Age", canonicalName);

      // Hack for special GC values
      if (canonicalName.equals("General_Category")) {
          String[] parts = GC_REMAP.get(UnicodeProperty.toSkeleton(propertyValue));
          if (parts != null) {
              for (String part : parts) {
                  prop.getSet(part, result);
              }
              return true;
          }
      }

      PatternMatcher patternMatcher = null;
      if (propertyValue.length() > 1 && propertyValue.startsWith("/") && propertyValue.endsWith("/")) {
        String fixedRegex = unicodeRegex.transform(propertyValue.substring(1, propertyValue.length() - 1));
        patternMatcher = new UnicodeProperty.RegexMatcher().set(fixedRegex);
      }
      UnicodeProperty otherProperty = null;
      boolean testCp = false;
      if (propertyValue.length() > 1 && propertyValue.startsWith("@") && propertyValue.endsWith("@")) {
        String otherPropName = propertyValue.substring(1, propertyValue.length() - 1).trim();
        if ("cp".equalsIgnoreCase(otherPropName)) {
          testCp = true;
        } else {
          otherProperty = factory.getProperty(otherPropName);
        }
      }
      if (prop != null) {
        UnicodeSet set;
        if (testCp) {
          set = new UnicodeSet();
          for (int i = 0; i <= 0x10FFFF; ++i) {
            if (UnicodeProperty.equals(i, prop.getValue(i))) {
              set.add(i);
            }
          }
        } else if (otherProperty != null) {
          set = new UnicodeSet();
          for (int i = 0; i <= 0x10FFFF; ++i) {
            String v1 = prop.getValue(i);
            String v2 = otherProperty.getValue(i);
            if (UnicodeProperty.equals(v1, v2)) {
              set.add(i);
            }
          }
        } else if (patternMatcher == null) {
          if (!isValid(prop, propertyValue)) {
            throw new IllegalArgumentException("The value '" + propertyValue + "' is illegal. Values for " + propertyName
                    + " must be in "
                    + prop.getAvailableValues() + " or in " + prop.getValueAliases());
          }
          if (isAge) {
            set = prop.getSet(new ComparisonMatcher(propertyValue, Relation.geq));
          } else {
            set = prop.getSet(propertyValue);
          }
        } else if (isAge) {
          set = new UnicodeSet();
          List<String> values = prop.getAvailableValues();
          for (String value : values) {
            if (patternMatcher.matches(value)) {
              for (String other : values) {
                if (other.compareTo(value) <= 0) {
                  set.addAll(prop.getSet(other));
                }
              }
            }
          }
        } else {
          set = prop.getSet(patternMatcher);
        }
        result.addAll(set);
        return true;
      }
      throw new IllegalArgumentException("Illegal property: " + propertyName);
    }

    

    private boolean isValid(UnicodeProperty prop, String propertyValue) {
//      if (prop.getName().equals("General_Category")) {
//        if (propertyValue)
//      }
      return prop.isValidValue(propertyValue);
    }

    public enum Relation {less, leq, equal, geq, greater}

    public static class ComparisonMatcher implements PatternMatcher {
        Relation relation;
        static Comparator comparator = new UTF16.StringComparator(true, false,0);

        String pattern;

        public ComparisonMatcher(String pattern, Relation comparator) {
          this.relation = comparator;
          this.pattern = pattern;
        }

        public boolean matches(Object value) {
          int comp = comparator.compare(pattern, value.toString());
          switch (relation) {
          case less: return comp < 0;
          case leq: return comp <= 0;
          default: return comp == 0;
          case geq: return comp >= 0;
          case greater: return comp > 0;
          }
        }

        public PatternMatcher set(String pattern) {
          this.pattern = pattern;
          return this;
        }
      }
  }
