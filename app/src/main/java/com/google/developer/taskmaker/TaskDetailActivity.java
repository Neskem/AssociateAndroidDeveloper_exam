package com.google.developer.taskmaker;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.developer.taskmaker.data.DatabaseContract;
import com.google.developer.taskmaker.data.Task;
import com.google.developer.taskmaker.data.TaskUpdateService;
import com.google.developer.taskmaker.reminders.AlarmManagerProvider;
import com.google.developer.taskmaker.reminders.AlarmScheduler;
import com.google.developer.taskmaker.views.DatePickerFragment;
import com.google.developer.taskmaker.views.TaskTitleView;

import java.util.Calendar;

public class TaskDetailActivity extends AppCompatActivity implements
        DatePickerDialog.OnDateSetListener {
    private long mDueDate = Long.MAX_VALUE;
    private Uri mUri;
    private TextView mDueDateView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_task);
        //Task must be passed to this activity as a valid provider Uri
        mUri = getIntent().getData();
        //TODO: Display attributes of the provided task in the UI
        initView(mUri);
    }

    private void initView(Uri uri) {
        TaskTitleView taskTitleView = (TaskTitleView) findViewById(R.id.detail_text_description);
        mDueDateView = (TextView) findViewById(R.id.detail_list_date);
        TextView dueDateLabel = (TextView) findViewById(R.id.due_date_label);
        ImageView priority = (ImageView) findViewById(R.id.detail_priority);

        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null) {
                cursor.moveToFirst();
                Task task = new Task(cursor);
                taskTitleView.setText(task.description);
                if (task.hasDueDate()) {
                    dueDateLabel.setVisibility(View.VISIBLE);
                    mDueDate = task.dueDateMillis;
                    CharSequence formatted = DateUtils
                            .getRelativeTimeSpanString(this, mDueDate);
                    mDueDateView.setText(formatted);
                } else {
                    dueDateLabel.setVisibility(View.GONE);
                }
                if (task.isPriority) {
                    priority.setImageResource(R.drawable.ic_priority);
                } else {
                    priority.setImageResource(R.drawable.ic_not_priority);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_task_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_delete) {
            deleteItem(mUri);
            backToMainActivity();
        }
        if (itemId == R.id.action_reminder) {
            DatePickerFragment datePickerFragment = DatePickerFragment.setArguments(true);
            FragmentManager fragmentManager = getSupportFragmentManager();
            datePickerFragment.show(fragmentManager, null);
        }

        if (itemId == R.id.home) {
            finish();
        }
        return false;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        //TODO: Handle date selection from a DatePickerFragment
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);

        setDateSelection(c.getTimeInMillis());
    }

    public void setDateSelection(long selectedTimestamp) {
            mDueDate = selectedTimestamp;
            updateDateDisplay();
            AlarmScheduler.scheduleAlarm(this, mDueDate, mUri);
    }

    private void updateDateDisplay() {
        if (getDateSelection() == Long.MAX_VALUE) {
            mDueDateView.setText(R.string.date_empty);
        } else {
            CharSequence formatted = DateUtils.getRelativeTimeSpanString(this, mDueDate);
            mDueDateView.setText(formatted);
        }
        updateItem(mUri);
    }

    public long getDateSelection() {
        return mDueDate;
    }

    private void deleteItem(Uri uri) {
        TaskUpdateService.deleteTask(this,uri);
        finish();
    }

    private void updateItem(Uri uri) {
        ContentValues values = new ContentValues(1);
        values.put(DatabaseContract.TaskColumns.DUE_DATE, getDateSelection());
        TaskUpdateService.updateTask(this, uri, values);
    }

    private void backToMainActivity() {
        Intent intent = new Intent();
        intent.setClass(this, MainActivity.class);
        startActivity(intent);
    }
}
