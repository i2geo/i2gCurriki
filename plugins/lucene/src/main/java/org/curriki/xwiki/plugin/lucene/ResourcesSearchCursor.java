package org.curriki.xwiki.plugin.lucene;

import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.api.Property;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.plugin.lucene.I2GLuceneProfile;
import com.xpn.xwiki.plugin.lucene.LucenePluginApi;
import com.xpn.xwiki.plugin.lucene.IndexFields;
import com.xpn.xwiki.plugin.lucene.I2GResourceData;
import org.curriki.xwiki.plugin.lucene.CurrikiAnalyzer;
import org.curriki.xwiki.plugin.curriki.CurrikiPluginApi;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.XWikiMessageTool;

import java.util.*;
import java.text.SimpleDateFormat;
import java.io.IOException;

import org.curriki.xwiki.plugin.asset.Asset;
import org.apache.lucene.search.*;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.index.Term;
import org.apache.lucene.document.Field;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.*;
import net.i2geo.xwiki.SearchI2GXWikiApi;
import net.i2geo.xwiki.SearchI2GXWiki;
import net.i2geo.xwiki.SearchI2GXWikiPlugin;
import net.i2geo.xwiki.impl.DefaultSearchI2GXWiki;
import net.i2geo.api.search.UserQuery;
import net.i2geo.api.search.QueryExpansionResult;

/** Simple cursor class to push to zero the contents of the velocity search-result tool.
 *  */
public class ResourcesSearchCursor implements Iterator {
    private Map<String, String> requestDefaults = Collections.EMPTY_MAP;


    public ResourcesSearchCursor() {
        makeSureUsageLogIsInitted();
    }

    private static Log LOG = LogFactory.getLog(ResourcesSearchCursor.class);

    private int start=0, limit=25;
    private String sortField1, sortField2, sortDir="ASC";
    private String[] sortFields;
    private List<String> queries;
    private String languages;
    private Object luceneQuery = null;
    private Query expandedQuery = null;
    private String extraQuery = null;
    // TODO use languages


    private XWikiMessageTool msg;
    private XWiki xwiki;
    private XWikiContext context;
    Object o =null;
    SearchI2GXWiki searchI2G = null;

    /* private SearchResults results;
    private Iterator<SearchResult> resultIterator;
    private SearchResult currentResult; */

    private Hits hits = null;
    private int num = -1;
    private int end = 10;
    private org.apache.lucene.document.Document currentDoc;
    private Asset currentAsset = null;
    private Log log = LogFactory.getLog(ResourcesSearchCursor.class);
    private Query precomputedLuceneQuery = null;
    private List<String> messages = new LinkedList<String>();

    public List<String> getMessages() {
        return messages;
    }

    public QueryParser buildQueryParser(String defaultFieldName) {
        String languages = xwiki.getLanguagePreference() + ",en,x-all";
        QueryParser parser = new QueryParser(defaultFieldName,
                    CurrikiAnalyzer.getInstance(languages,context, I2GLuceneProfile.getInstance()));
        return parser;
    }

    public void addQueryShould(BooleanQuery boolQ, Query q) {
        boolQ.add(q,BooleanClause.Occur.SHOULD);
    }
    public void addQueryMust(BooleanQuery boolQ, Query q) {
        boolQ.add(q,BooleanClause.Occur.MUST);
    }
    public void addQueryMustNot(BooleanQuery boolQ, Query q) {
        boolQ.add(q,BooleanClause.Occur.MUST_NOT);
    }

    public void setLuceneQuery(Query luceneQuery) {
        this.luceneQuery = luceneQuery;
    }

    public String setRequestDefaults(Map<String,String> m) {
        this.requestDefaults = m;
        return "requestDefaults: ok";
    }

