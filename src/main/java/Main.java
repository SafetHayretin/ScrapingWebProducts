import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static String BASE_URL = "http://books.toscrape.com/catalogue/";

    // link | directory to save photos
    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(5);
        String link = args[0];
        while (link != null) {
            ConnectionHtml con = new ConnectionHtml(link, args[1]);
            pool.execute(con);
            link = getLink(link);
        }
    }

    public static String getLink(String baseURL) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(new URI(baseURL)).GET();
            HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
            HttpRequest request = builder.build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            Document doc = Jsoup.parse(response.body(), response.uri().toString());

            Elements elements = doc.getElementsByClass("next");
            String href;
            if (elements.size() > 0) {
                href = getHref(elements);
                return BASE_URL + href;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getHref(Elements elements) {
        return elements.get(0).getElementsByTag("a").get(0).attr("href");

    }
}
