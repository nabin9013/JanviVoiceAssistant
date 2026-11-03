package com.jaanvi.assistant;
import android.Manifest;import android.app.*;import android.content.*;import android.content.pm.PackageManager;import android.graphics.drawable.GradientDrawable;import android.os.*;import android.speech.*;import android.speech.tts.TextToSpeech;import android.view.*;import android.widget.*;import org.json.*;import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.util.concurrent.*;public class MainActivity extends Activity{
 int bg=0xff100824,panel=0xff231649,purple=0xffa855f7;SharedPreferences pref;LinearLayout msgs;ScrollView scroll;EditText input;TextView state,toggle;boolean on;TextToSpeech tts;SpeechRecognizer sr;ExecutorService worker=Executors.newSingleThreadExecutor();JSONArray history=new JSONArray();
 public void onCreate(Bundle b){super.onCreate(b);pref=getSharedPreferences("janvi",0);on=pref.getBoolean("on",true);tts=new TextToSpeech(this,x->{if(x==0){if(tts.setLanguage(new Locale("hi","IN"))<0)tts.setLanguage(Locale.US);}});ui();restoreHistory();if(history.length()==0)message(false,"नमस्ते! मैं Janvi हूँ। हिंदी या English में बात करें।");}
 GradientDrawable shape(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(r);return d;}
 TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
 TextView button(String s){TextView t=label(s,16,-1);t.setGravity(17);t.setPadding(14,10,14,10);t.setBackground(shape(purple,28));return t;} void ui(){
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(1);
    root.setPadding(22,18,22,16);
    root.setBackgroundColor(0xff090515);
    setContentView(root);

    LinearLayout head = new LinearLayout(this);
    head.setGravity(Gravity.CENTER_VERTICAL);

    TextView logo = label("✦",30,0xffc026ff);
    head.addView(logo);

    TextView title = label(" JANVI",25,-1);
    title.setTypeface(null,1);
    head.addView(title,new LinearLayout.LayoutParams(0,-2,1));

    state = label(on ? "● ONLINE" : "○ OFFLINE",12,
            on ? 0xff86efac : 0xfff87171);
    head.addView(state);

    TextView settings = button("⚙");
    LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(50,48);
    sp.leftMargin = 10;
    head.addView(settings,sp);
    settings.setOnClickListener(v -> settings());
    root.addView(head);

    TextView greeting = label("Hello, I'm Janvi",27,-1);
    greeting.setTypeface(null,1);
    greeting.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1,-2);
    gp.topMargin = 30;
    root.addView(greeting,gp);

    TextView subtitle = label("Your personal AI voice assistant",14,0xffb8a9d6);
    subtitle.setGravity(Gravity.CENTER);
    root.addView(subtitle);

    LinearLayout center = new LinearLayout(this);
    center.setGravity(Gravity.CENTER);
    center.setOrientation(1);

    TextView mic = label("🎙",54,-1);
    mic.setGravity(Gravity.CENTER);
    mic.setBackground(shape(0xff32145c,120));
    mic.setElevation(18);
    LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(150,150);
    mp.topMargin = 36;
    mp.bottomMargin = 18;
    center.addView(mic,mp);
    mic.setOnClickListener(v -> listen());

    TextView hint = label("Tap the microphone and speak",14,0xffc4b5fd);
    hint.setGravity(Gravity.CENTER);
    center.addView(hint);

    toggle = button(on ? "ASSISTANT ON  ⏻" : "ASSISTANT OFF  ⏻");
    LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1,52);
    tp.topMargin = 24;
    center.addView(toggle,tp);

    toggle.setOnClickListener(v -> {
        on = !on;
        pref.edit().putBoolean("on",on).apply();
        toggle.setText(on ? "ASSISTANT ON  ⏻" : "ASSISTANT OFF  ⏻");
        state.setText(on ? "● ONLINE" : "○ OFFLINE");
        state.setTextColor(on ? 0xff86efac : 0xfff87171);
    });

    root.addView(center,new LinearLayout.LayoutParams(-1,0,1));

    TextView chatTitle = label("RECENT CONVERSATION",12,0xffa78bfa);
    chatTitle.setPadding(0,8,0,8);
    root.addView(chatTitle);

    scroll = new ScrollView(this);
    msgs = new LinearLayout(this);
    msgs.setOrientation(1);
    msgs.setPadding(0,4,0,8);
    scroll.addView(msgs);
    root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

    LinearLayout bar = new LinearLayout(this);
    bar.setGravity(Gravity.CENTER_VERTICAL);

    input = new EditText(this);
    input.setTextColor(-1);
    input.setHintTextColor(0xffb8a9d6);
    input.setHint("Message Janvi...");
    input.setMaxLines(3);
    input.setPadding(16,8,16,8);
    input.setBackground(shape(0xff231649,28));
    bar.addView(input,new LinearLayout.LayoutParams(0,-2,1));

    TextView sendButton = button("➤");
    LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(54,52);
    bp.leftMargin = 8;
    bar.addView(sendButton,bp);
    sendButton.setOnClickListener(v -> send());

    root.addView(bar);
}

 void message(boolean user,String s){TextView t=label((user?"You":"Janvi")+"\n"+s,16,-1);t.setPadding(16,12,16,12);t.setBackground(shape(user?0xff4c1d95:panel,22));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=10;msgs.addView(t,p);scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));}

 void saveHistory(){
     pref.edit().putString("chat_history", history.toString()).apply();
 }

 void restoreHistory(){
     try {
         String saved = pref.getString("chat_history", "");
         if(saved.isEmpty()) return;
         history = new JSONArray(saved);

         for(int i=0; i<history.length(); i++){
             JSONObject item = history.getJSONObject(i);
             String role = item.optString("role", "user");
             JSONArray parts = item.optJSONArray("parts");
             if(parts == null) continue;

             StringBuilder text = new StringBuilder();
             for(int j=0; j<parts.length(); j++){
                 text.append(parts.getJSONObject(j).optString("text", ""));
             }

             if(text.length() > 0){
                 message(role.equals("user"), text.toString());
             }
         }
     } catch(Exception e) {
         history = new JSONArray();
     }
 }

 void send(){String q=input.getText().toString().trim();if(q.isEmpty())return;if(!on){toast("Turn Janvi on first.");return;}input.setText("");message(true,q);try{history.put(new JSONObject().put("role","user").put("parts",new JSONArray().put(new JSONObject().put("text",q))));}catch(Exception e){}saveHistory();TextView wait=label("Janvi is thinking…",14,0xffc4b5fd);msgs.addView(wait);
 worker.execute(()->{String a=ask();runOnUiThread(()->{msgs.removeView(wait);message(false,a);try{history.put(new JSONObject().put("role","model").put("parts",new JSONArray().put(new JSONObject().put("text",a))));}catch(Exception e){}saveHistory();if(pref.getBoolean("speak",true))tts.speak(a,TextToSpeech.QUEUE_FLUSH,null,"janvi");});});}
 String ask(){String key=pref.getString("key","");if(key.isEmpty())return "Settings में Gemini API key जोड़ें।";HttpURLConnection c=null;try{String model=pref.getString("model","gemini-2.5-flash");URL u=new URL("https://generativelanguage.googleapis.com/v1beta/models/"+model+":generateContent?key="+URLEncoder.encode(key,"UTF-8"));c=(HttpURLConnection)u.openConnection();c.setRequestMethod("POST");c.setConnectTimeout(10000);c.setReadTimeout(20000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");JSONObject body=new JSONObject().put("contents",history).put("generationConfig",new JSONObject().put("maxOutputTokens",pref.getInt("tokens",512)).put("temperature",0.7));try(OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));}int code=c.getResponseCode();InputStream in=code<400?c.getInputStream():c.getErrorStream();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);JSONObject res=new JSONObject(out.toString("UTF-8"));if(code>=400)return "API error: "+res.optJSONObject("error").optString("message","request failed");JSONArray parts=res.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts");StringBuilder s=new StringBuilder();for(int i=0;i<parts.length();i++)s.append(parts.getJSONObject(i).optString("text",""));return s.length()==0?"No response.":s.toString();}catch(Exception e){return "Connection error: "+e.getMessage();}finally{if(c!=null)c.disconnect();}}
 void listen(){
    try {
        if (sr == null) {
            sr = SpeechRecognizer.createSpeechRecognizer(this);
        }

        sr.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) {
                toast("Speech error: " + error);
            }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    input.setText(matches.get(0));
                    send();
                }
            }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        sr.startListening(intent);
    } catch (Exception e) {
        toast("Voice error: " + e.getMessage());
    }
}

