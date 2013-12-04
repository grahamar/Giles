import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import util.EmailUtil

@RunWith(classOf[JUnitRunner])
class EmailTest extends Specification with Mockito {
  "Email" should {
    "send" in {
      EmailUtil.sendEmailTo("grhodes@gilt.com", "This is a test", "This is test content")
      "gilt.com" must_== "gilt.com"
    }
  }
}
