import com.xpn.xwiki.doc.*;
import com.xpn.xwiki.objects.*;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.Context;
import com.xpn.xwiki.api.Property;
import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.rightsmanager.RightsManagerException;
import org.curriki.xwiki.plugin.asset.Constants;
import com.xpn.xwiki.web.XWikiMessageTool;

import java.util.Date;

/** This class is responsible to convert a trace object (http://i2geo.net/xwiki/bin/view/Traces/)
 to a an asset keeping as much as possible of the information. */
public class ConvertTrace {

  public ConvertTrace() {}

  public void setTraceDoc(Document doc) {
    this.traceDoc = doc;
    this.traceObj = traceDoc.getObject("XWiki.TraceClass");}
  public void setUserName(String userName) {
    this.userName = userName;
    if(userName.startsWith("XWiki."))
      this.userName = userName.substring("XWiki.".length());
  }

  private Document traceDoc;
  private String userName;
  private com.xpn.xwiki.api.Object traceObj;
  private Document document;
  private XWikiMessageTool msg;

  public void setMsg(XWikiMessageTool msg) { this.msg = msg; }

  public Document doConversion(XWiki xwiki, Context context) throws Throwable {
    if(xwiki.getUser().toString().indexOf("XWikiGuest")>=0)
      throw new RightsManagerException(300,"Please login first.");
    // compute document name
    traceObj = traceDoc.getObject("XWiki.TraceClass");
    if(traceObj==null) return null;
    String title = (String) safeGetValue(traceObj.getProperty("Title"));
    if(title==null || title.length()==0) title = "-no-title-";
    String tamedName = computeAppropriateName(xwiki, title);
    String language = safeGetValue(traceObj.getProperty("language"));
    if(language==null || language.length()==0) language = "en";

    // create document
    assert( xwiki.copyDocument("Tracelang.TraceAssetTemplate",tamedName,language));
    // that object should contain:
    // an XWiki.AssetClass, an XWiki.ExternalAssetClass, an XWiki.AssetLicenseClass
    //CurrikiPluginApi currikiA = (CurrikiPluginApi) xwiki.getPlugin("curriki");
    //Asset asset = currikiA.createAsset(null);
    //currikiA.createAsset()
    // xwiki.getDocument(tamedName);
    System.out.println("Document's name " + tamedName);
    System.out.println("document is  of class " + xwiki.getDocument(tamedName).getClass() );
    System.out.println("Title is " + title);
    System.out.println("Language is " + language);
    document = xwiki.getDocument(tamedName);
    XWikiDocument baseDoc = document.getDocument();
    //new XWikiDocument("",tamedName);
    baseDoc.setParent("Traces.WebHome");
    baseDoc.setFullName(tamedName);
    //baseDoc.setLanguage(language);
    baseDoc.setTitle(title);
    baseDoc.setContent("#includeForm(\"XWiki.AssetTemplate\")\n## really  do");
    baseDoc.setContentDirty(true);
    baseDoc.removeObjects("XWiki.XWikiComments");
    //baseDoc.setCustomClass("org.curriki.xwiki.plugin.asset.external.ExternalAsset");
    //xwiki.getXWiki().saveDocument(baseDoc,context.getContext());
    XWikiContext xc = context.getContext();

    BaseObject assetObj = baseDoc.getObject(Constants.ASSET_CLASS);

    assetObj.set("title",title,xc);
    String link = safeGetValue(traceObj.getProperty("url"));

    //com.xpn.xwiki.api.Object externalAssetObj = document.getObject(Constants.EXTERNAL_ASSET_CLASS);
    BaseObject externalAssetO = baseDoc.getObject(Constants.EXTERNAL_ASSET_CLASS);
    externalAssetO.set(Constants.EXTERNAL_ASSET_LINK, link,xc);

    assetObj.set(Constants.ASSET_CLASS_KEYWORDS, "|" + safeGetValue(traceObj.getProperty("theme"))+ "|",xc);
    assetObj.set(Constants.ASSET_CLASS_INSTRUCTIONAL_COMPONENT,"activity_lab",xc);
    assetObj.set(Constants.ASSET_CLASS_LANGUAGE, language,xc);

    StringBuffer description = new StringBuffer(safeGetValue(traceObj.getProperty("shortDescription")));

    // -- description
    // TODO: internationalization
    description.append("\n\n");
    description.append("*").append(msg.get('this-resource-copied-from-trace')).append("*\n\n");
    String amount = safeGetValue(traceObj.getProperty("amount"));
    description.append(msg.get('coming-from-trace-which-mentioned-x-traces',
            [traceDoc.getFullName(),amount]))
            .append("\n\n");
    // coming-from-trace-which-mentionned-x-traces = Coming from trace [{0}] which mentionned {1} constructions
    //description.append("Coming from trace [").append(traceDoc.getFullName()).append("] which mentionned ");
    //description.append(traceObj.getProperty("amount").getValue()).append(" constructions.<br/>");
    description.append("- ");
    description.append(msg.get('traces.fields.dgSystem')).append(": [Softwares." )
            .append(safeGetValue(traceObj.getProperty("dgSystem"))).append("].\n");
    description.append("- ");
    description.append(msg.get('traces.fields.Country')).append(": ").append(
            safeGetValue(traceObj.getProperty("LicCountry"))).append("\n");
    description.append("- ");
    description.append(msg.get('traces.fields.level')).append(": ").append(
            safeGetValue(traceObj.getProperty("eduLevel"))).append("\n");
    description.append("- ");
    description.append(msg.get('traces.fields.Topic')).append(" ").append(
            safeGetValue(traceObj.getProperty("topic"))).append("\n");
    // TODO: map main topics
    description.append("- ");
    description.append(msg.get('traces.fields.OtherThemes')).append(
            safeGetValue(traceObj.getProperty("theme"))).append("\n");
    assetObj.set(Constants.ASSET_CLASS_DESCRIPTION,
            //xwiki.renderText(description.toString(),document)
            description.toString()
            ,xc);

    assetObj.set("trainedTopicsAndCompetencies",
            computeTopics(safeGetValue(traceObj.getProperty("topic"))),xc);

    assetObj.set("status","final",xc);
    assetObj.set("fcstatus","None",xc);
    //assetObj.set("fcreviewer",xwiki.getUser().toString(),xc);
    //assetObj.set("fcdate",new Date(),xc);

    // -- rights
    applyRights("public",baseDoc,xc);


    // -- license
    BaseObject licenseObj = baseDoc.getObject("XWiki.AssetLicenseClass");
    String licenseString = traceObj.getProperty("license") != null ?
      safeGetValue(traceObj.getProperty("license")) : "cc-by-sa";
    licenseObj.set("licenseType2",licenseString,xc);
    //licenseObj.set("jurisdiction",
    //        traceObj.getProperty("LicCountry").getValue().toString());
    // TODO: create country field
    licenseObj.set("rightsHolder",
            safeGetValue(traceObj.getProperty("CopyrightHolder")),xc);


    // ---- save and done
    //baseDoc.saveAllAttachments(xc);
    //document.save();
    baseDoc.setCustomClass("org.curriki.xwiki.plugin.asset.Asset");
    xwiki.getXWiki().saveDocument(baseDoc,xc);
    return document;

  }


