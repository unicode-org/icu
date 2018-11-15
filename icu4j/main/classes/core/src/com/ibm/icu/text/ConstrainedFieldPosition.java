// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.text;

import java.text.Format.Field;

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
 * @draft ICU 64
 * @provisional This API might change or be removed in a future release.
 */
public class ConstrainedFieldPosition {

    /**
     * Represents the type of constraint for ConstrainedFieldPosition.
     *
     * Constraints are used to control the behavior of iteration in FormattedValue.
     *
     * @draft ICU 64
     * @provisional This API might change or be removed in a future release.
     */
    public enum ConstraintType {
        /**
         * Represents the lack of a constraint.
         *
         * This is the return value of {@link #getConstraintType}
         * if no "constrain" methods were called.
         *
         * @draft ICU 64
         * @provisional This API might change or be removed in a future release.
         */
        NONE,

        /**
         * Represents that the field class is constrained.
         * Use {@link #getClassConstraint} to access the class.
         *
         * This is the return value of @link #getConstraintType}
         * after {@link #constrainClass} is called.
         *
         * FormattedValue implementations should not change the field when this constraint is active.
         *
         * @draft ICU 64
         * @provisional This API might change or be removed in a future release.
         */
        CLASS,

        /**
         * Represents that the field is constrained.
         * Use {@link #getField} to access the field.
         *
         * This is the return value of @link #getConstraintType}
         * after {@link #constrainField} is called.
         *
         * FormattedValue implementations should not change the field when this constraint is active.
         *
         * @draft ICU 64
         * @provisional This API might change or be removed in a future release.
         */
        FIELD
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
     * @draft ICU 64
     * @provisional This API might change or be removed in a future release.
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
     * @draft ICU 64
     * @provisional This API might change or be removed in a future release.
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
     * skipped unless they have the given category and field.
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
     * @draft ICU 64
     * @provisional This API might change or be removed in a future release.
     */
    public void constrainField(Field field) {
        if (field == null) {
            throw new IllegalArgumentException("Cannot constrain on null field");
        }
        fConstraint = ConstraintType.FIELD;
        fClassConstraint = Object.class;
        fField = field;
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
     * @draft ICU 64
     * @provisional This API might change or be removed in a future release.
     */
    public void constrainClass(Class<?> classConstraint) {
        if (classConstraint == null) {
            throw new IllegalArgumentException("Cannot constrain on null field class");
        }
        fConstraint = ConstraintType.CLASS;
        fClassConstraint = classConstraint;
        fField = null;
    }

    /**
     * Gets the currently active constraint.
     *
     * @return The currently active constraint type.
     * @draft ICU 64
     * @provisional This API might change or be removed in a future release.
     */
    public ConstraintType getConstraintType() {
        return fConstraint;
    }

    /**
     * Gets the class on which field positions are currently constrained.
     *
     * @return The class constraint from {@link #constrainClass}, or Object.class by default.
     * @draft ICU 64
     * @provisional This API might change or be removed in a future release.
     */
    public Class<?> getClassConstraint() {
        return fClassConstraint;
    }

    /**
     * Gets the field for the current position.
     *
     * If a field constraint was set, this function returns the constrained
     * field. Otherwise, the return value is well-defined and non-null only after
     * FormattedValue#nextPosition returns TRUE.
     *
     * @return The field saved in the instance. See above for null conditions.
     * @draft ICU 64
     * @provisional This API might change or be removed in a future release.
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
     * @draft ICU 64
     * @provisional This API might change or be removed in a future release.
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
     * @draft ICU 64
     * @provisional This API might change or be removed in a future release.
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
     * @draft ICU 64
     * @provisional This API might change or be removed in a future release.
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
     * @draft ICU 64
     * @provisional This API might change or be removed in a future release.
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
     * @draft ICU 64
     * @provisional This API might change or be removed in a future release.
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
     *            The new field value.
     * @param start
     *            The new inclusive start index.
     * @param limit
     *            The new exclusive end index.
     * @draft ICU 64
     * @provisional This API might change or be removed in a future release.
     */
    public void setState(Field field, Object value, int start, int limit) {
        fField = field;
        fValue = value;
        fStart = start;
        fLimit = limit;
    }

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
