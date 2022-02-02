package ftp.ui

import javafx.event.EventHandler
import javafx.scene.input.MouseEvent
import javafx.event.ActionEvent
import javafx.scene.control.TreeItem.TreeModificationEvent
import javafx.scene.input.KeyEvent

/**
 * het object verandert de scala lambdas voor de eventhandlers naar "java-lambdas".
 */
object FxEventHandlerImplicits 
{
  implicit def mouseEvent2EventHandler(event: (MouseEvent) => Any) = new EventHandler[MouseEvent] 
  {
    override def handle(dEvent: MouseEvent): Unit = event(dEvent)
  }
  implicit def actionEvent2EventHandler(event: (ActionEvent) => Any) = new EventHandler[ActionEvent] 
  {
    override def handle(dEvent: ActionEvent): Unit = event(dEvent)
  }
  implicit def keyEvent2EventHandler(event: (KeyEvent) => Any) = new EventHandler[KeyEvent] 
  {
    override def handle(dEvent: KeyEvent): Unit = event(dEvent)
  }
}