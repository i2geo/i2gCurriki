import org.curriki.xwiki.plugin.asset.Asset
import com.xpn.xwiki.api.Document
import com.xpn.xwiki.web.XWikiMessageTool


/** Set of utilities to render a review.
 */
public class ReviewToolGroovy {

    String assetName = null;
    Asset asset = null;
    Document reviewDoc = null;
    com.xpn.xwiki.api.Object reviewObj = null;
    XWikiMessageTool msg;
    StringWriter stringOut = new StringWriter();
    PrintWriter out = new PrintWriter(stringOut);
    private boolean inForm = true;

    public void setAsset(Asset a) {
        asset = a;
        assetName = a.fullName;
    }



    public void setReviewDoc(Document doc) {
        reviewDoc = doc;
        System.out.println("Review doc \"" + doc + "\".");
        reviewObj = doc.getObject("QF.ReviewClass");
        if(reviewObj==null) {
            reviewObj = doc.newObject("QF.ReviewClass");
            System.out.println("Review Object created.");
        } else System.out.println("Review Object found.");
    }

    public void setMsg(XWikiMessageTool m) {
        msg = m;
    }

    public void print(String s) {
        this.out.print(s);
    }
    public void println(String s) {
        this.out.println(s);
    }

    public void renderRating(String propName, String sectionName) {
        String objValue = ""+reviewObj?.getProperty(propName)?.value?.toString();
        for(value in 1..6) { out.print("<!--" + value + "-->");
            String valueS = ""+value;
            /* model:
     <input name="QF.ReviewClass_0_metadata" value="1"
             onclick="clearInput('questions_metadata')"
         #if($action=="view")disabled="disabled" #end#if($review.getProperty("metadata").value=="1")checked="checked"#end type="radio"> */
            out.print("<input type=\"radio\"name=\"QF.ReviewClass_0_");
            out.print(propName);
            out.print("\" value=\"");
            out.print(valueS); out.print("\"");
            if(sectionName==null)
            {out.print("\n onclick=\"zapSectionAnswers('sectionHead_"); out.print(propName); out.print("','questions_"); out.print(propName); out.print("')\" ");}
            else
            {out.print("\n onclick=\"computeMean('sectionHead_"); out.print(sectionName); out.print("','questions_"+sectionName+"')\" ");}
            // TODO: that's a section question, or?
            if(!inForm) out.print("disabled=\"disabled\" ");
            if(objValue.equals(valueS)) out.print("checked=\"checked\" ");
            out.print("/>");
        }
        out.print("<span style='display:none'>")
        if(objValue=="")
            out.print("<input type=\"radio\" value=\"\" name='QF.ReviewClass_0_"+propName+"' checked=\"checked\"/>");
        else
            out.print("<input type=\"radio\" value=\"\" name='QF.ReviewClass_0_"+propName+"' />");
        out.println("</span>");
    }

    public List listSectionNames() {
        return Arrays.asList(msg.get("QF.qs").split(",| "));
    }

    public List listQuestionNames(String sectionName) {
        return Arrays.asList(msg.get("QF.qs_"+sectionName).split(',| '))
    }

    void setAction(String actionName) {
        if('view'.equals(actionName)) inForm = false;
        else if('edit'.equals(actionName) || 'inline'.equals(actionName)) inForm = true;
        else {
            System.out.println("Bizarre action name \"" + actionName + "\", defaulting to view");
            inForm = false;
        }
    }



