package org.kum.mtg

object CardsData {

}

trait Mana {
  def totalCost: Short = 1
}

abstract class ColoredMana(val color: String) extends Mana { override def toString: String = s"{${color}}" }

case object WhiteMana extends ColoredMana("white")
case object BlueMana extends ColoredMana("blue")
case object BlackMana extends ColoredMana("black")
case object RedMana extends ColoredMana("red")
case object GreenMana extends ColoredMana("green")

case class ColorlessMana(val count: Short) extends Mana {
  override def toString: String = s"{$count}"

  override def totalCost: Short = count
}
case object SnowMana extends Mana { override def toString: String = "{snow}" }
case object PhyrexianAny extends Mana { override def toString: String = "{phyrexian}" }

case class HybridMana(one: ColoredMana, another: ColoredMana) {
  override def toString: String = s"{${one.color}/${another.color}"
}
case class Monocolored(mana: ColoredMana, cost: Int) { // cost -1 is an X
  override def toString: String = s"{$cost/${mana.color}}"
}
case class Phyrexian(mana: ColoredMana)

abstract class Rarity(val rarity: String) { override def toString: String = rarity }


case object UndefinedRatity extends Rarity("Undefined ratity")
case object BasicLandCard extends Rarity("Basic Land")
case object LandCard extends Rarity("Land")
case object CommonCard extends Rarity("Common")
case object UncommonCard extends Rarity("Uncommon")
case object RareCard extends Rarity("Rare")
case object MythicRareCard extends Rarity("Mythic Rare")