  private void applyRights(String right, XWikiDocument doc, XWikiContext context) throws XWikiException {
    doc.removeObjects("XWiki.XWikiRights");

    BaseObject assetObj = doc.getObject(Constants.ASSET_CLASS);
    String rights;

    if (right == null) {
      // Use existing rights value
      rights = assetObj.getStringValue(Constants.ASSET_CLASS_RIGHT);
    } else {
      rights = right;
      assetObj.setStringValue(Constants.ASSET_CLASS_RIGHT, right);
    }

    // Make sure rights value is valid, default to PUBLIC if not
    if (rights == null
            || !(rights.equals(Constants.ASSET_CLASS_RIGHT_PUBLIC)
            || rights.equals(Constants.ASSET_CLASS_RIGHT_MEMBERS)
            || rights.equals(Constants.ASSET_CLASS_RIGHT_PRIVATE))) {
      rights = Constants.ASSET_CLASS_RIGHT_PUBLIC;
      assetObj.setStringValue(Constants.ASSET_CLASS_RIGHT, rights);
    }

    // Rights
    BaseObject rightObj;

    // If collection is user
    String usergroupfield = Constants.RIGHTS_CLASS_USER;
    String uservalue =  ("".equals(doc.getCreator())) ? context.getUser() : doc.getCreator();
    String usergroupvalue = uservalue;

    // If collection is group
    if (doc.getSpace().startsWith(Constants.GROUP_COLLECTION_SPACE_PREFIX)) {
      usergroupfield = Constants.RIGHTS_CLASS_GROUP;
      usergroupvalue = doc.getSpace().substring(5) + ".MemberGroup";
    }

    // Always let the admin group have edit access
    rightObj = doc.newObject(Constants.RIGHTS_CLASS, context);
    rightObj.setLargeStringValue(Constants.RIGHTS_CLASS_GROUP, Constants.RIGHTS_ADMIN_GROUP);
    rightObj.setStringValue("levels", "edit");
    rightObj.setIntValue("allow", 1);

    // Always let the creator/group edit
    rightObj = doc.newObject(Constants.RIGHTS_CLASS, context);
    // CURRIKI-2468 - allow creator to always edit/view their creations
    if (usergroupfield.equals(Constants.RIGHTS_CLASS_GROUP)) {
      rightObj.setLargeStringValue(Constants.RIGHTS_CLASS_USER, uservalue);
    }
    rightObj.setLargeStringValue(usergroupfield, usergroupvalue);
    rightObj.setStringValue("levels", "edit");
    rightObj.setIntValue("allow", 1);

    if (rights.equals(Constants.ASSET_CLASS_RIGHT_PUBLIC)) {
      // Viewable by all and any member can edit
      rightObj = doc.newObject(Constants.RIGHTS_CLASS, context);
      rightObj.setLargeStringValue(Constants.RIGHTS_CLASS_GROUP, Constants.RIGHTS_ALL_GROUP);
      rightObj.setStringValue("levels", "edit");
      rightObj.setIntValue("allow", 1);
    } else if (rights.equals(Constants.ASSET_CLASS_RIGHT_MEMBERS)) {
      // Viewable by all, only user/group can edit
    } else {
      // rights == private, so only allow creator/group to view and edit
      rightObj = doc.newObject(Constants.RIGHTS_CLASS, context);
      // CURRIKI-2468 - allow creator to always edit/view their creations
      if (usergroupfield.equals(Constants.RIGHTS_CLASS_GROUP)) {
        rightObj.setLargeStringValue(Constants.RIGHTS_CLASS_USER, uservalue);
      }
      rightObj.setLargeStringValue(usergroupfield, usergroupvalue);
      rightObj.setStringValue("levels", "view");
      rightObj.setIntValue("allow", 1);
    }

  }

