package esde2019029.tol.oulu.fi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import esde2019029.tol.oulu.fi.cwprotocol.CWPControl;
import esde2019029.tol.oulu.fi.cwprotocol.CWProtocolListener;


public class ControlFragment extends Fragment implements Observer {
    private CWPControl cwpControl;

    public ControlFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        final String server = preferences.getString("key_server_address", "hölkynkölkyn.com");
        final int frequency = Integer.parseInt(preferences.getString("key_default_frequency", "70"));
        View view = inflater.inflate(R.layout.fragment_control, container, false);
        ToggleButton toggleButton = (ToggleButton) view.findViewById(R.id.CWPServerConnection);

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    try {
                        cwpControl.connect(server, 20000, frequency);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    try {
                        cwpControl.disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
        cwpControl = cwpProvider.getCWPControl();
        cwpControl.addObserver(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        cwpControl.deleteObserver(this);
        cwpControl = null;
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg == CWProtocolListener.CWPEvent.EConnected) {
            Toast.makeText(getActivity().getApplicationContext(), getString(R.string.connected), Toast.LENGTH_SHORT).show();
        }
        if (arg == CWProtocolListener.CWPEvent.EDisconnected) {
            Toast.makeText(getActivity().getApplicationContext(), getString(R.string.disconnected), Toast.LENGTH_SHORT).show();
        }
    }
}
