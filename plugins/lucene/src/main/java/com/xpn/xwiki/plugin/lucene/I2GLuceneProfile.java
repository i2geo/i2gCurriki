package com.xpn.xwiki.plugin.lucene;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import net.i2geo.xwiki.SearchI2GXWiki;
import net.i2geo.xwiki.SearchI2GXWikiPlugin;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.curriki.xwiki.plugin.asset.Asset;
import org.curriki.xwiki.plugin.asset.composite.CompositeAsset;

import java.util.*;

public class I2GLuceneProfile implements LuceneIndexProfile {


    private static Set<String> storedFields =
            new HashSet<String>(Arrays.asList(
                    "CurrikiCode.AssetClass.title",
                   "CurrikiCode.AssetClass.instructional_component",
                   "CRS.CurrikiReviewStatusClass.status",
                   "CurrikiCode.AssetClass.description",
                   "CurrikiCode.AssetClass.rightsHolder",
                    "CurrikiCode.AssetClass.trainedTopicsAndCompetencies",
                    "CurrikiCode.AssetClass.eduLevelFine",
                    "CurrikiCode.AttachmentAssetClass.file_type",
                    "assetType",
                    "CurrikiCode.AssetLicenseClass.rightsHolder",
                    "collections",
                    "VignettesCode.VignetteClass.heightBig",
                    "VignettesCode.VignetteClass.widthBig",
                    "VignettesCode.VignetteClass.heightSmall",
                    "VignettesCode.VignetteClass.widthSmall",
                    "VignettesCode.VignetteClass.heightTiny",
                    "VignettesCode.VignetteClass.widthTiny",
                   "i2geo.reviewOverallRanking","object",
                    "XWiki.ArticleClass.extract"
            ));

    private static Set<String> tokenizedFields =
            new HashSet<String>(Arrays.asList(
                    "CurrikiCode.AssetClass.title",
                   "CurrikiCode.AssetClass.description",
                   "CurrikiCode.AssetLicenseClass.rightsHolder"));

    private static Set<String> identifierFields=
            new HashSet<String>(Arrays.asList(
                    "CurrikiCode.AssetClass.eduLevelFine","CurrikiCode.AssetClass.trainedTopicsAndCompetencies",
                          "fullname", "name", "web", "object","type","author","creator",
                          "CurrikiCode.AssetClass.instructional_component.key",
                          "CurrikiCode.AssetLicenseClass.licenseType","_docid"
            ));

    private static Set<String> nonIndexedFields_Substring =
            new HashSet<String>(Arrays.asList("VignettesCode","XWikiRights"));

    public ObjectData createObjectData(XWikiDocument doc, XWikiContext context) {
        if(doc.getObject("CurrikiCode.AssetClass")!=null)
            return new I2GResourceData(doc,context,this);
        return new ObjectData(doc,context,this);
    }


