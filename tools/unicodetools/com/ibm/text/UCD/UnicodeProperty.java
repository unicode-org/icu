package com.ibm.text.UCD;
public abstract class UnicodeProperty implements UCD_Types {
  
    protected UCD       ucd;
    protected boolean   isStandard = true;
    protected boolean   hasUnassigned = false;
    protected boolean   valueVaries = false;
    protected byte      defaultValueStyle = SHORT;
    protected byte      defaultPropertyStyle = LONG;
    protected String    valueName = "YES";
    protected String    shortValueName = "Y";
    protected String    header;
    protected String    subheader;
    protected String    name;
    protected String    shortName;
      
      /**
       * Return the UCD in use
       */
      public UCD getUCD() { return ucd; }
      
      /**
       * Is it part of the standard, or just for my testing?
       */
      public boolean isStandard() { return isStandard; }
      public void setStandard(boolean in) { isStandard = in; }
      
      /**
       * Does it apply to any unassigned characters?
       */
      public boolean hasUnassigned() { return hasUnassigned; }
      public void setHasUnassigned(boolean in) { hasUnassigned = in; }
      
      /** Header used in DerivedXXX files
       */
      public String getHeader() { return header; }
      public void setHeader(String in) { header = in; }

      /** Header used in DerivedXXX files
       */
      public String getSubHeader() { return subheader; }
      public void setSubHeader(String in) { subheader = in; }

      /**
       * Get the full name. Style is SHORT, NORMAL, LONG
       */
      public String getFullName(byte style) { 
          return getProperty(style) + "=" + getValue(style);
      }
      
      public String getFullName() { 
          return getFullName(NORMAL);
      }
      /**
       * Get the property name. Style is SHORT, NORMAL, LONG
       */
      public String getProperty(byte style) { 
          if (style == NORMAL) style = defaultPropertyStyle;
          return style < LONG ? shortName : name;
      }
      
      public String getProperty() { return getProperty(NORMAL); }
      
      public void setProperty(byte style, String in) {
            switch (style) {
              case LONG: name = in; break;
              case SHORT: shortName = in; break;
              default: throw new IllegalArgumentException("Bad property: " + style);
            }
      }
      
      /**
       * Get the value name. Style is SHORT, NORMAL, LONG
       * "" if hasValue is false
       * MUST OVERRIDE getValue(cp...) if valueVaries
       */
      public String getValue(int cp, byte style) { 
            if (!hasValue(cp)) return "";
            return getValue(style);
      }
      
      public String getValue(int cp) { return getValue(cp, NORMAL); }

      public void setValue(byte style, String in) {
            if (valueVaries) throw new IllegalArgumentException("Can't set varying value: " + style);
            switch (style) {
              case LONG: valueName = in; break;
              case SHORT: shortValueName = in; break;
              default: throw new IllegalArgumentException("Bad value: " + style);
            }
      }
      
      public String getValue(byte style) {
            if (valueVaries) throw new IllegalArgumentException("Value varies; call getValue(cp)");
            if (style == NORMAL) style = defaultValueStyle;
            return style < LONG ? shortValueName : valueName;
      }
      
      /**
       * Does getProperty vary in contents?
       */
      public boolean valueVaries() { return valueVaries; }
      public void setValueVaries(boolean in) { valueVaries = in; }
      
      /**
       * Does it have the propertyValue?
       */
      abstract boolean hasValue(int cp);
      
      ///////////////////////////////////////////
      
      // Old Name for compatibility
      boolean isTest() { return isStandard(); }
      String getName(byte style) { return getProperty(style); }
      String getName() { return getProperty(); }
      
  }
    
