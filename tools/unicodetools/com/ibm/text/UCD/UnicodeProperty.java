package com.ibm.text.UCD;
public abstract class UnicodeProperty implements UCD_Types {
  
    protected UCD ucd;
    protected boolean isStandard = true;
    protected byte defaultStyle = LONG;
    protected String name, shortName, header;
      
      // Old Names for compatibility
      boolean isTest() { return isStandard(); }
      
      /**
       * Is it part of the standard, or just for my testing?
       */
      public boolean isStandard() { return isStandard; }
      
      /**
       * Get the property name. Style is SHORT, NORMAL, LONG
       */
      public String getName(byte style) { 
          if (style == NORMAL) style = defaultStyle;
          return style < LONG ? shortName : name;
      }
      
      /** Header used in DerivedXXX files
       */
      public String getHeader() { return header; }
      
      /**
       * Does getProperty vary in contents?
       */
      public boolean propertyVaries() { return false; }
      
      /**
       * Get the property value as a string, or "" if hasProperty is false
       */
      public String getProperty(int cp) { return hasProperty(cp) ? name : ""; }
      
      /**
       * Does it have the propertyValue
       */
      abstract boolean hasProperty(int cp);
  }
    
