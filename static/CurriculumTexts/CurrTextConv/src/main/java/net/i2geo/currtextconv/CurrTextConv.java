// CurrTextConv.java
// to compile: mvn package
// usage: java -cp target\CurrTextConv-2.0-SNAPSHOT-jar-with-dependencies.jar 
//        net.i2geo.currtextconv.CurrTextConv inputFile outputFile [encoding] 
//
// Test: main uses inputFile = a.htm; outputFile = b.htm
//
// Czech curricula use enconding windows-1250

// TO DO: 
//   - handle other encodings. Default utf-8. Write in the same encoding the original file is written in.
//   - handle .pdf files

package net.i2geo.currtextconv;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
//import java.io.FileNotFoundException;
import java.io.FileOutputStream;
//import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
//import java.io.StringWriter;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
//import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
//import javax.xml.parsers.ParserConfigurationException;
//import javax.xml.parsers.SAXParser;
//import javax.xml.parsers.SAXParserFactory;
//import javax.xml.transform.Source;
//import javax.xml.transform.TransformerFactory;

//import javax.xml.transform.dom.DOMSource;

//import javax.xml.transform.stream.StreamResult;

import org.apache.commons.httpclient.HttpClient;

import org.apache.commons.httpclient.HttpMethod;

import org.apache.commons.httpclient.methods.GetMethod;

//import org.cyberneko.html.parsers.DOMParser;

//import org.w3c.dom.Document;
import org.w3c.dom.Element;
//import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;
//import org.xml.sax.SAXException;
//import org.xml.sax.helpers.DefaultHandler;

public class CurrTextConv {

    public static final int VERBOSE = 0; // 0 = normal mode, no tracing
                                         // 1 = light tracing
                                         // 2 = writes _A_ _LOT_ _OF_ output for debugging

    public static final int CRITERION = 1; // 0 = old, slow, keeps many links; looks whether
                                           //     the "real" page is OK
                                           // 1 = new, fast, keeps few links; looks whether
                                           //     http://i2geo.net/SearchI2G/render?uri=%23
                                           //     is empty or not.

    // List of patterns of links pointing to the ontology
    public static List patterns = new ArrayList();

    // Link status can be:
    //     - NON_ONTOLOGY: the HREF does not attempt to point to an ontology node
    //     - WORKS: the link is working correctly.
    //     - DOES_NOT_WORK: the link is not working.
    // public enum LINKSTATUS {NONONTOLOGY2, WORKS2, DOESNOTWORK2}
    // FOR SOME REASON, THE STUPID MAVEN IS UNABLE TO COMPILE A ENUMERATION,
    // SO I HAVE TO USE CONSTANTS.
    public static final int NON_ONTOLOGY  = 0;
    public static final int WORKS         = 1;
    public static final int DOES_NOT_WORK = 2;

    static HttpClient httpClient;
    static String correctURL; // used to store results in calls to analyzeLink
    static String correctCompetencyName; // used to store results in calls to analyzeLink
    static String encoding = "utf-8"; // encoding of input file, by default is utf-8
    static boolean someURLHasWorked = false; // used to suspect whether Internet connection is failing
    static boolean connectionErrorHappened = false; // used to suspect whether Internet connection has failed

    // This ancillary class is used just to remember which links have to be modified.
    static class LinkToBeModified {
        public String oldHRef;
        public String newHRef;
        public String correctCompetencyName;
        public LinkToBeModified(String oldhref, String newhref, String ccn){
            oldHRef = oldhref;
            newHRef = newhref;
            correctCompetencyName = ccn;
        }
    };


