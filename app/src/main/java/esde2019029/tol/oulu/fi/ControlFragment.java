package esde2019029.tol.oulu.fi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import esde2019029.tol.oulu.fi.cwprotocol.CWPControl;
import esde2019029.tol.oulu.fi.cwprotocol.CWProtocolListener;


public class ControlFragment extends Fragment implements Observer {
    private CWPControl cwpControl;
    int freqFromEdit;

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

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        final View view = inflater.inflate(R.layout.fragment_control, container, false);

        ToggleButton connectionButton = (ToggleButton) view.findViewById(R.id.CWPServerConnection);
        connectionButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(getActivity().getApplicationContext(), "Initializing connection...", Toast.LENGTH_SHORT).show();
                    String server = preferences.getString("key_server_address", "defValue");
                    String serverArray[] = server.split(":");
                    int port = 0;
                    if (serverArray.length == 2) {
                        port = Integer.valueOf(serverArray[1]);
                        server = serverArray[0];
                        int frequency = Integer.parseInt(preferences.getString("key_default_frequency", "defValue"));
                        try {
                            cwpControl.connect(server, port, frequency);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }else if (!isChecked){
                    try {
                        Toast.makeText(getActivity().getApplicationContext(),"Disconnecting...", Toast.LENGTH_SHORT).show();
                        cwpControl.disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        Button freqButton = view.findViewById(R.id.freqChangeButton);
        freqButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity().getApplicationContext(),"Changing frequency...", Toast.LENGTH_SHORT).show();
                EditText editText = (EditText) view.findViewById(R.id.editFrequency);
                SharedPreferences.Editor edit = preferences.edit();

                freqFromEdit = Integer.parseInt(editText.getText().toString());
                if (freqFromEdit != cwpControl.frequency()){
                    try {
                        cwpControl.setFrequency(freqFromEdit);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                edit.putString("key_default_frequency", Integer.toString(cwpControl.frequency()) );
                edit.commit();
                Toast.makeText(getActivity().getApplicationContext(),"Frequency changed", Toast.LENGTH_SHORT).show();
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
        if (arg == CWProtocolListener.CWPEvent.EChangedFrequency) {
            Toast.makeText(getActivity().getApplicationContext(), getString(R.string.frequency_change) + cwpControl.frequency(), Toast.LENGTH_SHORT).show();
        }
    }
}