    public String readRequest(Map<String,String[]> paramsS) throws Exception {
        try { 
            Map<String,String> params = new HashMap<String,String>(paramsS.size());
            for(Map.Entry<String,String[]> entry: paramsS.entrySet()) {
                String[] val = entry.getValue();
                if(val.length==0) continue;
                String first = val[0];
                if(first!= null && first.length()>0)
                    params.put(entry.getKey(),first);
            }
            // add default values
            for(Map.Entry<String,String> entry: requestDefaults.entrySet()) {
                if(!params.containsKey(entry.getKey()))
                    params.put(entry.getKey(),entry.getValue());
            }

            // compute start and end
            if(params.get("start")!=null) {
                start = Integer.parseInt(params.get("start"));
            }
            if(params.get("limit")!=null) {
                limit = Integer.parseInt(params.get("limit"));
            }

            // compute sort-field
            if("title".equals(params.get("sort"))) {
                sortField1 = "title.untokenized";
                sortField2= "CurrikiCode.AssetClass.instructional_component.key.untokenized";
            } else if("ictText".equals(params.get("sort"))) {
                sortField1 = "CurrikiCode.AssetClass.instructional_component.key.untokenized";
                sortField2 = "title.untokenized";
            } else if("contributor".equals(params.get("sort"))) {
                sortField1 = "creator.untokenized";
                sortField2 = "title.untokenized";
            } else if("rating".equals(params.get("sort"))) {
                sortField1 = "-" + IndexFields.I2GEO_REVIEW_OVERALL_RANKING;
                sortField2 = "title.untokenized";
            } else if("updated".equals(params.get("sort"))) {
                sortField1 = "-date";
                sortField2 = "title.untokenized";
            } else {
                sortField1 = "relevance"; sortField2=null;
            }

            // descending asccending
            if(!"relevance".equals(sortField1) && "DESC".equals(params.get("dir"))) {
                if(sortField1.startsWith("-"))
                    sortField1 = sortField1.substring(1);
                else
                    sortField1 = "-" + sortField1;
            }
            sortFields = (sortField1 + "," + sortField2).split(",");


            // buid query
            queries = new ArrayList<String>();
            if(luceneQuery!=null) {
                this.precomputedLuceneQuery = (Query) luceneQuery;
            } else if(params.get("terms")!=null) {
                queries.add(params.get("terms"));
            }

            if(params.get("subject")!=null) {
                if(!params.get("subject").equals("UNCATEGORIZED")) {
                    queries.add("CurrikiCode.AssetClass.fw_items:"+params.get("subject"));
                } else {
                    List<String> docs = (List<String>)
                            xwiki.searchDocuments("where doc.web='FW_masterFramework' and doc.parent='FW_masterFramework.WebHome' order by doc.title");
                    for(String docName: docs) {
                        queries.add("-CurrikiCode.AssetClass.fw_items:" + docName);
                    }
                }
            }

            if(params.get("filetype")!=null) {
                queries.add("CurrikiCode.AssetClass.category:" +params.get("fileType"));
            }

            if(params.get("language")!=null) {
                queries.add("CurrikiCode.AssetClass.language:" +params.get("language"));
                languages = params.get("language");
            }
            if(params.get("level")!=null) {
                queries.add("CurrikiCode.AssetClass.educational_level2.key:" +params.get("level"));
            }
            if(params.get("ict")!=null) {
                queries.add("CurrikiCode.AssetClass.instructional_component2.key:" +params.get("ict"));
            }

            if(params.get("other")!=null) {
                queries.add(params.get("other"));
            }
            if(params.get("review")!=null) {
                /* TODO: queries for higher quality
                if("high".equals(params.get("review"))) {

                } else if ("highest".equals(params.get("review"))) {
                    queries.add()
                }*/
                /* if("partners".equals(params.get("review"))) {
                    queries.add("CRS.CurrikiReviewStatusClass.status:200");
                } else if("highest_rated".equals(params.get("review"))) {
                    queries.add("CRS.CurrikiReviewStatusClass.status:(800 OR 700)");
                }*/
            }


            if(params.get("special")!=null) {
                if("mine".equals(params.get("special"))) {
                    queries.add("creator:" + context.getUser());
                } else if("collections".equals(params.get("special"))) {
                    queries.add("XWiki.CompositeAssetClass.type:collection");
                } else if("reviewed".equals(params.get("collections"))) {
                    queries.add("CRS.CurrikiReviewStatusClass.status:(200 OR 300 OR 400 OR 500 OR 600 OR 700 OR 800)");
                } else if("updated".equals(params.get("special"))) {
                    queries.add("XWiki.CompositeAssetClass.type:collection");
                    GregorianCalendar cal = new GregorianCalendar();
                    cal.roll(GregorianCalendar.YEAR, false); // one year earlier
                    queries.add("date:[" + new SimpleDateFormat("yyyyMMdd").format(cal.getTime()) + " TO 29991231]");

                } else if("groups".equals(params.get("speical"))) {
                    queries.add("web:Group_Coll_*");
                }

                if(params.containsKey("extraQuery")) {
                    extraQuery = params.get("extraQuery");
                }
            }

            // assemble a string of queries
            return "ok";
        } catch (Exception e) {
            e.printStackTrace();
            messages.add("Trouble at reading requests: " + e.toString());
            return "error";
        }
    }

