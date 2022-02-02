package ftp.client.filesystem

/**
 * deze trait beschrijft de bestand infromatie van remote-files.
 * @bekijk [[ftp.client.filesystem.RemoteFile]]
 */
trait FileDescriptor extends Comparable[FileDescriptor] 
{
  /**
   * Test of het een map is.
   * @geeft true terug als het een map is, false als dat niet zo is.
   */
  def isDirectory(): Boolean
  /**
   * test of het een bestand is of niet.
   * 
   * @geeft true als het een bestand is, false als dat niet zo is.
   */
  def isFile(): Boolean = !isDirectory()
  /**
   * geeft het pad naar de file terug.
   *
   * De waarde die terug komt ziet er zo uit:
   * {{{ /tmp/hoi/testFile.txt }}}
   * @geeft het pad terug met de bestands naam
   */
  def getAbsoluteFilename(): String
  /**
   * geeft alleen de filename terug zonder het pad.
   *
   * De waarde die terug komt ziet er zo uit:
   * {{{ testFile.txt }}}
   * @geeft het bestaands naam
   */
  def getFilename() = 
  {
    val splitted = this.getAbsoluteFilename().split("/")
    splitted(splitted.length - 1)
  }

  override def toString() = getAbsoluteFilename()

  override def compareTo(other: FileDescriptor) =
    this.getFilename().compareTo(other.getFilename())

}
