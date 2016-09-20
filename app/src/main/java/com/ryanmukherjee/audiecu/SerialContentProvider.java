package com.ryanmukherjee.audiecu;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class SerialContentProvider extends ContentProvider {

    public static final String SERIAL_TABLE = "serial";
    public static final String SERIAL_ID = "_id";
    public static final String SERIAL_CONTENT = "content";
    public static final String SERIAL_TIMESTAMP = "timestamp";

    public static final String AUTHORITY = "com.ryanmukherjee.audiecu.provider";
    public static final String SERIAL_AUTHORITY = AUTHORITY + '/' + SERIAL_TABLE;

    public static final Uri SERIAL_URI = Uri.parse("content://" + SERIAL_AUTHORITY);

    // Instantiate UriMatcher
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    private static final int SERIAL_TABLE_ALL_ROWS = 0;
    private static final int SERIAL_TABLE_SINGLE_ROW = 1;

    public static final int DB_VERSION = 1;
    private SQLiteDatabase serialDb;

    static {
        // Select multiple rows
        URI_MATCHER.addURI(AUTHORITY, SERIAL_TABLE, SERIAL_TABLE_ALL_ROWS);
        // Select single row
        URI_MATCHER.addURI(AUTHORITY, SERIAL_TABLE, SERIAL_TABLE_SINGLE_ROW);
    }

    // MIME types
    public static final String MIME_SERIAL_ALL = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.serial.com.ryanmukherjee";
    public static final String MIME_SERIAL_SINGLE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.serial.com.ryanmukherjee";

    private static class OpenHelper extends SQLiteOpenHelper {
        private String table;

        public OpenHelper(Context context, String table) {
            super(context, table, null, DB_VERSION);
            this.table = table;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            try {
                db.beginTransaction();
                String sql;
                switch(this.table) {
                    case SERIAL_TABLE:
                        sql = String.format(
                                "CREATE TABLE %s (%s integer primary key autoincrement, " +
                                        "%s text, %s datetime default current_timestamp)",
                                SERIAL_TABLE, SERIAL_ID, SERIAL_CONTENT, SERIAL_TIMESTAMP
                        );
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported table specified in onCreate!");
                }

                db.execSQL(sql);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int numDeleted = 0;
        switch(URI_MATCHER.match(uri)) {
            case SERIAL_TABLE_ALL_ROWS:
                numDeleted = serialDb.delete(SERIAL_TABLE, selection, selectionArgs);
                break;
            case SERIAL_TABLE_SINGLE_ROW:
                String id = uri.getLastPathSegment();
                numDeleted = serialDb.delete(SERIAL_TABLE, SERIAL_ID + "=?", new String[] {id});
                break;
            default:
                throw new UnsupportedOperationException("Unrecognized URI: " + uri.toString());
        }

        if (numDeleted > 0)
            getContext().getContentResolver().notifyChange(uri, null);

        return numDeleted;
    }

    @Override
    public String getType(Uri uri) {
        switch(URI_MATCHER.match(uri)) {
            case SERIAL_TABLE_ALL_ROWS:
                return MIME_SERIAL_ALL;
            case SERIAL_TABLE_SINGLE_ROW:
                return MIME_SERIAL_SINGLE;
            default:
                throw new UnsupportedOperationException("Unrecognized URI: " + uri.toString());
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Uri uriOut = null;
        long id;
        switch(URI_MATCHER.match(uri)) {
            case SERIAL_TABLE_ALL_ROWS:
                id = serialDb.insert(SERIAL_TABLE, null, values);
                if (id > 0)
                    uriOut = ContentUris.withAppendedId(SERIAL_URI, id);
                break;
            default:
                throw new UnsupportedOperationException("Unrecognized URI: " + uri.toString());
        }

        if (uriOut != null)
            getContext().getContentResolver().notifyChange(uriOut, null);

        return uriOut;
    }

    @Override
    public boolean onCreate() {
        serialDb = new OpenHelper(getContext(), SERIAL_TABLE).getWritableDatabase();
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;
        switch(URI_MATCHER.match(uri)) {
            case SERIAL_TABLE_ALL_ROWS:
                cursor = serialDb.query(SERIAL_TABLE, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case SERIAL_TABLE_SINGLE_ROW:
                String id = uri.getLastPathSegment();
                cursor = serialDb.query(SERIAL_TABLE, projection, SERIAL_ID + "=?", new String[] {id}, null, null, sortOrder);
                break;
            default:
                throw new UnsupportedOperationException("Unrecognized URI: " + uri.toString());
        }

        if (cursor != null)
            cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int numChanges = 0;
        switch(URI_MATCHER.match(uri)) {
            case SERIAL_TABLE_ALL_ROWS:
                numChanges = serialDb.update(SERIAL_TABLE, values, selection, selectionArgs);
                break;
            case SERIAL_TABLE_SINGLE_ROW:
                String id = uri.getLastPathSegment();
                numChanges = serialDb.update(SERIAL_TABLE, values, SERIAL_ID + "=?", new String[] {id});
                break;
            default:
                throw new UnsupportedOperationException("Unrecognized URI: " + uri.toString());
        }

        if (numChanges > 0)
            getContext().getContentResolver().notifyChange(uri, null);

        return numChanges;
    }
}
