package com.grifon_kryuger.gfneko;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class GFNekoActivity extends AppCompatActivity {

    public static String Prefs = "com.grifon_kryuger.gfneko_preferences";

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_neko);

        if(Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Permission for accessing storage");
            builder.setMessage("You need to permit ALL_FILES_ACCESS_PERMISSION to access internal storage.");
            builder.setCancelable(false);
            builder.setPositiveButton("PERMIT", (dialog, which) -> {
                try {
                    Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
                finish();
            });
            builder.setNegativeButton("CLOSE", (dialog, which) -> finish());
            builder.show();
        }

        if(Build.VERSION.SDK_INT > 22 && Build.VERSION.SDK_INT < 30 && checkStoragePermission(this)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

        if(Build.VERSION.SDK_INT > 22 && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            Toast.makeText(getApplicationContext(), "이 기능을 사용하려면 다른 앱 위에 그리기 기능이 필요합니다!", Toast.LENGTH_SHORT).show();
            startActivity(intent);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.neko_prefs, new SettingsFragment(GFNekoActivity.this))
                .commitNowAllowingStateLoss();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(Build.VERSION.SDK_INT > 22 && checkStoragePermission(this)) finish();
    }

    @RequiresApi(23)
    static boolean checkStoragePermission(Context context) {
        boolean b1 = context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean b2 = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        return !b1 || !b2;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        Context context;

        SettingsFragment(Context context) { this.context = context; }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            File dataFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/GFNeko/skins/");
            if(!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            setPreferencesFromResource(R.xml.pref, rootKey);
            SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "preferences", MODE_PRIVATE);
            Preference Service_Enable = findPreference(AnimationService.PREF_KEY_ENABLE);
            ListPreference Skin = findPreference("motion.skin");

            assert Service_Enable != null;
            Service_Enable.setOnPreferenceChangeListener((preference, newValue) -> {
                if((Boolean)newValue) startAnimationService();
                return true;
            });

            assert Skin != null;
            Skin.setEntries(getEntries("IDW the Many"));
            Skin.setEntryValues(getEntries(""));

            if(Skin.getEntries().length < 2) Skin.setValueIndex(0);
            if(prefs.getBoolean(Service_Enable.getKey(),false)) startAnimationService();
        }

        private void startAnimationService() {
            SharedPreferences.Editor edit = context.getSharedPreferences(Prefs, MODE_PRIVATE).edit();
            edit.putBoolean(AnimationService.PREF_KEY_VISIBLE, true).apply();
            context.startService(new Intent(context, AnimationService.class).setAction(AnimationService.ACTION_START));
        }

        private CharSequence[] getEntries(String PreValue) {
            String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/GFNeko/skins/";
            ArrayList<String> list = new ArrayList<>();
            list.add(PreValue);

            if(!(Build.VERSION.SDK_INT > 22 && Build.VERSION.SDK_INT < 30 && checkStoragePermission(context))) {
                if(new File(dir).exists()) {
                    for (File skinObj : Objects.requireNonNull(new File(dir).listFiles())) {
                        if (skinObj.isDirectory()) {
                            for (File files : Objects.requireNonNull(skinObj.listFiles())) {
                                if(files.isFile()) {
                                    String name = files.getName();
                                    if (name.substring(name.lastIndexOf(".") + 1).equals("xml")) {
                                        list.add(files.getAbsolutePath().replace(dir, ""));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            CharSequence[] listChar = new CharSequence[list.size()];
            for(int i = 0; i < list.size(); i++) {
                listChar[i] = list.get(i);
            }
            return listChar;
        }
    }
}