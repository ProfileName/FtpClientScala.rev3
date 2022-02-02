package ftp.ui

import java.nio.file.Paths
import javafx.scene.control.TreeItem
import java.nio.file.Path
import ftp.client.filesystem.FileDescriptor
import ftp.client.filesystem.{RemoteFile}
import ftp.client.filesystem.WrappedPath

/**
 * houdt de pad for dummy items in een [[TreeItem]]
 */
object DummyItems 
{
  /**
   * toegang voor het locale bestands systeem.
   */
  val localFs: TreeItem[WrappedPath] = new TreeItem[WrappedPath](WrappedPath(Paths.get(".")))
  /**
   * toegang voor het locale bestands systeem.
   */
  val remoteFs: TreeItem[FileDescriptor] = new TreeItem[FileDescriptor](new RemoteFile(".", false))


}