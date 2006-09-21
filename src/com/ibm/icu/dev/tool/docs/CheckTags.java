/**
*******************************************************************************
* Copyright (C) 2002-2006, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/
/**
 * This is a tool to check the tags on ICU4J files.  In particular, we're looking for:
 *
 * - methods that have no tags
 * - custom tags: @draft, @stable, @internal?
 * - standard tags: @since, @deprecated
 *
 * Syntax of tags:
 * '@draft ICU X.X.X'
 * '@stable ICU X.X.X'
 * '@internal'
 * '@since  (don't use)'
 * '@obsolete ICU X.X.X'
 * '@deprecated to be removed in ICU X.X. [Use ...]'
 *
 * flags names of classes and their members that have no tags or incorrect syntax.
 *
 * Requires JDK 1.4 or later
 *
 * Use build.xml 'checktags' ant target, or
 * run from directory containing CheckTags.class as follows:
 * javadoc -classpath ${JAVA_HOME}/lib/tools.jar -doclet CheckTags -sourcepath ${ICU4J_src} [packagenames]
 */

package com.ibm.icu.dev.tool.docs;

import com.sun.javadoc.*;

public class CheckTags {
    RootDoc root;
    boolean log;
    boolean brief;
    boolean isShort;
    DocStack stack = new DocStack();

    class DocNode {
        private String header;
        private boolean printed;
        private boolean reportError;
        private int errorCount;

        public void reset(String header, boolean reportError) {
            this.header = header;
            this.printed = false;
            this.errorCount = 0;
            this.reportError = reportError;
        }
        public String toString() {
            return header +
                " printed: " + printed +
                " reportError: " + reportError +
                " errorCount: " + errorCount;
        }
    }

    class DocStack {
        private DocNode[] stack;
        private int index;
        private boolean newline;

        public void push(String header, boolean reportError) {
            if (stack == null) {
                stack = new DocNode[5];
            } else {
                if (index == stack.length) {
                    DocNode[] temp = new DocNode[stack.length * 2];
                    System.arraycopy(stack, 0, temp, 0, index);
                    stack = temp;
                }
            }
            if (stack[index] == null) {
                stack[index] = new DocNode();
            }
            //  System.out.println("reset [" + index + "] header: " + header + " report: " + reportError);
            stack[index++].reset(header, reportError);
        }

        public void pop() {
            if (index == 0) {
                throw new IndexOutOfBoundsException();
            }
            --index;

            int ec = stack[index].errorCount; // index already decremented
            if (ec > 0 || index == 0) { // always report for outermost element
                if (stack[index].reportError) {
                    output("(" + ec + (ec == 1 ? " error" : " errors") + ")", false, true, index);
                }

                // propagate to parent
                if (index > 0) {
                    stack[index-1].errorCount += ec;
                }
            }
            if (index == 0) {
                System.out.println(); // always since we always report number of errors
            }
        }

        public void output(String msg, boolean error, boolean newline) {
            output(msg, error, newline, index-1);
        }

        void output(String msg, boolean error, boolean newline, int ix) {
            DocNode last = stack[ix];
            if (error) {
                last.errorCount += 1;
	    }

            boolean show = !brief || last.reportError;
            // boolean nomsg = show && brief && error;
            //            System.out.println(">>> " + last + " error: " + error + " show: " + show + " nomsg: " + nomsg);

            if (show) {
                if (isShort || (brief && error)) {
                    msg = null; // nuke error messages if we're brief, just report headers and totals
                }
                for (int i = 0; i <= ix;) {
                    DocNode n = stack[i];
                    if (n.printed) {
                        if (msg != null || !last.printed) { // since index > 0 last is not null
                            if (this.newline && i == 0) {
                                System.out.println();
                                this.newline = false;
                            }
                            System.out.print("  ");
                        }
                        ++i;
                    } else {
                        System.out.print(n.header);
                        n.printed = true;
                        this.newline = true;
                        i = 0;
                    }
                }

                if (msg != null) {
                    if (index == 0 && this.newline) {
                        System.out.println();
                    }
                    if (error) {
                        System.out.print("*** ");
                    }
                    System.out.print(msg);
                }
            }

            this.newline = newline;
        }
    }

    public static boolean start(RootDoc root) {
        return new CheckTags(root).run();
    }

    public static int optionLength(String option) {
        if (option.equals("-log")) {
            return 1;
        } else if (option.equals("-brief")) {
            return 1;
        } else if (option.equals("-short")) {
            return 1;
        }
        return 0;
    }

    CheckTags(RootDoc root) {
        this.root = root;

        String[][] options = root.options();
        for (int i = 0; i < options.length; ++i) {
            String opt = options[i][0];
            if (opt.equals("-log")) {
                this.log = true;
            } else if (opt.equals("-brief")) {
                this.brief = true;
            } else if (opt.equals("-short")) {
                this.isShort = true;
            }
        }
    }

    boolean run() {
        doDocs(root.classes(), "Package", true);
        return false;
    }

    static final String[] tagKinds = {
        "@internal", "@draft", "@stable", "@since", "@deprecated", "@author", "@see", "@version",
        "@param", "@return", "@throws", "@obsolete", "@exception", "@serial", "@provisional"
    };

    static final int UNKNOWN = -1;
    static final int INTERNAL = 0;
    static final int DRAFT = 1;
    static final int STABLE = 2;
    static final int SINCE = 3;
    static final int DEPRECATED = 4;
    static final int AUTHOR = 5;
    static final int SEE = 6;
    static final int VERSION = 7;
    static final int PARAM = 8;
    static final int RETURN = 9;
    static final int THROWS = 10;
    static final int OBSOLETE = 11;
    static final int EXCEPTION = 12;
    static final int SERIAL = 13;
    static final int PROVISIONAL = 14;

