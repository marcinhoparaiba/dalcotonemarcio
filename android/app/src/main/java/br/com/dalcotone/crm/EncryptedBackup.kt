package br.com.dalcotone.crm

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

/**
 * Encrypted portable backup. Password never stored.
 * Version 1 exports records; restore requires explicit confirmation and an empty database.
 */
object EncryptedBackup {
 private const val ITERATIONS=210000
 private fun key(password:CharArray,salt:ByteArray):SecretKeySpec{
  val spec=PBEKeySpec(password,salt,ITERATIONS,256)
  return try{SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,"AES")}finally{spec.clearPassword()}
 }
 private fun encrypt(data:ByteArray,password:CharArray):ByteArray{
  require(password.size>=12){"Use uma senha de pelo menos 12 caracteres"}
  val salt=ByteArray(16).also{SecureRandom().nextBytes(it)}
  val nonce=ByteArray(12).also{SecureRandom().nextBytes(it)}
  val cipher=Cipher.getInstance("AES/GCM/NoPadding")
  cipher.init(Cipher.ENCRYPT_MODE,key(password,salt),GCMParameterSpec(128,nonce))
  return byteArrayOf(68,67,66,49)+salt+nonce+cipher.doFinal(data)
 }
 private fun decrypt(data:ByteArray,password:CharArray):ByteArray{
  require(data.size>32 && data.copyOfRange(0,4).contentEquals(byteArrayOf(68,67,66,49))){"Backup inválido"}
  val cipher=Cipher.getInstance("AES/GCM/NoPadding")
  cipher.init(Cipher.DECRYPT_MODE,key(password,data.copyOfRange(4,20)),GCMParameterSpec(128,data.copyOfRange(20,32)))
  return cipher.doFinal(data.copyOfRange(32,data.size))
 }
 suspend fun export(context:Context,uri:Uri,password:CharArray){
  val dao=CrmDatabase.get(context).dao()
  val root=JSONObject().put("version",1)
  root.put("salons",JSONArray().also{a->dao.salons().first().forEach{s->a.put(JSONObject().put("id",s.id).put("name",s.name).put("city",s.city).put("address",s.address).put("phone",s.phone).put("status",s.status).put("notes",s.notes).put("placeId",s.placeId).put("latitude",s.latitude).put("longitude",s.longitude).put("lastContactAt",s.lastContactAt).put("doNotContact",s.doNotContact))}})
  root.put("visits",JSONArray().also{a->dao.visits().first().forEach{v->a.put(JSONObject().put("id",v.id).put("salonId",v.salonId).put("visitedAt",v.visitedAt).put("outcome",v.outcome).put("notes",v.notes))}})
  root.put("appointments",JSONArray().also{a->dao.appointments().first().forEach{v->a.put(JSONObject().put("id",v.id).put("salonId",v.salonId).put("startsAt",v.startsAt).put("notes",v.notes).put("remindMinutes",v.remindMinutes).put("completed",v.completed))}})
  root.put("sales",JSONArray().also{a->dao.sales().first().forEach{v->a.put(JSONObject().put("id",v.id).put("salonId",v.salonId).put("soldAt",v.soldAt).put("dueAt",v.dueAt).put("totalCents",v.totalCents))}})
  root.put("items",JSONArray().also{a->dao.items().first().forEach{v->a.put(JSONObject().put("id",v.id).put("saleId",v.saleId).put("product",v.product).put("quantity",v.quantity).put("unitPriceCents",v.unitPriceCents))}})
  root.put("payments",JSONArray().also{a->dao.payments().first().forEach{v->a.put(JSONObject().put("id",v.id).put("saleId",v.saleId).put("amountCents",v.amountCents).put("paidAt",v.paidAt).put("method",v.method).put("receiptUri",v.receiptUri))}})
  val bytes=encrypt(root.toString().toByteArray(Charsets.UTF_8),password)
  context.contentResolver.openOutputStream(uri)?.use{it.write(bytes)}?:error("Não foi possível abrir destino")
 }
 suspend fun restoreIntoEmptyDatabase(context:Context,uri:Uri,password:CharArray){
  val bytes=context.contentResolver.openInputStream(uri)?.use{it.readBytes()}?:error("Não foi possível ler backup")
  require(bytes.size<50_000_000){"Arquivo grande demais"}
  val root=JSONObject(String(decrypt(bytes,password),Charsets.UTF_8))
  require(root.getInt("version")==1){"Versão incompatível"}
  val db=CrmDatabase.get(context);val dao=db.dao()
  require(dao.salons().first().isEmpty()){"Restauração permitida apenas em base vazia"}
  fun arr(name:String)=root.getJSONArray(name)
  db.withTransaction{
   arr("salons").let{a->for(i in 0 until a.length()){val o=a.getJSONObject(i);dao.saveSalon(Salon(id=o.getString("id"),name=o.getString("name"),city=o.optString("city"),address=o.optString("address"),phone=o.optString("phone"),status=o.optString("status"),notes=o.optString("notes"),placeId=o.optString("placeId").takeIf{it.isNotEmpty()&&it!="null"},latitude=o.optDouble("latitude").takeUnless{it.isNaN()},longitude=o.optDouble("longitude").takeUnless{it.isNaN()},lastContactAt=if(o.isNull("lastContactAt"))null else o.getLong("lastContactAt"),doNotContact=o.optBoolean("doNotContact")))}}
   arr("visits").let{a->for(i in 0 until a.length()){val o=a.getJSONObject(i);dao.addVisit(Visit(o.getString("id"),o.getString("salonId"),o.getLong("visitedAt"),o.getString("outcome"),o.optString("notes")))}}
   arr("appointments").let{a->for(i in 0 until a.length()){val o=a.getJSONObject(i);dao.addAppointment(Appointment(o.getString("id"),o.getString("salonId"),o.getLong("startsAt"),o.optString("notes"),o.optInt("remindMinutes",30),o.optBoolean("completed")))}}
   arr("sales").let{a->for(i in 0 until a.length()){val o=a.getJSONObject(i);dao.addSale(Sale(o.getString("id"),o.getString("salonId"),o.getLong("soldAt"),if(o.isNull("dueAt"))null else o.getLong("dueAt"),o.getLong("totalCents")))}}
   arr("items").let{a->for(i in 0 until a.length()){val o=a.getJSONObject(i);dao.addItem(SaleItem(o.getString("id"),o.getString("saleId"),o.getString("product"),o.getInt("quantity"),o.getLong("unitPriceCents")))}}
   arr("payments").let{a->for(i in 0 until a.length()){val o=a.getJSONObject(i);dao.addPayment(Payment(o.getString("id"),o.getString("saleId"),o.getLong("amountCents"),o.getLong("paidAt"),o.getString("method"),o.optString("receiptUri").takeIf{it.isNotEmpty()&&it!="null"}))}}
  }
 }
}
