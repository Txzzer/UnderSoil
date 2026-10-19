package com.example.undersoil.settings;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.undersoil.R;

public class FaqActivity extends AppCompatActivity {

    // Each question is { question, answer }
    private static final String[][] SCANNING_FAQ = {
            {"Which crops can I scan?",
                    "Apple, cherry, corn (maize), grape, peach, bell pepper, potato, strawberry and tomato each have their own detection model. Other crops use a general plant disease model, which may be less accurate."},
            {"Do I need internet to scan a plant?",
                    "No. The plant disease detection runs on your phone, so you can scan in the field without a connection."},
            {"What does AI confidence mean?",
                    "It shows how sure the detector is about its diagnosis. If confidence is low, retake the photo in good light with a single leaf filling the frame."},
            {"The camera won't open. What can I do?",
                    "UnderSoil needs camera permission. Allow it in your phone's Settings > Apps > UnderSoil > Permissions, or tap \"Upload Image\" to use a photo from your gallery instead."},
            {"Where can I find my past scans?",
                    "Open My Garden and tap a plant. Its Crop Logs screen lists every scan under Scan History. Tap a log to see the full result."},
            {"How do I remove a plant from My Garden?",
                    "Tap the delete icon next to the plant in My Garden."}
    };

    private static final String[][] IOT_FAQ = {
            {"How often does the IoT dashboard update?",
                    "Every 3 seconds while the dashboard is open. The status in the top card shows \"● Live (3s)\" when data is coming in."},
            {"The status is red and the data isn't updating. Why?",
                    "The app can't reach your device. Check that the device is powered on and connected to Wi-Fi, and that your phone has internet. The app keeps retrying automatically, so the dashboard recovers once the device is back."},
            {"What is an unintended flow alert?",
                    "It means water is flowing while the valve is closed. This may be a pipe leak, a stuck valve or a sensor mismatch. You can open the valve, snooze the alert for 30 seconds, or jump to the IoT Dashboard to check. Alerts are also saved in the Alerts Log on the Home screen."},
            {"How do I schedule watering?",
                    "Open a sector's IoT Dashboard, tap \"Schedule Watering\", then choose a date and time."},
            {"Why can't I see Recent Valve Actions?",
                    "The valve history is hidden while the device is unreachable, so you never see out-of-date actions. It returns once the connection is restored."}
    };

    private static final String[][] ACCOUNT_FAQ = {
            {"Why does the app ask for my location?",
                    "Only to show the weather where you are on the Home screen. UnderSoil uses your approximate location, never your exact position, and doesn't save it to your account. If you turn it off, tap \"Location off\" on the Home screen to enable it again."},
            {"How do I log out?",
                    "Go to Settings and tap \"Logout\" at the bottom of the screen."}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq);

        ImageButton backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        LinearLayout container = findViewById(R.id.llFaq);
        addSection(container, "Scanning", SCANNING_FAQ, true);
        addSection(container, "IoT Watering", IOT_FAQ, false);
        addSection(container, "Account", ACCOUNT_FAQ, false);
    }

    private void addSection(LinearLayout container, String title, String[][] questions, boolean isFirst) {
        TextView label = new TextView(this);
        label.setText(title);
        label.setTextAppearance(R.style.TextAppearance_AgriTech_Label);
        label.setTextColor(ContextCompat.getColor(this, R.color.us_bio_green));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = isFirst ? 0 : dp(26);
        params.bottomMargin = dp(4);
        container.addView(label, params);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (String[] qa : questions) {
            View item = inflater.inflate(R.layout.item_faq, container, false);
            TextView tvAnswer = item.findViewById(R.id.tvAnswer);
            ImageView ivChevron = item.findViewById(R.id.ivChevron);

            ((TextView) item.findViewById(R.id.tvQuestion)).setText(qa[0]);
            tvAnswer.setText(qa[1]);

            item.findViewById(R.id.faqHeader).setOnClickListener(v -> {
                boolean expand = tvAnswer.getVisibility() != View.VISIBLE;
                tvAnswer.setVisibility(expand ? View.VISIBLE : View.GONE);
                ivChevron.animate().rotation(expand ? 180f : 0f).setDuration(200).start();
            });

            container.addView(item);
        }
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
