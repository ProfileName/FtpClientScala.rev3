package ftp.ui

import java.nio.file.{ Path, Paths }

import javafx.application.{ Application, Platform }
import javafx.collections.{ FXCollections, ObservableList }
import javafx.event.ActionEvent
import javafx.geometry.Side
import javafx.scene.Scene
import javafx.scene.control.{ Button, CheckBox, ComboBox, Menu, MenuBar, MenuItem, PasswordField, SelectionMode, Tab, TabPane, TextArea, TextField, TreeItem, TreeView }
import javafx.scene.input.{ KeyCode, KeyEvent }
import javafx.scene.layout.{ BorderPane, GridPane, HBox, Pane, VBox }
import javafx.scene.text.Text
import javafx.stage.{ DirectoryChooser, Stage }

import scala.collection.JavaConversions.asScalaBuffer

import ftp.client.{ ClientFactory, FtpClient }
import ftp.client.filesystem.{ FileDescriptor, RemoteFile, WrappedPath, SshRemoteFile }
import ftp.client.sharemanager.{ Download, Exit, TransferManager, Upload }
import ftp.response.MessageHandler
import ftp.ui.FxEventHandlerImplicits.{ actionEvent2EventHandler, keyEvent2EventHandler }
import ftp.ui.errorhandle.{ ErrorHandle, ExceptionHandler }
import ftp.ui.listeners.RemoteItemChangeListener
import ftp.util.ConfigObj
import ftp.util.ImplicitConversions.funToRunnable




/**
 * gebruikt voor de FX-GUI.
 */
class FtpGui extends Application 
{
  
  
  private var ftpClient: FtpClient = null

  private val receiver: MessageHandler = new ReceiveHandler
  private val exh: ErrorHandle = new ExceptionHandler(receiver)
  private var primaryStage: Stage = null
  //menu
  private val menueBar = new MenuBar()
  private val fileMenue = new Menu(lang("file-menue"))
  private val helpMenue = new Menu(lang("help-menue"))
  //Connectie
  private val txtServer = new TextField()
  private val txtPort = new TextField("21")
  //private val txtPort = new TextField("22")
  private val txtUsername = new TextField()
  private val txtPassword = new PasswordField()
  private val btnConnect = new Button(lang("connect-btn"))
  private val btnDisconnect = new Button(lang("disconnect-btn"))
  private val cbAnon = new CheckBox(lang("anonymous-login"))
  //Log
  private val txaLog = new TextArea()
  private val txaLoads = new TextArea()
  private val tabLog = new Tab(lang("log-tab"))
  private val tabLoads = new Tab(lang("loads-tab"))
  //Filesystems
  //betstands systeem
  private var localFs: TreeView[WrappedPath] = null
  private var remoteFs: TreeView[FileDescriptor] = null

  //Down-/Uploads
  //added in genFileSystemView() together with the download-directory-chooser
  //
  private val btnUpload = new Button(lang("upload-btn"))
  private val btnDownload = new Button(lang("download-btn"))
  private val btnChangeDownloadDir = new Button(lang("download-choose-entry"))
  // transfermanager voor de up-/downloads
  private var trManager: TransferManager = null
  //Download-directory
  //download map
  private val downloadDir = new ComboBox[Path]()

  /**
   * haalt het gespecificeerd config-waarde van de config-object
   * @param de  sleutel voor de waarde
   * @return de waarde of "not defined"
   */
  private def conf(key: String): String = ConfigObj.getC(key) match 
  {
    case Some(x) => x
    case None =>
      receiver.status(s"The config value for: $key doesn't exist")
      "not defined"
  }

  /**
   * haalt de gespecificeerde taal-waarde van de config-object.
   * @param de sleutel voor de waarde
   * @retrun de waarde of "not defined"
   */
  private def lang(key: String): String = ConfigObj.getL(key) match 
  {
    case Some(x) => x
    case None =>
      receiver.status(s"The language value for: $key doesn't exist")
      "not defined"
  }

