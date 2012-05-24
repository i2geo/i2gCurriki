package org.curriki.xwiki.plugin.lucene;

import com.xpn.xwiki.plugin.lucene.I2GLuceneProfile;
import com.xpn.xwiki.plugin.lucene.LuceneIndexProfile;
import org.apache.avalon.framework.logger.Log4JLogger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.XWikiContext;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;

/** Analyzer that uses the rigth (sub-)analyzer depending on the language and the 
 */
public class CurrikiAnalyzer extends Analyzer {

    private final static Log LOG = LogFactory.getLog(CurrikiAnalyzer.class);

    private final LuceneIndexProfile indexProfile;

    public final static String IDENTIFIERS_LIST = "identifiersList";

    public CurrikiAnalyzer(String language, LuceneIndexProfile profile) {
        this.language = language;
        if(analyzers==null)
            analyzers = PredefinedAnalyzers.createAnalyzerMap(null);
        this.indexProfile = profile;
    }

    public CurrikiAnalyzer(XWikiDocument doc, LuceneIndexProfile profile, XWikiContext context) {
        this(findOutLanguage(doc,context),profile,context);
    }

    public CurrikiAnalyzer(String language, LuceneIndexProfile profile, XWikiContext context) {
        this.language = language;
        if(analyzers==null)
            analyzers = PredefinedAnalyzers.createAnalyzerMap(context);
        this.indexProfile = profile;
    }


    /** @deprecated context is unused, please use {@link #CurrikiAnalyzer(String,LuceneIndexProfile)} */
    public CurrikiAnalyzer(String language, XWikiContext context) {
        this.language = language;
        if(analyzers==null)
            analyzers = PredefinedAnalyzers.createAnalyzerMap(context);
        this.indexProfile = null;
    }


    public static CurrikiAnalyzer getInstance(String languages, XWikiContext context, LuceneIndexProfile indexProfile) {
        if(languages==null || languages.length()==0 && context!=null) languages = context.getLanguage();
        if(languages==null || languages.length()==0 ) languages= "x-all";
        String[] langs = languages.split(",| ");
        if(langs.length==0) langs = new String[] {"x-all"};
        return new CurrikiAnalyzer(langs[0],indexProfile);
    }

    private static String findOutLanguage(XWikiDocument doc, XWikiContext context) {
        // find out language
        String foundLanguage = null;
        BaseObject assetObj= doc.getObject("CurrikiCode.AssetClass");

        if(assetObj!=null) {
            foundLanguage = assetObj.getStringValue("language");
        }
        if(foundLanguage==null) {
            foundLanguage = doc.getLanguage();
        }
        if(foundLanguage==null)
            foundLanguage = context.getLanguage();
        if(foundLanguage !=null)
            return foundLanguage;
        else
            return "x-all";

    }

    private final String language;

    private static Map<String,Analyzer> analyzers = null;


    public TokenStream tokenStream(String fieldName, Reader reader) {
        boolean isIdentifier = false;
        if("ft".equals(fieldName)) return analyzers.get("x-all").tokenStream(fieldName,reader);
        if(indexProfile!=null) isIdentifier= indexProfile.isIdentifierField(fieldName);
        else isIdentifier = !fieldName.endsWith(".stemmed");
        if(isIdentifier) {
            Analyzer an = analyzers.get(IDENTIFIERS_LIST);
            //LOG.info("Field \"" + fieldName + "\" gives analyzer " + an);
            if(LOG.isTraceEnabled()) LOG.trace("Field \"" + fieldName + "\" gives analyzer " + an);
            return an.tokenStream(fieldName,reader);
        } else {
            Analyzer an = analyzers.get(language);
            if(an==null) an = analyzers.get("x-all");
            if(an==null) an = new StandardAnalyzer(Version.LUCENE_29);
            if(LOG.isTraceEnabled()) LOG.trace("IdentifierField \"" + fieldName + "\" gives analyzer " + an);
            return an.tokenStream(fieldName,reader);
        }
    }

    public static void main(String[] args) throws Throwable {
        String language = args[0],
            fieldName = args[1];
        displayAnalysis(language, fieldName, args[2], System.out);
    }

    public static void displayAnalysis(String language, String fieldName, String content, PrintStream out) throws Throwable{
        CurrikiAnalyzer analyzer = new CurrikiAnalyzer(language, new I2GLuceneProfile());
        TokenStream ts = analyzer.tokenStream(fieldName, new StringReader(content));
        out.println("Analyzer for field " + fieldName + " and for language " + language + " using token stream " + ts + " of class " + (ts!=null? ts.getClass():"null"));
        Token tok = ts.next();
        while(tok!=null) {
            out.println("- " + tok.term());
            tok = ts.next();
        }
        out.println("Finished analyzing.");

    }
}
