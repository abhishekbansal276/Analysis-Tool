package utils.kafka_code

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecords, KafkaConsumer, OffsetResetStrategy}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringDeserializer

import java.time.Duration
import java.util.{Collections, Properties}
import scala.concurrent.duration._
import scala.jdk.CollectionConverters.IteratorHasAsScala

object AkkaTesting extends App{
  implicit val system = ActorSystem("KafkaProducerWithApi")
  implicit val executionContext = system.dispatcher
  Class.forName("com.mysql.cj.jdbc.Driver")
  val props = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  val producer=new KafkaProducer[String, String](props)
  val consumerProps = new Properties()
  consumerProps.put("bootstrap.servers", "localhost:9092")
  consumerProps.put("group.id", "my-consumer-group")
  consumerProps.put("key.deserializer", classOf[StringDeserializer].getName)
  consumerProps.put("value.deserializer", classOf[StringDeserializer].getName)
  consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetResetStrategy.EARLIEST.toString.toLowerCase)
  val consumer = new KafkaConsumer[String, String](consumerProps)
  consumer.subscribe(Collections.singletonList("akka_testing"))

  val filePath = "app/kafka_code/MongoModule.scala.txt"
  val scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1)
  scheduler.scheduleAtFixedRate(new Runnable {
    def run(): Unit={
      try {
        val lines=scala.io.Source.fromFile(filePath).getLines()
        val data=lines.map(_.toDouble).toSeq
        val maxValue=data.max
        val minValue=data.min
        val avgValue=data.sum / data.length
        val timestamp=System.currentTimeMillis()
        val processedMessage = s"Max: $maxValue, Min: $minValue, Avg: $avgValue, Timestamp: $timestamp"
        val record = new ProducerRecord[String, String]("akka_testing", processedMessage)
        producer.send(record)
      } catch{
        case e: Exception => println(s"Error processing data: ${e.getMessage}")
      }
    }
  }, 0, 1, java.util.concurrent.TimeUnit.HOURS)
  val route =
    path("data")
    {
      get {
        val records: ConsumerRecords[String, String] = consumer.poll(Duration.ofSeconds(1))
        val kafkaData = records.iterator().asScala.map(_.value()).mkString("<br>")
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>Data from Kafka</h1><p>$kafkaData</p>"))
      }
    }
  val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)
  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  Thread.sleep(Long.MaxValue)
  bindingFuture
    .flatMap(_.unbind()).onComplete(_ => {
      consumer.close()
      system.terminate()
    })
}