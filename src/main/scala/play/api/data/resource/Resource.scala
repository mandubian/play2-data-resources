package play.api.data.resource

import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.json.JsPath
import play.api.data.validation._
import play.api.libs.concurrent.Promise

case class ResourceErrorMsg(key: String, message: String, args: Any*)

case class ResourceSuccess[T](value: T) extends ResourceResult[T]
case class ResourceValidationError[T](errors: Seq[ResourceErrorMsg]) extends ResourceResult[T]
case class ResourceOpError[T](errors: Seq[ResourceErrorMsg]) extends ResourceResult[T]

sealed trait ResourceResult[T] {
  def map[X](f: T => X): ResourceResult[X] = this match {
    case ResourceSuccess(t) => ResourceSuccess(f(t))
    case ResourceValidationError(e) => ResourceValidationError[X](e)
    case ResourceOpError(e) => ResourceOpError[X](e)
  }

  def flatMap[X](f: T => ResourceResult[X]): ResourceResult[X] = this match {
    case ResourceSuccess(t) => f(t)
    case ResourceValidationError(e) => ResourceValidationError[X](e)
    case ResourceOpError(e) => ResourceOpError[X](e)
  }

  def fold[X](
    errorValid: Seq[ResourceErrorMsg] => X, 
    errorOp: Seq[ResourceErrorMsg] => X, 
    success: T => X) = this match {
      case ResourceSuccess(v) => success(v)
      case ResourceValidationError(e) => errorValid(e)
      case ResourceOpError(e) => errorOp(e)
    }

  def foldValid[X](
    invalid: Seq[ResourceErrorMsg] => X, 
    valid: T => X) = this match {
      case ResourceSuccess(v) => valid(v)
      case ResourceValidationError(e) => invalid(e)
      case _ => sys.error("unexpected state")
    }

  def foldOp[X](
    error: Seq[ResourceErrorMsg] => X, 
    success: T => X) = this match {
      case ResourceSuccess(v) => success(v)
      case ResourceOpError(e) => error(e)
      case _ => sys.error("unexpected state")
    }
}

class Resource[T](tmpl: ResourceTemplate[T], 
                  inputTransform: T => T = { t: T => t }, 
                  outputTransform: T => T = { t: T => t },
                  queryTransform: T => T = { t: T => t })
                  (implicit reader: Reads[T], writer: Writes[T]) {

  def insert(json: JsValue): Promise[ResourceResult[T]] = {
    reader.reads(json).fold(
      invalid = { e => Promise.pure(ResourceValidationError(e.map{ case(path, errors) => errors.map( err => ResourceErrorMsg(path.toJsonString, err.message, err.args:_*) ) }.flatten)) },
      valid = { s => 
        tmpl.insert(s).map( _.foldOp(
          error = { e => ResourceOpError(e.map( e => ResourceErrorMsg(e.key, e.message, e.args:_*) )) },
          success = { e => ResourceSuccess(s) }
        ))
      }
    )
  }

  def findOne(json: JsValue): Promise[ResourceResult[T]] = {
    tmpl.findOne(json)
  }


  def find(json: JsValue): Promise[ResourceResult[Enumerator[T]]] = {
    tmpl.find(json)
  }

  def checking[A](c: (JsPath, Format[A]))(implicit v: Format[A]) = {
    new Resource(
      this.tmpl, 
      this.inputTransform, 
      this.outputTransform,
      this.queryTransform)(JsTupler(c) andThen this.reader, this.writer)
  }
  
  def transformInput( f: T => T ) = new Resource(this.tmpl, f, this.outputTransform, this.queryTransform)
  def transformOutput( f: T => T ) = new Resource(this.tmpl, this.inputTransform, f, this.queryTransform)
  def transformQuery( f: T => T ) = new Resource(this.tmpl, this.inputTransform, this.outputTransform, f)
}

object Resource {
  def apply[T](tmpl: ResourceTemplate[T])(implicit fmt: Format[T]) = new Resource[T](tmpl)

  def apply[T, A1](c1: (JsPath, Format[A1]))
                  (apply: Function1[A1, T])(unapply: Function1[T, Option[A1]])
                  (tmpl: ResourceTemplate[T])
                  (implicit fmtA1: Format[A1]): Resource[T] = {
    implicit val fmt = JsMapper(c1)(apply)(unapply)

    new Resource[T](tmpl)                
  }

  def apply[T, A1, A2](c1: (JsPath, Format[A1]), c2: (JsPath, Format[A2]))
                  (apply: Function2[A1, A2, T])(unapply: Function1[T, Option[(A1, A2)]])
                  (tmpl: ResourceTemplate[T])
                  (implicit fmtA1: Format[A1], fmtA2: Format[A2]): Resource[T] = {
    implicit val fmt = JsMapper(c1, c2)(apply)(unapply)

    new Resource[T](tmpl)                
  }

}



/**
 *
 * The Resource Controller to be plugged in your application
 *
 */
class ResourceController[T](res: Resource[T])(implicit fmtT: Format[T]) extends Controller {

  def insert = Action(parse.json) { implicit request =>
    Async {
      res.insert(request.body).map( _.fold(
        errorValid = { errors => BadRequest(errors.toString) },
        errorOp = { errors => BadRequest(errors.toString) },
        success = { value => Ok("inserted " + value) }
      ))
    }
  } 

  def findOne(q: String) = Action {implicit request =>
    val json = Json.parse(q)
    Async {
      /*val json = request.queryString.foldLeft(JsObject(Nil))( (all: JsObject, elt: (String, Seq[String])) => 
        all ++ (if(elt._2.length == 1 ) Json.obj(elt._1 -> Json.toJson(elt._2(0))) else Json.obj(elt._1 -> Json.toJson(elt._2)))
      )*/
      res.findOne(json).map( _.fold(
        errorValid = { errors => BadRequest(errors.toString) },
        errorOp = { errors => BadRequest(errors.toString) },
        success = { value => Ok(Json.toJson(value)) }
      ))
    }
  } 
}

