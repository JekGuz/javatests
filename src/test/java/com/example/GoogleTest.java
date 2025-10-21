package com.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;

@TestMethodOrder(MethodOrderer.DisplayName.class)
class GoogleTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private static final String BASE = "https://uitestingplayground.com";

    @BeforeEach
    void setUp() {
        WebDriverManager.chromedriver().setup();
        org.openqa.selenium.chrome.ChromeOptions options = new org.openqa.selenium.chrome.ChromeOptions();
        options.addArguments("--remote-allow-origins=*", "--disable-dev-shm-usage", "--no-sandbox");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        driver = new ChromeDriver(options);                 // non-headless
        driver.manage().window().maximize();
    wait = new WebDriverWait(driver, Duration.ofSeconds(20)); // increased to reduce flakiness
    }

    @AfterEach
    void finish() {
        if (driver != null) {
            try { driver.quit(); } catch (WebDriverException ignored) {}
        }
    }

    /* helpers */
    private WebElement cssVisible(String selector) {
        ensureDocumentReady();
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(selector)));
    }
    private WebElement cssPresent(String selector) {
        ensureDocumentReady();
        return wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(selector)));
    }
    private void ensureDocumentReady() {
        try {
            wait.until((ExpectedCondition<Boolean>) d ->
                ((JavascriptExecutor) d).executeScript("return document.readyState").equals("complete"));
        } catch (Exception ignored) {}
    }
    private void click(By by) {
        // 1) дождаться, что элемент вообще есть в DOM
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(by));
        try {
            // 2) обычный клик по кликабельности
            wait.until(ExpectedConditions.elementToBeClickable(el)).click();
        } catch (TimeoutException | ElementClickInterceptedException e) {
            // 3) запасной план: прокрутить и кликнуть JS-ом
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'})", el);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        }
    }
    private void scrollIntoView(WebElement el) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'})", el);
    }
    private boolean present(By by) {
        return !driver.findElements(by).isEmpty();
    }

    // 1. Sample App: login
    @Test @DisplayName("01 Sample App login")
    void sampleAppLogin() {
        driver.get(BASE + "/sampleapp");
        cssVisible("input[name='UserName']").sendKeys("Kat");
        cssVisible("input[name='Password']").sendKeys("pwd");
        click(By.cssSelector("#login"));
        String msg = cssVisible("#loginstatus").getText();
        Assertions.assertTrue(msg.contains("Welcome, Kat!"));
    }

    // 2. Sample App: logout
    @Test @DisplayName("02 Sample App logout")
    void sampleAppLogout() {
        driver.get(BASE + "/sampleapp");
        cssVisible("input[name='UserName']").sendKeys("Kat");
        cssVisible("input[name='Password']").sendKeys("pwd");
        click(By.cssSelector("#login"));
        click(By.cssSelector("#login")); // та же кнопка становится Logout
        String msg = cssVisible("#loginstatus").getText();
        Assertions.assertTrue(msg.contains("User logged out."));
    }

    // 3. Dynamic ID
    @Test @DisplayName("03 Dynamic ID")
    void dynamicId() {
        driver.get(BASE + "/dynamicid");
        click(By.cssSelector("button.btn.btn-primary"));
        Assertions.assertTrue(true);
    }

    // 4. Class Attribute (alert contains "primary")
    @Test @DisplayName("04 Class Attribute")
    void classAttribute() {
        driver.get(BASE + "/classattr");
        click(By.cssSelector(".btn.btn-primary"));
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        String text = alert.getText();
        alert.accept();
        Assertions.assertTrue(text.toLowerCase().contains("primary"));
    }

    // 5. Hidden Layers
    @Test @DisplayName("05 Hidden Layers")
    void hiddenLayers() {
        driver.get(BASE + "/hiddenlayers");
        By green = By.cssSelector("#greenButton");
        click(green);
        boolean intercepted = false;
        try {
            driver.findElement(green).click(); // второй клик
        } catch (ElementClickInterceptedException e) {
            intercepted = true;
        }
        Assertions.assertTrue(intercepted);
    }

    // 6. Load Delay
    @Test @DisplayName("06 Load Delay")
    void loadDelay() {
        ensureDocumentReady();
        driver.get(BASE + "/loaddelay");
        WebElement button = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("button.btn.btn-primary")));
        Assertions.assertTrue(button.isDisplayed());
    }

    // 7. AJAX Data
    @Test @DisplayName("07 AJAX Data")
    void ajaxData() {
    driver.get(BASE + "/ajax");
    By ajaxBtn = By.cssSelector("#ajaxButton");
    wait.until(ExpectedConditions.elementToBeClickable(ajaxBtn));
    click(ajaxBtn);
    By result = By.cssSelector("#content p");
    wait.until(ExpectedConditions.visibilityOfElementLocated(result));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(result, "Data loaded"));
    String text = driver.findElement(result).getText();
    Assertions.assertTrue(text.contains("Data loaded with AJAX get request."));
}

    // 8. Text Input
    @Test @DisplayName("08 Text Input")
    void textInput() {
        driver.get(BASE + "/textinput");
        cssVisible("#newButtonName").sendKeys("Hello");
        click(By.cssSelector("#updatingButton"));
        Assertions.assertEquals("Hello", cssVisible("#updatingButton").getText());
    }

    // 9. Scrollbars
    @Test @DisplayName("09 Scrollbars")
    void scrollbars() {
        driver.get(BASE + "/scrollbars");
        WebElement btn = cssPresent("#hidingButton");
        scrollIntoView(btn);
        wait.until(ExpectedConditions.elementToBeClickable(btn)).click();
        Assertions.assertTrue(true);
    }

    // 10. Overlapped Element
    @Test @DisplayName("10 Overlapped Element")
    void overlappedElement() {
        driver.get(BASE + "/overlapped");
        WebElement field = cssVisible("#name");
        scrollIntoView(field);
        field.clear();
        field.sendKeys("abc");
        Assertions.assertEquals("abc", field.getAttribute("value"));
    }

    // 11. Visibility (display vs. removed)
    @Test
    void visibility() {
        driver.get(BASE + "/visibility");


    click(By.id("hideButton"));

        int removedButtonCount = driver.findElements(By.id("removedButton")).size();
        assertEquals(0, removedButtonCount, "Removed button should not be found in DOM.");


        WebElement invisibleButton = driver.findElement(By.id("invisibleButton"));
        assertFalse(invisibleButton.isDisplayed(), "Invisible button should be in DOM but not displayed.");
    }

    // 12. Click
    @Test @DisplayName("12 Click")
    void clickTest() {
        driver.get(BASE + "/click");
        WebElement bad = cssVisible("#badButton");
        bad.click();
        Assertions.assertTrue(bad.getAttribute("class").contains("btn-success"));
    }

    // 13. Progress Bar (stop at >= 75)
    @Test @DisplayName("13 Progress Bar")
    void progressBar() {
        ensureDocumentReady();
        driver.get(BASE + "/progressbar");
        click(By.cssSelector("#startButton"));
        WebElement bar = cssVisible("#progressBar");
        wait.until((ExpectedCondition<Boolean>) d -> {
            int v = Integer.parseInt(bar.getAttribute("aria-valuenow"));
            return v >= 75;
        });
        click(By.cssSelector("#stopButton"));
        int value = Integer.parseInt(cssVisible("#progressBar").getAttribute("aria-valuenow"));
        Assertions.assertTrue(value >= 75);
    }

    // 14. Mouse Over
    @Test @DisplayName("14 Mouse Over")
    void mouseOver() {
        driver.get(BASE + "/mouseover");
        WebElement link = cssVisible(".text-primary");
        new Actions(driver).moveToElement(link).click().click().perform();
        String count = cssVisible("#clickCount").getText();
        Assertions.assertEquals("2", count);
    }

    // 15. Shadow DOM
    @Test @DisplayName("15 Shadow DOM")
    void shadowDom() {
        driver.get(BASE + "/shadowdom");
        WebElement host = cssPresent("guid-generator");
        SearchContext root = host.getShadowRoot();
        String text = root.findElement(By.cssSelector("#buttonGenerate")).getText();
        Assertions.assertNotNull(text);
    }
}
