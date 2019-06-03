package esde2019029.tol.oulu.fi.cwprotocol;

import android.os.ConditionVariable;
import android.os.Handler;
import android.util.Log;

import java.net.SocketException;
import java.util.Arrays;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Observer;
import java.util.concurrent.Semaphore;

public class CWProtocolImplementation implements CWPControl, CWPMessaging, Runnable  {

    CWPConnectionReader cwpConnectionReader;
    CWProtocolListener listener;

    public static final int DEFAULT_FREQUENCY = -1;
    public static final int FORBIDDEN_VALUE = -2147483648;

    private boolean lineUpByUser;
    private boolean lineUpByServer;

    public enum CWPState {Disconnected, Connected, LineUp, LineDown};
    private volatile CWPState currentState = CWPState.Disconnected;
    private CWPState nextState = currentState;
    private int currentFrequency = DEFAULT_FREQUENCY;
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
    public void addObserver(Observer observer) { }

    @Override
    public void deleteObserver(Observer observer) { }

    @Override
    public void lineUp() throws IOException {
        Log.d(TAG, "lineUp method started...");
        boolean stateChanged = false;
        if (!lineUpByUser && (currentState == CWPState.LineUp || currentState == CWPState.LineDown )){
            Log.d(TAG, "lineUp by server happening...");
           Long tempStamp = System.currentTimeMillis() - connectionEstablished;
           timestamp = tempStamp.intValue();
           fourBytes = timestamp;
           conditionVariable.open();
           if(currentState == CWPState.LineDown && !lineUpByServer){
               Log.d(TAG, "lineUp by user happening...");
               try{
                   Log.d(TAG, "Acquiring lineUp lock...");
                   lock.acquire();
                   stateChanged = true;
                   currentState = CWPState.LineUp;
               }catch(InterruptedException e){
                   e.printStackTrace();
               }finally{
                   lock.release();
                   Log.d(TAG, "lineUp lock released");
               }
           }
           lineUpByUser = true;
           if(stateChanged){
               listener.onEvent(CWProtocolListener.CWPEvent.ELineUp, timestamp);
           }
        }
    }

    @Override
    public void lineDown() throws IOException {
        Log.d(TAG, "lineDown method started...");
        boolean stateChanged = false;
        if(currentState == CWPState.LineUp && lineUpByUser){
            twoBytes = (short) (System.currentTimeMillis() - connectionEstablished - timestamp);
            conditionVariable.open();
            lineUpByUser = false;
            if(!lineUpByServer){
                Log.d(TAG, "lineDown happening by user...");
                try{
                    Log.d(TAG, "Acquiring lineDown lock...");
                    lock.acquire();
                    currentState = CWPState.LineDown;
                    stateChanged = true;
                }catch(InterruptedException e){
                    e.printStackTrace();
                }finally{
                    lock.release();
                    Log.d(TAG, "lineDown lock released");
                }
            }
            if(stateChanged){
                listener.onEvent(CWProtocolListener.CWPEvent.ELineDown, twoBytes);
            }
        }
    }

    @Override
    public void connect(String serverAddr, int serverPort, int frequency) throws IOException {
        Log.d(TAG, "State change to Connected happening..");
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        setFrequency(frequency);
        cwpConnectionReader = new CWPConnectionReader(this);
        cwpConnectionWriter = new CWPConnectionWriter();
        cwpConnectionReader.startReading();
        cwpConnectionWriter.startSending();
        Log.d(TAG, "Connected");
    }

    @Override
    public void disconnect() throws IOException, InterruptedException {
        Log.d(TAG, "State change to Disconnected happening..");
        try{
            if(cwpConnectionWriter != null){
                cwpConnectionWriter.stopSending();
                cwpConnectionWriter.join();
                cwpConnectionWriter = null;
            }
            if(cwpConnectionReader != null){
                cwpConnectionReader.stopReading();
                cwpConnectionReader.join();
                cwpConnectionReader = null;
            }
        }catch(InterruptedException e){
            e.printStackTrace();
        }
        serverAddr = null;
        serverPort = 0;
        currentFrequency = DEFAULT_FREQUENCY;
        Log.d(TAG, "Disconnected");
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
        if (currentFrequency != frequency && frequency != FORBIDDEN_VALUE) {
            Log.d(TAG, "Starting frequency change...");
            if (frequency > 0) {
                currentFrequency = -frequency;
            } else if (frequency == 0) {
                currentFrequency = DEFAULT_FREQUENCY;
            } else {
                currentFrequency = frequency;
            }
            Log.d(TAG, "frequency: " + currentFrequency);
            sendFrequency();
        }
    }