    public String graspContexts(XWiki xwiki, XWikiContext context, XWikiMessageTool msg) {
        this.xwiki = xwiki;
        this.context = context;
        this.msg = msg;
        o = xwiki.getPlugin("searchi2g");
        searchI2G =  (SearchI2GXWiki) o;
        if(log.isDebugEnabled()) log.debug("xwiki=" + xwiki + ", context=" + context + ", msg=" + msg);
        return "ok";
    }

    private Query assembleQuery() throws ParseException {
        BooleanQuery bq1;
        languages = xwiki.getLanguagePreference() + ",en,x-all";
        QueryParser parserStemmed = new QueryParser("ft.stemmed",
                    CurrikiAnalyzer.getInstance(languages,context, I2GLuceneProfile.getInstance())),
                parserKeys = new QueryParser("ft",
                        CurrikiAnalyzer.getInstance("x-all",context, I2GLuceneProfile.getInstance()));
        bq1 = (BooleanQuery) parserKeys.parse("+object:CurrikiCode.AssetClass AND NOT CurrikiCode.TextAssetClass.type:2 AND NOT web:AssetTemp AND NOT web:Coll_Templates AND NOT name:WebHome AND NOT name:WebPreferences AND NOT name:MyCollections AND NOT name:SpaceIndex AND NOT CurrikiCode.AssetClass.hidden_from_search:1 AND NOT name:Favorites");
        boolean done = false;
        if(precomputedLuceneQuery!=null) {
            try {
                //convertTextQueries(precomputedLuceneQuery);
                if(precomputedLuceneQuery.getClass().getName().endsWith("SubjectBooleanQuery")) {
                    bq1 = (BooleanQuery) precomputedLuceneQuery;
                } else {
                    bq1.add(precomputedLuceneQuery,BooleanClause.Occur.MUST);
                }
                done = true;
            } catch (Exception e) {
                e.printStackTrace();
                messages.add("Expansion trouble: " + e);
            }
        }
        if(!done && searchI2G!=null && queries!=null && queries.size()>0) {
            // use plugin for expansion
            try {
                List<String> langs = Arrays.asList(xwiki.getLanguagePreference(),"en","x-all");
                UserQuery uq = new UserQuery(queries.get(0),UserQuery.Level.LITTLE,langs);
                QueryExpansionResult results =searchI2G.expandUserQuery(uq);
                messages.addAll(results.getMessages());
                expandedQuery = (Query) results.getQuery();
                bq1.add(expandedQuery,BooleanClause.Occur.MUST);
                done=true;
            } catch (Exception e) {
                e.printStackTrace();
                messages.add("Expansion trouble: " + e);
            }
        }
        if(!done) {
            // simple text matches
            if(queries==null) queries = new ArrayList<String>();
            BooleanQuery bq2 = new BooleanQuery();
            for(String q: queries) {
                Query q1 = parserStemmed.parse(q);
                bq2.add(q1,BooleanClause.Occur.SHOULD);
                q1 = parserKeys.parse(q); // non-stemmed more boosted
                q1.setBoost(1.2f);
                bq2.add(q1,BooleanClause.Occur.SHOULD);
            }
            bq1.add(bq2,BooleanClause.Occur.MUST);
        }

        // add a preference for higher quality
        if(!bq1.getClass().getName().endsWith("SubjectBooleanQuery"))
            bq1.add(prepareQualityQuery(),BooleanClause.Occur.SHOULD);

        // extraQuery
        if(extraQuery!=null) {
            Query eq = parserKeys.parse(extraQuery);
            log.debug("Adding extraQuery: " + eq + " of type " + eq.getClass());
            bq1.add(eq,BooleanClause.Occur.MUST);
        }
        return bq1;
    }


    static final String[] weights= new String[] {"0.8","0.9","1.0","1.1","1.2"};
    private Query prepareQualityQuery() {
        BooleanQuery bq = new BooleanQuery();
        for(String weight: weights) {
            Query q = new TermQuery(new Term(IndexFields.I2GEO_REVIEW_INDEX_WEIGHT,weight));
            q.setBoost(Float.parseFloat(weight));
            bq.add(q,BooleanClause.Occur.SHOULD);
        }
        return bq;
    }


