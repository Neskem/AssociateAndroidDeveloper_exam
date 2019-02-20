package com.google.developer.taskmaker;

import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.developer.taskmaker.data.DatabaseContract;
import com.google.developer.taskmaker.data.Task;
import com.google.developer.taskmaker.data.TaskAdapter;
import com.google.developer.taskmaker.data.TaskProvider;
import com.google.developer.taskmaker.data.TaskUpdateService;

public class MainActivity extends AppCompatActivity implements
        TaskAdapter.OnItemClickListener,
        View.OnClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "MainActivity";
    private TaskAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mAdapter = new TaskAdapter(null);
        mAdapter.setOnItemClickListener(this);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewTask();
            }
        });

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* Click events in Floating Action Button */
    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, AddTaskActivity.class);
        startActivity(intent);
    }

    /* Click events in RecyclerView items */
    @Override
    public void onItemClick(View v, int position) {
        //TODO: Handle list item click event
        Task task = mAdapter.getItem(position);
        Uri uri = Uri.withAppendedPath(DatabaseContract.CONTENT_URI, String.valueOf(task.id));
        intentToDetailActivity(uri);
    }

    /* Click events on RecyclerView item checkboxes */
    @Override
    public void onItemToggled(boolean active, int position) {
        //TODO: Handle task item checkbox event
        Task task = mAdapter.getItem(position);
        Uri uri = Uri.withAppendedPath(DatabaseContract.CONTENT_URI, String.valueOf(task.id));
        ContentValues values = new ContentValues(1);
        values.put(DatabaseContract.TaskColumns.IS_COMPLETE, active);
        TaskUpdateService.updateTask(this, uri, values);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(TAG, "onCreateLoader");
        String key = getStringPreference(getString(R.string.pref_sortBy_key));
        String dueSort = getString(R.string.pref_sortBy_due);
        if (key.equals(dueSort)) {
            return new CursorLoader(this, DatabaseContract.CONTENT_URI, null, null, null,
                    DatabaseContract.DATE_SORT);
        } else {
            return new CursorLoader(this, DatabaseContract.CONTENT_URI, null, null, null,
                    DatabaseContract.DEFAULT_SORT);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(TAG, "onLoadFinished");
        mAdapter.swapCursor(data);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(TAG, "onLoaderReset");
        mAdapter.swapCursor(null);
    }

    private void intentToDetailActivity(Uri data) {
        Intent intent = new Intent();
        intent.setClass(this, TaskDetailActivity.class);
        intent.setData(data);
        startActivity(intent);
    }

    private void createNewTask() {
        Intent intent = new Intent();
        intent.setClass(this, AddTaskActivity.class);
        startActivity(intent);
    }

    private String getStringPreference(String key) {
        String value = null;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences != null) {
            value = sharedPreferences.getString(key, "");
        }
        return value;
    }
}
