package uk.ac.bbsrc.tgac.miso.webapp.integrationtest.util;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;

public class MoreExpectedConditions {

  private MoreExpectedConditions() {
    throw new IllegalStateException("Util class not intended for instantiation");
  }

  public static ExpectedCondition<Boolean> textDoesNotContain(WebElement element, String text) {
    return (driver) -> !element.getText().contains(text);
  }

  public static ExpectedCondition<Boolean> elementDoesNotExist(By selector) {
    return (driver) -> driver.findElements(selector).size() == 0;
  }

  public static ExpectedCondition<Boolean> jsReturnsTrue(String script) {
    return (driver) -> (Boolean) ((JavascriptExecutor) driver).executeScript(script);
  }

}
