package com.intelliviz.expensetracker;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by edm on 6/13/2015.
 */
public class SimpleButtonDialog extends DialogFragment {
    public static final String EXTRA_LABEL = "com.intelliviz.SimpleTextDialog.label";
    public static final String EXTRA_TITLE = "com.intelliviz.SimpleTextDialog.title";
    public static final int FRAG_ID = 1;
    private String mLabel;
    private String mTitle;
    private TextView mLabelText;
    private OnClickListener mListener;

    public interface OnClickListener {
        public void onClickOk(String text);
    }

    public SimpleButtonDialog() {
    }

    public static SimpleButtonDialog newInstance(String title, String label) {
        SimpleButtonDialog fragment = new SimpleButtonDialog();

        Bundle args = new Bundle();
        args.putSerializable(EXTRA_LABEL, label);
        args.putSerializable(EXTRA_TITLE, title);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.simple_button_layout, container, false);
        mLabelText = (TextView) view.findViewById(R.id.editTextLabel);

        Button okButton = (Button) view.findViewById(R.id.okSimpleButton);
        Button cancelButton = (Button) view.findViewById(R.id.cancelSimpleButton);

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SimpleButtonDialog.this.dismiss();
                sendResult(Activity.RESULT_OK);
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SimpleButtonDialog.this.dismiss();
                sendResult(Activity.RESULT_CANCELED);
            }
        });

        mLabelText.setText(mLabel);
        getDialog().setTitle(mTitle);
        setCancelable(false);

        return view;
    }

    /**
     * Do not set any UI here; it is not ready. Just read parameters passed in and save them.
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLabel = (String) getArguments().getSerializable(EXTRA_LABEL);
        mTitle = (String) getArguments().getSerializable(EXTRA_TITLE);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void sendResult(int resultCode) {
        if (getTargetFragment() == null) {
            return;
        }

        Intent i = new Intent();

        getActivity().sendBroadcast(i);
        getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, i);
    }
}
