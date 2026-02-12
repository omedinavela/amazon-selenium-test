package tests;

import framework.driver.DriverFactory;
import framework.models.Product;
import framework.pages.AmazonHomePage;
import framework.pages.AmazonResultsPage;
import framework.utils.ScreenshotUtil;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

public class AmazonSearchTest {
    private WebDriver driver;
    private String baseUrl;

    @BeforeClass
    public void loadConfig() throws Exception {
        Properties props = new Properties();
        props.load(new FileInputStream("src/test/resources/config.properties"));
        baseUrl = props.getProperty("baseUrl", "https://www.amazon.com");
        System.setProperty("headless", props.getProperty("headless", "false"));
    }

    @BeforeMethod
    public void setup() {
        driver = DriverFactory.createDriver();
    }

    @Test
    public void amazonScenario() {
        AmazonHomePage home = new AmazonHomePage(driver);
        AmazonResultsPage results = new AmazonResultsPage(driver);

        // 1) Buscar “zapatos”
        home.open(baseUrl);
        home.search("zapatos");

        System.out.println("Resultados (zapatos ): " + results.getResultsSummaryText());

        // 2) Filtrar Skechers
        results.filterBrand("Skechers");

        System.out.println("Resultados (zapatos + Skechers): " + results.getResultsSummaryText());
        // 3) Rango $100 - $200
       // results.setPriceRange(100, 200);

        System.out.println("⚠️ Paso 3 omitido: Amazon no muestra filtro de rango de precio en este layout/región.");

        // 4) Imprimir # resultados (texto)

        // 5-7) Ordenar por “Precio: De más alto a más bajo” y tomar top 5
        //results.sortByVisibleText("Price: High to Low");
        results.sortByValue("price-desc-rank"); //“Precio: De más alto a más bajo”
        List<Product> top5PriceDesc = results.getTopNProductsWithTitle(5);

        System.out.println("\nTOP 5 - Precio (alto a bajo):"+ top5PriceDesc.size());
        top5PriceDesc.forEach(p -> System.out.println(p));

        // 8) Ordenar e imprimir por nuevos lanzamientos
        results.sortByValue("date-desc-rank");
        List<Product> top5Newest = results.getTopNProductsWithTitleAndPrice(5);
        System.out.println("\nTOP 5 - Nuevos lanzamientos:");
        top5Newest.forEach(p -> System.out.println(p));

        // 9) Ordenar e imprimir por promedio de comentarios
        results.sortByValue("review-rank");
        List<Product> top5Reviews = results.getTopNProductsWithTitleAndPrice(5);
        System.out.println("\nTOP 5 - Promedio comentarios:");
        top5Reviews.forEach(p -> System.out.println(p));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            ScreenshotUtil.takeScreenshot(driver, "final_state");
            driver.quit();
        }
    }
}
