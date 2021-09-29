// © 2021 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * “Known issues” manager.
 * Intended to be shared between ICU, CLDR, &c.
 * Test frameworks can create an instance of this to manage known issues
 */
public class UnicodeKnownIssues {
    /**
     * From Java 1.8
     */
    public interface Consumer<T> { void accept(T t); }

    private Map<String, List<String>> knownIssues = new TreeMap<>();
    /**
     * Max number of lines to show by default (including the "more")
     * unless -allKnownIssues is given. Must be at least 2.
     */
    public static final int KNOWN_ISSUES_CURTAILMENT = 2;

    /**
     * true if all issues should be shown, false if they should
     * be curtailed.
     */
    private boolean allKnownIssues;

    /**
     * Construct a known issue manager
     * @param allKnownIssues true if all known issues should be printed,
     * not curtailed
     */
    public UnicodeKnownIssues(boolean allKnownIssues) {
        this.allKnownIssues = allKnownIssues;
    }

    /**
     * Base URL for browsing Unicode JIRA
     */
    public static final String UNICODE_JIRA_BROWSE = "https://unicode-org.atlassian.net/browse/";

    static final Pattern ICU_TICKET_PATTERN = Pattern.compile(
        "(?i)(?:icu-)?(\\d+)"
    );
    static final Pattern CLDR_TICKET_PATTERN = Pattern.compile(
        "(?i)cldr(?:bug:)?(?:-)?(\\d+)"
    );

    /**
     * Match all linkable ticket patterns
     * @see {org.unicode.cldr.util.CLDRURLS#CLDR_TICKET_BROWSE}
     */
    static final Pattern UNICODE_JIRA_PATTERN = Pattern.compile(
        "(CLDR|ICU)-(\\d+)"
    );

    /**
     * Log the known issue.
     * Call this from the test framework when logKnownIssue() is called.
     *
     * @param path Path to the error, will be returned in the
     * known issue list
     * @param ticket A ticket number string. For an ICU ticket, use "ICU-10245".
     * For a CLDR ticket, use "CLDR-12345".
     * For compatibility, "1234" -> ICU-1234 and "cldrbug:456" -> CLDR-456
     * @param comment Additional comment, or null
     *
     */
    public void logKnownIssue(String path, String ticket, String comment) {

        StringBuilder descBuf = new StringBuilder(path);

        if (comment != null && comment.length() > 0) {
            descBuf.append(" (" + comment + ")");
        }
        String description = descBuf.toString();

        String ticketLink = "Unknown Ticket";
        if (ticket != null && ticket.length() > 0) {
            Matcher matcher = ICU_TICKET_PATTERN.matcher(ticket);
            if (matcher.matches()) {
                ticketLink = "ICU-" + matcher.group(1);
            } else {
                matcher = CLDR_TICKET_PATTERN.matcher(ticket);
                if (matcher.matches()) {
                    ticketLink = "CLDR-" + matcher.group(1);
                }
            }
        }

        List<String> lines = knownIssues.get(ticketLink);
        if (lines == null) {
            lines = new ArrayList<>();
            knownIssues.put(ticketLink, lines);
        }
        if (!lines.contains(description)) {
            lines.add(description);
        }
    }

    /**
     * Print out all known issues to the logFn.
     * Usage:  printKnownIssues(System.out::println)
     * @param logFn consumer for Strings (e.g. System.out::println)
     * @return true if (!allKnownIssues) and we had to curtail
     */
    public boolean printKnownIssues(Consumer<String> logFn) {
        boolean didCurtail = false;
        if (knownIssues.isEmpty()) {
            return false;
        }
        logFn.accept("\n " + knownIssues.size() + " Known Issues:");
        for (Entry<String, List<String>> entry : knownIssues.entrySet()) {
            String ticketLink = entry.getKey();
            if (UNICODE_JIRA_PATTERN.matcher(ticketLink) != null) {
                logFn.accept(ticketLink + " <" + UNICODE_JIRA_BROWSE + ticketLink + ">");
            } else {
                // Unknown or something else
                logFn.accept("<" + ticketLink + ">");
            }
            List<String> entries = entry.getValue();
            int issuesToShow = entries.size();
            if (!allKnownIssues && issuesToShow > KNOWN_ISSUES_CURTAILMENT) {
                issuesToShow = (KNOWN_ISSUES_CURTAILMENT - 1);
            }
            for (int i=0; i<issuesToShow; i++) {
                logFn.accept("  - " + entries.get(i));
            }
            if (entries.size() > issuesToShow) {
                didCurtail = true;
                logFn.accept("  ... and " +
                    (entries.size() - issuesToShow) + " more");
            }
        }
        return didCurtail;
    }

    /**
     * Reset the known issues
     */
    public void reset() {
        knownIssues.clear();
    }
}
