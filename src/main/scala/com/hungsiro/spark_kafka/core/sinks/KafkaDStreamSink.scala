package sinks
import org.apache.spark.{SparkContext, TaskContext}
import org.apache.spark.streaming.dstream.DStream
import com.hungsiro.spark_kafka.core.KafkaPayLoad
import com.hungsiro.spark_kafka.core.KafkaProducerFactory
import org.apache.log4j.Logger
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, RecordMetadata}
import org.apache.spark.broadcast.Broadcast
/**
  * Created by hungdv on 10/03/2017.
  */
class KafkaDStreamSink(dstream: DStream[KafkaPayLoad]) extends Serializable{

  def sendToKafka(config: Map[String,Object],topic: String):Unit ={
 /* def sendToKafka(configs: Map[String,Object],topic: String,sc: SparkContext):Unit ={
    val sink: KafkaProducer[Array[Byte], Array[Byte]] = KafkaProducerFactory.getOrCreateProducer(configs)
    val producer: Broadcast[KafkaProducer[Array[Byte], Array[Byte]]] = sc.broadcast(sink)*/

    dstream.foreachRDD{ rdd =>
      rdd.foreachPartition{ records =>

        val producer = KafkaProducerFactory.getOrCreateProducer(config)

        val context = TaskContext.get
        val logger  = Logger.getLogger(getClass)

        val callback = new KafkaDStreamSinkExceptionHandler
        logger.debug(s"Send Spark partition: ${context.partitionId()} to Kafka topic : $topic")

        val metadata = records.map{ record =>
          callback.throwExceptionIfAny()
          producer.send(new ProducerRecord(topic,record.key.orNull,record.value), callback)
          /*producer.value.send(new ProducerRecord(topic,record.key.orNull,record.value), callback)*/
        }.toList

        logger.debug(s"Flush Spark partition: ${context.partitionId} to Kafka topic: $topic")
        metadata.foreach{
          metadata => metadata.get()
        }

        callback.throwExceptionIfAny()
      }

    }
  }
}

object KafkaDStreamSink{
  import scala.language.implicitConversions

  implicit def createKafkaDStreamSink(dsStream: DStream[KafkaPayLoad]): KafkaDStreamSink = {
    new KafkaDStreamSink(dsStream)
  }
}