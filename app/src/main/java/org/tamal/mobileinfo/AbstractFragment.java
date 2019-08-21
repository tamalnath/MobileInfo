package org.tamal.mobileinfo;

import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.constraintlayout.widget.Barrier;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractFragment extends Fragment {

    private static final String TAG = "AbstractFragment";
    static final String ROOT = "https://developer.android.com/reference/";
    Adapter adapter;
    private static Field constrainedWidth;

    @StringRes
    abstract int getTitle();

    @DrawableRes
    abstract int getIcon();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.recycler_view, container, false);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new Adapter();
        recyclerView.setAdapter(adapter);
        return recyclerView;
    }

    Decorator addHeader(Class<?> cls) {
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

    Decorator addHeader(String text, String url) {
        final CharSequence header;
        if (url == null) {
            header = text;
        } else {
            String hyperlink = String.format("<a href='%s'>%s</a>", url, text);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                header = Html.fromHtml(hyperlink, Html.FROM_HTML_MODE_COMPACT);
            } else {
                header = Html.fromHtml(hyperlink);
            }
        }
        Decorator decorator = new Decorator() {
            @Override
            public void decorate(ViewHolder viewHolder) {
                TextView textView = (TextView) viewHolder.itemView;
                if (header instanceof Spanned) {
                    textView.setClickable(true);
                    textView.setMovementMethod(LinkMovementMethod.getInstance());
                }
                textView.setText(header);
            }

            @Override
            public int getViewType() {
                return R.layout.view_header;
            }
        };
        adapter.add(decorator);
        return decorator;
    }

    class Adapter extends RecyclerView.Adapter<ViewHolder> {

        private List<Decorator> list = new ArrayList<>();

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, @LayoutRes int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            list.get(position).decorate(holder);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        @LayoutRes
        @Override
        public int getItemViewType(int position) {
            return list.get(position).getViewType();
        }

        void add(Decorator decorator) {
            int position = list.indexOf(decorator);
            if (position == -1) {
                list.add(decorator);
                adapter.notifyItemInserted(list.size() - 1);
            } else {
                list.remove(decorator);
                list.add(position, decorator);
                adapter.notifyItemChanged(position);
            }
        }

        boolean remove(Decorator decorator) {
            int position = list.indexOf(decorator);
            if (position != -1) {
                list.remove(position);
                adapter.notifyItemRemoved(position);
            }
            return position != -1;
        }

    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ViewHolder(View itemView) {
            super(itemView);
        }
    }

    interface Decorator {
        void decorate(ViewHolder viewHolder);

        @LayoutRes
        int getViewType();
    }

    class KeyValues implements Decorator {

        private Map<?, ?> map;
        boolean verticalOrientation;

        KeyValues set(Map<?, ?> map) {
            this.map = map == null ? Collections.emptyMap() : Collections.unmodifiableMap(map);
            adapter.add(this);
            return this;
        }

        @Override
        public void decorate(ViewHolder viewHolder) {
            ConstraintLayout layout = (ConstraintLayout) viewHolder.itemView;
            layout.removeAllViews();
            ConstraintSet set = new ConstraintSet();
            set.clone(layout);
            int barrierId = View.NO_ID;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                TextView key = buildTextView(set);
                key.setTypeface(Typeface.DEFAULT_BOLD);
                layout.addView(key);
                set.connect(key.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                if (barrierId == View.NO_ID) {
                    set.connect(key.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                } else {
                    set.connect(key.getId(), ConstraintSet.TOP, barrierId, ConstraintSet.BOTTOM);
                }
                TextView value = buildTextView(set);
                value.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
                layout.addView(value);
                if (verticalOrientation) {
                    set.connect(value.getId(), ConstraintSet.TOP, key.getId(), ConstraintSet.BOTTOM);
                    barrierId = value.getId();
                } else {
                    set.connect(value.getId(), ConstraintSet.START, key.getId(), ConstraintSet.END);
                    set.connect(value.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
                    set.connect(value.getId(), ConstraintSet.BASELINE, key.getId(), ConstraintSet.BASELINE);
                    set.setHorizontalBias(value.getId(), 1.0f);
                    constrainWidth(set, value.getId());

                    barrierId = View.generateViewId();
                    set.createBarrier(barrierId, Barrier.BOTTOM, key.getId(), value.getId());
                }
                decorate(entry.getKey(), entry.getValue(), key, value);
            }
            set.applyTo(layout);
        }

        void decorate(Object key, Object value, TextView keyView, TextView valueView) {
            keyView.setText(Utils.toString(key));
            valueView.setText(Utils.toString(value));
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


        @Override
        public int getViewType() {
            return R.layout.constraint_layout;
        }
    }
}
