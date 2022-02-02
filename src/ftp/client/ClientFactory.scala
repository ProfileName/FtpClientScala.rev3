package ftp.client

import java.io.PrintWriter
import java.net.{ InetAddress, Socket }
import java.util.Scanner

import ftp.response.Receivable

/**
 * deze factory maakt ftp-clients aan.
 * 
 * alle methodes die het Object type FtpClient terug geven.
 */
object ClientFactory 
{
  /**
   * maakt een simple basis client aan voor standard gebruik.
   * 
   * het gebruikt de gegeven servername en port for de connectie met de server.
   * 
   * @param serverName de servername of het ip address
   * @param port het port numer voor de control socket
   * @param rc het te ontvangen obeject die alle accepts en interogates 
   * 
   */
  def newBaseClient(serverName: String, port: Int, rc: Receivable): FtpClient = 
  {
    var sckt = new Socket(InetAddress.getByName(serverName), port)
    var scanner = new Scanner(sckt.getInputStream)
    var writer = new PrintWriter(sckt.getOutputStream, true)

    rc.status("Socket connect: " + scanner.nextLine)
    return new BaseClient(sckt, writer, scanner, rc)
  }

  /** 
   * maakt een nieuwe simple base client voor standaard gebruik
   * 
   * deze functie gebruikt de standaart control port 21 voor het control  socket port
   * 
   * @implnode deze methode roept de newBaseClient(serverName,21,rc)
   * 
   * @param serverName de servername of het ip address
   * @param rc de te ontvangen object.
   */
  def newBaseClient(serverName: String, rc: Receivable): FtpClient = newBaseClient(serverName, 21, rc)
}
