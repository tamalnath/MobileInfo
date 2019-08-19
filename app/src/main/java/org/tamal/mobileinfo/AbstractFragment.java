package org.tamal.mobileinfo;

import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.constraintlayout.widget.Barrier;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractFragment extends Fragment {

    static final String TAG = "AbstractFragment";
    static final String ROOT = "https://developer.android.com/reference/";
    final int id = View.generateViewId();
    ViewGroup viewGroup;
    private static Field constrainedWidth;

    @StringRes
    abstract int getTitle();

    @DrawableRes
    abstract int getIcon();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.nested_scroll_view, container, false);
        viewGroup = view.findViewById(R.id.id_linear_layout);
        return view;
    }

    TextView addHeader(Class<?> cls) {
        while (cls.getComponentType() != null) {
            cls = cls.getComponentType();
        }
        if (cls.isPrimitive() || cls.isAssignableFrom(Number.class) || cls == String.class) {
            return null;
        }
        String url = cls.getCanonicalName();
        if (url != null) {
            url = ROOT + url.replace('.', '/') + ".html";
        }
        return addHeader(cls.getSimpleName(), url);
    }

    TextView addHeader(String header, String url) {
        TextView textView = (TextView) getLayoutInflater().inflate(R.layout.view_header, viewGroup, false);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        if (url == null) {
            textView.setText(header);
        } else {
            textView.setClickable(true);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            String hyperlink = String.format("<a href='%s'>%s</a>", url, header);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                textView.setText(Html.fromHtml(hyperlink, Html.FROM_HTML_MODE_COMPACT));
            } else {
                textView.setText(Html.fromHtml(hyperlink));
            }
        }
        viewGroup.addView(textView);
        return textView;
    }

    ConstraintLayout addMap(Map<?, ?> map) {
        ConstraintLayout layout = new ConstraintLayout(getContext());
        ConstraintSet set = new ConstraintSet();
        set.clone(layout);
        int barrierId = View.NO_ID;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            TextView key = buildTextView(set);
            key.setText(Utils.toString(entry.getKey()));
            key.setTypeface(Typeface.DEFAULT_BOLD);
            layout.addView(key);
            set.connect(key.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            if (barrierId == View.NO_ID) {
                set.connect(key.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            } else {
                set.connect(key.getId(), ConstraintSet.TOP, barrierId, ConstraintSet.BOTTOM);
            }
            TextView value = buildTextView(set);
            value.setText(Utils.toString(entry.getValue()));
            value.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            layout.addView(value);
            set.connect(value.getId(), ConstraintSet.START, key.getId(), ConstraintSet.END);
            set.connect(value.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
            set.connect(value.getId(), ConstraintSet.BASELINE, key.getId(), ConstraintSet.BASELINE);
            set.setHorizontalBias(value.getId(), 1.0f);
            constrainWidth(set, value.getId());

            barrierId = View.generateViewId();
            set.createBarrier(barrierId, Barrier.BOTTOM, key.getId(), value.getId());
        }
        set.applyTo(layout);
        viewGroup.addView(layout);
        return layout;
    }

    private TextView buildTextView(ConstraintSet set) {
        TextView textView = new TextView(getContext());
        int id = View.generateViewId();
        textView.setId(id);
        textView.setTextIsSelectable(true);
        set.constrainHeight(id, ConstraintSet.WRAP_CONTENT);
        set.constrainWidth(id, ConstraintSet.WRAP_CONTENT);
        return textView;
    }

    private void constrainWidth(ConstraintSet set, int viewId) {
        Object constraint = set.getParameters(viewId);
        try {
            if (constrainedWidth == null) {
                Object object = set.getParameters(viewId);
                constrainedWidth = object.getClass().getField("constrainedWidth");
                constrainedWidth.setAccessible(true);
            }
            constrainedWidth.setBoolean(constraint, true);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, e.toString());
        }

    }

    Map<String, TextView> addKeyValues(Map<String, ?> map) {
        Map<String, TextView> textViewMap = new HashMap<>();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            textViewMap.put(entry.getKey(), addKeyValue(entry.getKey(), entry.getValue()));
        }
        return textViewMap;
    }

    void updateKeyValues(Map<String, ?> map, Map<String, TextView> viewMap) {
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            TextView textView = viewMap.get(entry.getKey());
            if (textView != null) {
                textView.setText(Utils.toString(entry.getValue(), "\n", "", "", null));
            }
        }
    }

    TextView addKeyValue(String key, Object value) {
        View view = getLayoutInflater().inflate(R.layout.view_key_value, viewGroup, false);
        TextView keyView = view.findViewById(R.id.key);
        TextView valueView = view.findViewById(R.id.value);
        keyView.setText(key);
        valueView.setText(Utils.toString(value, "\n", "", "", null));
        viewGroup.addView(view);
        return valueView;
    }

}
