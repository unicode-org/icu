# README for configuration files used by org.unicode.icu.tool.cldrtoicu.regex.RegexTransformer.
#
# © 2019 and later: Unicode, Inc. and others.
#
# CLDR data files are interpreted according to the LDML specification (http://unicode.org/reports/tr35/)
# For terms of use, see http://www.unicode.org/copyright.html

======
Basics
======

The RegexTransformer class converts CLDR paths and values to ICU Resource Bundle paths
and values, based on a set of transformation rules typically loaded from a text file
(e.g. ldml2icu_locale.txt).

The basic format of transformation rules is:
  <path-specification> ; <resource-bundle-specification> [; <instruction>=<argument>]*

A simple example of a transformation rule is:

  //ldml/localeDisplayNames/keys/key[@type="(%A)"] ; /Keys/$1

which transforms CLDR values whose path matches the path specification, and emits:
* A resource bundle path "/Keys/xx", where 'xx' is the captured type attribute.
* A resource bundle value, which is just the CLDR value's base value.

A path specification can be thought of as a regular expression which matches the CLDR
path and can capture some element names or attribute values; however unlike a regular
expression, the '[',']' characters are treated as literals, similar to XPath expressions.

If a single CLDR value should produce more than one resource bundle path/value, then
it should be written:

  <path-specification>
     ; <resource-bundle-1-specification> [; <instruction> ]*
     ; <resource-bundle-2-specification> [; <instruction> ]*

=====================
Argument Substitution
=====================

Before a rule can be matched, any %-variables must be substituted. These are defined
in the same configuration file as the rules, and look something like:
  %W=[\w\-]++
or:
  %D=//ldml/numbers/defaultNumberingSystem

The first case can be thought of as just a snippet of regular expression (in this case
something that matches hyphen separated words) and, importantly, here '[' and ']' are
treated as regular expression metacharacters. These arguments are static and wil be
substituted exactly as-is into the regular expression to be used for matching.

The second case (used exactly once) is a dynamic argument which references a CLDR value
in the set of data being transformed. This is simply indicated by the fact that it starts
with '//'. This path is resolved and the value is substituted just prior to matching.

Variable names are limited to a single upper-case letter (A-Z).

===========================
Implicit Argument Splitting
===========================

This is a (somewhat non-obvious) mechanism which allows for a single rule to generate
multiple results from a single input path when a argument is a list of tokens.

Consider the rule:

//supplementalData/timeData/hours[@allowed="(%W)"][@preferred="(%W)"][@regions="(%W)"]
  ; /timeData/$3/allowed   ; values=$1
  ; /timeData/$3/preferred ; values=$2

where the "regions" attributes (which is captured as '$3') contains a whitespace separated
list of region codes (e.g. "US GB AU NZ"). In this case the rule is applied once for each
region, producing paths such as "/timeData/US/allowed" or "/timeData/NZ/preferred". Note
that there is no explicit instruction to do this, it just happens.

The rule is that the first unquoted argument in the resource bundle path is always treated
as splittable.

To suppress this behaviour, the argument must be quoted (e.g. /timeData/"$3"/allowed). Now,
if there were another following unquoted argument, that would become implicitly splittable
(but only one argument is ever splittable).

============
Instructions
============

Additional instructions can be supplied to control value transformation and specify fallback
values. The set of instructions is:
* values:     The most common instruction which defines how values are transformed.
* fallback:   Defines a fallback value to be used if this rule was not matched.

There are two other special case instructions which should (if at all possible) not be used,
and might be removed at some point:
* group:      Causes values to be grouped as sub-arrays for very specific use cases
              (prefer using "Hidden Labels" where possible).
* base_xpath: Allows deduplication of results between multiple different rules (this is a
              hack to work around limitations in how matching is performed).

-------------------
values=<expression>
-------------------

The "values" instruction defines an expression whose evaluated result becomes the output
resource bundle value(s). Unless quoting is present, this evaluated expression is split
on whitespace and can become multiple values in the resulting resource bundle.

Examples:

* values=$1 $2 $3

  Produces three separate values in the resource bundle for the first three captured
  arguments.

* values="$1 $2" $3

  Produces two values in the resource bundle, the first of which is two captured values
  separated by a space character.

* values={value}

  Substitutes the CLDR value, but then performs whitespace splitting on the result. This
  differs from the behaviour when no "values" instructions is present (which does not
  split the results).

* values="{value}" $1

  Produces two values, the first of which is the unsplit CLDR value, and the second is a
  captured argument.

* values=&func($1, {value})

  Invokes a transformation function, passing in a captured argument and the CLDR value,
  and the result is then split. The set of functions available to a transformer is
  configured when it is created.

