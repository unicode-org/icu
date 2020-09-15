// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;
import java.text.AttributedCharacterIterator;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

import com.ibm.icu.impl.FormattedStringBuilder;
import com.ibm.icu.impl.FormattedValueStringBuilderImpl;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.util.ULocale;

/**
 * A wrapper around LocalizedNumberFormatter implementing the Format interface, enabling improved
 * compatibility with other APIs. This class is serializable.
 */
public class LocalizedNumberFormatterAsFormat extends Format {

    private static final long serialVersionUID = 1L;

    private final transient LocalizedNumberFormatter formatter;

    // Even though the locale is inside the LocalizedNumberFormatter, we have to keep it here, too, because
    // LocalizedNumberFormatter doesn't have a getLocale() method, and ICU-TC didn't want to add one.
    private final transient ULocale locale;

    public LocalizedNumberFormatterAsFormat(LocalizedNumberFormatter formatter, ULocale locale) {
        this.formatter = formatter;
        this.locale = locale;
    }

    /**
     * Formats a Number using the wrapped LocalizedNumberFormatter. The provided object must be a Number.
     *
     * {@inheritDoc}
     */
    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        if (!(obj instanceof Number)) {
            throw new IllegalArgumentException();
        }
        DecimalQuantity dq = new DecimalQuantity_DualStorageBCD((Number) obj);
        FormattedStringBuilder string = new FormattedStringBuilder();
        formatter.formatImpl(dq, string);
        // always return first occurrence:
        pos.setBeginIndex(0);
        pos.setEndIndex(0);
        boolean found = FormattedValueStringBuilderImpl.nextFieldPosition(string, pos);
        if (found && toAppendTo.length() != 0) {
            pos.setBeginIndex(pos.getBeginIndex() + toAppendTo.length());
            pos.setEndIndex(pos.getEndIndex() + toAppendTo.length());
        }
        Utility.appendTo(string, toAppendTo);
        return toAppendTo;
    }

    /**
     * Formats a Number using the wrapped LocalizedNumberFormatter. The provided object must be a Number.
     *
     * {@inheritDoc}
     */
    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        if (!(obj instanceof Number)) {
            throw new IllegalArgumentException();
        }
        return formatter.format((Number) obj).toCharacterIterator();
    }

    /**
     * Not supported. This method will throw UnsupportedOperationException.
     */
    @Override
    public Object parseObject(String source, ParsePosition pos) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the LocalizedNumberFormatter that this wrapper class uses to format numbers.
     *
     * @return The unwrapped LocalizedNumberFormatter.
     */
    public LocalizedNumberFormatter getNumberFormatter() {
        return formatter;
    }

    @Override
    public int hashCode() {
        return formatter.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof LocalizedNumberFormatterAsFormat)) {
            return false;
        }
        return formatter.equals(((LocalizedNumberFormatterAsFormat) other).getNumberFormatter());
    }

    private Object writeReplace() throws ObjectStreamException {
        Proxy proxy = new Proxy();
        proxy.languageTag = locale.toLanguageTag();
        proxy.skeleton = formatter.toSkeleton();
        return proxy;
    }

    static class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        String languageTag;
        String skeleton;

        // Must have public constructor, to enable Externalizable
        public Proxy() {
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeByte(0); // version
            out.writeUTF(languageTag);
            out.writeUTF(skeleton);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            in.readByte(); // version
            languageTag = in.readUTF();
            skeleton = in.readUTF();
        }

        private Object readResolve() throws ObjectStreamException {
            return NumberFormatter.forSkeleton(skeleton)
                    .locale(ULocale.forLanguageTag(languageTag))
                    .toFormat();
        }
    }
}
