ICU-9562 Update language tag/subtag mapping tables from the IANA registry

uloc_forLanguageTag has a few mapping tables to map grandfathered
language tags and deprecated language subtags to their preferred or
moern values. Update them based on the latest version of the IANA
language tag registry.

Five grandfathered tags without a preferred value are still mapped to
what ICU has mapped them to for the backward compatibility until the
wisdom of continuing to do so is reviewed.

In addition, map redundant language tags to their preferred values
whether they're followed by other subtags or not. e.g.
zh-yue vs zh-yue-u-co-pinyin.

Similary, ja-latn-hepburn-heploc is mapped to ja-latn-alaic97 (the
variant subtag 'hepburn-helploc' with the prefix 'ja-latn' has the
preferred value, 'alaic97'.

Add a new table for deprecated region subtags to map them to their
modern values. (e.g. 'DD' to 'DE').
