// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2014, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.docs;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class DeprecatedAPIChecker {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Illegal command argument. Specify the API signature file path.");
        }
        // Load the ICU4J API signature file
        Set<APIInfo> apiInfoSet = APIData.read(new File(args[0]), true).getAPIInfoSet();

        DeprecatedAPIChecker checker = new DeprecatedAPIChecker(apiInfoSet, new PrintWriter(System.err, true));
        checker.checkDeprecated();
        System.exit(checker.errCount);
    }

    private int errCount = 0;
    private Set<APIInfo> apiInfoSet;
    private PrintWriter pw;

    public DeprecatedAPIChecker(Set<APIInfo> apiInfoSet, PrintWriter pw) {
        this.apiInfoSet = apiInfoSet;
        this.pw = pw;
    }

    public int errorCount() {
        return errCount;
    }

    public void checkDeprecated() {
        // Gather API class/enum names and its names that can be
        // used for Class.forName()
        Map<String, String> apiClassNameMap = new TreeMap<>();
        for (APIInfo api : apiInfoSet) {
            if (!api.isPublic() && !api.isProtected()) {
                continue;
            }
            if (!api.isClass() && !api.isEnum()) {
                continue;
            }
            String packageName = api.getPackageName();
            String className = api.getName();

            // Replacing separator for nested class/enum (replacing '.' with
            // '$'), so we can use the name for Class.forName(String)
            String classNamePath = className.contains(".") ? className.replace('.', '$') : className;

            apiClassNameMap.put(packageName + "." + classNamePath, packageName + "." + className);
        }

        // Walk through API classes using reflection
        for (Entry<String, String> classEntry : apiClassNameMap.entrySet()) {
            String classNamePath = classEntry.getKey();
            try {
                Class<?> cls = Class.forName(classNamePath);
                if (cls.isEnum()) {
                    checkEnum(cls, apiClassNameMap);
                } else {
                    checkClass(cls, apiClassNameMap);
                }
            } catch (ClassNotFoundException e) {
                pw.println("## Error ## Class " + classNamePath + " is not found.");
                errCount++;
            }
        }
    }

    private void checkClass(Class<?> cls, Map<String, String> clsNameMap) {
        assert !cls.isEnum();

        String clsPath = cls.getName();
        String clsName = clsNameMap.get(clsPath);
        APIInfo api = null;

        if (clsName != null) {
            api = findClassInfo(apiInfoSet, clsName);
        }
        if (api == null) {
            pw.println("## Error ## Class " + clsName + " is not found in the API signature data.");
            errCount++;
        }

        // check class
        compareDeprecated(isAPIDeprecated(api), cls.isAnnotationPresent(Deprecated.class), clsName, null, "Class");

        // check fields
        for (Field f : cls.getDeclaredFields()) {
            if (!isPublicOrProtected(f.getModifiers())) {
                continue;
            }

            String fName = f.getName();
            api = findFieldInfo(apiInfoSet, clsName, fName);
            if (api == null) {
                pw.println("## Error ## Field " + clsName + "." + fName + " is not found in the API signature data.");
                errCount++;
                continue;
            }

            compareDeprecated(isAPIDeprecated(api), f.isAnnotationPresent(Deprecated.class), clsName, fName, "Field");
        }

        // check constructors
        for (Constructor<?> ctor : cls.getDeclaredConstructors()) {
            if (!isPublicOrProtected(ctor.getModifiers())) {
                continue;
            }

            List<String> paramNames = getParamNames(ctor);

            Class<?> declClass = cls.getDeclaringClass();
            if (declClass != null && !Modifier.isStatic(cls.getModifiers())) {
                // This is non-static inner class's constructor.
                // javac automatically injects instance of declaring class
                // as the first param of the constructor, but ICU's API
                // signature is based on javadoc and it generates signature
                // without the implicit parameter.
                assert paramNames.get(0).equals(declClass.getName());
                paramNames.remove(0);
            }

            api = findConstructorInfo(apiInfoSet, clsName, paramNames);

            if (api == null) {
                pw.println("## Error ## Constructor " + clsName + formatParams(paramNames)
                        + " is not found in the API signature data.");
                errCount++;
                continue;
            }

            compareDeprecated(isAPIDeprecated(api), ctor.isAnnotationPresent(Deprecated.class), clsName,
                    api.getClassName() + formatParams(paramNames), "Constructor");
        }

        // check methods
        for (Method mtd : cls.getDeclaredMethods()) {
            // Note: We exclude synthetic method.
            if (!isPublicOrProtected(mtd.getModifiers()) || mtd.isSynthetic()) {
                continue;
            }

            String mtdName = mtd.getName();
            List<String> paramNames = getParamNames(mtd);
            api = findMethodInfo(apiInfoSet, clsName, mtdName, paramNames);

            if (api == null) {
                pw.println("## Error ## Method " + clsName + "#" + mtdName + formatParams(paramNames)
                        + " is not found in the API signature data.");
                errCount++;
                continue;
            }

            compareDeprecated(isAPIDeprecated(api), mtd.isAnnotationPresent(Deprecated.class), clsName, mtdName
                    + formatParams(paramNames), "Method");

        }
    }

    private void checkEnum(Class<?> cls, Map<String, String> clsNameMap) {
        assert cls.isEnum();

        String enumPath = cls.getName();
        String enumName = clsNameMap.get(enumPath);
        APIInfo api = null;

        if (enumName != null) {
            api = findEnumInfo(apiInfoSet, enumName);
        }
        if (api == null) {
            pw.println("## Error ## Enum " + enumName + " is not found in the API signature data.");
            errCount++;
        }

        // check enum
        compareDeprecated(isAPIDeprecated(api), cls.isAnnotationPresent(Deprecated.class), enumName, null, "Enum");

        // check enum constants
        for (Field ec : cls.getDeclaredFields()) {
            if (!ec.isEnumConstant()) {
                continue;
            }
            String ecName = ec.getName();
            api = findEnumConstantInfo(apiInfoSet, enumName, ecName);
            if (api == null) {
                pw.println("## Error ## Enum constant " + enumName + "." + ecName
                        + " is not found in the API signature data.");
                errCount++;
                continue;
            }

            compareDeprecated(isAPIDeprecated(api), ec.isAnnotationPresent(Deprecated.class), enumName, ecName,
                    "Enum Constant");
        }

        // check methods
        for (Method mtd : cls.getDeclaredMethods()) {
            // Note: We exclude built-in methods in a Java Enum instance
            if (!isPublicOrProtected(mtd.getModifiers()) || isBuiltinEnumMethod(mtd)) {
                continue;
            }

            String mtdName = mtd.getName();
            List<String> paramNames = getParamNames(mtd);
            api = findMethodInfo(apiInfoSet, enumName, mtdName, paramNames);

            if (api == null) {
                pw.println("## Error ## Method " + enumName + "#" + mtdName + formatParams(paramNames)
                        + " is not found in the API signature data.");
                errCount++;
                continue;
            }

            compareDeprecated(isAPIDeprecated(api), mtd.isAnnotationPresent(Deprecated.class), enumName, mtdName
                    + formatParams(paramNames), "Method");

        }
    }

    private void compareDeprecated(boolean depTag, boolean depAnt, String cls, String name, String type) {
        if (depTag != depAnt) {
            String apiName = cls;
            if (name != null) {
                apiName += "." + name;
            }
            if (depTag) {
                pw.println("No @Deprecated annotation: [" + type + "] " + apiName);
            } else {
                pw.println("No @deprecated JavaDoc tag: [" + type + "] " + apiName);
            }
            errCount++;
        }
    }

    private static boolean isPublicOrProtected(int modifier) {
        return ((modifier & Modifier.PUBLIC) != 0) || ((modifier & Modifier.PROTECTED) != 0);
    }

    // Check if a method is automatically generated for a each Enum
    private static boolean isBuiltinEnumMethod(Method mtd) {
        // Just check method name for now
        String name = mtd.getName();
        return name.equals("values") || name.equals("valueOf");
    }

    private static boolean isAPIDeprecated(APIInfo api) {
        return api.isDeprecated() || api.isInternal() || api.isObsolete();
    }

    private static APIInfo findClassInfo(Set<APIInfo> apis, String cls) {
        for (APIInfo api : apis) {
            String clsName = api.getPackageName() + "." + api.getName();
            if (api.isClass() && clsName.equals(cls)) {
                return api;
            }
        }
        return null;
    }

    private static APIInfo findFieldInfo(Set<APIInfo> apis, String cls, String field) {
        for (APIInfo api : apis) {
            String clsName = api.getPackageName() + "." + api.getClassName();
            if (api.isField() && clsName.equals(cls) && api.getName().equals(field)) {
                return api;
            }
        }
        return null;
    }

    private static APIInfo findConstructorInfo(Set<APIInfo> apis, String cls, List<String> params) {
        for (APIInfo api : apis) {
            String clsName = api.getPackageName() + "." + api.getClassName();
            if (api.isConstructor() && clsName.equals(cls)) {
                // check params
                List<String> paramsFromApi = getParamNames(api);
                if (paramsFromApi.size() == params.size()) {
                    boolean match = true;
                    for (int i = 0; i < params.size(); i++) {
                        if (!params.get(i).equals(paramsFromApi.get(i))) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        return api;
                    }
                }
            }
        }
        return null;
    }

    private static APIInfo findMethodInfo(Set<APIInfo> apis, String cls, String method, List<String> params) {
        for (APIInfo api : apis) {
            String clsName = api.getPackageName() + "." + api.getClassName();
            if (api.isMethod() && clsName.equals(cls) && api.getName().equals(method)) {
                // check params
                List<String> paramsFromApi = getParamNames(api);
                if (paramsFromApi.size() == params.size()) {
                    boolean match = true;
                    for (int i = 0; i < params.size(); i++) {
                        if (!params.get(i).equals(paramsFromApi.get(i))) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        return api;
                    }
                }
            }
        }
        return null;
    }

    private static APIInfo findEnumInfo(Set<APIInfo> apis, String ecls) {
        for (APIInfo api : apis) {
            String clsName = api.getPackageName() + "." + api.getName();
            if (api.isEnum() && clsName.equals(ecls)) {
                return api;
            }
        }
        return null;
    }

    private static APIInfo findEnumConstantInfo(Set<APIInfo> apis, String ecls, String econst) {
        for (APIInfo api : apis) {
            String clsName = api.getPackageName() + "." + api.getClassName();
            if (api.isEnumConstant() && clsName.equals(ecls) && api.getName().equals(econst)) {
                return api;
            }
        }
        return null;
    }

    private static List<String> getParamNames(APIInfo api) {
        if (!api.isMethod() && !api.isConstructor()) {
            throw new IllegalArgumentException(api.toString() + " is not a constructor or a method.");
        }

        List<String> nameList = new ArrayList<>();
        String signature = api.getSignature();
        int start = signature.indexOf('(');
        int end = signature.indexOf(')');

        if (start < 0 || end < 0 || start > end) {
            throw new RuntimeException(api.toString() + " has bad API signature: " + signature);
        }

        String paramsSegment = signature.substring(start + 1, end);
        // erase generic args
        if (paramsSegment.indexOf('<') >= 0) {
            StringBuilder buf = new StringBuilder();
            int genericsNestLevel = 0;
            for (int i = 0; i < paramsSegment.length(); i++) {
                char c = paramsSegment.charAt(i);
                if (genericsNestLevel > 0) {
                    if (c == '<') {
                        genericsNestLevel++;
                    } else if (c == '>') {
                        genericsNestLevel--;
                    }
                } else {
                    if (c == '<') {
                        genericsNestLevel++;
                    } else {
                        buf.append(c);
                    }
                }
            }
            paramsSegment = buf.toString();
        }

        if (paramsSegment.length() > 0) {
            String[] params = paramsSegment.split("\\s*,\\s*");
            for (String p : params) {
                if (p.endsWith("...")) {
                    // varargs to array
                    p = p.substring(0, p.length() - 3) + "[]";
                }
                nameList.add(p);
            }
        }

        return nameList;
    }

    private static List<String> getParamNames(Constructor<?> ctor) {
        return toTypeNameList(ctor.getGenericParameterTypes());
    }

    private static List<String> getParamNames(Method method) {
        return toTypeNameList(method.getGenericParameterTypes());
    }

    private static final String[] PRIMITIVES = { "byte", "short", "int", "long", "float", "double", "boolean", "char" };
    private static char[] PRIMITIVE_SIGNATURES = { 'B', 'S', 'I', 'J', 'F', 'D', 'Z', 'C' };

    private static List<String> toTypeNameList(Type[] types) {
        List<String> nameList = new ArrayList<>();

        for (Type t : types) {
            StringBuilder s = new StringBuilder();
            if (t instanceof ParameterizedType) {
                // throw away generics parameters
                ParameterizedType prdType = (ParameterizedType) t;
                Class<?> rawType = (Class<?>) prdType.getRawType();
                s.append(rawType.getCanonicalName());
            } else if (t instanceof WildcardType) {
                // we don't need to worry about WildcardType,
                // because this tool erases generics parameters
                // for comparing method/constructor parameters
                throw new RuntimeException("WildcardType not supported by this tool");
            } else if (t instanceof TypeVariable) {
                // this tool does not try to resolve actual parameter
                // type - for example, "<T extends Object> void foo(T in)"
                // this tool just use the type variable "T" for API signature
                // comparison. This is actually not perfect, but should be
                // sufficient for our purpose.
                TypeVariable<?> tVar = (TypeVariable<?>) t;
                s.append(tVar.getName());
            } else if (t instanceof GenericArrayType) {
                // same as TypeVariable. "T[]" is sufficient enough.
                GenericArrayType tGenArray = (GenericArrayType) t;
                s.append(tGenArray.toString());
            } else if (t instanceof Class) {
                Class<?> tClass = (Class<?>) t;
                String tName = tClass.getCanonicalName();

                if (tName.charAt(0) == '[') {
                    // Array type
                    int idx = 0;
                    for (; idx < tName.length(); idx++) {
                        if (tName.charAt(idx) != '[') {
                            break;
                        }
                    }
                    int dimension = idx;
                    char sigChar = tName.charAt(dimension);

                    String elemType = null;
                    if (sigChar == 'L') {
                        // class
                        elemType = tName.substring(dimension + 1, tName.length() - 1);
                    } else {
                        // primitive
                        for (int i = 0; i < PRIMITIVE_SIGNATURES.length; i++) {
                            if (sigChar == PRIMITIVE_SIGNATURES[i]) {
                                elemType = PRIMITIVES[i];
                                break;
                            }
                        }
                    }

                    if (elemType == null) {
                        throw new RuntimeException("Unexpected array type: " + tName);
                    }

                    s.append(elemType);
                    for (int i = 0; i < dimension; i++) {
                        s.append("[]");
                    }
                } else {
                    s.append(tName);
                }
            } else {
                throw new IllegalArgumentException("Unknown type: " + t);
            }

            nameList.add(s.toString());
        }

        return nameList;
    }

    private static String formatParams(List<String> paramNames) {
        StringBuilder buf = new StringBuilder("(");
        boolean isFirst = true;
        for (String p : paramNames) {
            if (isFirst) {
                isFirst = false;
            } else {
                buf.append(", ");
            }
            buf.append(p);
        }
        buf.append(")");

        return buf.toString();
    }
}