  override def start(primStage: Stage) =
  {
    primaryStage = primStage
    val vboxContainer = new VBox()
    val root = new BorderPane()
    root.setId("rootPane")
    val top = new GridPane()
    top.setId("topGrid")
    root.setTop(top)
    val scene = new Scene(vboxContainer, 800, 700)

    ConfigObj.getCss() match 
    {
      //geen error 
      case Right(x) => scene.getStylesheets().add(x)
      //error 
      case Left(x) =>
        val stylesheetPath = x.split(":")(1)
        receiver.error("Can't load the pathed stylesheet.\nStylesheet passed: " + stylesheetPath)
    }

    //Menu
    //Bestands menu
    val chLocalMnItem = new MenuItem(lang("local-root"))
    val chRemoteMnItem = new MenuItem(lang("remote-root"))
    val exitMnItem = new MenuItem(lang("exit"))
    //verandert de lokale root view
    chLocalMnItem.setOnAction((ev: ActionEvent) => changeLocalRootDir())
    // verandert de afstandelijke root map
    chRemoteMnItem.setOnAction((ev: ActionEvent) => changeRemoteRootDir())
    exitMnItem.setOnAction((ev: ActionEvent) => primStage.close())
    fileMenue.getItems.addAll(chLocalMnItem, chRemoteMnItem, exitMnItem)
    //Help menu
    val clientInfoMnItem = new MenuItem(lang("client-information-item"))
    val serverInfoMnItem = new MenuItem(lang("server-information-item"))
    val aboutInfoMnItem = new MenuItem(lang("about-item"))
    clientInfoMnItem.setOnAction((ev: ActionEvent) => showClientInformation())
    serverInfoMnItem.setOnAction((ev: ActionEvent) => showServerInformation())
    aboutInfoMnItem.setOnAction((ev: ActionEvent) => showAbout())
    helpMenue.getItems.addAll(clientInfoMnItem, serverInfoMnItem, aboutInfoMnItem)

    //voegt menu toe aan de menubar, en voegt menubar toe
    menueBar.getMenus.addAll(fileMenue, helpMenue)

    btnConnect.setId("connect-btn")
    btnConnect.setOnAction((ev: ActionEvent) => connect())
    btnDisconnect.setId("disconnect-btn")
    btnDisconnect.setOnAction((ev: ActionEvent) => disconnect())
    btnUpload.setOnAction((ev: ActionEvent) => shareFiles(ev))
    btnDownload.setOnAction((ev: ActionEvent) => shareFiles(ev))
    btnUpload.setId("upload-btn")
    btnDownload.setId("download-btn")

    txtPort.setMaxWidth(50)
    txtPassword.setOnKeyPressed((ev: KeyEvent) => if (ev.getCode == KeyCode.ENTER) connect())

    cbAnon.setSelected(false)
    //handler voor tick-untick "login anonnymous"
    cbAnon.setOnAction((ev: ActionEvent) => if (cbAnon.isSelected) 
    {
      txtUsername.setDisable(true)
      txtPassword.setDisable(true)
    } 
    else 
    {
      txtUsername.setDisable(false)
      txtPassword.setDisable(false)
    })

    top.add(newBoldText(lang("servername")), 0, 0)
    top.add(txtServer, 1, 0)
    top.add(newBoldText(lang("port")), 2, 0)
    top.add(txtPort, 3, 0)
    top.add(newBoldText(lang("username")), 0, 1)
    top.add(txtUsername, 1, 1)
    top.add(newBoldText(lang("password")), 2, 1)
    top.add(txtPassword, 3, 1)
    top.add(btnConnect, 4, 1)
    top.add(btnDisconnect, 4, 0)
    top.add(cbAnon, 5, 1)

    root.setCenter(genFileSystemView())

    // log
    val pane = new TabPane();
    pane.setId("bottomPane")
    pane.setSide(Side.BOTTOM)
    txaLoads.setEditable(false)
    txaLoads.setEditable(false)
    tabLoads.setContent(txaLoads)
    tabLog.setContent(txaLog)
    tabLoads.setClosable(false)
    tabLog.setClosable(false)
    pane.getTabs.addAll(tabLoads, tabLog)

    root.setBottom(pane)
    vboxContainer.getChildren.addAll(menueBar, root)
    primStage.setTitle(conf("software-name"))
    primStage.setScene(scene)
    primStage.sizeToScene()
    primStage.show()
  }
  /**
   * Method invoked when the last window is closed or the application is stopped.
   * functie die wordt gebruikt om de connenctie te stoppen.
   */
  override def stop() = 
  {
    disconnect()
  }
  /**
   * genereet de centrale panelen met de lokale en afstandelijke bestands systeem-treeviews.
   * Daarnaast voegt de upload en download button toe.

   */
  private def genFileSystemView(): Pane = 
  {
    val fsRoot = new GridPane()
    fsRoot.setId("fsGrid")

    fsRoot.add(newBoldText(lang("local-filesystem-title")), 0, 0)
    fsRoot.add(newBoldText(lang("remote-filesystem-title")), 1, 0)
    localFs = genLocalFs()
    localFs.setMinSize(370, 300)
    remoteFs = genRemoteFs()
    remoteFs.setMinSize(370, 300)

    fsRoot.add(localFs, 0, 1)
    fsRoot.add(remoteFs, 1, 1)

    //download map
    val downloadPane = new HBox()
    val l: ObservableList[Path] = FXCollections.observableArrayList(Paths.get(conf("download-dir")), Paths.get(conf("local-start-dir")));
    downloadPane.setId("downloadPane")
    downloadDir.setItems(l)
    downloadDir.getSelectionModel().selectFirst()
    downloadDir.setMinWidth(150)
    //handler voor het zien van de map kieser.
    btnChangeDownloadDir.setOnAction((ev: ActionEvent) => 
    {
      val chooser = new DirectoryChooser()
      chooser.setTitle(lang("download-chooser-title"))
      val file = chooser.showDialog(primaryStage)
      if (file != null) {
        val path = file.toPath()
        downloadDir.getItems.add(0, path)
        downloadDir.getSelectionModel().selectFirst()
      }
    })

    downloadPane.getChildren.addAll(newBoldText(lang("download-dir")),
      downloadDir, btnChangeDownloadDir, btnUpload, btnDownload)


    val root = new VBox()
    root.setId("centeredView")
    root.getChildren.addAll(fsRoot, downloadPane)
    return root
  }

