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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import static android.view.inputmethod.EditorInfo.IME_ACTION_SEND;

public class TerminalFragment extends Fragment implements View.OnClickListener {

    private static final int SERIAL_LOADER = 1;
    private RecyclerView mSerialRecycler;
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
                    SerialContentProvider.SERIAL_TYPE,
                    SerialContentProvider.SERIAL_TIMESTAMP
            };

            return new CursorLoader(getActivity(), SerialContentProvider.SERIAL_URI, projection,
                    null, null, SerialContentProvider.SERIAL_TIMESTAMP + " ASC");
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (mSerialCursorAdapter != null) {
                mSerialCursorAdapter.changeCursor(data);
                // Scroll to the bottom of the terminal log
                mSerialRecycler.scrollToPosition(mSerialRecycler.getAdapter().getItemCount()-1);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            if (mSerialCursorAdapter != null)
                mSerialCursorAdapter.changeCursor(null);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.terminal_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_clear:
                getActivity().getContentResolver().delete(SerialContentProvider.SERIAL_URI, null, null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_terminal, container, false);
        mInput = (EditText) v.findViewById(R.id.input);
        final ImageButton button = (ImageButton) v.findViewById(R.id.send);
        button.setOnClickListener(this);

        mInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == IME_ACTION_SEND) {
                    button.performClick();
                    return true;
                }
                return false;
            }
        });
        mSerialRecycler = (RecyclerView) v.findViewById(R.id.serialRecycler);
        mSerialCursorAdapter = new SerialCursorAdapter(getActivity().getLayoutInflater());
        mSerialRecycler.setAdapter(mSerialCursorAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        layoutManager.setStackFromEnd(true);
        mSerialRecycler.setLayoutManager(layoutManager);

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
