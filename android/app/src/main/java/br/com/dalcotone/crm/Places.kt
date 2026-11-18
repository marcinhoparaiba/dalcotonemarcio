package br.com.dalcotone.crm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class Prospect(val placeId:String,val name:String,val address:String,val latitude:Double?,val longitude:Double?)
class PlacesRepository {
 private val client=OkHttpClient()
 suspend fun nearby(latitude:Double,longitude:Double):List<Prospect>=withContext(Dispatchers.IO){
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
