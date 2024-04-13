import com.google.inject.AbstractModule
import modules.MongoModule

class Module extends AbstractModule {
  override def configure(): Unit = {
    install(new MongoModule)
  }
}