package batch

import org.apache.spark.sql.SaveMode
import utils.SparkUtils._

case class Activity1(timestamp_hour: Long,
                    referrer: String,
                    action: String,
                    prevPage: String,
                    visitor: String,
                    page: String,
                    product: String,
                    inputProps: Map[String, String] = Map()
                   )

/**
  * @author behzad.pirvali, 8/27/18
  */
object BatchJob1 {
  def main (args: Array[String]): Unit = {

    // setup spark context
    val sc = getSparkContext("Lambda with Spark")
    val sqlContext = getSQLContext(sc)

    import org.apache.spark.sql.functions._
    import sqlContext.implicits._

    // initialize input RDD
    val sourceFile = "file:///vagrant/data.tsv"
    val input = sc.textFile(sourceFile)

    val inputDF = input.flatMap{ line =>
      val record = line.split("\\t")
      val MS_IN_HOUR = 1000 * 60 * 60
      if (record.length == 7)
        Some(Activity1(record(0).toLong / MS_IN_HOUR * MS_IN_HOUR, record(1), record(2), record(3), record(4), record(5), record(6)))
      else
        None
    }.toDF()

    val df = inputDF.select(
      add_months(from_unixtime(inputDF("timestamp_hour") / 1000), 1).as("timestamp_hour"),
      inputDF("referrer"), inputDF("action"), inputDF("prevPage"), inputDF("page"), inputDF("visitor"), inputDF("product")
    ).cache()

    df.registerTempTable("activity")
    val visitorsByProduct = sqlContext.sql(
      """SELECT product, timestamp_hour, COUNT(DISTINCT visitor) as unique_visitors
        |FROM activity GROUP BY product, timestamp_hour
      """.stripMargin)

    val activityByProduct = sqlContext.sql("""SELECT
                                            product,
                                            timestamp_hour,
                                            sum(case when action = 'purchase' then 1 else 0 end) as purchase_count,
                                            sum(case when action = 'add_to_cart' then 1 else 0 end) as add_to_cart_count,
                                            sum(case when action = 'page_view' then 1 else 0 end) as page_view_count
                                            from activity
                                            group by product, timestamp_hour """).cache()

    activityByProduct.write.partitionBy("timestamp_hour").mode(SaveMode.Append).parquet("hdfs://localhost:9000/lambda/batch1")
    //visitorsByProduct.foreach(println)
//    visitorsByProduct.foreach(println)
//    activityByProduct.foreach(println)

  }

}
