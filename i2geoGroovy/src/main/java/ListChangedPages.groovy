// <%
import java.text.DateFormat
import java.text.SimpleDateFormat;
/** Simple class to browse through the list of all documents and
 * print the document name if modified since a given date
 */
//public HttpServletRequest request;
//public XWiki xwiki;

String since = request.getParameter("since");
if(since == null || since.length()==0) {
  println("Please indicate since when you wish to see changes.");
  println("Now quitting.");
  return;
}

DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

Date date = df.parse(since);
String hql = "where doc.date>? order by doc.web asc, doc.name asc";
System.out.println("hqling " + hql);
List params = new ArrayList(); params.add(date);
List docs = xwiki.searchDocuments(hql,params);
String baseURL = xwiki.getXWiki().Param('curriki.system.hostname','http://i2geo.net/')

for(doc in docs) {
  d = xwiki.getDocument(doc);
  println("- "+ df.format(d.date) + ": [" + d.fullName + " > " + baseURL+"/xwiki/bin/view/"+d.web + "/" + d.name + "?viewer=code&showlinenumbers=0]" );
}

// %>