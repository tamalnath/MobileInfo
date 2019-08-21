package org.tamal.mobileinfo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorDirectChannel;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SensorsFragment extends AbstractFragment implements SensorEventListener {

    private static final int DELAY_MILLIS = 500;
    private static final Map<String, Float> GRAVITY = Utils.findConstants(SensorManager.class, float.class, "GRAVITY_(.+)");
    private static final Map<String, Float> LIGHT = Utils.findConstants(SensorManager.class, float.class, "LIGHT_(.+)");

    private SensorManager sensorManager;
    private List<Sensor> sensors;
    private TriggerListener triggerListener = new TriggerListener();
    private Map<Sensor, Long> sensorUpdateMap = new HashMap<>();
    private Map<Sensor, TextView> sensorValuesMap = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.nested_scroll_view, container, false);
        Activity activity = getActivity();
        if (activity == null) {
            return rootView;
        }
        sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        sensors = new ArrayList<>(sensorManager.getSensorList(Sensor.TYPE_ALL));
        Collections.sort(sensors, new Comparator<Sensor>() {
            @Override
            public int compare(Sensor s1, Sensor s2) {
                return s1.getStringType().compareTo(s2.getStringType());
            }
        });
        LinearLayout layout = rootView.findViewById(R.id.linear_layout);
        ViewGroup sensorDetails = (ViewGroup) inflater.inflate(R.layout.sensor_details, rootView, false);
        for (Sensor sensor : sensors) {
            LinearLayout linearLayout = new LinearLayout(getContext());
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            TextView sensorNameView = new TextView(getContext());
            sensorNameView.setTypeface(Typeface.DEFAULT_BOLD);
            sensorNameView.setText(sensor.getName());
            linearLayout.addView(sensorNameView);
            TextView sensorValueView = new TextView(getContext());
            linearLayout.addView(sensorValueView);
            linearLayout.setOnClickListener(new SensorDetailsClickListener(sensor, sensorDetails));
            layout.addView(linearLayout);
            sensorValuesMap.put(sensor, sensorValueView);
        }
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        for (Sensor sensor : sensors) {
            switch (sensor.getReportingMode()) {
                case Sensor.REPORTING_MODE_CONTINUOUS:
                case Sensor.REPORTING_MODE_ON_CHANGE:
                case Sensor.REPORTING_MODE_SPECIAL_TRIGGER:
                    sensorManager.registerListener(this, sensor, 1000 * DELAY_MILLIS);
                    break;
                case Sensor.REPORTING_MODE_ONE_SHOT:
                    sensorManager.requestTriggerSensor(triggerListener, sensor);
                    break;
                default:
                    Toast.makeText(getContext(), "onResume: Unknown Reporting Mode: " + sensor.getReportingMode(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        for (Sensor sensor : sensors) {
            switch (sensor.getReportingMode()) {
                case Sensor.REPORTING_MODE_CONTINUOUS:
                case Sensor.REPORTING_MODE_ON_CHANGE:
                case Sensor.REPORTING_MODE_SPECIAL_TRIGGER:
                    sensorManager.unregisterListener(this, sensor);
                    break;
                case Sensor.REPORTING_MODE_ONE_SHOT:
                    sensorManager.cancelTriggerSensor(triggerListener, sensor);
                    break;
                default:
                    Toast.makeText(getContext(), "onPause: Unknown Reporting Mode: " + sensor.getReportingMode(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        Long lastUpdated = sensorUpdateMap.get(sensor);
        long now = System.currentTimeMillis();
        if (lastUpdated != null && now - lastUpdated < DELAY_MILLIS) {
            return;
        }
        sensorUpdateMap.put(sensor, now);
        String unit = getUnit(sensor.getType());
        String value;
        float[] v = event.values;
        float magnitude;
        switch (sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_ACCELEROMETER_UNCALIBRATED:
            case Sensor.TYPE_LINEAR_ACCELERATION:
            case Sensor.TYPE_GRAVITY:
                magnitude = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
                value = getString(R.string.sensor_values_xyz_unit, v[0], v[1], v[2], unit);
                value += " (" + findNearest(GRAVITY, magnitude) + ")";
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                value = getString(R.string.sensor_values_xyz_unit, v[0], v[1], v[2], unit);
                break;
            case Sensor.TYPE_GYROSCOPE:
                value = getString(R.string.sensor_values_xyz_unit, v[0], v[1], v[2], unit);
                break;
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                value = getString(R.string.sensor_values_gyroscope_uncalibrated, v[0], v[1], v[2], v[3], v[4], v[5]);
                break;
            case Sensor.TYPE_ORIENTATION:
                value = getString(R.string.sensor_values_xyz_unit, v[2], v[0], v[1], unit);
                break;
            case Sensor.TYPE_PROXIMITY:
                if (event.values[0] == 0) {
                    value = getString(R.string.sensor_value_near);
                } else if (event.values[0] == sensor.getMaximumRange()) {
                    value = getString(R.string.sensor_value_far);
                } else {
                    value = getString(R.string.sensor_value_unit, event.values[0], unit);
                }
                break;
            case Sensor.TYPE_LIGHT:
                value = getString(R.string.sensor_value_unit, event.values[0], unit);
                value += " (" + findNearest(LIGHT, event.values[0]) + ")";
                break;
            case Sensor.TYPE_STEP_COUNTER:
                value = getString(R.string.sensor_value_unit, event.values[0], unit);
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
                value = getString(R.string.sensor_values_rotation, v[0], v[1], v[2], v[3]);
                break;
            default:
                StringBuilder sb = new StringBuilder();
                for (float val : v) {
                    sb.append(String.format(Locale.getDefault(), ", %+.2f", val));
                }
                value = sb.substring(1);
        }
        TextView valueView = sensorValuesMap.get(sensor);
        if (valueView != null) {
            valueView.setText(value);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private static String findNearest(Map<String, Float> map, float value) {
        float absValue = Math.abs(value);
        String name = null;
        float minDelta = Float.MAX_VALUE;
        for (Map.Entry<String, Float> entry : map.entrySet()) {
            float delta = Math.abs(entry.getValue() - absValue);
            if (delta < minDelta) {
                minDelta = delta;
                name = entry.getKey();
            }
        }
        if (name == null) {
            return null;
        }
        return name;
    }

    @SuppressWarnings("deprecation")
    private String getUnit(int sensorType) {
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_ACCELEROMETER_UNCALIBRATED:
            case Sensor.TYPE_LINEAR_ACCELERATION:
            case Sensor.TYPE_GRAVITY:
                return getString(R.string.sensor_unit_ms2);
            case Sensor.TYPE_MAGNETIC_FIELD:
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                return getString(R.string.sensor_unit_ut);
            case Sensor.TYPE_GYROSCOPE:
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                return getString(R.string.sensor_unit_rad);
            case Sensor.TYPE_ORIENTATION:
                return getString(R.string.sensor_unit_deg);
            case Sensor.TYPE_PROXIMITY:
                return getString(R.string.sensor_unit_cm);
            case Sensor.TYPE_LIGHT:
                return getString(R.string.sensor_unit_lx);
            case Sensor.TYPE_PRESSURE:
                return getString(R.string.sensor_unit_pascal);
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                return getString(R.string.sensor_unit_percent);
            case Sensor.TYPE_STEP_COUNTER:
                return getString(R.string.sensor_unit_step);
            case Sensor.TYPE_TEMPERATURE:
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                return getString(R.string.sensor_unit_centigrade);
        }
        return "";
    }

    private class SensorDetailsClickListener implements View.OnClickListener {

        private Sensor sensor;
        private ViewGroup viewGroup;

        SensorDetailsClickListener(Sensor sensor, ViewGroup sensorDetails) {
            this.sensor = sensor;
            this.viewGroup = sensorDetails;
        }

        @Override
        public void onClick(View v) {
            String sensorType = Utils.findConstant(Sensor.class, sensor.getType(), "TYPE_(.+)");
            String unit = getUnit(sensor.getType());
            TextView view;
            view = viewGroup.findViewById(R.id.sensor_id);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                view.setText(String.valueOf(sensor.getId()));
            } else {
                view.setVisibility(View.GONE);
                viewGroup.findViewById(R.id.sensor_id_label).setVisibility(View.GONE);
            }
            view = viewGroup.findViewById(R.id.sensor_type);
            view.setText(sensorType);
            view = viewGroup.findViewById(R.id.sensor_vendor);
            view.setText(sensor.getVendor());
            view = viewGroup.findViewById(R.id.sensor_version);
            view.setText(String.valueOf(sensor.getVersion()));
            view = viewGroup.findViewById(R.id.sensor_power);
            view.setText(getString(R.string.sensor_power_unit, sensor.getPower()));
            view = viewGroup.findViewById(R.id.sensor_delay);
            view.setText(getString(R.string.sensor_delay_unit, sensor.getMinDelay(), sensor.getMaxDelay()));
            view = viewGroup.findViewById(R.id.sensor_resolution);
            view.setText(getString(R.string.sensor_resolution_unit, sensor.getResolution(), unit));
            view = viewGroup.findViewById(R.id.sensor_max_range);
            view.setText(getString(R.string.sensor_value_unit, sensor.getMaximumRange(), unit));
            view = viewGroup.findViewById(R.id.sensor_reserved_event);
            view.setText(getString(R.string.sensor_event_unit, sensor.getFifoReservedEventCount()));
            view = viewGroup.findViewById(R.id.sensor_max_event);
            view.setText(getString(R.string.sensor_event_unit, sensor.getFifoMaxEventCount()));
            view = viewGroup.findViewById(R.id.sensor_direct_report_rate);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                view.setText(Utils.findConstant(SensorDirectChannel.class, sensor.getHighestDirectReportRateLevel(), "RATE_(.*)"));
            } else {
                view.setVisibility(View.GONE);
                viewGroup.findViewById(R.id.sensor_direct_report_rate_label).setVisibility(View.GONE);
            }
            view = viewGroup.findViewById(R.id.sensor_reporting_mode);
            view.setText(Utils.findConstant(Sensor.class, sensor.getReportingMode(), "REPORTING_MODE_(.*)"));
            view = viewGroup.findViewById(R.id.sensor_dynamic);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                view.setText(String.valueOf(sensor.isDynamicSensor()));
            } else {
                view.setVisibility(View.GONE);
                viewGroup.findViewById(R.id.sensor_dynamic_label).setVisibility(View.GONE);
            }
            view = viewGroup.findViewById(R.id.sensor_wake_up);
            view.setText(String.valueOf(sensor.isWakeUpSensor()));
            view = viewGroup.findViewById(R.id.sensor_additional_info);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                view.setText(String.valueOf(sensor.isAdditionalInfoSupported()));
            } else {
                view.setVisibility(View.GONE);
                viewGroup.findViewById(R.id.sensor_additional_info_label).setVisibility(View.GONE);
            }
            Context context = getContext();
            if (context != null) {
                new AlertDialog.Builder(getContext())
                        .setTitle(sensor.getName())
                        .setView(viewGroup)
                        .setPositiveButton("Dismiss", null)
                        .show();
            }
        }
    }

    class TriggerListener extends TriggerEventListener {
        public void onTrigger(TriggerEvent event) {
            Sensor sensor = event.sensor;
            TextView valueView = sensorValuesMap.get(sensor);
            if (valueView != null) {
                String value = getString(R.string.sensor_no_values, event.timestamp);
                valueView.setText(value);
            }
        }
    }

    @Override
    int getTitle() {
        return R.string.menu_sensors;
    }

    @Override
    int getIcon() {
        return R.drawable.ic_sensors;
    }

}
