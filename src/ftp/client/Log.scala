package ftp.client

import ftp.response.Receivable

/**
 * 
 * Genereet de logs voor de ftp-connectie
 * 
 * @param rc waar de ontvange data gelogt moet worden.
 */
private[client] class Log(private val rc: Receivable) 
{

  private var lastResult: Boolean = false;

  /**
   * haalt een nieuw bericht en schrijft de status of error berichten afhankelijk van de test van x.
   * @param lijn van het bericht.
   * @param x het coresponderde test.
   */
  def newMsg(line: String, x: String => Boolean): Unit = 
  {
    lastResult = x.apply(line)
    if (lastResult) {
      rc.status(line)
    } else rc.error(line)
  }

  /**
   * schrijft nieuw status bericht.
   * deze schrijft altijd de status.
   * @param lijn van het bericht.
   */
  def newMsg(line: String): Unit = 
  {
    newMsg(line, x => true)
  }

  /**
   * maakt een niewe error bericht aan.
   * deze methode schrijft altijd errors
   * @param line the Message
   */
  def newError(line: String): Unit = 
  {
    newMsg(line, x => false)
  }

  /**
   * geeft het resultaat terug van de laaste test.
   */
  def getLastResult: Boolean = return lastResult;
}
