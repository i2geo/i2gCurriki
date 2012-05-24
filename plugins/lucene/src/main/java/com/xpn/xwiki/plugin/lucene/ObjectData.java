/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.plugin.lucene;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Field;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.*;

/**
 * Hold the property values of the XWiki.ArticleClass Objects.
 */
public class ObjectData extends IndexData
{
    private static final Log LOG = LogFactory.getLog(ObjectData.class);

    public ObjectData(final XWikiDocument doc, final XWikiContext context, final LuceneIndexProfile profile)
    {
        super(doc, context, profile);
        setAuthor(doc.getAuthor());
        setCreator(doc.getCreator());
        setModificationDate(doc.getDate());
        setCreationDate(doc.getCreationDate());
    }

    /**
     * @see com.xpn.xwiki.plugin.lucene.IndexData#getType()
     */
    @Override
    public String getType()
    {
        return LucenePlugin.DOCTYPE_OBJECTS;
    }

    @Override
    public String getId()
    {
        return new StringBuffer(super.getId()).append(".objects").toString();
    }

    /**
     * @return a string containing the result of {@link IndexData#getFullText(XWikiDocument,XWikiContext)}plus the full
     *         text content (values of title,category,content and extract ) XWiki.ArticleClass Object, as far as it
     *         could be extracted.
     */
    @Override
    public String getFullText(XWikiDocument doc, XWikiContext context)
    {
        StringBuffer retval = new StringBuffer(super.getFullText(doc, context));
        String contentText = getContentAsText(doc, context);
        if (contentText != null) {
            retval.append(" ").append(contentText).toString();
        }
        return retval.toString();
    }

    /**
     * @return string containing value of title,category,content and extract of XWiki.ArticleClass
     */
    protected String getContentAsText(XWikiDocument doc, XWikiContext context)
    {
        StringBuffer contentText = new StringBuffer();
        try {
            LOG.info(doc.getFullName());
            for (String className : doc.getxWikiObjects().keySet()) {
                for (BaseObject obj : doc.getObjects(className)) {
                    extractContent(contentText, doc, obj, context);
                }
            }
        } catch (Exception e) {
            LOG.error("error getting content from  XWiki Objects ", e);
            e.printStackTrace();
        }
        return contentText.toString();
    }

    protected void extractContent(StringBuffer contentText, XWikiDocument doc, BaseObject baseObject, XWikiContext context)
    {
        try {
            if (baseObject != null) {
                Object[] propertyNames = baseObject.getPropertyNames();
                for (int i = 0; i < propertyNames.length; i++) {
                    BaseProperty baseProperty = (BaseProperty) baseObject.getField((String) propertyNames[i]);
                    if ((baseProperty != null) && (baseProperty.getValue() != null)) {
                        PropertyInterface prop = baseObject.getxWikiClass(context).getField((String) propertyNames[i]);
                        if (!(prop instanceof PasswordClass)) {
                            String text = baseProperty.getValue().toString();
                            if(prop instanceof TextAreaClass) {
                                try {
                                    text = new XWiki(context.getWiki(),context).renderText(text,new Document(doc,context));
                                    text = TextExtractor.getText(("<html><body><div>" + text + "</div></body></html>").getBytes("utf-8"),"text/html");
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                }
                            }
                            contentText.append(text);
                            if(LOG.isDebugEnabled()) LOG.debug("text: " + text);
                        }
                    }
                    contentText.append(" ");
                }
            }
        } catch (Throwable e) {
            LOG.error("error getting content from  XWiki Object ", e);
            e.printStackTrace();
            if(e instanceof ThreadDeath) throw (ThreadDeath) e;
        }
    }

    @Override
    public void addDataToLuceneDocument(org.apache.lucene.document.Document luceneDoc, XWikiDocument doc,
        XWikiContext context)
    {
        // let the basic be inserted
        super.addDataToLuceneDocument(luceneDoc, doc, context);

        // add all object properties
        for (String className : doc.getxWikiObjects().keySet()) {
            for (BaseObject obj : doc.getObjects(className)) {
                if (obj != null) {
                    luceneDoc.add(new Field(IndexFields.OBJECT, obj.getClassName(),
                             Field.Store.YES,
                        Field.Index.TOKENIZED));
                    Object[] propertyNames = obj.getPropertyNames();
                    for (int i = 0; i < propertyNames.length; i++) {
                        try {
                            indexProperty(luceneDoc, obj, doc, (String) propertyNames[i], context);
                        } catch (Exception e) {
                            LOG.error("error extracting fulltext for document " + this, e);
                        }
                    }
                }
            }
        }

        // allow the profile to add extra fields
        try {
            super.getLuceneIndexProfile().addExtraFields(doc,context,luceneDoc);
        } catch (Exception e) { e.printStackTrace(); }

    }

    private Field.Store guessStore(XWikiDocument doc, String fieldName, XWikiContext context) {
        if(shouldStore(doc,fieldName,context)) return Field.Store.YES;
        return Field.Store.NO;
    }

    private Field.Index guessIndex(XWikiDocument doc, String fieldName, XWikiContext context) {
        if(shouldIndex(doc,fieldName,context)) {
            if(shouldTokenize(doc,fieldName,context)) return Field.Index.ANALYZED;
            else return Field.Index.NOT_ANALYZED;
        } else return Field.Index.NO;
    }

