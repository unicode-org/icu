/**
 * The Builder class for DictionaryBasedBreakIterator inherits almost all of
 * its functionality from the Builder class for RuleBasedBreakIterator, but
 * extends it with extra logic to handle the "<dictionary>" token
 */
protected class Builder extends RuleBasedBreakIterator.Builder {

    /**
     * A CharSet that contains all the characters represented in the dictionary
     */
    private CharSet dictionaryChars = new CharSet();
    private String dictionaryExpression = "";

    /**
     * No special initialization
     */
    public Builder() {
    }

    /**
     * We override handleSpecialSubstitution() to add logic to handle
     * the <dictionary> tag.  If we see a substitution named "<dictionary>",
     * parse the substitution expression and store the result in
     * dictionaryChars.
     */
    protected void handleSpecialSubstitution(String replace, String replaceWith,
                                             int startPos, String description) {
        super.handleSpecialSubstitution(replace, replaceWith, startPos, description);

        if (replace.equals("<dictionary>")) {
            if (replaceWith.charAt(0) == '(') {
                error("Dictionary group can't be enclosed in (", startPos, description);
            }
            dictionaryExpression = replaceWith;
            dictionaryChars = CharSet.parseString(replaceWith);
        }
    }

    /**
     * The other half of the logic to handle the dictionary characters happens here.
     * After the inherited builder has derived the real character categories, we
     * set up the categoryFlags array in the iterator.  This array contains "true"
     * for every character category that includes a dictionary character.
     */
    protected void buildCharCategories(Vector tempRuleList) {
        super.buildCharCategories(tempRuleList);

        categoryFlags = new boolean[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            CharSet cs = (CharSet)categories.elementAt(i);
            if (!(cs.intersection(dictionaryChars).empty())) {
                categoryFlags[i] = true;
            }
        }
    }

    // This function is actually called by RuleBasedBreakIterator.buildCharCategories(),
    // which is called by the function above.  This gives us a way to create a separate
    // character category for the dictionary characters even when RuleBasedBreakIterator
    // isn't making a distinction
    protected void mungeExpressionList(Hashtable expressions) {
        expressions.put(dictionaryExpression, dictionaryChars);
    }
}
