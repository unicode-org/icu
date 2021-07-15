// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.tool.docs;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Checks if API status of equals/hashCode is same with its containing class.
 *
 * @author Yoshito
 */
public class APIStatusConsistencyChecker {
    public static void main(String[] args) {
        // args[0]  API signature file path
        // args[1]  (Optional) List of classes to be skipped, separated by semicolon
        if (args.length < 1) {
            System.err.println("Missing API signature file path.");
        } else if (args.length > 2) {
            System.err.println("Too many command arguments");
        }

        List<String> skipClasses = Collections.emptyList();
        if (args.length == 2) {
            String[] classes = args[1].split(";");
            skipClasses = Arrays.asList(classes);
        }

        // Load the ICU4J API signature file
        Set<APIInfo> apiInfoSet = APIData.read(new File(args[0]), true).getAPIInfoSet();
        APIStatusConsistencyChecker checker = new APIStatusConsistencyChecker(apiInfoSet, skipClasses, new PrintWriter(System.err, true));
        checker.checkConsistency();
        System.exit(checker.errCount);
   }

    private int errCount = 0;
    private Set<APIInfo> apiInfoSet;
    private PrintWriter pw;
    private List<String> skipClasses;

    public APIStatusConsistencyChecker(Set<APIInfo> apiInfoSet, List<String> skipClasses, PrintWriter pw) {
        this.apiInfoSet = apiInfoSet;
        this.skipClasses = skipClasses;
        this.pw = pw;
    }

    public int errorCount() {
        return errCount;
    }

    // Methods that should have same API status with a containing class
    static final String[][] METHODS = {
          //{"<method name>",   "<method signature in APIInfo data>"},
            {"equals",      "boolean(java.lang.Object)"},
            {"hashCode",    "int()"},
            {"toString",    "java.lang.String()"},
            {"clone",       "java.lang.Object()"},
    };

    public void checkConsistency() {
        Map<String, APIInfo> classMap = new TreeMap<>();
        // Build a map of APIInfo for classes, indexed by class name
        for (APIInfo api : apiInfoSet) {
            if (!api.isPublic() && !api.isProtected()) {
                continue;
            }
            if (!api.isClass() && !api.isEnum()) {
                continue;
            }
            String fullClassName = api.getPackageName() + "." + api.getName();
            classMap.put(fullClassName, api);
        }

        // Walk through methods
        for (APIInfo api : apiInfoSet) {
            if (!api.isMethod()) {
                continue;
            }

            String fullClassName = api.getPackageName() + "." + api.getClassName();
            if (skipClasses.contains(fullClassName)) {
                continue;
            }

            boolean checkWithClass = false;
            String methodName = api.getName();
            String methodSig = api.getSignature();

            for (String[] method : METHODS) {
                if (method[0].equals(methodName) && method[1].equals(methodSig)) {
                    checkWithClass = true;
                }
            }

            if (!checkWithClass) {
                continue;
            }

            // Check if this method has same API status with the containing class
            APIInfo clsApi = classMap.get(fullClassName);
            if (clsApi == null) {
                pw.println("## Error ## Class " + fullClassName + " is not found.");
                errCount++;
            }

            int methodStatus = api.getVal(APIInfo.STA);
            String methodVer = api.getStatusVersion();
            int classStatus = clsApi.getVal(APIInfo.STA);
            String classVer = clsApi.getStatusVersion();

            if (methodStatus != classStatus || !Objects.equals(methodVer, classVer)) {
                pw.println("## Error ## " + methodName + " in " + fullClassName);
                errCount++;
            }
        }
    }
}
