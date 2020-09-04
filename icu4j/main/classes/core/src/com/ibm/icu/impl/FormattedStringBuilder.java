// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

// NumberFormat is imported only for the toDebugString() implementation.
import com.ibm.icu.text.NumberFormat;

/**
 * A StringBuilder optimized for formatting. It implements the following key features beyond a
 * normal JDK StringBuilder:
 *
 * <ol>
 * <li>Efficient prepend as well as append.
 * <li>Keeps tracks of Fields in an efficient manner.
 * <li>String operations are fast-pathed to code point operations when possible.
 * </ol>
 *
 * See also FormattedValueStringBuilderImpl.
 *
 * @author sffc (Shane Carr)
 */
public class FormattedStringBuilder implements CharSequence, Appendable {

    /** A constant, empty FormattedStringBuilder. Do NOT call mutative operations on this. */
    public static final FormattedStringBuilder EMPTY = new FormattedStringBuilder();

    char[] chars;
    Object[] fields;
    int zero;
    int length;

    /** Number of characters from the end where .append() operations insert. */
    int appendOffset = 0;

    /** Field applied when Appendable methods are used. */
    Object appendableField = null;

    public FormattedStringBuilder() {
        this(40);
    }

    public FormattedStringBuilder(int capacity) {
        chars = new char[capacity];
        fields = new Object[capacity];
        zero = capacity / 2;
        length = 0;
    }

    public FormattedStringBuilder(FormattedStringBuilder source) {
        copyFrom(source);
    }

    public void copyFrom(FormattedStringBuilder source) {
        chars = Arrays.copyOf(source.chars, source.chars.length);
        fields = Arrays.copyOf(source.fields, source.fields.length);
        zero = source.zero;
        length = source.length;
    }

    @Override
    public int length() {
        return length;
    }

    public int codePointCount() {
        return Character.codePointCount(this, 0, length());
    }

    @Override
    public char charAt(int index) {
        assert index >= 0;
        assert index < length;
        return chars[zero + index];
    }

    public Object fieldAt(int index) {
        assert index >= 0;
        assert index < length;
        return fields[zero + index];
    }

    public int getFirstCodePoint() {
        if (length == 0) {
            return -1;
        }
        return Character.codePointAt(chars, zero, zero + length);
    }

    public int getLastCodePoint() {
        if (length == 0) {
            return -1;
        }
        return Character.codePointBefore(chars, zero + length, zero);
    }

    public int codePointAt(int index) {
        return Character.codePointAt(chars, zero + index, zero + length);
    }

    public int codePointBefore(int index) {
        return Character.codePointBefore(chars, zero + index, zero);
    }

    public FormattedStringBuilder clear() {
        zero = getCapacity() / 2;
        length = 0;
        return this;
    }

    /**
     * Sets the index at which append operations insert. Defaults to the end.
     *
     * @param index The index at which append operations should insert.
     */
    public void setAppendIndex(int index) {
        appendOffset = length - index;
    }

    public int appendChar16(char codeUnit, Object field) {
        return insertChar16(length - appendOffset, codeUnit, field);
    }

    public int insertChar16(int index, char codeUnit, Object field) {
        int count = 1;
        int position = prepareForInsert(index, count);
        chars[position] = codeUnit;
        fields[position] = field;
        return count;
    }

    /**
     * Appends the specified codePoint to the end of the string.
     *
     * @return The number of chars added: 1 if the code point is in the BMP, or 2 otherwise.
     */
    public int appendCodePoint(int codePoint, Object field) {
        return insertCodePoint(length - appendOffset, codePoint, field);
    }

    /**
     * Inserts the specified codePoint at the specified index in the string.
     *
     * @return The number of chars added: 1 if the code point is in the BMP, or 2 otherwise.
     */
    public int insertCodePoint(int index, int codePoint, Object field) {
        int count = Character.charCount(codePoint);
        int position = prepareForInsert(index, count);
        Character.toChars(codePoint, chars, position);
        fields[position] = field;
        if (count == 2)
            fields[position + 1] = field;
        return count;
    }

    /**
     * Appends the specified CharSequence to the end of the string.
     *
     * @return The number of chars added, which is the length of CharSequence.
     */
    public int append(CharSequence sequence, Object field) {
        return insert(length - appendOffset, sequence, field);
    }