Note that in the above examples, it is assumed that the $N arguments do not contain spaces.
If they did, it would result in more output values. To be strict about things, every value
which should not be split must be quoted (e.g. values="$1" "$2" "$3") but since captured
values are often IDs or other tokens, this is not what is seen in practice, so it is not
reflected in these examples.

---------------------
fallback=<expression>
---------------------

The fallback instruction provides a way for default values to be emitted for a path that
was not matched. Fallbacks are useful when several different rules produce values for the
same resource bundle. In this case the output path produced by one rule can be used as
the "key" for any unmatched rules with fallback values (to "fill in the gaps").

Consider the two rules which can emit the same resource bundle path:

//ldml/numbers/currencies/currency[@type="(%W)"]/symbol
     ; /Currencies/$1 ; fallback=$1
//ldml/numbers/currencies/currency[@type="(%W)"]/displayName
     ; /Currencies/$1 ; fallback=$1

These rules, if both matched, will produce two values for the same resource bundle path.
Consider the CLDR values:

//ldml/numbers/currencies/currency[@type="USD"]/symbol      ==> "$"
//ldml/numbers/currencies/currency[@type="USD"]/displayName ==> "US Dollar"

After matching both of these paths, the values for the resource bundle "/Currencies/USD"
will be the array { "$", "US Dollar" }.

However, if only one value were present to be converted, the converter could use the
matched path "/Currencies/XXX" and infer the missing fallback value, ensuring that the
output array (it if was emitted at all) was always two values.

Note that in order for this to work, the fallback value must be derivable only from the
matched path. E.g. it cannot contain arguments that are not also present in the matched
path, and obviously cannot reference the "{value}" at all. Thus the following would not
be permitted:

//ldml/foo/bar[@type="(%W)"][@region=(%A)] ; /Foo/$1 ; fallback=$2

However the fallback value can reference existing CLDR or resource bundle paths (expected
to be present from other rules). For example:
  fallback=/weekData/001:intvector[0]
or:
  fallback=//ldml/numbers/symbols[@numberSystem="%D"]/decimal

The latter case is especially complex because it also uses the "dynamic" argument:
  %D=//ldml/numbers/defaultNumberingSystem

So determining the resulting value will require:
1) resolving "//ldml/numbers/defaultNumberingSystem" to, for example, "arab"
2) looking up the value of "//ldml/numbers/symbols[@numberSystem="arab"]/decimal"

-----------------
base_xpath=<path>
-----------------

The base_xpath instruction allows a rule to specify a proxy path which is used in place of
the originally matched path in the returned result. This is a useful hack for cases where
values are derived from information in a path prefix.

Because path matching for transformation happens only on full paths, it is possible that
several distinct CLDR paths might effectively generate the same result if they share the
same prefix (i.e. paths in the same "sub hierarchy" of the CLDR data).

If this happens, then you end up generating "the same" result from different paths. To
fix this, a "surrogate" CLDR path can be specified as a proxy for the source path,
allowing several results to appears to have come from the same source, which results in
deduplication of the final value.

For example, the two rules :

//supplementalData/territoryInfo/territory[...][@writingPercent="(%N)"][@populationPercent="(%N)"][@officialStatus="(%W)"](?:[@references="%W"])?
    ; /territoryInfo/$1/territoryF:intvector ; values=&exp($2) &exp($3,-2) &exp($4) ; base_xpath=//supplementalData/territoryInfo/territory[@type="$1"]

//supplementalData/territoryInfo/territory[...][@writingPercent="(%N)"][@populationPercent="(%N)"](?:[@references="%W"])?
    ; /territoryInfo/$1/territoryF:intvector ; values=&exp($2) &exp($3,-2) &exp($4) ; base_xpath=//supplementalData/territoryInfo/territory[@type="$1"]

Produce the same results for different paths (with or without the "officialStatus"
attribute) but only one such result is desired. By specifying the same base_xpath on
both rules, the conversion logic can deduplicate these to produce only one result.

When using base_xpath, it is worth noting that:
1) Base xpaths must be valid "distinguishing" paths (but are never matched to any rule).
2) Base xpaths can use arguments to achieve the necessary level of uniqueness.
3) Rules which share the same base xpath must always produce the same values.

Note however that this is a still very much a hack because since two rules are responsible
for generating the same result, there is no well defined "line number" to use for ordering
of values. Thus this mechanism should only be used for rules which produce "single"
values, and must not be used in cases where the ordering of values in arrays is important.

This mechanism only exists because there is currently no mechanism for partial matching
or a way to match one path against multiple rules.

-----
group
-----

The "group" instruction should be considered a "last resort" hack for controlling value
grouping, in cases where "hidden labels" are not suitable (see below).

