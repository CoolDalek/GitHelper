package effects

private[effects] trait CryptoCompanion[CryptoTypeclass[_]] extends Summoner[CryptoTypeclass] {

  final val Utf8 = "UTF-8"

}