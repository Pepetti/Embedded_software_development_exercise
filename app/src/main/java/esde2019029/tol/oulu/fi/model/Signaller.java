package esde2019029.tol.oulu.fi.model;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.Log;

import java.util.Observable;
import java.util.Observer;

import esde2019029.tol.oulu.fi.cwprotocol.CWProtocolListener;
import esde2019029.tol.oulu.fi.cwprotocol.CWPMessaging;


public class Signaller implements Observer {

    private CWPMessaging cwpMessaging;
    private ToneGenerator t = new ToneGenerator(AudioManager.STREAM_DTMF, 100);
    private static final String TAG = "Signaller";

    private void start(){
        t.startTone(AudioManager.STREAM_DTMF);
    }

    private void stop(){
        t.stopTone();
    }

    @Override
    public void update(Observable o, Object arg) {
        if(arg == CWProtocolListener.CWPEvent.ELineUp) {
            Log.d(TAG, "Start audio...");
            start();
        }
        else if(arg == CWProtocolListener.CWPEvent.ELineDown){
            stop();
        }
        else if (arg == CWProtocolListener.CWPEvent.EDisconnected){
            stop();
        }
    }
}
