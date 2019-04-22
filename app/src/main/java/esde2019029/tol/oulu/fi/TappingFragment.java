package esde2019029.tol.oulu.fi;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import esde2019029.tol.oulu.fi.cwprotocol.CWPMessaging;
import esde2019029.tol.oulu.fi.cwprotocol.CWProtocolImplementation;
import esde2019029.tol.oulu.fi.cwprotocol.CWProtocolListener;
import esde2019029.tol.oulu.fi.model.CWPModel;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TappingFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TappingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TappingFragment extends Fragment implements Observer {

    private ImageView hall9000_offline;
    private CWPMessaging cwpMessaging;

    public TappingFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_tapping, container, false);
        hall9000_offline = (ImageView) view.findViewById(R.id.hall9000_offline);

        hall9000_offline.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    hall9000_offline.setImageResource(R.mipmap.hal9000_up);
                    hall9000_offline.findViewById(R.id.userLineState);
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    hall9000_offline.setImageResource(R.mipmap.hal9000_down);
                    hall9000_offline.findViewById(R.id.serverLineState);
                    return true;
                }
                return false;
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
            try {
                cwpMessaging.lineUp();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (arg == CWProtocolListener.CWPEvent.ELineDown){
            hall9000_offline.setImageResource(R.mipmap.hal9000_down);
            try {
                cwpMessaging.lineDown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
