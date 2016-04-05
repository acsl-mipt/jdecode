import org.scalatest.{FlatSpec, Matchers}
import ru.mipt.acsl.decode.model.domain.impl.component.message.ArrayRange
import ru.mipt.acsl.decode.model.domain.impl.types.ArraySize

/**
  * Created by metadeus on 05.04.16.
  */
class RangeTest extends FlatSpec with Matchers {

  "A range" should "compute correct result array size" in {
    var range = ArrayRange(1, Some(3))

    // 0 | 1 | 2 | 3 | 4 |
    // +++++++++++++++++++
    //   |---range---|

    range.size(ArraySize(0, 0)) shouldEqual ArraySize(0, 3)
    range.size(ArraySize(1, 0)) shouldEqual ArraySize(0, 3)
    range.size(ArraySize(2, 0)) shouldEqual ArraySize(1, 3)
    range.size(ArraySize(4, 0)) shouldEqual ArraySize(3, 3)
    range.size(ArraySize(5, 0)) shouldEqual ArraySize(3, 3)

    a [IllegalArgumentException] should be thrownBy {
      range.size(ArraySize(0, 10))
    }

    range = ArrayRange(0, None)
    range.size(ArraySize(0, 0)) shouldEqual ArraySize(0, 0)
    range.size(ArraySize(0, 4)) shouldEqual ArraySize(0, 4)
    range.size(ArraySize(3, 4)) shouldEqual ArraySize(3, 4)
    range.size(ArraySize(4, 4)) shouldEqual ArraySize(4, 4)
  }

}
