package com.spark.core

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkContext, SparkConf}

/**
 * Created by Administrator on 2017/9/29.
 */
object LogPVandUVCalc {
  def main(args: Array[String]) {

    //创建SparkConf
    val conf = new SparkConf()
      .setMaster("local[*]")
      .setAppName("log-pv-uv")

    //创建SparkContext
    val sc = SparkContext.getOrCreate(conf)

    //存储数据文件路径
    val logPath = "file:///E:\\spark\\spark_demo2\\data\\page_views.data"

    /**
     * 时间：2013-05-19 13:00:00
     * 访问URL地址：http://www.taobao.com/17/?tracker_u=1624169&type=1
     * 用户ID：B58W48U4WKZCJ5D1T3Z9ZY88RU7QA7B1
     * 上一页面访问的地址：http://hao.360.cn/
     * IP地址：1.196.34.243
     * NULL
     * -1
     */

    //读取文件RDD
    val logRDD = sc.textFile(logPath)

    //数据过滤
    val filterRDD = logRDD.filter(_.length > 0)

    //
    val PVandUVRDD:RDD[(String,String,String)] = filterRDD.map(line => {
      //数据分隔
      val list = line.split("\t")

      if(list.length == 7){

        val date = list(0)
        val url = list(1)
        val uuid = list(2)

        (date.substring(0,Math.min(10,date.length)),url,uuid)
      }else{
        (null,null,null)
      }
    }).filter(tuple => tuple._1 != null && tuple._1.length > 0)

    //计算PV
    val PVRDD = PVandUVRDD
      .filter(tuple => tuple._2 != null && tuple._2.length > 0)
      .map(tp => (tp._1,1))
      .reduceByKey(_ + _)


    //计算UV
    val UVRDD = PVandUVRDD
      .filter(tuple => tuple._3 != null && tuple._3.length > 0)
      .map(tp => (tp._1,tp._3))
      .distinct()
      .map(tp => (tp._1,1))
      .reduceByKey(_ + _)

    val PVandUVResultRDD = PVRDD.join(UVRDD)
      .map(tuple => (tuple._1,tuple._2._1,tuple._2._2))

    PVandUVResultRDD.foreach(println)

    sc.stop()

  }

}
