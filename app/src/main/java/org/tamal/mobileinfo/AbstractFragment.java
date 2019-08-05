package org.tamal.mobileinfo;

import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractFragment extends Fragment {

    static final String ROOT = "https://developer.android.com/reference/";
    final int id = View.generateViewId();
    ViewGroup viewGroup;

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