    public static void main( String[] args ){

        // List of patterns of links that point to the ontology
        httpClient = new HttpClient();
        // patterns.get(0) is for links of the form <a href="http://www.inter2geo.eu/2008/ontology/ontology.owl#Collinear_vectors" target="_blank">
        patterns.add( Pattern.compile(".*ontology\\.owl#([^\"']*).*") );
        // patterns.get(1) is for links of the form <a href="http://www.inter2geo.eu/2008/ontology/GeoSkills#Collinear_vectors" target="_blank">
        patterns.add( Pattern.compile(".*GeoSkills#([^\"']*).*") );
        // patterns.get(2) is for links of the form <a href="http://i2geo.net/comped/showTopic.html?uri=common_multiple">
        patterns.add( Pattern.compile(".*showTopic\\.html\\?uri=([^\"']*).*") );
        // patterns.get(3) is for links of the form <a href="http://i2geo.net/comped/showCompetency.html?uri=use_solids_to_solve_realworld_problems">
        patterns.add( Pattern.compile(".*showCompetency\\.html\\?uri=([^\"']*).*") );
        // patterns.get(4) is for links of the form <a href="http://i2geo.net/comped/show.html?uri=common_multiple">
        patterns.add( Pattern.compile(".*show\\.html\\?uri=([^\"']*).*") );

        if ((args.length != 2) && (args.length != 3)) {
            System.out.println("\nCurrTextConv usage:  CurrTextConv inputFile.ext outputFile[.ext] [encoding:utf-8]");
            return;
        }
        String inputFileName  = args[0];
        String outputFileName = args[1];
        if (args.length==3) {
            encoding = args[2];
        }
        int dotIndex = inputFileName.lastIndexOf(".");
        if (dotIndex == -1) {
            // Input file name has no extension
            System.out.println("\nInput file must have extension .htm or .html");
            return;
        }
        String inputExt = inputFileName.substring(dotIndex+1, inputFileName.length());
        if (outputFileName.lastIndexOf(".") == -1) {
            // Output file has no extension; assume same as input
            outputFileName = outputFileName + "." + inputExt;
        }
        
        if (!inputExt.toLowerCase().equals("html") && !inputExt.toLowerCase().equals("htm")) {
            // TO DO: handle .pdf files
            // Input file name has wrong extension
            System.out.println("\nInput file must have extension .htm or .html");
            return;
        }else{
            processHTMLFile(inputFileName, outputFileName);
        }

        if (!someURLHasWorked) {
            System.out.println("\n\nWARNING! NO LINK HAS BEEN FOUND TO BE WORKING!");
            System.out.println("ARE YOU SURE YOUR INTERNET CONNECTION IS WORKING?");
        }
        if (connectionErrorHappened) {
            System.out.println("SOME EXCEPTION HAS HAPPENED; ARE YOU SURE YOUR INTERNET CONNECTION IS WORKING?");
        }
    }


    // This function receives two file names; reads in the input and writes out 
    // the output. It first tries the DOMParser, then the DocumentBuilderFactory,
    // and if everything has failed, it searches for the links using regular
    // expressions.
    static void processHTMLFile(String inputFileName, String outputFileName) {
        Document doc;
        int i, j;

        String fileContent = readFileAsString(inputFileName);
        if (fileContent == null) { // The file could not be read
            return;
        }

        if (tryDOMParser(fileContent, outputFileName)) {
            // The DOMParser has failed
            if (tryDocumentBuilder(fileContent, outputFileName)) {
                // The DocumentBuilder parser has failed
                if (tryRegularExpressions(fileContent, outputFileName)) {
                    System.err.println("CurrTextConv.processHTMLFile( inputFileName = " 
                        + inputFileName + ")  : no parser can parse this file");
                }else{
                    System.err.println("CurrTextConv.processHTMLFile( inputFileName = " 
                        + inputFileName + ")  : WARNING this file could be parsed only with regular expressions");
                }
            }
        }
    }


    public static String readFileAsString(String filename) {
        try{
            StringBuilder aux = new StringBuilder("");
            LineNumberReader lnr = new LineNumberReader(new InputStreamReader(new FileInputStream(filename), encoding));
            String line;
            while ((line = lnr.readLine()) != null) {
                aux.append(line + System.getProperty("line.separator"));
            }
            String inputFileContent = aux.toString();
            
            if (VERBOSE!=0) {
                System.out.println("inputFileContent = : \n" + inputFileContent);
                System.out.println("");
                System.out.println("");
                System.out.println("--------------------------------------------------------------");
                System.out.println("");
                System.out.println("");
            }
            
            return inputFileContent;
        }catch(Exception e){
            System.err.println("CurrTextConv.readFileAsString( filename = " + filename + ") : Error while reading file");
            e.printStackTrace();
            return null;
        }
    }


