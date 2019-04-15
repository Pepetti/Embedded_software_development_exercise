package esde2019029.tol.oulu.fi.cwprotocol;

import java.io.IOException;
import java.util.Observer;

import esde2019029.tol.oulu.fi.TappingFragment;

public interface CWPMessaging {
    public void addObserver(Observer observer);
    public void deleteObserver(Observer observer);
    public void lineUp() throws IOException;
    public void lineDown() throws IOException;
    public boolean isConnected();
    public boolean lineIsUp();
    boolean serverSetLineUp();

}
