package com.ryanmukherjee.audiecu;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
        viewHolder.serialContent.setText(content);
    }

    public static class SerialViewHolder extends RecyclerView.ViewHolder {
        private TextView serialContent;

        public SerialViewHolder(View view) {
            super(view);
            serialContent = (TextView) view.findViewById(R.id.serialContent);
        }
    }
}
