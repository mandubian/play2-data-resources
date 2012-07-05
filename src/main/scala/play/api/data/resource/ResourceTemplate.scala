package play.api.data.resource

import play.api.libs.json._
import play.api.libs.concurrent.Promise
import play.api.libs.iteratee.Enumerator


trait ResourceTemplate[T] {
  def insert(t: T): Promise[ResourceResult[T]]
  def findOne(json: JsValue): Promise[ResourceResult[T]]
  def find(json: JsValue): Promise[ResourceResult[Enumerator[T]]]
  //def update(s: S): T
  //def delete(s: S)
  //def getBatch(s: Enumerator[S]): Enumerator[T]
}