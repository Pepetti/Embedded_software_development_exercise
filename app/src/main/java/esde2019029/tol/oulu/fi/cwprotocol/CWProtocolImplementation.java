package esde2019029.tol.oulu.fi.cwprotocol;

import android.os.ConditionVariable;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Timestamp;
import java.util.Observer;
import java.util.concurrent.Semaphore;

import esde2019029.tol.oulu.fi.model.CWPModel;

public class CWProtocolImplementation implements CWPControl, CWPMessaging, Runnable  {

    CWPConnectionReader cwpConnectionReader;
    CWProtocolListener listener;

    public static final int DEFAULT_FREQUENCY = -1;

    private boolean lineUpByUser;
    private boolean lineUpByServer;

    public enum CWPState {Disconnected, Connected, LineUp, LineDown};
    private volatile CWPState currentState = CWPState.Disconnected;
    private CWPState nextState = currentState;
    private int currentFrequency = DEFAULT_FREQUENCY;
    private CWPConnectionReader connection = null;
    private Handler receiveHandler = new Handler();
    private int messageValue = 0;
    private static final String TAG = "CWPImplementation";

    private static final int BUFFER_LENGTH = 64;
    private ByteBuffer outBuffer = null;
    private OutputStream nos = null; //Network Output Stream
    private String serverAddr = null;
    private int serverPort = -1;

    private int fourBytes = 0;
    private short twoBytes = 0;

    private CWPConnectionWriter cwpConnectionWriter = null;
    private ConditionVariable conditionVariable = new ConditionVariable();

    private int timestamp;
    private long connectionEstablished;

