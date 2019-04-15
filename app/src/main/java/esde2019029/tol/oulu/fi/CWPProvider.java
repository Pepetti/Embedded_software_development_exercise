package esde2019029.tol.oulu.fi;

import esde2019029.tol.oulu.fi.cwprotocol.CWPControl;
import esde2019029.tol.oulu.fi.cwprotocol.CWPMessaging;

public interface CWPProvider {
    CWPMessaging getMessaging();
    CWPControl getCWPControl();
}
