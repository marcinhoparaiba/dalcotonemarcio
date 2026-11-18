package br.com.dalcotone.crm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

/**
 * OpenStreetMap discovery is a separate, optional online source.
 * Do not treat OSM as an exhaustive business registry.
 */
class OpenStreetMapRepository {
 private val client=OkHttpClient.Builder().callTimeout(25,java.util.concurrent.TimeUnit.SECONDS).build()
 suspend fun nearby(lat:Double,lon:Double,radiusMeters:Int=3000):List<Prospect> = withContext(Dispatchers.IO){
  val radius=radiusMeters.coerceIn(250,10000)
  val query="[out:json][timeout:20];(nwr(around:$radius,$lat,$lon)[shop=hairdresser];nwr(around:$radius,$lat,$lon)[shop=beauty];nwr(around:$radius,$lat,$lon)[beauty=salon];);out center 100;"
  val url="https://overpass.kumi.systems/api/interpreter?data="+URLEncoder.encode(query,"UTF-8")
  val req=Request.Builder().url(url).header("User-Agent","DalCotoneCRM/0.1 (commercial field research)").build()
  client.newCall(req).execute().use{response->
   if(!response.isSuccessful)error("OpenStreetMap indisponível: HTTP ${response.code}")
   val data=JSONObject(response.body?.string()?:"{}").optJSONArray("elements")?:return@withContext emptyList()
   (0 until data.length()).mapNotNull{i->
    val el=data.getJSONObject(i)
    val tags=el.optJSONObject("tags")?:return@mapNotNull null
    val name=tags.optString("name").trim()
    if(name.isEmpty())return@mapNotNull null
    val center=el.optJSONObject("center")
    val latitude=if(el.has("lat"))el.optDouble("lat") else center?.optDouble("lat")
    val longitude=if(el.has("lon"))el.optDouble("lon") else center?.optDouble("lon")
    if(latitude==null||longitude==null||!latitude.isFinite()||!longitude.isFinite())return@mapNotNull null
    val address=listOf(tags.optString("addr:street"),tags.optString("addr:housenumber"),tags.optString("addr:city")).filter{it.isNotBlank()}.joinToString(", ")
    Prospect("osm:${el.optString("type")}:${el.optLong("id")}",name,address,latitude,longitude)
   }.distinctBy{it.placeId}
  }
 }
}

/** Manual CNPJ validation/lookup, not a bulk business directory or GPS discovery API. */
class CnpjLookupRepository {
 private val client=OkHttpClient.Builder().callTimeout(15,java.util.concurrent.TimeUnit.SECONDS).build()
 suspend fun lookup(cnpj:String):CnpjBusiness=withContext(Dispatchers.IO){
  val digits=cnpj.filter{it.isDigit()}
  require(digits.length==14){"Informe um CNPJ de 14 dígitos"}
  val req=Request.Builder().url("https://brasilapi.com.br/api/cnpj/v1/$digits").build()
  client.newCall(req).execute().use{res->
   if(!res.isSuccessful)error("Consulta CNPJ indisponível (HTTP ${res.code})")
   val j=JSONObject(res.body?.string()?:"{}")
   CnpjBusiness(j.optString("razao_social"),j.optString("nome_fantasia"),j.optString("descricao_situacao_cadastral"),j.optString("municipio"),j.optString("uf"))
  }
 }
}
data class CnpjBusiness(val legalName:String,val tradeName:String,val status:String,val city:String,val state:String)
