package com.example.miniword;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private EditText input;
    private TextView result, status;
    private Button search;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static class Sense {
        String pos, definition, example;
        Sense(String p, String d, String e) {
            pos = p; definition = d; example = e;
        }
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(30), dp(22), dp(28));
        root.setBackgroundColor(Color.rgb(248, 249, 252));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("MiniWord 极简词典");
        title.setTextSize(26);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.rgb(30, 41, 59));
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("查单词 · 看词性 · 学释义 · 记搭配");
        hint.setTextSize(14);
        hint.setTextColor(Color.rgb(100, 116, 139));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(7);
        root.addView(hint, hp);

        input = new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(18);
        input.setHint("输入英文单词，例如 prevail");
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, -2);
        ip.topMargin = dp(24);
        root.addView(input, ip);

        search = new Button(this);
        search.setText("查询单词");
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.topMargin = dp(10);
        root.addView(search, bp);

        status = new TextView(this);
        status.setTextSize(14);
        status.setTextColor(Color.rgb(100, 116, 139));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(18);
        root.addView(status, sp);

        result = new TextView(this);
        result.setTextSize(16);
        result.setTextColor(Color.rgb(30, 41, 59));
        result.setLineSpacing(dp(3), 1.0f);
        result.setTextIsSelectable(true);
        result.setPadding(dp(16), dp(16), dp(16), dp(20));
        result.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.topMargin = dp(10);
        root.addView(result, rp);

        setContentView(scroll);
        search.setOnClickListener(v -> lookup());
        input.setOnEditorActionListener((v, actionId, event) -> {
            lookup();
            return true;
        });
    }

    private void lookup() {
        final String word = input.getText().toString().trim().toLowerCase();
        if (word.isEmpty()) {
            status.setText("请先输入英文单词");
            result.setText("");
            return;
        }
        if (!word.matches("[a-zA-Z][a-zA-Z'-]*")) {
            status.setText("请输入英文单词，不要输入句子或特殊字符");
            result.setText("");
            return;
        }

        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(input.getWindowToken(), 0);
        search.setEnabled(false);
        status.setText("正在查询词典和中文释义，请稍候…");
        result.setText("");

        executor.execute(() -> {
            String answer = null;
            String error = null;
            try {
                answer = buildDictionaryResult(word);
            } catch (Exception e) {
                error = e.getMessage() == null ? "查询失败，请检查网络后重试" : e.getMessage();
            }
            final String finalAnswer = answer;
            final String finalError = error;
            runOnUiThread(() -> {
                search.setEnabled(true);
                if (finalError != null) {
                    status.setText(finalError);
                    result.setText("暂时没有获取到词典内容。\n\n请检查网络连接，稍后再试。");
                } else {
                    status.setText("查询完成 · " + word);
                    result.setText(finalAnswer);
                }
            });
        });
    }

    private String buildDictionaryResult(String word) throws Exception {
        String encoded = URLEncoder.encode(word, "UTF-8");
        String jsonText = httpGet("https://api.dictionaryapi.dev/api/v2/entries/en/" + encoded);
        JSONArray entries = new JSONArray(jsonText);
        String phonetic = "";
        ArrayList<Sense> senses = new ArrayList<>();

        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.optJSONObject(i);
            if (entry == null) continue;

            if (phonetic.isEmpty()) {
                phonetic = entry.optString("phonetic", "");
                JSONArray phonetics = entry.optJSONArray("phonetics");
                if (phonetic.isEmpty() && phonetics != null) {
                    for (int p = 0; p < phonetics.length(); p++) {
                        JSONObject item = phonetics.optJSONObject(p);
                        if (item != null) {
                            String value = item.optString("text", "");
                            if (!value.isEmpty()) {
                                phonetic = value;
                                break;
                            }
                        }
                    }
                }
            }

            JSONArray meanings = entry.optJSONArray("meanings");
            if (meanings == null) continue;
            for (int m = 0; m < meanings.length() && senses.size() < 6; m++) {
                JSONObject meaning = meanings.optJSONObject(m);
                if (meaning == null) continue;
                String pos = meaning.optString("partOfSpeech", "other");
                JSONArray definitions = meaning.optJSONArray("definitions");
                if (definitions == null) continue;

                for (int d = 0; d < definitions.length() && senses.size() < 6; d++) {
                    JSONObject def = definitions.optJSONObject(d);
                    if (def == null) continue;
                    String text = def.optString("definition", "").trim();
                    String example = def.optString("example", "").trim();
                    if (!text.isEmpty()) senses.add(new Sense(pos, text, example));
                }
            }
        }

        if (senses.isEmpty()) throw new Exception("没有找到这个单词，请检查拼写");

        StringBuilder out = new StringBuilder();
        out.append(word).append("\n");
        if (!phonetic.isEmpty()) out.append("音标：").append(phonetic).append("\n");
        out.append("━━━━━━━━━━━━━━━━━━\n");
        out.append("【常用中文释义】\n");
        String wordChinese = safeTranslate(word);
        out.append(wordChinese.isEmpty() ? "可查看下面各条详细释义。" : wordChinese);
        out.append("\n\n【词性与详细释义】\n");

        int number = 1;
        int translationCount = 0;
        for (Sense sense : senses) {
            out.append(number++).append(". ").append(posChinese(sense.pos))
                    .append("（").append(sense.pos).append("）\n");

            if (translationCount < 6) {
                String chinese = safeTranslate(sense.definition);
                translationCount++;
                if (!chinese.isEmpty()) out.append("   中文：").append(chinese).append("\n");
            }
            out.append("   英文：").append(sense.definition).append("\n");

            if (!sense.example.isEmpty()) {
                out.append("   例句：").append(sense.example).append("\n");
                if (translationCount < 7) {
                    String exampleChinese = safeTranslate(sense.example);
                    translationCount++;
                    if (!exampleChinese.isEmpty()) out.append("   翻译：").append(exampleChinese).append("\n");
                }
            }
            out.append("\n");
        }

        out.append("【学习提示】\n不同语境下，一个单词可能有不同意思。建议结合词性、英文释义和例句一起记忆。\n\n");
        out.append("数据来源：Free Dictionary API；中文翻译：MyMemory。");
        return out.toString();
    }

    private String safeTranslate(String text) {
        try {
            if (text == null || text.trim().isEmpty()) return "";
            String encoded = URLEncoder.encode(text, "UTF-8");
            String response = httpGet("https://api.mymemory.translated.net/get?q="
                    + encoded + "&langpair=en|zh-CN");
            JSONObject json = new JSONObject(response);
            JSONObject data = json.optJSONObject("responseData");
            if (data == null) return "";
            return data.optString("translatedText", "").trim()
                    .replace("&#39;", "'").replace("&quot;", "\"")
                    .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">");
        } catch (Exception e) {
            return "";
        }
    }

    private String httpGet(String address) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(address).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(12000);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "MiniWord-Android/1.0");

            int code = conn.getResponseCode();
            InputStream stream = (code >= 200 && code < 300)
                    ? conn.getInputStream() : conn.getErrorStream();
            if (stream == null) throw new Exception("网络服务暂时不可用（" + code + "）");

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = stream.read(buffer)) != -1) output.write(buffer, 0, count);
            stream.close();

            if (code < 200 || code >= 300) {
                if (code == 404) throw new Exception("没有找到这个单词，请检查拼写");
                throw new Exception("词典服务暂时不可用（" + code + "）");
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String posChinese(String pos) {
        switch (pos.toLowerCase()) {
            case "noun": return "名词";
            case "verb": return "动词";
            case "adjective": return "形容词";
            case "adverb": return "副词";
            case "pronoun": return "代词";
            case "preposition": return "介词";
            case "conjunction": return "连词";
            case "interjection":
            case "exclamation": return "感叹词";
            case "determiner": return "限定词";
            default: return "其他词性";
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
