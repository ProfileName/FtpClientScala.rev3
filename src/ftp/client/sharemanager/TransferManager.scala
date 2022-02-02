
package ftp.client.sharemanager

import java.nio.file.Files

import scala.actors.Actor

import ftp.client.FtpClient
import ftp.response.Receivable
import ftp.ui.errorhandle.ErrorHandle

/**
 * deze manager brengt bestanden naar de ftpserven en downloads de bestanden van de ftpserver.
 *
 * het gebruikt upload-/download berichten voor de bestanden.
 * "als de client null is doet deze actor niks als standaart"
 *
 * @param ftpClient - de  FtpClient-Connection
 */
class TransferManager(private val ftpClient: FtpClient, private val rc: Receivable, private val exh: ErrorHandle) extends Actor 
{
  def act(): Unit = loop 
  {
    react 
    {
      
      case msg: Upload if (ftpClient != null) => 
        {
        msg.getFiles.foreach 
        {
          _ match 
          {
            case x if (Files.isDirectory(x)) => rc.status("Upload: Skipping directory: " + x + ". Can't send directorys.")
            case x if (Files.isRegularFile(x)) => 
              {
              rc.status("Upload: " + x.toString())
              exh.catching { ftpClient.sendFile(x.toAbsolutePath().toString()) }
            }
            case x if (!Files.isRegularFile(x)) => rc.status("Upload: Skipping: " + x + ". Is not a regular file.")
            case _                              => rc.error("Skipping: unknown file format.")
          }
        }
      } 
      case msg: Download if (ftpClient != null) => {
        msg.getFiles.foreach {
          _ match {
            case x if (x.isDirectory()) => rc.status("Download: Skipping directory: " + x + ". Can't receive directorys.")
            case x if (x.isFile()) => {
              val dest = msg.dest + "/" + x.getFilename()
              rc.status("Download: src: " + x.getAbsoluteFilename + " dest: " + dest)
              exh.catching { ftpClient.receiveFile(x.getAbsoluteFilename, dest) }
            }
            case _ => rc.error("Skipping: unknown file format.")
          }
        }
      } 
      case msg: Exit => this.exit()
    } 
  } 
}