    /**
     * Inserts the specified CharSequence at the specified index in the string.
     *
     * @return The number of chars added, which is the length of CharSequence.
     */
    public int insert(int index, CharSequence sequence, Object field) {
        if (sequence.length() == 0) {
            // Nothing to insert.
            return 0;
        } else if (sequence.length() == 1) {
            // Fast path: on a single-char string, using insertCodePoint below is 70% faster than the
            // CharSequence method: 12.2 ns versus 41.9 ns for five operations on my Linux x86-64.
            return insertCodePoint(index, sequence.charAt(0), field);
        } else {
            return insert(index, sequence, 0, sequence.length(), field);
        }
    }

    /**
     * Inserts the specified CharSequence at the specified index in the string, reading from the
     * CharSequence from start (inclusive) to end (exclusive).
     *
     * @return The number of chars added, which is the length of CharSequence.
     */
    public int insert(int index, CharSequence sequence, int start, int end, Object field) {
        int count = end - start;
        int position = prepareForInsert(index, count);
        for (int i = 0; i < count; i++) {
            chars[position + i] = sequence.charAt(start + i);
            fields[position + i] = field;
        }
        return count;
    }

    /**
     * Replaces the chars between startThis and endThis with the chars between startOther and endOther of
     * the given CharSequence. Calling this method with startThis == endThis is equivalent to calling
     * insert.
     *
     * @return The number of chars added, which may be negative if the removed segment is longer than the
     *         length of the CharSequence segment that was inserted.
     */
    public int splice(
            int startThis,
            int endThis,
            CharSequence sequence,
            int startOther,
            int endOther,
            Object field) {
        int thisLength = endThis - startThis;
        int otherLength = endOther - startOther;
        int count = otherLength - thisLength;
        int position;
        if (count > 0) {
            // Overall, chars need to be added.
            position = prepareForInsert(startThis, count);
        } else {
            // Overall, chars need to be removed or kept the same.
            position = remove(startThis, -count);
        }
        for (int i = 0; i < otherLength; i++) {
            chars[position + i] = sequence.charAt(startOther + i);
            fields[position + i] = field;
        }
        return count;
    }

    /**
     * Appends the chars in the specified char array to the end of the string, and associates them with
     * the fields in the specified field array, which must have the same length as chars.
     *
     * @return The number of chars added, which is the length of the char array.
     */
    public int append(char[] chars, Object[] fields) {
        return insert(length - appendOffset, chars, fields);
    }

    /**
     * Inserts the chars in the specified char array at the specified index in the string, and associates
     * them with the fields in the specified field array, which must have the same length as chars.
     *
     * @return The number of chars added, which is the length of the char array.
     */
    public int insert(int index, char[] chars, Object[] fields) {
        assert fields == null || chars.length == fields.length;
        int count = chars.length;
        if (count == 0)
            return 0; // nothing to insert
        int position = prepareForInsert(index, count);
        for (int i = 0; i < count; i++) {
            this.chars[position + i] = chars[i];
            this.fields[position + i] = fields == null ? null : fields[i];
        }
        return count;
    }

    /**
     * Appends the contents of another {@link FormattedStringBuilder} to the end of this instance.
     *
     * @return The number of chars added, which is the length of the other {@link FormattedStringBuilder}.
     */
    public int append(FormattedStringBuilder other) {
        return insert(length - appendOffset, other);
    }

    /**
     * Inserts the contents of another {@link FormattedStringBuilder} into this instance at the given index.
     *
     * @return The number of chars added, which is the length of the other {@link FormattedStringBuilder}.
     */
    public int insert(int index, FormattedStringBuilder other) {
        if (this == other) {
            throw new IllegalArgumentException("Cannot call insert/append on myself");
        }
        int count = other.length;
        if (count == 0) {
            // Nothing to insert.
            return 0;
        }
        int position = prepareForInsert(index, count);
        for (int i = 0; i < count; i++) {
            this.chars[position + i] = other.charAt(i);
            this.fields[position + i] = other.fieldAt(i);
        }
        return count;
    }

