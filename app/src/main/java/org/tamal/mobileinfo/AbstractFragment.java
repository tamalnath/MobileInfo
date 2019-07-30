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

    TextView addHeader(String header) {
        return addHeader(header, null);
    }

    TextView addHeader(String header, String url) {
        TextView textView = (TextView) getLayoutInflater().inflate(R.layout.view_header, null);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        setText(textView, header, url);
        viewGroup.addView(textView);
        return textView;
    }

    TextView[] addKeyValue(String key, String value) {
        return addKeyValue(key, null, value, null);
    }

    TextView[] addKeyValue(String key, String kURL, String value, String vURL) {
        View view = getLayoutInflater().inflate(R.layout.view_key_value, null);
        TextView[] textViews = {view.findViewById(R.id.key), view.findViewById(R.id.value)};
        setText(textViews[0], key, kURL);
        setText(textViews[1], value, vURL);
        viewGroup.addView(view);
        return textViews;
    }

}
