package esde2019029.tol.oulu.fi.model;

import java.io.IOException;
import java.util.Observable;

import esde2019029.tol.oulu.fi.cwprotocol.CWPControl;
import esde2019029.tol.oulu.fi.cwprotocol.CWPMessaging;
import esde2019029.tol.oulu.fi.cwprotocol.CWProtocolImplementation;
import esde2019029.tol.oulu.fi.cwprotocol.CWProtocolListener;

public class CWPModel extends Observable implements CWPMessaging, CWPControl, CWProtocolListener {
    private CWProtocolImplementation cwProtocolImplementation = new CWProtocolImplementation(this);

    @Override
    public void connect(String serverAddr, int serverPort, int frequency) throws IOException {
        cwProtocolImplementation.connect(serverAddr, serverPort, frequency);
        Signaller s = new Signaller();
        addObserver(s);
    }

    @Override
    public void disconnect() throws IOException, InterruptedException {
        cwProtocolImplementation.disconnect();
    }

    @Override
    public void setFrequency(int frequency) throws IOException {
        cwProtocolImplementation.setFrequency(frequency);
    }

    @Override
    public int frequency() { return cwProtocolImplementation.frequency(); }

    @Override
    public void lineUp() throws IOException {
        cwProtocolImplementation.lineUp();
    }

    @Override
    public void lineDown() throws IOException {
        cwProtocolImplementation.lineDown();
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean lineIsUp() {
       return cwProtocolImplementation.lineIsUp();
    }

    @Override
    public boolean serverSetLineUp() {
        return cwProtocolImplementation.serverSetLineUp();
    }

    @Override
    public void onEvent(CWPEvent event, int param) {
        setChanged();
        notifyObservers(event);
    }
}