    public void addExtraFields(XWikiDocument doc, XWikiContext context, Document luceneDoc) {
        // now add parent topics
        try {
            addEnclosingTopicsToLuceneDoc(luceneDoc,doc,context);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // now add quality review summary
        try {
            addReviewSummaries(luceneDoc,doc,context);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void addEnclosingTopicsToLuceneDoc(org.apache.lucene.document.Document luceneDoc, XWikiDocument doc,
        XWikiContext context)
    {
        SearchI2GXWiki searchI2G = ((SearchI2GXWikiPlugin) context.getWiki().getPlugin("searchi2g",context)).getImplementation();
        try {
            for(Field f: luceneDoc.getFields("CurrikiCode.AssetClass.trainedTopicsAndCompetencies")) {
                try {
                    HashSet<String> ancestors = new HashSet<String>();
                    for(String g: f.stringValue().split(",")) {
                        ancestors.add(g);
                        for(String pUri : searchI2G.getNodeParents(g,null)) {
                            ancestors.add(pUri);
                        }
                    }
                    for(String u:ancestors) {
                        if(! u.startsWith("#") && !u.startsWith("http://")) u = "#" + u;
                        luceneDoc.add(new Field("i2geo.ancestorTopics",u,
                                Field.Store.YES, Field.Index.UN_TOKENIZED));
                    }
                } catch (Exception e) {
                    System.err.println("Can't index parent topics of property " + f.stringValue());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("Sorry, searchI2G plugin cannot be obtained or be used.");
            e.printStackTrace();
        }
    }

    private void addReviewSummaries(org.apache.lucene.document.Document luceneDoc, XWikiDocument doc,
        XWikiContext context)
    {
        try {
            String hql = "select doc.name from XWikiDocument doc,  BaseObject obj, " +
                    "StringProperty prop  where  doc.fullName=obj.name and " +
                    "obj.className='QF.ReviewSummaryClass' and obj.id=prop.id.id  " +
                    "and prop.name='resource' and   prop.value='"+doc.getFullName()
                    +"'";
            List<Object> qrNames = context.getWiki().search(hql,context);
            for(Object qrNameO : qrNames) {
                try {
                    String qrName = (String) qrNameO;
                    XWikiDocument qrDoc = context.getWiki().getDocument("QFSummaries." + qrName,context);
                    BaseObject qrObj = qrDoc.getObject("QF.ReviewSummaryClass");
                    if(qrObj!=null) {
                        int qualitySummary = qrObj.getIntValue("summaryRank");
                        float indexWeight = qrObj.getFloatValue("indexWeight");
                        if(indexWeight==0) indexWeight = 1;
                        luceneDoc.add(new Field(IndexFields.I2GEO_REVIEW_OVERALL_RANKING,qualitySummary+"", Field.Store.YES,Field.Index.UN_TOKENIZED));
                        luceneDoc.add(new Field(IndexFields.I2GEO_REVIEW_INDEX_WEIGHT,indexWeight+"", Field.Store.YES,Field.Index.UN_TOKENIZED));
                    }
                } catch (XWikiException e) {
                    e.printStackTrace();
                }
            }
        } catch (XWikiException e) {
            e.printStackTrace();
        }

    }

    public I2GLuceneProfile() {
        super();
        if(firstInstance ==null) firstInstance = this;
    }

    private static I2GLuceneProfile firstInstance = null;

    public static I2GLuceneProfile getInstance() {
        if(firstInstance==null)
            new I2GLuceneProfile();
        return firstInstance;
    }


    public List<String> shouldAlsoIndexOtherDocs(XWikiDocument doc,XWikiContext context) {
        // TODO: add: index resource when a review is made (currently is done separately)
        try {
            Asset asset = Asset.fetchAsset(doc.getFullName(),context);
            if(asset.isCollection()) {
                CompositeAsset ca = (CompositeAsset) asset;
                return ca.getSubassetList();
            }
        } catch (XWikiException e) {
            e.printStackTrace();
        }
        return (List<String>) Collections.EMPTY_LIST;        
    }

    public boolean isDocumentDesireable(XWikiDocument doc, XWikiContext context) {
        if("AssetTemp".equals(doc.getWeb()) ||
                "CurrikiCode".equals(doc.getWeb()) ||
                doc.getWeb().contains("Template")) return false;

        /* BaseObject obj = doc.getObject("CurrikiCode.Asset");
        if(obj!=null){
            PropertyInterface interf = ;
            ((BooleanClass)
                    obj.get("CurrikiCode.AssetClass.hidden_from_search")).
        }*/
        return true;
    }

    public boolean shouldStore(XWikiDocument doc, String fieldName, XWikiContext context) {
        return storedFields.contains(fieldName);
    }

    public int shouldTrim(XWikiDocument doc, String fieldName, XWikiContext context) {
        return fieldName.endsWith("description") ? 100 : -1;
    }

    public boolean shouldTokenize(XWikiDocument doc, String fieldName, XWikiContext context) {
        return fieldName.endsWith(".stemmed") || tokenizedFields.contains(fieldName);
    }

    public boolean isIdentifierField(String fieldName) {
        return fieldName.endsWith(".untokenized") || identifierFields.contains(fieldName);
    }

    public boolean shouldIndex(XWikiDocument doc, String fieldName, XWikiContext context) {
        for(String fld:nonIndexedFields_Substring) {
            if(fieldName.contains(fld)) return false;
        }
        return true;
    }
}
