package org.curriki.xwiki.plugin.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.util.*;

import com.xpn.xwiki.XWikiContext;
import org.apache.lucene.util.Version;

/** */
public class PredefinedAnalyzers {
    // ============= predefined analyzers for each language ================================
    public static Map<String,Analyzer> createAnalyzerMap(XWikiContext context) {
        Map<String,Analyzer> analyzers = new HashMap<String,Analyzer>();
        putFirstAnalyzers(analyzers);
        String languagesS = null;
        if(context!=null)
            languagesS = context.getWiki().getXWikiPreference("languages",context);
        if(languagesS==null || languagesS.length()==0) languagesS = "en,fr,de";
        StringTokenizer stok = new StringTokenizer(languagesS,", ",false);
        while(stok.hasMoreTokens()) {
            String lang = stok.nextToken();
            if(!analyzers.containsKey(lang))analyzers.put(lang, makeAnalyzer(lang));
            if(lang.length()==2) {// add three letter variant as well
                String otherLang = new Locale(lang).getISO3Language();
                if(!analyzers.containsKey(otherLang))
                    analyzers.put(otherLang, makeAnalyzer(otherLang));
            }
        }
        analyzers.put("x-all", makeAnalyzer("x-all"));
        return analyzers;
    }

    private static Analyzer makeAnalyzer(String lang) {
        return new StandardAnalyzer(Version.LUCENE_29,Collections.EMPTY_SET);
    }

    private static void putFirstAnalyzers(Map<String,Analyzer>map) {
        // put both language codes here, see, e.g., http://fr.wikipedia.org/wiki/Liste_des_codes_ISO_639-2
        map.put(CurrikiAnalyzer.IDENTIFIERS_LIST,new IdentifierListAnalyzer());
        map.put("es",new SnowballAnalyzer(Version.LUCENE_29,"Spanish"));
        map.put("spa",new SnowballAnalyzer(Version.LUCENE_29,"Spanish"));
        map.put("de", new GermanAnalyzer(Version.LUCENE_29));
        map.put("deu",new GermanAnalyzer(Version.LUCENE_29));
        map.put("fr", new FrenchAnalyzer(Version.LUCENE_29));
        map.put("fra",new FrenchAnalyzer(Version.LUCENE_29));
        map.put("it", new SnowballAnalyzer(Version.LUCENE_29,"Italian"));
        map.put("ita",new SnowballAnalyzer(Version.LUCENE_29,"Italian"));
        map.put("nl", new DutchAnalyzer(Version.LUCENE_29));
        map.put("dut",new DutchAnalyzer(Version.LUCENE_29));
        map.put("nld",new DutchAnalyzer(Version.LUCENE_29));
        map.put("ru", new RussianAnalyzer(Version.LUCENE_29));
        map.put("rus",new RussianAnalyzer(Version.LUCENE_29));
        map.put("en", new SnowballAnalyzer(Version.LUCENE_29,"English"));
        map.put("eng",new SnowballAnalyzer(Version.LUCENE_29,"English"));
        map.put("pt",new SnowballAnalyzer(Version.LUCENE_29,"Portuguese"));
        map.put("por",new SnowballAnalyzer(Version.LUCENE_29,"Portuguese"));

    }



}