  public String computeAppropriateName(XWiki xwiki, String title) throws Throwable {
    StringBuffer tamedTitle = new StringBuffer();
    title = java.text.Normalizer.normalize(title, java.text.Normalizer.Form.NFD);
    boolean nextShouldCapitalize = true;
    int i=-1;
    while(++i<title.length()) {
      char c=title.charAt(i);
      if(c==' ')
      { nextShouldCapitalize = true; continue; }
      if(c>='A' && c<='Z' || c>='a' && c<='z') {
        // a tamed letter use it
        if(nextShouldCapitalize) tamedTitle.append(Character.toUpperCase(c));
        else tamedTitle.append(c);
      }
      nextShouldCapitalize = false;
    }
    if(tamedTitle.length()==0) tamedTitle.append("missingTitle");
    String goodName = "Coll_" + userName+ "." + tamedTitle;
    String candidateName = goodName;
    Document tdoc = xwiki.getDocument(goodName);
    i=2;
    while(xwiki.exists(candidateName)) {
      candidateName = goodName + i;
      i++;
      System.out.println("Trying " + candidateName + " tdoc is " + tdoc);
    }
    return candidateName;
  }

  public String getErrors() {
    return null;
  }

  public String getCreatedAssetName() {
    return null;
  }

  public String safeGetValue(Property prop) {
    if(prop==null) return "";
    if(prop.getValue()==null) return "";
    return prop.getValue().toString();
  }

