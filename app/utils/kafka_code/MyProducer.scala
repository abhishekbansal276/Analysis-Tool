package utils.kafka_code

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

class MyProducer {
  import scala.util.Using

  def createTopicAndInsertData(topicName: String, filePath: String)(implicit ec: ExecutionContext): Future[Boolean] = Future {
    Using.resource(Source.fromFile(filePath)) { source =>
      var producer: KafkaProducer[String, String] = null
      try {
        val producerProps = new Properties()
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)

        producer = new KafkaProducer[String, String](producerProps)

        source.getLines().foreach { line =>
          val record = new ProducerRecord[String, String](topicName, line)
          producer.send(record)
        }
        true
      } catch {
        case ex: Exception =>
          Console.println(s"Error: ${ex.getMessage}")
          false
      } finally {
        if (producer != null) producer.close()
      }
    }
  }

  import scala.concurrent.Future

  def sendDataToTopic(topicName: String, data: String)(implicit ec: ExecutionContext): Future[Boolean] = Future {
    val producerProps = {
      val props = new Properties()
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
      props
    }

    val producer = new KafkaProducer[String, String](producerProps)

    try {
      val record = new ProducerRecord[String, String](topicName, data)
      producer.send(record)
      true
    } catch {
      case ex: Exception =>
        Console.println(s"Error sending data to topic $topicName: ${ex.getMessage}")
        false
    } finally {
      producer.close()
    }
  }
}