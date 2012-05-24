package com.xpn.xwiki.plugin.lucene;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Document;
import org.curriki.xwiki.plugin.asset.Asset;
import org.curriki.xwiki.plugin.curriki.CurrikiPluginApi;
import org.curriki.xwiki.plugin.lucene.ResourcesSearchCursor;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import net.i2geo.xwiki.SearchI2GXWikiApi;
import net.i2geo.xwiki.SearchI2GXWiki;
import net.i2geo.xwiki.SearchI2GXWikiPlugin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 */
public class I2GResourceData extends ObjectData {

    private static final Log LOG = LogFactory.getLog(I2GResourceData.class);

    public I2GResourceData(final XWikiDocument doc, final XWikiContext context, LuceneIndexProfile profile)
    {
        super(doc, context, profile);
        setAuthor(doc.getAuthor());
        setCreator(doc.getCreator());
        setModificationDate(doc.getDate());
        setCreationDate(doc.getCreationDate());
    }

    @Override
    public String getFullText(XWikiDocument doc, XWikiContext context)
    {
        return super.getFullText(doc,context);
    }

    @Override
    public void addDataToLuceneDocument(org.apache.lucene.document.Document luceneDoc, XWikiDocument doc,
        XWikiContext context)
    {
        super.addDataToLuceneDocument(luceneDoc,doc, context);
        if(doc.getObject("CurrikiCode.AssetClass")!=null) {
            try {
                // add collection names where our item is in
                String hql = "select doc.name from XWikiDocument doc,  BaseObject obj, " +
                        "StringProperty prop  where  doc.fullName=obj.name and " +
                        "obj.className='CurrikiCode.SubAssetClass' and obj.id=prop.id.id  " +
                        "and prop.name='assetpage' and   prop.value='"+doc.getFullName()
                        +"'";
                List<String> collectionPageNames = (List<String>) context.getWiki().search(hql,context);
                for(String collName: collectionPageNames) {
                    luceneDoc.add(new Field("incollection",collName, Field.Store.YES, Field.Index.UN_TOKENIZED));
                }

                // add asset type
                Asset asset = ((CurrikiPluginApi)(context.getWiki().getPluginApi("curriki",context)))
                        .fetchAsset(doc.getFullName());
                luceneDoc.add(new Field("assetType",asset.getAssetType(), Field.Store.YES, Field.Index.UN_TOKENIZED));

            } catch (XWikiException e) {
                e.printStackTrace();
            }
        }
    }




}