  public static String computeTopics(String traceTopic) {
    String i2gTopic = null;
    // have 29 traces topics.
    if(false){
    } else if(traceTopic.equals("Algebra")) {
      i2gTopic ="#Algebraic_calculation_r,#Number_r,#SymbolicExpression_r";
    } else if(traceTopic.equals("Angles")) {
      i2gTopic ="#Angle_r";
    } else if(traceTopic.equals("Arcs and circles")) {
      i2gTopic ="#Arc_r,#Circle_r";
    } else if(traceTopic.equals("Areas")) {
      i2gTopic ="#Area_r";
    } else if(traceTopic.equals("Arts")) {
      i2gTopic ="#Unknown_r";
    } else if(traceTopic.equals("Barycenter")) {
      i2gTopic ="#Barycenter_r";
    } else if(traceTopic.equals("Calculus")) {
      i2gTopic ="#Function_attributes_r,#Function_r,#Number_r";
    } else if(traceTopic.equals("Lengths")) {
      i2gTopic ="#Length_r,#Length_unit_r";
    } else if(traceTopic.equals("Lines")) {
      i2gTopic ="#Straight_Line_r";
    } else if(traceTopic.equals("Locus")) {
      i2gTopic ="#Locus_r";
    } else if(traceTopic.equals("missing")) {
      i2gTopic ="#Unknown_r";
    } else if(traceTopic.equals("Geometry (other topics)")) {
      i2gTopic ="#Unknown_r";
    } else if(traceTopic.equals("Points")) {
      i2gTopic ="#Point_r";
    } else if(traceTopic.equals("Polygons")) {
      i2gTopic ="#Polygon_r";
    } else if(traceTopic.equals("Probability")) {
      i2gTopic ="#Probability_r";
    } else if(traceTopic.equals("proportions")) {
      i2gTopic ="#Proportional_r";
    } else if(traceTopic.equals("Quadrilaterals")) {
      i2gTopic ="#Quadrilateral_r";
    } else if(traceTopic.equals("Sections")) {
      i2gTopic ="#Section_r";
    } else if(traceTopic.equals("Solids")) {
      i2gTopic ="#Solidobjects_r";
    } else if(traceTopic.equals("Stat")) {
      i2gTopic ="#Data_analysis_r";
    } else if(traceTopic.equals("3D coordinates")) {
      i2gTopic ="#ThreeD_coordinates_r";
    } else if(traceTopic.equals("3D-Plots")) {
      i2gTopic ="#Graphical_Representation_of_Function_r,#Graphical_display_of_data_r,#ThreeDObject_r";
    } else if(traceTopic.equals("Transformations")) {
      i2gTopic ="#Transformation_r";
    } else if(traceTopic.equals("triangle-equalities")) {
      i2gTopic ="#Reflection_r,#Rotation_r,#Translation_r,#Triangle_r";
    } else if(traceTopic.equals("Triangles")) {
      i2gTopic ="#Triangle_r";
    } else if(traceTopic.equals("Trigonometry")) {
      i2gTopic ="#Trigonometric_function_r,#Trigonometric_ratio_r";
    } else if(traceTopic.equals("2D coordinates")) {
      i2gTopic ="#Cartesian_coordinates_r";
    } else if(traceTopic.equals("2D-Plots")) {
      i2gTopic ="#Graphical_Representation_of_Function_r,#Graphical_display_of_data_r";
    } else if(traceTopic.equals("")) {
      i2gTopic ="#Vector_r";
    }

    if(i2gTopic==null) i2gTopic = "";
    return i2gTopic;
  }

  public String getEscapedFullDocName() {
    return URLEncoder.encode(document.getFullName());
  }


} // class ConvertTrace