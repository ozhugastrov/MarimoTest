package inc.zhugastrov.marimo.k8s.utils

sealed trait OperationResult
object OperationSuccessful extends OperationResult
case class OperationFailed(reason: String, ex: Option[Throwable] = None) extends OperationResult
case class OperationFailedNotFound(reason: String) extends OperationResult
