import org.apache.spark.SparkContext
import org.apache.spark.graphx.{GraphLoader, PartitionStrategy}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import java.io.File

object main {
  // Preprocess text data
  private def preprocess(text: String): String = {
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

  private def lab4(sc:SparkContext): Unit = {
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

  private def lab5(sc:SparkContext, spark: SparkSession): Unit = {
    import spark.implicits._

    val dataPath = "C:\\Users\\havri\\Programming\\University\\concurrency\\spark-triangles-scala\\TrianglesSpark\\resources\\news20\\20_newsgroup"

    // Load files
    val files = sc.wholeTextFiles(dataPath + "/*")
      .map { case (filePath, content) =>
        val filename = filePath.split("/").last.split("\\.").head
        val cleaned = preprocess(content)
        (filename, cleaned)
      }.toDF("id", "content")

    // Break down into words and filter out non-alphabetic words
    val words = files
      .select($"id", explode(split($"content", "\\s+")).as("word"))
      .filter($"word".rlike("^[a-z']+$") && !$"word".startsWith("\'"))

    // Find the inverted index
    val invertedIndex = words
      .groupBy("word")
      .agg(
        count("*").as("total_count"),
        collect_set("id").as("documents")
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
      .csv("C:\\Users\\havri\\Programming\\University\\concurrency\\spark-triangles-scala\\TrianglesSpark\\output")
  }

  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName("Labs")
      .master("local[*]")
      .config("spark.driver.extraJavaOptions", "--add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED")
      .getOrCreate()

    val sc = spark.sparkContext

    //lab4(sc)
    lab5(sc, spark)

    spark.stop()
  }
}