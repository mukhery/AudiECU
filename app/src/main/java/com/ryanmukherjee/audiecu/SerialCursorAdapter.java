package com.ryanmukherjee.audiecu;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ryanmukherjee.audiecu.BluetoothSPPService.SerialType;

public class SerialCursorAdapter extends CursorRecyclerViewAdapter<SerialCursorAdapter.SerialViewHolder> {

    private LayoutInflater layoutInflater;

    public SerialCursorAdapter(LayoutInflater layoutInflater) {
        super(null);
        this.layoutInflater = layoutInflater;
    }

    @Override
    public SerialViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.fragment_terminal_serial_item, parent, false);
        return new SerialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SerialViewHolder viewHolder, Cursor cursor) {
        if (cursor == null || cursor.getCount() <= 0)
            return;

        String content = cursor.getString(cursor.getColumnIndex(SerialContentProvider.SERIAL_CONTENT));
        SerialType type = SerialType.values()[cursor.getInt(cursor.getColumnIndex(SerialContentProvider.SERIAL_TYPE))];
        String timestamp = cursor.getString(cursor.getColumnIndex(SerialContentProvider.SERIAL_TIMESTAMP));
        timestamp = timestamp.replace(' ', '\n');
        viewHolder.timestamp.setText(timestamp);
        // Add terminal prompt
        if (type == SerialType.INPUT) {
            content = "> " + content;
        }
        viewHolder.serialContent.setText(content);
    }

    public static class SerialViewHolder extends RecyclerView.ViewHolder {
        private TextView timestamp, serialContent;

        public SerialViewHolder(View view) {
            super(view);
            timestamp = (TextView) view.findViewById(R.id.timestamp);
            serialContent = (TextView) view.findViewById(R.id.serialContent);
        }
    }
}
