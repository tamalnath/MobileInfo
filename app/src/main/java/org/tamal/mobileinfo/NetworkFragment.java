package org.tamal.mobileinfo;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class NetworkFragment extends AbstractFragment {

    private static final Map<String, Integer> CAPABILITIES = Utils.findConstants(NetworkCapabilities.class, int.class, "NET_CAPABILITY_(.+)");
    private static final Map<String, Integer> TRANSPORT = Utils.findConstants(NetworkCapabilities.class, int.class, "TRANSPORT_(.+)");
    private static final String BANDWIDTH = "Bandwidth";
    private static final String NET_TRANSPORT = "Network Transport";
    private static final String NET_CAPABILITIES = "Network Capabilities";
    private ConnectivityManager connectivityManager;
    private NetworkCallback callback = new NetworkCallback();
    private TextView networkState;
    private Map<String, TextView> networkInfo;
    private Map<String, TextView> networkCapabilities;
    private Map<String, TextView> linkProperties;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        networkState = addKeyValue("Network State", "Unavailable");
        addHeader(NetworkInfo.class);
        networkInfo = buildKeyValues(Utils.findProperties(NetworkInfo.class).keySet());
        addHeader(NetworkCapabilities.class);
        networkCapabilities = buildKeyValues(Arrays.asList(BANDWIDTH, NET_CAPABILITIES, NET_TRANSPORT));
        addHeader(LinkProperties.class);
        linkProperties = buildKeyValues(Utils.findProperties(LinkProperties.class).keySet());
        final Context context = getContext();
        if (context == null) {
            return view;
        }
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest request = new NetworkRequest.Builder().build();
        connectivityManager.registerNetworkCallback(request, callback);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        connectivityManager.unregisterNetworkCallback(callback);
    }

    private Map<String, TextView> buildKeyValues(Collection<String> keys) {
        Map<String, String> map = new TreeMap<>();
        for (String key : keys) {
            map.put(key, "");
        }
        return addKeyValues(map);
    }

    private void updateNetworkDetails(Network network, String state) {
        networkState.setText(state);
        if (network == null || state.equals("Lost")) {
            for (TextView textView : networkInfo.values()) {
                textView.setText("");
            }
            for (TextView textView : networkCapabilities.values()) {
                textView.setText("");
            }
            for (TextView textView : linkProperties.values()) {
                textView.setText("");
            }
            return;
        }
        Map<String, Object> info = Utils.findProperties(connectivityManager.getNetworkInfo(network));
        updateKeyValues(info, networkInfo);
        Map<String, String> capabilities = new TreeMap<>();
        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
        if (networkCapabilities != null) {
            capabilities.put(BANDWIDTH, getString(R.string.network_bandwidth, networkCapabilities.getLinkUpstreamBandwidthKbps()/1024, networkCapabilities.getLinkDownstreamBandwidthKbps()/1024));
            StringBuilder transport = new StringBuilder();
            for (Map.Entry<String, Integer> entry : TRANSPORT.entrySet()) {
                if (networkCapabilities.hasTransport(entry.getValue())) {
                    transport.append('\n').append(entry.getKey());
                }
            }
            capabilities.put(NET_TRANSPORT, transport.substring(1));
            StringBuilder capability = new StringBuilder();
            for (Map.Entry<String, Integer> entry : CAPABILITIES.entrySet()) {
                if (networkCapabilities.hasCapability(entry.getValue())) {
                    capability.append('\n').append(entry.getKey());
                }
            }
            capabilities.put(NET_CAPABILITIES, capability.substring(1));
            if (this.networkCapabilities == null) {
                addHeader(NetworkCapabilities.class);
                this.networkCapabilities = addKeyValues(capabilities);
            } else {
                updateKeyValues(capabilities, this.networkCapabilities);
            }
        }
        Map<String, Object> link = Utils.findProperties(connectivityManager.getLinkProperties(network));
        updateKeyValues(link, linkProperties);
    }

    private class NetworkCallback extends ConnectivityManager.NetworkCallback {

        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);
            updateNetworkDetails(network, "Available");
        }

        @Override
        public void onLosing(Network network, int maxMsToLive) {
            super.onLosing(network, maxMsToLive);
            updateNetworkDetails(network, "Losing (" + maxMsToLive + "ms)");
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);
            updateNetworkDetails(network, "Lost");
        }

        @Override
        public void onUnavailable() {
            super.onUnavailable();
            updateNetworkDetails(null, "Unavailable");
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            updateNetworkDetails(network, "Capabilities Changed");
        }

        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
            super.onLinkPropertiesChanged(network, linkProperties);
            updateNetworkDetails(network, "Link Properties Changed");
        }

        private void updateNetworkDetails(final Network network, final String state) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                public void run()
                {
                    NetworkFragment.this.updateNetworkDetails(network, state);
                }
            });
        }
    }

    @Override
    int getTitle() {
        return R.string.menu_network;
    }

    @Override
    int getIcon() {
        return R.drawable.ic_network;
    }

}
