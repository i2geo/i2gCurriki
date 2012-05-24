// <%
import com.xpn.xwiki.doc.XWikiDocument
import com.xpn.xwiki.criteria.impl.RevisionCriteria
import com.xpn.xwiki.objects.BaseObject
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

println();

XWiki xwiki = xwiki.xWiki;
XWikiContext context = context.context;

XWikiDocument doc = xwiki.getDocument("Coll_bclerc.ThormedeThalsconnaissancesde4me",context)
String actor = "XWiki.adminPolx"


Set recipients = new HashSet();

// add all committers of that resource (includes commenters)
List revisions = doc.getRevisions(new RevisionCriteria(), context);
for(String version in revisions) {
  recipients.add(doc.getRevisionInfo(version,context).author);
}


// add all reviewers (only last author)
def hql = "select doc.fullName from XWikiDocument doc, BaseObject obj, StringProperty prop where doc.web = 'QR' and obj.name=doc.fullName and obj.className='QF.ReviewClass' and obj.id=prop.id.id and prop.name='resource' and prop.value='"+doc.fullName+"' ";
List docNames = xwiki.search(hql, 50, 0, context);
for(String docName in docNames) {
  String reviewAuthor = xwiki.getDocument(docName,context).author;
  recipients.add(reviewAuthor);
}


// add everyone having it in one of his collections (includes Favorites)
def hql2="select doc.fullName from XWikiDocument doc, BaseObject obj, StringProperty prop where obj.className = 'CurrikiCode.SubAssetClass' and prop.name='assetpage' and prop.value='"+doc.fullName+"' and  obj.id=prop.id.id and obj.name=doc.fullName "
List collNames = xwiki.search(hql2, 50, 0, context);
for(String collName in collNames) {
  //println "- collname " + collName
  if(collName!=null && collName.startsWith("Coll_") && collName.contains(".")) {
    String author = collName.substring("Coll_".length(),collName.indexOf('.'));
    owner = "XWiki."+ author;
    recipients.add(owner);
  }
}


// remove actor
recipients.remove(actor);

// convert list of recipients to email-list
List recipientsEmails = new ArrayList(recipients.size())
for(String u in recipients) {
  BaseObject obj = xwiki.getDocument(u,context).getObject("XWiki.XWikiUsers");
  String optOut = obj.getStringValue("opt_out"),
    options = obj.getStringValue("email_options")
  //println("- " + u + ": " + optOut + " " + options + " " + options.contains("reviews"))

  if("0".equals(optOut) ||
          "2".equals(optOut) && options!=null && options.contains("reviews")) {
    String authorEmail = xwiki.getUser(u,context).email;
    recipientsEmails.add(authorEmail);
  }
}


for(String r in recipientsEmails) println(" - "+r);

println ();
// for(String s in recipientsEmails) println "- recipient: " + s
// %>