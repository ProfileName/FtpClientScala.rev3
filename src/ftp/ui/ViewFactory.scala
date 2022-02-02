package ftp.ui

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.scene.control.TreeItem
import scala.collection.JavaConversions.iterableAsScalaIterable
import ftp.ui.filewalker.GenerateTree
import ftp.ui.listeners._
import javafx.scene.control.TreeItem
import javafx.event.EventHandler
import javafx.event.ActionEvent
import javafx.scene.control.TreeItem.TreeModificationEvent
import javafx.beans.property.BooleanProperty
import ftp.client.filesystem.FileDescriptor
import ftp.client.filesystem.RemoteFile
import ftp.client.filesystem.WrappedPath
import javafx.scene.control.CheckBoxTreeItem
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.Alert
import javafx.scene.layout.GridPane
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import java.io.StringWriter
import java.io.PrintWriter
import javafx.scene.layout.Priority
import javafx.scene.control.TextInputDialog
import ftp.util.ConfigObj
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import javafx.scene.layout.HBox

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import fr.janalyse.ssh._
/**
 * maakt JavaFx-components.
 */
object ViewFactory 
{

  private val dialogTheme = ConfigObj.getRsc("/style/dialogs.css") match 
  {
    case Left(x) =>
      println(x);
      ""
    //error
    case Right(x) => x
  }

  /**
   * genereet een nieuwe TreeView van de gegeven bestanden.
   * 
   *
   * @param file het bestand.
   */
  @deprecated def newView(file: Path): TreeItem[Path] = 
  {
    val root = new CheckBoxTreeItem[Path](file)

    val fileWalker = new GenerateTree(root)
    Files.walkFileTree(file, fileWalker)
    return fileWalker.getView
  }

  /**
   * genereet een tijdelijke "view" aan, zonder andere onderdelen.
   * 
   * @param file de echte rootpad
   */
  def newLazyView(file: Path): TreeItem[WrappedPath] = 
  {
    val root = new TreeItem[WrappedPath](WrappedPath(file))
    val listener: ChangeListener[java.lang.Boolean] = new LocalItemChangeListener()

    def generateItem(x: Path): TreeItem[WrappedPath] = 
    {
      val item = new TreeItem[WrappedPath](WrappedPath(x))

      item.expandedProperty().addListener(listener)
      return item
    }


    val files = Files.newDirectoryStream(file).filterNot(Files.isHidden(_)).toList

    files.sortWith((x, y) => compareByName(x, y) < 0).foreach(
      _ match 
      {
        case x if (Files.isDirectory(x)) =>
   
          val xItem = generateItem(x)
          xItem.getChildren.add(DummyItems.localFs)
          root.getChildren.add(xItem)
        case x if (!Files.isDirectory(x)) => root.getChildren.add(new TreeItem[WrappedPath](WrappedPath(x)))
      })

    root.setExpanded(true)
    return root
  }

  /**
   * genereet een nieuw view voor het gegeven map waarin de content hoort.
   * 
   * voornamelijk gebruikt als een reactie voor de server ftp ls() opdracht.
   * @param dir het echte root-map
   * @param content het content van de map
   */
  def newSubView(dir: String, content: List[FileDescriptor]): TreeItem[FileDescriptor] = 
  {
    val root = new TreeItem[FileDescriptor](new RemoteFile(dir, true))

    //genereet map content
    content.sortWith((x, y) => compareByName(x, y) < 0).foreach 
    {
      _ match 
      {
        case f if (f.isDirectory()) =>
          val xItem = new TreeItem[FileDescriptor](f)
          xItem.getChildren.add(DummyItems.remoteFs)
          root.getChildren.add(xItem)
        case f if (f.isFile()) =>
          root.getChildren.add(new TreeItem[FileDescriptor](f))
      }
    }

    root.setExpanded(true)
    return root
  }
  


  /**
   * vergelijkt de gegeven elementen met de string representatie.
   * 
   * deze methode negeert de grote of lager karakter.
   */
  private def compareByName[T <: Comparable[T]](x: T, y: T) =
    x.toString.toLowerCase().compareTo(y.toString.toLowerCase())
 
 // private def comparingbyName(x,y)=
    //x.toString.toLowerCase().compareTo(y.toString.toLowerCase())
 

  /**
   * maakt een nieuw error-dialog aan met de gegeven content
   * 
   * deze functie gebruikt javaFx Alert.
   * @param title de titel van het dialog 
   * @param header de header-line van het dialog
   * @param msg het bericht van het dialog
   * @return an alert van het dialog 
   */
  def newErrorDialog(title: String = "Error", header: String = "An error occured!", msg: String) = 
  {
    val dialog = new Alert(AlertType.ERROR)
    dialog.getDialogPane.getStylesheets.add(dialogTheme)
    dialog.setTitle(title)
    dialog.setHeaderText(header)
    dialog.setContentText(msg)

    dialog
  }

  /**
   * maakt een nieuwe alert dialog met de gegeven content.
   *
   * deze functie gebruikt de JavaFx alert
   * @param title de title van het dialog
   * @param header de header-line van het dialog
   * @param msd het bericht van het dialog
   * @return an alert van het dialog
   */
  def newWarningDialog(title: String = "Warning", header: String = "Attention", msg: String) = 
  {
    val dialog = new Alert(AlertType.WARNING)
    dialog.getDialogPane.getStylesheets.add(dialogTheme)
    dialog.setTitle(title)
    dialog.setHeaderText(header)
    dialog.setContentText(msg)

    dialog
  }

