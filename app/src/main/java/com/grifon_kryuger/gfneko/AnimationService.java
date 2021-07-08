package com.grifon_kryuger.gfneko;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

public class AnimationService extends Service {
    public static final String ACTION_START = "com.grifon_kryuger.gfneko.action.START";
    public static final String ACTION_STOP = "com.grifon_kryuger.gfneko.action.STOP";
    public static final String ACTION_TOGGLE = "com.grifon_kryuger.gfneko.action.TOGGLE";

    public static final String META_KEY_SKIN = "com.grifon_kryuger.gfneko.skin";

    public static final String PREF_KEY_ENABLE = "motion.enable";
    public static final String PREF_KEY_BATTERY = "battery.enable";
    public static final String PREF_KEY_VISIBLE = "motion.visible";
    public static final String PREF_KEY_TRANSPARENCY = "motion.transparency";
    public static final String PREF_KEY_SIZE = "motion.size";
    public static final String PREF_KEY_BEHAVIOUR = "motion.behaviour";
    public static final String PREF_KEY_SKIN_COMPONENT = "motion.skin";

    private static final int MSG_ANIMATE = 1;
    private static final int MSG_TXT = 1;

    private static final long ANIMATION_INTERVAL = 125; // msec
    private static final long TV_INTERVAL = 250; // msec
    // msec

    public static final String GFNEKO_SKINS = "/GFNeko/skins";

    private int image_width = 240;
    private int image_height = 240;

    boolean showTimeBattery = true;

    private enum Behaviour {
        closer, further, whimsical
    }

    private static final Behaviour[] BEHAVIOURS = {
            Behaviour.closer, Behaviour.further, Behaviour.whimsical};

    private boolean is_started;
    private SharedPreferences prefs;
    private PreferenceChangeListener pref_listener;

    private Handler handler;
    private Handler tvHandler;
    private MotionState motion_state = null;
    private Random random;
    private View touch_view = null;
    private ImageView image_view = null;
    private LayoutParams image_params = null;
    private BroadcastReceiver receiver = null;

    TextView textV;
    private LayoutParams textParams = null;

    ImageView balloonV;
    LayoutParams balloonParams = null;


    @Override
    public void onCreate() {
        is_started = false;
        handler = new Handler(this::onHandleMessage);
        tvHandler = new Handler(this::onTextVHandleMessage);
        random = new Random();
        prefs = getSharedPreferences(GFNekoActivity.Prefs , MODE_PRIVATE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!is_started &&
                (intent == null || ACTION_START.equals(intent.getAction()))) {
            startAnimation();
            setForegroundNotification(true);
            is_started = true;
        } else if (ACTION_TOGGLE.equals(intent.getAction())) {
            toggleAnimation();
        } else if (is_started &&
                ACTION_STOP.equals(intent.getAction())) {
            stopAnimation();
            stopSelfResult(startId);
            setForegroundNotification(false);
            is_started = false;
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onConfigurationChanged(Configuration conf) {
        if (!is_started || motion_state == null) {
            return;
        }

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        motion_state.setDisplaySize(width, height);
    }

    private void startAnimation() {
        pref_listener = new PreferenceChangeListener();
        prefs.registerOnSharedPreferenceChangeListener(pref_listener);

        if (!checkPrefEnable()) {
            return;
        }

        if (!loadMotionState()) {
            return;
        }

        // prepare to receive broadcast
        IntentFilter filter;
        receiver = new Receiver();

        filter = new IntentFilter();

        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);

//    filter.addAction(Intent.ACTION_PACKAGE_ADDED);
//    filter.addDataScheme("package");
        registerReceiver(receiver, filter);

//    filter = new IntentFilter();
//    filter.addAction(ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
//    registerReceiver(receiver, filter);

        // touch event sink and overlay view
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);

    /*
    
    https://thdev.net/665
    https://github.com/kpbird/android-global-touchevent
    https://stackoverflow.com/questions/32224452/android-unable-to-add-window-permission-denied-for-this-window-type
     */
        touch_view = new View(this);
        touch_view.setOnTouchListener(new TouchListener());
        //        0, 0,
        //        LayoutParams.TYPE_TOAST,
        //        (ICS_OR_LATER ?
        //            LayoutParams.TYPE_PHONE :
        ////            LayoutParams.TYPE_SYSTEM_ALERT :
        //            LayoutParams.TYPE_SYSTEM_OVERLAY),
        //            | LayoutParams.FLAG_LAYOUT_NO_LIMITS
        LayoutParams touch_params = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
//        0, 0,
//        LayoutParams.TYPE_TOAST,
                Build.VERSION.SDK_INT > 25 ? LayoutParams.TYPE_APPLICATION_OVERLAY : LayoutParams.TYPE_PHONE,
//        (ICS_OR_LATER ?
//            LayoutParams.TYPE_PHONE :
////            LayoutParams.TYPE_SYSTEM_ALERT :
//            LayoutParams.TYPE_SYSTEM_OVERLAY),

                LayoutParams.FLAG_NOT_FOCUSABLE
//            | LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | LayoutParams.FLAG_NOT_TOUCHABLE
                        | LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                ,
                PixelFormat.TRANSLUCENT);

//    touch_view.setBackgroundColor(0x4000FF00);

