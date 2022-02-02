package ftp.client

import ftp.client.filesystem.FileDescriptor
import java.nio.file.Path

/**
 * beschrijving van de methodes die een ftp-client nodig heeft.
 *
 * FTP is een synchrone protocol dus je can verzenden & ontvangen methodes.
 */
trait FtpClient 
{
  /**
   * grote van de output-buffer in bytes.
   */
  protected val BUFFER_SIZE: Int = 1024 * 64

  /**
   * verbind de client met de server 
   *
   * @param username de gebruikers naam voor authenticatie 
   * @param password het wachtwoord voor authenticatie
   */
  def connect(username: String, password: String): Boolean
  /**
   * maakt de connectie aan via anonymous authenticatie
   */
  def connectAnonymous() = connect("anonymous", "anon")

  /**
   * verbreekt de connectie tussen de client en server
   */
  def disconnect(): Boolean
  /**
   * veranderd de map naar het gegeven map pad
   * 
   * @return het nieuwe echte pad
   */
  def cd(path: String): String
  def changeDirectory(path: String) = cd(_)
  /**
   * maakt een lijst van wat er in de map zit
   * 
   * @bekijk [[ftp.client.filesystem.FileDescriptor]]
   * @geeft een Lijst[FileDescriptor] van wat er in de map zit 
   * 
   * 
   */
  def ls(): List[FileDescriptor]
  def list() = ls()
  def listFiles() = ls()
  /**
   * geeft de huidige map.
   */
  def pwd(): String
  /**
   * verstuurd het geselecteerde bestaand naar de server.
   * 
   * @param filename de filename
   * @return '''true''' als de transmissie een succes is,  zoniet <br/>'''false'''
   */
  def sendFile(filename: String): Boolean
  def sendFile(file: Path): Boolean = sendFile(file.toAbsolutePath.toString)
  /**
   *ontvangt de file van de server
   * 
   * @param filename het bestand dat gedownload moet worden
   * @return ''''true''' als de transmissie een succes is,  zoniet <br/>'''false'''
   */
  def receiveFile(filename: String, dest: String): Boolean
  def receiveFile(file: Path, dest: Path): Boolean = receiveFile(file.toAbsolutePath.toString, dest.toAbsolutePath.toString)
  /**
   * geeft informatie of de server.
   *
   * @geeft de informatie van de server terug in een string vorm
   */
  def getServerInformation(): String
  /**
   * geeft de server informatie terug als key -> value map
   * @bekijk [[ftp.client.FtpClient#getServerInformation()]]
   */
  def getServerInformationAsMap(): Map[String, String] =
    getServerInformation().split("\n").flatMap(line =>
      if (line.contains("=")) 
      {
        val pairs = line.split("=")
        Some((pairs(0), pairs(1)))
      } else None).toMap

  /**
   * geeft informatie of de client.
   * 
   * @return the information about the client in string-representation
   * @geeft de informatie van de client terug in een string vorm
   */
  def getClientInformation(): String

  /**
   * geeft de client informatie terug als key -> value map
   * @bekijk [[ftp.client.FtpClient#getClientInformation()]]
   */
  def getClientInformationAsMap(): Map[String, String] =
    getClientInformation().split("\n").flatMap(line =>
      if (line.contains("=")) 
      {
        val pairs = line.split("=")
        Some((pairs(0), pairs(1)))
      } 
      else None).toMap

  /**
   * veranderd de transfer-mode. Het is active of passive.
   * 
   * @pararm active true wanneer in active mode, false wanneer in passive-mode
   * 
   */
  def changeMode(active: Boolean): Boolean
  /**
   * verander de naam van het bestand
   *
   * @param oldPath het huidige pad van het bestand
   * @param newPath het nieuwe pad van het bestand
   * 
   *    
   *    */
  def renameFile(oldPath: String, newPath: String): Boolean
  /**
   * verwijdert de geven '''file'''
   * 
   * <p>'''Uses:''' DELE <path></p>
   * @param path het pad van het bestand
   */
  def deleteFile(path: String): Boolean
  /**
   * 
   * verwijderd het gegeven '''directory'''.
   *
   * '''Uses:''' RMD <path>
   * @param path het pad naar de map
   */
  def deleteDir(path: String): Boolean
  /**
   * maakt een nieuwe map aan.
   *
   * <p>'''Uses:''' MKD <path></p>
   * @param path het pad naar de map
   */
  def mkdir(path: String): Boolean
  /**
   * Maakt een nieuwe map aan.
   * @param path het pad naar de map
   * @bekijk [[ftp.client.FtpClient#mkdir()]]
   */
  def createDirectory(path: String) = mkdir(_)
  /**
   * Maakt een nieuwe map aan.
   * @bekijk [[ftp.client.FtpClient#mkdir()]]
   */
  def makeDirectory(path: String) = mkdir(_)
  /**
   * Stops the actual executed process and quits it.
   * stopt de huidige processing die worden uit gevoerd
   */
  def quit()
}
