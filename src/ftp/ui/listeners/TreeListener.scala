package ftp.ui.listeners

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.scene.control.TreeItem
import javafx.beans.property.BooleanProperty

trait TreeListener[T] extends ChangeListener[java.lang.Boolean] 
{
  override def changed(obVal: ObservableValue[_ <: java.lang.Boolean], oldVal: java.lang.Boolean, newVal: java.lang.Boolean): Unit = {
    /*
       * newVal = new state of the component (true if expanded, false otherwise)
       * newVal = nieuwe state van het component.
       * bb.getBean houdt de huidige item
       */
    if (newVal) {
      val bb = obVal.asInstanceOf[BooleanProperty];
      val item = bb.getBean.asInstanceOf[TreeItem[T]];

      onChanged(item)
    }
  }

  /**
   * haalt de functie zoals aan geroepen treeitem.
   * @param item het verandert [[TreeItem]]
   */
  def onChanged(item: TreeItem[T]): Unit
}
