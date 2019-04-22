package esde2019029.tol.oulu.fi.cwprotocol;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

public class CWProtocolImplementation implements CWPControl, CWPMessaging, Runnable  {

    CWPConnectionReader cwpConnectionReader;
    public static final int DEFAULT_FREQUENCY = -1;

    CWProtocolListener listener;


    public enum CWPState {Disconnected, Connected, LineUp, LineDown};
    private volatile CWPState currentState = CWPState.Disconnected;

    public CWPState getCurrentState(){
        return currentState;
    }



    private CWPState nextState = currentState;
    private int currentFrequency = DEFAULT_FREQUENCY;
    private CWPConnectionReader connection = null;
    private Handler receiveHandler = new Handler();
    private int messageValue = 0;

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

    }

    @Override
    public void lineDown() throws IOException {

    }

    @Override
    public void connect(String serverAddr, int serverPort, int frequency) throws IOException {
        cwpConnectionReader = new CWPConnectionReader(this);
        cwpConnectionReader.startReading();
    }

    @Override
    public void disconnect() throws IOException {
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
        return false;
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
            //Log.d(TAG, "Change protocol state to " + state);
            nextState = state;
            messageValue = param;
            receiveHandler.post(myProcessor);
        }

        void startReading(){
            running = true;
            start();
            run();
        }

        void stopReading() throws InterruptedException{
            myTimer.cancel();
            running = false;
            myTimer = null;
            myTimerTask = null;
            currentState = CWPState.Disconnected;
        }

        private void doInitialize() throws InterruptedException{
            currentState = CWPState.Connected;
            myTimer = new Timer();
            myTimerTask = new TimerTask() {
                @Override
                public void run() {
                    myTimerTask.run();
                }
            };
            myTimer.scheduleAtFixedRate(myTimerTask, 500, 1000);
            if(currentState == CWPState.LineDown){
                currentState = CWPState.LineUp;
            }else if(currentState == CWPState.LineUp){
                currentState = CWPState.LineDown;
            }
        }

        @Override
        public void run(){
            try {
                doInitialize();
                changeProtocolState(CWPState.LineDown, 1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //placeholder
            while(running){

            }

        }


    }

    public void run(){
        switch(nextState){
            case Connected:
                Log.d(null, "State change to Connected happening..");
                currentState = nextState;
                listener.onEvent(CWProtocolListener.CWPEvent.EConnected, 0);
                break;
            case Disconnected:
                Log.d(null, "State change to Disconnected happening...");
            case LineDown:
                Log.d(null, "State change to LineDown happening...");
            case LineUp:
                Log.d(null, "State change to LineUp happening..");
        }
    }
}
