// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2016, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.coverage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A tool used for scanning JaCoCo report.xml and detect methods not covered by the
 * ICU4J unit tests. This tool is called from ICU4J ant target: coverageJaCoCo, and
 * signals failure if there are any methods with no test coverage (and not included
 * in 'coverage-exclusion.txt').
 */
public class JacocoReportCheck {
    public static void main(String... args) {
        if (args.length < 1) {
            System.err.println("Missing jacoco report.xml");
            System.exit(1);
        }

        System.out.println("Checking method coverage in " + args[0]);
        if (args.length > 1) {
            System.out.println("Coverage check exclusion file: " + args[1]);
        }

        File reportXml = new File(args[0]);
        Map<String, ReportEntry> entries = parseReport(reportXml);
        if (entries == null) {
            System.err.println("Failed to parse jacoco report.xml");
            System.exit(2);
        }

        Set<String> excludedSet = new HashSet<String>();
        if (args.length > 1) {
            File exclusionTxt = new File(args[1]);
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(exclusionTxt)));
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    line = line.trim();
                    if (line.startsWith("//") || line.length() == 0) {
                        // comment or blank line
                        continue;
                    }
                    boolean added = excludedSet.add(line);
                    if (!added) {
                        System.err.println("Warning: Duplicated exclusion entry - " + line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        // ignore
                    }
                }
            }
        }


        Set<String> noCoverageSet = new TreeSet<String>();
        Set<String> coveredButExcludedSet = new TreeSet<String>();

        for (ReportEntry reportEntry : entries.values()) {
            String key = reportEntry.key();
            Counter methodCnt = reportEntry.method().methodCounter();
            int methodMissed = methodCnt == null ? 1 : methodCnt.missed();
            if (methodMissed > 0) {
                // no test coverage
                if (!excludedSet.contains(key)) {
                    noCoverageSet.add(key);
                }
            } else {
                // covered
                if (excludedSet.contains(key)) {
                    coveredButExcludedSet.add(key);
                }
            }
        }

        if (noCoverageSet.size() > 0) {
            System.out.println("//");
            System.out.println("// Methods with no test coverage, not included in the exclusion set");
            System.out.println("//");
            for (String key : noCoverageSet) {
                System.out.println(key);
            }
        }

        if (coveredButExcludedSet.size() > 0) {
            System.out.println("//");
            System.out.println("// Methods covered by tests, but included in the exclusion set");
            System.out.println("//");
            for (String key : coveredButExcludedSet) {
                System.out.println(key);
            }
        }

        System.out.println("Method coverage check finished");

        if (noCoverageSet.size() > 0) {
            System.err.println("Error: Found method(s) with no test coverage");
            System.exit(-1);
        }
    }

    private static Map<String, ReportEntry> parseReport(File reportXmlFile) {
        try {
            Map<String, ReportEntry> entries = new TreeMap<String, ReportEntry>();
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            docBuilder.setEntityResolver(new EntityResolver() {
                // Ignores JaCoCo report DTD
                public InputSource resolveEntity(String publicId, String systemId) {
                    return new InputSource(new StringReader(""));
                }
            });
            Document doc = docBuilder.parse(reportXmlFile);
            NodeList nodes = doc.getElementsByTagName("report");
            for (int idx = 0; idx < nodes.getLength(); idx++) {
                Node node = nodes.item(idx);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element reportElement = (Element)node;
                NodeList packages = reportElement.getElementsByTagName("package");
                for (int pidx = 0 ; pidx < packages.getLength(); pidx++) {
                    Node pkgNode = packages.item(pidx);
                    if (pkgNode.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    Element pkgElement = (Element)pkgNode;
                    NodeList classes = pkgElement.getChildNodes();
                    if (classes == null) {
                        continue;
                    }

                    // Iterate through classes
                    for (int cidx = 0; cidx < classes.getLength(); cidx++) {
                        Node clsNode = classes.item(cidx);
                        if (clsNode.getNodeType() != Node.ELEMENT_NODE || !"class".equals(clsNode.getNodeName())) {
                            continue;
                        }
                        Element clsElement = (Element)clsNode;
                        String cls = clsElement.getAttribute("name");

                        NodeList methods = clsNode.getChildNodes();
                        if (methods == null) {
                            continue;
                        }

                        // Iterate through method elements
                        for (int midx = 0; midx < methods.getLength(); midx++) {
                            Node mtdNode = methods.item(midx);
                            if (mtdNode.getNodeType() != Node.ELEMENT_NODE || !"method".equals(mtdNode.getNodeName())) {
                                continue;
                            }
                            Element mtdElement = (Element)mtdNode;
                            String mtdName = mtdElement.getAttribute("name");
                            String mtdDesc = mtdElement.getAttribute("desc");
                            String mtdLineStr = mtdElement.getAttribute("line");
                            assert mtdName != null;
                            assert mtdDesc != null;
                            assert mtdLineStr != null;

                            int mtdLine = -1;
                            try {
                                 mtdLine = Integer.parseInt(mtdLineStr);
                            } catch (NumberFormatException e) {
                                // Ignore line # parse failure
                                e.printStackTrace();
                            }

                            // Iterate through counter elements and add report entries

                            Counter instructionCnt = null;
                            Counter branchCnt = null;
                            Counter lineCnt = null;
                            Counter complexityCnt = null;
                            Counter methodCnt = null;

                            NodeList counters = mtdNode.getChildNodes();
                            if (counters == null) {
                                continue;
                            }
                            for (int i = 0; i < counters.getLength(); i++) {
                                Node cntNode = counters.item(i);
                                if (cntNode.getNodeType() != Node.ELEMENT_NODE) {
                                    continue;
                                }
                                Element cntElement = (Element)cntNode;
                                String type = cntElement.getAttribute("type");
                                String missedStr = cntElement.getAttribute("missed");
                                String coveredStr = cntElement.getAttribute("covered");
                                assert type != null;
                                assert missedStr != null;
                                assert coveredStr != null;

                                int missed = -1;
                                int covered = -1;
                                try {
                                    missed = Integer.parseInt(missedStr);
                                } catch (NumberFormatException e) {
                                    // Ignore missed # parse failure
                                    e.printStackTrace();
                                }
                                try {
                                    covered = Integer.parseInt(coveredStr);
                                } catch (NumberFormatException e) {
                                    // Ignore covered # parse failure
                                    e.printStackTrace();
                                }

                                if (type.equals("INSTRUCTION")) {
                                    instructionCnt = new Counter(missed, covered);
                                } else if (type.equals("BRANCH")) {
                                    branchCnt = new Counter(missed, covered);
                                } else if (type.equals("LINE")) {
                                    lineCnt = new Counter(missed, covered);
                                } else if (type.equals("COMPLEXITY")) {
                                    complexityCnt = new Counter(missed, covered);
                                } else if (type.equals("METHOD")) {
                                    methodCnt = new Counter(missed, covered);
                                } else {
                                    System.err.println("Unknown counter type: " + type);
                                    // Ignore
                                }
                            }
                            // Add the entry
                            Method method = new Method(mtdName, mtdDesc, mtdLine,
                                    instructionCnt, branchCnt, lineCnt, complexityCnt, methodCnt);

                            ReportEntry entry = new ReportEntry(cls, method);
                            ReportEntry prev = entries.put(entry.key(), entry);
                            if (prev != null) {
                                System.out.println("oh");
                            }
                        }
                    }
                }
            }
            return entries;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        } catch (SAXException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static class Counter {
        final int missed;
        final int covered;

        Counter(int missed, int covered) {
            this.missed = missed;
            this.covered = covered;
        }

        int missed() {
            return missed;
        }

        int covered() {
            return covered;
        }
    }

    private static class Method {
        final String name;
        final String desc;
        final int line;

        final Counter instructionCnt;
        final Counter branchCnt;
        final Counter lineCnt;
        final Counter complexityCnt;
        final Counter methodCnt;

        Method(String name, String desc, int line,
                Counter instructionCnt, Counter branchCnt, Counter lineCnt,
                Counter complexityCnt, Counter methodCnt) {
            this.name = name;
            this.desc = desc;
            this.line = line;
            this.instructionCnt = instructionCnt;
            this.branchCnt = branchCnt;
            this.lineCnt = lineCnt;
            this.complexityCnt = complexityCnt;
            this.methodCnt = methodCnt;
        }

        String name() {
            return name;
        }

        String desc() {
            return desc;
        }

        int line() {
            return line;
        }

        Counter instructionCounter() {
            return instructionCnt;
        }

        Counter branchCounter() {
            return branchCnt;
        }

        Counter lineCounter() {
            return lineCnt;
        }

        Counter complexityCounter() {
            return complexityCnt;
        }

        Counter methodCounter() {
            return methodCnt;
        }
    }

    private static class ReportEntry {
        final String cls;
        final Method method;
        final String key;

        ReportEntry(String cls, Method method) {
            this.cls = cls;
            this.method = method;
            this.key = cls + "#" + method.name() + ":" + method.desc();
        }

        String key() {
            return key;
        }

        String cls() {
            return cls;
        }

        Method method() {
            return method;
        }
    }

}