    protected boolean shouldStore(XWikiDocument doc, String fieldName, XWikiContext context) {
        return getLuceneIndexProfile().shouldStore(doc,fieldName,context);
    }
    protected int shouldTrim(XWikiDocument doc, String fieldName, XWikiContext context) {
        return getLuceneIndexProfile().shouldTrim(doc,fieldName,context);
    }
    protected boolean shouldTokenize(XWikiDocument doc, String fieldName, XWikiContext context) {
        return getLuceneIndexProfile().shouldTokenize(doc,fieldName,context);
    }

    protected boolean shouldIndex(XWikiDocument doc, String fieldName, XWikiContext context) {
        return getLuceneIndexProfile().shouldIndex(doc,fieldName,context);
    }

    protected void indexProperty(org.apache.lucene.document.Document luceneDoc, BaseObject baseObject,
                                   XWikiDocument doc,
        String propertyName, XWikiContext context)
    {
        String fieldFullName = baseObject.getClassName() + "." + propertyName;
        BaseClass bClass = baseObject.getxWikiClass(context);
        PropertyInterface prop = bClass.getField(propertyName);
        if(LOG.isTraceEnabled()) LOG.trace("Property : " + propertyName);
        if (prop instanceof PasswordClass) {
            // Do not index passwords
        } else if (prop instanceof StaticListClass && ((StaticListClass) prop).isMultiSelect()) {
            indexStaticList(luceneDoc, baseObject, (StaticListClass) prop, propertyName, context);
        } else {
            final String text = getContentAsText(baseObject, propertyName, context);
            if (text != null) {
                int trimAmount = shouldTrim(doc,fieldFullName,context);
                Field.Store store = guessStore(doc,fieldFullName,context);
                Field.Index index = guessIndex(doc,fieldFullName,context);
                if(store==Field.Store.YES || index!=Field.Index.NO)
                    luceneDoc.add(new Field(fieldFullName, trimAmount >0 ? text.substring(0,Math.min(trimAmount,text.length())): text,
                        store, index));
                luceneDoc.add(new Field(fieldFullName + IndexFields.UNTOKENIZED, text, Field.Store.NO, Field.Index.NOT_ANALYZED));
                luceneDoc.add(new Field(fieldFullName + IndexFields.STEMMED, text, Field.Store.NO, Field.Index.ANALYZED));
            }
        }
    }

    protected void indexStaticList(org.apache.lucene.document.Document luceneDoc, BaseObject baseObject,
        StaticListClass prop, String propertyName, XWikiContext context)
    {
        Map possibleValues = prop.getMap(context);
        List keys = baseObject.getListValue(propertyName);
        String fieldFullName = baseObject.getClassName() + "." + propertyName;
        Iterator it = keys.iterator();
        while (it.hasNext()) {
            String value = (String) it.next();
            ListItem item = (ListItem) possibleValues.get(value);
            if (item != null) {
                // we index the key of the list
                String fieldName = fieldFullName + ".key";
                luceneDoc.add(new Field(fieldName, item.getId(), Field.Store.YES, Field.Index.TOKENIZED));
                luceneDoc.add(new Field(fieldName + IndexFields.UNTOKENIZED, item.getId(), Field.Store.NO, Field.Index.UN_TOKENIZED));

                // we index the value
                fieldName = fieldFullName + ".value";
                luceneDoc.add(new Field(fieldName, item.getValue(), Field.Store.YES, Field.Index.TOKENIZED));
                luceneDoc.add(new Field(fieldName + IndexFields.UNTOKENIZED, item.getValue(), Field.Store.NO, Field.Index.UN_TOKENIZED));
                if (!item.getId().equals(item.getValue())) {
                    luceneDoc.add(new Field(fieldFullName, item.getValue(), Field.Store.YES, Field.Index.TOKENIZED));
                    luceneDoc.add(new Field(fieldFullName + IndexFields.UNTOKENIZED, item.getValue(), Field.Store.NO, Field.Index.UN_TOKENIZED));
                }
            }

            // we index both if value is not equal to the id(key)
            luceneDoc.add(new Field(fieldFullName, value, Field.Store.YES, Field.Index.TOKENIZED));
            luceneDoc.add(new Field(fieldFullName + IndexFields.UNTOKENIZED, value, Field.Store.NO, Field.Index.UN_TOKENIZED));
        }
    }

    public String getFullText(XWikiDocument doc, BaseObject baseObject, String property, XWikiContext context)
    {
        return getContentAsText(baseObject, property, context);
    }

    private String getContentAsText(BaseObject baseObject, String property, XWikiContext context)
    {
        StringBuffer contentText = new StringBuffer();
        try {
            BaseProperty baseProperty;
            baseProperty = (BaseProperty) baseObject.getField(property);
            if (baseProperty.getValue() != null) {
                PropertyInterface prop = baseObject.getxWikiClass(context).getField(property);
                if (!(prop instanceof PasswordClass)) {
                    contentText.append(baseProperty.getValue().toString());
                }
            }
        } catch (Exception e) {
            LOG.error("error getting content from  XWiki Objects ", e);
            e.printStackTrace();
        }
        return contentText.toString();
    }
}
