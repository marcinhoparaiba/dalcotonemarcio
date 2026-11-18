package br.com.dalcotone.crm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Recreate local reminders after device restart or application update. */
class BootReceiver:BroadcastReceiver(){
 override fun onReceive(context:Context,intent:Intent){
  if(intent.action !in listOf(Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_MY_PACKAGE_REPLACED,Intent.ACTION_TIME_CHANGED,Intent.ACTION_TIMEZONE_CHANGED))return
  val pending=goAsync()
  CoroutineScope(SupervisorJob()+Dispatchers.IO).launch{
   try{
    val dao=CrmDatabase.get(context).dao()
    val names=dao.salons().first().associateBy{it.id}
    dao.appointments().first().filter{!it.completed}.forEach{a->
     val salon=names[a.salonId]
     if(salon!=null)Reminders.schedule(context,a,salon.name)
    }
   }finally{pending.finish()}
  }
 }
}
