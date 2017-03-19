package output_storage.es

import org.apache.spark.streaming.dstream.DStream
import org.elasticsearch.spark._
import output_storage.StorageWriter

import scala.reflect.ClassTag
/**
  * Created by hungdv on 17/03/2017.
  */
/**
  * Persist DStream to Elastic Search.
  * Use supported driver by ES.
  * Don't need to rewrite any things.
  *
  * @param dstream
  *
  * @tparam T
  */
class ElasticSearchDStreamWriter[T: ClassTag](@transient private val dstream: DStream[T] )extends StorageWriter{
  private val defaultIndexName: String  = "spark_streaming_default_index"
  private val defaultTypeName: String   = "spark_streaming_default_type"

  override def persistToStorage(storageConfig: Map[String, String]): Unit = {
    val indexName     = storageConfig.getOrElse("index", defaultIndexName)
    val `typeName`      = storageConfig.getOrElse("type",defaultTypeName)
    println("index :  " + indexName)
    println("type :  " + `typeName`)
    dstream.foreachRDD{
      rdd =>
        rdd.foreach(println(_))
        rdd.saveToEs(indexName + "/" + `typeName`)
    }
    println("Saved to ES")
  }
}
object ElasticSearchDStreamWriter{
  import scala.language.implicitConversions
  implicit def createESWriter[T: ClassTag](dstream: DStream[T]): ElasticSearchDStreamWriter[T] = {
    new ElasticSearchDStreamWriter[T](dstream)
  }
}