    // This function tries to parser the file using a DOMParser; it returns true
    // if an error happens.
    public static boolean tryDOMParser(String fileContent, String outputFileName) {
        if (VERBOSE!=0) {
            System.out.println("TRYING DOMPARSER");
        }
        try {
            DOMParser parser = new DOMParser();
            parser.parse(new InputSource(new StringReader(fileContent)));
            Document doc = parser.getDocument();
            return modifyLinksInDoc(doc, fileContent, outputFileName);
        }catch(Exception e) {
            System.err.println("ERROR WHILE PARSING HTML WITH DOM PARSER");
            e.printStackTrace();
            return true;
        }
    }


    // This function tries to parse the file using a DocumentBuilderFactory;
    // it returns true is an error happens.
    public static boolean tryDocumentBuilder(String fileContent, String outputFileName) {
        if (VERBOSE!=0) {
            System.out.println("TRYING DOCUMENTBUILDERFACTORY");
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setValidating(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(fileContent)));
            return modifyLinksInDoc(doc, fileContent, outputFileName);
        }catch(Exception e) {
            System.err.println("ERROR WHILE PARSING HTML WITH DOCUMENTBUILDERFACTORY");
            e.printStackTrace();
            return true;
        }
    }


    public static boolean tryRegularExpressions(String fileContent, String outputFileName){
        if (VERBOSE!=0) {
            System.out.println("TRYING REGULAR EXPRESSIONS");
        }
        Vector linksToBeModified = new Vector();
        Vector linksToBeRemoved  = new Vector();
        
        // Search for links:                   <    A    [....]HREF    =      "' (       )  "' [...]>tttttttttt<    /    A    >
        // whitespace                           wwww wwww          wwww wwww                                    wwww wwww wwww
        Pattern patternLink = Pattern.compile("<\\s*A\\s+[^>]*?HREF\\s*=\\s*[\"']([^\"']*)[\"'][^>]*>[^uFFFF]*?<\\s*/\\s*A\\s*>");
        Matcher matcher = patternLink.matcher(fileContent.toUpperCase());
        while (matcher.find()) {
            int beginHRef = matcher.start(1);
            int endHRef   = matcher.end  (1);
            String href   = fileContent.substring(beginHRef, endHRef);
            testHRef(href, linksToBeModified, linksToBeRemoved);
        }        
        return modifyLinksInString(linksToBeModified, linksToBeRemoved, fileContent, outputFileName);
    }


    // This function tries to extract the links in doc, replace them in fileContent,
    // and write outputFileName. It returns true if some error happens.
    public static boolean modifyLinksInDoc(Document doc, String fileContent, String outputFileName){
        int i;
        Vector linksToBeModified = new Vector();
        Vector linksToBeRemoved  = new Vector();
        
        NodeList list = doc.getElementsByTagName("*");
        if (VERBOSE!=0) {
            System.out.println("list.getLength() = " + list.getLength());
        }
        for (i=0; i<list.getLength(); i++) {
            Element element = (Element)list.item(i);
            if ((VERBOSE==2) || ((VERBOSE==1) && (element.getNodeName().toUpperCase().equals("A")))) {
                System.out.println("element.getNodeName() = " + element.getNodeName() + "   \"" + element.getTextContent() + "\"");
            }
            if (element.getNodeName().toUpperCase().equals("A")) {
                String href = element.getAttribute("href");
                testHRef(href, linksToBeModified, linksToBeRemoved);
            }
        }
        return modifyLinksInString(linksToBeModified, linksToBeRemoved, fileContent, outputFileName);
    }


    // This function modifies or removes links in the string fileContent.
    // If an error happens, it returns true.
    public static boolean modifyLinksInString(Vector linksToBeModified, Vector linksToBeRemoved, 
                                            String fileContent, String outputFileName)
    {
        Pattern patternLink, patternEndLink;
        Matcher matcher, matcherEndLink;
        String hrefInitial, hrefFinal, cCompName, newA;
        int i, j, k, beginOpenA, endOpenA, beginCloseA, endCloseA, lastFound;
        boolean found, newMatch;
        
        try {
            // Modify the links that have to be modified
            for (i=0; i<linksToBeModified.size(); i++) {
                LinkToBeModified ltbm = (LinkToBeModified)linksToBeModified.get(i);
                hrefInitial = ltbm.oldHRef;
                hrefFinal   = ltbm.newHRef;
                cCompName   = ltbm.correctCompetencyName;
                if (VERBOSE!=0) {
                    System.out.println("MODIFYING hrefInitial=\"" + hrefInitial 
                        + "\"   hrefFinal=\"" + hrefFinal + "\"    cCompName=\"" + cCompName + "\"");
                }
                
                // This loop is a little bit weird because we will be modifying the string fileContent
                // while we are using it to search for the pattern. This could produce errors. Hence, 
                // each time a matching is found, we start a new search. But then, if the original link
                // was correct, we'll fall in an infinite loop, because we will keep on changing for
                // ever the first link and leaving it the way it was; and we have to change it, to put
                // the onCLick clause. Hence, remember where it was last found, and modify only links
                // after that position.
                // Search for links:              (<    A    [....]HREF    =      "'     hrefInitial      "' [...]>)tttttttttt(<    /    A    >)
                // whitespace                       wwww wwww          wwww wwww                                                wwww wwww wwww
                // patternLink = Pattern.compile("(<\\s*A\\s+[^>]*?HREF\\s*=\\s*[\"']" + hrefInitial + "[\"'][^>]*>)[^uFFFF]*?(<\\s*/\\s*A\\s*>)");
                patternLink    = Pattern.compile("(<\\s*A\\s+[^>]*?HREF\\s*=\\s*[\"']" 
                                 + convertToRegularExpressionConstant(hrefInitial.toUpperCase()) 
                                 + "[\"'][^>]*>)");  // [^uFFFF]*?(<\\s*/\\s*A\\s*>)");
                
                lastFound = -1;
                found = true;
                while (found) {   
                    matcher = patternLink.matcher(fileContent.toUpperCase());
                    // Search for new match
                    newMatch = false;
                    while (found && !newMatch) {
                        found = matcher.find();
                        if (found) {
                            beginOpenA = matcher.start(1);
                            endOpenA   = matcher.end  (1);
                            if (beginOpenA > lastFound) {
                                // This is a new occurrence; modify and exit inner loop
                                lastFound = beginOpenA;
                                newMatch = true;
                                
                                beginOpenA  = matcher.start(1);
                                endOpenA    = matcher.end(1);
                                newA = "<a href=\"" + hrefFinal + "\" onclick=\"window.opener.chooseNode('" 
                                    + cCompName + "'); return false;\">";
                                if (VERBOSE!=0) {
                                    System.out.println("    Modified at " + beginOpenA + " - " + endOpenA);
                                }
                                if (VERBOSE==2) {
                                    System.out.println("tag A     = " + fileContent.substring(beginOpenA, endOpenA));
                                    System.out.println("new tag A = " + newA);
                                }
                                fileContent = fileContent.substring(0, beginOpenA) + newA + fileContent.substring(endOpenA);
                                if (VERBOSE==2) {
                                    System.out.println("new fileContent = " + fileContent);
                                }
                            }
                        }
                    }
                }
            }
            
            if (VERBOSE==2) {
                System.out.println("");
                System.out.println("");
                System.out.println("--------------------------------------------------------------");
                System.out.println("");
                System.out.println("");
                System.out.println("fileContent after modifying links:");
                System.out.println(fileContent);
            }
        
            patternEndLink = Pattern.compile("(<\\s*/\\s*A\\s*>)");
            // Remove the links that have to be modified
            for (i=0; i<linksToBeRemoved.size(); i++) {
                hrefInitial = (String)linksToBeRemoved.get(i);
                if (VERBOSE!=0) {
                    System.out.println("REMOVING hrefInitial=\"" + hrefInitial + "\"");
                }
                
                // This loop is a little bit weird because we will be modifying the string fileContent
                // while we are using it to search for the pattern. This could produce errors. Hence, 
                // each time a matching is found, we start a new search.
                // Search for links:              (<    A    [....]HREF    =      "'     hrefInitial      "' [...]>)tttttttttt(<    /    A    >)
                // whitespace                       wwww wwww           wwww wwww                                               wwww wwww wwww
                // patternLink = Pattern.compile("(<\\s*A\\s+[^>]*?HREF\\s*=\\s*[\"']" + hrefInitial + "[\"'][^>]*>)[^uFFFF]*?(<\\s*/\\s*A\\s*>)");
                patternLink    = Pattern.compile("(<\\s*A\\s+[^>]*?HREF\\s*=\\s*[\"']"
                                 + convertToRegularExpressionConstant(hrefInitial.toUpperCase())
                                 + "[\"'][^>]*>)" ); // [^uFFFF]*?(<\\s*/\\s*A\\s*>)");
                found = true;
                while (found) {
                    matcher = patternLink.matcher(fileContent.toUpperCase());
                    found = matcher.find();
                    if (found) {
                        beginOpenA  = matcher.start(1);
                        endOpenA    = matcher.end(1);
                        //beginCloseA = matcher.start(2);
                        //endCloseA   = matcher.end(2);
                        matcherEndLink = patternEndLink.matcher(fileContent.toUpperCase());
                        if (matcherEndLink.find(endOpenA)) {
                            beginCloseA = matcherEndLink.start(1);
                            endCloseA   = matcherEndLink.end(1);
                            
                            if (VERBOSE!=0) {
                                System.out.println("    Found at (" + beginOpenA + "-" + endOpenA + "), (" + beginCloseA + "-" + endCloseA);
                            }
                            if (VERBOSE==2) {
                                System.out.println("    Matched link with href " + hrefInitial);
                                System.out.println("    tag A     = " + fileContent.substring(beginOpenA, endOpenA));
                                System.out.println("    end tag A = " + fileContent.substring(beginCloseA, endCloseA));
                            }
                            fileContent =   fileContent.substring(0, beginOpenA)
                                          + fileContent.substring(endOpenA, beginCloseA) 
                                          + fileContent.substring(endCloseA);
                        }
                    }
                }
            }
            
            if (VERBOSE!=0) {
                System.out.println("");
                System.out.println("");
                System.out.println("--------------------------------------------------------------");
                System.out.println("");
                System.out.println("");
                System.out.println("fileContent after modifying and removing links:");
                System.out.println(fileContent);
            }

            // Finally, let us write the string in the output file
            try {
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFileName), encoding));
                out.write(fileContent);
                out.close();
                return false;
            }catch (Exception e){
                System.err.println("Error while writing to output file " + outputFileName);
                e.printStackTrace();
                return true;
            }
        }catch(Exception e){
            System.err.println("CurrTextConv.modifyLinksInString : error while modifying string");
            e.printStackTrace();
            return true;
        }
    }


    // This function receives a string and returns an equivalent constant string 
    // for use in regular expressions. For instance, a "?" in hrefInitial would be
    // interpreted as a quantifier. Replace it with \?, where \ is \\.
    // The same thing could happen with ()[]{}^+-*\$|?+.
    // So, this function receives something like "show.html?uri=Domain"
    // and returns "show\\.html\\?uri=Domain", which can be inserted into a regular
    // expression without causing disasters.
    static String convertToRegularExpressionConstant(String s) {
        int j, k;
        String charsReplace = "\\()[]{}^+-*$|?+.";
        String snew = s;
        
        for (k=snew.length()-1; k>=0; k--) {
            for (j=0; j<charsReplace.length(); j++) {
                if (snew.charAt(k) == charsReplace.charAt(j)) {
                    snew = snew.substring(0,k) + "\\" + snew.substring(k);
                }
            }
        }
        return snew;
    }

    // This function tests and original href and decides whether to add it to the 
    // list of links to be modified or to the list of links to be removed.
    static void testHRef(String href, Vector linksToBeModified, Vector linksToBeRemoved) {
        if (VERBOSE!=0) {
            System.out.println("LINK href=" + href);
        }
        int linkStatus = analyzeLink(href);
        if (linkStatus == WORKS) {
            // Replace link with correct URL and put onclick
            if (VERBOSE!=0) {
                System.out.println("WORKS");
            }
            // Note: we insert also the links whose href needs not be modified,
            // to make sure that they also get the onclick
            linksToBeModified.add( new LinkToBeModified( href , correctURL , correctCompetencyName ) );
        }else if(linkStatus == NON_ONTOLOGY) {
            // This is not a link to the ontology; leave it as it is
            if (VERBOSE!=0) {
                System.out.println("NON_ONTOLOGY");
            }
        }else{
            // remove link, but leave content
            if (VERBOSE!=0) {
                System.out.println("DOES_NOT_WORK");
            }
            linksToBeRemoved.add(href);
        }
    }


    // This function receives a link and decides whether it is working or not.
    // The input if the href of a link.
    // The output is:
    //     - return value is a LINK_STATUS.
    //     - correctURL and competencyName are overwritten (static variables)
    static int analyzeLink(String linkHref){
        String firstLetter, competencyName, upperCompetencyName;

        correctCompetencyName = "";
        correctURL = "";

        // Look in the list of patterns of links pointing to the ontology, and see
        // whether the provided link matches any of them
        ListIterator iterator = patterns.listIterator();
        boolean matched = false;
        competencyName = "";
        correctURL = "";
        while (iterator.hasNext() && !matched) {
            Pattern pattern = (Pattern) iterator.next();
            Matcher matcher = pattern.matcher(linkHref);
            if (matcher.find()) {
                competencyName = linkHref.substring(matcher.start(1),matcher.end(1));
                correctURL = "http://i2geo.net/comped/show.html?uri=" + competencyName;
                matched = true;
            }
        }

        // If no competency has been found, assume this link was not intended to point
        // to the ontology; do not modify it.
        if (competencyName.equals("")) {
            return NON_ONTOLOGY;
        }

        correctCompetencyName = "";
        correctURL = "";
        
        if (VERBOSE!=0) {
            System.out.println("testing competencyName=\"" + competencyName + "\"");
        }
        int test1 = testCompetencyName(competencyName);

        // If the first letter of the competency name is lower case, try upper case
        upperCompetencyName = "";
        firstLetter = competencyName.substring(0,1);
        if (! firstLetter.toUpperCase().equals(firstLetter)) {
            upperCompetencyName = firstLetter.toUpperCase() + competencyName.substring(1);
        }
        if (VERBOSE==2) {
            System.out.println("upperCompetencyName = \"" + upperCompetencyName + "\"");
        }
        if (!upperCompetencyName.equals("")) {
            if (correctURL.equals("")) {
                int test2 = testCompetencyName(upperCompetencyName);
            }
        }
        
        // Try adding _r
        int index_r = competencyName.lastIndexOf("_r");
        if (VERBOSE==2) {
            System.out.println("index_r=" + index_r);
            System.out.println("competencyName.length() = " + competencyName.length());
        }
        if (index_r + 2 != competencyName.length()) {
            if (correctURL.equals("")) {
                int test3 = testCompetencyName(competencyName + "_r");
            }
            // Try adding _r to the upper cased competency name
            if (upperCompetencyName != "") {
                if (correctURL.equals("")) {
                    int test4 = testCompetencyName(upperCompetencyName + "_r");
                }
            }
        }
        
        if (correctURL.equals("")) {
            // This link is failing
            System.out.println("DOES NOT WORK" + ": " + competencyName);
            return DOES_NOT_WORK;
        }else{
            // The link is working
            if (VERBOSE==2) {
                System.out.println("WORKS");
            }
            return WORKS;
        }
    }
    
    
    // Find out whether this competency name is working; in this case, modify
    // the static variables correctURL and correctCompetencyName
    static int testCompetencyName(String competencyName) {
        HttpMethod getMethod;
        String pageSourceString;
        int loc;
        boolean responseRead, connectionReleased;
        
        // Now, see whether the page is displayed correctly
        responseRead = false;
        connectionReleased = false;
        getMethod = null; // just so that the compiler doesn't complain it is not initialized
        if (VERBOSE==2) {
            System.out.println("testing competencyName = " + competencyName);
        }
        
        try {
            String testURL = "http://i2geo.net/SearchI2G/render?uri=" /*+"%23"*/ + competencyName;
            if (VERBOSE==2) {
                System.out.println("testURL    = " + testURL   );
            }
            
            if (CRITERION==0) {
                getMethod = new GetMethod(correctURL); 
            }else{
                getMethod = new GetMethod(testURL);
            }
            
            httpClient.executeMethod(getMethod);
            responseRead = true;
            
            // OLD: get using getResponseBodyAsString()
            // pageSourceString = getMethod.getResponseBodyAsString();
            
            // NEW: get using getResponseBodyAsStream()
            BufferedInputStream bis = new BufferedInputStream(getMethod.getResponseBodyAsStream());
            LineNumberReader lnr = new LineNumberReader(new InputStreamReader( bis ));
            pageSourceString = "";
            StringBuilder aux = new StringBuilder("");
            String line;
            while ((line = lnr.readLine()) != null) {
                aux.append(line + System.getProperty("line.separator"));
            }
            pageSourceString = aux.toString();
            
            if (VERBOSE==2) {
                System.out.println("pageSourceString = " + pageSourceString);
            }
            
            connectionReleased = true;
            getMethod.releaseConnection();

            if (VERBOSE==2) {
                System.out.println("page source length = " + pageSourceString.length());
            }
            
            // These are the values that will be returned within the next if blocks; 
            // if the link is not working, these variables will be set to "" later on.
            correctURL = "http://i2geo.net/comped/show.html?uri=" + competencyName;
            correctCompetencyName = competencyName;
            
            // If the page contains the substring java.lang.NullPointerException, then say
            // the link doesn't exist (it does, but an error happens).
            if (VERBOSE==2) {
                System.out.println("pageSourceString = " + pageSourceString);
            }
            if (pageSourceString.indexOf("java.lang.NullPointerException")==-1) { 
                if (CRITERION==0) {
                    // OLD TESTING CRITERION
                    // If the page source contains "<div class="topic-description">", then the link is working.
                    loc = pageSourceString.indexOf("<div class=\"topic-description\">");
                    if (loc != -1) {
                        someURLHasWorked = true;                 
                        return WORKS;
                    }
                    // If the page source contains "javascript:window.opener.chooseNode(", then the link is working.
                    loc = pageSourceString.indexOf("javascript:window.opener.chooseNode(");
                    if (loc != -1) {
                        someURLHasWorked = true;
                        return WORKS;
                    }
                }else{
                    // NEW TESTING CRITERION
                    if (pageSourceString.length() != 0) {
                        someURLHasWorked = true;
                        return WORKS;
                    }
                }
            }
            
            // If we have arrived here, it's because the link has failed
            correctURL = "";
            correctCompetencyName = "";
            return DOES_NOT_WORK;
            
        }catch(Exception e){
            if (!responseRead) {
                try {
                    pageSourceString= getMethod.getResponseBodyAsString();
                }catch(Exception e2) {;}
            }
            if (!connectionReleased) {
                try {
                    getMethod.releaseConnection();
                }catch(Exception e2) {;}
            }
            
            System.err.println("When trying to see page " + correctURL + ", the following error happened: " + e.getMessage());
            connectionErrorHappened = true;
            correctURL = "";
            correctCompetencyName = "";
            return DOES_NOT_WORK;
        }
    }
}
