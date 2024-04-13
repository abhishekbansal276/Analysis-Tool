package modules

import com.google.inject.AbstractModule
import akka.actor.ActorSystem

class AppModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[ActorSystem]).toInstance(ActorSystem("MyActorSystem"))
  }
}