package ftp.response

trait MessageHandler extends Receivable
{
  /**
   * wordt aan geroepen als er een exception gebeurt.
   *
   * @param ex de exception gebeurt
   */
  def newException(ex: Exception): Unit
}
