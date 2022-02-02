

package ftp.response

/**
 * beschrijft methodes die de ontvanger voor de ftp-client nodig heeft.
 */
trait Receivable 
{
  /**
   * ontvangt een nieuw bericht van de server. 
   */
  def newMsg(msg: String): Unit;
  /**
   * haalt de status informatie van de server.
   *
   * @param msg de status bericht.
   */
  def status(msg: String): Unit;

  def error(msg: String): Unit;
}