    /**
     * Shifts around existing data if necessary to make room for new characters.
     *
     * @param index
     *            The location in the string where the operation is to take place.
     * @param count
     *            The number of chars (UTF-16 code units) to be inserted at that location.
     * @return The position in the char array to insert the chars.
     */
    private int prepareForInsert(int index, int count) {
        if (index == -1) {
            index = length;
        }
        if (index == 0 && zero - count >= 0) {
            // Append to start
            zero -= count;
            length += count;
            return zero;
        } else if (index == length && zero + length + count < getCapacity()) {
            // Append to end
            length += count;
            return zero + length - count;
        } else {
            // Move chars around and/or allocate more space
            return prepareForInsertHelper(index, count);
        }
    }

    private int prepareForInsertHelper(int index, int count) {
        // Java note: Keeping this code out of prepareForInsert() increases the speed of append
        // operations.
        int oldCapacity = getCapacity();
        int oldZero = zero;
        char[] oldChars = chars;
        Object[] oldFields = fields;
        if (length + count > oldCapacity) {
            int newCapacity = (length + count) * 2;
            int newZero = newCapacity / 2 - (length + count) / 2;

            char[] newChars = new char[newCapacity];
            Object[] newFields = new Object[newCapacity];

            // First copy the prefix and then the suffix, leaving room for the new chars that the
            // caller wants to insert.
            System.arraycopy(oldChars, oldZero, newChars, newZero, index);
            System.arraycopy(oldChars,
                    oldZero + index,
                    newChars,
                    newZero + index + count,
                    length - index);
            System.arraycopy(oldFields, oldZero, newFields, newZero, index);
            System.arraycopy(oldFields,
                    oldZero + index,
                    newFields,
                    newZero + index + count,
                    length - index);

            chars = newChars;
            fields = newFields;
            zero = newZero;
            length += count;
        } else {
            int newZero = oldCapacity / 2 - (length + count) / 2;

            // First copy the entire string to the location of the prefix, and then move the suffix
            // to make room for the new chars that the caller wants to insert.
            System.arraycopy(oldChars, oldZero, oldChars, newZero, length);
            System.arraycopy(oldChars,
                    newZero + index,
                    oldChars,
                    newZero + index + count,
                    length - index);
            System.arraycopy(oldFields, oldZero, oldFields, newZero, length);
            System.arraycopy(oldFields,
                    newZero + index,
                    oldFields,
                    newZero + index + count,
                    length - index);

            zero = newZero;
            length += count;
        }
        return zero + index;
    }

    /**
     * Removes the "count" chars starting at "index". Returns the position at which the chars were
     * removed.
     */
    private int remove(int index, int count) {
        int position = index + zero;
        System.arraycopy(chars, position + count, chars, position, length - index - count);
        System.arraycopy(fields, position + count, fields, position, length - index - count);
        length -= count;
        return position;
    }

    private int getCapacity() {
        return chars.length;
    }

    /** Note: this returns a FormattedStringBuilder. Do not return publicly. */
    @Override
    @Deprecated
    public CharSequence subSequence(int start, int end) {
        assert start >= 0;
        assert end <= length;
        assert end >= start;
        FormattedStringBuilder other = new FormattedStringBuilder(this);
        other.zero = zero + start;
        other.length = end - start;
        return other;
    }

    /** Use this instead of subSequence if returning publicly. */
    public String subString(int start, int end) {
        if (start < 0 || end > length || end < start) {
            throw new IndexOutOfBoundsException();
        }
        return new String(chars, start + zero, end - start);
    }

    /**
     * Returns the string represented by the characters in this string builder.
     *
     * <p>
     * For a string intended be used for debugging, use {@link #toDebugString}.
     */
    @Override
    public String toString() {
        return new String(chars, zero, length);
    }

    private static final Map<Object, Character> fieldToDebugChar = new HashMap<>();