    @Override
    public int frequency() {
        return currentFrequency;
    }

    private void sendFrequency() throws IOException{
        boolean didIt = false;
        try{
            Log.d(TAG, "Acquiring sendFrequency lock...");
            lock.acquire();
            if(currentState == CWPState.LineDown){
                Log.d(TAG, "Sending frequency...");
                fourBytes = currentFrequency;
                conditionVariable.open();
                currentState = CWPState.Connected;
                didIt = true;
            }else{
                Log.d(TAG, "Line not down. Try again later...");
            }
        }catch(InterruptedException e){
            e.printStackTrace();
        }finally{
            lock.release();
            Log.d(TAG, "Lock released");
        }
        if(didIt){
            listener.onEvent(CWProtocolListener.CWPEvent.EConnected, 0);
        }

    }

    public void run() {
        int tempMSG = messageValue;
        switch (nextState) {
            case Connected:
                Log.d(TAG, "State change to Connected happening..");
                currentState = nextState;
                lineUpByServer = false;
                lock.release();
                Log.d(TAG, "Lock released");
                listener.onEvent(CWProtocolListener.CWPEvent.EConnected, tempMSG);
                Log.d(TAG, "State is connected");
                break;
            case Disconnected:
                Log.d(TAG, "State change to Disconnected happening...");
                currentState = nextState;
                lineUpByServer = false;
                lock.release();
                Log.d(TAG, "Lock released");
                try {
                    disconnect();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                listener.onEvent(CWProtocolListener.CWPEvent.EDisconnected, tempMSG);
                Log.d(TAG, "State is disconnected");
                break;
            case LineDown:
                if (currentState == CWPState.Connected) {
                    currentState = nextState;
                    lock.release();
                    Log.d(TAG, "Lock released");
                    Log.d(TAG, "Frequency change happening...");
                    if (currentFrequency != tempMSG) {
                        try {
                            sendFrequency();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        listener.onEvent(CWProtocolListener.CWPEvent.EChangedFrequency, tempMSG);
                        Log.d(TAG, "Frequency changed");
                    }
                } else {
                    lineUpByServer = false;
                    if (!lineUpByUser) {
                        currentState = nextState;
                        lock.release();
                        Log.d(TAG, "Lock released");
                        Log.d(TAG, "State change to LineDown happening...");
                        listener.onEvent(CWProtocolListener.CWPEvent.ELineDown, tempMSG);
                        Log.d(TAG, "State is lineDown");
                    } else {
                        lock.release();
                        Log.d(TAG, "Lock released");
                        Log.d(TAG, "State change to LineDown happening...");
                        listener.onEvent(CWProtocolListener.CWPEvent.ELineDown, 0);
                        Log.d(TAG, "State is lineDown");
                    }
                }
                break;
            case LineUp:
                lineUpByServer = true;
                if (!lineUpByUser) {
                    Log.d(TAG, "State change to LineUp happening..");
                    currentState = nextState;
                    lock.release();
                    Log.d(TAG, "Lock released");
                    listener.onEvent(CWProtocolListener.CWPEvent.ELineUp, tempMSG);
                    Log.d(TAG, "State is lineUp");
                } else {
                    lock.release();
                    Log.d(TAG, "Lock released");
                    listener.onEvent(CWProtocolListener.CWPEvent.EServerStateChange, lineUpByServer ? 1 : 0);
                    Log.d(TAG, "Server state changed");
                }
                break;
            default:
                lock.release();
                Log.d(TAG, "Lock released");
        }
    }

    private class CWPConnectionReader extends Thread{

        private Socket cwpSocket = null;
        private InputStream nis = null; //Network Input Stream

        private volatile boolean running = false;
        private Runnable myProcessor = null;
        private static final String TAG = "CWPReader";

        CWPConnectionReader(Runnable processor) {
            myProcessor = processor;
        }

        private void changeProtocolState(CWPState state, int param) throws InterruptedException{
            Log.d(TAG, "Change protocol state to " + state);
            Log.d(TAG, "Acquiring lock...");
            lock.acquire();
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
            running = false;
            if(nos != null){
                nos.close();
                nos = null;
            }
            if(nis != null){
                nis.close();
                nis = null;
            }
            if(cwpSocket != null){
                cwpSocket.close();
                cwpSocket = null;
            }
            changeProtocolState(CWPState.Disconnected, 0);
            Log.d(TAG, "Disconnected");
        }

        private void doInitialize() throws IOException, InterruptedException {
            Log.d(TAG, "connectionReader thread starting...");
            SocketAddress socketAddress = new InetSocketAddress(serverAddr, serverPort);
            cwpSocket = new Socket();
            cwpSocket.connect(socketAddress, 5000);
            connectionEstablished = System.currentTimeMillis();
            nis = cwpSocket.getInputStream();
            nos = cwpSocket.getOutputStream();
            lineUpByUser = false;
            lineUpByServer = false;
            changeProtocolState(CWPState.Connected, 0);
            Log.d(TAG, "Started connectionReader");
        }

        private int readLoop(byte [] bytes, int bytesToRead) throws IOException {
            int bytesRead = 0;
            do {
                Arrays.fill(bytes, (byte) 0);
                int readNow = nis.read(bytes, bytesRead, bytesToRead - bytesRead);
                Log.d(TAG, "Bytecount Read " + readNow);
                if (readNow == -1) {
                    throw new IOException("Read -1 from stream");
                } else {
                    bytesRead += readNow;
                }
            } while (bytesRead < bytesToRead );
            return bytesRead;
        }

        @Override
        public void run(){
            try{
                doInitialize();
                int bytesToRead;
                int bytesRead;
                byte[] bytes = new byte[BUFFER_LENGTH];
                ByteBuffer buffer = ByteBuffer.allocate(BUFFER_LENGTH);
                buffer.order(ByteOrder.BIG_ENDIAN);
                while(running){
                    bytesToRead = 4;
                    Log.d(TAG, "Bytecount excpected: " + bytesToRead);
                    bytesRead = readLoop(bytes, bytesToRead);
                    if(bytesRead > 0){
                        buffer.clear();
                        buffer.put(bytes, 0, bytesRead);
                        buffer.position(0);
                        int value = buffer.getInt();
                        if(value >= 0){
                            changeProtocolState(CWPState.LineUp, value);
                            bytesToRead = 2;
                            bytesRead = readLoop(bytes, bytesToRead);
                            if(bytesRead > 0){
                                buffer.clear();
                                buffer.put(bytes, 0, bytesRead);
                                buffer.position(0);
                                short shortValue = buffer.getShort();
                                changeProtocolState(CWPState.LineDown, shortValue);
                            }
                        }else if(value != FORBIDDEN_VALUE){
                            changeProtocolState(CWPState.LineDown, value); //value == frequency from server
                        }
                    }
                }
            }catch(IOException e){
                Log.d(TAG, "IOException in reader thread");
                e.printStackTrace();
            }catch(InterruptedException e){
                Log.d(TAG, "InterruptedException in reader thread");
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
                    Log.d(TAG, "Aqcuiring lock...");
                    lock.acquire();
                    if (fourBytes != 0) {
                        sendMessage(fourBytes);
                        fourBytes = 0;
                    }
                    else if (twoBytes > 0){
                        sendMessage(twoBytes);
                        twoBytes = 0;
                    }
                } catch (IOException e) {
                    Log.d(TAG, "IOException in writer thread");
                    e.printStackTrace();
                } catch (InterruptedException e2){
                    Log.d(TAG, "InterruptedException in writer thread");
                    e2.printStackTrace();
                }finally{
                    conditionVariable.close();
                    lock.release();
                    Log.d(TAG, "Lock released");
                }

            }
        }

        private void sendMessage(int msg) throws IOException {
            Log.d(TAG, "Sending msg..." + msg);
            outBuffer = ByteBuffer.allocate(4);
            outBuffer.order(ByteOrder.BIG_ENDIAN);
            outBuffer.putInt(msg);
            outBuffer.position(0);
            byte[] byteArray = outBuffer.array();
            try{
                nos.write(byteArray);
            }catch(SocketException e){
                Log.d(TAG, "Socket exception on long send");
                e.printStackTrace();
            }
            nos.flush();
            outBuffer = null;
            Log.d(TAG, "Sent msg");
        }

        private void sendMessage(short msg) throws IOException{
            Log.d(TAG, "Sending short msg..." + msg);
            outBuffer = ByteBuffer.allocate(2);
            outBuffer.order(ByteOrder.BIG_ENDIAN);
            outBuffer.putShort(msg);
            outBuffer.position(0);
            byte[] byteArray = outBuffer.array();
            try{
                nos.write(byteArray);
            }catch(SocketException e){
                Log.d(TAG, "Socket exception on short send");
                e.printStackTrace();
            }
            nos.flush();
            outBuffer = null;
            Log.d(TAG, "Sent short msg");
        }
    }
}