  private def newBoldText(s: String): Text = 
  {
    val text = new Text(s)
    text.setId("bold-text")
    return text
  }

  /**
   * genereet de view voor het lokale bestands systeem.
   * Deze fuctie gebruikt de factory voor het generen van de view.
   */
  private def genLocalFs(): TreeView[WrappedPath] = 
  {
    val next = Paths.get(conf("local-start-dir"))
    val root = ViewFactory.newLazyView(next)
    val view = new TreeView[WrappedPath](root)

    view.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    return view
  }

  /**
   * Genereet de standaard view voor de afstandelijk bestands systeem
   */
  private def genRemoteFs(): TreeView[FileDescriptor] = 
  {
    val tree = new TreeView[FileDescriptor](new TreeItem[FileDescriptor](new RemoteFile(lang("default-remote-entry"))))
    tree.setDisable(true) // Deze functie mag niet werken als er niet is ingelogt
    tree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    return tree
  }
 private def SSHgenRemoteFs():TreeView[Iterable[String]]=
 {
   val tree = new TreeView[Iterable[String]](new TreeItem[Iterable[String]](new SshRemoteFile(lang("default-remote-entry"))))
   tree.setDisable(true)
   tree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
   return tree  
 }
  /**
   * Genereet de nieuwe afstandlijk view na de login en ook als de mappen zijn verandert.
   * deze methode wordt gebruikt tijdens runtime
   */
  private def genRemoteFs(dir: String, content: List[FileDescriptor]) = 
  {
    val root = ViewFactory.newSubView(dir, content)

    /**
     * geeft None of Some() terug met de lijst van de nieuwe map.
     */
    def listRemoteFiles(parent: FileDescriptor): Option[List[FileDescriptor]] =
      ftpClient match 
      {
        case null => None
        case _    => ftpClient.cd(parent.getAbsoluteFilename()); 
        Some(ftpClient.ls())
      }

    val listener = new RemoteItemChangeListener(listRemoteFiles)

   
    root.getChildren.filter(!_.isLeaf()).foreach 
    { 
      x => x.expandedProperty().addListener(listener) 
    }

    remoteFs.setDisable(false) //activate because now it's usable
    remoteFs.setRoot(root)
  }
  
  /*
   * ------------- EventHandlers --------------------
   * -----------------------------------------------
   */

  /**
   * Connects the client by using the top-textfields.
   */
  private def connect() = 
  {
    val servername = txtServer.getText
    val port = txtPort.getText.toInt
    var username = new String()
    var password = new String()
    var userDir = List[FileDescriptor]()
    var actualDir = new String()
    //anonymous login
    if (cbAnon.isSelected()) {
      username = conf("anon-username")
      password = conf("anon-password")
    } 
    else 
    { //authenticated login
      username = txtUsername.getText
      password = txtPassword.getText
    }

    if (servername.isEmpty() || txtPort.getText.isEmpty()) 
      receiver.error("Specify Server & Port.")
    else if (username.isEmpty() || password.isEmpty()) 
      receiver.error("Specify username/password.")        
    else 
    {
      exh.catching 
      {
        ftpClient = ClientFactory.newBaseClient(servername, port, receiver)
        if (ftpClient.connect(username, password)) 
        {
          actualDir = ftpClient.pwd()
          userDir = ftpClient.ls()
          genRemoteFs(actualDir, userDir)
          //setup voor transfer-manager
          if (trManager != null) trManager ! Exit() 
          trManager = new TransferManager(ftpClient, receiver, exh)
          trManager.start()
        }
      }
    }

  } //connect
  
