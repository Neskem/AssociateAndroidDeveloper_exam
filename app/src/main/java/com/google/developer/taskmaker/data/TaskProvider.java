package com.google.developer.taskmaker.data;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

public class TaskProvider extends ContentProvider {
    private static final String TAG = TaskProvider.class.getSimpleName();
    private static final String UNIMPLEMENTED_DESCRIPTION = "Not yet implemented";
    private static final String INVALID_URI_DESCRIPTION = "Not yet implemented";

    private static final int CLEANUP_JOB_ID = 43;

    private static final int TASKS = 100;
    private static final int TASKS_WITH_ID = 101;

    private TaskDbHelper mDbHelper;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        // content://com.google.developer.taskmaker/tasks
        sUriMatcher.addURI(DatabaseContract.CONTENT_AUTHORITY,
                DatabaseContract.TABLE_TASKS,
                TASKS);

        // content://com.google.developer.taskmaker/tasks/id
        sUriMatcher.addURI(DatabaseContract.CONTENT_AUTHORITY,
                DatabaseContract.TABLE_TASKS + "/#",
                TASKS_WITH_ID);
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new TaskDbHelper(getContext());
        manageCleanupJob();
        return true;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null; /* Not used */
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        //TODO: Implement task query
        //TODO: Expected "query all" Uri: content://com.google.developer.taskmaker/tasks
        //TODO: Expected "query one" Uri: content://com.google.developer.taskmaker/tasks/{id}
        Cursor cursor;
        SQLiteDatabase db;
        switch (sUriMatcher.match(uri)) {
            case TASKS:
                db = mDbHelper.getWritableDatabase();
                cursor = db.query(DatabaseContract.TABLE_TASKS,
                        projection, selection, selectionArgs, null, null, sortOrder);
                cursor.setNotificationUri(getContext().getContentResolver(), uri);
                return cursor;

            case TASKS_WITH_ID:
                db = mDbHelper.getWritableDatabase();
                cursor = db.query(DatabaseContract.TABLE_TASKS,
                        projection,
                        DatabaseContract.TaskColumns._ID + " = ?",
                        new String[]{uri.getLastPathSegment()}, null, null, sortOrder);
                cursor.setNotificationUri(getContext().getContentResolver(), uri);
                return cursor;

            default:
                throw new UnsupportedOperationException(UNIMPLEMENTED_DESCRIPTION);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        //TODO: Implement new task insert
        //TODO: Expected Uri: content://com.google.developer.taskmaker/tasks
        if (sUriMatcher.match(uri) == TASKS) {
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            long id = db.insert(DatabaseContract.TABLE_TASKS, null, values);
            Uri insertUri = Uri.withAppendedPath(DatabaseContract.CONTENT_URI, String.valueOf(id));
            db.close();
            notifyChanges(uri);
            return  insertUri;
        } else {
            throw new UnsupportedOperationException("Uri \n" + INVALID_URI_DESCRIPTION);
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        //TODO: Implement existing task update
        //TODO: Expected Uri: content://com.google.developer.taskmaker/tasks/{id}
        if (sUriMatcher.match(uri) == TASKS_WITH_ID) {
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            int modifiedLines = db.update(DatabaseContract.TABLE_TASKS,
                    values,
                    DatabaseContract.TaskColumns._ID + " = ?",
                    new String[]{uri.getLastPathSegment()});
            db.close();
            notifyChanges(uri);
            return modifiedLines;

        } else {
            throw new UnsupportedOperationException("Uri \n" + INVALID_URI_DESCRIPTION);
        }
    }


    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {

        switch (sUriMatcher.match(uri)) {
            case TASKS:
                //Rows aren't counted with null selection
                selection = (selection == null) ? "1" : selection;
                break;
            case TASKS_WITH_ID:
                long id = ContentUris.parseId(uri);
                selection = String.format("%s = ?", DatabaseContract.TaskColumns._ID);
                selectionArgs = new String[]{String.valueOf(id)};
                break;
            default:
                throw new IllegalArgumentException("Illegal delete URI");
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int count = db.delete(DatabaseContract.TABLE_TASKS, selection, selectionArgs);

        if (count > 0) {
            //Notify observers of the change
            notifyChanges(uri);
        }

        return count;
    }

    /* Initiate a periodic job to clear out completed items */
    private void manageCleanupJob() {
        Log.d(TAG, "Scheduling cleanup job");
        JobScheduler jobScheduler = (JobScheduler) getContext()
                .getSystemService(Context.JOB_SCHEDULER_SERVICE);

        //Run the job approximately every hour
        long jobInterval = 60 * 60 * 1000L;

        ComponentName jobService = new ComponentName(getContext(), CleanupJobService.class);
        JobInfo task = new JobInfo.Builder(CLEANUP_JOB_ID, jobService)
                .setMinimumLatency(jobInterval)
                .setPersisted(true)
                .build();

        if (jobScheduler.schedule(task) != JobScheduler.RESULT_SUCCESS) {
            Log.w(TAG, "Unable to schedule cleanup job");
        }
    }

    private void notifyChanges(Uri uri) {
        if (getContext() != null && getContext().getContentResolver() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
    }
}
