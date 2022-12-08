import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConnectionHtml implements Runnable {
    private final String link;

    private final String imagesDirectory;

    private final List<Product> downloadedProducts;

    private final ProductsDao dao = new ProductsDao();

    private final List<String> photoLinks = new ArrayList<>();

    private final HttpClient client;

    public ConnectionHtml(String link, String imagesDirectory) {
        this.link = link;
        this.client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
        this.imagesDirectory = imagesDirectory;
        downloadedProducts = dao.getAll();
        getAllLinksToPhotos();
    }

    @Override
    public void run() {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(new URI(link)).GET();

            HttpRequest request = builder.build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            Document doc = Jsoup.parse(response.body(), response.uri().toString());
            Elements elements = doc.getElementsByClass("col-xs-6 col-sm-4 col-md-3 col-lg-3");

            scrapeHtml(elements);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scrapeHtml(Elements elements) throws MalformedURLException {
        for (Element e : elements) {
            String imgLink = getImageLink(e);

            Product product = new Product();

            Elements ele = e.getElementsByTag("h3");
            String name = ele.get(0).text();
            product.setName(name);

            ele = e.getElementsByClass("product_price");
            String price = getPrice(ele);
            product.setPrice(price);
            product.setPath(imgLink);

            product.setDateAdded(getDate());
            if (photoLinks.contains(imgLink)) {
                dao.update(product);
                photoLinks.remove(imgLink);
                continue;
            }

            dao.insert(product);
            System.out.println(product.getName() + " " + product.getPrice());

            String linkToDownload = this.link + imgLink;
            downloadPhoto(linkToDownload, product.getId());
        }
    }


    private java.sql.Date getDate() {
        java.util.Date date = new Date();
        return new java.sql.Date(date.getTime());
    }

    private void getAllLinksToPhotos() {
        for (Product p : downloadedProducts) {
            photoLinks.add(p.getPath());
        }
    }

    private String getPrice(Elements elements) {
        return elements.get(0).getElementsByTag("p").get(0).text();
    }

    private String getImageLink(Element e) {
        Elements elements = e.getElementsByTag("img");
        return elements.get(0).attr("src");
    }

    private void downloadPhoto(String link, int id) throws MalformedURLException {
        URL url = new URL(link);
        String name = id + ".jpg";
        try (InputStream in = new BufferedInputStream(url.openStream());
             ByteArrayOutputStream out = new ByteArrayOutputStream();
             FileOutputStream fos = new FileOutputStream(imagesDirectory + name);) {
            byte[] buf = new byte[1024];
            for (int n; -1 != (n = in.read(buf)); ) {
                out.write(buf, 0, n);
            }
            byte[] response = out.toByteArray();
            fos.write(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
