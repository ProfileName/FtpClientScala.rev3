package ftp.client

import java.io.{ BufferedOutputStream, FileOutputStream, PrintWriter }
import java.net.{ InetAddress, Socket, SocketException }
import java.nio.file.{ Files, Paths }
import java.util.Scanner
import scala.util.Properties
import ftp.client.filesystem.{ FileDescriptor, RemoteFile }
import ftp.response.Receivable
import com.jcraft.jsch._

/**
 * basis ftpclient.
 */
class BaseClient private[client] (private val socket: Socket, private val output: PrintWriter, private val incoming: Scanner, private val receiver: Receivable) extends FtpClient {
  private var passiveMode: Boolean = true //true wanneer in passiveMode, in activeMode is false 
  private var dataSocket: Socket = null //Socket voor data connectie 
  private var dataInput: Scanner = null
  private var dataOutput: PrintWriter = null
  private var actualDir: String = "" //holds the path to the actual directory
  private val lg: Log = new Log(receiver)

  /**
   * methode om gewoon nextline aan te roepen in plaats van incoming.nextLine.
   */
  private def nextLine(): String = incoming.nextLine
  /**
   * methode om gewoon writeLine in plaats van output.println
   */
  private def writeLine(msg: String): Unit = output.println(msg)
  /**
   * om de connectie met de datasocket met een (nieuwe) serversocket
   */
  private def setupPassiveConnection(): Boolean = 
  {
    writeLine("PASV")
    var resp = nextLine

    
    //stopt de setup als het niet lukt om passive mode aan de praat te krijgen.
    lg.newMsg(resp, x => x.startsWith("227"))
    if (!lg.getLastResult) return false;

    //response is (ip,ip,ip,ip,portSub1,portSub2) --> ip = ip.ip.ip.ip | port = portSub1*256+portSub2
    //haalt de ip en port met regex
    var matcher = "\\(.*\\)".r
    resp = matcher.findFirstIn(resp).get
    resp = resp.substring(1, resp.length - 1)

    //match the ip-segments & port
    //vergelijk de ip gedeeltes & port
    val ip = resp.split(",").dropRight(2).reduce((res, current) => res + "." + current)
    val port = resp.split(",").drop(4).map(s => s.toInt).reduce((res, current) => res * 256 + current)
    receiver.status("Trying to connect to: " + ip + " on Port: " + port)

    dataSocket = new Socket(InetAddress.getByName(ip), port)
    dataInput = new Scanner(dataSocket.getInputStream)
    dataOutput = new PrintWriter(dataSocket.getOutputStream(), true)
    return true;
  }

