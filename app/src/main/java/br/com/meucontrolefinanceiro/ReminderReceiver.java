package br.com.meucontrolefinanceiro;
import android.app.*;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.os.*;
public class ReminderReceiver extends android.content.BroadcastReceiver {
 public static void schedule(Context context,int id,long when){
  AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
  Intent intent=new Intent(context,ReminderReceiver.class).setAction("br.com.meucontrolefinanceiro.REMINDER").putExtra("id",id);
  PendingIntent pi=PendingIntent.getBroadcast(context,id,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  if(when>System.currentTimeMillis())am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);
 }
 public static void cancel(Context context,int id){
  AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
  PendingIntent pi=PendingIntent.getBroadcast(context,id,new Intent(context,ReminderReceiver.class).setAction("br.com.meucontrolefinanceiro.REMINDER"),PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);
  if(pi!=null){am.cancel(pi);pi.cancel();}
 }
 @Override public void onReceive(Context context,Intent intent){
  if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){
   try(SQLiteDatabase db=context.openOrCreateDatabase("financas.db",Context.MODE_PRIVATE,null)){
    try(Cursor c=db.rawQuery("SELECT id,quando FROM agenda WHERE quando>?",new String[]{String.valueOf(System.currentTimeMillis())})){
     while(c.moveToNext())schedule(context,c.getInt(0),c.getLong(1));
    }
   }catch(Exception ignored){}
   return;
  }
  int id=intent.getIntExtra("id",-1);if(id<0)return;
  try(SQLiteDatabase db=context.openOrCreateDatabase("financas.db",Context.MODE_PRIVATE,null)){
   try(Cursor c=db.rawQuery("SELECT titulo FROM agenda WHERE id=?",new String[]{String.valueOf(id)})){
    if(!c.moveToFirst())return;
    String title=c.getString(0);
    NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    String channel="agenda_lembretes";
    if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new NotificationChannel(channel,"Lembretes e agenda",NotificationManager.IMPORTANCE_HIGH));
    Intent open=new Intent(context,MainActivity.class);
    PendingIntent tap=PendingIntent.getActivity(context,id,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    Notification notification=new Notification.Builder(context,channel).setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle("Lembrete da agenda").setContentText(title).setStyle(new Notification.BigTextStyle().bigText(title)).setAutoCancel(true).setContentIntent(tap).build();
    manager.notify(id,notification);
   }
  }catch(Exception ignored){}
 }
}