==============================
Value Arrays and Hidden Labels
==============================

In the simplest case, one rule produces one or more output path/values per matched CLDR
value (i.e. one-to-one or one-to-many). If that happens, then output ordering of the
resource bundle paths is just the natural resource bundle path ordering.

However it is also possible for several rules to produce values for a single output path
(i.e. many-to-one). When this happens there are some important details about how results
are grouped and ordered.

------------
Value Arrays
------------

If several rules produce results for the same resource bundle path, the values produced
by the rules are always ordered according to the order of the rule in the configuration
rule (and it is best practice to group any such rules together for clarity).

If each rule produces multiple values, then depending on grouping, those values can either
be concatenated together in a single array or grouped individually to create an array
of arrays.

In the example below, there are four rules producing values for the same path (

//.../firstDay[@day="(%W)"][@territories="(%W)"]     ; /weekData/$2:intvector ; values=&day_number($1)
//.../minDays[@count="(%N)"][@territories="(%W)"]    ; /weekData/$2:intvector ; values=$1
//.../weekendStart[@day="(%W)"][@territories="(%W)"] ; /weekData/$2:intvector ; values=&day_number($1) 0
//.../weekendEnd[@day="(%W)"][@territories="(%W)"]   ; /weekData/$2:intvector ; values=&day_number($1) 86400000

The first two rules produce one value each, and the last two produce two values each. This
results in the resource bundle "/weekData/xxx:intvector" having a single array consisting
of six values. In the real configuration, these rules also use fallback instructions to
ensure that the resulting array of values is always six values, even if some CLDR paths are
not present.

-------------
Hidden Labels
-------------

Sometimes rules should produce separate "sub-arrays" of values, rather than having all the
values appended to a single array. Consider the following path/value pairs:

x/y: a
x/y: b
x/y: c

Which produce the resource bundle "x/y" with three values:

x{
  y{
    "a",
    "b",
    "c"
  }
}

Now suppose we want to make a resource bundle where the values are grouped into their
own sub-array:

x{
  y{
    { "a", "b", "c" }
  }
}

We can think of this as coming from the path/value pairs:

x/y/-: a
x/y/-: b
x/y/-: c

where to represent the sub-array we introduce the idea of an empty path element '-'.

In a transformation rule, these "empty elements" are represent as "hidden labels", and look
like "<some-label>". They are treated as "normal" path elements for purposes of ordering and
grouping, but are treated as empty when the paths are written to the ICU data files.

For example the rule:

//.../currencyCodes[@type="(%W)"][@numeric="(%N)"].* ; /codeMappingsCurrency/<$1> ; values=$1 $2

Generates a series of grouped, 2-element sub-arrays split by the captured type attribute.

  codeMappingCurrency{
    { type-1, numeric-1 }
    { type-2, numeric-2 }
    { type-3, numeric-3 }
  }

<FIFO> is a special hidden label which is substituted for in incrementing counting when
sorting paths. It ensures that values in the same array are sorted in the order that they
were encountered. However this mechanism imposes a strict requirement that the ordering
of CLDR values to be transformed matches the expected ICU value order, so it should be
avoided where possible to avoid this implicit, subtle dependency. Note that this mechanism
is currently only enabled for the transformation of "supplemental data" and may eventually
be removed.

Hidden labels are a neat solution which permits the generation of sub-array values, but they
don't quite work in every case. For example if you need to produce a resource bundle with a
mix of values and sub-arrays, like:

x{
  y{
    "a",
    { "b", "c" }
    "d"
  }
}

which can be thought of as coming from the path/value pairs:

x/y: a
x/y/<z>: b
x/y/<z>: c
x/y: d

we find that, after sorting the resource bundle paths, we end up with:

x/y: a
x/y: d
x/y/<z>: b
x/y/<z>: c

which produces the wrong result. This happens because values with different paths are
sorted primarily by their path. I cases like this, where a mix of values and sub-arrays
are required, the "group" instruction can be used instead.

For example:

//ldml/numbers/currencies/currency[@type="(%W)"]/symbol      ; /Currencies/$1
//ldml/numbers/currencies/currency[@type="(%W)"]/displayName ; /Currencies/$1
//ldml/numbers/currencies/currency[@type="(%W)"]/pattern     ; /Currencies/$1 ; group
//ldml/numbers/currencies/currency[@type="(%W)"]/decimal     ; /Currencies/$1 ; group
//ldml/numbers/currencies/currency[@type="(%W)"]/group       ; /Currencies/$1 ; group

Produces resource bundles which look like:

Currencies{
  xxx{
     "<symbol>",
     "<display name>",
     { "<pattern>", "<decimal>", "<group>" }
  }
}
