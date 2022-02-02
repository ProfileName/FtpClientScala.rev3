package ftp.ui.listeners

import javafx.scene.control.TreeItem
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import ftp.client.filesystem.FileDescriptor
import ftp.ui.FtpGui
import ftp.ui.DummyItems
import ftp.ui.ViewFactory

class RemoteItemChangeListener(ls: (FileDescriptor) => Option[List[FileDescriptor]], dummy: TreeItem[FileDescriptor] = DummyItems.remoteFs) extends TreeListener[FileDescriptor] {
  def onChanged(item: TreeItem[FileDescriptor]): Unit = 
  {
    // zet nieuw sub pad aan voor de geven map als het nog niet is aangemaakt
    if (item.getChildren.contains(dummy)) 
    {
      //verwijdert de dummy en vervangt de childrens
      item.getChildren.remove(dummy)
      val path = item.getValue

      ls(path) match 
      {
        case None => { }
        case Some(x: List[FileDescriptor]) =>
          // genereet de subview van het verandert element en voegt de tree toe.
          val subview = ViewFactory.newSubView(path.getFilename(), x)
          //voegt de cd handler toe aan de nieuwe subview.
          subview.getChildren.asScala.filter(_.getValue().isDirectory()).foreach { x => x.expandedProperty().addListener(this) }
          //voeg de subview aan de root
          item.getChildren.addAll(subview.getChildren)
      }

    }
  }
}
