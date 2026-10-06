package com.ember.demo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    static final String CAT_ALL = "အားလုံး";
    static final String CAT_VIDEO = "ဗီဒီယို";
    static final String CAT_AUDIO = "အသံ";
    static final String CAT_IMAGE = "ပုံ";
    static final String CAT_TEXT = "စာသား";
    static final String CAT_CODE = "ကုဒ်";
    static final String CAT_SEARCH = "ရှာဖွေရေး";
    static final String CAT_MEETING = "အစည်းအဝေး";
    static final String CAT_BIZ = "လုပ်ငန်း";

    static class Tool {
        final String name;
        final String desc;
        final String url;
        final String cat;
        Tool(String name, String desc, String url, String cat) {
            this.name = name;
            this.desc = desc;
            this.url = url;
            this.cat = cat;
        }
    }

    static final Tool[] TOOLS = new Tool[] {
        new Tool("Explee", "B2B Lead တွေကို ရှာဖွေပေးပြီး Cold Email တွေကို အလိုအလျောက် ပို့ပေးတဲ့ Tool။", "https://explee.com", CAT_BIZ),
        new Tool("NoteGPT", "စာရွက်စာတမ်းရှည်တွေကို အနှစ်ချုပ်ပေးပြီး Podcast အသံဖိုင်အဖြစ် ပြောင်းလဲပေးနိုင်တဲ့ ဝဘ်ဆိုက်။", "https://notegpt.io", CAT_TEXT),
        new Tool("Napkin AI", "သာမန်စာသားတွေကို စနစ်ကျပြီး ကြည့်ကောင်းတဲ့ Business Diagram တွေအဖြစ် ချက်ချင်း ပြောင်းပေးတယ်။", "https://napkin.ai", CAT_BIZ),
        new Tool("Ideogram", "ပုံတွေထဲမှာ စာလုံးဒီဇိုင်းနဲ့ Typography တွေကို အမှားအယွင်းမရှိ တိကျသပ်ရပ်စွာ ထည့်သွင်းဖန်တီးပေးနိုင်တဲ့ Tool။", "https://ideogram.ai", CAT_IMAGE),
        new Tool("Suno", "စာသား Prompt လေးတစ်ခု ပေးရုံနဲ့ ကိုယ်လိုချင်တဲ့ ဂီတအမျိုးအစားအလိုက် သီချင်းတစ်ပုဒ်လုံးကို အသံသွင်းပြီးသားအထိ ထုတ်ပေးတယ်။", "https://suno.com", CAT_AUDIO),
        new Tool("HeyGen", "ကိုယ့်ရဲ့ မျက်နှာနဲ့ အသံကို Clone လုပ်ပြီး ဘာသာစကားမျိုးစုံနဲ့ နှုတ်ခမ်းလှုပ်ရှားမှု သဘာဝကျတဲ့ AI Video တွေ ပြုလုပ်ပေးနိုင်တယ်။", "https://heygen.com", CAT_VIDEO),
        new Tool("Kling AI", "ရုပ်ရှင်အဆင့် ရုပ်ထွက်နဲ့ လှုပ်ရှားမှု သဘာဝကျကျ ထွက်ပေါ်စေတဲ့ ထိပ်တန်း AI Video Generator။", "https://klingai.com", CAT_VIDEO),
        new Tool("ElevenLabs", "ဘယ်သူ့အသံကိုမဆို စိတ်ခံစားမှု၊ အတက်အကျ သဘာဝအတိုင်း Clone လုပ်ပေးနိုင်တဲ့ ထိပ်တန်း အသံဖန်တီးရေး Tool။", "https://elevenlabs.io", CAT_AUDIO),
        new Tool("Gamma", "Prompt ရိုက်ထည့်လိုက်ရုံနဲ့ Professional Presentation Slide တွေ၊ Doc တွေနဲ့ Web Page တွေကို စက္ကန့်ပိုင်းအတွင်း ဖန်တီးပေးတယ်။", "https://gamma.app", CAT_BIZ),
        new Tool("Perplexity", "တကယ့် ခိုင်လုံတဲ့ အရင်းအမြစ် Source တွေနဲ့ Reference တွေကို တွဲဖက်ဖော်ပြပေးတဲ့ AI Search Engine။", "https://perplexity.ai", CAT_SEARCH),
        new Tool("Pika", "သာမန်ဓာတ်ပုံ ငြိမ်ငြိမ်လေးတွေကို လှုပ်ရှားမှုအပြည့်နဲ့ ဗီဒီယိုအဖြစ် အသက်သွင်းပေးတဲ့ Tool။", "https://pika.art", CAT_VIDEO),
        new Tool("Runway", "Filmmaker တွေနဲ့ ဗီဒီယိုဖန်တီးသူတွေအတွက် Cinematic အဆင့် AI Video တွေ ထုတ်လုပ်ပေးတဲ့ စနစ်။", "https://runwayml.com", CAT_VIDEO),
        new Tool("Cursor", "Code ရေးတာ၊ ပြင်ဆင်တာနဲ့ Debug လုပ်တာတွေကို ကိုယ်နဲ့အတူ တွဲလုပ်ပေးတဲ့ AI Code Editor။", "https://cursor.com", CAT_CODE),
        new Tool("v0", "သာမန် စကားပြော Prompt လေးတွေနဲ့တင် Frontend UI Component တွေနဲ့ Web Page တွေကို အလွယ်တကူ တည်ဆောက်ပေးတယ်။", "https://v0.dev", CAT_CODE),
        new Tool("Lovable", "စိတ်ကူး စိတ်သန်းနဲ့ ဒီဇိုင်းတွေကို အမှန်တကယ် အသုံးပြုလို့ရတဲ့ Full-stack Web App အဖြစ် ပြောင်းလဲပေးနိုင်တဲ့ Tool။", "https://lovable.dev", CAT_CODE),
        new Tool("Descript", "ထွက်လာတဲ့ စာသား Transcript ကို စာရိုက်ပြင်သလို ဖြတ်ထုတ်ရုံနဲ့ ဗီဒီယိုနဲ့ အသံဖိုင်ကို တည်းဖြတ်ပေးနိုင်တဲ့ Tool။", "https://descript.com", CAT_VIDEO),
        new Tool("Opus Clip", "ဗီဒီယို အရှည်ကြီးတွေကို Caption စာတန်းထိုးပါတဲ့ Short-form ဗီဒီယို တိုတိုလေးတွေအဖြစ် အလိုအလျောက် ဖြတ်ထုတ်ပေးတယ်။", "https://opus.pro", CAT_VIDEO),
        new Tool("Krea AI", "ကင်းဗတ်စ်ပေါ်မှာ ဆွဲရင်း ချက်ချင်း ပုံထွက်လာစေတဲ့ Real-time AI Image Generator။", "https://krea.ai", CAT_IMAGE),
        new Tool("Magnific", "ပုံဝါးတာတွေ၊ Resolution နိမ့်တာတွေကို အသေးစိတ် Texture ပေါင်းထည့်ပြီး အရည်အသွေး အဆမတန် မြှင့်တင်ပေးတဲ့ Tool။", "https://magnific.ai", CAT_IMAGE),
        new Tool("Viggle", "ကိုယ်ဖန်တီးထားတဲ့ Character ကို ဘယ်ဗီဒီယိုထဲက လှုပ်ရှားမှုနဲ့မဆို သဘာဝကျကျ အစားထိုး ကပြလှုပ်ရှားစေနိုင်တယ်။", "https://viggle.ai", CAT_VIDEO),
        new Tool("tl;dv", "Zoom, Teams နဲ့ Google Meet အစည်းအဝေးတွေကို မှတ်တမ်းတင်ပေးပြီး အရေးကြီးတဲ့ အချက်တွေကို အနှစ်ချုပ်ပေးတယ်။", "https://tldv.io", CAT_MEETING),
        new Tool("Fireflies", "Meeting တွေထဲ ဝင်ရောက်ပြီး ဆွေးနွေးချက် မှတ်စုတွေ၊ Action Item တွေကို အလိုအလျောက် ရေးမှတ်ပေးတဲ့ AI Assistant။", "https://fireflies.ai", CAT_MEETING),
        new Tool("Castmagic", "အသံဖိုင်တွေကို Newsletter၊ Blog Post နဲ့ Social Media Content တွေအဖြစ် ချက်ချင်း ပြောင်းလဲပေးတဲ့ Tool။", "https://castmagic.io", CAT_AUDIO),
        new Tool("Replit", "Browser ပေါ်ကနေတင် Code ရေး၊ စမ်းသပ်ပြီး တိုက်ရိုက် Deploy လုပ်နိုင်တဲ့ Cloud Platform။", "https://replit.com", CAT_CODE),
        new Tool("Leonardo AI", "Creator တွေအတွက် ပုံစံမျိုးစုံနဲ့ အရည်အသွေးမြင့် ပုံတွေကို လျင်မြန်စွာ ဖန်တီးပေးနိုင်တဲ့ Tool။", "https://leonardo.ai", CAT_IMAGE),
        new Tool("Synthesia", "ကင်မရာ၊ မိုက်ခရိုဖုန်း မလိုဘဲ AI Avatar တွေနဲ့ လုပ်ငန်းသုံး ဗီဒီယို Presentation တွေကို အလွယ်တကူ ဖန်တီးနိုင်တယ်။", "https://synthesia.io", CAT_VIDEO),
        new Tool("Fliki", "စာသားတွေနဲ့ ဆောင်းပါးတွေကို သဘာဝကျတဲ့ အသံနောက်ခံပါတဲ့ ဗီဒီယိုတွေအဖြစ် ပြောင်းလဲပေးတဲ့ Tool။", "https://fliki.ai", CAT_VIDEO),
        new Tool("Photoroom", "ကုန်ပစ္စည်း ဓာတ်ပုံတွေရဲ့ နောက်ခံကို ဖျက်ပေးပြီး Studio အဆင့် အလင်းအမှောင်တွေ အလိုအလျောက် ထည့်သွင်းပေးတယ်။", "https://photoroom.com", CAT_IMAGE),
        new Tool("Invideo AI", "စာသား Prompt လေး ပေးလိုက်ရုံနဲ့ Script၊ အသံနဲ့ တည်းဖြတ်မှု အပြည့်အစုံပါတဲ့ ဗီဒီယို အချောထည် ထုတ်ပေးတယ်။", "https://invideo.io", CAT_VIDEO),
        new Tool("Consensus", "သိပ္ပံနည်းကျ သုတေသန စာတမ်းတွေက ဘာပြောထားသလဲဆိုတာကို တိကျစွာ ရှာဖွေပေးတဲ့ AI Search Tool။", "https://consensus.app", CAT_SEARCH),
        new Tool("SciSpace", "ရှုပ်ထွေးတဲ့ Research Paper တွေ၊ သုတေသနဇယားတွေနဲ့ တွက်ချက်မှုတွေကို နားလည်လွယ်အောင် ရှင်းပြပေးတယ်။", "https://scispace.com", CAT_SEARCH),
        new Tool("Tome", "စီးပွားရေး Pitch Deck တွေနဲ့ Storytelling Presentation တွေကို ပုံနဲ့တကွ စနစ်တကျ ရေးဖွဲ့တည်ဆောက်ပေးတယ်။", "https://tome.app", CAT_BIZ),
        new Tool("Beautiful AI", "Slide ဒီဇိုင်းနဲ့ Layout တွေကို အချိုးအစား ညီညွတ်အောင် အလိုအလျောက် ချိန်ညှိပေးတဲ့ Smart Presentation Tool။", "https://beautiful.ai", CAT_BIZ),
        new Tool("Meshy", "စာသား ဒါမှမဟုတ် ဓာတ်ပုံကနေ 3D Model နဲ့ Texture တွေကို ချက်ချင်း ဖန်တီးပေးနိုင်တဲ့ Tool။", "https://meshy.ai", CAT_IMAGE),
        new Tool("Vizcom", "လက်နဲ့ ရေးဆွဲထားတဲ့ ပုံကြမ်း (Sketch) တွေကို လက်တွေ့ကျတဲ့ 3D Render ရုပ်ထွက်အဖြစ် Real-time ပြောင်းပေးတယ်။", "https://vizcom.ai", CAT_IMAGE),
    };

    private final List<Tool> filtered = new ArrayList<Tool>();
    private ToolAdapter adapter;
    private String query = "";
    private String cat = CAT_ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(12);
        root.setPadding(pad, pad, pad, pad);

        TextView header = new TextView(this);
        header.setText("AI Tools Hub — ၃၅ ခု");
        header.setTextSize(22);
        int hpad = dp(4);
        header.setPadding(hpad, hpad, hpad, dp(8));

        final EditText searchBox = new EditText(this);
        searchBox.setHint("ရှာရန်…");
        searchBox.setSingleLine(true);

        final Spinner catSpinner = new Spinner(this);
        final String[] cats = new String[] {
            CAT_ALL, CAT_VIDEO, CAT_AUDIO, CAT_IMAGE, CAT_TEXT,
            CAT_CODE, CAT_SEARCH, CAT_MEETING, CAT_BIZ
        };
        ArrayAdapter<String> catAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, cats);
        catAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        catSpinner.setAdapter(catAdapter);

        final ListView listView = new ListView(this);
        adapter = new ToolAdapter();
        listView.setAdapter(adapter);

        searchBox.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            public void afterTextChanged(Editable s) {
                query = s.toString().trim().toLowerCase();
                applyFilter();
            }
        });

        catSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                cat = cats[position];
                applyFilter();
            }
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Tool t = filtered.get(position);
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(t.url));
                startActivity(i);
            }
        });

        root.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(searchBox, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(catSpinner, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        applyFilter();
        setContentView(root);
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(v * d);
    }

    private void applyFilter() {
        filtered.clear();
        for (int i = 0; i < TOOLS.length; i++) {
            Tool t = TOOLS[i];
            boolean okCat = cat.equals(CAT_ALL) || t.cat.equals(cat);
            boolean okQuery = query.length() == 0
                    || t.name.toLowerCase().contains(query)
                    || t.desc.toLowerCase().contains(query);
            if (okCat && okQuery) {
                filtered.add(t);
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private class ToolAdapter extends BaseAdapter {
        public int getCount() {
            return filtered.size();
        }
        public Object getItem(int position) {
            return filtered.get(position);
        }
        public long getItemId(int position) {
            return position;
        }
        public View getView(int position, View convertView, ViewGroup parent) {
            Tool t = filtered.get(position);
            LinearLayout row = new LinearLayout(MainActivity.this);
            row.setOrientation(LinearLayout.VERTICAL);
            int p = dp(10);
            row.setPadding(p, p, p, p);

            TextView nameView = new TextView(MainActivity.this);
            nameView.setText(t.name);
            nameView.setTextSize(18);

            TextView descView = new TextView(MainActivity.this);
            descView.setText(t.desc);
            descView.setTextSize(14);

            row.addView(nameView);
            row.addView(descView);
            return row;
        }
    }
}
