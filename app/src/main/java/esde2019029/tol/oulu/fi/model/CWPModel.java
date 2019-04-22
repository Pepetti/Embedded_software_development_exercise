package esde2019029.tol.oulu.fi.model;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import esde2019029.tol.oulu.fi.cwprotocol.CWPControl;
import esde2019029.tol.oulu.fi.cwprotocol.CWPMessaging;
import esde2019029.tol.oulu.fi.cwprotocol.CWProtocolImplementation;
import esde2019029.tol.oulu.fi.cwprotocol.CWProtocolListener;

public class CWPModel extends Observable implements CWPMessaging, CWPControl, CWProtocolListener {
    CWProtocolImplementation cwProtocolImplementation;

    @Override
    public void connect(String serverAddr, int serverPort, int frequency) throws IOException {
        setChanged();
        notifyObservers(cwProtocolImplementation.getCurrentState());
    }

    @Override
    public void disconnect() throws IOException {
        setChanged();
        notifyObservers(cwProtocolImplementation.getCurrentState());

    }

    @Override
    public void setFrequency(int frequency) throws IOException {

    }

    @Override
    public int frequency() { return CWProtocolImplementation.DEFAULT_FREQUENCY; }

    public void addObserver(Observer observer){
    }

    public void deleteObserver(Observer observer){
    }

    @Override
    public void lineUp() throws IOException {
        setChanged();
        notifyObservers(cwProtocolImplementation.getCurrentState());
    }

    @Override
    public void lineDown() throws IOException {
        setChanged();
        notifyObservers(cwProtocolImplementation.getCurrentState());
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean lineIsUp() {
        return false;
    }

    @Override
    public boolean serverSetLineUp() {
        return false;
    }

    @Override
    public void onEvent(CWPEvent event, int param) {
        setChanged();
        notifyObservers(event);
    }
}