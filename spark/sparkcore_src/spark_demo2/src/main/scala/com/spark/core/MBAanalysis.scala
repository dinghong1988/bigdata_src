package com.spark.core

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkContext, SparkConf}

/** 使用SparkCore实现购物蓝分析
 * Created by Administrator on 2017/9/27.
 */
import scala.collection.mutable
object MBAanalysis {
  def main(args: Array[String]) {

    //创建sparkContext
    val conf = new SparkConf()
      .setMaster("local[*]")
      .setAppName("MBAanaly_app")
    val sc = SparkContext.getOrCreate(conf)

    //测试数据存储路径
    val path = "file:///E:/spark/spark_demo2/data/transactions/10"
   // val path = "/user/spark/data/100"

    val minSupport = 1  //最小支持度
    val minConfidence = 0.4 // 最小置信度

    //创建RDD读取原始交易数据
    //假设交易数据是按行存储的，每行是一条交易，每条交易数据包含商品ID使用","分割
    val rdd = sc.textFile(path)

    //一、计算频繁项集
    //1.获取每条交易存在项集
    val itemSetsRDD:RDD[String] = rdd.flatMap(transform => {

      //1.1 获取当前交易所包含的商品ID
      val items = transform.split(",")
        .filter(!_.isEmpty)
        .sorted
        .toList
        .zipWithIndex

      //1.2 根据获取的商品ID信息产生项集
      val size = items.length

      //创建空缓存
      val cache = mutable.Map[Int, List[List[(String,Int)]]]()

      //产生项集中项的数量是size的项集
      val allItems:List[List[String]] = (1 to size).map(size => {

        findItemSets(items,size,cache)

      }).foldLeft(List[List[String]]())((v1,v2) => {
        v2.map(_.map(_._1).toString()) :: v1
      })

      //返回值
      allItems.map(_.mkString(","))
    })

   // itemSetsRDD.saveAsTextFile("file:///E:\\spark\\spark_demo2\\out\\11_rs")
    //itemSetsRDD.saveAsTextFile("/user/spark/out")

    //二、计算支持度

    //获取频繁项集
    val supportedItemSetsRDD = itemSetsRDD
      //转化成二元组
      .map((_,1))
      //数据聚合求支持度
      .reduceByKey(_ + _)
      //过滤产生的频繁项集==>商品的支持度必须大于等于最小支持度
      .filter(_._2 >= minSupport)

    //三、对每个频繁项集获取子项集
    val subSupportedItemSetsRDD = supportedItemSetsRDD.flatMap(tuple => {

      val itemSets = tuple._1.split(",").toList.zipWithIndex
      val frequency = tuple._2  //获取支持度

      // 1.1 构建辅助对象
      val itemSize = itemSets.size

      //创建空缓存
      val cache = mutable.Map[Int, List[List[(String,Int)]]]()

      //产生项集中所有的子项集
      val allSubItemSets:List[List[String]] = (1 to itemSize).map(size => {

        findItemSets(itemSets,size,cache)

      }).foldLeft(List[List[String]]())((v1,v2) => {
        v2.map(_.map(_._1).toString()) :: v1
      })

      allSubItemSets.map(subItem => {

        (subItem.mkString(","),((itemSets.toBuffer -- subItem).mkString(","),frequency))
      })
    })

    //四、计算置信度
    val assocRulesRDD = subSupportedItemSetsRDD.groupByKey().flatMap(tuple => {

      // 获取左件
      val lhs = tuple._1.mkString("<",",",">")

      val frequency = tuple._2
        .filter(_._1.isEmpty)
        .map(_._2).toList match {
        case head :: Nil => head
        case e => {
          print("异常："+e)
          throw new IllegalArgumentException("异常")
        }
      }

      tuple._2.filter(!_._1.isEmpty).map{

        case (rhs,support) => (
          lhs,rhs.split(",").mkString("<",",",">"),1.0 * support /frequency)
      }
    })

    // 4.2 过滤置信度太低的数据
    val resultRDD = assocRulesRDD.filter(_._3 > minConfidence)

  //  print(assocRulesRDD)
    resultRDD.saveAsTextFile("file:///E:\\spark\\spark_demo2\\out\\11_as")
  }

  /**
   * 从缓存中获取数据，如果不存在，直接获取
   * @param items 商品列表 ：eg：[A,B,C]
   * @param size  最终项集包含商品的数量
   * @param cache
   * @return
   */
  def findItemsByCache(items: List[(String,Int)], size: Int, cache: mutable.Map[Int, List[List[(String,Int)]]]): List[List[(String,Int)]] = {

    //从缓存中读取数据
    cache.get(size).orElse {
      //不存在，直接获取
      val result = findItemSets(items,size - 1, cache)

      //更新缓存
      cache += size -> result

      //返回数据
      Some(result)
    }.get
  }

  /**
   * 构建项集基于items商品列表，项集中的商品是size指定
   * @param items 商品列表 ：eg：[A,B,C]
   * @param size  最终项集包含商品的数量
   * @param cache
   * @return
   */
  def findItemSets(items: List[(String,Int)], size: Int, cache: mutable.Map[Int, List[List[(String,Int)]]]): List[List[(String,Int)]] = {

    if(size == 1){
      //items中的每个商品都是一个项集
      items.map(_ :: Nil)
    }else{
      //当size不是1的时候
      //1.获取项集大小为size-1的项集列表
      val tmpItems = findItemsByCache(items,size,cache)

      //2.给tmpItems中添加一个新的不重复的项 ====》数据转换
      val newItems:List[List[(String,Int)]] = tmpItems.flatMap(tpitem => {
        //2.1 给tmpItems添加一个新的商品ID，要求不重复
        items
          //将包含的商品过滤掉
          .filter(!tpitem.contains(_))
          //将商品添加到项集中，产生一个新的项集
          .map(item => (item :: tpitem))
      })

      //返回值
      newItems
    }
  }
}