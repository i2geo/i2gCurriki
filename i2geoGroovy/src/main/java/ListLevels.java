        import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.api.Document;

import java.util.List;
import java.util.Iterator;
        import java.util.Set;
        import java.util.TreeSet;


        public class ListLevels {


    public Set run(XWiki xwiki) throws Exception {

        String query =  ", BaseObject as obj where doc.web = 'Traces' and obj.name=doc.fullName ";

        List docs = xwiki.searchDocuments(query);
        println("We have " + docs.size() + " documents.");
        Set s = new TreeSet();

        Iterator it=docs.iterator();
        while(it.hasNext() ) {
            print("- ");
            Document doc = (Document) xwiki.getDocument((String)it.next());
            com.xpn.xwiki.api.Object traceObj= doc.getObject("XWiki.TraceClass");
            if(traceObj==null) {
                println("no trace at " + doc);
            } else {
                String level = traceObj.get("eduLevel").toString();
                println(level);
                s.add(level);
            }
        }
        return s;
    }
}

s = new ListLevels().run(xwiki);

for(Object l:s) {
        println("- " + l);
}