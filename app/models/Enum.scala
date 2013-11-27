package models

trait Enum[A] {
  trait Value { self: A =>
    _values :+= this
  }
  private var _values = Seq.empty[A]
  def values = _values
  def apply(name: String): Option[A] = {
    values.find(_.toString.equals(name))
  }
  def unapply(value: A): String = {
    value.toString
  }
}
