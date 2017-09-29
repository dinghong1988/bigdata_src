import org.apache.spark.{SparkContext, SparkConf}

/**
 * Created by Administrator on 2017/9/21.
 */
object TOPNSparkCore {
  def main(args: Array[String]) {

    val conf = new SparkConf()
      .setMaster("local[*]")
      .setAppName("TOPN")

    val sc = new SparkContext(conf)

    //文件的读取路径
    val path = "/user/spark/data/wc.input"

    //将数据转换可操作的rdd形式
    val rdd = sc.textFile(path)

    val N = 3

    val TopN = rdd
      //数据过滤
      .filter(_.length > 0)
      //数据转换
      .flatMap(_.split(" ").map((_,1)))
      //数据分组合并
      .reduceByKey(_ + _)
      //取前N个数字
      .top(N)(ord = new scala.math.Ordering[(String,Int)] {
        override def compare(x: (String, Int), y: (String, Int)): Int = {
          val tmp = x._2.compare(y._2)
          if(tmp == 0) x._1.compare(y._1) else tmp
        }
      })


    //打印输出结果
  //  TopN.foreach(println)
    sc.stop()
}

}