    private Query convertTextQueries(Query query) {
        if(query instanceof BooleanQuery) {
            for(BooleanClause clause:(List<BooleanClause>)((BooleanQuery)query).clauses()) {
                clause.setQuery(convertTextQueries(clause.getQuery()));
            }
            return query;
        } else if (query instanceof TermQuery) {
            TermQuery tq = (TermQuery) query;
            String field = tq.getTerm().field();
            String text = tq.getTerm().text();
            if(field.startsWith("text-") || field.startsWith("title-")) {
                TermQuery r = new TermQuery(new Term("ft",text));
                TermQuery t = new TermQuery(new Term("ft.stemmed",text));
                BooleanQuery bq = new BooleanQuery();
                // TODO: do the stemming only now and not earlier!
                bq.add(r, BooleanClause.Occur.SHOULD);
                bq.add(t, BooleanClause.Occur.SHOULD);
                bq.setBoost(tq.getBoost());
                return bq;
            }
            
        } else if (query instanceof FuzzyQuery) {
            return query;
        } else if (query instanceof PrefixQuery) {
            return query;
        }
        return query;
    }

    public void doQuery() {
        Query query = null;
        try {
            query = assembleQuery();
            LucenePluginApi pluginApi = (LucenePluginApi) xwiki.getPlugin("lucene");
            Hits hits = pluginApi.getLuceneHits(query,sortFields);
            this.hits = hits;
            num = start;
            end = Math.min(start+limit,hits.length());
            makeSureUsageLogIsInitted();
            if(query.getClass().getName().endsWith("SubjectQuery"))
                USAGE_LOG.info("Searched for terms \u00ab"+ queries+"\u00bb yielding a big query; from " + num + " to " + end +
                    " and obtained" + hits.length() + " results.");
            else
                USAGE_LOG.info("Searched for terms \u00ab"+ queries+"\u00bb yielding query \u00ab" + query + "\u00bb from " + num + " to " + end +
                    " and obtained" + hits.length() + " results.");
        } catch (Exception e) {
            e.printStackTrace();
            USAGE_LOG.info("Searched for query \u00ab" + query + "\u00bb from " + num + " to " + end +
                    "\n and obtained " + (hits!=null ? (hits.length()+ " results.") : "no hits."));
            hits = null;
            messages.add("Trouble at invoking query " + e.toString());
        }
    }

    public int getResultCount() {
        return hits.length();
    }

    public int getModifiedCount() {
        int v = getResultCount();
        if(v> end) v = end;
        return v;
    }

    public int getEndCount() {
        int v = getResultCount();
        if(v> end) v = end;
        return v;
    }
    public int getStartCount() {
        return start;
    }
    public int getLimit() {
        return limit;
    }
    public Query getLuceneQuery() {
        return precomputedLuceneQuery;
    }

