package com.joyboy.forcehz;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final int[] RATES = {60, 90, 120, 144};
    private static final long REFRESH_INTERVAL_MS = 500L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView currentRate;
    private TextView state;
    private TextView serviceState;

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshStatus();
            handler.postDelayed(this, REFRESH_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int pad = dp(20);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(pad, pad, pad, pad);
        content.setBackgroundColor(0xFFF7F7F7);

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextColor(0xFF111111);
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        content.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Vivo / iQOO refresh-rate controller");
        subtitle.setTextColor(0xFF666666);
        subtitle.setTextSize(14);
        LinearLayout.LayoutParams subtitleParams =
                new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(4);
        subtitleParams.bottomMargin = dp(24);
        content.addView(subtitle, subtitleParams);

        TextView currentLabel = new TextView(this);
        currentLabel.setText("ACTIVE REFRESH RATE");
        currentLabel.setTextColor(0xFF666666);
        currentLabel.setTextSize(12);
        currentLabel.setLetterSpacing(0.08f);
        content.addView(currentLabel);

        currentRate = new TextView(this);
        currentRate.setText("-- Hz");
        currentRate.setTextColor(0xFF111111);
        currentRate.setTextSize(48);
        currentRate.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams rateParams =
                new LinearLayout.LayoutParams(-1, -2);
        rateParams.topMargin = dp(2);
        rateParams.bottomMargin = dp(8);
        content.addView(currentRate, rateParams);

        serviceState = new TextView(this);
        serviceState.setTextColor(0xFF555555);
        serviceState.setTextSize(13);
        content.addView(serviceState);

        state = new TextView(this);
        state.setTextColor(0xFF222222);
        state.setTextSize(15);
        state.setPadding(0, dp(18), 0, dp(12));
        content.addView(state);

        TextView chooseLabel = new TextView(this);
        chooseLabel.setText("FORCE REFRESH RATE");
        chooseLabel.setTextColor(0xFF666666);
        chooseLabel.setTextSize(12);
        chooseLabel.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams chooseParams =
                new LinearLayout.LayoutParams(-1, -2);
        chooseParams.topMargin = dp(14);
        chooseParams.bottomMargin = dp(8);
        content.addView(chooseLabel, chooseParams);

        for (int fps : RATES) {
            Button rateButton = new Button(this);
            rateButton.setAllCaps(false);
            rateButton.setText(getString(R.string.force_rate, fps));
            rateButton.setTextSize(16);
            rateButton.setOnClickListener(v -> applyRate(fps));

            LinearLayout.LayoutParams buttonParams =
                    new LinearLayout.LayoutParams(-1, dp(54));
            buttonParams.bottomMargin = dp(8);
            content.addView(rateButton, buttonParams);
        }

        Button resetButton = new Button(this);
        resetButton.setAllCaps(false);
        resetButton.setText(R.string.reset_default);
        resetButton.setTextSize(16);
        resetButton.setOnClickListener(v -> resetToDefault());

        LinearLayout.LayoutParams resetParams =
                new LinearLayout.LayoutParams(-1, dp(54));
        resetParams.topMargin = dp(8);
        content.addView(resetButton, resetParams);

        TextView footnote = new TextView(this);
        footnote.setText(R.string.footer);
        footnote.setTextColor(0xFF777777);
        footnote.setTextSize(12);
        footnote.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footnoteParams =
                new LinearLayout.LayoutParams(-1, -2);
        footnoteParams.topMargin = dp(24);
        content.addView(footnote, footnoteParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(content);
        setContentView(scroll);

        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(refreshRunnable);
        handler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refreshRunnable);
        super.onPause();
    }

    private void applyRate(int fps) {
        boolean accepted = Rms.vote(this, fps);
        if (accepted) {
            Rms.saveSelectedRate(this, fps);
            state.setText(getString(R.string.vote_submitted, fps));
        } else {
            state.setText(R.string.vote_failed);
        }
        refreshStatus();
    }

    private void resetToDefault() {
        boolean released = Rms.reset();
        if (released) {
            Rms.saveSelectedRate(this, 0);
            state.setText(R.string.default_restored);
        } else {
            state.setText(R.string.reset_failed);
        }
        refreshStatus();
    }

    private void refreshStatus() {
        boolean available = Rms.isAvailable();
        serviceState.setText(available
                ? R.string.rms_connected
                : R.string.rms_unavailable);

        float rate = Rms.currentRate();
        currentRate.setText(rate > 0
                ? String.format(Locale.US, "%.1f Hz", rate)
                : "-- Hz");

        int selected = Rms.selectedRate(this);
        if (selected > 0) {
            state.setText(getString(R.string.pinned_status, selected));
        } else {
            state.setText(R.string.default_status);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
