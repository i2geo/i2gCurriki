package org.curriki.xwiki.plugin.lucene;


import com.xpn.xwiki.plugin.lucene.I2GLuceneProfile;
import junit.framework.TestCase;
import org.apache.lucene.analysis.*;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;

import java.io.PrintStream;
import java.io.StringReader;

public class CurrikiAnalyzerTest extends TestCase {

    public CurrikiAnalyzerTest(String name) { super(name); }


    public void testSimpleStemming() throws Throwable {
        String fieldName = "ft", language = "eng", content = "horses";
        //CurrikiAnalyzer.displayAnalysis("eng","ft.stemmed","horses", System.out);
        checkAnalysis("ft","eng","Horses should be stemmed to hors","horses","hors",false);
        checkAnalysis("ft.stemmed","eng","Horses should be stemmed to hors","horses","hors",true);

        checkAnalysis("ft","eng","Spinning should be stemmed to sping","Spinning","spin",false);
        checkAnalysis("ft.stemmed","eng","Spinning should be stemmed to sping","Spinning","spin",true);

        checkAnalysis("ft","eng","Dared should be stemmed to dar","Dared","dare",false);
        checkAnalysis("ft.stemmed","eng","Dared should be stemmed to dar","Dared","dare",true);
    }

    private static void checkAnalysis(String fieldName, String language, String msg, String content, String expected, boolean shouldBeEqual) throws Throwable {
        CurrikiAnalyzer analyzer = new CurrikiAnalyzer(language,new I2GLuceneProfile());
        PrintStream out = System.out;
        TokenStream ts = analyzer.tokenStream(fieldName, new StringReader(content));
        out.println("Analyzer for field " + fieldName + " and for language " + language + " using token stream " + ts + " of class " + (ts!=null? ts.getClass():"null"));
        Token tok = ts.next();
        StringBuffer buff = new StringBuffer();
        while(tok!=null) {
            out.println("- " + tok.term());
            buff.append(tok.term()).append('\n');
            tok = ts.next();
        }
        out.println("Finished analyzing.");
        if(shouldBeEqual)
            assertEquals(msg, expected, buff.toString().trim());
        else
            assertNotSame(msg, expected, buff.toString().trim());
    }


    public void testQueryParserEnglish() throws Exception {
        checkQueryParser("text","eng","object:CurrikiCode.AssetClass blip:blop", "object", "CurrikiCode.AssetClass");
        checkQueryParser("ft","eng","author:XWiki.polx blip:blop", "author", "XWiki.polx");
    }
    public void testQueryParserSpanish() throws Exception {
        checkQueryParser("ft.stemmed","spa","ángulo blip:blop", "ft.stemmed", "ángulo");
    }

    private void checkQueryParser(String defaultField, String language, String query, String expectedField, String expectedText) throws Exception {
        QueryParser qp = new QueryParser(Version.LUCENE_29,defaultField,
                //,new WhitespaceAnalyzer());
                CurrikiAnalyzer.getInstance(language,null,new I2GLuceneProfile()));
        Query q = qp.parse(query);
        assertTrue(q instanceof BooleanQuery);
        BooleanQuery bq = (BooleanQuery)q;
        assertTrue((bq.getClauses()[0].getQuery()) instanceof TermQuery);
        TermQuery tq = (TermQuery) bq.getClauses()[0].getQuery();
        assertEquals(expectedField,tq.getTerm().field());
        assertEquals(expectedText,tq.getTerm().text());
    }
}