package framework.pages;

import framework.utils.Waits;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class AmazonHomePage {
    private final WebDriver driver;
    private final Waits waits;

    private final By searchBox = By.id("twotabsearchtextbox");
    private final By searchBtn = By.id("nav-search-submit-button");

    // Banner cookies (a veces aparece)
    private final By acceptCookiesBtn = By.id("sp-cc-accept");

    public AmazonHomePage(WebDriver driver) {
        this.driver = driver;
        this.waits = new Waits(driver, 15);
    }

    public void open(String baseUrl) {
        driver.get(baseUrl);
        acceptCookiesIfPresent();
    }

    private void acceptCookiesIfPresent() {
        try {
            driver.findElement(acceptCookiesBtn).click();
        } catch (Exception ignored) {}
    }

    public void search(String text) {
        waits.type(searchBox, text);
        waits.click(searchBtn);
    }
}
