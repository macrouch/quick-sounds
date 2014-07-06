package com.sleekcoder.quick_sounds;

import android.app.DialogFragment;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ToggleButton;

import java.util.List;

public class ButtonDialogFragment extends DialogFragment {
    private static final String colSoundText = "Text";
    private static final String colSoundPath = "Path";
    private static final String colSoundLoop = "Loop";
    DataAccess da = new DataAccess();
    int buttonId;

    static ButtonDialogFragment newInstance(int bId) {
        ButtonDialogFragment f = new ButtonDialogFragment();

        Bundle args = new Bundle();
        args.putInt("buttonId", bId);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buttonId = getArguments().getInt("buttonId");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.set_button_dialog, container);

        getDialog().setTitle((R.string.buttonDialogTitle));
        EditText text = (EditText)view.findViewById(R.id.editButtonText);

        text.setText(da.getSound(buttonId, colSoundText, view.getContext()));

        //handles the select sound button
        Button selectButton = (Button)view.findViewById(R.id.buttonSelectSound);
        selectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("audio/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                //only use the Google/Android music choosers
                String type1 = "google";
                String type2 = "android";
                boolean found = false;
                //gets the list of intents that can be loaded
                List<ResolveInfo> resInfo = view.getContext().getPackageManager().queryIntentActivities(intent, 0);
                if (!resInfo.isEmpty())
                {
                    for (ResolveInfo info : resInfo)
                    {
                        if (info.activityInfo.packageName.toLowerCase().contains(type1) ||
                                info.activityInfo.name.toLowerCase().contains(type1) ||
                                info.activityInfo.packageName.toLowerCase().contains(type2) ||
                                info.activityInfo.name.toLowerCase().contains(type2))
                        {
                            intent.setPackage(info.activityInfo.packageName);
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                        return;
                }

                getActivity().startActivityForResult(Intent.createChooser(intent, "Select file"), buttonId);
            }
        });

        //handles the loop toggle button
        ToggleButton loopButton = (ToggleButton)view.findViewById(R.id.loopButton);
        loopButton.setChecked((da.getSound(buttonId, colSoundLoop, view.getContext()).equals("true")));
        loopButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                da.setButton(buttonId, null, null, isChecked, view.getContext());
            }
        });

        //handles the finish button
        Button finishButton = (Button)view.findViewById(R.id.finishButton);
        finishButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //set text preference
                EditText editButtonText = (EditText)view.findViewById(R.id.editButtonText);
                String buttonText = editButtonText.getText().toString();

                //find loop button
                ToggleButton loopButton = (ToggleButton)view.findViewById(R.id.loopButton);
                boolean loop = loopButton.isChecked();

                //setStringPreferences("text"+bId, buttonText);
                da.setButton(buttonId, buttonText, null, loop, view.getContext());

                //reload preferences
                ((QuickSoundsActivity)getActivity()).setupButtons();
                ((QuickSoundsActivity)getActivity()).dismissButtonDialog();
            }
        });

        return view;
    }
}
