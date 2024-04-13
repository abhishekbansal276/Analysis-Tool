package utils.kafka_code

import java.util.Properties
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.common.TopicPartition

import scala.jdk.CollectionConverters._
import java.time.Duration

class MyConsumer {
  def readFromTopic(topicName: String): List[String] = {
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "my_group")
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    var consumer: KafkaConsumer[String, String] = null
    try {
      consumer = new KafkaConsumer[String, String](props)
      consumer.subscribe(List(topicName).asJava)

      Console.println(topicName)

      val result = scala.collection.mutable.ListBuffer[String]()

      while (true) {
        val records = consumer.poll(Duration.ofMillis(1000))
        val recordIterator = records.iterator()
        while (recordIterator.hasNext) {
          val record = recordIterator.next()
          result += record.value()
        }
        if (records.isEmpty)
          return result.toList
      }
      result.toList
    } catch {
      case ex: Exception =>
        println(s"Error fetching data from Kafka topic: ${ex.getMessage}")
        ex.printStackTrace()
        List.empty[String]
    } finally {
      if (consumer != null) consumer.close()
    }
  }

  import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
  import java.util.Properties

  def readFromTopicIterator(topicName: String): Iterator[String] = {
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "your-group-id")
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")

    val consumer = new KafkaConsumer[String, String](props)
    consumer.subscribe(Seq(topicName).asJava)

    val records = consumer.poll(1000)
    val recordIterator = records.iterator().asScala.map(_.value())

    consumer.close()
    recordIterator
  }

}