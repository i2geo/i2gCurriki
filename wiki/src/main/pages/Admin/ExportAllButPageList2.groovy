


1 XML Export of the Wiki that are not in Admin/PageList
<%
import com.xpn.xwiki.*;
import com.xpn.xwiki.api.*;
import com.xpn.xwiki.doc.*;
import com.xpn.xwiki.plugin.packaging.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

def pagelist = "Admin.PageList"
def exportdir = "/export/home/intergeo/platform/dataExport/nonApplicationFiles";
  //"/opt/local/activemath/users/ilo/intergeo/platform2/dataExport/nonApplicationFiles";
if (request.pagelist != null){
  pagelist = request.pagelist
}
def max = 100000;

Log LOG = LogFactory.getLog("groovypages." + doc.web + "." +doc.name);

LOG.info("Confirm ? " + request.confirm);
if(request.doc!=null && xwiki.getUser().isUserInGroup("XWiki.AdminGroup")) {
  org.dom4j.Document xml = xwiki.getDocument(request.getParameter("doc")).getDocument()
          .toXMLDocument(true,false,true,true,context.getContext());
  OutputStream out = response.getOutputStream();
  new org.dom4j.io.XMLWriter(out).writeNode((org.dom4j.Document) xml);
  out.flush(); out.close();
}
else if (request.confirm=="1") {
  println "Starting export"
  int n = 0;
  LOG.info("Starting export for max " + max + " docs.");
  // TODO: println "Delete existing files"

  //pack.setDescription("Export of Curriki Wiki-based Application Code");
  //pack.setBackupPack(true);
  //pack.setPreserveVersion(true);
  //pack.setWithVersions(false)
  //  pack.setWithVersions(true)
  //  pack.addDocumentFilter(filter)
  XWikiContext newContext = context.context.clone();
  def pages = context.context.getWiki().getDocument(pagelist, newContext).content
  def list = pages.split("(\r|\n)+")
  def excludes = new java.util.HashSet(java.util.Arrays.asList(list));
  def i = 0;
  for(String s in xwiki.getSpaces()) {
    //if(!"QR".equals(s)) continue;
    File spaceDir = new File(new File(exportdir),s);
    spaceDir.mkdirs();
    println("1.1.1 " + s)
    for(String p in xwiki.getSpaceDocsName(s)) {
      def page = s + "." + p;
      if(excludes.contains(page)) continue;
      println("- [" + page + "]");
      LOG.info("Exporting " + page);
      File file = new File(spaceDir,p);
      n++;
      if(n>max) break;
      if(n % 100 == 0) {
         LOG.info("--- flushing gc.");
         //xwiki.flushCache() 
         Runtime.getRuntime().gc()
      }
      try {
        XWikiDocument d = xwiki.getDocument(page).getDocument();
        org.dom4j.Document xd = d.toXMLDocument(true,false,true,true,newContext);
        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        new org.dom4j.io.XMLWriter(out).writeNode(xd);
        out.flush(); out.close();
      } catch (Exception e){
        println "Error exporting"
        println e.getMessage()
        e.printStackTrace();
      }

    }
  }

  println "Finished."
  LOG.info("Finished.");
} else {
  println("add confirm parameter please.");
  println "[Confirm export>"+doc.name+"?pagelist="+pagelist+"&confirm=1]"
}
%>