package com.grifon_kryuger.gfneko;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class GFNekoActivity extends PreferenceActivity {
  @Override
  public void onCreate(Bundle savedState) {
    super.onCreate(savedState);
    addPreferencesFromResource(R.xml.pref);

    getPreferenceManager().findPreference(AnimationService.PREF_KEY_ENABLE)
        .setOnPreferenceClickListener(new OnEnableClickListener());

    startAnimationService();
  }

  private void startAnimationService() {
    SharedPreferences.Editor edit =
        getPreferenceManager().getSharedPreferences().edit();
    edit.putBoolean(AnimationService.PREF_KEY_VISIBLE, true);
    edit.commit();

    startService(new Intent(this, AnimationService.class)
        .setAction(AnimationService.ACTION_START));
  }

  private class OnEnableClickListener
      implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(Preference pref) {
      startAnimationService();
      return true;
    }
  }
}
