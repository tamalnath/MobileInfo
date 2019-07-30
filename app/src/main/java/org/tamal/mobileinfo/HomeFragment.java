package org.tamal.mobileinfo;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

public class HomeFragment extends AbstractFragment {

    private static final String P_URL = "https://developer.android.com/reference/android/Manifest.permission.html#";
    private static final String PERM_URL = "https://developer.android.com/reference/android/content/pm/PackageManager.html#PERMISSION_";
    private static final String GRANTED = "GRANTED";
    private static final String DENIED = "DENIED";
    private boolean requested;
    private int REQUEST_CODE;
    private Map<String, TextView> permissionMap = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        grantPermissions();
        return view;
    }

    private void grantPermissions() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        addHeader("Permissions", "https://developer.android.com/reference/android/content/pm/PackageInfo.html#requestedPermissions");
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
            TextView[] kv = addKeyValue(permission, P_URL + permission, grant, PERM_URL + grant);
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
            setText(permissionMap.get(permission), grant, PERM_URL + grant);
        }
        String message = getString(R.string.permission_denied, Utils.toString(deniedPermissions, ", ", null, null, null));
        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
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
