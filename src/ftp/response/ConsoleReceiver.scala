package ftp.response

/**
 * deze klasse beschrijft een simple command-line-using ontvanger voor de ftp-client.
 * deze klasse gebruikt print methodes voor het printen van status berichten. als er een error bericht is ontvangen wordt het programma afgesloten.
 */
class ConsoleReceiver extends Receivable 
{
  override def newMsg(msg: String): Unit = 
  {
    printf("Message: %s\n", msg)
    
  }
  override def status(msg: String): Unit = 
  {
    printf("Status: %s\n", msg)
  }
  override def error(msg: String): Unit = 
  {
    System.err.printf("Error: %s\n", msg)
    System.exit(0)
  }
}
