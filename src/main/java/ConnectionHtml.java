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
import java.util.Date;
import java.util.List;

public class ConnectionHtml implements Runnable {
    private final String baseURL;

    private final String imagesDirectory;

    private final List<Product> DOWNLOADED_PRODUCTS;

    private final ProductsDao dao = new ProductsDao();

    private final HttpClient client;

    private final String BASE_URL = "http://books.toscrape.com/";

    public ConnectionHtml(String baseURL, String imagesDirectory) {
        this.baseURL = baseURL;
        this.client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
        this.imagesDirectory = imagesDirectory;
        DOWNLOADED_PRODUCTS = dao.getAll();
    }

    @Override
    public void run() {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(new URI(baseURL)).GET();

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
            String imgLink = getImageLinkFromHtml(e);

            Product product = new Product();

            Elements ele = e.getElementsByTag("h3");
            String name = ele.get(0).text();
            product.setName(name);

            ele = e.getElementsByClass("product_price");
            String price = getPriceFromHtml(ele);
            product.setPrice(price.substring(1));
            product.setPath(imgLink.substring(3));

            product.setDateAdded(getDate());
            if (DOWNLOADED_PRODUCTS.contains(product)) {
                dao.update(product);
                Product prevProd = getProductFromDB(product);

                assert prevProd != null;
                if (prevProd.getPrice().equals(product.getPrice()))
                    logPriceUpdate(prevProd, product);

                DOWNLOADED_PRODUCTS.remove(product);
                continue;
            }

            dao.insert(product);
            System.out.println(product.getName() + " " + product.getPrice());

            String linkToDownload = BASE_URL + imgLink.substring(3);
            System.out.println(product);
            downloadPhoto(linkToDownload, product.getId());
        }

//        for (Product p : DOWNLOADED_PRODUCTS) {
//            System.out.println("Product is no longer available: " + p.getName());
//            dao.delete(p);
//        }
    }

    private void logPriceUpdate(Product prevProd, Product newProd) {
        System.out.println("name: " + prevProd.getName());
        System.out.println("prev price: " + prevProd.getPrice());
        System.out.println("new price: " + newProd.getPrice());
    }

    private Product getProductFromDB(Product product) {
        for (Product p : DOWNLOADED_PRODUCTS) {
            if (p.equals(product))
                return p;
        }

        return null;
    }


    private java.sql.Date getDate() {
        java.util.Date date = new Date();
        return new java.sql.Date(date.getTime());
    }

    private String getPriceFromHtml(Elements elements) {
        return elements.get(0).getElementsByTag("p").get(0).text();
    }

    private String getImageLinkFromHtml(Element e) {
        Elements elements = e.getElementsByTag("img");
        return elements.get(0).attr("src");
    }

    private void downloadPhoto(String link, int id) throws MalformedURLException {
        URL url = new URL(link);
        String name = id + ".jpg";
        try (InputStream in = new BufferedInputStream(url.openStream());
             ByteArrayOutputStream out = new ByteArrayOutputStream();
             FileOutputStream fos = new FileOutputStream(imagesDirectory + name)) {
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
