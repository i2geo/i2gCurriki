import java.net.URLEncoder;

/**
 */
public class CreateDocumentByUrl {

    public static void main(String[] args) throws Exception {
        CreateDocumentByUrl c = new CreateDocumentByUrl("http://draft.i2geo.net/xwiki/","edit","Suggestions","testSuggestion","en",
                "un titré avec des âcceñts","un contenu avec des caractères étranges \n(par exemple boulette• et degré °. et quelques caractères mathématiques λ π ∈ ↦)");
                //new CreateDocumentByUrl(args[0],args[1],args[2],args[3],args[4],args[5],args[6]);
        System.out.println(c.getEncodedUrl());
    }

    String encoding = "utf-8";
    public CreateDocumentByUrl(String base, String action, String space, String name, String language,
                               String title, String content) throws Exception {
        StringBuffer b = new StringBuffer(base);
        b.append("bin/").append(action).append('/').append(space).append('/').append(name).append('?');
        b.append("language=").append(language);
        b.append("&title=").append(URLEncoder.encode(title,encoding));
        b.append("&content=").append(URLEncoder.encode(content,encoding));
        encodedUrl = b.toString();
    }

    private String encodedUrl;

    public String getEncodedUrl() {
        return encodedUrl;
    }
}
