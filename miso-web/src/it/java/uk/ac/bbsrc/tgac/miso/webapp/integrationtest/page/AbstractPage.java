package uk.ac.bbsrc.tgac.miso.webapp.integrationtest.page;

import static org.openqa.selenium.support.ui.ExpectedConditions.stalenessOf;

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.bbsrc.tgac.miso.webapp.integrationtest.page.element.AbstractElement;

public abstract class AbstractPage extends AbstractElement {

  private static final Logger log = LoggerFactory.getLogger(AbstractPage.class);

  private static final ExpectedCondition<Boolean> pageLoaded = (driver) -> {
    return ((JavascriptExecutor) driver).executeScript("return document.readyState").equals("complete");
  };

  protected AbstractPage(WebDriver driver) {
    super(driver);
  }

  protected static void waitExplicitly(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new RuntimeException("Thread interrupted during sleep", e);
    }
  }

  /**
   * Assesses page refresh by checking the staleness state of the HTML tag of the current page.
   * 
   * @param html the HTML element of the current page
   */
  protected void waitForPageRefresh(WebElement html) {
    waitUntil(ExpectedConditions.stalenessOf(html));
    waitUntil(pageLoaded);
  }

  protected void waitForPageRefresh(long seconds) {
    waitUntil(pageLoaded, seconds);
  }

  protected static void setText(String input, WebElement element) {
    element.click();
    element.clear();
    element.sendKeys(input == null ? "" : input);
  }

  protected static void setCheckbox(Boolean check, WebElement element) {
    // only change if given value and element value differ
    if (check != element.isSelected()) {
      element.click();
    }
  }

  protected static void setDropdown(String input, WebElement element) {
    element.click();
    Select select = new Select(element);
    select.selectByVisibleText(input);
  }

  protected static String getSelectedDropdownText(WebElement element) {
    Select dropdown = new Select(element);
    return dropdown.getFirstSelectedOption().getText();
  }

  protected static void setRadioButton(String input, List<WebElement> buttons) {
    WebElement targetButton = buttons.stream().filter(button -> button.getAttribute("value").equalsIgnoreCase((input)))
        .findAny().orElse(null);
    if (targetButton == null) throw new IllegalArgumentException("Could not find radio button with label " + input);
    targetButton.click();
  }

  protected static String getSelectedRadioButtonValue(List<WebElement> buttons) {
    WebElement selectedButton = buttons.stream().filter(button -> button.isSelected()).findAny().orElse(null);
    if (selectedButton == null) throw new IllegalArgumentException("No buttons are selected for set " + buttons.get(0).getAttribute("id"));
    return selectedButton.getAttribute("value");
  }

  private static final String MISO_URL = "%smiso/%s";
  private static final String MISO_STACKTRACE = "uk.ac.bbsrc";

  public static boolean checkForErrors(WebDriver driver, String baseUrl, String urlSlug) {
    String url = String.format(MISO_URL, baseUrl, urlSlug);
    try {
      driver.get(url);
      // confirm that page contains logo
      if (driver.findElements(By.id("misologo")).isEmpty())
        throw new IllegalArgumentException("Page at /miso/" + urlSlug + " is completely empty. Is resource correct?");

      List<WebElement> errors = driver.findElements(By.xpath("//li[contains(text(), '" + MISO_STACKTRACE + "')]"));
      if (errors.size() > 0) {
        errors.stream().map(item -> item.getText().trim()).collect(Collectors.toList()).toString();
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      log.error(String.format("Exception thrown while checking slug: %s", urlSlug), e);
      throw e;
    }
  }

  protected WebElement getHtmlElement() {
    return getDriver().findElement(By.tagName("html"));
  }

  protected void clickOk() {
    WebElement okButton = getDriver().findElement(By.id("ok"));
    okButton.click();
  }

  protected void clickCancel() {
    WebElement cancelButton = getDriver().findElement(By.id("cancel"));
    cancelButton.click();
  }

  public String getCurrentUrl() {
    return getDriver().getCurrentUrl();
  }

  protected String clickLinkButtonAndGetUrl(String linkText, List<String> selections, boolean confirmRequired) {
    WebElement button = getDriver().findElement(By.linkText(linkText));
    WebElement html = getHtmlElement();
    button.click();
    if (confirmRequired) {
      clickOk();
    }
    if (selections != null && !selections.isEmpty()) {
      selections.forEach(selection -> {
        WebElement link = getDriver().findElement(By.id("dialog")).findElement(By.partialLinkText(selection));
        link.click();
        waitUntil(stalenessOf(link));
      });
    }
    waitForPageRefresh(html);
    return getCurrentUrl();
  }
}
