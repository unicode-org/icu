// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import com.ibm.icu.message2.MFDataModel.CatchallKey;
import com.ibm.icu.message2.MFDataModel.Declaration;
import com.ibm.icu.message2.MFDataModel.Expression;
import com.ibm.icu.message2.MFDataModel.FunctionRef;
import com.ibm.icu.message2.MFDataModel.FunctionExpression;
import com.ibm.icu.message2.MFDataModel.InputDeclaration;
import com.ibm.icu.message2.MFDataModel.Literal;
import com.ibm.icu.message2.MFDataModel.LiteralExpression;
import com.ibm.icu.message2.MFDataModel.LiteralOrCatchallKey;
import com.ibm.icu.message2.MFDataModel.LiteralOrVariableRef;
import com.ibm.icu.message2.MFDataModel.LocalDeclaration;
import com.ibm.icu.message2.MFDataModel.Option;
import com.ibm.icu.message2.MFDataModel.PatternMessage;
import com.ibm.icu.message2.MFDataModel.SelectMessage;
import com.ibm.icu.message2.MFDataModel.VariableExpression;
import com.ibm.icu.message2.MFDataModel.VariableRef;
import com.ibm.icu.message2.MFDataModel.Variant;

// I can merge all this in the MFDataModel class and make it private
class MFDataModelValidator {
    private final MFDataModel.Message message;
    private final Set<String> declaredVars = new HashSet<>();

    MFDataModelValidator(MFDataModel.Message message) {
        this.message = message;
    }

    boolean validate() throws MFParseException {
        if (message instanceof PatternMessage) {
            validateDeclarations(((PatternMessage) message).declarations);
        } else if (message instanceof SelectMessage) {
            SelectMessage sm = (SelectMessage) message;
            validateDeclarations(sm.declarations);
            validateSelectors(sm.selectors);
            int selectorCount = sm.selectors.size();
            validateVariants(sm.variants, selectorCount);
        }
        return true;
    }

    private boolean validateVariants(List<Variant> variants, int selectorCount)
            throws MFParseException {
        if (variants == null || variants.isEmpty()) {
            error("Selection messages must have at least one variant");
        }

        // Look for an entry with all keys = '*'
        boolean hasUltimateFallback = false;
        Set<String> fakeKeys = new HashSet<>();
        for (Variant variant : variants) {
            if (variant.keys == null || variant.keys.isEmpty()) {
                error("Selection variants must have at least one key");
            }
            if (variant.keys.size() != selectorCount) {
                error("Selection variants must have the same number of variants as the selectors.");
            }
            int catchAllCount = 0;
            StringJoiner fakeKey = new StringJoiner("<<::>>");
            for (LiteralOrCatchallKey key : variant.keys) {
                if (key instanceof CatchallKey) {
                    catchAllCount++;
                    fakeKey.add(CatchallKey.AS_KEY_STRING);
                } else if (key instanceof Literal) {
                    fakeKey.add(((Literal) key).value);
                }
            }
            if (fakeKeys.contains(fakeKey.toString())) {
                error("Dumplicate combination of keys");
            } else {
                fakeKeys.add(fakeKey.toString());
            }
            if (catchAllCount == selectorCount) {
                hasUltimateFallback = true;
            }
        }
        if (!hasUltimateFallback) {
            error("There must be one variant with all the keys being '*'");
        }
        return true;
    }

    private boolean validateSelectors(List<Expression> selectors) throws MFParseException {
        if (selectors == null || selectors.isEmpty()) {
            error("Selection messages must have selectors");
        }
        return true;
    }

    /*
     * .input {$foo :number} .input {$foo} => ERROR
     * .input {$foo :number} .local $foo={$bar} => ERROR, local foo overrides an input
     * .local $foo={...} .local $foo={...} => ERROR, foo declared twice
     * .local $a={$foo} .local $b={$foo} => NOT AN ERROR (foo is used, not declared)
     * .local $a={:f opt=$foo} .local $foo={$foo} => ERROR, foo declared after beeing used in opt
     */
    private boolean validateDeclarations(List<Declaration> declarations) throws MFParseException {
        if (declarations == null || declarations.isEmpty()) {
            return true;
        }
        for (Declaration declaration : declarations) {
            if (declaration instanceof LocalDeclaration) {
                LocalDeclaration ld = (LocalDeclaration) declaration;
                validateExpression(ld.value, false);
                addVariableDeclaration(ld.name);
            } else if (declaration instanceof InputDeclaration) {
                InputDeclaration id = (InputDeclaration) declaration;
                validateExpression(id.value, true);
            }
        }
        return true;
    }

    /*
     * One might also consider checking if the same variable is used with more than one type:
     *   .local $a = {$foo :number}
     *   .local $b = {$foo :string}
     *   .local $c = {$foo :datetime}
     *
     * But this is not necesarily an error.
     * If $foo is a number, then it might be formatted as a number, or as date (epoch time),
     * or something else.
     *
     * So it is not safe to complain. Especially with custom functions:
     *   # get the first name from a `Person` object
     *   .local $b = {$person :getField fieldName=firstName}
     *   # get formats a `Person` object
     *   .local $b = {$person :person}
     */
    private void validateExpression(Expression expression, boolean fromInput)
            throws MFParseException {
        String argName = null;
        boolean wasLiteral = false;
        FunctionRef function = null;
        if (expression instanceof Literal) {
            // ...{foo}... or ...{|foo|}... or ...{123}...
            // does not declare anything
        } else if (expression instanceof LiteralExpression) {
            LiteralExpression le = (LiteralExpression) expression;
            argName = le.arg.value;
            function = le.function;
            wasLiteral = true;
        } else if (expression instanceof VariableExpression) {
            VariableExpression ve = (VariableExpression) expression;
            // ...{$foo :bar opt1=|str| opt2=$x opt3=$y}...
            // .input {$foo :number} => declares `foo`, if already declared is an error
            // .local $a={$foo} => declares `a`, but only used `foo`, does not declare it
            argName = ve.arg.name;
            function = ve.function;
        } else if (expression instanceof FunctionExpression) {
            // ...{$foo :bar opt1=|str| opt2=$x opt3=$y}...
            FunctionExpression fe = (FunctionExpression) expression;
            function = fe.function;
        }

        if (function instanceof FunctionRef) {
            FunctionRef fa = (FunctionRef) function;
            if (fa.options != null) {
                for (Option opt : fa.options.values()) {
                    LiteralOrVariableRef val = opt.value;
                    if (val instanceof VariableRef) {
                        // We had something like {:f option=$val}, it means we's seen `val`
                        // It is not a declaration, so not an error.
                        addVariableDeclaration(((VariableRef) val).name);
                    }
                }
            }
        }

        // We chech the argument name after options to prevent errors like this:
        // .local $foo = {$a :b option=$foo}
        if (argName != null) {
            // if we come from `.input {$foo :function}` then `varName` is null
            // and `argName` is `foo`
            if (fromInput) {
                addVariableDeclaration(argName);
            } else {
                // Remember that we've seen it, to complain if there is a declaration later
                if (!wasLiteral) {
                    // We don't consider {|bar| :func} to be a declaration of a "bar" variable
                    declaredVars.add(argName);
                }
            }
        }
    }

    private boolean addVariableDeclaration(String varName) throws MFParseException {
        if (declaredVars.contains(varName)) {
            error("Variable '" + varName + "' already declared");
            return false;
        }
        declaredVars.add(varName);
        return true;
    }

    private void error(String text) throws MFParseException {
        throw new MFParseException(text, -1);
    }
}
