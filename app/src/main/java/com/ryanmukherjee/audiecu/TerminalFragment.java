package com.ryanmukherjee.audiecu;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

public class TerminalFragment extends Fragment implements View.OnClickListener {

    private static final int SERIAL_LOADER = 1;
    private SerialCursorAdapter mSerialCursorAdapter;

    private EditText mInput;

    public static TerminalFragment newInstance() {
        return new TerminalFragment();
    }

    private LoaderManager.LoaderCallbacks<Cursor> serialLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String[] projection = {
                    SerialContentProvider.SERIAL_ID,
                    SerialContentProvider.SERIAL_CONTENT,
                    SerialContentProvider.SERIAL_TIMESTAMP
            };

            return new CursorLoader(getActivity(), SerialContentProvider.SERIAL_URI, projection,
                    null, null, SerialContentProvider.SERIAL_TIMESTAMP + " DESC");
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (mSerialCursorAdapter != null)
                mSerialCursorAdapter.changeCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            if (mSerialCursorAdapter != null)
                mSerialCursorAdapter.changeCursor(null);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_terminal, container, false);
        mInput = (EditText) v.findViewById(R.id.input);
        ImageButton button = (ImageButton) v.findViewById(R.id.send);
        button.setOnClickListener(this);

        RecyclerView serialRecycler = (RecyclerView) v.findViewById(R.id.serialRecycler);
        mSerialCursorAdapter = new SerialCursorAdapter(getActivity().getLayoutInflater());
        serialRecycler.setAdapter(mSerialCursorAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        layoutManager.setStackFromEnd(true);
        serialRecycler.setLayoutManager(layoutManager);

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        LoaderManager loaderManager = getActivity().getLoaderManager();
        loaderManager.initLoader(SERIAL_LOADER, null, serialLoaderCallbacks);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        LoaderManager loaderManager = getActivity().getLoaderManager();
        loaderManager.destroyLoader(SERIAL_LOADER);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.send:
                String input = mInput.getText().toString();
                if (!TextUtils.isEmpty(input)) {
                    mInput.getText().clear();
                    Intent intent = new Intent(BluetoothSPPService.ACTION_SEND_COMMAND);
                    intent.putExtra("content", input);
                    getActivity().sendBroadcast(intent);
                } else {
                    mInput.setError("No command entered!");
                }

                break;
            default:
                throw new RuntimeException("Unhandled onClick call!");
        }
    }
}
