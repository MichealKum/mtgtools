package org.kum.mtg

object AppParams {

}

trait InfoType
case object UnknownInfoType extends InfoType
case object OracleInfo extends InfoType
case class  CardSetInfoType(name: String) extends InfoType