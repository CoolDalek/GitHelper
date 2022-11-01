package effects

private[effects] trait CryptoCompanion[CryptoTypeclass[_]] extends Summoner[CryptoTypeclass]:
  inline val Utf8 = "UTF-8"
