package br.com.dalcotone.crm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class Prospect(val placeId:String,val name:String,val address:String,val latitude:Double?,val longitude:Double?,val phone:String?=null,val photoName:String?=null)
class PlacesRepository {
 private val client=OkHttpClient()
 suspend fun details(p:Prospect):Prospect = withContext(Dispatchers.IO){
  if(p.placeId.startsWith("osm:"))return@withContext p
  val key=BuildConfig.PLACES_API_KEY
  require(key.isNotBlank()){"Google Places não configurado"}
  val req=Request.Builder().url("https://places.googleapis.com/v1/places/"+p.placeId)
   .addHeader("X-Goog-Api-Key",key)
   .addHeader("X-Goog-FieldMask","id,nationalPhoneNumber,internationalPhoneNumber,photos").build()
  client.newCall(req).execute().use{response->
   if(!response.isSuccessful)error("Detalhes indisponíveis: HTTP ${response.code}")
   val j=JSONObject(response.body?.string()?:"{}")
   p.copy(phone=j.optString("nationalPhoneNumber").ifBlank{j.optString("internationalPhoneNumber")}.ifBlank{null},
    photoName=j.optJSONArray("photos")?.optJSONObject(0)?.optString("name"))
  }
 }
 suspend fun photoUrl(photoName:String):String? = withContext(Dispatchers.IO){
  val key=BuildConfig.PLACES_API_KEY
  if(key.isBlank())return@withContext null
  val req=Request.Builder().url("https://places.googleapis.com/v1/$photoName/media?maxWidthPx=500&skipHttpRedirect=true")
   .addHeader("X-Goog-Api-Key",key).build()
  client.newCall(req).execute().use{response->
   if(!response.isSuccessful)return@withContext null
   JSONObject(response.body?.string()?:"{}").optString("photoUri").ifBlank{null}
  }
 }
 suspend fun nearby(latitude:Double,longitude:Double):List<Prospect> = withContext(Dispatchers.IO){
  val key=BuildConfig.PLACES_API_KEY
  require(key.isNotBlank()){"Google Places não configurado"}
  val body=JSONObject().put("includedTypes",org.json.JSONArray().put("beauty_salon").put("hair_salon"))
   .put("maxResultCount",20)
   .put("locationRestriction",JSONObject().put("circle",JSONObject().put("center",JSONObject().put("latitude",latitude).put("longitude",longitude)).put("radius",10000.0)))
  val request=Request.Builder().url("https://places.googleapis.com/v1/places:searchNearby")
   .addHeader("X-Goog-Api-Key",key).addHeader("X-Goog-FieldMask","places.id,places.displayName,places.formattedAddress,places.location,places.businessStatus")
   .post(body.toString().toRequestBody("application/json".toMediaType())).build()
  client.newCall(request).execute().use{response->
   if(!response.isSuccessful)error("Falha Google Places: HTTP ${response.code}")
   val arr=JSONObject(response.body?.string()?:"{}").optJSONArray("places")?:return@withContext emptyList()
   (0 until arr.length()).mapNotNull{ i->val p=arr.getJSONObject(i);if(p.optString("businessStatus")=="CLOSED_PERMANENTLY")null else {
    val loc=p.optJSONObject("location");Prospect(p.getString("id"),p.optJSONObject("displayName")?.optString("text")?:"Salão",p.optString("formattedAddress"),loc?.optDouble("latitude"),loc?.optDouble("longitude"))
   }}
  }
 }
}
