
package ftp.ui.errorhandle

/**
 * beschrijft error en exception handlers.
 */
trait ErrorHandle 
{
  /**
   * probeert de geven functie uit te voeren en behandelt mogelijke exceptions.
   *  
   * als de het niet lukt om de functie uit te voeren geeft het none terug, anders geeft het Some() met de return value van de gegeven functie.
   *
   * @param f de functie die een exception terug geeft.
   * @tparam A de return-type van de gegeven functie.
   * @return None als het niet lukt om de functie uit te voeren, Some(x:[A]) anders
   */
  def catching[A](f: => A): Option[A]
}
