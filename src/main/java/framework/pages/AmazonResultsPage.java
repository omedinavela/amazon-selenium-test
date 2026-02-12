package framework.pages;

import framework.models.Product;
import framework.utils.Waits;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class AmazonResultsPage {
    private final WebDriver driver;
    private final Waits waits;

    private final By sortDropdown = By.id("s-result-sort-select");
    private final By lowPrice = By.id("low-price");
    private final By highPrice = By.id("high-price");
    private final By goPriceBtn = By.cssSelector("input.a-button-input[type='submit']");

    //private final By searchResults = By.cssSelector("div[data-component-type='s-search-result']");
    //private final By searchResults = By.cssSelector("div.s-result-item[data-asin]:not([data-asin=''])");
    private final By searchResults = By.cssSelector("div[data-component-type='s-search-result'][data-asin]:not([data-asin=''])");
    //private final By searchResults = By.cssSelector("div.s-result-item[data-asin]:not([data-asin=''])");


    private final By titleInCard = By.cssSelector("h2 span");
    private final By priceInCard = By.cssSelector("span.a-price span.a-offscreen");
    private final By sponsoredInCard = By.xpath(".//span[normalize-space()='Patrocinado' or normalize-space()='Sponsored']");
    private final By resultsSummary = By.cssSelector("div.s-breadcrumb h2");
    private final By brandsRefinements = By.id("brandsRefinements");


    public AmazonResultsPage(WebDriver driver) {
        this.driver = driver;
        this.waits = new Waits(driver, 20);
    }

    public void filterBrand(String brand) {

        // Para esperar recarga real (mejor que url)
        WebElement firstCard = driver.findElements(searchResults).stream().findFirst().orElse(null);

        // 1) Asegurar que el panel de marcas exista
        waits.visible(brandsRefinements);

        // 2) Localizar la fila del filtro que contiene el texto de la marca (dentro de brandsRefinements)
        By brandRow = By.xpath(
                "//div[@id='brandsRefinements']//li[.//span[contains(normalize-space(),'" + brand + "')]]"
        );

        // 3) Si ya está marcado, no hagas nada
        By brandCheckbox = By.xpath(
                "//div[@id='brandsRefinements']//li[.//span[contains(normalize-space(),'" + brand + "')]]//input[@type='checkbox']"
        );
        if (!driver.findElements(brandCheckbox).isEmpty() && driver.findElement(brandCheckbox).isSelected()) {
            return;
        }

        WebElement row = waits.visible(brandRow);

        // 4) Click “seguro”: primero intenta el <a>, si no existe, el <label>, si no, la fila
        List<WebElement> link = row.findElements(By.cssSelector("a"));
        if (!link.isEmpty()) {
            waits.click(link.get(0));
        } else {
            List<WebElement> label = row.findElements(By.cssSelector("label"));
            if (!label.isEmpty()) {
                waits.click(label.get(0));
            } else {
                waits.click(row);
            }
        }

        // 5) Esperar recarga de resultados
        if (firstCard != null) {
            waits.waitForStaleness(firstCard);
        }
        waits.waitUntil(d -> d.findElements(searchResults).size() > 0);
    }



    public void setPriceRange(int min, int max) {
        waits.type(lowPrice, String.valueOf(min));
        waits.type(highPrice, String.valueOf(max));
        waits.click(goPriceBtn);
        // suele agregar p_36 para rango de precio
        waits.waitForUrlContains("p_36");
    }

    public String getResultsText() {
        // Amazon cambia esto seguido; por eso lo busco “flexible”
        try {
            WebElement el = driver.findElement(By.xpath("(//span[contains(.,'result') or contains(.,'results')])[1]"));
            return el.getText();
        } catch (Exception e) {
            return "No se pudo obtener el texto de resultados (layout cambió).";
        }
    }

    public void sortByVisibleText(String visibleText) {
        WebElement dd = waits.visible(sortDropdown);
        new Select(dd).selectByVisibleText(visibleText);
        // espera suave a que refresque (podría mejorarse con condiciones)
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
    }

    public void sortByValue(String value) {
        WebElement firstCard = driver.findElements(searchResults).stream().findFirst().orElse(null);

        Select s = new Select(waits.visible(sortDropdown));
        s.selectByValue(value);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        if (firstCard != null) {
            wait.until(ExpectedConditions.stalenessOf(firstCard));
        }

        // Espera a que haya AL MENOS 10 resultados (suficiente para extraer top 5)
        wait.until(d -> d.findElements(searchResults).size() >= 0);

        // Espera a que el primer card ya tenga título (y así no lees “a medio cargar”)
        wait.until(d -> {
            List<WebElement> cards = d.findElements(searchResults);
            if (cards.isEmpty()) return false;
            WebElement c0 = cards.get(0);
            return !c0.findElements(By.cssSelector("h2 span")).isEmpty();
        });
    }

    public List<Product> getTopNProductsWithTitleAndPrice(int n) {
        List<Product> products = new ArrayList<>();

        List<WebElement> cards = driver.findElements(searchResults);
        //System.out.println("Cards encontrados: " + cards.size());

        int noTitle = 0, noPrice = 0;

        for (WebElement card : cards) {
            if (products.size() >= n) break;

            // OJO: por ahora NO saltes patrocinados, porque el top 5 puede incluirlos y te quedas sin 5
            // Si luego quieres excluirlos, lo hacemos con “si ya tengo 5, recién filtro”.

            // ----- TITLE (fallback) -----
            String title = firstNonBlank(
                    card,
                    By.cssSelector("h2 span"),
                    By.cssSelector("h2 a span")
            );
            if (title == null) { noTitle++; continue; }

            // ----- PRICE (fallback) -----
            String priceText = firstNonBlank(
                    card,
                    By.cssSelector("span.a-price span.a-offscreen"),
                    By.cssSelector("span.a-price")
            );

            // si no hay offscreen, intenta whole+fraction
            if (priceText == null) {
                String whole = firstNonBlank(card, By.cssSelector("span.a-price-whole"));
                if (whole != null) {
                    String frac = firstNonBlank(card, By.cssSelector("span.a-price-fraction"));
                    if (frac == null) frac = "00";
                    priceText = whole + "." + frac;
                }
            }

            if (priceText == null) { noPrice++; continue; }

            double price;
            try {
                price = parsePrice(priceText);
            } catch (Exception e) {
                noPrice++;
                continue;
            }

            products.add(new Product(title, price));
        }

        System.out.println("Sin título: " + noTitle + " | Sin precio: " + noPrice);
        System.out.println("Productos extraídos con precio: " + products.size());
        return products;
    }
    public List<Product> getTopNProductsWithTitle(int n) {
        List<Product> products = new ArrayList<>();

        // Asegurar que haya resultados
        waits.waitUntil(d -> d.findElements(searchResults).size() > 0);

        // Releer cards ya “estables”
        List<WebElement> cards = driver.findElements(searchResults);

        // 2 pasadas: 1) sin patrocinados, 2) incluye patrocinados si faltan
        for (int pass = 0; pass < 2 && products.size() < n; pass++) {
            boolean skipSponsored = (pass == 0);

            for (WebElement card : cards) {
                if (products.size() >= n) break;

                if (skipSponsored && !card.findElements(sponsoredInCard).isEmpty()) {
                    continue;
                }

                // Título
                List<WebElement> titleEls = card.findElements(titleInCard);
                if (titleEls.isEmpty()) continue;

                String title = titleEls.get(0).getText().trim();
                if (title.isBlank()) continue;

                // Precio (SIN reventar si no existe)
                Double price = null;
                List<WebElement> priceEls = card.findElements(priceInCard);
                if (!priceEls.isEmpty()) {
                    String priceText = priceEls.get(0).getText().trim();
                    if (!priceText.isBlank()) {
                        try {
                            price = parsePrice(priceText);
                        } catch (Exception ignored) {
                            price = null;
                        }
                    }
                }


                products.add(new Product(title, price));
            }
        }

        return products;
    }

    private String firstNonBlank(WebElement root, By... locators) {
        for (By loc : locators) {
            List<WebElement> els = root.findElements(loc);
            for (WebElement e : els) {
                String t = e.getText();
                if (t != null && !t.trim().isEmpty()) return t.trim();
            }
        }
        return null;
    }


    private double parsePrice(String priceText) {
        String cleaned = priceText.replaceAll("[^0-9.,]", "");

        if (cleaned.isBlank()) {
            throw new IllegalArgumentException("Precio vacío: " + priceText);
        }

        if (cleaned.contains(",") && !cleaned.contains(".")) cleaned = cleaned.replace(",", ".");
        if (cleaned.contains(",") && cleaned.contains(".")) cleaned = cleaned.replace(",", "");

        return Double.parseDouble(cleaned);
    }

    public String getResultsSummaryText() {
        return waits.visible(resultsSummary).getText().trim();
    }
    public int getCardsCount() {
        return driver.findElements(searchResults).size();
    }
}
