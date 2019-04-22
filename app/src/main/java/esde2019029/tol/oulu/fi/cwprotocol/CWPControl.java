package esde2019029.tol.oulu.fi.cwprotocol;


import java.io.IOException;
import java.util.Observer;

public interface CWPControl {

    public void addObserver(Observer observer);
    public void deleteObserver(Observer observer);
    // Connection management
    public void connect(String serverAddr, int serverPort, int frequency) throws IOException;
    public void disconnect()  throws IOException;
    public boolean isConnected();
    // Frequency management
    public void setFrequency(int frequency) throws IOException;
    public int frequency();
}