    private Semaphore lock = new Semaphore(1);


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
        boolean stateChanged = false;
        if (!lineUpByUser && (currentState == CWPState.LineUp || currentState == CWPState.LineDown )) {
           Long tempStamp = System.currentTimeMillis() - connectionEstablished;
           timestamp = tempStamp.intValue();
           messageValue = timestamp;
           conditionVariable.open();
           if(currentState == CWPState.LineDown && !lineUpByServer){
               try{
                   lock.acquire();
                   stateChanged = true;
                   currentState = CWPState.LineUp;
               }catch(InterruptedException e){
                   e.printStackTrace();
               }finally{
                   lock.release();
               }
           }
           lineUpByUser = true;
           if(stateChanged){
               CWProtocolListener.onEvent(CWProtocolListener.CWPEvent.ELineUp, timestamp);
           }
        }
    }


    @Override
    public void lineDown() throws IOException {

        if (currentState == CWPState.LineUp && lineUpByUser){
            /*
            LASKELMAT TÄHÄN!
             */
        lineUpByUser = false;
        if (!lineUpByServer) {
                Log.d(TAG, "State change to LineDown happening..");
                try{
                    lock.acquire();
                    currentState = CWPState.LineDown;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.release();
                }
                lineUpByUser = false;
                listener.onEvent(CWProtocolListener.CWPEvent.ELineDown, messageValue);
            }
        }
    }

    @Override
    public void connect(String serverAddr, int serverPort, int frequency) throws IOException {
        Log.d(TAG, "State change to Connected happening..");
        currentState = CWPState.Connected;
        connectionEstablished = System.currentTimeMillis();
        listener.onEvent(CWProtocolListener.CWPEvent.EConnected, 0);
        cwpConnectionReader = new CWPConnectionReader(this);
        cwpConnectionWriter = new CWPConnectionWriter();
        cwpConnectionReader.startReading();
        cwpConnectionWriter.startSending();
    }

    @Override
    public void disconnect() throws IOException, InterruptedException {
        Log.d(TAG, "State change to Disconnected happening..");
        currentState = CWPState.Disconnected;
        connectionEstablished = 0L;
        listener.onEvent(CWProtocolListener.CWPEvent.EDisconnected, 0);
        if (cwpConnectionReader != null) {
            cwpConnectionReader.stopReading();
            try {
                cwpConnectionWriter.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            cwpConnectionReader = null;
        }
        cwpConnectionWriter.stopSending();
        cwpConnectionWriter = null;
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
        return lineUpByServer;
    }

    @Override
    public void setFrequency(int frequency) throws IOException {
        if (frequency < 0 && frequency != currentFrequency) {
            currentFrequency = frequency;
            sendFrequency();
        }
    }

    @Override
    public int frequency() {
        return currentFrequency;
    }

    public void sendFrequency(){
        if (currentState == CWPState.LineDown) {
            messageValue = frequency();
        }
        conditionVariable.open();

    }

    public void run() {
        switch (nextState) {
            case Connected:
                lineUpByServer = false;
                Log.d(TAG, "State change to Connected happening..");
                currentState = nextState;
                listener.onEvent(CWProtocolListener.CWPEvent.EConnected, 0);
                break;
            case Disconnected:
                lineUpByServer = false;
                Log.d(TAG, "State change to Disconnected happening...");
                currentState = nextState;
                listener.onEvent(CWProtocolListener.CWPEvent.EDisconnected, 0);
                break;
            case LineDown:
                lineUpByServer = false;
                if (!lineUpByUser && currentState == CWPState.Connected) {
                    if (currentFrequency != messageValue) {
                        sendFrequency();
                    }else{
                        listener.onEvent(CWProtocolListener.CWPEvent.EChangedFrequency, 0);
                    }
                    Log.d(TAG, "State change to LineDown happening...");
                    currentState = nextState;
                    listener.onEvent(CWProtocolListener.CWPEvent.ELineDown, 0);
                }else{
                    Log.d(TAG, "State change to LineDown happening...");
                    currentState = nextState;
                    listener.onEvent(CWProtocolListener.CWPEvent.ELineDown, 0);
                }
                break;
            case LineUp:
                lineUpByServer = true;
                if (!lineUpByUser) {
                    Log.d(TAG, "State change to LineUp happening..");
                    currentState = nextState;
                    listener.onEvent(CWProtocolListener.CWPEvent.ELineUp, 0);
                }
                break;
        }
    }

    private class CWPConnectionReader extends Thread{

        private Socket cwpSocket = null;
        private InputStream nis = null; //Network Input Stream

        private volatile boolean running = false;
        private Runnable myProcessor = null;
        private static final String TAG = "CWPReader";

        int bytesToRead = 4;
        int bytesRead = 0;

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

        void stopReading() throws IOException, InterruptedException {
            Log.d(TAG, "Disconnecting...");
            cwpSocket.close();
            cwpSocket = null;
            changeProtocolState(CWPState.Disconnected, 0);
            serverAddr = null;
            serverPort = -1;
            Log.d(TAG, "Disconnected");
        }

        private void doInitialize() throws IOException, InterruptedException {
            serverAddr = "cwp.opimobi.com";
            serverPort = 20000;

            SocketAddress socketAddress = new InetSocketAddress(serverAddr, serverPort);
            cwpSocket = new Socket();
            cwpSocket.connect(socketAddress, 5000);
            nis = cwpSocket.getInputStream();
            nos = cwpSocket.getOutputStream();
            changeProtocolState(CWPState.Connected, 0);
            Log.d(TAG, "Connected");
        }

        private int readLoop(byte [] bytes, int bytesToRead) throws IOException {
            int readNow;
            do{
               readNow = nis.read(bytes, bytesRead, bytesToRead-bytesRead);
            } while (readNow != -1);
            if (readNow == -1) {
                throw new IOException("Read -1 from stream");
            }

            return readNow;
        }

        @Override
        public void run(){

            byte[] byteArray = new byte[4];
            ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.SIZE / 8);
            byteBuffer.order(ByteOrder.BIG_ENDIAN);

            try {
                doInitialize();
                changeProtocolState(CWPState.LineDown, currentFrequency);

                while(running){
                    readLoop(byteArray, bytesToRead);
                    byteBuffer.clear();
                    byteBuffer.wrap(byteArray);
                    byteBuffer.position(0);
                    int intValue = byteBuffer.getInt();
                    if (intValue < 0){
                        changeProtocolState(CWPState.LineDown, 0 );
                    }
                    else if (intValue > 0){
                        changeProtocolState(CWPState.LineUp, 0);
                        readLoop(byteArray, 2);
                        changeProtocolState(CWPState.LineDown,  0);
                    }

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    private class CWPConnectionWriter extends Thread{

        private static final String TAG = "CWPWriter";
        boolean running = false;

        void startSending(){
            running = true;
            start();
        }

        void stopSending(){
            running = false;
            conditionVariable.open();
        }

        @Override
        public void run(){
            while (running){
                try {
                    conditionVariable.block();
                    if (fourBytes != 0) {
                        sendMessage(fourBytes);
                        fourBytes = 0;
                        conditionVariable.close();
                    }
                    else if (twoBytes > 0){
                        sendMessage(twoBytes);
                        twoBytes = 0;
                        conditionVariable.close();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        private void sendMessage(int msg) throws IOException {
            Log.d(TAG, "Sending msg...");
            outBuffer = ByteBuffer.allocate(Integer.SIZE / 8);
            outBuffer.order(ByteOrder.BIG_ENDIAN);
            outBuffer.putInt(msg);
            outBuffer.position(0);
            byte[] byteArray = outBuffer.array();
            nos.write(byteArray);
            nos.flush();
            outBuffer = null;
            Log.d(TAG, "Sent msg");
        }

        private void sendMessage(short msg) throws IOException{
            Log.d(TAG, "Sending short msg...");
            outBuffer = ByteBuffer.allocate(Integer.SIZE / 4);
            outBuffer.order(ByteOrder.BIG_ENDIAN);
            outBuffer.putShort(msg);
            outBuffer.position(0);
            byte[] byteArray = outBuffer.array();
            nos.write(byteArray);
            nos.flush();
            outBuffer = null;
            Log.d(TAG, "Sent short msg");
        }

    }

}
