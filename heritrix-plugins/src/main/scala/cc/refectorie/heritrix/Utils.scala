package cc.refectorie.heritrix

object Utils {
  def maybe[T](t: T): Option[T] = if (t != null) Some(t) else None

  def uniqList[T <: Ordered[T]](seq:Seq[T]): Seq[T] = {
    val s = Set(seq:_*)
    s.toList
  }

}
