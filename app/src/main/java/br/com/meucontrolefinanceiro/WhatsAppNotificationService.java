package br.com.meucontrolefinanceiro;

import android.app.*;
import android.content.*;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import java.util.Locale;
import java.util.regex.Pattern;

public class WhatsAppNotificationService extends NotificationListenerService {
 static final String PREFS="whatsapp_capture";
 static final String ENABLED="enabled";
 static final String CHANNEL="financial_review";
 private String lastFingerprint="";
 private long lastAt=0L;

 @Override public void onNotificationPosted(StatusBarNotification sbn){
  try {
   if(!getSharedPreferences(PREFS,MODE_PRIVATE).getBoolean(ENABLED,false))return;
   String pkg=sbn.getPackageName();
   if(!"com.whatsapp".equals(pkg)&&!"com.whatsapp.w4b".equals(pkg))return;
   Notification n=sbn.getNotification();
   if(n==null||(n.flags&Notification.FLAG_GROUP_SUMMARY)!=0)return;
   CharSequence body=n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
   if(TextUtils.isEmpty(body))body=n.extras.getCharSequence(Notification.EXTRA_TEXT);
   if(TextUtils.isEmpty(body))return;
   String raw=body.toString().trim();
   if(raw.isEmpty()||raw.length()>4000)return;
   String low=raw.toLowerCase(new Locale("pt","BR"));
   boolean financial=Pattern.compile("(?iu)\\b(recebi|recebido|receita|pagamento|paguei|pagou|pago|gastei|despesa|transferi|pix|depósito|deposito|cobrança|cobranca|fatura|valor)\\b").matcher(low).find();
   boolean money=Pattern.compile("(?i)(?:R\\$\\s*\\d|\\b\\d[\\d.]*,\\d{2}\\b)").matcher(raw).find();
   if(!financial||!money)return;
   CharSequence title=n.extras.getCharSequence(Notification.EXTRA_TITLE);
   String sender=title==null?"":title.toString();
   String fingerprint=pkg+"|"+sender+"|"+raw;
   long now=System.currentTimeMillis();
   if(fingerprint.equals(lastFingerprint)&&now-lastAt<120000)return;
   lastFingerprint=fingerprint;lastAt=now;
   Intent intent=new Intent(this,ShareImportActivity.class);
   intent.setAction("br.com.meucontrolefinanceiro.REVIEW_NOTIFICATION");
   intent.putExtra(Intent.EXTRA_TEXT,raw);
   intent.putExtra("sender",sender);
   intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
   PendingIntent open=PendingIntent.getActivity(this,(int)(now&0x7fffffff),intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
   if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new NotificationChannel(CHANNEL,"Revisão de mensagens financeiras",NotificationManager.IMPORTANCE_DEFAULT));
   Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
   Notification alert=builder.setSmallIcon(R.drawable.app_brand_icon)
    .setContentTitle("Possível movimentação no WhatsApp")
    .setContentText("Toque para revisar. Nada será salvo automaticamente.")
    .setContentIntent(open).setAutoCancel(true).build();
   manager.notify((int)(now&0x7fffffff),alert);
  }catch(Exception ignored){}
 }
}