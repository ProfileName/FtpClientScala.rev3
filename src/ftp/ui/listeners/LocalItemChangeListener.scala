package ftp.ui.listeners

import javafx.scene.control.TreeItem
import java.nio.file.Path
import java.nio.file.Paths
import ftp.ui.ViewFactory
import ftp.ui.DummyItems
import ftp.client.filesystem.WrappedPath

class LocalItemChangeListener(private val dummy: TreeItem[WrappedPath] = DummyItems.localFs) extends TreeListener[WrappedPath] {
  def onChanged(item: TreeItem[WrappedPath]): Unit = 
  {
    //zet de nieuwe subpad voor het maken van gegeven map als deze nog niet is gemaakt.
    if (item.getChildren.contains(dummy)) 
    {
      //verwijdert de dummy en vervangt de children
      item.getChildren.remove(dummy)
      val path = item.getValue.path

      //genereet de subview van het veranderte element en voegt de tree toe.
      val subview = ViewFactory.newLazyView(path)
      item.getChildren.addAll(subview.getChildren)
    }
  }
}