void settings(){
    SharedPreferences prefs = getSharedPreferences("janvi", MODE_PRIVATE);
    LinearLayout panel = new LinearLayout(this);
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setPadding(40, 24, 40, 8);

    EditText keyField = new EditText(this);
    keyField.setHint("API key");
    keyField.setSingleLine(true);
    keyField.setInputType( android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
    keyField.setText(prefs.getString("key", ""));
    panel.addView(keyField);

    EditText modelField = new EditText(this);
    modelField.setHint("Model");
    modelField.setSingleLine(true);
    modelField.setText(prefs.getString("model", "gemini-2.5-flash"));
    panel.addView(modelField);

    EditText tokenField = new EditText(this);
    tokenField.setHint("Max tokens");
    tokenField.setSingleLine(true);
    tokenField.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    tokenField.setText(String.valueOf(prefs.getInt("tokens", 1000)));
    panel.addView(tokenField);

    CheckBox speakBox = new CheckBox(this);
    speakBox.setText("Speak replies");
    speakBox.setChecked(prefs.getBoolean("speak", true));
    panel.addView(speakBox);

    new AlertDialog.Builder(this)
        .setTitle("Janvi Settings")
        .setView(panel)
        .setNegativeButton("Cancel", null)
        .setPositiveButton("Save", (dialog, which) -> {
            int tokens = 1000;
            try {
                tokens = Integer.parseInt(tokenField.getText().toString().trim());
            } catch (Exception ignored) {}
            prefs.edit()
                .putString("key", keyField.getText().toString().trim())
                .putString("model", modelField.getText().toString().trim())
                .putInt("tokens", Math.max(1, tokens))
                .putBoolean("speak", speakBox.isChecked())
                .apply();
            toast("Settings saved");
        })
        .show();
}

void toast(String text){
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
}

@Override
protected void onDestroy(){
    if (sr != null) {
        sr.destroy();
        sr = null;
    }
    if (tts != null) {
        tts.stop();
        tts.shutdown();
    }
    super.onDestroy();
}
}
