/**
 * This is a tool to check the tags on ICU4J files.  In particular, we're looking for:
 *
 * - methods that have no tags
 * - custom tags: @draft, @stable, @internal?
 * - standard tags: @since, @deprecated
 *
 * Syntax of tags:
 * @draft ICU X.X.X
 * @stable ICU X.X.X
 * @internal
 * @since  (don't use)
 * @deprecated to be removed in ICU X.X. [Use ...]
 *
 * flags names of classes and their members that have no tags or incorrect syntax.
 *
 * Requires JDK 1.4 or later
 * 
 * Run from directory containing CheckTags.class as follows:
 * javadoc -classpath ${JAVA_HOME}/lib/tools.jar -doclet CheckTags -sourcepath ${ICU4J_src} [packagenames]
 */

package com.ibm.icu.dev.tool.docs;

import com.sun.javadoc.*;

public class CheckTags {
    RootDoc root;
    boolean log;
    boolean brief = true;
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
            stack[index++].reset(header, reportError);
        }

        public void pop() {
            if (index == 0) {
                throw new IndexOutOfBoundsException();
            }
            --index;
            handleErrors();
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
            if (show) {
                if (brief && error) {
                    msg = null; // nuke error messages if we're brief, just report headers and totals
                }
                for (int i = 0; i < index;) {
                    DocNode n = stack[i];
                    if (n.printed) {
                        if (msg != null || !last.printed) { // since index > 0 last is not null
                            if (this.newline && i == 0) {
                                System.out.println();
                            }
                            System.out.print("  ");
                            this.newline = false;
                        }
                        ++i;
                    } else {
                        System.out.print(n.header);
                        this.newline = true;
                        n.printed = true;
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

        void handleErrors() {
            // index is already decremented
            int ec = stack[index].errorCount;
            if (ec > 0 || index == 0) { // always report for outermost element
                if (stack[index].reportError) {
                    output("(" + ec + (ec == 1 ? " error" : " errors") + ")", false, true, index);
                }

                // propagate to parent
                if (index > 0) {
                    stack[index-1].errorCount += ec;
                }
            }
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
            }
        }
    }

    boolean run() {
        doDocs(root.classes(), "Package", true);
        return false;
    }

    static final String[] tagKinds = {
        "@internal", "@draft", "@stable", "@since", "@deprecated", "@author", "@see", "@version",
        "@param", "@return", "@throws"
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
        if (doc != null && (doc.isPublic() || doc.isProtected())) {
            boolean isClass = doc.isClass();
            String header = "--- " + (isClass ? doc.qualifiedName() : doc.name());
            if (doc instanceof ExecutableMemberDoc) {
                header += ((ExecutableMemberDoc)doc).flatSignature();
            }
            header += " ---";
            stack.push(header, isClass);
            if (log) {
                logln();
            }
            doTags(doc.tags());
            if (isClass) {
                ClassDoc cdoc = (ClassDoc)doc;
                doDocs(cdoc.innerClasses(), "Inner Classes", true);
                doDocs(cdoc.fields(), "Fields", !brief);
                doDocs(cdoc.constructors(), "Constructors", !brief);
                doDocs(cdoc.methods(), "Methods", !brief);
            }
            stack.pop();
        }
    }

    void doTags(Tag[] tags) {
        boolean foundRequiredTag = false;
        for (int i = 0; i < tags.length; ++i) {
            Tag tag = tags[i];
                
            String kind = tag.kind();
            int ix = tagKindIndex(kind);

            switch (ix) {
            case UNKNOWN:
                errln("unknown kind: " + kind);
                break;
                   
            case INTERNAL:
                tagErr(tag);
                break;

            case DRAFT:
                if (tag.text().indexOf("ICU") != 0) {
                    tagErr(tag);
                }
                foundRequiredTag = true;
                break;

            case STABLE:
                if (tag.text().length() != 0) {
                    tagErr(tag);
                }
                foundRequiredTag = true;
                break;

            case SINCE:
                tagErr(tag);
                break;

            case DEPRECATED:
                if (tag.text().indexOf("be removed") == -1) {
                    tagErr(tag);
                }
                foundRequiredTag = true;
                break;

            case AUTHOR:
            case SEE:
            case PARAM:
            case RETURN:
            case THROWS:
                break;

            case VERSION:
                tagErr(tag);
                break;

            default:
                errln("unknown index: " + ix);
            }
        }
        if (!foundRequiredTag) {
            errln("missing required tag");
        }
    }
}
