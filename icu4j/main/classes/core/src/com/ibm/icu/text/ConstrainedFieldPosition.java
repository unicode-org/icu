// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.text;

import java.text.Format.Field;
import java.util.Objects;

/**
 * Represents a span of a string containing a given field.
 *
 * This class differs from FieldPosition in the following ways:
 *
 *   1. It has information on the field category.
 *   2. It allows you to set constraints to use when iterating over field positions.
 *   3. It is used for the newer FormattedValue APIs.
 *
 * @author sffc
 * @stable ICU 64
 */
public class ConstrainedFieldPosition {

    /**
     * Represents the type of constraint for ConstrainedFieldPosition.
     *
     * Constraints are used to control the behavior of iteration in FormattedValue.
     */
    private enum ConstraintType {
        /**
         * Represents the lack of a constraint.
         *
         * This is the value of fConstraint
         * if no "constrain" methods were called.
         */
        NONE,

        /**
         * Represents that the field class is constrained.
         *
         * This is the value of fConstraint
         * after {@link #constrainClass} is called.
         *
         * FormattedValue implementations should not change the field class when this constraint is active.
         */
        CLASS,

        /**
         * Represents that the field is constrained.
         *
         * This is the value of fConstraint
         * after {@link #constrainField} is called.
         *
         * FormattedValue implementations should not change the field when this constraint is active.
         */
        FIELD,

        /**
         * Represents that the field value is constrained.
         *
         * This is the value of fConstraint
         * after {@link #constrainField} is called.
         *
         * FormattedValue implementations should not change the field or value with this constraint.
         */
        VALUE
    };

    private ConstraintType fConstraint;
    private Class<?> fClassConstraint;
    private Field fField;
    private Object fValue;
    private int fStart;
    private int fLimit;
    private long fContext;

    /**
     * Initializes a CategoryFieldPosition.
     *
     * By default, the CategoryFieldPosition has no iteration constraints.
     *
     * @stable ICU 64
     */
    public ConstrainedFieldPosition() {
        reset();
    }

    /**
     * Resets this ConstrainedFieldPosition to its initial state, as if it were newly created:
     *
     * - Removes any constraints that may have been set on the instance.
     * - Resets the iteration position.
     *
     * @stable ICU 64
     */
    public void reset() {
        fConstraint = ConstraintType.NONE;
        fClassConstraint = Object.class;
        fField = null;
        fValue = null;
        fStart = 0;
        fLimit = 0;
        fContext = 0;
    }

    /**
     * Sets a constraint on the field.
     *
     * When this instance of ConstrainedFieldPosition is passed to {@link FormattedValue#nextPosition}, positions are
     * skipped unless they have the given field.
     *
     * Any previously set constraints are cleared.
     *
     * For example, to loop over all grouping separators:
     *
     * <pre>
     * ConstrainedFieldPosition cfpos;
     * cfpos.constrainField(NumberFormat.Field.GROUPING_SEPARATOR);
     * while (fmtval.nextPosition(cfpos)) {
     *   // handle the grouping separator position
     * }
     * </pre>
     *
     * Changing the constraint while in the middle of iterating over a FormattedValue
     * does not generally have well-defined behavior.
     *
     * @param field
     *            The field to fix when iterating.
     * @stable ICU 64
     */
    public void constrainField(Field field) {
        if (field == null) {
            throw new IllegalArgumentException("Cannot constrain on null field");
        }
        fConstraint = ConstraintType.FIELD;
        fClassConstraint = Object.class;
        fField = field;
        fValue = null;
    }

    /**
     * Sets a constraint on the field class.
     *
     * When this instance of ConstrainedFieldPosition is passed to {@link FormattedValue#nextPosition}, positions are
     * skipped unless the field is an instance of the class constraint, including subclasses.
     *
     * Any previously set constraints are cleared.
     *
     * For example, to loop over only the number-related fields:
     *
     * <pre>
     * ConstrainedFieldPosition cfpos;
     * cfpos.constrainClass(NumberFormat.Field.class);
     * while (fmtval.nextPosition(cfpos)) {
     *   // handle the number-related field position
     * }
     * </pre>
     *
     * @param classConstraint
     *            The field class to fix when iterating.
     * @stable ICU 64
     */
    public void constrainClass(Class<?> classConstraint) {
        if (classConstraint == null) {
            throw new IllegalArgumentException("Cannot constrain on null field class");
        }
        fConstraint = ConstraintType.CLASS;
        fClassConstraint = classConstraint;
        fField = null;
        fValue = null;
    }

