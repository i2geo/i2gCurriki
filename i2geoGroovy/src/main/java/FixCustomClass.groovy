

<%
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.api.Document;

private XWikiDocument bdoc = null;
private Document doc = null;
private String docFullName = null;

  if(request.getParameter("doc")) {
    doc = xwiki.getDocument(request.getParameter("doc"));
    bdoc=doc.getDocument();
    println("1 Custom Class Fix")
    println("Have fetched: " + doc +" and bdoc=" +bdoc);
    println("before: doc.getCustomClass() = " + bdoc.getCustomClass());
    println("");
    println("...operating change...");

    bdoc.setCustomClass("org.curriki.xwiki.plugin.asset.external.ExternalAsset");
    xwiki.getXWiki().saveDocument(bdoc,context.getContext());
    println("change saved.");
    bdoc = null; doc = null;
    println("");
    println("After that: ");
    println("getCustomClass() gives: " +
            xwiki.getDocument(request.getParameter("doc")).getDocument().getCustomClass());
  }
    else { println("Please provide parameter doc.")
  }

%>