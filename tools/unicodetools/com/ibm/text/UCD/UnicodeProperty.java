package com.ibm.text.UCD;
public abstract class UnicodeProperty implements UCD_Types {
  
    protected UCD       ucd;
    protected boolean   isStandard = true;
    protected byte      type = NOT_DERIVED;
    protected boolean   hasUnassigned = false;
    protected boolean   valueVaries = false;
    protected byte      defaultValueStyle = SHORT;
    protected byte      defaultPropertyStyle = LONG;
    protected String    valueName;
    protected String    numberValueName;
    protected String    shortValueName;
    protected String    header;
    protected String    subheader;
    protected String    name;
    protected String    shortName;
    protected String    numberName;
      
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
       * What type is it?
       */
      public byte getType() { return type; }
      public void setType(byte in) { type = in; }
      
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
            switch (style) {
                case LONG: return name.toString();
                case SHORT: return shortName.toString();
                case NUMBER: return numberName.toString();
                default: throw new IllegalArgumentException("Bad property: " + style);
            }
      }
      
      public String getProperty() { return getProperty(NORMAL); }
      
      public void setProperty(byte style, String in) {
            if (style == NORMAL) style = defaultPropertyStyle;
            switch (style) {
              case LONG: name = in; break;
              case SHORT: shortName = in; break;
              case NUMBER: numberName = in; break;
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
            if (style == NORMAL) style = defaultValueStyle;
            switch (style) {
              case LONG: valueName = in; break;
              case SHORT: shortValueName = in; break;
              case NUMBER: numberValueName = in; break;
              default: throw new IllegalArgumentException("Bad value: " + style);
            }
      }
      
      public String getValue(byte style) {
            if (valueVaries) throw new IllegalArgumentException(
                "Value varies in " + getName(LONG) + "; call getValue(cp)");
            try {
                if (style == NORMAL) style = defaultValueStyle;
                switch (style) {
                    case LONG: return valueName.toString();
                    case SHORT: return shortValueName.toString();
                    case NUMBER: return numberValueName.toString();
                    default: throw new IllegalArgumentException("Bad property: " + style);
                }
            } catch (RuntimeException e) {
                throw new com.ibm.text.utility.ChainException("Unset value string in " + getName(LONG), null, e);
            }
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
    