    /**
     * Sets a constraint on field and field value.
     *
     * When this instance of ConstrainedFieldPosition is passed to {@link FormattedValue#nextPosition}, positions are
     * skipped unless both the field and the field value are equal.
     *
     * Any previously set constraints are cleared.
     *
     * For example, to find the span a date interval corresponding to the first date:
     *
     * <pre>
     * ConstrainedFieldPosition cfpos;
     * cfpos.constrainFieldAndValue(DateIntervalFormat.SpanField.DATE_INTERVAL_SPAN, 0);
     * while (fmtval.nextPosition(cfpos)) {
     *   // handle the span of the first date in the date interval
     * }
     * </pre>
     *
     * @param field The field to fix when iterating.
     * @param fieldValue The field value to fix when iterating.
     * @internal ICU 64 technology preview
     * @deprecated This API is for technology preview and might be changed or removed in a future release.
     */
    @Deprecated
    public void constrainFieldAndValue(Field field, Object fieldValue) {
        fConstraint = ConstraintType.VALUE;
        fClassConstraint = Object.class;
        fField = field;
        fValue = fieldValue;
    }

    /**
     * Gets the field for the current position.
     *
     * The return value is well-defined and non-null only after
     * FormattedValue#nextPosition returns TRUE.
     *
     * @return The field saved in the instance. See above for null conditions.
     * @stable ICU 64
     */
    public Field getField() {
        return fField;
    }

    /**
     * Gets the INCLUSIVE start index for the current position.
     *
     * The return value is well-defined only after FormattedValue#nextPosition returns TRUE.
     *
     * @return The start index saved in the instance.
     * @stable ICU 64
     */
    public int getStart() {
        return fStart;
    }

    /**
     * Gets the EXCLUSIVE end index stored for the current position.
     *
     * The return value is well-defined only after FormattedValue#nextPosition returns TRUE.
     *
     * @return The end index saved in the instance.
     * @stable ICU 64
     */
    public int getLimit() {
        return fLimit;
    }

    /**
     * Gets the value associated with the current field position. The field value is often not set.
     *
     * The return value is well-defined only after FormattedValue#nextPosition returns TRUE.
     *
     * @return The value for the current position. Might be null.
     * @stable ICU 64
     */
    public Object getFieldValue() {
        return fValue;
    }

    /**
     * Gets an int64 that FormattedValue implementations may use for storage.
     *
     * The initial value is zero.
     *
     * Users of FormattedValue should not need to call this method.
     *
     * @return The current iteration context from {@link #setInt64IterationContext}.
     * @stable ICU 64
     */
    public long getInt64IterationContext() {
        return fContext;
    }

    /**
     * Sets an int64 that FormattedValue implementations may use for storage.
     *
     * Intended to be used by FormattedValue implementations.
     *
     * @param context
     *            The new iteration context.
     * @stable ICU 64
     */
    public void setInt64IterationContext(long context) {
        fContext = context;
    }

    /**
     * Sets new values for the primary public getters.
     *
     * Intended to be used by FormattedValue implementations.
     *
     * It is up to the implementation to ensure that the user-requested
     * constraints are satisfied. This method does not check!
     *
     * @param field
     *            The new field.
     * @param value
     *            The new field value. Should be null if there is no value.
     * @param start
     *            The new inclusive start index.
     * @param limit
     *            The new exclusive end index.
     * @stable ICU 64
     */
    public void setState(Field field, Object value, int start, int limit) {
        // Check matchesField only as an assertion (debug build)
        if (field != null) {
            assert matchesField(field, value);
        }

        fField = field;
        fValue = value;
        fStart = start;
        fLimit = limit;
    }

    /**
     * Determines whether a given field and value should be included given the
     * constraints.
     *
     * Intended to be used by FormattedValue implementations.
     *
     * @param field The field to test.
     * @param fieldValue The field value to test. Should be null if there is no value.
     * @return Whether the field should be included given the constraints.
     * @stable ICU 64
     */
    public boolean matchesField(Field field, Object fieldValue) {
        if (field == null) {
            throw new IllegalArgumentException("field must not be null");
        }
        switch (fConstraint) {
        case NONE:
            return true;
        case CLASS:
            return fClassConstraint.isAssignableFrom(field.getClass());
        case FIELD:
            return fField == field;
        case VALUE:
            // Note: Objects.equals is Android API level 19 and Java 1.7
            return fField == field && Objects.equals(fValue, fieldValue);
        default:
            throw new AssertionError();
        }
    }

    /**
     * {@inheritDoc}
     * @stable ICU 64
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CFPos[");
        sb.append(fStart);
        sb.append('-');
        sb.append(fLimit);
        sb.append(' ');
        sb.append(fField);
        sb.append(']');
        return sb.toString();
    }
}