    public void renderQuestion(String qName, String sectionName, boolean expanded) {
        out.println("");
        char propNamePrefix = 'i'; if(sectionName!=null) propNamePrefix = 'q';
        out.println('<tr style="background: rgb(246, 248, 201); width:100% " onmouseover="flyingByCell(this)" onmouseout="flyingAwayCell(this)">'); // none repeat scroll 0% 0%; -moz-background-clip: -moz-initial; -moz-background-origin: -moz-initial; -moz-background-inline-policy: -moz-initial;
        out.print('<!-- question: '); out.print(qName);
        if(sectionName==null) { out.print(" (section head)"); }
        if(reviewObj!=null) {
            if(reviewObj.getProperty(qName))
            {out.print(", value: "); out.print(""+reviewObj.getProperty(qName).value);}
        }
        out.println(" -->");

        //<td class="expanded" width="18" height="13" onclick="toggleFold('questions_math',this)">
        if(sectionName!=null) {
            out.print('<td width="18" height="13">&nbsp;</td>'); // onclick="toggleFold(\'questions_'+qName+'\',this)">
        } else {
            out.print('<td style="text-align: left;" height="13" width="18" ');
            out.print('class="'+(expanded?'expanded':'collapsed')+'" onclick="toggleFold(\'questions_'+qName+'\',this)"');
            out.println('> &nbsp;<br></td>');
        }
        out.println('<td style="text-align: left; width:8em" nowrap="noWrap">');
        renderRating(qName, sectionName);
        out.println('</td>');
        out.println('<td style="text-align: left; width:80%"'); // width:90%
        if(sectionName==null)
        {out.print('onclick="toggleFold(\'questions_'); out.print(qName); out.print('\',getFirstTDOf(this.parentNode));"')}
        out.print(' title="');out.print(msg.get("QF."+propNamePrefix+"tp_"+qName));out.println('" ');
        out.print('>'); out.print(msg.get("QF."+propNamePrefix+"tx_" + qName)); out.println('</td>');
        out.println('</tr>');
        out.println("<!-- ======================================================== -->");
    }


    public String renderReviewSections(String sec) {
        stringOut.getBuffer().delete(0,stringOut.getBuffer().length());
            if(sec==null || sec.length()==0) return "";
            out.println("<!-- ReviewToolGroovy version 34 -->")
            out.println('<div class="main_metadata" style="border-style: solid; border-width: 1pt; padding:4pt; background:rgb(246, 248, 201);">');
            out.println('<table width="100%" id="sectionHead_'+sec+'" cellspacing="0" style="font-weight: bolder;">');
            // check if there's a value that's defined, if yes... display block otherwise display none
            String display = 'none';
            for(String q in listQuestionNames(sec)) {
              String prop = sec + "_" + q;
              if(reviewObj.getProperty(prop)!=null && reviewObj.getProperty(prop).value.toString().length()>0) {
                display = 'block'; break;
              }
            }
            renderQuestion(sec,null,'none'!=display);
            out.println("</table>");
            out.println("");
            out.println('<table id="questions_'+sec+'" width="100%" cellspacing="0" style="display: '+display+';">');
            for(String q in listQuestionNames(sec)) {
                if(q==null || q.length()==0) continue;
                renderQuestion(sec + '_' + q,sec,false);
            }
            out.println("<tr><td colspan='3' style='width:100%; background:rgb(246, 248, 201)'><p>"+msg.get('QF.reviews-comments')+":</p>");
            if(inForm) {
                out.print('<input id="QF.ReviewClass_0_'+sec+'_comment" style="width:95%" type="text" name="QF.ReviewClass_0_'+sec+'_comment"  value="');
                if(null != reviewObj.getProperty(sec + "_comment")) out.print(escapeForXml(reviewObj.getProperty(sec+"_comment").value.toString()));
                out.println('"/>');
            } else {
                if(null != reviewObj.getProperty(sec + "_comment")) out.print(escapeForXml(reviewObj.getProperty(sec+"_comment").value.toString()));
            }
            out.print('</td></tr>');
            out.println('</table></div>');
        return stringOut.toString();
    }

    public void close() {
        stringOut = null; out = null;
    }


    String escapeForXml(String v) {
      StringBuffer b = new StringBuffer(v.length());
      for(char c in v) {
          if(c=='&') b.append('&amp;');
          else if (c=='\'') b.append('&apos;')
          else if (c=='"') b.append("&quot;")
          else if (c=='<') b.append("&lt;");
          else if (c=='>') b.append(">");
          else b.append(c);
      }
      return b.toString();
    }


    String renderJSassignmentSectionHeadNames() {
        StringBuffer buff = new StringBuffer();
        buff.append("<script type='text/javascript'>\n");
        buff.append("window.sectionHeadNames = [");
        boolean first = true;
        for(x in listSectionNames()) {
            if(x=="") continue;
            if(!first) buff.append(',');
            buff.append('"sectionHead_').append(x).append('"');
            first = false;
        }
        buff.append("];\n");
        buff.append("</script>");
        return buff.toString();
    }
}