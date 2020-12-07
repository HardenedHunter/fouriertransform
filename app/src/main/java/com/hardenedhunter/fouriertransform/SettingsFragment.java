package com.hardenedhunter.fouriertransform;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import static android.R.layout.simple_list_item_1;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends Fragment {

    ISettingsEventListener settingsEventListener;

    private ListView listBluetooth;

    private static final String ARG_PARAM_DEVICES = "devices";

    private static ArrayList<String> devices;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        try {
            settingsEventListener = (ISettingsEventListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement onSomeEventListener");
        }
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment SettingsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_PARAM_DEVICES, devices);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            devices = getArguments().getStringArrayList(ARG_PARAM_DEVICES);
            if (devices == null)
                devices = new ArrayList<>();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        listBluetooth = view.findViewById(R.id.listBluetooth);

        listBluetooth.setOnItemClickListener(this::onListViewClick);

        ArrayAdapter<String> pairedDeviceAdapter = new ArrayAdapter<>(getContext(), simple_list_item_1, devices);
        listBluetooth.setAdapter(pairedDeviceAdapter);

        return view;
    }

    //region listeners
    private void onListViewClick(AdapterView<?> adapterView, View view, int i, long l) {
        String itemValue = (String) listBluetooth.getItemAtPosition(i);
        String MAC = itemValue.substring(itemValue.length() - 17); // Вычленяем MAC-адрес
        settingsEventListener.macSelectedEvent(MAC);
    }
    //endregion

}