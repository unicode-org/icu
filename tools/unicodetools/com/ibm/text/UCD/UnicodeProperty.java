package com.ibm.text.UCD;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.text.utility.*;

public abstract class UnicodeProperty implements UCD_Types {
  
    // TODO: turn all of these into privates, and use setters only
    
    protected UCD       ucd;
    protected boolean   isStandard = true;
    protected byte      type = NOT_DERIVED;
    private byte        valueType = BINARY;
    protected boolean   hasUnassigned = false;
    protected boolean   isBinary = true;
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
    protected boolean   skeletonize = true;
      
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
       * What type is it? DERIVED..
       */
      public byte getType() { return type; }
      public void setType(byte in) { type = in; }
      
      /**
       * Does getProperty vary in contents? ENUMERATED,...
       */
      public byte getValueType() { return valueType; }
      public void setValueType(byte in) { valueType = in; }
            
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
                case LONG: return skeletonize ? Utility.getUnskeleton(name.toString(), false) : name.toString();
                case SHORT: return shortName.toString();
                case NUMBER: return numberName.toString();
                default: throw new IllegalArgumentException("Bad property: " + style);
            }
      }
      
      public String getProperty() { return getProperty(NORMAL); }
      
      public void setProperty(byte style, String in) {
            if (style == NORMAL) style = defaultPropertyStyle;
            switch (style) {
              case LONG: name = Utility.getUnskeleton(in, false); break;
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
            if (getValueType() < BINARY) throw new IllegalArgumentException("Can't set varying value: " + style);
            if (style == NORMAL) style = defaultValueStyle;
            switch (style) {
              case LONG: valueName = Utility.getUnskeleton(in, false); break;
              case SHORT: shortValueName = in; break;
              case NUMBER: numberValueName = in; break;
              default: throw new IllegalArgumentException("Bad value: " + style);
            }
      }
      
      public String getValue(byte style) {
            if (getValueType() < BINARY) throw new IllegalArgumentException(
                "Value varies in " + getName(LONG) + "; call getValue(cp)");
            try {
                if (style == NORMAL) style = defaultValueStyle;
                switch (style) {
                    case LONG: return Utility.getUnskeleton(valueName.toString(), false);
                    case SHORT: return shortValueName.toString();
                    case NUMBER: return numberValueName.toString();
                    default: throw new IllegalArgumentException("Bad property: " + style);
                }
            } catch (RuntimeException e) {
                throw new com.ibm.text.utility.ChainException("Unset value string in " + getName(LONG), null, e);
            }
      }
      
      /**
       * special hack for NFD/NFKD
       */
		public String getListingValue(int cp) {
        	if (getValueType() != BINARY) return getValue(cp, LONG);
        	return getProperty(LONG);
		}
      
      /**
       * Does it have the propertyValue?
       */
      abstract public boolean hasValue(int cp);
      
      /**
       * Get the set of characters it contains
       */
      
      private UnicodeSet cache = null;
      
      public UnicodeSet getSet() {
        if (cache == null) {
            cache = new UnicodeSet();
            for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                if (hasValue(cp)) cache.add(cp);
            }
        }
        return (UnicodeSet) cache.clone();
      }
      
      ///////////////////////////////////////////
      
      // Old Name for compatibility
      boolean isTest() { return isStandard(); }
      String getName(byte style) { return getProperty(style); }
      String getName() { return getProperty(); }
      
  }
    
