package models

import org.specs2.mutable.Specification
import org.specs2.mock.Mockito
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CoreModelsTest extends Specification with Mockito {
  "Build" should {
    "be ordered correctly" in {
      val orderedList = Seq(
        Build("", "", "1.1", Seq.empty, "", null),
        Build("", "", "19.1", Seq.empty, "", null),
        Build("", "", "2.33", Seq.empty, "", null),
        Build("", "", "2.3-test", Seq.empty, "", null),
        Build("", "", "2.3-abc", Seq.empty, "", null)
      ).sorted(util.Util.BuildOrdering)

      orderedList(4).version must be("1.1")
      orderedList(3).version must be("2.3-abc")
      orderedList(2).version must be("2.3-test")
      orderedList(1).version must be("2.33")
      orderedList(0).version must be("19.1")
    }
  }
}
