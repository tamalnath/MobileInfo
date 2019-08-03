package org.tamal.mobileinfo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public class HomeFragment extends AbstractFragment {

    private static final String GRANTED = "GRANTED";
    private static final String DENIED = "DENIED";
    private boolean requested;
    private int REQUEST_CODE;
    private Map<String, TextView> permissionMap = new HashMap<>();
    private Map<String, TextView> configMap;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        Activity activity = getActivity();
        if (activity == null) {
            return view;
        }
        requestPermissions(activity);
        addBatteryStatus(activity);
        addResourceDetails();
        addStaticData();
        return view;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Map<String, Object> map = getConfiguration(newConfig);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            TextView textView = configMap.get(entry.getKey());
            if (textView != null) {
                textView.setText(Utils.toString(entry.getValue(), "\n", "", "", null));
            }
        }
    }

    private void requestPermissions(Activity activity) {
        addHeader("Permissions", ROOT + "android/content/pm/PackageInfo.html#requestedPermissions");
        String[] permissions;
        try {
            permissions = activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), PackageManager.GET_PERMISSIONS)
                    .requestedPermissions;
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(activity, e.toString(), Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> deniedPermissions = new ArrayList<>();
        for (String permission : permissions) {
            int p = activity.checkSelfPermission(permission);
            String grant = GRANTED;
            if (p == PackageManager.PERMISSION_DENIED) {
                grant = DENIED;
                deniedPermissions.add(permission);
            }
            String[] split = permission.split("\\.");
            permission = split[split.length - 1];
            TextView[] kv = addKeyValue(permission, grant);
            permissionMap.put(permission, kv[1]);
        }
        if (!(deniedPermissions.isEmpty() || requested)) {
            String[] denied = deniedPermissions.toArray(new String[0]);
            REQUEST_CODE = this.getId() & 0xFFFF;
            activity.requestPermissions(denied, REQUEST_CODE);
            requested = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_CODE) {
            String message = "Expected Request Code: " + REQUEST_CODE + " but found: " + requestCode;
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
            return;
        }
        List<String> deniedPermissions = new ArrayList<>();
        for (int i = 0; i <= permissions.length; i++) {
            String[] split = permissions[i].split("\\.");
            String permission = split[split.length - 1];
            String grant = GRANTED;
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                grant = DENIED;
                deniedPermissions.add(permission);
            }
            TextView textView = permissionMap.get(permission);
            if (textView != null) {
                textView.setText(grant);
            }
        }
        String message = getString(R.string.permission_denied, Utils.toString(deniedPermissions, ", ", null, null, null));
        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    }

    private void addBatteryStatus(Context context) {
        addHeader(BatteryManager.class);

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, intentFilter);
        if (batteryStatus == null) {
            return;
        }
        boolean present = batteryStatus.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false);
        addKeyValue("Battery Present", present);
        if (!present) {
            return;
        }

        int key = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        String value = Utils.findConstant(BatteryManager.class, key, "BATTERY_STATUS_(.*)");
        addKeyValue("Battery Status", value);

        key = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
        value = Utils.findConstant(BatteryManager.class, key, "BATTERY_HEALTH_(.*)");
        addKeyValue("Battery Health", value);

        value = getString(R.string.unknown);
        key = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        if (key > 0) {
            value = Utils.findConstant(BatteryManager.class, key, "BATTERY_PLUGGED_(.*)");
        } else if (key == 0) {
            value = "Unplugged";
        }
        addKeyValue("Battery Plugged", value);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int percent = 100 * level / scale;
        addKeyValue("Battery Charge", percent);

        int voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        addKeyValue("Battery Voltage", (voltage / 1000f) + "V");

        float temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f;
        addKeyValue("Battery Temperature", temperature + getString(R.string.sensor_unit_deg));

        value = batteryStatus.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
        addKeyValue("Battery Technology", value);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            boolean low = batteryStatus.getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW, false);
            addKeyValue("Battery Low", low);
        }

    }

    private void addResourceDetails() {
        Resources resources = getResources();
        Map<String, Object> map = getConfiguration(resources.getConfiguration());
        addHeader(Configuration.class);
        configMap = addKeyValues(map);
        addHeader(DisplayMetrics.class);
        addKeyValues(Utils.findFields(resources.getDisplayMetrics()));
    }

    private Map<String, Object> getConfiguration(Configuration configuration) {
        Map<String, Object> map = Utils.findFields(configuration);
        map.putAll(Utils.findProperties(configuration));
        Utils.expand(map, "LayoutDirection", View.class, "LAYOUT_DIRECTION_(.*)");
        Utils.expand(map, "hardKeyboardHidden", Configuration.class, "HARDKEYBOARDHIDDEN_(.*)");
        Utils.expand(map, "keyboard", Configuration.class, "KEYBOARD_(.*)");
        Utils.expand(map, "keyboardHidden", Configuration.class, "KEYBOARDHIDDEN_(.*)");
        Utils.expand(map, "navigation", Configuration.class, "NAVIGATION_(.*)");
        Utils.expand(map, "navigationHidden", Configuration.class, "NAVIGATIONHIDDEN_(.*)");
        Utils.expand(map, "orientation", Configuration.class, "ORIENTATION_(.*)");
        int layout = (int) map.remove("screenLayout");
        String value = Utils.findConstant(Configuration.class, layout & Configuration.SCREENLAYOUT_SIZE_MASK, "SCREENLAYOUT_SIZE_(.*)");
        map.put("Screen Layout Size", value);
        value = Utils.findConstant(Configuration.class, layout & Configuration.SCREENLAYOUT_LONG_MASK, "SCREENLAYOUT_LONG_(.*)");
        map.put("Screen Layout Long", value);
        value = Utils.findConstant(Configuration.class, layout & Configuration.SCREENLAYOUT_LAYOUTDIR_MASK, "SCREENLAYOUT_LAYOUTDIR_(.*)");
        map.put("Screen Layout Direction", value);
        value = Utils.findConstant(Configuration.class, layout & Configuration.SCREENLAYOUT_ROUND_MASK, "SCREENLAYOUT_ROUND_(.*)");
        map.put("Screen Layout Round", value);
        Utils.expand(map, "touchscreen", Configuration.class, "TOUCHSCREEN_(.*)");
        int uiMode = (int) map.remove("uiMode");
        value = Utils.findConstant(Configuration.class, uiMode & Configuration.UI_MODE_TYPE_MASK, "UI_MODE_TYPE_(.*)");
        map.put("UI Mode Type", value);
        value = Utils.findConstant(Configuration.class, uiMode & Configuration.UI_MODE_NIGHT_MASK, "UI_MODE_NIGHT_(.*)");
        map.put("UI Mode Night", value);
        return map;
    }

    private void addStaticData() {
        addHeader(Build.class);
        Map<String, Object> build = Utils.findConstants(Build.class, null, null);
        addKeyValues(build);
        addHeader(Build.VERSION.class);
        Map<String, Object> VERSION = Utils.findConstants(Build.VERSION.class, null, null);
        addKeyValues(VERSION);
        String versionCode = Utils.findConstant(Build.VERSION_CODES.class, Build.VERSION.SDK_INT, null);
        addKeyValue("Version Code", versionCode);

        addHeader("Environment Variables", ROOT + "java/lang/System.html#getenv()");
        addKeyValues(System.getenv());
        addHeader("System Properties", ROOT + "java/lang/System.html#getProperties()");
        Properties properties = System.getProperties();
        Map<String, String> map = new TreeMap<>();
        for (String property : properties.stringPropertyNames()) {
            map.put(property, properties.getProperty(property));
        }
        addKeyValues(map);
    }

    @Override
    int getTitle() {
        return R.string.menu_home;
    }

    @Override
    int getIcon() {
        return R.drawable.ic_home;
    }


}