        touch_params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
        wm.addView(touch_view, touch_params);

        image_view = new ImageView(this);
        int OverlayParam = Build.VERSION.SDK_INT > 25 ? LayoutParams.TYPE_APPLICATION_OVERLAY : LayoutParams.TYPE_SYSTEM_OVERLAY;

        image_params = new LayoutParams(
                image_width, image_height,
//        LayoutParams.WRAP_CONTENT,
//        LayoutParams.WRAP_CONTENT,
                OverlayParam,
                LayoutParams.FLAG_NOT_FOCUSABLE |
                        LayoutParams.FLAG_NOT_TOUCHABLE |
                        LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        image_params.gravity = Gravity.START | Gravity.TOP;
        wm.addView(image_view, image_params);

//    image_view.setBackgroundColor(Color.YELLOW);
//    Log.d("AAAA", " image_params.width = " +     image_params.width
//    + "  image_params.height= " +     image_params.height) ;

        final int bW = 100;
        int bH = (int) (bW * .8);
        balloonV = new ImageView(this);
        balloonV.setImageResource(R.drawable.balloon);
        balloonParams = new LayoutParams(
                bW, bH,
//        image_width, LayoutParams.WRAP_CONTENT, // 최소크기
                OverlayParam,
                LayoutParams.FLAG_NOT_FOCUSABLE |
                        LayoutParams.FLAG_NOT_TOUCHABLE |
                        LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        balloonParams.gravity = Gravity.START | Gravity.TOP;
        wm.addView(balloonV, balloonParams);

        textV = new TextView(this);
        textParams = new LayoutParams(
//        image_width, image_height,
                bW, (int) (bH * 0.75), // 최소크기
                OverlayParam,
                LayoutParams.FLAG_NOT_FOCUSABLE |
                        LayoutParams.FLAG_NOT_TOUCHABLE |
                        LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        textParams.gravity = Gravity.START | Gravity.TOP | Gravity.CENTER;

//    textV.setBackgroundColor(0x8000FF00);
        textV.setTextColor(0xFF000000);
        textV.setGravity(Gravity.CENTER);

        textV.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
//    textV.setGravity(Gravity.CENTER);

        wm.addView(textV, textParams);
        setBalloonVisible(showTimeBattery);
        requestAnimate();
    }

    void setBalloonVisible(boolean v) {
        if (v) {
            textV.setVisibility(View.VISIBLE);
            balloonV.setVisibility(View.VISIBLE);
        } else {
            textV.setVisibility(View.INVISIBLE);
            balloonV.setVisibility(View.INVISIBLE);
        }
    }

    private void stopAnimation() {
        prefs.unregisterOnSharedPreferenceChangeListener(pref_listener);
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        if (touch_view != null) {
            wm.removeView(touch_view);
        }
        if (image_view != null) {
            wm.removeView(image_view);
        }

        if (textV != null) {
            wm.removeView(textV);
        }

        if (balloonV != null) {
            wm.removeView(balloonV);
        }

        if (receiver != null) {
            unregisterReceiver(receiver);
        }

        motion_state = null;
        touch_view = null;
        image_view = null;
        receiver = null;

        handler.removeMessages(MSG_ANIMATE);
        tvHandler.removeMessages(MSG_TXT);
    }

    private void toggleAnimation() {
        boolean visible = prefs.getBoolean(PREF_KEY_VISIBLE, true);

        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(PREF_KEY_VISIBLE, !visible);
        edit.apply();

        startService(new Intent(this, AnimationService.class)
                .setAction(ACTION_START));
    }

    private void setForegroundNotification(boolean start) {
        PendingIntent intent = PendingIntent.getService(
                this, 0,
                new Intent(this, AnimationService.class).setAction(ACTION_TOGGLE),
                0);

        if (Build.VERSION.SDK_INT > 25) {
            NotificationChannel channel = new NotificationChannel("ANeko", "ANeko Service",
                    NotificationManager.IMPORTANCE_MIN);
            ((NotificationManager) Objects.requireNonNull(getSystemService(Context.NOTIFICATION_SERVICE)))
                    .createNotificationChannel(channel);
        }

        Notification.Builder builder = Build.VERSION.SDK_INT > 25 ? new Notification.Builder(this, "ANeko") : new Notification.Builder(this);
        builder
                .setContentIntent(intent)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(start ? R.string.notification_enable : R.string.notification_disable))
                .setPriority(Notification.PRIORITY_LOW);

        Notification notif = builder.build();
        notif.flags = Notification.FLAG_ONGOING_EVENT;
        notif.flags = Notification.FLAG_AUTO_CANCEL;
        notif.flags = Notification.FLAG_ONLY_ALERT_ONCE;

        stopForeground(true);
        if (start) {
            startForeground(1, notif);
            return;
        }
        if (this.prefs.getBoolean(PREF_KEY_ENABLE, true)) {
            ((NotificationManager) Objects.requireNonNull(getSystemService(NOTIFICATION_SERVICE))).notify(1, notif);
        }
    }

    private boolean loadMotionState() {
        boolean loaded = false;
        String skinPath = prefs.getString(PREF_KEY_SKIN_COMPONENT, "");
//    showTimeBattery =  prefs.getBoolean(PREF_KEY_BATTERY, false);
        try {
            File externalStorageDirectory = Environment.getExternalStorageDirectory();
//      File gfnekoDir = new File(externalStorageDirectory, "/GFNeko");
//      gfnekoDir.mkdirs();
            File skinsDir = new File(externalStorageDirectory, GFNEKO_SKINS);
            skinsDir.mkdirs();

            String[] ts = skinPath.split("/");
            String folder = ts[0];
            String xmlFile = ts[1];
            File dir = new File(skinsDir, "/" + folder);

            PackageManager pm = getPackageManager();
            ComponentName skin_comp = new ComponentName(this, NekoSkin.class);
            Resources res = pm.getResourcesForActivity(skin_comp);

            MotionParams params2 = new MotionParams2(res, dir, xmlFile);
            motion_state = new MotionState();
            motion_state.setParams(params2);
            loaded = true;
        } catch (Exception e) {
            Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        if (loaded) {
            afterMotionLoaded();
            return true;
        }
        ComponentName skin_comp;
//    String skin_pkg = prefs.getString(PREF_KEY_SKIN_COMPONENT, null);
//    skin_comp =
//        (skin_pkg == null ? null :
//            ComponentName.unflattenFromString(skin_pkg));
//    if (skin_comp != null && loadMotionState(skin_comp)) {
//      return true;
//    }
        skin_comp = new ComponentName(this, NekoSkin.class);
        return loadMotionState(skin_comp);
    }

    private boolean loadMotionState(ComponentName skin_comp) {
        motion_state = new MotionState();
        try {
            PackageManager pm = getPackageManager();
            ActivityInfo ai = pm.getActivityInfo(
                    skin_comp, PackageManager.GET_META_DATA);
            Resources res = pm.getResourcesForActivity(skin_comp);
            int rid = ai.metaData.getInt(META_KEY_SKIN, 0);
            MotionParams params = new MotionParams(res, rid);
            motion_state.setParams(params);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.msg_skin_load_failed,
                    Toast.LENGTH_LONG)
                    .show();

            startService(new Intent(this, AnimationService.class)
                    .setAction(ACTION_TOGGLE));
            return false;
        }
        afterMotionLoaded();
        return true;
    }

    private void afterMotionLoaded() {
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int dw = size.x;
        int dh = size.y;
        int cx, cy;
        {
            int pos = random.nextInt(400);
            int ratio = pos % 100;
            if (pos / 200 == 0) {
                cx = (dw + 200) * ratio / 100 - 100;
                cy = ((pos / 100) % 2 == 0 ? -100 : dh + 100);
            } else {
                cx = ((pos / 100) % 2 == 0 ? -100 : dw + 100);
                cy = (dh + 200) * ratio / 100 - 100;
            }
        }

        String alpha_str = prefs.getString(PREF_KEY_TRANSPARENCY, "0.0");
        float opacity = 1 - Float.parseFloat(alpha_str);
        motion_state.alpha = (int) (opacity * 0xff);
//    textV.setAlpha(opacity);
        motion_state.setBehaviour(
                Behaviour.valueOf(
                        prefs.getString(PREF_KEY_BEHAVIOUR,
                                motion_state.behaviour.toString())));

        motion_state.setDisplaySize(dw, dh);
        motion_state.setCurrentPosition(cx, cy);
        motion_state.setTargetPositionDirect((float) dw / 2, (float) dh / 2);
        refreshMotionSize();
    }

    private void refreshMotionSize() {
        int v = 240;
        try {
            v = Integer.parseInt(prefs.getString(PREF_KEY_SIZE, "240"));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        this.image_width = this.image_height = v;

        WindowManager wm =
                (WindowManager) getSystemService(WINDOW_SERVICE);
        if (image_params != null && image_view != null) {
            image_params.width = v;
            image_params.height = v;
            wm.updateViewLayout(image_view, image_params);
        }
    }


    private void requestAnimate() {
        if (!handler.hasMessages(MSG_ANIMATE)) {
            handler.sendEmptyMessage(MSG_ANIMATE);
        }
        if (!tvHandler.hasMessages(MSG_TXT)) {
            tvHandler.sendEmptyMessage(MSG_TXT);
        }
    }

    private void updateDrawable() {
        if (motion_state == null || image_view == null) {
            return;
        }

        MotionDrawable drawable = motion_state.getCurrentDrawable();
        if (drawable == null) {
            return;
        }

        drawable.setAlpha(motion_state.alpha);
        image_view.setImageDrawable(drawable);
        drawable.stop();
        drawable.start();

        balloonV.getDrawable().setAlpha(motion_state.alpha);

    }

    private void updatePosition() {
        Point pt = motion_state.getPosition();
        image_params.x = pt.x;
        image_params.y = pt.y;

        WindowManager wm =
                (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.updateViewLayout(image_view, image_params);

        if (
                textV != null
                        && balloonV != null) {
            int w = image_view.getWidth();
            int cx = pt.x + w / 2;
            balloonParams.x = cx - balloonV.getWidth() / 2;
            balloonParams.y = pt.y - (int) (balloonV.getHeight() * 0.8);
            textParams.x = cx - textV.getWidth() / 2;
            textParams.y = balloonParams.y + 3;
            wm.updateViewLayout(textV, textParams);
            wm.updateViewLayout(balloonV, balloonParams);
        }
    }

    private void updateToNext() {
        if (motion_state.checkWall() ||
                motion_state.updateMovingState() ||
                motion_state.changeToNextState()) {
            updateDrawable();
            updatePosition();
            requestAnimate();
        }
    }

    private boolean onTextVHandleMessage(Message msg) {
        tvHandler.removeMessages(msg.what);
        if (showTimeBattery) {
            Date dt = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm", Locale.getDefault());
            String ts = dateFormat.format(dt);
            if (batteryInfo != null) {
//      batteryInfo.getLevel() + batteryInfo.get
                textV.setText(String.format(Locale.getDefault(), "%s\n%d%%", ts, batteryInfo.level));
            } else {
                textV.setText(String.format("%s\nno battery info.", ts));
            }
            tvHandler.sendEmptyMessageDelayed(MSG_TXT, TV_INTERVAL);
        }
        return true;
    }

    private boolean onHandleMessage(Message msg) {
        if (msg.what == MSG_ANIMATE) {
            handler.removeMessages(MSG_ANIMATE);
            motion_state.updateState();
            if (motion_state.isStateChanged() ||
                    motion_state.isPositionMoved()) {
                if (motion_state.isStateChanged()) {
                    updateDrawable();
                }

                updatePosition();
                handler.sendEmptyMessageDelayed(
                        MSG_ANIMATE, ANIMATION_INTERVAL);
            }
        } else {
            return false;
        }
        return true;
    }

    private boolean checkPrefEnable() {
        boolean enable = prefs.getBoolean(PREF_KEY_ENABLE, true);
        boolean visible = prefs.getBoolean(PREF_KEY_VISIBLE, true);
        showTimeBattery = prefs.getBoolean(PREF_KEY_BATTERY, false);

        if (!enable || !visible) {
            startService(new Intent(this, AnimationService.class)
                    .setAction(ACTION_STOP));
            return false;
        } else {
            return true;
        }
    }

    private class PreferenceChangeListener
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (PREF_KEY_ENABLE.equals(key) || PREF_KEY_VISIBLE.equals(key)) {
                checkPrefEnable();
            } else if (PREF_KEY_SIZE.equals(key)) {
                refreshMotionSize();
            } else if (PREF_KEY_BATTERY.equals(key)) {
                showTimeBattery = prefs.getBoolean(PREF_KEY_BATTERY, false);
                setBalloonVisible(showTimeBattery);
            } else if (loadMotionState()) {
                requestAnimate();
            }
        }
    }

    BatteryInfo batteryInfo;
    boolean batteryOk = true;

    private class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                AnimationService.this.batteryInfo = new BatteryInfo(intent);
                requestAnimate();
//        Intent updateIntent = new Intent(context, UpdateService.class);
//        updateIntent.setAction(UpdateService.ACTION_BATTERY_CHANGED);
//        batteryInfo.saveToIntent(updateIntent);
//        context.startService(updateIntent);
            } else if (Intent.ACTION_BATTERY_LOW.equals(intent.getAction())) {
                batteryOk = false;
                requestAnimate();
//        Intent updateIntent = new Intent(context, UpdateService.class);
//        updateIntent.setAction(UpdateService.ACTION_BATTERY_LOW);
//        context.startService(updateIntent);
            } else if (Intent.ACTION_BATTERY_OKAY.equals(intent.getAction())) {
                batteryOk = true;
                requestAnimate();
//        Intent updateIntent = new Intent(context, UpdateService.class);
//        updateIntent.setAction(UpdateService.ACTION_BATTERY_OKAY);
//        context.startService(updateIntent);
            }

//      String[] pkgnames = null;
//      if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction()) &&
//          intent.getData() != null) {
//        pkgnames = new String[]{
//            intent.getData().getEncodedSchemeSpecificPart()};
//      } else if (ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(
//          intent.getAction())) {
//        pkgnames = intent.getStringArrayExtra(
//            EXTRA_CHANGED_PACKAGE_LIST);
//      }
//      if (pkgnames == null) {
//        return;
//      }
//
//      String skin = prefs.getString(PREF_KEY_SKIN_COMPONENT, null);
//      ComponentName skin_comp =
//          (skin == null ? null :
//              ComponentName.unflattenFromString(skin));
//      if (skin_comp == null) {
//        return;
//      }
//
//      String skin_pkg = skin_comp.getPackageName();
//      for (String pkgname : pkgnames) {
//        if (skin_pkg.equals(pkgname)) {
//          if (loadMotionState()) {
//            requestAnimate();
//          }
//          break;
//        }
//      }
        }
    }

    private class TouchListener implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent ev) {
            Log.d("AAAA", ev.getRawX() + " , " + ev.getRawY() + " , act=" + ev.getAction());
//      Log.d("AAAA", "image: " + image_view.getWidth() + " , " + image_view.getHeight());

            if (motion_state == null) {
                return false;
            }

            if (ev.getAction() == MotionEvent.ACTION_OUTSIDE) {
                motion_state.setTargetPosition();
//        motion_state.setTargetPosition(ev.getX(), ev.getY());
                requestAnimate();
            } else if (ev.getAction() == MotionEvent.ACTION_CANCEL) {
                motion_state.forceStop();
                requestAnimate();
            }
            return true;
        }
    }

    private class MotionState {
        private float cur_x = 0;
        private float cur_y = 0;
        private float target_x = 0;
        private float target_y = 0;
        private float vx = 0;                   // pixels per sec
        private float vy = 0;                   // pixels per sec

        private int display_width = 1;
        private int display_height = 1;

        private MotionParams params;
        private int alpha = 0xff;

        //    private Behaviour behaviour = Behaviour.closer;
        private Behaviour behaviour = Behaviour.whimsical;

        private String cur_state = null;

        private boolean moving_state = false;
        private boolean state_changed = false;
        private boolean position_moved = false;

        private final MotionEndListener on_motion_end = new MotionEndListener();

        private void updateState() {
            state_changed = false;
            position_moved = false;

            float dx = target_x - cur_x;
            float dy = target_y - cur_y;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len <= params.getProximityDistance()) {
                if (moving_state) {
                    vx = 0;
                    vy = 0;
                    changeState(params.getInitialState());
                }
                return;
            }

            if (!moving_state) {
                String nstate = params.getAwakeState();
                if (params.hasState(nstate)) {
                    changeState(nstate);
                }
                return;
            }

            float interval = ANIMATION_INTERVAL / 1000f;

            float acceleration = params.getAcceleration();
            float max_velocity = params.getMaxVelocity();
            float deaccelerate_distance = params.getDeaccelerationDistance();

            vx += acceleration * interval * dx / len;
            vy += acceleration * interval * dy / len;
            float vec = (float) Math.sqrt(vx * vx + vy * vy);
            float vmax = max_velocity *
                    Math.min((len + 1) / (deaccelerate_distance + 1), 1);
            if (vec > vmax) {
                float vr = vmax / vec;
                vx *= vr;
                vy *= vr;
            }

            cur_x += vx * interval;
            cur_y += vy * interval;
            position_moved = true;

            changeToMovingState();
        }

        private boolean checkWall() {
            if (!params.needCheckWall(cur_state)) {
                return false;
            }

            MotionDrawable drawable = getCurrentDrawable();
            float dw2 = drawable.getIntrinsicWidth() / 2f;
            float dh2 = drawable.getIntrinsicHeight() / 2f;

            MotionParams.WallDirection dir;
            float nx = cur_x;
            float ny = cur_y;
            if (cur_x >= 0 && cur_x < dw2) {
                nx = dw2;
                dir = MotionParams.WallDirection.LEFT;
            } else if (cur_x <= display_width && cur_x > display_width - dw2) {
                nx = display_width - dw2;
                dir = MotionParams.WallDirection.RIGHT;
            } else if (cur_y >= 0 && cur_y < dh2) {
                ny = dh2;
                dir = MotionParams.WallDirection.UP;
            } else if (cur_y <= display_height && cur_y > display_height - dh2) {
                ny = display_height - dh2;
                dir = MotionParams.WallDirection.DOWN;
            } else {
                return false;
            }

            String nstate = params.getWallState(dir);
            if (!params.hasState(nstate)) {
                return false;
            }

            cur_x = target_x = nx;
            cur_y = target_y = ny;
            changeState(nstate);

            return true;
        }

        private boolean updateMovingState() {
            if (!params.needCheckMove(cur_state)) {
                return false;
            }

            float dx = target_x - cur_x;
            float dy = target_y - cur_y;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len <= params.getProximityDistance()) {
                return false;
            }

            changeToMovingState();
            return true;
        }

        private void setParams(MotionParams _params) {
            String nstate = _params.getInitialState();
            if (!_params.hasState(nstate)) {
                throw new IllegalArgumentException(
                        "Initial State does not exist");
            }

            params = _params;

            changeState(nstate);
            moving_state = false;
        }

        private void changeState(String state) {
            if (state.equals(cur_state)) {
                return;
            }

            cur_state = state;
            state_changed = true;
            moving_state = false;
            getCurrentDrawable().setOnMotionEndListener(on_motion_end);
        }

        private boolean changeToNextState() {
            String next_state = params.getNextState(motion_state.cur_state);
            if (next_state == null) {
                return false;
            }

            changeState(next_state);
            return true;
        }

        private void changeToMovingState() {
//      int dir = (int) (Math.atan2(vy, vx) * 4 / Math.PI + 8.5) % 8;
//      MotionParams.MoveDirection dirs[] = {
//          MotionParams.MoveDirection.RIGHT,
//          MotionParams.MoveDirection.DOWN_RIGHT,
//          MotionParams.MoveDirection.DOWN,
//          MotionParams.MoveDirection.DOWN_LEFT,
//          MotionParams.MoveDirection.LEFT,
//          MotionParams.MoveDirection.UP_LEFT,
//          MotionParams.MoveDirection.UP,
//          MotionParams.MoveDirection.UP_RIGHT
//      };
//      String nstate = params.getMoveState(dirs[dir]);

            MotionParams.MoveDirection d = vx >= 0
                    ? MotionParams.MoveDirection.RIGHT
                    : MotionParams.MoveDirection.LEFT;

            String nstate = params.getMoveState(d);
            if (!params.hasState(nstate)) {
                return;
            }

            changeState(nstate);
            moving_state = true;
        }

        private void setDisplaySize(int w, int h) {
            display_width = w;
            display_height = h;
        }

        private void setBehaviour(Behaviour b) {
            behaviour = b;

            for (Behaviour value : BEHAVIOURS) {
                if (value == behaviour) {
                    break;
                }
            }
        }

        private void setCurrentPosition(float x, float y) {
            cur_x = x;
            cur_y = y;
        }

        private void setTargetPosition() {

//      Log.d("AAAA", "x=" + x + " y=" + y);

            float min_wh2 = Math.min(display_width, display_height) / 2f;
            float r = random.nextFloat() * min_wh2 + min_wh2;
            float a = random.nextFloat() * 360;
            float nx = cur_x + r * (float) Math.cos(a);
            float ny = cur_y + r * (float) Math.sin(a);

            nx = (nx < 0 ? -nx :
                    nx >= display_width ? display_width * 2 - nx - 1 :
                            nx);
            ny = (ny < 0 ? -ny :
                    ny >= display_height ? display_height * 2 - ny - 1 :
                            ny);
            setTargetPositionDirect(nx, ny);
        }

        private void setTargetPositionDirect(float x, float y) {
            target_x = x;
            target_y = y;
        }

        private void forceStop() {
            setTargetPosition();
            vx = 0;
            vy = 0;
        }

        private boolean isStateChanged() {
            return state_changed;
        }

        private boolean isPositionMoved() {
            return position_moved;
        }

        private MotionDrawable getCurrentDrawable() {
            return params.getDrawable(cur_state);
        }

        private Point getPosition() {
            MotionDrawable drawable = getCurrentDrawable();
            return new Point((int) (cur_x - drawable.getIntrinsicWidth() / 2f),
                    (int) (cur_y - drawable.getIntrinsicHeight() / 2f));
        }
    }

    private class MotionEndListener
            implements MotionDrawable.OnMotionEndListener {
        @Override
        public void onMotionEnd(MotionDrawable drawable) {
            if (is_started && motion_state != null &&
                    drawable == motion_state.getCurrentDrawable()) {
                updateToNext();
            }
        }
    }
}
