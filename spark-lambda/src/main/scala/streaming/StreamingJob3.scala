package streaming

import domain.{Activity, ActivityByProduct}
import org.apache.spark.SparkContext
import org.apache.spark.streaming.{Duration, Seconds, StreamingContext}
import utils.SparkUtils

/**
  * @author Behzad Pirvali, 10/11/18
  *
  * Extends StreamingJob2 by doing some aggregations
  */
object StreamingJob3 {
  def main(args: Array[String]): Unit = {
    // setup spark context
    val sc = SparkUtils.getSparkContext("Lambda with Spark")
    val sqlContext = SparkUtils.getSQLContext(sc)
    import sqlContext.implicits._

    val batchDuration = Seconds(4)

    def streamingApp(sc: SparkContext, batchDuration: Duration): StreamingContext = {
      val ssc = new StreamingContext(sc, batchDuration)
      val inputPath = SparkUtils.isIDE match {
        case true => "file:///Users/behzad.pirvali/Tools/vagrant/boxes/spark-kafka-cassandra-applying-lambda-architecture/vagrant/input"
        case false => "file:///vagrant/input"
      }
      val textDStream = ssc.textFileStream(inputPath)

      val activityStream = textDStream.transform(input => { // input is a rdd, which is a distributed list
        input.flatMap { line =>
          val record = line.split("\\t")
          val MS_IN_ONE_HOUR = 1000 * 60 * 60
          if (record.length == 7)
            Some(Activity(record(0).toLong / MS_IN_ONE_HOUR * MS_IN_ONE_HOUR, record(1), record(2), record(3), record(4), record(5), record(6)))
          else
            None
        }
      })

      activityStream.transform(rdd => {
        val df = rdd.toDF()
        df.createOrReplaceTempView("activity")

        val activityByProduct = sqlContext.sql("""SELECT
                                            product,
                                            timestamp_hour,
                                            sum(case when action = 'purchase' then 1 else 0 end) as purchase_count,
                                            sum(case when action = 'add_to_cart' then 1 else 0 end) as add_to_cart_count,
                                            sum(case when action = 'page_view' then 1 else 0 end) as page_view_count
                                            from activity
                                            group by product, timestamp_hour
                                            """)
      activityByProduct.map(r => ((r.getString(0), r.getLong(1)),
          ActivityByProduct(r.getString(0),r.getLong(1), r.getLong(2), r.getLong(3), r.getLong(4)))).rdd
      }).print()




      ssc
    }

    val ssc = SparkUtils.getStreamingContext(streamingApp, sc, batchDuration)
    ssc.start()
    ssc.awaitTermination()
  }
}
