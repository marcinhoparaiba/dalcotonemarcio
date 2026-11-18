package br.com.dalcotone.crm

import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/** Portable JSON backup. No network access or external account required. */
object CrmBackup {
 private fun nullableString(o:JSONObject,k:String):String?=if(o.isNull(k))null else o.getString(k)
 private fun nullableDouble(o:JSONObject,k:String):Double?=if(o.isNull(k))null else o.getDouble(k)
 private fun nullableLong(o:JSONObject,k:String):Long?=if(o.isNull(k))null else o.getLong(k)
 suspend fun export(db:CrmDatabase):String {
  val dao=db.dao()
  val root=JSONObject().put("format","dalcotone-crm").put("version",1).put("createdAt",System.currentTimeMillis())
  fun array(items:List<JSONObject>):JSONArray=JSONArray().apply{items.forEach{put(it)}}
  root.put("salons",array(dao.salons().first().map{JSONObject().apply{
   put("id",it.id);put("name",it.name);put("city",it.city);put("address",it.address);put("phone",it.phone)
   put("status",it.status);put("notes",it.notes);put("placeId",it.placeId);put("latitude",it.latitude);put("longitude",it.longitude)
   put("lastContactAt",it.lastContactAt);put("doNotContact",it.doNotContact)
  }}))
  root.put("visits",array(dao.visits().first().map{JSONObject().apply{
   put("id",it.id);put("salonId",it.salonId);put("visitedAt",it.visitedAt);put("outcome",it.outcome);put("notes",it.notes)
  }}))
  root.put("appointments",array(dao.appointments().first().map{JSONObject().apply{
   put("id",it.id);put("salonId",it.salonId);put("startsAt",it.startsAt);put("notes",it.notes);put("remindMinutes",it.remindMinutes);put("completed",it.completed)
  }}))
  root.put("sales",array(dao.sales().first().map{JSONObject().apply{
   put("id",it.id);put("salonId",it.salonId);put("soldAt",it.soldAt);put("dueAt",it.dueAt);put("totalCents",it.totalCents)
  }}))
  root.put("items",array(dao.items().first().map{JSONObject().apply{
   put("id",it.id);put("saleId",it.saleId);put("product",it.product);put("quantity",it.quantity);put("unitPriceCents",it.unitPriceCents)
  }}))
  root.put("payments",array(dao.payments().first().map{JSONObject().apply{
   put("id",it.id);put("saleId",it.saleId);put("amountCents",it.amountCents);put("paidAt",it.paidAt);put("method",it.method);put("receiptUri",it.receiptUri)
  }}))
  return root.toString(2)
 }
 suspend fun restore(db:CrmDatabase,json:String):Int {
  require(json.length<=25_000_000){"Arquivo muito grande"}
  val root=JSONObject(json)
  require(root.optString("format")=="dalcotone-crm" && root.optInt("version")==1){"Arquivo não é um backup compatível do Dal Cotone"}
  fun objects(key:String):List<JSONObject>{
   val arr=root.getJSONArray(key)
   require(arr.length()<=100_000){"Backup excede limite de registros"}
   return (0 until arr.length()).map{arr.getJSONObject(it)}
  }
  val salons=objects("salons").map{Salon(
   id=it.getString("id"),name=it.getString("name"),city=it.optString("city"),address=it.optString("address"),
   phone=it.optString("phone"),status=it.optString("status","nao_visitado"),notes=it.optString("notes"),
   placeId=nullableString(it,"placeId"),latitude=nullableDouble(it,"latitude"),longitude=nullableDouble(it,"longitude"),
   lastContactAt=nullableLong(it,"lastContactAt"),doNotContact=it.optBoolean("doNotContact")
  )}
  val visits=objects("visits").map{Visit(id=it.getString("id"),salonId=it.getString("salonId"),visitedAt=it.getLong("visitedAt"),outcome=it.getString("outcome"),notes=it.optString("notes"))}
  val appointments=objects("appointments").map{Appointment(id=it.getString("id"),salonId=it.getString("salonId"),startsAt=it.getLong("startsAt"),notes=it.optString("notes"),remindMinutes=it.optInt("remindMinutes",30),completed=it.optBoolean("completed"))}
  val sales=objects("sales").map{Sale(id=it.getString("id"),salonId=it.getString("salonId"),soldAt=it.getLong("soldAt"),dueAt=nullableLong(it,"dueAt"),totalCents=it.getLong("totalCents"))}
  val items=objects("items").map{SaleItem(id=it.getString("id"),saleId=it.getString("saleId"),product=it.getString("product"),quantity=it.getInt("quantity"),unitPriceCents=it.getLong("unitPriceCents"))}
  val payments=objects("payments").map{Payment(id=it.getString("id"),saleId=it.getString("saleId"),amountCents=it.getLong("amountCents"),paidAt=it.getLong("paidAt"),method=it.getString("method"),receiptUri=nullableString(it,"receiptUri"))}
  val salonIds=salons.map{it.id}.toSet()+db.dao().salons().first().map{it.id}
  require(visits.all{it.salonId in salonIds} && appointments.all{it.salonId in salonIds} && sales.all{it.salonId in salonIds}){"Backup contém referências inválidas"}
  val saleIds=sales.map{it.id}.toSet()+db.dao().sales().first().map{it.id}
  require(items.all{it.saleId in saleIds} && payments.all{it.saleId in saleIds}){"Backup contém vendas inválidas"}
  db.withTransaction {
   val dao=db.dao()
   salons.forEach{dao.saveSalon(it)}
   visits.forEach{dao.restoreVisit(it)}
   appointments.forEach{dao.restoreAppointment(it)}
   sales.forEach{dao.restoreSale(it)}
   items.forEach{dao.restoreItem(it)}
   payments.forEach{dao.restorePayment(it)}
  }
  return salons.size
 }
}