    public Object next() {
        try {
            if(hits==null) return null;
            if(num>= end) return null;
            currentDoc = hits.doc(num);
            currentAsset = null;
            num++;            
            if(log.isDebugEnabled()) log.debug("next: " + currentDoc.get("fullname"));
            return currentDoc;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("can't fetch next doc.",e);
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public boolean hasNext() {
        return hits!=null && hits.length()>0 && num < end;
    }

    public String readTitle() {
        try {
            String tit = currentDoc.get("title");
            if(tit==null) tit = currentDoc.get("title");
            if(tit==null) tit = currentDoc.get("name");
            if(tit==null) tit = msg.get("caption.untitled");
            return filterForJSON(tit);
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    public Map<String,String> getIctValues() {
        try {
            String[] icts = currentDoc.getValues("CurrikiCode.AssetClass.instructional_component.key");
            HashMap<String,String> r = new HashMap<String,String>(3);

            if(icts==null || icts.length==0) {
                r.put("ictIcon","");
                r.put("ictRaw","");
                r.put("ictText","");
            } else if (icts.length>1){
                r.put("ictIcon",xwiki.getSkinFile("icons/ICTIcon-Multiple.gif"));
                r.put("ictRaw","multiple");
                r.put("ictText",filterForJSON(msg.get("search.selector.ict.multiple")));
            } else {
                // icts.length==1
                String ict = icts[0];
                r.put("ictRaw",filterForJSON(ict));
                String icon = ict.substring(0,1).toUpperCase() + ict.substring(1);
                icon = xwiki.getSkinFile("icons/ICTIcon-"+icon+".gif");
                r.put("ictIcon",filterForJSON(icon));
                r.put("ictText",filterForJSON(msg.get("search.selector.ict."+ict.replaceFirst("_", "."))));
}
            return r;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String,String> m = new HashMap<String,String>();
            m.put("ictText","error");
            m.put("ictIcon","");
            m.put("ictRaw","error");
            return m;
        }
    }

    public String getCRSReviewStatus() {
        try {
            String status = currentDoc.get("CRS.CurrikiReviewStatusClass.status");
            return filterForJSON(status);
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    public String getAssetType() {
        String type = currentDoc.get("assetType");
        if(type == null) type = "assetType.unknown"; // ????
        return type;
    }

    public String getDescription() {
        if(currentDoc==null) return "error";
        String desc = currentDoc.get("CurrikiCode.AssetClass.description");
        if(desc==null) return "";
        desc = desc.replaceAll("[\\n|\\r]+", "<br />"); // !!!! this is really bad style I feel: everything html, no story! (it may be a security issue)
        // the highlighter should be used one day
        return filterForJSON(desc);
    }

    public Query getExpandedQuery() {
        return expandedQuery;
    }

    public org.apache.lucene.document.Document getLuceneDocument() {
        return currentDoc;
    }

    public String getShortTokenizedDescription() {
        String r = currentDoc.get("CurrikiCode.AssetClass.description");
        if(r==null) return "";
        StringBuffer buff = new StringBuffer(Math.min(100,r.length()));
        for(int i=0, l=r.length(); i<l; i++) {
            char c = r.charAt(i);
            if(Character.isWhitespace(c)) {
                buff.append(" ");
            } else if(Character.isLetterOrDigit(c))
                buff.append(c);
            if(buff.length()>=100) {
                buff.append("...");break;
            }
        }
        return buff.toString();
    }

    public String getExpandedInfo() throws XWikiException {
        try {
            if(currentDoc==null) return "error";
            boolean isComposite = false;
            String[] objClasses = currentDoc.getValues("object");
            for(String objClass:objClasses) if("CurrikiCode.AssetClass".equals(objClass)) {isComposite = true; break;}
            if(isComposite) {
                String hql = ", BaseObject as composite, BaseObject as subasset, StringProperty as cprops, StringProperty as sprops " +
                    "where doc.name != 'Favorites' and doc.name != 'WebHome' and composite.name=doc.fullName " +
                    "and composite.className='CurrikiCode.CompositeAssetClass' and composite.id=cprops.id.id " +
                    "and cprops.id.name='type' and (cprops.value='collection' or cprops.value='curriki_document') " +
                    "and subasset.name=doc.fullName and subasset.id=sprops.id.id and " +
                    "subasset.className='CurrikiCode.SubAssetClass' and sprops.id.name='assetpage' " +
                    "and sprops.value='"+ currentDoc.get("fullname") +"' order by doc.name";
                List<String> parentList = xwiki.searchDocuments(hql);
                if(parentList.size()>0) {
                    StringBuffer infoB = new StringBuffer(msg.get("search.resource.resource.expanded.title")).append("<ul>");
                    for(String parentPage : parentList) {
                        Document doc = xwiki.getDocument(parentPage);
                        com.xpn.xwiki.api.Object assetObj = doc.getObject("CurrikiCode.AssetClass"),
                            compositeAssetObj = doc.getObject("CurrikiCode.CompositeAssetClass");
                        // TODO: could lucenize here to walk the included assets
                        String title= (String) assetObj.get("title");
                        if(title ==null || title.length()==0)
                            title = msg.get("caption.untitled");
                        Property descProp = assetObj.getProperty("description");
                        String description = (descProp!=null && descProp.getValue()!=null) ?
                                descProp.getValue().toString() : "";
                        String url = xwiki.getURL(parentPage);
                        String parentType= compositeAssetObj!=null ? compositeAssetObj.getProperty("type").toString() : "";
                        String parentTypeClass= "collection".equals(parentType) ?
                                "resource-CollectionComposite" : "resource-FolderComposite";
                        infoB.append("<li class=\"").append(parentTypeClass).append("\">").
                            append("<a href=\"").append(url).append("\" ext:qtitle=\"Description\" ext:qtip=\"").
                                append(description).append("\">").append(title).append("</a></li>");
                    }
                    infoB.append("</ul>");
                    return filterForJSON(infoB.toString().replaceAll("[\n|\r]+", "<br />"));
                }
            }
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    public String getFullName() {
        return currentDoc!=null ? currentDoc.get("fullname") : null; 
    }

    public String getCreator() {
        if(currentDoc==null) return null;
        String r = currentDoc.get("author");
        return r;
    }
    public String getContributor() {
        String rightsHolder = currentDoc.get("CurrikiCode.AssetClass.rightsHolder");
        if(rightsHolder==null)
            rightsHolder = currentDoc.get("creator");
        if(rightsHolder==null)
            rightsHolder = currentDoc.get("author");
        return filterForJSON(rightsHolder);
    }

    public String getCreatorName() {
        try {
            if(currentDoc==null) return null;
            String authorName = xwiki.getUserName(getCreator(),false);
            return  filterForJSON(authorName);
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    public String getRightsHolder() {
        return currentDoc.get("CurrikiCode.AssetLicenseClass.rightsHolder");
    }

    public boolean isOwnResource() {
        String creatorName = xwiki.getUserName(getCreator(),false);
        String copyrightHolderName = getRightsHolder();
        if(creatorName == null || copyrightHolderName==null) return false;
        return copyrightHolderName.equals(creatorName) ||
                copyrightHolderName.equals(creatorName + " (as found on i2geo.net)");
    }

    public String getDate() {
        try {
            if(currentDoc==null) return null;
            return filterForJSON(getCurrentAsset().getDate().toString());
            //return filterForJSON(currentDoc.get("date"));
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    public Asset getCurrentDocsAsset() {
        try {
            return getCurrentAsset();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private Asset getCurrentAsset() throws XWikiException {
        if(currentAsset==null)
            currentAsset = ((CurrikiPluginApi)xwiki.get("curriki")).fetchAsset(
                currentDoc.get("fullname"));
        return currentAsset;
    }


    public String getLastModifiedShortDate() {
        String shortDate = currentDoc.get("date");
        if(shortDate==null || shortDate.length()!=12)
            return shortDate;
        return shortDate.substring(0,4) + "-" + shortDate.substring(4,6) + "-" + shortDate.substring(6,8) + " " + shortDate.substring(8,10) + ":" + shortDate.substring(10,12);
    }


    public float getScore() {
        try {
            if(num-1<0 || num>=hits.length()) return 0;
            else return hits.score(num-1);
        } catch(Exception ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    public String getHtmlTableOfScore() {
        StringBuffer buff = new StringBuffer();
        buff.append("<table cellspacing='2' border='0'><tr>");
        int score = (int) (getScore()*10);
        for(int i=0; i<score; i++) {
            buff.append("<td style='background-color:#");
            char c; int reverse = 15-i;
            if(reverse<10) c= (char)('0'+reverse);
            else c = (char) ('A' -10+reverse);
            for(int k=0; k<6; k++) buff.append(c);
            buff.append("; width:1em;'>&nbsp;</td> ");
        }
        buff.append("</tr></table>");
        return buff.toString();
    }

    public String getEarlierStart() {
        if(start==0) return null;
        int earlier= Math.max(0,start-limit);
        return ""+earlier;
    }

    public String getLaterStart() {
        int later = start + limit;
        if(later>hits.length()) return null;
        return ""+later;
    }
    

    public String filterForJSON(String s) {
        if(s==null) return "";
        return s.replaceAll("'", "&#39;");
    }

    // ================== LOGGING ======================================
    private static Log USAGE_LOG = null;
    private static FileAppender appender;
    private static void makeSureUsageLogIsInitted() {
        if(USAGE_LOG!=null) return;
        try {
            Logger logger = Logger.getLogger("ResourcesSearchCursor-Usage");
            logger.setLevel(Level.DEBUG);
            USAGE_LOG = new org.apache.commons.logging.impl.Log4JLogger(logger);
            Layout layout = new PatternLayout("%d %-5p %c{2} %x - %m\\n");
            appender = new FileAppender(layout,"logs/searchi2g-usage.log",true);
            logger.addAppender(appender);
            appender.setThreshold(Priority.DEBUG);
        } catch (Throwable x) {
            x.printStackTrace();
            if(x instanceof ThreadDeath) throw (ThreadDeath) x;
        }
    }

    public void finalize() throws Throwable {
        try {
            appender.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.finalize();
    }


}
