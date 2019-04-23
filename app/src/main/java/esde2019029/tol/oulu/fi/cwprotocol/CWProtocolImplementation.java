package esde2019029.tol.oulu.fi.cwprotocol;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

public class CWProtocolImplementation implements CWPControl, CWPMessaging, Runnable  {

    CWPConnectionReader cwpConnectionReader;
    CWProtocolListener listener;

    public static final int DEFAULT_FREQUENCY = -1;

    boolean lineUpByUser;
    boolean lineUpByServer;

    public enum CWPState {Disconnected, Connected, LineUp, LineDown};
    private volatile CWPState currentState = CWPState.Disconnected;
    private CWPState nextState = currentState;
    private int currentFrequency = DEFAULT_FREQUENCY;
    private CWPConnectionReader connection = null;
    private Handler receiveHandler = new Handler();
    private int messageValue = 0;
    private static final String TAG = "CWPImplementation";

    public CWPState getCurrentState(){
        return currentState;
    }

    public CWProtocolImplementation(CWProtocolListener listener_p){
        listener = listener_p;
    }

    @Override
    public void addObserver(Observer observer) {

    }

    @Override
    public void deleteObserver(Observer observer) {

    }

    @Override
    public void lineUp() throws IOException {
        lineUpByUser = true;
        Log.d(TAG, "State change to LineUp happening..");
        currentState = CWPState.LineUp;
        listener.onEvent(CWProtocolListener.CWPEvent.ELineUp, 0);
    }

    @Override
    public void lineDown() throws IOException {
        lineUpByUser = false;
        if (lineUpByServer == false) {
            Log.d(TAG, "State change to LineDown happening..");
            currentState = CWPState.LineDown;
            listener.onEvent(CWProtocolListener.CWPEvent.ELineDown, 0);
        }
    }

    @Override
    public void connect(String serverAddr, int serverPort, int frequency) throws IOException {
        Log.d(TAG, "State change to Connected happening..");
        currentState = CWPState.Connected;
        listener.onEvent(CWProtocolListener.CWPEvent.EConnected, 0);
        cwpConnectionReader = new CWPConnectionReader(this);
        cwpConnectionReader.startReading();
    }

    @Override
    public void disconnect() throws IOException {
        Log.d(TAG, "State change to Disconnected happening..");
        currentState = CWPState.Disconnected;
        listener.onEvent(CWProtocolListener.CWPEvent.EDisconnected, 0);
        if (cwpConnectionReader.myProcessor != null) {
            try {
                cwpConnectionReader.stopReading();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                cwpConnectionReader.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            cwpConnectionReader.myProcessor = null;
        }
    }

    @Override
    public boolean isConnected() {
        return currentState == CWPState.Connected;
    }

    @Override
    public boolean lineIsUp() {
        if (currentState == CWPState.LineUp) {
            return true;
        }
        return false;
    }

    @Override
    public boolean serverSetLineUp() {
        return false;
    }

    @Override
    public void setFrequency(int frequency) throws IOException {

    }

    @Override
    public int frequency() {
        return 0;
    }

    public void run() {
        switch (nextState) {
            case Connected:
                Log.d(TAG, "State change to Connected happening..");
                currentState = nextState;
                listener.onEvent(CWProtocolListener.CWPEvent.EConnected, 0);
                break;
            case Disconnected:
                Log.d(TAG, "State change to Disconnected happening...");
                currentState = nextState;
                listener.onEvent(CWProtocolListener.CWPEvent.EDisconnected, 0);
                break;
            case LineDown:
                Log.d(TAG, "State change to LineDown happening...");
                currentState = nextState;
                listener.onEvent(CWProtocolListener.CWPEvent.ELineDown, 0);
                break;
            case LineUp:
                Log.d(TAG, "State change to LineUp happening..");
                currentState = nextState;
                listener.onEvent(CWProtocolListener.CWPEvent.ELineUp, 0);
                break;
        }
    }

    private class CWPConnectionReader extends Thread{

        private volatile boolean running = false;
        private Runnable myProcessor = null;
        private static final String TAG = "CWPReader";

        // Used before networking for timing cw signals
        private Timer myTimer = null;
        private TimerTask myTimerTask = null;

        CWPConnectionReader(Runnable processor) {
            myProcessor = processor;

        }

        private void changeProtocolState(CWPState state, int param) throws InterruptedException{
            Log.d(TAG, "Change protocol state to " + state);
            nextState = state;
            messageValue = param;
            receiveHandler.post(myProcessor);
        }

        void startReading(){
            running = true;
            start();
        }

        void stopReading() throws InterruptedException{
            myTimer.cancel();
            running = false;
            myTimer = null;
            myTimerTask = null;
            currentState = CWPState.Disconnected;
        }

        private void doInitialize() throws InterruptedException{
            changeProtocolState(CWPState.Connected, 0);
            myTimer = new Timer();
            myTimerTask = new TimerTask() {
                @Override
                public void run() {
                    if (currentState == CWPState.LineDown) {
                        lineUpByServer = true;
                        try{
                            changeProtocolState(CWPState.LineUp, 0);
                        }catch (InterruptedException ie){
                            ie.printStackTrace();
                        }
                    } else if (currentState == CWPState.LineUp) {
                        lineUpByServer = false;
                        if (lineUpByUser == false) {
                            try {
                                changeProtocolState(CWPState.LineDown, 0);
                            } catch (InterruptedException ie) {
                                ie.printStackTrace();
                            }
                        }
                    }
                }
            };
            myTimer.scheduleAtFixedRate(myTimerTask, 500, 1000);
        }

        @Override
        public void run(){
            try {
                doInitialize();
                changeProtocolState(CWPState.LineDown, 0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //placeholder
            while(running){

            }

        }

    }

}
