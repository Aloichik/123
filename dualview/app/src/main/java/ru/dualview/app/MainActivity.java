package ru.dualview.app;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;
import java.util.Locale;

public class MainActivity extends Activity {
    private final Slot[] slots = { new Slot(), new Slot() };
    private boolean swapped = false;
    private static final int BG=0xff10111e, CARD=0xff202438, FG=0xfff6f3ff, PURPLE=0xffab91fc;
    private static class Slot {
        int index; EditText url; FrameLayout frame; VideoView video; WebView web; MediaPlayer player;
        SeekBar gain; TextView status; Button pause; boolean nativeVideo=false; float volume=0.85f;
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView text(String t,int size,int color){
        TextView x=new TextView(this);x.setText(t);x.setTextSize(size);x.setTextColor(color);x.setGravity(Gravity.CENTER_VERTICAL);return x;
    }
    private GradientDrawable bg(int color){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(12));return d;
    }
    private Button btn(String title){
        Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextColor(FG);b.setTextSize(11);b.setBackground(bg(0xff373a53));b.setMinimumHeight(0);b.setMinHeight(0);return b;
    }
    private void addRow(LinearLayout parent, View view,int width,int height){
        parent.addView(view,new LinearLayout.LayoutParams(width,height<0?height:dp(height)));
    }
    @Override public void onCreate(Bundle s){
        super.onCreate(s);getWindow().addFlags(128);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(8),dp(6),dp(8),dp(6));root.setBackgroundColor(BG);setContentView(root);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(top,new LinearLayout.LayoutParams(-1,dp(44)));
        TextView heading=text("DualView",23,FG);
        top.addView(heading,new LinearLayout.LayoutParams(0,-1,1f));
        Button swap=btn("⇄ Поменять уши");top.addView(swap,new LinearLayout.LayoutParams(-2,dp(38)));
        swap.setOnClickListener(v->{swapped=!swapped;for(Slot slot:slots)applyPan(slot);Toast.makeText(this,"Каналы видеофайлов поменяны",Toast.LENGTH_SHORT).show();});
        addRow(root,text("2 источника одновременно • L/R для видеофайлов",12,0xffb9b9ce),-1,22);
        for(int i=0;i<2;i++){Slot slot=new Slot();slot.index=i;slots[i]=slot;createSlot(root,slot);}
        addRow(root,text("Для YouTube и Twitch разделение звука недоступно",11,0xffffb89d),-1,22);
    }
    private void createSlot(LinearLayout root,Slot slot){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(8),dp(6),dp(8),dp(7));card.setBackground(bg(CARD));
        LinearLayout.LayoutParams cardLp=new LinearLayout.LayoutParams(-1,0,1f);
        cardLp.setMargins(0,dp(3),0,dp(3));root.addView(card,cardLp);
        addRow(card,text("ВИДЕО "+(slot.index+1)+"     •     "+(slot.index==0?"ЛЕВОЕ":"ПРАВОЕ")+" УХО",13,PURPLE),-1,25);
        slot.url=new EditText(this);slot.url.setSingleLine();slot.url.setTextSize(12);
        slot.url.setTextColor(FG);slot.url.setHintTextColor(0xff999bb2);slot.url.setHint("Ссылка YouTube, Twitch или .mp4");
        slot.url.setPadding(dp(9),0,dp(9),0);slot.url.setBackground(bg(0xff30354a));
        addRow(card,slot.url,-1,36);
        LinearLayout controls=new LinearLayout(this);card.addView(controls,new LinearLayout.LayoutParams(-1,dp(38)));
        Button open=btn("Открыть ссылку"), file=btn("Видео из телефона");
        controls.addView(open,new LinearLayout.LayoutParams(0,dp(34),1f));
        controls.addView(file,new LinearLayout.LayoutParams(0,dp(34),1f));
        open.setOnClickListener(v->openLink(slot));
        file.setOnClickListener(v->{
            Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("video/*");
            startActivityForResult(intent,100+slot.index);
        });
        slot.frame=new FrameLayout(this);slot.frame.setBackgroundColor(Color.BLACK);
        LinearLayout.LayoutParams frameLp=new LinearLayout.LayoutParams(-1,0,1f);
        frameLp.setMargins(0,dp(5),0,dp(3));card.addView(slot.frame,frameLp);
        slot.video=new VideoView(this);
        slot.video.setVisibility(View.GONE);
        slot.video.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE);
        slot.frame.addView(slot.video,new FrameLayout.LayoutParams(-1,-1));
        slot.web=new WebView(this);
        slot.web.setVisibility(View.GONE);slot.web.setBackgroundColor(Color.BLACK);
        slot.web.getSettings().setJavaScriptEnabled(true);
        slot.web.getSettings().setDomStorageEnabled(true);
        slot.web.getSettings().setMediaPlaybackRequiresUserGesture(true);
        slot.web.getSettings().setAllowFileAccess(false);
        slot.web.setWebChromeClient(new WebChromeClient());
        slot.web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
                String scheme=request.getUrl().getScheme();
                return !"http".equalsIgnoreCase(scheme)&&!"https".equalsIgnoreCase(scheme);
            }
        });
        slot.frame.addView(slot.web,new FrameLayout.LayoutParams(-1,-1));
        slot.status=text("Открой ссылку или выбери видео",11,0xffb8bbcd);
        addRow(card,slot.status,-1,22);
        LinearLayout bottom=new LinearLayout(this);card.addView(bottom,new LinearLayout.LayoutParams(-1,dp(34)));
        slot.pause=btn("▶ / ❚❚");slot.pause.setEnabled(false);
        bottom.addView(slot.pause,new LinearLayout.LayoutParams(dp(82),-1));
        slot.pause.setOnClickListener(v->{
            if(!slot.nativeVideo)return;
            if(slot.video.isPlaying())slot.video.pause();else slot.video.start();
        });
        slot.gain=new SeekBar(this);slot.gain.setMax(100);slot.gain.setProgress(85);slot.gain.setEnabled(false);
        bottom.addView(slot.gain,new LinearLayout.LayoutParams(0,dp(34),1f));
        slot.gain.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int p,boolean user){slot.volume=p/100f;applyPan(slot);}
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });
    }
    private void openLink(Slot slot){
        String value=slot.url.getText().toString().trim();
        if(!value.startsWith("https://")&&!value.startsWith("http://")){
            Toast.makeText(this,"Вставь ссылку https://...",Toast.LENGTH_SHORT).show();return;
        }
        Uri uri=Uri.parse(value);
        if(uri.getHost()==null)return;
        String path=uri.getPath()==null?"":uri.getPath().toLowerCase(Locale.ROOT);
        if(path.endsWith(".mp4")||path.endsWith(".3gp")||path.endsWith(".m3u8"))showVideo(slot,uri);
        else{
            slot.nativeVideo=false;slot.player=null;slot.video.stopPlayback();
            slot.video.setVisibility(View.GONE);slot.web.setVisibility(View.VISIBLE);
            slot.pause.setEnabled(false);slot.gain.setEnabled(false);
            slot.status.setText("Сайт: звук L/R не разделяется");
            slot.web.loadUrl(value);
        }
    }
    private void showVideo(Slot slot,Uri uri){
        slot.nativeVideo=true;
        slot.web.loadUrl("about:blank");slot.web.setVisibility(View.GONE);
        slot.video.setVisibility(View.VISIBLE);
        slot.gain.setEnabled(true);slot.pause.setEnabled(false);slot.player=null;
        slot.status.setText("Загрузка видео...");
        slot.video.stopPlayback();
        slot.video.setOnPreparedListener(mp->{
            slot.player=mp;applyPan(slot);slot.pause.setEnabled(true);
            slot.status.setText("Видеофайл: звук только в одном ухе");
            slot.video.start();
        });
        slot.video.setOnErrorListener((mp,what,extra)->{
            slot.status.setText("Ошибка видео. Попробуй MP4.");return true;
        });
        slot.video.setVideoURI(uri);
    }
    private void applyPan(Slot slot){
        if(slot==null||!slot.nativeVideo||slot.player==null)return;
        boolean left=(slot.index==0)!=swapped;
        try{slot.player.setVolume(left?slot.volume:0f,left?0f:slot.volume);}catch(Exception ignored){}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(result==RESULT_OK&&data!=null&&request>=100&&request<=101){
            Uri uri=data.getData();if(uri!=null)showVideo(slots[request-100],uri);
        }
    }
    @Override protected void onPause(){
        super.onPause();
        for(Slot s:slots)if(s!=null&&s.nativeVideo&&s.video.isPlaying())s.video.pause();
    }
    @Override protected void onDestroy(){
        for(Slot s:slots)if(s!=null){
            if(s.video!=null)s.video.stopPlayback();
            if(s.web!=null){s.web.stopLoading();s.web.destroy();}
        }
        super.onDestroy();
    }
}