  /**
   * sluit de dataSocket en alle dataStreams af.
   */
  private def closeDataSocket(): Unit = 
  {
    try 
    {
      dataSocket.getOutputStream.flush
      dataSocket.getOutputStream.close
      dataSocket.getInputStream.close
      dataSocket.close
    } catch 
    {
      case e: SocketException => {}
    }

    dataSocket = null
  }

   
  override def connect(username: String, password: String): Boolean = 
  {
    var resp = ""
    writeLine("USER " + username)

    resp = nextLine
    lg.newMsg(resp, x => x.startsWith("331"))
    if (!lg.getLastResult) return false;

    writeLine("PASS " + password)

    resp = nextLine
    lg.newMsg(resp, x => x.startsWith("230"))

    //als het lukt -> sla de echte directory op
    if (lg.getLastResult)
      actualDir = pwd()

    return lg.getLastResult

  }
   def disconnectSSH(): Boolean = 
  {
    return lg.getLastResult
  }
  override def disconnect(): Boolean = 
  {
    var resp = ""
    writeLine("QUIT")

    resp = nextLine
    lg.newMsg(resp, x => x.startsWith("221"))

    output.close()
    incoming.close()
    socket.close()

    return lg.getLastResult
  }
  override def cd(path: String): String = 
  {
    var resp = ""
    writeLine("CWD " + path)

    resp = nextLine
    lg.newMsg(resp, x => x.startsWith("250"))
    if (lg.getLastResult)
      actualDir = pwd()

    return actualDir;
  }
  /**
   * @bekijk ftp.client.Ftpclient#ls()
   * deze ls implementatie gebruikt het einde van het bestand of het een bestand is of een map.
   */
  override def ls(): List[FileDescriptor] = 
  {
    if (dataSocket == null)
      changeMode(false)

    writeLine("NLST")
    var respCtrl = nextLine

    lg.newMsg(respCtrl, x => x.startsWith("150"))
    if (!lg.getLastResult) return List()

    var response = List[FileDescriptor]()

    //scala magie(berijp het nog niet helemaal)
    while (dataInput.hasNext)
      dataInput.nextLine match {
     
        case x if (x.matches(".*([.]).*"))  => response = response :+ new RemoteFile(actualDir + "/" + x, false)
        case x if (!x.matches(".*([.]).*")) => response = response :+ new RemoteFile(actualDir + "/" + x, true)
      }

    respCtrl = nextLine
    lg.newMsg(respCtrl, x => x.startsWith("226"))

    closeDataSocket()
    return response
  }
  override def pwd(): String = 
  {
    var resp = ""
    writeLine("PWD")
    resp = nextLine

    lg.newMsg(resp, x => x.startsWith("257"))
    if (!lg.getLastResult) return "ERROR"
    else {
      lg.newMsg(resp)

      val matcher = "\\\".*\\\"".r
      var path = matcher.findFirstIn(resp).get
      path = path.substring(1, path.length - 1)
      return (path)
    }
  }
  override def sendFile(filename: String): Boolean = 
  {
    if (dataSocket == null)
      changeMode(false)

    val outputStream = new BufferedOutputStream(dataSocket.getOutputStream)
    val file = Paths.get(filename)
    val fileStream = Files.newInputStream(file)

    if (!Files.exists(file)) {
      lg.newError("File " + file.toString() + " doesn't exist")
      return false;
    }

    writeLine("STOR " + file.getFileName.toString)
    var resp = nextLine

    lg.newMsg(resp, x => x.startsWith("150"))

    var buffer = new Array[Byte](BUFFER_SIZE)
    var length: Int = 0

    //gebruikt "tail-recursion"
    //@tailrec
    def writeBytes(): Unit = if (length != -1) {
      length = fileStream.read(buffer)
      outputStream.write(buffer)
      writeBytes()
    }
    writeBytes()

    outputStream.flush
    outputStream.close
    fileStream.close

    resp = nextLine
    if (!resp.startsWith("226")) 
    {
      receiver.error(resp + "\nCan't upload " + file.getFileName.toString)
      return false
    } else 
    {
      receiver.status(resp)
      receiver.status("Upload of " + file.getFileName.toString + " was successfull.")
    }

    closeDataSocket()

    return true;
  }
  override def receiveFile(filename: String, dest: String): Boolean = 
  {
    if (dataSocket == null)
      changeMode(false)

    val canonicalFilename = if (filename.startsWith("/")) filename else actualDir.concat("/" + filename)
    val localFile = Paths.get(dest)
    val incomingStream = dataSocket.getInputStream
    val localStream = new FileOutputStream(localFile.toFile())

    writeLine("RETR " + filename)
    var resp = nextLine

    lg.newMsg(resp, x => x.startsWith("150"))

    var length: Int = 0

    //gebruikt tail-recursion
   // @tailrec
    def readBytes(): Unit = if (length != -1) 
    {
      
      length = incomingStream.read()
      localStream.write(length)
      readBytes()
    }
    readBytes()

    localStream.flush
    localStream.close

    resp = nextLine
    lg.newMsg(resp, x => x.startsWith("226"))
    val res = lg.getLastResult

    closeDataSocket()
    return res
  }
  override def getServerInformation(): String = 
  {
    val sb = new StringBuilder()
    sb ++= "Hostname=" + this.socket.getInetAddress.getHostName + "\n"
    sb ++= "IP-Address=" + this.socket.getInetAddress + "\n"
    sb ++= "(Ftp) Control Port=" + this.socket.getRemoteSocketAddress + "\n"
    if (this.dataSocket != null) sb ++= "(Ftp) Data Port=" + this.dataSocket.getRemoteSocketAddress.toString + "\n"

    return sb.toString
  }
  override def getClientInformation(): String = 
  {
    val sb = new StringBuilder()
    val inet = InetAddress.getLocalHost
    sb ++= "OS=" + Properties.osName + "\n"
    sb ++= "Hostname=" + inet.getHostName + "\n"
    sb ++= "Username=" + Properties.userName + "\n"
    sb ++= "IP-Address=" + inet.getHostAddress + "\n"
    sb ++= "(Ftp) Control Port=" + this.socket.getPort() + "\n"
    if (this.dataSocket != null) sb ++= "(Ftp) Data Port=" + this.dataSocket.getPort() + "\n"

    return sb.toString
  }
  override def changeMode(active: Boolean): Boolean = 
  {
    if (active) 
    {

    } 
    else setupPassiveConnection()

    passiveMode = active
    return passiveMode
  }

  override def renameFile(oldPath: String, newPath: String): Boolean = 
  {
    var op = if (oldPath.startsWith("/")) oldPath else actualDir.concat(oldPath)
    var np = if (newPath.startsWith("/")) newPath else actualDir.concat(newPath)

    writeLine("RNFR " + op)
    var resp = nextLine
    if (resp.startsWith("350")) receiver.status(resp)
    else receiver.error(resp)

    writeLine("RNTO " + np)
    resp = nextLine

    lg.newMsg(resp, x => x.startsWith("250"))
    return lg.getLastResult

  }

  override def deleteFile(path: String): Boolean = 
  {
    val p = if (path.startsWith("/")) path else actualDir.concat(path)

    writeLine("DELE" + p)
    val resp = nextLine
    lg.newMsg(resp, x => x.startsWith("250"))
    return lg.getLastResult
  }

  override def deleteDir(path: String): Boolean = 
  {
    val p = if (path.startsWith("/")) path else actualDir.concat(path)

    writeLine("RMD" + p)
    val resp = nextLine
    lg.newMsg(resp, x => x.startsWith("250"))
    return lg.getLastResult
  }

  override def mkdir(path: String): Boolean = 
  {
    val p = if (path.startsWith("/")) path else actualDir.concat(path)

    writeLine("MKD" + p)
    val resp = nextLine
    lg.newMsg(resp, x => x.startsWith("250"))
    return lg.getLastResult
  }

  override def quit() = 
  {
    writeLine("ABOR")
    val resp = nextLine

  }
}
