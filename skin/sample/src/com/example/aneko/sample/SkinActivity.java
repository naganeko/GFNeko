package com.example.gfneko.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

public class SkinActivity extends Activity
{
    private static final String GFNEKO_PACKAGE = "com.grifon_kryuger.gfneko";
    private static final String GFNEKO_ACTIVITY =
        "com.grifon_kryuger.gfneko.GFNekoActivity";

    private static final Uri GFNEKO_MARKET_URI =
        Uri.parse("market://search?q=" + GFNEKO_PACKAGE);

    @Override
    public void onResume()
    {
        super.onResume();

        boolean package_found = false;
        try {
            PackageManager pm = getPackageManager();
            package_found = (pm.getPackageInfo(GFNEKO_PACKAGE, 0) != null);
        }
        catch(PackageManager.NameNotFoundException e) {
            // ignore
        }

        int msg_id;
        final Intent intent;
        if(package_found) {
            msg_id = R.string.msg_usage;
            intent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setClassName(GFNEKO_PACKAGE, GFNEKO_ACTIVITY);
        }
        else {
            msg_id = R.string.msg_no_package;
            intent = new Intent(Intent.ACTION_VIEW, GFNEKO_MARKET_URI);
        }

        new AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(msg_id)
            .setPositiveButton(
                android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            startActivity(intent);
                        }
                        catch(ActivityNotFoundException e) {
                            e.printStackTrace();
                            Toast.makeText(SkinActivity.this,
                                           R.string.msg_unexpected_err,
                                           Toast.LENGTH_SHORT)
                                .show();
                        }

                        finish();
                    }
                })
            .setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
            .show();
    }
}
