package br.com.dalcotone.crm

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Reminders {
 const val CHANNEL="dalcotone_visits"
 fun channel(context:Context){if(Build.VERSION.SDK_INT>=26){val manager=context.getSystemService(NotificationManager::class.java);manager.createNotificationChannel(NotificationChannel(CHANNEL,"Lembretes de visitas",NotificationManager.IMPORTANCE_HIGH))}}
 fun schedule(context:Context,a:Appointment,name:String):Boolean{
  val at=a.startsAt-a.remindMinutes*60000L
  if(at<=System.currentTimeMillis())return false
  val intent=Intent(context,ReminderReceiver::class.java).putExtra("name",name).putExtra("id",a.id)
  val pending=PendingIntent.getBroadcast(context,a.id.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val alarm=context.getSystemService(AlarmManager::class.java)
  if(Build.VERSION.SDK_INT>=31 && !alarm.canScheduleExactAlarms()){
   // Inexact fallback; Android may deliver later than the requested 30 minutes.
   alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pending)
   return false
  }
  alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pending)
  return true
 }
}
class ReminderReceiver:BroadcastReceiver(){
 override fun onReceive(context:Context,intent:Intent){
  Reminders.channel(context)
  if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
  val name=intent.getStringExtra("name")?:"salão"
  val open=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val notification=NotificationCompat.Builder(context,Reminders.CHANNEL).setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle("Dal Cotone • Visita em 30 minutos").setContentText("Seu compromisso com $name está chegando.").setContentIntent(open).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH).build()
  NotificationManagerCompat.from(context).notify((intent.getStringExtra("id")?:"").hashCode(),notification)
 }
}
