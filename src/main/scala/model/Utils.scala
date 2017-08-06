package model

import java.awt.Image
import java.io.File
import java.net.URL
import javax.swing.ImageIcon

/**
  * Created by chiaravarini on 01/07/17.
  */
object Utils {



  val IMAGES_BASE_PATH = "/characters/"
  val IMAGES_EXTENSION = ".png"

  def getResource(path: String): URL = Utils.getClass.getResource(path)   //TODO lanciare eccezione nel caso in cui non trovi la risorsa!

  def getImage(path: String): Image = {
    val completePath: String = IMAGES_BASE_PATH + path + IMAGES_EXTENSION
    new ImageIcon(getResource(completePath)).getImage
  }

  def getJavaList[E](list: List[E]): java.util.List[E] = {
    import scala.collection.JavaConverters._
    list.asJava
  }

  def transformInString (array: Array[Char]): String = {
    var res = ""
    array.toSeq.foreach(c=> res += c)
    res
  }

  def getScalaMap[A,B](map: java.util.Map[A,B]): scala.collection.mutable.Map[A,B] = {
    import scala.collection.JavaConverters._
    map.asScala
  }


  def getImageForPlayground(x: File): Image = ???
}
