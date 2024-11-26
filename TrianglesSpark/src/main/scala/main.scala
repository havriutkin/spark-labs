
/*
--add-exports java.base/sun.nio.ch=ALL-UNNAMED
*/
// scalastyle:off println

// $example on$
import org.apache.spark.SparkContext
import org.apache.spark.graphx.{GraphLoader, PartitionStrategy}
// $example off$
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import java.io.File

object main {
  // Clear text data
  private def cleanText(text: String): String = {
    text.toLowerCase
      .replaceAll("(?i)path:.*", "")
      .replaceAll("(?i)newsgroups:.*", "")
      .replaceAll("(?i)writes:.*", "")
      .replaceAll("(?i)from:.*", "")
      .replaceAll("(?i)lines:.*", "")
      .replaceAll("(?i)date:.*", "")
      .replaceAll("(?i)References:.*", "")
      .replaceAll("(?i)Organization:.*", "")
      .replaceAll("(?i)Nntp-Posting-Host:.*", "")
      .replaceAll("(?i)Sender.*", "")
      .replaceAll("(?i)Message-ID.*", "")
      .replaceAll("(?i)Xref.*", "")
      .replaceAll("'","\'")
      .replaceAll("(?is)begin\\s+\\d+\\s+\\S+.*?end", "")
      .replaceAll("[^a-z']", " ")
  }

  def lab4(sc:SparkContext): Unit = {
    val graph = GraphLoader.edgeListFile(sc,
        "C:\\Users\\havri\\Programming\\University\\concurrency\\spark-triangles-scala\\TrianglesSpark\\resources\\graph.txt",
        canonicalOrientation = false)
        .partitionBy(PartitionStrategy.RandomVertexCut)

    // Find the triangle count for each vertex
    val triCounts = graph.triangleCount().vertices

    // Sum the triangle counts and divide by 3 to get the total number of unique triangles
    val totalTriangles = triCounts.map(_._2).reduce(_ + _) / 3

    println(s"Total number of triangles: $totalTriangles")
  }

  def lab5(sc:SparkContext, spark: SparkSession): Unit = {
    import spark.implicits._

    val dataPath = "C:/Users/mykol/Downloads/news20/20_newsgroup"

    // Зчитування всіх файлів з каталогу
    val files = sc.wholeTextFiles(dataPath + "/*")
      .map { case (filePath, content) =>
        val filename = filePath.split("/").last.split("\\.").head
        val cleanedContent = cleanText(content)
        (filename, cleanedContent)
      }.toDF("docId", "content")

    // Розбиття контенту на слова і фільтрація
    val words = files
      .select($"docId", explode(split($"content", "\\s+")).as("word"))
      .filter($"word".rlike("^[a-z']+$") && !$"word".startsWith("\'"))

    // Обчислення інвертованого індексу
    val invertedIndex = words
      .groupBy("word")
      .agg(
        count("*").as("total_count"),
        collect_set("docId").as("documents")
      )
      .select(
        $"word",
        $"total_count",
        concat_ws(" ", $"documents").as("doc_list")
      )

    invertedIndex.coalesce(1)
      .write
      .mode("overwrite")
      .option("header", "true")
      .csv("E:/Parallel-and-distributed-computing/Lab4/MyOut")
  }

  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName("Labs")
      .master("local[*]")
      .config("spark.driver.extraJavaOptions", "--add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED")
      .getOrCreate()

    val sc = spark.sparkContext

    lab4(sc)
    //lab5(sc, spark)

    spark.stop()
  }
}