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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractFragment extends Fragment {

    final int id = View.generateViewId();
    private ViewGroup viewGroup;

    static void setText(TextView textView, String text, String url) {
        if (url == null) {
            textView.setText(text);
        } else {
            textView.setClickable(true);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            String hyperlink = String.format("<a href='%s'>%s</a>", url, text);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                textView.setText(Html.fromHtml(hyperlink, Html.FROM_HTML_MODE_COMPACT));
            } else {
                textView.setText(Html.fromHtml(hyperlink));
            }
        }
    }

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
        String url = Utils.getURL(cls);
        return addHeader(cls.getSimpleName(), url);
    }

    TextView addHeader(String header, String url) {
        TextView textView = (TextView) getLayoutInflater().inflate(R.layout.view_header, viewGroup, false);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        setText(textView, header, url);
        viewGroup.addView(textView);
        return textView;
    }

    List<TextView[]> addKeyValues(Set<KeyValue> keyValues) {
        List<TextView[]> list = new ArrayList<>(keyValues.size());
        for (KeyValue keyValue : keyValues) {
            list.add(addKeyValue(keyValue.key, keyValue.kUrl, keyValue.value, keyValue.vUrl));
        }
        return list;
    }

    List<TextView[]> addKeyValues(Map<?, ?> map) {
        List<TextView[]> list = new ArrayList<>(map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            list.add(addKeyValue(entry.getKey(), entry.getValue()));
        }
        return list;
    }

    TextView[] addKeyValue(Object key, Object value) {
        return addKeyValue(key, null, value, null);
    }

    TextView[] addKeyValue(Object key, String kURL, Object value, String vURL) {
        View view = getLayoutInflater().inflate(R.layout.view_key_value, viewGroup, false);
        TextView[] textViews = {view.findViewById(R.id.key), view.findViewById(R.id.value)};
        setText(textViews[0], Utils.toString(key), kURL);
        setText(textViews[1], Utils.toString(value, "\n", "", "", null), vURL);
        viewGroup.addView(view);
        return textViews;
    }

}
