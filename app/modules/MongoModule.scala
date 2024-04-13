package modules

import com.google.inject.AbstractModule
import org.mongodb.scala.MongoClient

class MongoModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[MongoClient]).toInstance(MongoClient())
  }
}