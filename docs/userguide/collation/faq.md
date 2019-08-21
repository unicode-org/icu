# Collation FAQ

## Q. Should I turn Full Normalization on all the time?

**A.** You can if you want, but you don't typically need to. The key is that
normalization for most characters is already built into ICU's collation by
default. Everything that can be done without affecting performance is already
there, and will work with most languages. So the normalization parameter in ICU
really only changes whether full normalization is invoked.

The outlying cases are situations where a language uses multiple accents
(non-spacing marks) on the same base letter, such as Vietnamese or Arabic. In
those cases, full normalization needs to be turned on. If you use the right
locale (or language) when creating a collation in ICU, then full normalization
will be turned on or off according to what the language typically requires.

## Q. Are there any cases where I would want to override the Full Normalization setting?

**A.** The only case where you really need to worry about that parameter is for
very unusual cases, such as sorting an list containing of names according to
English conventions, but where the list contains, for example, some Vietnamese
names. One way to check for such a situation is to open a collator for each of
the languages you expect to find, and see if any of them have the full
normalization flags set.

## Q. How can collation rules mimic word sorting?

Word sort is a way of sorting where certain interpunction characters are
completely ignored, while other are considered. An example of word sort below
ignores hyphens and apostrophes:

Word SortString Sortbilletbill'sbills
billetbill'sbillscannotcan'tcantcannotcan'tcantconco-opcoopconco-opcoop

This specific behavior can be mimicked using a tailoring that makes these
characters completely ignorable. In this case, appropriate rule would be
"&\\u0000 = '' = '-'".

Please note that we don't think that such solution is correct, since different
languages have different word elements. Instead one should use shifted mode for
comparison.
