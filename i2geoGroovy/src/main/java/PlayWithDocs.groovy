import org.curriki.xwiki.plugin.asset.Asset
import org.curriki.xwiki.plugin.curriki.CurrikiPluginApi
import org.curriki.xwiki.plugin.asset.Constants
import org.curriki.xwiki.plugin.asset.external.ExternalAsset
import com.xpn.xwiki.api.XWiki

/**
 */

public class PlayWithDocs {

  public ExternalAsset ea;
  public String docName;


  public void play(XWiki xwiki) {
    Asset asset = ((CurrikiPluginApi) xwiki.getPlugin("curriki")).createAsset("Traces.WebHome");
    //document = xwiki.xwiki.getDocument(tamedName);
    //XWikiDocument baseDoc = document.getDocument();
    //new XWikiDocument("",tamedName);



    asset.setTitle("a title");
    asset.setCustomClass(ExternalAsset.class.getName());
    asset.save();
    String name = asset.getFullName();

    ea = (ExternalAsset) xwiki.getDocument(name);
    System.out.println("External Asset : " + ea + " of class " + ea.getClass());
    ea.setLink("http://www.activemath.org/");
    ea.applyRightsPolicy(Constants.ASSET_CLASS_RIGHT_PUBLIC); 
    //((CurrikiPluginApi) xwiki.getPlugin("curriki")).
    docName = ea.getFullName();




  }


}