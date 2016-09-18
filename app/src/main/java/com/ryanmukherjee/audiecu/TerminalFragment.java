package com.ryanmukherjee.audiecu;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

public class TerminalFragment extends Fragment implements View.OnClickListener {

    public static TerminalFragment newInstance() {
        return new TerminalFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_terminal, container, false);
        ImageButton button = (ImageButton) v.findViewById(R.id.send);
        button.setOnClickListener(this);
        return v;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.send:
                break;
            default:
                throw new RuntimeException("Unhandled onClick call!");
        }
    }
}