  private def disconnect() = 
  {
    if (trManager != null)
      trManager ! Exit() //stop the actor

    if (ftpClient != null) 
    {
      ftpClient.disconnect()
      ftpClient = null
    }

    remoteFs.setRoot(new TreeItem[FileDescriptor](new RemoteFile(lang("default-remote-entry"))))
    remoteFs.setDisable(true)
  }

  /**
   * verandert de lokale root map.
   */
  private def changeLocalRootDir() = 
  {
    val chooser = new DirectoryChooser()
    chooser.setTitle(lang("local-root-chooser-title"))

    val file = chooser.showDialog(primaryStage)
    if (file != null) 
    {
      val path = file.toPath()
      localFs.setRoot(ViewFactory.newLazyView(path))
    }
  }
  
  private def sshchangeLocalRootDir() =
  {
    val chooser = new DirectoryChooser()
    chooser.setTitle(lang("local-root-chooser-title"))
    
    val file = chooser.showDialog(primaryStage)
    if(file != null)
    {
      val path = file.toPath()
      localFs.setRoot(ViewFactory.newLazyView(path))
    }
  }

  /**
   * verandert de server root map.
   */
  private def changeRemoteRootDir() = if (ftpClient != null)
  {
    // toont de input-dialog en zet de root.
    val dialog = ViewFactory.newChangeRemoteRootDialog()
    val optResult = dialog.showAndWait()
    if (optResult.isPresent) 
    {
      val path = optResult.get
      ftpClient.cd(path)
      val content = ftpClient.list()
      genRemoteFs(path, content)
    }
  } else receiver.error("Please connect to the server first.")
  
 
  private def showServerInformation() = if (ftpClient != null) 
  {
    val infos = ftpClient.getServerInformationAsMap()
    val dialog = ViewFactory.newSystemsInfo(lang("server-information-title"), lang("server-information-header"), lang("server-information-content"), infos)
    dialog.showAndWait()
  } 
  else receiver.error("Please connect to the server first!")
  
  
  private def showClientInformation() = if (ftpClient != null) 
  {
    val infos = ftpClient.getClientInformationAsMap()
    val dialog = ViewFactory.newSystemsInfo(lang("client-information-title"), lang("client-information-header"), lang("client-information-content"), infos)
    dialog.showAndWait()
  } 
  else receiver.error("Please connect to the server first!")

  
  private def showAbout() = 
  {
    ???
  }

  /**
   * Handles de file transfers.
   * Handles voor de bestands transfers.
   */
  private def shareFiles(ev: ActionEvent) = if (ev.getSource == btnUpload && ftpClient != null) 
  {
    val selectedElements = this.localFs.getSelectionModel.getSelectedItems.map(_.getValue.path).toList

    trManager ! Upload(selectedElements)
  }   
  else if (ev.getSource == btnDownload && ftpClient != null) 
  {
    val selectedElements = this.remoteFs.getSelectionModel.getSelectedItems.map(_.getValue).toList
    // haalt de active elementen van het dowload-ComboBox
    // verandert de huidige pad naar absolute pad.
    val destination = downloadDir.getSelectionModel.getSelectedItem.toAbsolutePath()

    trManager ! Download(selectedElements, destination.toString())
  } 
  else receiver.error("Please connect to the server before starting a transfer.")

  /**
   * Observer-/Handler for the logs.
   */
  private class ReceiveHandler extends MessageHandler 
  {
    /*
     * 
     * Implementatie-info:
     * alle functies voor het aanpassen van UI-Components,
     * so elke functie is uit gevoerd in de JAVAFX-EDT
     */

    def error(msg: String): Unit = Platform.runLater(() => 
    {
      txaLog.appendText(s"ERROR: $msg\n")
      tabLog.getTabPane.getSelectionModel.select(tabLog)

      val dialog = ViewFactory.newErrorDialog(msg = msg)
            val opt = dialog.showAndWait()
    })
    def newMsg(msg: String): Unit = Platform.runLater(() => txaLog.appendText(msg + "\n"))
    def status(msg: String): Unit = Platform.runLater(() => {
      if (msg.startsWith("Download") || msg.startsWith("Upload:")) txaLoads.appendText(msg + "\n")
      else txaLog.appendText(msg + "\n")
    })

    def newException(ex: Exception): Unit = Platform.runLater(() => 
    {
      txaLog.appendText("Exception occured: " + ex.toString)
      tabLog.getTabPane.getSelectionModel.select(tabLog)

      val dialog = ViewFactory.newExceptionDialog(msg = "You found a bug.", ex = ex)
      val opt = dialog.showAndWait()
    })
  }
}

object FtpGui 
{
  def main(args: Array[String]): Unit = 
  {
    Application.launch(classOf[FtpGui], args: _*)
  }
}
