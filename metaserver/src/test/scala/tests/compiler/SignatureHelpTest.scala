package tests.compiler

import scala.concurrent.ExecutionContext.Implicits.global
import scala.meta.languageserver.compiler.SignatureHelpProvider
import langserver.types.SignatureHelp
import play.api.libs.json.Json

object SignatureHelpTest extends CompilerSuite {

  def check(
      filename: String,
      code: String,
      fn: SignatureHelp => Unit
  ): Unit = {
    targeted(
      filename,
      code, { pos =>
        val obtained = SignatureHelpProvider.signatureHelp(compiler, pos)
        fn(obtained)
      }
    )
  }

  def check(
      filename: String,
      code: String,
      expected: String
  ): Unit = {
    check(filename, code, { result =>
      val obtained = Json.prettyPrint(Json.toJson(result))
      assertNoDiff(obtained, expected)
    })
  }

  check(
    "assert",
    """
      |object a {
      |  Predef.assert<<(>>
      |}
    """.stripMargin,
    """
      |{
      |  "signatures" : [ {
      |    "label" : "assert(assertion: Boolean, message: => Any)Unit",
      |    "parameters" : [ {
      |      "label" : "assertion: Boolean"
      |    }, {
      |      "label" : "message: => Any"
      |    } ]
      |  }, {
      |    "label" : "assert(assertion: Boolean)Unit",
      |    "parameters" : [ {
      |      "label" : "assertion: Boolean"
      |    } ]
      |  } ],
      |  "activeParameter" : 0
      |}""".stripMargin
  )

  check(
    "multiarg",
    """
      |object b {
      |  Predef.assert("".substring(1, 2), <<msg>>
      |}
    """.stripMargin,
    """
      |{
      |  "signatures" : [ {
      |    "label" : "assert(assertion: Boolean, message: => Any)Unit",
      |    "parameters" : [ {
      |      "label" : "assertion: Boolean"
      |    }, {
      |      "label" : "message: => Any"
      |    } ]
      |  } ],
      |  "activeParameter" : 1
      |}
    """.stripMargin
  )

  check(
    "no-result",
    """
      |object c {
      |  assert(true)
      |  <<caret>>
      |}
    """.stripMargin, { obtained =>
      assert(obtained.signatures.isEmpty)
    }
  )

  check(
    "tricky-comma",
    """
      |object b {
      |  Predef.assert(","<<caret>>
      |}
    """.stripMargin, { obtained =>
      val activeParameter = obtained.activeParameter
      assert(activeParameter.nonEmpty)
      // TODO(olafur) should be 0 since the comma is quoted
      // we can fix this if we use the tokenizer, but then we have to handle
      // other tricky cases like unclosed string literals.
      assert(activeParameter.get == 1)
    }
  )
}
