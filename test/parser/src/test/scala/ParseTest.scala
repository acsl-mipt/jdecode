import org.scalatest.{FlatSpec, Matchers}
import ru.mipt.acsl.decode.parser.ModelRegistry

/**
  * @author Artem Shein
  */
class ParseTest extends FlatSpec with Matchers {
  "A Parser" should "parse test.decode" in {
    ModelRegistry.registry
  }
}
