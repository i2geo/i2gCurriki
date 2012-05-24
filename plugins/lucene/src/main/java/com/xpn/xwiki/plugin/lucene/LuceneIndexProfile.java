package com.xpn.xwiki.plugin.lucene;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import org.apache.lucene.document.Document;

import java.util.List;

/** Interface to describe the strategies of indexing of the Lucene plugin.
 */
public interface LuceneIndexProfile {


    /**
     * Called by the index-updater to add other documents to the index-queue when
     * this document is indexed (e.g. the original document when a document ranking
     * it is modified). (not used yet)
     * @param context where are we evaluating
     * @param doc the document that needs indexing
     * @return document names that also need to be indexed.
     * */
    public List<String> shouldAlsoIndexOtherDocs(XWikiDocument doc,XWikiContext context);

    /**
     * Called by the index-updater to make sure that thisk document should be
     * updated. For example, temporary documents should not be...
     * @param doc the candidate document
     * @param context where are we evaluating
     * @return whether that document should be indexed.
     */
    public boolean isDocumentDesireable(XWikiDocument doc,XWikiContext context);


    /**
     * called by the index-updater's {@link ObjectData} to check if the given field's
     * value should be stored. Storing a field value is only useful if you use the field
     * in search-results display, it is then good practice and fast and avoids to load the document
     * from the DB, but it does take memory (even if not used!). In doubt, return false.
     * Note that the index still indexes the value, and puts it in full-text if of type text,
     * independently of this value.
     *
     * @param doc the document containing the candidate field
     * @param fieldName the field name, typically [class-name].[property-name]
     * @param context where are evaluating
     * @return whether to store.
     */
    public boolean shouldStore(XWikiDocument doc, String fieldName, XWikiContext context);

    /**
     * Called by the index-updater's {@link ObjectData} to check if the given field
     * should be trimmed before being put to the index (indexed and stored). This is
     * practical for fields which one doesn't query and only reads from the storage
     * but need to be trimmed in result-presentation anyways.
     *
     * @param doc the document containing the candidate field
     * @param fieldName the field name, typically [class-name].[property-name]
     * @param context where are we evaluating
     * @return a strictly positive number if the text should be trimmed.
     */
    public int shouldTrim(XWikiDocument doc, String fieldName, XWikiContext context);

    /**
     * Called by the index-updater's {@link ObjectData} to check if the given field's
     * value should be run through the {@link org.apache.lucene.analysis.Analyzer} before
     * being put to the field of the given name and there queried and stored.
     *
     * Note that a stemmed and tokenized variant is indexed in the [fieldName].stemmed field
     * which is a good place to use for quering (provided the query terms are stemmed as well).
     *
     * @param doc the document containing the candidate field
     * @param fieldName the field name, typically [class-name].[property-name]
     * @param context where are we evaluating
     * @return whether it should be tokenized.
     */
    public boolean shouldTokenize(XWikiDocument doc, String fieldName, XWikiContext context);

    public boolean isIdentifierField(String fieldName);


    /**
     * Called by the index-updater's {@link ObjectData} to check if the given field's
     * value should be put into the index. Returning true will allow this value to be
     * searched, returning false will save memory.
     *
     * @param doc the document containing the candidate field
     * @param fieldName the field name, typically [class-name].[property-name]
     * @param context where are we evaluating
     * @return whether it should be tokenized.
     */
    public boolean shouldIndex(XWikiDocument doc, String fieldName, XWikiContext context);

    /**
     * Called by the index-update to create a subclass of {@link ObjectData} so as
     * to serve particular purposes (e.g. differentiate the analyzer per field-type)...
     *
     * @param doc the document whose data we are indexing
     * @param context where are we evaluating
     * @return the potential subclass
     */
    public ObjectData createObjectData(XWikiDocument doc, XWikiContext context);

    /**
     * Called by {@link ObjectData} after adding all the traditional fields so
     * as to complement the document with custom-created fields which can be
     * queriable and storable.
     * @param doc the document whose data we are indexing
     * @param context where are we evaluating
     * @param luceneDoc the document where to add extra fields
     * @return the potential subclass
     * */
    public void addExtraFields(XWikiDocument doc, XWikiContext context, Document luceneDoc);

}
