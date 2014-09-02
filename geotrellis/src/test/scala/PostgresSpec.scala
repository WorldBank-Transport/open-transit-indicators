package opentransitgt

import com.typesafe.config.{ConfigFactory,Config}
import org.scalatest._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.{Database, Session}
import scala.sys.process._

// trait that sets up a fresh database for each spec, and drops it afterwards
trait PostgresSpec extends Suite with BeforeAndAfterAll {
  // use the same username and password, but a different database
  val config = ConfigFactory.load
  val dbUser = config.getString("database.user")
  val dbPassword = config.getString("database.password")

  // set the dbname to the name of the testing spec, so we can run multiple test
  // specs in parallel without any interference
  val dbName = getClass.getSimpleName.toLowerCase

  // connection to the test database
  val db = Database.forURL(s"jdbc:postgresql:$dbName", driver = "org.postgresql.Driver",
    user = dbUser, password = dbPassword)

  // connection to the postgres database -- used for dropping the test database
  val postgres = Database.forURL("jdbc:postgresql:postgres", driver = "org.postgresql.Driver",
    user = dbUser, password = dbPassword)

  // database initialization
  postgres withSession { implicit session: Session =>
    // drop the test database if it exists -- we want a fresh one for each spec
    Q.updateNA(s"DROP DATABASE IF EXISTS $dbName").execute

    // initialize the test database via the setup_db script
    s"sudo -u postgres ../deployment/setup_db.sh $dbName $dbUser $dbPassword ..".!!
  }

  // after all tests have been run in the spec, drop the test database
  override def afterAll() {
    postgres withSession { implicit session: Session =>
      Q.updateNA(s"DROP DATABASE $dbName").execute
    }
  }
}
