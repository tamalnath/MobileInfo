package org.tamal.mobileinfo;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class FontsFragment extends AbstractFragment implements CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener, TextWatcher, View.OnClickListener {

    private Switch bold;
    private Switch italic;
    private SeekBar size;
    private EditText sampleText;
    private Map<String, Typeface> fonts;
    private Map<String, TextView> textViews = new HashMap<>();

    @SuppressWarnings("unchecked")
    public FontsFragment() {
        try {
            String fieldName = "sSystemFontMap";
            Field field = Typeface.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            fonts = new TreeMap((Map<String, Typeface>) field.get(null));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Toast.makeText(getContext(), e.toString(), Toast.LENGTH_SHORT).show();
            fonts = Utils.findConstants(Typeface.class, Typeface.class, null);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ConstraintLayout layout = (ConstraintLayout) inflater.inflate(R.layout.fragment_fonts, container, false);
        viewGroup = layout.findViewById(R.id.id_linear_layout);
        bold = layout.findViewById(R.id.bold);
        italic = layout.findViewById(R.id.italic);
        size = layout.findViewById(R.id.size);
        sampleText = layout.findViewById(R.id.sample);
        bold.setOnCheckedChangeListener(this);
        italic.setOnCheckedChangeListener(this);
        size.setOnSeekBarChangeListener(this);
        sampleText.addTextChangedListener(this);
        final int style = getStyle();
        for (final Map.Entry<String, Typeface> entry : fonts.entrySet()) {
            final Typeface typeface = entry.getValue();
            TextView textView = addKeyValue(entry.getKey(), sampleText.getText());
            textView.setTypeface(typeface, style);
            textView.setTextSize(size.getProgress() + 8);
            textView.setOnClickListener(this);
            textViews.put(entry.getKey(), textView);
        }
        return layout;
    }

    private int getStyle() {
        int boldStyle = bold.isChecked() ? 0x1 : 0x0;
        int italicStyle = italic.isChecked() ? 0x2 : 0x0;
        return boldStyle | italicStyle;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        for (Map.Entry<String, TextView> entry : textViews.entrySet()) {
            entry.getValue().setTypeface(fonts.get(entry.getKey()), getStyle());
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // Don't do anything
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // Don't do anything
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        for (TextView textView : textViews.values()) {
            textView.setTextSize(size.getProgress() + 8);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Don't do anything
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        for (TextView textView : textViews.values()) {
            textView.setText(sampleText.getText());
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        // Don't do anything
    }

    @Override
    public void onClick(View v) {
        TextView textView = (TextView) v;
        sampleText.setTypeface(textView.getTypeface());
    }

    @Override
    int getTitle() {
        return R.string.menu_fonts;
    }

    @Override
    int getIcon() {
        return R.drawable.ic_text;
    }

}
