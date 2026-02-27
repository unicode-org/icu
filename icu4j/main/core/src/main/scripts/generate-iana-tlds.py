#!/usr/bin/env python3
# © 2025 and later: Unicode, Inc. and others.
# License & terms of use: https://www.unicode.org/copyright.html
#
# Generates IanaTlds.java from the IANA root zone TLD list.
# Downloads https://data.iana.org/TLD/tlds-alpha-by-domain.txt when
# the output file is absent or older than 7 days.

import os
import sys
import time
import urllib.request
import datetime

IANA_URL   = "https://data.iana.org/TLD/tlds-alpha-by-domain.txt"
OUTPUT     = os.path.join(os.path.dirname(__file__),
                          "../java/com/ibm/icu/text/IanaTlds.java")
MAX_AGE_S  = 7 * 24 * 3600   # one week

def needs_refresh(path):
    if not os.path.exists(path):
        return True
    age = time.time() - os.path.getmtime(path)
    return age > MAX_AGE_S

def ace_to_unicode(label):
    """Convert an ACE label (xn--...) to its Unicode form via raw Punycode decoding."""
    if not label.lower().startswith("xn--"):
        return label
    try:
        import codecs
        return codecs.decode(label[4:].encode("ascii"), "punycode")
    except Exception:
        return label  # keep Punycode if decode fails

def fetch_tlds():
    with urllib.request.urlopen(IANA_URL, timeout=4) as resp:
        text = resp.read().decode("utf-8")
    lines = [l.strip() for l in text.splitlines()]
    version_line = next((l for l in lines if l.startswith("#")), "# unknown")
    tlds = [ace_to_unicode(l.lower()) for l in lines if l and not l.startswith("#")]
    return version_line, tlds

def write_java(path, version_line, tlds):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    today = datetime.date.today().isoformat()
    with open(path, "w", encoding="utf-8") as f:
        f.write(f"""\
// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html
//
// THIS FILE IS GENERATED. DO NOT EDIT BY HAND.
// Run generate-iana-tlds.py to regenerate.
// Source: {IANA_URL}
// {version_line}
// Generated: {today}
package com.ibm.icu.text;

import java.util.Arrays;
import java.util.HashSet;

/**
 * IANA root-zone top-level domains, for use in link detection.
 * The list is stored in lowercase Unicode form, matching the output of
 * UTS #46 nameToUnicode on any TLD label.
 *
 * <p>Regenerate with {{@code generate-iana-tlds.py}} (auto-run by Maven
 * when the file is older than one week).
 */
class IanaTlds {{
    private static final HashSet<String> TLDS = new HashSet<>(Arrays.asList(
""")
        # write in rows of 8
        for i in range(0, len(tlds), 8):
            row = tlds[i:i+8]
            joined = ", ".join(f'"{t}"' for t in row)
            comma = "," if i + 8 < len(tlds) else ""
            f.write(f"        {joined}{comma}\n")
        f.write("""\
    ));

    /** Returns true if {@code label} (case-insensitive) is a known IANA TLD. */
    static boolean isTld(String label) {
        return TLDS.contains(label.toLowerCase(java.util.Locale.ROOT));
    }
}
""")

def main():
    out = os.path.normpath(os.path.join(os.path.dirname(__file__), OUTPUT))
    if not needs_refresh(out):
        print(f"generate-iana-tlds.py: {out} is fresh, skipping download.")
        sys.exit(0)
    print(f"generate-iana-tlds.py: refreshing {out} from {IANA_URL} ...")
    try:
        version_line, tlds = fetch_tlds()
    except Exception:
        sys.exit(0)
    write_java(out, version_line, tlds)
    print(f"generate-iana-tlds.py: wrote {len(tlds)} TLDs.")

if __name__ == "__main__":
    main()