    static {
        fieldToDebugChar.put(NumberFormat.Field.SIGN, '-');
        fieldToDebugChar.put(NumberFormat.Field.INTEGER, 'i');
        fieldToDebugChar.put(NumberFormat.Field.FRACTION, 'f');
        fieldToDebugChar.put(NumberFormat.Field.EXPONENT, 'e');
        fieldToDebugChar.put(NumberFormat.Field.EXPONENT_SIGN, '+');
        fieldToDebugChar.put(NumberFormat.Field.EXPONENT_SYMBOL, 'E');
        fieldToDebugChar.put(NumberFormat.Field.DECIMAL_SEPARATOR, '.');
        fieldToDebugChar.put(NumberFormat.Field.GROUPING_SEPARATOR, ',');
        fieldToDebugChar.put(NumberFormat.Field.PERCENT, '%');
        fieldToDebugChar.put(NumberFormat.Field.PERMILLE, '‰');
        fieldToDebugChar.put(NumberFormat.Field.CURRENCY, '$');
        fieldToDebugChar.put(NumberFormat.Field.MEASURE_UNIT, 'u');
        fieldToDebugChar.put(NumberFormat.Field.COMPACT, 'C');
    }

    /**
     * Returns a string that includes field information, for debugging purposes.
     *
     * <p>
     * For example, if the string is "-12.345", the debug string will be something like
     * "&lt;FormattedStringBuilder [-123.45] [-iii.ff]&gt;"
     *
     * @return A string for debugging purposes.
     */
    public String toDebugString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<FormattedStringBuilder [");
        sb.append(this.toString());
        sb.append("] [");
        for (int i = zero; i < zero + length; i++) {
            if (fields[i] == null) {
                sb.append('n');
            } else if (fieldToDebugChar.containsKey(fields[i])) {
                sb.append(fieldToDebugChar.get(fields[i]));
            } else {
                sb.append('?');
            }
        }
        sb.append("]>");
        return sb.toString();
    }

    /** @return A new array containing the contents of this string builder. */
    public char[] toCharArray() {
        return Arrays.copyOfRange(chars, zero, zero + length);
    }

    /** @return A new array containing the field values of this string builder. */
    public Object[] toFieldArray() {
        return Arrays.copyOfRange(fields, zero, zero + length);
    }

    /**
     * Call this method before using any of the Appendable overrides.
     *
     * @param field The field used when inserting strings.
     */
    public void setAppendableField(Object field) {
        appendableField = field;
    }

    /**
     * This method is provided for Java Appendable compatibility. In most cases, please use the append methods that take
     * a Field parameter. If you do use this method, you must call {@link #setAppendableField} first.
     */
    @Override
    public Appendable append(CharSequence csq) {
        assert appendableField != null;
        insert(length - appendOffset, csq, appendableField);
        return this;
    }

    /**
     * This method is provided for Java Appendable compatibility. In most cases, please use the append methods that take
     * a Field parameter. If you do use this method, you must call {@link #setAppendableField} first.
     */
    @Override
    public Appendable append(CharSequence csq, int start, int end) {
        assert appendableField != null;
        insert(length - appendOffset, csq, start, end, appendableField);
        return this;
    }

    /**
     * This method is provided for Java Appendable compatibility. In most cases, please use the append methods that take
     * a Field parameter. If you do use this method, you must call {@link #setAppendableField} first.
     */
    @Override
    public Appendable append(char c) {
        assert appendableField != null;
        insertChar16(length - appendOffset, c, appendableField);
        return this;
    }

    /**
     * @return Whether the contents and field values of this string builder are equal to the given chars
     *         and fields.
     * @see #toCharArray
     * @see #toFieldArray
     */
    public boolean contentEquals(char[] chars, Object[] fields) {
        if (chars.length != length)
            return false;
        if (fields.length != length)
            return false;
        for (int i = 0; i < length; i++) {
            if (this.chars[zero + i] != chars[i])
                return false;
            if (this.fields[zero + i] != fields[i])
                return false;
        }
        return true;
    }

    /**
     * @param other
     *            The instance to compare.
     * @return Whether the contents of this instance is currently equal to the given instance.
     */
    public boolean contentEquals(FormattedStringBuilder other) {
        if (length != other.length)
            return false;
        for (int i = 0; i < length; i++) {
            if (charAt(i) != other.charAt(i) || fieldAt(i) != other.fieldAt(i)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException("Don't call #hashCode() or #equals() on a mutable.");
    }

    @Override
    public boolean equals(Object other) {
        throw new UnsupportedOperationException("Don't call #hashCode() or #equals() on a mutable.");
    }
}