  /**
   * maakt een nieuwe informatie dialog aan met de gegeven content.
   * 
   * deze functie gebruikt de JavaFx alert 
   * @param title de titel van het dialog
   * @param header de header-line van het dialog
   * @param ms het bericht van de dialog
   * @return an alert dialog
   */
  def newInformationDialog(title: String = "Information", header: String = "Information:", msg: String) = 
  {
    val dialog = new Alert(AlertType.INFORMATION)
    dialog.getDialogPane.getStylesheets.add(dialogTheme)
    dialog.setTitle(title)
    dialog.setHeaderText(header)
    dialog.setContentText(msg)

    dialog
  }

  def newSystemsInfo[T](title: String = "Connection", header: String = "Information:", msg: String, infos: Map[T, T]) = 
  {
    def getValue(item: Option[T]) = item match 
    {
      case None    => "not defined"
      case Some(x) => x.toString
    }
    //setup an information dialog
    val dialog = newInformationDialog(title, header, msg)
    dialog.getDialogPane.getStylesheets.add(dialogTheme)
    dialog.getDialogPane.setMinSize(200, 200)
    //    dialog.setHeight(200)
    //    dialog.setWidth(200)

    val pane = new GridPane()
    pane.setId("info-content-grid")
    var lineIndex = 0
    //add the custom informations
    infos.keys.foreach { key =>
      //create UI-Components from the key & value
      val keyLbl = new Text(key.toString)
      //key in bold
      keyLbl.setId("bold-text")
      val valueLbl = new Text(getValue(infos.get(key)))
      valueLbl.setId("centered-value")

      pane.add(keyLbl, 0, lineIndex)
      pane.add(valueLbl, 1, lineIndex)
      lineIndex += 1
    }

    dialog.getDialogPane().setContent(pane)
    dialog
  }

  /**
   * Creates a new <b>exception-dialgue</b> with the given content.
   * 
   * maakt een nieuw exception dialof aan met de gegeven content
   *   * deze functie gebruikt de JavaFx alert 
   * @param title de titel van het dialog
   * @param header de header-line van het dialog
   * @param ms het bericht van de dialog
   * @param ex het exception die gebeurt
   * @return an alert dialog
   */
  def newExceptionDialog(title: String = "EXCEPTION - ERROR", header: String = "Oups that shouldn't happen:", msg: String, ex: Exception) = 
  {
    val dialog = new Alert(AlertType.ERROR)
    dialog.getDialogPane.getStylesheets.add(dialogTheme)
    dialog.setTitle(title)
    dialog.setHeaderText(header)
    dialog.setContentText(msg)

    //schrijft de stacktrace naar een string
    val sw = new StringWriter()
    val pw = new PrintWriter(sw)
    ex.printStackTrace(pw)
    val exceptionText = sw.toString

    val label = new Label("The exception stacktrace was:");
    val textArea = new TextArea(exceptionText);
    textArea.setEditable(false);
    textArea.setWrapText(true);
    textArea.setMaxSize(Double.MaxValue, Double.MaxValue)

    //vergoot de text
    GridPane.setVgrow(textArea, Priority.ALWAYS)
    GridPane.setHgrow(textArea, Priority.ALWAYS)

    val pane = new GridPane()
    pane.setMaxWidth(Double.MaxValue)
    pane.add(label, 0, 0)
    pane.add(textArea, 0, 1)

    dialog.getDialogPane().setMinSize(400, 400)
    dialog.getDialogPane().setExpandableContent(pane)

    dialog
  }

  /**
   * Creates a dialog for setting the remote-root directory.
   * maakt een dialog aan voor de setting de remote root map.
   */
  def newChangeRemoteRootDialog() = 
  {
    /*
     * method for getting the specified keys from the ConfigObj.
     * This method is a shortcut.
     * 
     * functie voor het halen van de gespeciferde sleutel van ConfigObj
     */
    def getL(key: String) = ConfigObj.getL(key) match 
    {
      case None    => "not defined"
      case Some(x) => x
    }

    //setup the dialog with the language-keys
    //setup voor het dialog met de taal sleutels
    val dialog = new TextInputDialog("/")
    ConfigObj.getCss() match 
    {
      case Right(x) => dialog.getDialogPane.getStylesheets().add(x)
      //error 
      case Left(x)  => println(x)
    }
    dialog.setTitle(getL("remote-root-chooser-title"))
    dialog.setHeaderText(getL("remote-root-chooser-header"))
    dialog.setContentText(getL("remote-root-chooser-content"))
    dialog
  }

  @deprecated
  private class ItemChangeListener extends ChangeListener[java.lang.Boolean] 
{
    override def changed(obVal: ObservableValue[_ <: java.lang.Boolean], oldVal: java.lang.Boolean, newVal: java.lang.Boolean): Unit = 
    {
      /*
       * newVal = new state of the component (true if expanded, false otherwise)
       * newVal = nieuwe state van het component(waar als uitgebreid, vals zoniet)
       * obVal = the "observed" item
       * obVal = het object
       */
      if (newVal) 
      {
        //System.out.println("newValue = " + newVal);
        val bb = obVal.asInstanceOf[BooleanProperty];
        //System.out.println("bb.getBean() = " + bb.getBean());
        val t = bb.getBean.asInstanceOf[TreeItem[WrappedPath]];
        val path = t.getValue.path

        //set new subpath for the given directory if it's not created yet
        if (t.getChildren.contains(DummyItems.localFs)) 
        {
          //remove the dummy and replace the childrens
          t.getChildren.remove(DummyItems.localFs)
          val subview = newLazyView(path)
          t.getChildren.addAll(subview.getChildren)
        }
      }
    }
  } //class ItemChangeListener
}
