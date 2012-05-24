<% import com.xpn.xwiki.api.XWiki
import com.xpn.xwiki.api.User;
import com.xpn.xwiki.api.Document
import com.xpn.xwiki.api.Property

String userName=xwiki.getUser().getUser();
String title= request.getParameter("title");
if(title==null || title.length()==0) title="Missing-suggestion-title";
hql = " where doc.web ='Suggestions' and doc.author='"+userName+"'";
System.out.println("CreateSuggestion will query " + hql);
List docs= xwiki.searchDocuments(hql);

if(userName.startsWith("XWiki.")) {
  userName = userName.substring("XWiki.".length());
}

String candidate = null;
for(int i in docs.size()..6000){
  candidate = "Suggestions." + userName + "-" + i;
  if(!xwiki.exists(candidate)) break;
  candidate = null;
}

if(candidate==null)
  throw new IllegalStateException("Sorry no space left to file a new suggestion");


StringBuffer url = new StringBuffer((String) xwiki.getURL(candidate,"edit"));
url.append("?parent=Suggestions.WebHome&webname=Suggestions&name=").append(candidate);
url.append("&title=" + URLEncoder.encode(title,"utf-8"));
url.append("&language=" + context.language);
if(request.getParameter("content")!=null) {
  url.append("&content=");
  url.append(URLEncoder.encode(request.getParameter("content"),"utf-8"));
}
url.append("#edit");
response.sendRedirect(url.toString());


%>
