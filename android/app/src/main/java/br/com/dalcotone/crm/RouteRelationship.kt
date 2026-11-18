package br.com.dalcotone.crm

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CityRoute(val city:String,val dateMillis:Long)
object RouteRelationship {
 private const val PREFS="dalcotone_city_routes"
 private const val KEY="routes"
 fun list(context:Context):List<CityRoute>{
  return try{
   val data=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY,"[]") ?: "[]"
   val array=JSONArray(data)
   (0 until array.length()).map{array.getJSONObject(it)}.map{CityRoute(it.getString("city"),it.getLong("dateMillis"))}.sortedBy{it.dateMillis}
  }catch(_:Exception){emptyList()}
 }
 fun save(context:Context,route:CityRoute){
  val routes=list(context).filterNot{it.city.equals(route.city,ignoreCase=true)}+route
  val array=JSONArray()
  routes.forEach{array.put(JSONObject().put("city",it.city).put("dateMillis",it.dateMillis))}
  context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,array.toString()).apply()
 }
 fun date(millis:Long):String=SimpleDateFormat("EEEE, dd/MM",Locale("pt","BR")).format(Date(millis))
 fun message(name:String,kind:String,route:CityRoute?,balanceCents:Long=0):String{
  val first=name.trim().substringBefore(" ").ifBlank{"mulher"}
  return when(kind){
   "rota"->"Oi, $first! Tudo bem? ❤️ Passando para avisar que estaremos em ${route?.city ?: "sua cidade"} ${route?.let{date(it.dateMillis)} ?: "em breve"} com a rota Dal Cotone. Se precisar de reposição, me avisa para eu organizar tudo! 🥰"
   "pagamento"->"Oi, $first! Tudo bem? ❤️ Passando para lembrar que estaremos em ${route?.city ?: "sua cidade"} ${route?.let{date(it.dateMillis)} ?: "em breve"} com a rota Dal Cotone. Se ficou algum acerto combinado para essa visita, me confirma por favor para organizarmos direitinho. 🥰"
   "reposicao"->"Oi, $first! ❤️ Como estão os produtos Dal Cotone por aí? Está precisando repor alguma coisa? Me conta que já organizo para nossa próxima passagem. 😍"
   else->"Oi, $first! Tudo bem por aí? ❤️ Passando para saber como estão as coisas no salão e se os produtos Dal Cotone estão rendendo direitinho. Qualquer coisa, estou por aqui! 🥰"
  }
 }
}
