//> using scala "3.7.1"
//> using dep "org.seleniumhq.selenium:selenium-java:4.33.0"
//> using dep "com.lihaoyi::ujson:4.2.1"
//> using dep "com.lihaoyi::requests:0.9.0"

import java.nio.file.*
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.Dimension
import org.openqa.selenium.OutputType
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.JavascriptExecutor
import java.io.{File, FileWriter}
import java.nio.file.Paths

object UrlToImage {
  val userId = "1ee3440e-9e6f-4acc-a755-a4ca12dd6424"

  def main(args: Array[String]): Unit = {
    val apiUrl = s"https://wakatime.com/api/v1/users/$userId/stats/all_time?timeout=15"
    val response = requests.get(apiUrl)
    val json = ujson.read(response.text())
    val languages = json("data")("languages").arr
      .filter(lang => lang("percent").num >= 0.1)
      .sortBy(lang => -lang("percent").num)
      .take(10)


    val html = s"""
                  |<!DOCTYPE html>
                  |<html>
                  |<head>
                  |  <meta charset="UTF-8">
                  |  <style>
                  |    body { font-family: Arial, sans-serif; background: #fff; padding: 20px; margin: 0; }
                  |    .language-bar {
                  |      display: flex;
                  |      align-items: center;
                  |      margin: 10px 0;
                  |      width: 100%;
                  |    }
                  |    .language-name {
                  |      width: 120px;
                  |      text-align: right;
                  |      padding-right: 10px;
                  |      font-size: 14px;
                  |    }
                  |    .bar-container {
                  |      flex-grow: 1;
                  |      height: 20px;
                  |      background: #f0f0f0;
                  |      border-radius: 3px;
                  |      overflow: hidden;
                  |    }
                  |    .bar {
                  |      height: 100%;
                  |      background: #2196F3;
                  |      transition: width 0.3s ease;
                  |    }
                  |    .percentage {
                  |      margin-left: 10px;
                  |      font-size: 14px;
                  |      color: #666;
                  |    }
                  |  </style>
                  |</head>
                  |<body>
                  |  ${languages.map { lang =>
      val name = lang("name").str
      val percent = lang("percent").num
      val hours = lang("hours").num
      val minutes = lang("minutes").num
      s"""
         |  <div class="language-bar">
         |    <div class="language-name">$name</div>
         |    <div class="bar-container">
         |      <div class="bar" style="width: ${percent}%;"></div>
         |    </div>
         |    <div class="percentage">${"%.1f".format(percent)}% (${hours}h ${minutes}m)</div>
         |  </div>
           """.stripMargin
    }.mkString}
                  |</body>
                  |</html>
    """.stripMargin


    val tempDir = System.getProperty("java.io.tmpdir")
    val htmlFile = Paths.get(tempDir, "wakatime_stats.html").toFile
    val writer = new FileWriter(htmlFile)
    writer.write(html)
    writer.close()

    val options = new ChromeOptions()
    options.addArguments("--headless")
    options.addArguments("--disable-gpu")
    options.addArguments("--no-sandbox")
    options.addArguments("--disable-dev-shm-usage")
    options.addArguments("--window-size=800,600")
    options.addArguments("--allow-file-access-from-files")

    val driver = new ChromeDriver(options)
    try {
      val fileUrl = htmlFile.toURI.toString
      println(s"Accessing file: $fileUrl")

      driver.get(fileUrl)

      val wait = new WebDriverWait(driver, Duration.ofSeconds(10))
      wait.until { webDriver =>
        webDriver.asInstanceOf[JavascriptExecutor]
          .executeScript("return document.readyState")
          .equals("complete")
      }

      val body = driver.findElement(By.tagName("body"))
      val screenshot = body.getScreenshotAs(OutputType.FILE)

      val destination = "language_stats.png"
      val destFile = Paths.get(destination)
      Files.copy(screenshot.toPath, destFile, StandardCopyOption.REPLACE_EXISTING)

      println(s"Screenshot saved as $destination")
    } catch {
      case e: Exception =>
        println(s"Error occurred: ${e.getMessage}")
        e.printStackTrace()
    } finally {
      driver.quit()
      htmlFile.delete()
    }
  }
}