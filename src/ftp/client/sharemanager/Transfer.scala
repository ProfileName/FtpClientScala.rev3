package ftp.client.sharemanager

import java.nio.file.Path

import ftp.client.filesystem.FileDescriptor

/**
 * defini�rt of er een upload of download bericht voor de transferManager-actor.
 */
abstract class Transfer[T](protected val files: List[T]) 
{
  /**
   * geeft een lijst van bestanden die moeten worden overgedragen.
   *
   * @geeft een lijst van bestanden terug die moeten worden overgedragen.
   */
  def getFiles() = files
}

/**
 * defini�rt upload overdracht berichten.
 * @param files de lijst van bestanden die moeten worden geupload.
 */
case class Upload(override protected val files: List[Path]) extends Transfer(files) 
{

}
/**
 * defini�rt download overdracht berichten.
 * @param files de lijst van bestanden die moeten worden gedownload.
 * @param dest de map waar de gedownloade bestand naar toe moet.
 */
case class Download(override protected val files: List[FileDescriptor], val dest: String) extends Transfer(files) 
{

}
/**
 * defini�rt exit voor de TransferManager.
 *
 */
case class Exit() 
{

}