    static int tagKindIndex(String kind) {
        for (int i = 0; i < tagKinds.length; ++i) {
            if (kind.equals(tagKinds[i])) {
                return i;
            }
        }
        return UNKNOWN;
    }

    boolean newline = false;

    void output(String msg, boolean error, boolean newline) {
        stack.output(msg, error, newline);
    }

    void log() {
        output(null, false, false);
    }

    void logln() {
        output(null, false, true);
    }

    void log(String msg) {
        output(msg, false, false);
    }

    void logln(String msg) {
        output(msg, false, true);
    }

    void err(String msg) {
        output(msg, true, false);
    }

    void errln(String msg) {
        output(msg, true, true);
    }

    void tagErr(Tag tag) {
        // Tag.position() requires JDK 1.4, build.xml tests for this
        errln(tag.toString() + " [" + tag.position() + "]");
    }

    void doDocs(ProgramElementDoc[] docs, String header, boolean reportError) {
        if (docs != null && docs.length > 0) {
            stack.push(header, reportError);
            for (int i = 0; i < docs.length; ++i) {
                doDoc(docs[i]);
            }
            stack.pop();
        }
    }

    void doDoc(ProgramElementDoc doc) {
        if (doc != null && (doc.isPublic() || doc.isProtected())
            && !(doc instanceof ConstructorDoc && ((ConstructorDoc)doc).isSynthetic())) {

            // unfortunately, in JDK 1.4.1 MemberDoc.isSynthetic is not properly implemented for
            // synthetic constructors.  So you'll have to live with spurious errors or 'implement'
            // the synthetic constructors...

            boolean isClass = doc.isClass() || doc.isInterface();
            String header;
            if (!isShort || isClass) {
                header = "--- ";
            } else {
                header = "";
            }
            header += (isClass ? doc.qualifiedName() : doc.name());
            if (doc instanceof ExecutableMemberDoc) {
                header += ((ExecutableMemberDoc)doc).flatSignature();
            }
            if (!isShort || isClass) {
                header += " ---";
            }
            stack.push(header, isClass);
            if (log) {
                logln();
            }
            boolean recurse = doTags(doc);
            if (recurse && isClass) {
                ClassDoc cdoc = (ClassDoc)doc;
                doDocs(cdoc.fields(), "Fields", !brief);
                doDocs(cdoc.constructors(), "Constructors", !brief);
                doDocs(cdoc.methods(), "Methods", !brief);
            }
            stack.pop();
        }
    }

    /** Return true if subelements of this doc should be checked */
    boolean doTags(ProgramElementDoc doc) {
        Tag[] tags = doc.tags();
        boolean foundRequiredTag = false;
        boolean foundDraftTag = false;
	boolean foundProvisionalTag = false;
        boolean foundDeprecatedTag = false;
        boolean foundObsoleteTag = false;
	boolean foundInternalTag = false;
	boolean foundStableTag = false;
	boolean retainAll = false;

        for (int i = 0; i < tags.length; ++i) {
            Tag tag = tags[i];

            String kind = tag.kind();
            int ix = tagKindIndex(kind);

            switch (ix) {
            case UNKNOWN:
                errln("unknown kind: " + kind);
                break;

            case INTERNAL:
                foundRequiredTag = true;
		foundInternalTag = true;
                break;

            case DRAFT:
                foundRequiredTag = true;
                foundDraftTag = true;
                if (tag.text().indexOf("ICU 2.8") != -1 &&
		    tag.text().indexOf("(retain") == -1) { // catch both retain and retainAll
                    tagErr(tag);
                    break;
                }
                if (tag.text().indexOf("ICU") != 0) {
                    tagErr(tag);
                    break;
                }
		retainAll |= (tag.text().indexOf("(retainAll)") != -1);
                break;

	    case PROVISIONAL:
		foundProvisionalTag = true;
		break;

            case DEPRECATED:
                foundDeprecatedTag = true;
                if (tag.text().indexOf("ICU") == 0) {
                    foundRequiredTag = true;
                }
                break;

            case OBSOLETE:
                if (tag.text().indexOf("ICU") != 0) {
                    tagErr(tag);
                }
                foundObsoleteTag = true;
                foundRequiredTag = true;
                break;

            case STABLE:
                {
                    String text = tag.text();
                    if (text.length() != 0 && text.indexOf("ICU") != 0) {
                        tagErr(tag);
                    }
                    foundRequiredTag = true;
		    foundStableTag = true;
                }
                break;

            case SINCE:
                tagErr(tag);
                break;

            case EXCEPTION:
                logln("You really ought to use @throws, you know... :-)");

            case AUTHOR:
            case SEE:
            case PARAM:
            case RETURN:
            case THROWS:
            case SERIAL:
                break;

            case VERSION:
                tagErr(tag);
                break;

            default:
                errln("unknown index: " + ix);
            }
        }
        if (!foundRequiredTag) {
            errln("missing required tag [" + doc.position() + "]");
        }
        if (foundInternalTag && !foundDeprecatedTag) {
            errln("internal tag missing deprecated");
        }
        if (foundDraftTag && !(foundDeprecatedTag || foundProvisionalTag)) {
            errln("draft tag missing deprecated or provisional");
        }
        if (foundObsoleteTag && !foundDeprecatedTag) {
            errln("obsolete tag missing deprecated");
        }
	if (foundStableTag && foundDeprecatedTag) {
	    logln("stable deprecated");
	}

	return !retainAll;
    }
}
