package esde2019029.tol.oulu.fi;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import esde2019029.tol.oulu.fi.cwprotocol.CWPMessaging;
import esde2019029.tol.oulu.fi.cwprotocol.CWProtocolListener;


public class TappingFragment extends Fragment implements Observer {

    private ImageView hall9000_offline;
    private CWPMessaging cwpMessaging;
    private TextView userLine;
    private View view;

    public TappingFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_tapping, container, false);
        hall9000_offline = (ImageView) view.findViewById(R.id.hall9000_offline);

        hall9000_offline.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    try {
                        cwpMessaging.lineUp();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                else if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    try {
                        cwpMessaging.lineDown();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                else {
                    return false;
                }
            }
        });
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        CWPProvider cwpProvider;

        cwpProvider = (CWPProvider) getActivity();
        cwpMessaging = cwpProvider.getMessaging();
        cwpMessaging.addObserver(this);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        cwpMessaging.deleteObserver(this);
        cwpMessaging = null;
    }


    @Override
    public void update (Observable obs, Object arg){

        if (arg == CWProtocolListener.CWPEvent.ELineUp) {
            hall9000_offline.setImageResource(R.mipmap.hal9000_up);
            userLine = view.findViewById(R.id.userLineState);
            userLine.setText(R.string.LineUp);
        }

        else if (arg == CWProtocolListener.CWPEvent.ELineDown){
            hall9000_offline.setImageResource(R.mipmap.hal9000_down);
            userLine = view.findViewById(R.id.userLineState);
            userLine.setText(R.string.LineDown);
        }

    }

}
