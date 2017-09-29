/**
 * Created by Administrator on 2017/9/25.
 */
import scala.collection.mutable
object GroupNum {

  def main(args: Array[String]) {
    val list = List("AF","B","C","D","E")
    val tp = List("B","C")

    print(list.toBuffer -- tp)

    val size = list.size

    //构建辅助对象
    val cache = mutable.Map[Int,List[List[String]]]()


    val allItemSets:List[List[String]] = (1 to size).map(size => {

      findSets(list, size, cache)

    }).foldLeft(List[List[String]]())((v1,v2) => {
      v1 ::: v2
    })

    val result = allItemSets.map(_.mkString(""))

//    result.foreach(vlu =>{
//      print(vlu+",")
//    })

  }

  def findSetsByCahce(items:List[String],size:Int,cache:mutable.Map[Int,List[List[String]]]): List[List[String]]={
      //获取缓存中数据
      cache.get(size).orElse{
      val result = findSets(items,size,cache)

      //更新缓存
      cache += size -> result

      //返回值
      Some(result)

    }.get


  }

  def findSets(items:List[String],size:Int,cache:mutable.Map[Int,List[List[String]]]): List[List[String]] ={

    if(size == 1){
      items.map(_::Nil)
    }else {
      //List(List(A), List(B), List(C), List(D))
      val tmpItems = findSetsByCahce(items, size - 1,cache)

      val newtmp = tmpItems.flatMap(ite => {

        items.filter(!ite.contains(_)).map(tp =>{

          (tp::ite).sorted
        })

      }).distinct
      newtmp
    }
  }

}

