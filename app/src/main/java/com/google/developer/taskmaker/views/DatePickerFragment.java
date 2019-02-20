package com.google.developer.taskmaker.views;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import java.util.Calendar;

/* Wrapper to show a managed date picker */
public class DatePickerFragment extends DialogFragment {
    private static final String DUE_DATE = "due_date";

    public static DatePickerFragment setArguments(boolean dueDate) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(DUE_DATE, dueDate);

        DatePickerFragment datePickerFragment = new DatePickerFragment();
        datePickerFragment.setArguments(bundle);
        return datePickerFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        Bundle bundle = getArguments();
        boolean dueDateCanNotChooseBefore = bundle.getBoolean(DUE_DATE);
        // Create a new instance of DatePickerDialog and return it
        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(),
                (DatePickerDialog.OnDateSetListener) getActivity(), year, month, day);
        if (dueDateCanNotChooseBefore) {
            datePickerDialog.getDatePicker().setMinDate(c.getTimeInMillis());
        }
        return datePickerDialog;
    }
}
