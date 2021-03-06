
package ftp.util

import java.nio.file.Paths
import java.nio.file.Files
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import java.nio.charset.Charset

/**
 * gebruikt ftpfx config voor het laden van de gespecifieerde taal settings.
 */
object ConfigObj 
{
  //path-separator
  val ps = System.getProperty("file.separator")
  private val defaultConfDescription = "ftpfx's default configuration file."
  private val defaultLangDescription = "ftpfx's default language file."
  private var configPath = Paths.get("rsc" + ps + "conf" + ps + "ftpfxDefault.conf")
  private var langPath = Paths.get("rsc" + ps + "lang" + ps + "ftpfx-en.conf")
  private val config: java.util.Properties = loadConfig()
  private val language: java.util.Properties = loadLanguage()

  private def loadConfig(): java.util.Properties = 
  {
    val conf: java.util.Properties = new java.util.Properties()
    if (!Files.exists(configPath)) 
    {
      DefaultValues.defaultConfKeys.foreach 
      { 
        case (key, value) => conf.setProperty(key, value) 
      }
      conf.store(Files.newOutputStream(configPath), defaultConfDescription)
    } 
    else 
    {
      conf.load(Files.newBufferedReader(configPath, Charset.forName("UTF-8")))
     
      if (!checkConfig(conf)) 
      {
        DefaultValues.defaultConfKeys.foreach 
        { 
          case (key, value) => conf.setProperty(key, value) 
        }
        conf.store(Files.newOutputStream(configPath), defaultConfDescription)
      }

      loadSpecifiedConfig(conf)
    }

    return conf
  }

  private def loadSpecifiedConfig(conf: java.util.Properties) = 
  {
    val langPath = conf.getProperty("language-file")
    if (langPath != null && langPath != "default") 
    {
      this.langPath = Paths.get(langPath)
    }
  }

  private def loadLanguage(): java.util.Properties = 
  {
    val prop: java.util.Properties = new java.util.Properties()
    if (!Files.exists(langPath)) 
    {
      DefaultValues.defaultLangKeys.foreach 
      { 
        case (key, value) => prop.setProperty(key, value) 
      }
      prop.store(Files.newOutputStream(langPath), defaultLangDescription)
    } 
    else 
    {
      prop.load(Files.newBufferedReader(langPath, Charset.forName("UTF-8")))
      if (!checkLanguage(prop)) 
      {
        DefaultValues.defaultLangKeys.foreach 
        { 
          case (key, value) => prop.setProperty(key, value) 
        }
        prop.store(Files.newOutputStream(langPath), defaultLangDescription)
      }
    }

    return prop
  }

  /**
   * haalt de config waarde van de gegeven sleutel.
   *
   * @param the sleutel voor de waarde
   * @return Some() als het (key,value)-pair bestaat, None als de sleutel niet bestaat
   */
  def getC(key: String) = key match 
  {
    case null            => None
    case "software-name" => Some(DefaultValues.swName)
    case "version"       => Some(DefaultValues.swVersion)
    case "port"          => Some(DefaultValues.port)
    case "anon-username" => Some(DefaultValues.anonUsername)
    case "anon-password" => Some(DefaultValues.anonPassword)
    case x: String => {
      val value = config.getProperty(x)
      if (value != null) Some(value)
      else None
    }
  }
  /**
   * haalt de taal waarde voor de gegeven sleutel.
   *
   * @param de sleutel voor de waarde
   * @return Some() als de (key,value)-pair exists, None als de sleutel niet bestaat
   * 
   */
  def getL(key: String) = key match 
  {
    case null            => None
    case "software-name" => Some(DefaultValues.swName)
    case "port"          => Some(DefaultValues.port)
    case x: String => {
      val value = language.getProperty(x)
      if (value != null) Some(value)
      else None
    }
  }

  /**
   * haalt de csss-file.
   */
  def getCss() =
    if (getC("theme").get == "default")
      getRsc(ps + "style" + ps + "FtpGui.css")
    else
      getRsc(getC("theme").get)

  def getRsc(path: String) = try 
  {
    Right(this.getClass.getResource(path).toExternalForm())
  } 
  catch 
  {
    case _: NullPointerException => Left("Can't load: " + path)
  }

  private def checkConfig(conf: java.util.Properties): Boolean = 
  {
    val origKeys = DefaultValues.defaultConfKeys.keySet
    val extractedKeys = conf.keySet().filter(x => x.isInstanceOf[String])
    return origKeys.forall 
    { 
      x => extractedKeys.contains(x) 
      
    }
  }

  private def checkLanguage(prop: java.util.Properties): Boolean = 
  {
    val origKeys = DefaultValues.defaultLangKeys.keySet
    val extractedKeys = prop.keySet().filter(x => x.isInstanceOf[String])
    return origKeys.forall 
    { 
      x => extractedKeys.contains(x) 
      
    }
  }
}

//==================================================================
//standaart sleutel waarde, als het bestand niet bestaat. 
private object DefaultValues 
{
  val swName = "SFTP Server"
  val swVersion = "1.0"
  val port = "Port"
  val anonUsername = "anonymous"
  val anonPassword = "anon"
  private val defaultLocalDir = System.getProperty("user.home")
  private val defaultDownloadDir = defaultLocalDir + ConfigObj.ps + "Downloads"

  /*
   * de waarde this/default is gebruikt als standaart waarde
   */
  val defaultConfKeys: Map[String, String] = Map(
    "config-file" -> "this",
    "language-file" -> "default",
    "language" -> "en",
    "theme" -> "default",
    "local-start-dir" -> defaultLocalDir,
    "download-dir" -> defaultDownloadDir)

  val defaultLangKeys: Map[String, String] = Map(
    //menues
    "file-menue" -> "File",
    "help-menue" -> "Help",
    //-- Filemenue
    "local-root" -> "Set local root...",
    "remote-root" -> "Set remote root...",
    "local-root-chooser-title" -> "Set local root directory",
    "remote-root-chooser-title" -> "Set remote root directory",
    "remote-root-chooser-header" -> "Setup the remote root directory.",
    "remote-root-chooser-content" -> "Please enter the new root path:",
    "exit" -> "Exit",
    //-- Helpmenue
    "client-information-item" -> "Client information",
    "server-information-item" -> "Server information",
    "about-item" -> "About...",
    //connect-header
    "servername" -> "Servername",
    "username" -> "Username",
    "password" -> "Password",
    "connect-btn" -> "Connect",
    "disconnect-btn" -> "Disconnect",
    "upload-btn" -> "Upload",
    "download-btn" -> "Download",
    "anonymous-login" -> "Login\nanonymously",
    //filesystem-view
    "local-filesystem-title" -> "Local Filesystem",
    "remote-filesystem-title" -> "Remote Filesystem",
    //filesystem treeview-entrys
    "default-remote-entry" -> "Not Connected.",
    //download-directory
    "download-dir" -> "Download directory:",
    "download-choose-entry" -> "Choose..",
    "download-chooser-title" -> "Set download directory",
    //Log-tabbar
    "loads-tab" -> "Up-/Downloads",
    "log-tab" -> "Log",
    //Client-informations
    "client-information-title" -> "Client information",
    "client-information-header" -> "Informations about this computer.",
    "client-information-content" -> "Below are informations about this local computer.",
    //Server-informations
    "server-information-title" -> "Server information",
    "server-information-header" -> "Informations about the connected FTP-Server.",
    "server-information-content" -> "Below are informations about the FTP-Server.")
}
