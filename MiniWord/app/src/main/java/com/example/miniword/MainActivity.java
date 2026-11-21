package com.example.miniword;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private EditText input;
    private TextView result, status;
    private Button search;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(36), dp(24), dp(24));
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("极简词典");
        title.setTextSize(26);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.rgb(30, 30, 30));
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("输入英文单词，查询中文意思");
        hint.setTextSize(14);
        hint.setTextColor(Color.GRAY);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(8);
        root.addView(hint, hp);

        input = new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(18);
        input.setHint("例如 apple");
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, -2);
        ip.topMargin = dp(28);
        root.addView(input, ip);

        search = new Button(this);
        search.setText("查询");
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.topMargin = dp(12);
        root.addView(search, bp);

        status = new TextView(this);
        status.setText("");
        status.setTextSize(14);
        status.setTextColor(Color.GRAY);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(24);
        root.addView(status, sp);

        result = new TextView(this);
        result.setTextSize(23);
        result.setTextColor(Color.rgb(25, 25, 25));
        result.setTypeface(null, Typeface.BOLD);
        result.setTextIsSelectable(true);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.topMargin = dp(12);
        root.addView(result, rp);

        setContentView(root);
        search.setOnClickListener(v -> lookup());
        input.setOnEditorActionListener((v, actionId, event) -> {
            lookup();
            return true;
        });
    }

    private void lookup() {
        String word = input.getText().toString().trim();
        if (word.isEmpty()) {
            status.setText("请输入英文单词");
            result.setText("");
            return;
        }

        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(input.getWindowToken(), 0);
        search.setEnabled(false);
        status.setText("查询中…");
        result.setText("");

        executor.execute(() -> {
            String translation = null;
            String error = null;
            HttpURLConnection conn = null;
            try {
                String encoded = URLEncoder.encode(word, "UTF-8");
                URL url = new URL("https://api.mymemory.translated.net/get?q="
                        + encoded + "&langpair=en|zh-CN");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("Accept", "application/json");

                int code = conn.getResponseCode();
                if (code != 200) throw new Exception("服务暂时不可用（" + code + "）");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
                reader.close();

                JSONObject json = new JSONObject(body.toString());
                JSONObject data = json.optJSONObject("responseData");
                translation = data == null ? null : data.optString("translatedText", "");
                if (translation == null || translation.trim().isEmpty())
                    throw new Exception("没有查到释义，请换个单词试试");
                translation = translation.replace("&#39;", "'")
                        .replace("&quot;", "\"").replace("&amp;", "&");
            } catch (Exception e) {
                error = e.getMessage() == null ? "网络错误，请稍后重试" : e.getMessage();
            } finally {
                if (conn != null) conn.disconnect();
            }

            final String answer = translation;
            final String message = error;
            runOnUiThread(() -> {
                search.setEnabled(true);
                if (message != null) {
                    status.setText(message);
                } else {
                    status.setText(word);
                    result.setText(answer);
                }
            });
        });
    }

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
