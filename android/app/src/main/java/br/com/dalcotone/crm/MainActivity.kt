package br.com.dalcotone.crm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

private val Ruby=Color(0xFFCA1737)
private val Ink=Color(0xFFF8F5F7)
private val Muted=Color(0xFFBFB6C1)
private val CanvasBg=Color(0xFF0B0B11)
private val CardBg=Color(0xFF201A24)
private fun statusLabel(status:String)=when(status){"nao_visitado"->"Não visitado";"cliente"->"Cliente";"retorno"->"Retorno pendente";"recusou"->"Não interessado";"inativo"->"Inativo";else->status.replace('_',' ')}
@Composable private fun PremiumCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){
 Card(modifier=modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),
  colors=CardDefaults.cardColors(containerColor=CardBg),elevation=CardDefaults.cardElevation(defaultElevation=2.dp)){
  Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp),content=content)
 }
}
@Composable private fun EmptyPanel(title:String,subtitle:String){
 PremiumCard{Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,color=Ink)
 Text(subtitle,color=Muted,style=MaterialTheme.typography.bodyMedium)}
}

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);Reminders.channel(this)
  setContent { MaterialTheme(colorScheme=darkColorScheme(primary=Ruby,onPrimary=Color.White,background=CanvasBg,surface=CardBg,onSurface=Ink,secondary=Ink,surfaceVariant=Color(0xFF302330),outline=Color(0xFF72505A))) { CRMApp() } }
 }
 @Composable private fun CRMApp(){
  val dao=remember{CrmDatabase.get(this).dao()}
  val salons by dao.salons().collectAsStateWithLifecycle(initialValue=emptyList())
  val appointments by dao.appointments().collectAsStateWithLifecycle(initialValue=emptyList())
  val sales by dao.sales().collectAsStateWithLifecycle(initialValue=emptyList())
  val payments by dao.payments().collectAsStateWithLifecycle(initialValue=emptyList())
  val items by dao.items().collectAsStateWithLifecycle(initialValue=emptyList())
  val scope=rememberCoroutineScope()
  var tab by remember{mutableStateOf("Início")}
  val mapSession=remember{SalonMapSession()}
  var selected by remember{mutableStateOf<Salon?>(null)}
  var phoneEditor by remember{mutableStateOf<Salon?>(null)}
  var phoneDraft by remember{mutableStateOf("")}
  var cityRoutes by remember{mutableStateOf(RouteRelationship.list(this))}
  var messageSalon by remember{mutableStateOf<Salon?>(null)}
  var messageKind by remember{mutableStateOf("relacionamento")}
  var messageDraft by remember{mutableStateOf("")}
  var add by remember{mutableStateOf(false)}
  var quickCapture by remember{mutableStateOf(false)}
  var capturedLatitude by remember{mutableStateOf<Double?>(null)}
  var capturedLongitude by remember{mutableStateOf<Double?>(null)}
  var capturedAccuracy by remember{mutableStateOf<Float?>(null)}
  var name by remember{mutableStateOf("")};var city by remember{mutableStateOf("")};var address by remember{mutableStateOf("")};var phone by remember{mutableStateOf("")}
  var message by remember{mutableStateOf("")}
  var search by remember{mutableStateOf("")}
  var filter by remember{mutableStateOf("todos")}
  var selectedCity by remember{mutableStateOf<String?>(null)}
  var routeMode by remember{mutableStateOf(false)}
  var routeAll by remember{mutableStateOf(true)}
  var routeSelectedIds by remember{mutableStateOf<Set<String>>(emptySet())}
  var routeBatch by remember{mutableIntStateOf(0)}
  var routeCity by remember{mutableStateOf<String?>(null)}
  var routeAudience by remember{mutableStateOf("clientes")}
  var cityFilter by remember{mutableStateOf("todos")}
  var editOutcome by remember{mutableStateOf(false)}
  var outcome by remember{mutableStateOf("cliente")}
  var notes by remember{mutableStateOf("")}
  var schedule by remember{mutableStateOf(false)}
  var dateTime by remember{mutableStateOf("")}
  var sell by remember{mutableStateOf(false)}
  var product by remember{mutableStateOf("")};var qty by remember{mutableStateOf("1")};var price by remember{mutableStateOf("")}
  var paymentSale by remember{mutableStateOf<Sale?>(null)}
  var amount by remember{mutableStateOf("")}
  var paymentMethod by remember{mutableStateOf("pix")}
  var prospectList by remember{mutableStateOf<List<Prospect>>(emptyList())}
  var chosenProspect by remember{mutableStateOf<Prospect?>(null)}
  var prospectPhotoUrl by remember{mutableStateOf<String?>(null)}
  var detailsLoading by remember{mutableStateOf(false)}
  var cnpjDialog by remember{mutableStateOf(false)}
  var cnpjText by remember{mutableStateOf("")}
  var cnpjResult by remember{mutableStateOf<CnpjBusiness?>(null)}
  var searching by remember{mutableStateOf(false)}
  var scanProgress by remember{mutableIntStateOf(0)}
  var scanFinished by remember{mutableStateOf(false)}
  LaunchedEffect(searching){if(searching){while(searching){delay(160);scanProgress=(scanProgress+maxOf(1,(94-scanProgress)/12)).coerceAtMost(94)}}}
  var enrichingPhones by remember{mutableStateOf(false)}
  val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){}
  val locationPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){}
  val locationManager=remember{getSystemService(LOCATION_SERVICE) as android.location.LocationManager}
  val places=remember{PlacesRepository()}
  val osm=remember{OpenStreetMapRepository()}
  val cnpjLookup=remember{CnpjLookupRepository()}
  val money=remember{NumberFormat.getCurrencyInstance(Locale("pt","BR"))}
  val db=remember{CrmDatabase.get(this)}
  var voiceDraft by remember{mutableStateOf<String?>(null)}
  var voiceSalonId by remember{mutableStateOf<String?>(null)}
  val exportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
   if(uri!=null)scope.launch{
    try{
     val backup=withContext(Dispatchers.IO){CrmBackup.export(db)}
     withContext(Dispatchers.IO){contentResolver.openOutputStream(uri)?.use{it.write(backup.toByteArray(Charsets.UTF_8))} ?: error("Não foi possível gravar o arquivo")}
     message="Backup exportado. Guarde o arquivo em local seguro."
    }catch(e:Exception){message="Erro no backup: ${e.message}"}
   }
  }
  val importLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
   if(uri!=null)scope.launch{
    try{
     val count=withContext(Dispatchers.IO){
      val json=contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()} ?: error("Arquivo inacessível")
      CrmBackup.restore(db,json)
     }
     message="Backup restaurado: $count salões. Confira seus dados."
    }catch(e:Exception){message="Falha ao restaurar: ${e.message}"}
   }
  }
  val speechLauncher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
   if(result.resultCode==android.app.Activity.RESULT_OK){
    voiceDraft=result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
   }
  }
  fun speak(){
   voiceSalonId=selected?.id
   try{
    speechLauncher.launch(Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
     putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
     putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE,"pt-BR")
     putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT,"Diga o resumo da visita ou próximo retorno")
    })
   }catch(e:Exception){message="Reconhecimento de voz não disponível neste aparelho."}
  }
  val contactWindow=7L*24*60*60*1000
  fun contactDays(s:Salon):Long=if(s.lastContactAt==null)7L else ((System.currentTimeMillis()-s.lastContactAt).coerceAtLeast(0L)/(24L*60*60*1000))
  fun isAttendedClient(s:Salon):Boolean=s.status=="cliente" || sales.any{it.salonId==s.id}
  fun contactDue(s:Salon):Boolean=isAttendedClient(s) && !s.doNotContact && s.status!="recusou" && s.status!="inativo" && (s.lastContactAt==null || System.currentTimeMillis()-s.lastContactAt>=contactWindow)
  fun confirmContact(s:Salon){
   scope.launch{
    dao.saveSalon(s.copy(lastContactAt=System.currentTimeMillis()))
    message="Contato confirmado com ${s.name}. Próximo contato em até 7 dias."
   }
  }
  fun leadScore(s:Salon):Int {
   var score=when(s.status){"cliente"->55;"retorno"->65;"nao_visitado"->50;"recusou"->5;"inativo"->15;else->30}
   if(s.phone.isNotBlank())score+=10
   if(s.latitude!=null&&s.longitude!=null)score+=10
   if(contactDue(s))score+=20
   else if(contactDays(s)>=6)score+=10
   if(s.doNotContact)score=0
   return score.coerceIn(0,100)
  }

  fun whatsapp(raw:String){
   val digits=raw.filter{it.isDigit()}
   val international=when{
    digits.length in 12..13&&digits.startsWith("55")->digits
    digits.length in 10..11->"55"+digits
    else->""
   }
   if(international.isBlank()){message="Número inválido para WhatsApp. Confira o DDD.";return}
   val uri=Uri.parse("https://wa.me/$international")
   try{
    startActivity(Intent(Intent.ACTION_VIEW,uri).apply{
     addCategory(Intent.CATEGORY_BROWSABLE)
    })
   }catch(e:android.content.ActivityNotFoundException){
    message="Não foi possível abrir o WhatsApp neste aparelho."
   }catch(e:SecurityException){
    message="O Android bloqueou a abertura da conversa."
   }
  }
  fun openMessage(s:Salon,kind:String="relacionamento"){
   messageSalon=s
   messageKind=kind
   val route=cityRoutes.find{it.city.equals(s.city,ignoreCase=true)}
   val balance=sales.filter{it.salonId==s.id}.sumOf{it.totalCents-payments.filter{p->p.saleId==it.id}.sumOf{p->p.amountCents}}
   messageDraft=RouteRelationship.message(s.name,kind,route,balance)
  }
  fun sendPreparedMessage(){
   val s=messageSalon?:return
   val digits=s.phone.filter{it.isDigit()}
   val international=when{
    digits.length in 12..13 && digits.startsWith("55")->digits
    digits.length in 10..11->"55"+digits
    else->""
   }
   if(international.isBlank()){message="Cadastre um telefone com DDD na ficha antes de enviar.";return}
   val uri=Uri.parse("https://wa.me/$international?text="+Uri.encode(messageDraft))
   try{
    startActivity(Intent(Intent.ACTION_VIEW,uri).addCategory(Intent.CATEGORY_BROWSABLE))
    messageSalon=null
    message="Mensagem preparada no WhatsApp. Confirme o envio na ficha depois."
   }catch(e:Exception){message="Não foi possível abrir o WhatsApp: ${e.message}"}
  }
  fun openSalonWhatsapp(s:Salon){
   val digits=s.phone.filter{it.isDigit()}
   if(digits.length !in 10..11 && !(digits.length in 12..13 && digits.startsWith("55"))){
    phoneDraft=s.phone
    phoneEditor=s
    return
   }
   whatsapp(s.phone)
  }
  fun routeOrder(stops:List<Salon>):List<Salon>{
   val remaining=stops.filter{it.latitude!=null&&it.longitude!=null}.toMutableList()
   val provider=when{
    locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)->android.location.LocationManager.GPS_PROVIDER
    locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)->android.location.LocationManager.NETWORK_PROVIDER
    else->null
   }
   val here=try{if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED||checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED)provider?.let{locationManager.getLastKnownLocation(it)} else null}catch(_:SecurityException){null}
   var lat=here?.latitude?:remaining.firstOrNull()?.latitude?:return emptyList()
   var lon=here?.longitude?:remaining.firstOrNull()?.longitude?:return emptyList()
   val ordered=mutableListOf<Salon>()
   while(remaining.isNotEmpty()){
    val next=remaining.minByOrNull{s->
     val out=FloatArray(1)
     android.location.Location.distanceBetween(lat,lon,s.latitude!!,s.longitude!!,out)
     out[0]
    }!!
    ordered.add(next);remaining.remove(next);lat=next.latitude!!;lon=next.longitude!!
   }
   return ordered
  }
  fun openRoute(stops:List<Salon>,batchIndex:Int=0){
   val eligible=routeOrder(stops)
   if(eligible.isEmpty()){message="Nenhum cliente selecionado possui coordenadas para navegação.";return}
   val batches=eligible.chunked(8)
   if(batchIndex !in batches.indices){message="Etapa não encontrada.";return}
   val batch=batches[batchIndex]
   val points=batch.map{"${it.latitude},${it.longitude}"}
   val destination=Uri.encode(points.last())
   val waypoints=points.dropLast(1).joinToString("|")
   val url="https://www.google.com/maps/dir/?api=1&destination=$destination&travelmode=driving"+if(waypoints.isNotBlank())"&waypoints="+Uri.encode(waypoints) else ""
   try{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)));routeBatch=batchIndex
    message="Etapa ${batchIndex+1} de ${batches.size} aberta no Maps. Ordem aproximada por proximidade; o Maps calcula trânsito e duração."
   }catch(e:Exception){message="Não foi possível abrir o Google Maps: ${e.message}"}
  }
  fun navigate(s:Salon){val q=Uri.encode(listOf(s.address,s.city,"Paraíba Brasil").joinToString(" "));startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/search/?api=1&query=$q")))}
  fun captureHere(){
   if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
    locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION));message="Autorize o GPS e toque novamente em Salvar salão aqui.";return
   }
   val provider=when{locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)->android.location.LocationManager.GPS_PROVIDER;locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)->android.location.LocationManager.NETWORK_PROVIDER;else->null}
   if(provider==null){message="Ative a localização do celular.";return}
   try{
    locationManager.getCurrentLocation(provider,null,mainExecutor){location->
     if(location==null){message="Não foi possível obter o ponto. Tente novamente em local aberto."}
     else if(location.accuracy>100f){message="Precisão baixa (${location.accuracy.toInt()} m). Aguarde e tente novamente."}
     else{capturedLatitude=location.latitude;capturedLongitude=location.longitude;capturedAccuracy=location.accuracy;quickCapture=true;message="Localização capturada. Informe o nome do salão."}
    }
   }catch(e:SecurityException){message="Permissão de localização necessária."}
  }
  fun cityFromAddress(address:String):String {
   val match=Regex("""(?:^|,\s*)([^,]+?)\s*-\s*[A-Z]{2}(?:,|\s*-\s*Brasil|$)""").findAll(address).lastOrNull()
   return match?.groupValues?.get(1)?.trim()?.substringAfterLast(",")?.trim().orEmpty()
  }
  suspend fun persistDiscovery(found:List<Prospect>):Int {
   var saved=0
   found.distinctBy{it.placeId}.forEach{p->
    if(p.placeId.isNotBlank()){
     val s=Salon(name=p.name,address=p.address,city=cityFromAddress(p.address),
      placeId=p.placeId,latitude=p.latitude,longitude=p.longitude,phone=p.phone?:"",
      status="nao_visitado")
     if(dao.saveDiscoveredSalon(s)!=-1L)saved++
    }
   }
   return saved
  }
  fun enrichPublicPhones(){
   if(enrichingPhones || searching)return
   if(BuildConfig.PLACES_API_KEY.isBlank()){
    message="Configure a chave Google Places para consultar telefones públicos."
    return
   }
   val candidates=salons.filter{it.phone.isBlank() && !it.placeId.isNullOrBlank() && !it.placeId!!.startsWith("osm:")}
   if(candidates.isEmpty()){message="Nenhum salão do Google sem telefone para consultar.";return}
   scope.launch{
    enrichingPhones=true
    var updated=0
    var unavailable=0
    try{
     candidates.forEach{s->
      try{
       val detail=places.details(Prospect(s.placeId!!,s.name,s.address,s.latitude,s.longitude))
       val phone=detail.phone?.trim().orEmpty()
       if(phone.isNotBlank()){
        val current=salons.firstOrNull{it.id==s.id}
        if(current!=null && current.phone.isBlank()){
         dao.saveSalon(current.copy(phone=phone))
         updated++
        }
       }else unavailable++
      }catch(e:Exception){unavailable++}
     }
     message="Telefones públicos: $updated preenchidos; $unavailable indisponíveis ou com falha. Verifique os números antes de usar no WhatsApp."
    }finally{enrichingPhones=false}
   }
  }
  fun scan(){
   if(searching)return
   if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
    locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION));message="Autorize a localização e toque novamente.";return
   }
   val provider=when{locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)->android.location.LocationManager.GPS_PROVIDER;locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)->android.location.LocationManager.NETWORK_PROVIDER;else->null}
   val location=provider?.let{locationManager.getLastKnownLocation(it)}
   if(location==null){message="Sem posição recente. Ative o GPS e tente novamente.";return}
   scope.launch{
    searching=true
    scanFinished=false
    scanProgress=3
    message="Buscando salões próximos no Google..."
    try{
     val known=salons.mapNotNull{it.placeId}.toSet()
     val found=places.nearby(location.latitude,location.longitude).filter{it.placeId !in known}
     var phonesFound=0
     val detailed=found.map{p->
      try{
       val detail=places.details(p)
       if(!detail.phone.isNullOrBlank())phonesFound++
       detail
      }catch(_:Exception){p}
     }
     val saved=persistDiscovery(detailed)
     prospectList=emptyList()
     message="${found.size} salões encontrados no Google • $saved novos salvos • $phonesFound telefones públicos encontrados."
     scanProgress=100
     scanFinished=true
    }catch(e:Exception){scanProgress=0;scanFinished=false;message="Não foi possível consultar o Google: "+(e.message?:"erro de conexão")}
    finally{searching=false}
   }
  }
  fun searchOsm(){
   if(searching)return
   if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
    locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
    message="Autorize a localização e toque novamente.";return
   }
   val provider=when{locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)->android.location.LocationManager.GPS_PROVIDER;locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)->android.location.LocationManager.NETWORK_PROVIDER;else->null}
   val location=provider?.let{locationManager.getLastKnownLocation(it)}
   if(location==null){message="Sem localização recente. Ative o GPS e tente novamente.";return}
   scope.launch{
    searching=true
    try{
     val known=salons.mapNotNull{it.placeId}.toSet()
     val existing=prospectList.associateBy{it.placeId}
     val found=osm.nearby(location.latitude,location.longitude).filter{it.placeId !in known}
     val saved=persistDiscovery(found)
     prospectList=emptyList()
     message="${found.size} salões encontrados no OpenStreetMap • $saved novos salvos offline."
    }catch(e:Exception){message=e.message?:"Busca OpenStreetMap indisponível"}
    finally{searching=false}
   }
  }
  Box(Modifier.fillMaxSize()){
   Box(Modifier.fillMaxSize().background(
    androidx.compose.ui.graphics.Brush.verticalGradient(
     if(tab=="Rota"||tab=="Mapa") listOf(Color(0xFF0A0A11),Color(0xFF1D101A),Color(0xFF290B15))
     else listOf(Color(0xFF0B0B11),Color(0xFF18111A),Color(0xFF230B14))
    )
   ))
   androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides Ink){
   Column(Modifier.fillMaxSize().padding(horizontal=16.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
   Surface(shape=RoundedCornerShape(20.dp),color=Color(0xFF160F18),modifier=Modifier.fillMaxWidth()){
    Row(Modifier.padding(horizontal=18.dp,vertical=14.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
     Surface(shape=RoundedCornerShape(12.dp),color=Ruby,modifier=Modifier.size(44.dp)){
      Box(contentAlignment=androidx.compose.ui.Alignment.Center){Text("DC",color=Color.White,fontWeight=FontWeight.ExtraBold)}
     }
     Column(Modifier.weight(1f)){
      Text("Dalcotone",color=Color.White,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.ExtraBold)
      Text("CONTROLE E CAPTAÇÃO DE CLIENTES",color=Color(0xFFFFA0B0),style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
      Text(when(tab){"Início"->"Painel comercial";"Mapa"->"Radar comercial";"Cidades"->"Territórios";"Clientes"->"Minha carteira";"Rota"->"Planejar visitas";"Agenda"->"Minha agenda";else->"Financeiro"},color=Color.White,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
     }
     Column(horizontalAlignment=androidx.compose.ui.Alignment.End){
      Text("${salons.size}",color=Color.White,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
      Text("salões",color=Color(0xFFD6C5CE),style=MaterialTheme.typography.labelSmall)
     }
    }
   }
   val navigationItems=listOf("Início" to "⌂","Mapa" to "◉","Cidades" to "⌖","Clientes" to "♙","Rota" to "➤","Agenda" to "▦","Financeiro" to "R$")
   LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(vertical=2.dp)){
    items(navigationItems){(target,symbol)->
     val active=tab==target
     Surface(onClick={tab=target;selected=null;message=""},shape=RoundedCornerShape(15.dp),
      color=if(active)Ruby else CardBg,
      border=if(active)null else androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF6A3845))){
      Row(Modifier.padding(horizontal=14.dp,vertical=11.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
       Text(symbol,color=if(active)Color.White else Ruby,fontWeight=FontWeight.Bold)
       Text(target,color=if(active)Color.White else Ink,fontWeight=if(active)FontWeight.Bold else FontWeight.Medium)
      }
     }
    }
   }
   if(message.isNotBlank())Surface(color=Color(0xFF381C29),shape=RoundedCornerShape(12.dp)){
    Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
     Text(message,Modifier.weight(1f),color=Ruby,style=MaterialTheme.typography.bodySmall)
     TextButton(onClick={message=""},contentPadding=PaddingValues(horizontal=6.dp)){Text("Fechar")}
    }
   }
   if(selected!=null){
    val s=selected!!
    TextButton(onClick={selected=null}){Text("← Voltar")}
    PremiumCard{
     Text(s.name,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,color=Ink)
     Text(statusLabel(s.status),color=Ruby,fontWeight=FontWeight.SemiBold)
     Text(s.city.ifBlank{"Cidade não informada"},color=Muted)
     if(s.address.isNotBlank())Text(s.address,color=Muted)
     Text("PRIORIDADE COMERCIAL  •  ${leadScore(s)}/100",color=Ruby,style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Bold)
     Text(
      when{
       s.doNotContact->"CONTATO BLOQUEADO"
       !isAttendedClient(s)->"PROSPECTO • SEM ROTINA DE 7 DIAS"
       s.lastContactAt==null->"SEM CONTATO REGISTRADO • PRIORIDADE"
       contactDue(s)->"CONTATO ATRASADO • ${contactDays(s)} DIAS"
       contactDays(s)>=6->"CONTATO VENCE EM BREVE • ${contactDays(s)} DIAS"
       else->"CONTATO EM DIA • ${contactDays(s)} DIAS"
      },color=if(contactDue(s))Ruby else Muted,fontWeight=FontWeight.Bold
     )
     if(isAttendedClient(s))Text("Próximo contato: "+(s.lastContactAt?.let{java.text.DateFormat.getDateInstance().format(java.util.Date(it+contactWindow))}?:"hoje"),color=Muted)
     if(isAttendedClient(s) && !s.doNotContact)OutlinedButton(onClick={openMessage(s)},modifier=Modifier.fillMaxWidth()){
      Text("♡ Preparar mensagem de relacionamento")
     }
     if(isAttendedClient(s) && !s.doNotContact)OutlinedButton(onClick={confirmContact(s)},modifier=Modifier.fillMaxWidth()){
      Text("✓ Confirmar que enviei a mensagem")
     }
     Text("AÇÕES RÁPIDAS",color=Muted,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
     Button(onClick={openSalonWhatsapp(s)},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF168956)),shape=RoundedCornerShape(14.dp)){
      Text(if(s.phone.filter{it.isDigit()}.length>=10)"◉  Conversar pelo WhatsApp" else "＋  Adicionar WhatsApp",color=Color.White)
     }
     Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
      OutlinedButton(onClick={navigate(s)},modifier=Modifier.weight(1f)){Text("➤  Abrir rota")}
      OutlinedButton(onClick={phoneDraft=s.phone;phoneEditor=s},modifier=Modifier.weight(1f)){Text("✎  Telefone")}
     }
    }
    val ownSales=sales.filter{it.salonId==s.id}
    val outstanding=ownSales.sumOf{sale->sale.totalCents-payments.filter{it.saleId==sale.id}.sumOf{it.amountCents}}
    PremiumCard{
     Text("FINANCEIRO DO SALÃO",style=MaterialTheme.typography.labelMedium,color=Muted)
     Text(money.format(outstanding/100.0),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold,color=Ruby)
     Text("Saldo a receber",color=Muted)
    }
    OutlinedButton(onClick={speak()},modifier=Modifier.fillMaxWidth()){Text("🎙 Registrar anotação por voz")}
    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
     Button(onClick={editOutcome=true},modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=4.dp)){Text("Visita")}
     OutlinedButton(onClick={schedule=true},modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=4.dp)){Text("Agendar")}
     OutlinedButton(onClick={sell=true},modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=4.dp)){Text("Venda")}
    }
    Text("Histórico de compras",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
    if(ownSales.isEmpty())EmptyPanel("Nenhuma compra registrada","A primeira venda transforma este salão em cliente automaticamente.")
    LazyColumn{items(ownSales){sale->
     Card(Modifier.fillMaxWidth().padding(vertical=5.dp)){Column(Modifier.padding(12.dp)){
      Text("Venda: ${money.format(sale.totalCents/100.0)}")
      items.filter{it.saleId==sale.id}.forEach{Text("${it.quantity}x ${it.product}")}
      val due=sale.totalCents-payments.filter{it.saleId==sale.id}.sumOf{it.amountCents}
      Text("Falta: ${money.format(due/100.0)}")
      payments.filter{it.saleId==sale.id}.sortedByDescending{it.paidAt}.forEach{payment->
       val method=when(payment.method){"pix"->"Pix";"dinheiro"->"Dinheiro";"cartao"->"Cartão";"transferencia"->"Transferência";else->payment.method}
       Text("Recebido: ${money.format(payment.amountCents/100.0)} • $method • ${java.text.DateFormat.getDateInstance().format(java.util.Date(payment.paidAt))}",style=MaterialTheme.typography.bodySmall,color=Color(0xFF16885D))
      }
      if(due>0)TextButton(onClick={paymentSale=sale}){Text("Registrar pagamento")}
     }}
    }}
   }else when(tab){
    "Início"->{
     Surface(shape=RoundedCornerShape(22.dp),color=Color(0xFF3B0C1B),modifier=Modifier.fillMaxWidth()){
      Row(Modifier.fillMaxWidth().padding(20.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
       Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(9.dp)){
        Text("DALCOTONE PROFESSIONAL",color=Color(0xFFFF889A),style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
        Text("Mais do que produtos, resultado para os salões!",color=Color.White,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold)
        Text("Controle, captação e relacionamento em um só lugar.",color=Muted,style=MaterialTheme.typography.bodySmall)
       }
       Text("DC",color=Color(0xFFFF3B59),style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Black)
      }
     }
     val shortcuts=listOf(Triple("Rastrear Salões","Mapa","◉"),Triple("Rotas","Rota","➤"),Triple("Clientes","Clientes","♙"),Triple("Cobranças","Financeiro","R$"),Triple("Contato Automático","Clientes","✉"),Triple("Relatórios","Financeiro","▦"))
     shortcuts.chunked(3).forEachIndexed{rowIndex,row->
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)){
       row.forEachIndexed{index,item->
        Surface(onClick={tab=item.second},modifier=Modifier.weight(1f).height(112.dp),shape=RoundedCornerShape(17.dp),color=if(rowIndex==0&&index==0)Ruby else CardBg,border=androidx.compose.foundation.BorderStroke(1.dp,if(rowIndex==0&&index==0)Color(0xFFFF6A80) else Color(0xFF58414C))){
         Column(Modifier.fillMaxSize().padding(8.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally){
          Text(item.third,color=Color.White,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
          Text(item.first,color=Color.White,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
         }
        }
       }
      }
     }
     PremiumCard{
      Text("Resumo da Semana",color=Ink,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
      Text("Salões mapeados: ${salons.size}",color=Ink)
      Text("Visitas agendadas: ${appointments.size}",color=Ink)
      Text("Clientes ativos: ${salons.count{it.status==\"cliente\"}}",color=Ink)
     }
    }
    "Cidades"->{
     Text("Explore por cidade",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
     Text("Organize a prospecção por território e acompanhe os atendimentos.",style=MaterialTheme.typography.bodySmall,color=Muted)
     if(salons.isEmpty())EmptyPanel("Nenhum salão cadastrado","Abra o Radar comercial e busque salões próximos para começar.")
     if(selectedCity==null){
      val groups=salons.groupBy{it.city.trim().ifBlank{"Cidade não informada"}}.toSortedMap()
      LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){
       items(groups.entries.toList()){entry->
        Card(onClick={selectedCity=entry.key},shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=CardBg),elevation=CardDefaults.cardElevation(defaultElevation=2.dp),modifier=Modifier.fillMaxWidth()){
         Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween){
          Column{
           Text(entry.key,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
           Text("${entry.value.size} salões salvos • ${entry.value.count{it.status=="nao_visitado"}} não visitados",style=MaterialTheme.typography.bodySmall)
          }
          Text("Abrir ›",color=Ruby)
         }
        }
       }
      }
     }else{
      TextButton(onClick={selectedCity=null}){Text("← Todas as cidades")}
      Text(selectedCity!!,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
      Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
       listOf("todos","nao_visitado","cliente").forEach{f->
        FilterChip(selected=cityFilter==f,onClick={cityFilter=f},label={Text(when(f){"todos"->"Todos";"nao_visitado"->"Não visitados";else->"Clientes"})})
       }
      }
      LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){
       items(salons.filter{it.city.trim().ifBlank{"Cidade não informada"}==selectedCity&&(cityFilter=="todos"||it.status==cityFilter)}.sortedBy{it.name}){s->
        Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=CardBg),modifier=Modifier.fillMaxWidth()){
         Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
          Text(s.name,fontWeight=FontWeight.Bold)
          Text(s.address.ifBlank{"Endereço não informado"},style=MaterialTheme.typography.bodySmall)
          Text(statusLabel(s.status),color=Ruby,style=MaterialTheme.typography.bodySmall)
          Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
           Button(onClick={selected=s},modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=4.dp)){Text("Ficha")}
           OutlinedButton(onClick={navigate(s)},modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=4.dp)){Text("Rota")}
           OutlinedButton(onClick={openSalonWhatsapp(s)},modifier=Modifier.weight(1.4f),contentPadding=PaddingValues(horizontal=4.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=Color(0xFF50D48B))){Text("WhatsApp")}
          }
         }
        }
       }
      }
     }
    }
    "Rota"->{
     Text("Seu próximo roteiro",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
     Text("Planeje uma sequência de visitas sem deslocamentos desnecessários.",style=MaterialTheme.typography.bodyMedium,color=Muted)
     Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
      listOf("clientes" to "Clientes","prospectos" to "Prospectos","ambos" to "Mista").forEach{(value,label)->
       FilterChip(selected=routeAudience==value,onClick={routeAudience=value;routeSelectedIds=emptySet()},label={Text(label)})
      }
     }
     val customers=salons.filter{s->
      val customer=s.status=="cliente"||sales.any{sale->sale.salonId==s.id}
      when(routeAudience){"clientes"->customer;"prospectos"->!customer&&s.status=="nao_visitado";else->customer||s.status=="nao_visitado"}
     }
     val geolocated=customers.filter{it.latitude!=null&&it.longitude!=null}
     if(customers.isNotEmpty())PremiumCard{
      Text("PRIORIDADES COMERCIAIS",color=Ruby,fontWeight=FontWeight.Bold)
      customers.filter{!it.doNotContact}.sortedByDescending{leadScore(it)}.take(3).forEach{s->
       Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
        Text(s.name,Modifier.weight(1f),maxLines=1)
        Text("${leadScore(s)}/100",color=Ruby,fontWeight=FontWeight.Bold)
        TextButton(onClick={selected=s}){Text("Ficha")}
       }
      }
     }
     PremiumCard{
      Text("ESTABELECIMENTOS DISPONÍVEIS",style=MaterialTheme.typography.labelSmall,color=Muted)
      Text("${customers.size} salões  •  ${geolocated.size} com GPS",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,color=Ink)
      if(customers.isEmpty())Text("Nenhum salão nesta categoria. Selecione Prospectos para planejar novas visitas.",color=Muted)
     }
     Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
      FilterChip(selected=routeAll,onClick={routeAll=true},label={Text("ROTA TOTAL")})
      FilterChip(selected=!routeAll,onClick={routeAll=false},label={Text("Selecionar paradas")})
     }
     Text("Selecione uma cidade ou use todas. Escolha todos ou apenas alguns salões.",style=MaterialTheme.typography.bodySmall)
     val routeCities=customers.map{it.city.trim().ifBlank{"Cidade não informada"}}.distinct().sorted()
     Text("Cidades",fontWeight=FontWeight.Bold)
     LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){
      item{FilterChip(selected=routeCity==null,onClick={routeCity=null},label={Text("Toda Paraíba")})}
      items(routeCities){town->FilterChip(selected=routeCity==town,onClick={routeCity=town},label={Text(town)})}
     }
     val inCity=geolocated.filter{routeCity==null||it.city.trim().ifBlank{"Cidade não informada"}==routeCity}
     val chosen=if(routeAll)inCity else inCity.filter{it.id in routeSelectedIds}
     val ordered=routeOrder(chosen)
     Text("${ordered.size} paradas  •  ${(ordered.size+7)/8} etapa(s)",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
     Text("Ordem aproximada por proximidade. O Google Maps calcula o percurso viário de cada etapa.",style=MaterialTheme.typography.bodySmall,color=Muted)
     Button(onClick={openRoute(chosen,0)},enabled=chosen.isNotEmpty(),modifier=Modifier.fillMaxWidth()){
      Text("INICIAR PRIMEIRA ETAPA NO GOOGLE MAPS")
     }
     if(ordered.size>8) {
      Text("Escolha a próxima etapa depois de concluir a anterior:",style=MaterialTheme.typography.bodySmall)
      ordered.chunked(8).forEachIndexed{index,batch->
       OutlinedButton(onClick={openRoute(chosen,index)},modifier=Modifier.fillMaxWidth()){
        Text("Etapa ${index+1} • ${batch.size} paradas")
       }
      }
     }
     LazyColumn{
      items(customers.filter{routeCity==null||it.city.trim().ifBlank{"Cidade não informada"}==routeCity}.sortedWith(compareBy({it.city},{it.name}))){s->
       Row(Modifier.fillMaxWidth().padding(vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){
        if(!routeAll)Checkbox(checked=s.id in routeSelectedIds,onCheckedChange={checked->routeSelectedIds=if(checked)routeSelectedIds+s.id else routeSelectedIds-s.id})
        Column(Modifier.weight(1f)){
         Text(s.name,fontWeight=FontWeight.Bold)
         Text("${s.city.ifBlank{"Cidade não informada"}} • ${if(s.latitude==null||s.longitude==null)"Sem GPS" else "GPS salvo"}",style=MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick={selected=s}){Text("Ficha")}
       }
      }
     }
    }
    "Mapa","Clientes"->{
     if(tab=="Mapa"){
      PremiumCard{
       Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
        Column{
         Text("RADAR AO VIVO",color=Ruby,style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Bold)
         Text("Explore seu território",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
        }
        Surface(color=Color(0xFF381C29),shape=RoundedCornerShape(14.dp)){
         Text("${prospectList.size} novos",Modifier.padding(horizontal=12.dp,vertical=8.dp),color=Ruby,fontWeight=FontWeight.Bold)
        }
       }
       Text("${salons.size} salões salvos  •  ${salons.count{it.status=="cliente"}} clientes",color=Muted,style=MaterialTheme.typography.bodySmall)
      }
      Surface(shape=RoundedCornerShape(24.dp),color=Color(0xFFE8EBED),modifier=Modifier.fillMaxWidth().height(440.dp)){
       SalonMap(salons=salons,prospects=prospectList,onSelect={selected=it},onProspect={chosenProspect=it},modifier=Modifier.fillMaxSize(),session=mapSession)
      }
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){
       Button(onClick={scan()},enabled=!searching,modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=8.dp,vertical=13.dp)){
        Text(if(searching)"Buscando..." else "Buscar salões")
       }
       OutlinedButton(onClick={captureHere()},modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=8.dp,vertical=13.dp)){
        Text("Salvar pelo GPS")
       }
      }
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){
       OutlinedButton(onClick={searchOsm()},enabled=!searching,modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=8.dp)){
        Text("Busca alternativa")
       }
       TextButton(onClick={cnpjDialog=true},modifier=Modifier.weight(1f)){Text("Consultar CNPJ")}
      }
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
       OutlinedButton(onClick={speak()},modifier=Modifier.weight(1f)){Text("🎙 Anotar por voz")}
       OutlinedButton(onClick={tab="Agenda"},modifier=Modifier.weight(1f)){Text("Agenda inteligente")}
      }
      if(searching||scanFinished){
       PremiumCard{
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(if(searching)"Rastreando salões..." else "Pesquisa concluída",color=Ink,fontWeight=FontWeight.Bold);Text("$scanProgress%",color=Ruby,fontWeight=FontWeight.Bold)}
        LinearProgressIndicator(progress={scanProgress/100f},modifier=Modifier.fillMaxWidth().height(12.dp),color=Ruby,trackColor=Color(0xFF42202D))
        Text(if(searching)"Consultando estabelecimentos e telefones disponíveis..." else "Resultados recebidos e salvos.",color=Muted,style=MaterialTheme.typography.bodySmall)
       }
      }
      Text("Mapa: OpenStreetMap  •  Salões salvos disponíveis offline",color=Muted,style=MaterialTheme.typography.labelSmall)
      if(prospectList.isNotEmpty())Text("Oportunidades encontradas",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
      prospectList.take(20).forEach{p->
       Card(onClick={chosenProspect=p;prospectPhotoUrl=null},shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFFF9F9FA)),elevation=CardDefaults.cardElevation(defaultElevation=2.dp),modifier=Modifier.fillMaxWidth().padding(vertical=5.dp)){
        Row(Modifier.padding(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){
         Surface(color=Color(0xFFF3E9EA),shape=RoundedCornerShape(10.dp),modifier=Modifier.size(72.dp)){
          Box(contentAlignment=androidx.compose.ui.Alignment.Center){Text("✂",style=MaterialTheme.typography.headlineMedium,color=Ruby)}
         }
         Column(Modifier.weight(1f)){
          Text(p.name,fontWeight=FontWeight.Bold)
          Text(p.address.ifBlank{"Endereço não informado"},style=MaterialTheme.typography.bodySmall)
          Text("Não visitado • Toque para detalhes",style=MaterialTheme.typography.bodySmall,color=Ruby)
         }
        }
       }
      }
     }
     OutlinedTextField(search,{search=it},label={Text("Buscar salão ou cidade")},singleLine=true,shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth())
     LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp)){items(listOf("todos","cliente","retorno","nao_visitado")){f->FilterChip(selected=filter==f,onClick={filter=f},label={Text(if(f=="todos")"Todos" else statusLabel(f))})}}
     Button(onClick={add=true}){Text("+ Novo salão")}
     val visibleSalons=salons.filter{(filter=="todos"||it.status==filter)&&("${it.name} ${it.city}".contains(search,true))}
     if(visibleSalons.isEmpty())EmptyPanel("Nenhum salão encontrado","Altere os filtros ou adicione um novo estabelecimento.")
     LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(visibleSalons){s->
      Card(onClick={selected=s},shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=CardBg),elevation=CardDefaults.cardElevation(defaultElevation=1.dp),modifier=Modifier.fillMaxWidth()){
       Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){
        Surface(shape=RoundedCornerShape(14.dp),color=Color(0xFFF7E8EB),modifier=Modifier.size(48.dp)){
         Box(contentAlignment=androidx.compose.ui.Alignment.Center){Text("✂",color=Ruby,style=MaterialTheme.typography.titleLarge)}
        }
        Column(Modifier.weight(1f)){
         Text(s.name,fontWeight=FontWeight.Bold,color=Ink)
         Text(statusLabel(s.status),color=Ruby,style=MaterialTheme.typography.bodySmall)
         Text(s.city.ifBlank{"Cidade não informada"},color=Muted,style=MaterialTheme.typography.bodySmall)
        }
        Text("›",color=Ruby,style=MaterialTheme.typography.headlineSmall)
       }
      }
     }}
    }
    "Agenda"->{
     Text("Próximos compromissos",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
     Text("${appointments.count{!it.completed}} agendamentos registrados",color=Muted)
     PremiumCard{
      Text("ROTAS POR CIDADE",color=Ruby,fontWeight=FontWeight.Bold)
      Text("Cadastre a próxima passagem. O recolhimento é organizado para o dia combinado da rota.",color=Muted)
      val cities=salons.map{it.city.trim()}.filter{it.isNotBlank()}.distinct().sorted()
      if(cities.isEmpty())Text("Cadastre a cidade dos salões para organizar suas rotas.",color=Muted)
      cities.forEach{city->
       val route=cityRoutes.find{it.city.equals(city,ignoreCase=true)}
       Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
        Column(Modifier.weight(1f)){
         Text(city,color=Ink,fontWeight=FontWeight.Bold)
         Text(route?.let{RouteRelationship.date(it.dateMillis)}?:"Próxima rota não definida",color=Muted)
        }
        TextButton(onClick={
         val cal=java.util.Calendar.getInstance()
         android.app.DatePickerDialog(this@MainActivity,{_,year,month,day->
          val chosen=java.util.Calendar.getInstance().apply{set(year,month,day,12,0,0);set(java.util.Calendar.MILLISECOND,0)}
          RouteRelationship.save(this@MainActivity,CityRoute(city,chosen.timeInMillis))
          cityRoutes=RouteRelationship.list(this@MainActivity)
         },cal.get(java.util.Calendar.YEAR),cal.get(java.util.Calendar.MONTH),cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }){Text(if(route==null)"Definir dia" else "Alterar")}
       }
       if(route!=null){
        salons.filter{it.city.equals(city,ignoreCase=true) && isAttendedClient(it) && !it.doNotContact}.take(12).forEach{s->
         Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
          Text(s.name,Modifier.weight(1f),maxLines=1,color=Ink)
          TextButton(onClick={openMessage(s,"rota")}){Text("Avisar rota")}
          if(sales.any{it.salonId==s.id && it.totalCents>payments.filter{p->p.saleId==it.id}.sumOf{p->p.amountCents}}){
           TextButton(onClick={openMessage(s,"pagamento")}){Text("Acerto")}
          }
         }
        }
       }
      }
     }
     PremiumCard{
      Text("ENRIQUECIMENTO DE TELEFONES",color=Ruby,fontWeight=FontWeight.Bold)
     Text("Consulta números comerciais públicos dos salões encontrados no Google. Pode gerar custos na API.",color=Muted)
     Button(onClick={enrichPublicPhones()},enabled=!enrichingPhones && !searching,modifier=Modifier.fillMaxWidth()){
      Text(if(enrichingPhones)"Consultando telefones..." else "Buscar telefones públicos que faltam")
     }
     Text("CLIENTES ATENDIDOS • CONTATO A CADA 7 DIAS",color=Ruby,fontWeight=FontWeight.Bold)
      Text("${salons.count{contactDue(it)}} salões precisam de contato agora",color=Muted)
      val pending=salons.filter{contactDue(it)}
       .sortedWith(compareByDescending<Salon>{contactDue(it)}.thenByDescending{contactDays(it)}).take(20)
      if(pending.isEmpty())Text("Todas as clientes atendidas estão em dia.",color=Muted)
      pending.forEach{s->
       Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
        Text(s.name,Modifier.weight(1f),color=Ink,maxLines=1)
        TextButton(onClick={openMessage(s)}){Text("Mensagem")}
        TextButton(onClick={selected=s}){Text("Ficha")}
       }
      }
     }
     if(appointments.isEmpty())EmptyPanel("Sua agenda está livre","Abra a ficha de um salão e toque em Agendar para criar uma visita ou retorno.")
     LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(appointments){a->
      val s=salons.find{it.id==a.salonId}
      PremiumCard{
       Text(s?.name?:"Salão",fontWeight=FontWeight.Bold,color=Ink)
       Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(a.startsAt)),color=Ruby)
       if(a.notes.isNotBlank())Text(a.notes,color=Muted)
       if(s!=null)TextButton(onClick={selected=s}){Text("Abrir ficha")}
      }
     }}
    }
    else->{
     PremiumCard{
      Text("SEGURANÇA DOS DADOS",color=Ruby,fontWeight=FontWeight.Bold)
      Text("Salve uma cópia completa dos salões, visitas, agenda, vendas e pagamentos. O arquivo contém dados de clientes; guarde-o com segurança.",color=Muted)
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
       Button(onClick={exportLauncher.launch("dalcotone-backup-${java.text.SimpleDateFormat("yyyyMMdd",Locale.US).format(java.util.Date())}.json")},modifier=Modifier.weight(1f)){Text("Exportar backup")}
       OutlinedButton(onClick={importLauncher.launch(arrayOf("application/json","text/plain"))},modifier=Modifier.weight(1f)){Text("Restaurar")}
      }
     }
     Text("Visão financeira",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
     PremiumCard{
      Text("TOTAL A RECEBER",color=Muted,style=MaterialTheme.typography.labelMedium)
      Text(money.format((sales.sumOf{it.totalCents}-payments.sumOf{it.amountCents})/100.0),color=Ruby,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
      Text("${sales.size} vendas registradas",color=Muted)
     }
     if(sales.isEmpty())EmptyPanel("Nenhuma venda registrada","Registre uma venda na ficha do salão para acompanhar recebimentos.")
     LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(sales){sale->
      val s=salons.find{it.id==sale.salonId}
      PremiumCard{
       Text(s?.name?:"Salão",fontWeight=FontWeight.Bold)
       Text("Saldo: ${money.format((sale.totalCents-payments.filter{it.saleId==sale.id}.sumOf{it.amountCents})/100.0)}",color=Ruby)
       if(s!=null)TextButton(onClick={selected=s}){Text("Abrir ficha")}
      }
     }}
    }
   }
  }
  }
  }
  if(messageSalon!=null) AlertDialog(
   onDismissRequest={messageSalon=null},
   title={Text("Mensagem Dal Cotone")},
   text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
    Text(messageSalon!!.name,color=Ink,fontWeight=FontWeight.Bold)
    Text("Revise e personalize. O envio é manual e não confirma contato automaticamente.",color=Muted)
    Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
     listOf("relacionamento" to "Carinho","reposicao" to "Reposição","rota" to "Rota","pagamento" to "Acerto").forEach{(kind,label)->
      FilterChip(selected=messageKind==kind,onClick={
       messageKind=kind
       val s=messageSalon!!
       messageDraft=RouteRelationship.message(s.name,kind,cityRoutes.find{it.city.equals(s.city,ignoreCase=true)})
      },label={Text(label)})
     }
    }
    OutlinedTextField(value=messageDraft,onValueChange={messageDraft=it},modifier=Modifier.fillMaxWidth(),minLines=4)
   }},
   confirmButton={Button(onClick={sendPreparedMessage()}){Text("Abrir no WhatsApp")}},
   dismissButton={TextButton(onClick={messageSalon=null}){Text("Cancelar")}}
  )
  if(voiceDraft!=null) AlertDialog(
   onDismissRequest={voiceDraft=null},
   title={Text("Anotação por voz")},
   text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
    Text("Revise o texto antes de registrar. Nenhuma ação será executada automaticamente.",color=Muted)
    OutlinedTextField(value=voiceDraft?:"",onValueChange={voiceDraft=it},modifier=Modifier.fillMaxWidth(),minLines=3)
    Text("Associar ao salão:",color=Ink,fontWeight=FontWeight.Bold)
    if(salons.isEmpty())Text("Cadastre um salão antes de salvar.",color=Muted)
    else androidx.compose.foundation.lazy.LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){
     items(salons.take(30)){s->
      FilterChip(selected=voiceSalonId==s.id,onClick={voiceSalonId=s.id},label={Text(s.name,maxLines=1)})
     }
    }
   }},
   confirmButton={Button(onClick={
    val draft=voiceDraft.orEmpty().trim()
    if(draft.isNotBlank()){
     val s=salons.find{it.id==voiceSalonId}
     if(s!=null)scope.launch{dao.saveSalon(s.copy(notes=(s.notes+"\n"+draft).trim()));message="Anotação salva na ficha de ${s.name}."}
     else message="Selecione um salão para salvar a anotação."
    }
    voiceDraft=null
   }){Text("Confirmar anotação")}},
   dismissButton={TextButton(onClick={voiceDraft=null}){Text("Cancelar")}}
  )
  if(phoneEditor!=null) AlertDialog(
   onDismissRequest={phoneEditor=null},
   title={Text("WhatsApp do salão")},
   text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
    Text(phoneEditor!!.name,color=Ink,fontWeight=FontWeight.Bold)
    Text("Informe o DDD e o número. O cadastro não confirma se a linha possui WhatsApp.",color=Muted)
    OutlinedTextField(value=phoneDraft,onValueChange={phoneDraft=it},label={Text("Telefone com DDD")},singleLine=true,modifier=Modifier.fillMaxWidth(),keyboardOptions=KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Phone))
   }},
   confirmButton={Button(onClick={
    val s=phoneEditor!!
    val digits=phoneDraft.filter{it.isDigit()}
    if(digits.length !in 10..11 && !(digits.length in 12..13 && digits.startsWith("55"))){
     message="Informe um telefone válido com DDD."
    }else{
     val updated=s.copy(phone=phoneDraft.trim())
     selected=if(selected?.id==s.id)updated else selected
     scope.launch{dao.saveSalon(updated)}
     phoneEditor=null
     whatsapp(updated.phone)
    }
   }){Text("Salvar e abrir WhatsApp")}},
   dismissButton={TextButton(onClick={phoneEditor=null}){Text("Cancelar")}}
  )
  if(cnpjDialog) AlertDialog(
   onDismissRequest={cnpjDialog=false},
   title={Text("Consultar empresa pelo CNPJ")},
   text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
    OutlinedTextField(cnpjText,{cnpjText=it},label={Text("CNPJ (14 dígitos)")})
    Text("Consulta online por dados públicos. Não encontra todos os salões por proximidade.",style=MaterialTheme.typography.bodySmall)
    cnpjResult?.let{b->Text(b.tradeName.ifBlank{b.legalName});Text("${b.city} / ${b.state} • ${b.status}")}
   }},
   confirmButton={TextButton(onClick={
    scope.launch{
     try{cnpjResult=cnpjLookup.lookup(cnpjText);message="Consulta CNPJ concluída."}
     catch(e:Exception){message=e.message?:"Falha na consulta CNPJ"}
    }
   }){Text("Consultar")}},
   dismissButton={TextButton(onClick={cnpjDialog=false;cnpjResult=null}){Text("Fechar")}}
  )
  if(chosenProspect!=null) AlertDialog(
   onDismissRequest={chosenProspect=null},
   title={Text(chosenProspect!!.name)},
   text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
    if(prospectPhotoUrl!=null)AsyncImage(model=prospectPhotoUrl,contentDescription="Foto do estabelecimento fornecida pelo Google",modifier=Modifier.fillMaxWidth().height(150.dp),contentScale=ContentScale.Crop)
    Text(chosenProspect!!.address)
    Text(chosenProspect!!.phone?.let{"Telefone: $it"}?:"Telefone não consultado ou indisponível")
    if(detailsLoading)CircularProgressIndicator()
    if(chosenProspect!!.placeId.startsWith("osm:"))Text("Fonte OpenStreetMap: telefone e foto podem não estar disponíveis.")
    else TextButton(enabled=!detailsLoading,onClick={
     val p=chosenProspect!!
     scope.launch{
      detailsLoading=true
      try{val detail=places.details(p);chosenProspect=detail;prospectPhotoUrl=detail.photoName?.let{places.photoUrl(it)}}
      catch(e:Exception){message=e.message?:"Detalhes indisponíveis"}
      finally{detailsLoading=false}
     }
    }){Text("Consultar telefone e foto (Google • pode gerar cobrança)")}
    chosenProspect!!.phone?.takeIf{it.isNotBlank()}?.let{tel->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
     Button(onClick={whatsapp(tel)}){Text("WhatsApp")}
     OutlinedButton(onClick={startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+tel.filter{it.isDigit()})))}){Text("Ligar")}
    }}
    chosenProspect!!.let{p->TextButton(onClick={
     val query=Uri.encode(if(p.latitude!=null&&p.longitude!=null)"${p.latitude},${p.longitude}" else p.address)
     startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/search/?api=1&query="+query)))
    }){Text("Traçar rota")}}
    Text("Oportunidade encontrada online • ainda não cadastrada")
   }},
   confirmButton={TextButton(onClick={
    val p=chosenProspect!!
    scope.launch{
     dao.saveSalon(Salon(name=p.name,address=p.address,placeId=p.placeId,latitude=p.latitude,longitude=p.longitude,phone=p.phone?:"",status="nao_visitado"))
     prospectList=prospectList.filterNot{it.placeId==p.placeId}
     message="Salão salvo como Não visitado."
    }
    chosenProspect=null
   }){Text("Salvar como Não visitado")}},
   dismissButton={TextButton(onClick={chosenProspect=null}){Text("Cancelar")}}
  )
  if(quickCapture) AlertDialog(
   onDismissRequest={quickCapture=false},
   title={Text("Salvar salão encontrado")},
   text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
    Text("Ponto salvo pelo GPS • status automático: Não visitado",color=Ruby)
    Text("Latitude: ${capturedLatitude ?: "-"}  Longitude: ${capturedLongitude ?: "-"}",style=MaterialTheme.typography.bodySmall)
    Text("Precisão aproximada: ${capturedAccuracy?.toInt() ?: 0} metros",style=MaterialTheme.typography.bodySmall)
    OutlinedTextField(name,{name=it},label={Text("Nome do salão *")},modifier=Modifier.fillMaxWidth())
    OutlinedTextField(address,{address=it},label={Text("Endereço (opcional, offline)")},modifier=Modifier.fillMaxWidth())
   }},
   confirmButton={TextButton(onClick={
    val lat=capturedLatitude;val lon=capturedLongitude
    if(name.isBlank()||lat==null||lon==null){message="Informe o nome e capture o GPS."}
    else{
     val salon=Salon(name=name.trim(),address=address.trim(),latitude=lat,longitude=lon,status="nao_visitado")
     scope.launch{dao.saveSalon(salon);message="Salão salvo offline como Não visitado."}
     quickCapture=false;name="";address="";capturedLatitude=null;capturedLongitude=null;capturedAccuracy=null
    }
   }){Text("Salvar ponto")}},
   dismissButton={TextButton(onClick={quickCapture=false}){Text("Cancelar")}}
  )
  if(add) AlertDialog(onDismissRequest={add=false},title={Text("Novo salão")},text={Column{OutlinedTextField(name,{name=it},label={Text("Nome")});OutlinedTextField(city,{city=it},label={Text("Cidade")});OutlinedTextField(address,{address=it},label={Text("Endereço")});OutlinedTextField(phone,{phone=it},label={Text("Telefone")})}},confirmButton={TextButton(onClick={if(name.isNotBlank()){scope.launch{dao.saveSalon(Salon(name=name,city=city,address=address,phone=phone))};add=false;name="";city="";address="";phone=""}}){Text("Salvar")}},dismissButton={TextButton(onClick={add=false}){Text("Cancelar")}})
  if(editOutcome&&selected!=null)AlertDialog(onDismissRequest={editOutcome=false},title={Text("Registrar visita • Se tornou cliente?")},text={Column{listOf("cliente","retorno","recusou","inativo").forEach{f->Row{RadioButton(selected=outcome==f,onClick={outcome=f});Text(f)}};OutlinedTextField(notes,{notes=it},label={Text("Observações")})}},confirmButton={TextButton(onClick={val s=selected!!;scope.launch{dao.addVisit(Visit(salonId=s.id,outcome=outcome,notes=notes));val updated=s.copy(status=outcome,lastContactAt=System.currentTimeMillis(),notes=notes,doNotContact=outcome=="recusou");dao.saveSalon(updated);selected=updated};editOutcome=false}){Text("Registrar")}})
  if(schedule&&selected!=null)AlertDialog(onDismissRequest={schedule=false},title={Text("Agendar retorno")},text={Column{Text("Data e hora: AAAA-MM-DD HH:MM");OutlinedTextField(dateTime,{dateTime=it});Text("Lembrete 30 minutos antes, offline")}},confirmButton={TextButton(onClick={try{val formatter=java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");val at=java.time.LocalDateTime.parse(dateTime,formatter).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();val s=selected!!;val a=Appointment(salonId=s.id,startsAt=at);scope.launch{dao.addAppointment(a)};val exact=Reminders.schedule(this,a,s.name);message=if(exact)"Agendado com alarme exato." else "Agendado; alerta pode atrasar conforme Android.";if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)permission.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS));schedule=false}catch(e:Exception){message="Data inválida: use AAAA-MM-DD HH:MM"}}){Text("Salvar")}})
  if(sell&&selected!=null)AlertDialog(onDismissRequest={sell=false},title={Text("Nova venda")},text={Column{OutlinedTextField(product,{product=it},label={Text("Produto")});OutlinedTextField(qty,{qty=it},label={Text("Quantidade")});OutlinedTextField(price,{price=it},label={Text("Preço unitário (R$)")})}},confirmButton={TextButton(onClick={val q=qty.toIntOrNull();val cents=(price.replace(",",".").toBigDecimalOrNull()?.multiply(java.math.BigDecimal(100)))?.toLong();if(q!=null&&q>0&&cents!=null&&cents>0&&product.isNotBlank()){val sale=Sale(salonId=selected!!.id,dueAt=null,totalCents=q*cents);scope.launch{dao.addSale(sale);dao.addItem(SaleItem(saleId=sale.id,product=product,quantity=q,unitPriceCents=cents));val current=selected!!;val updated=current.copy(status="cliente");dao.saveSalon(updated);selected=updated;message="Venda registrada. Salão classificado automaticamente como Cliente."};sell=false;product="";price=""}else message="Confira os valores."}){Text("Salvar")}})
  if(paymentSale!=null)AlertDialog(
   onDismissRequest={paymentSale=null},
   title={Text("Registrar pagamento parcial")},
   text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
    val sale=paymentSale!!
    val alreadyPaid=payments.filter{it.saleId==sale.id}.sumOf{it.amountCents}
    Text("Saldo atual: ${money.format((sale.totalCents-alreadyPaid)/100.0)}",fontWeight=FontWeight.Bold,color=Ruby)
    OutlinedTextField(amount,{amount=it},label={Text("Valor recebido (R$)")},modifier=Modifier.fillMaxWidth(),singleLine=true)
    Text("Método de pagamento",fontWeight=FontWeight.Medium)
    listOf("pix" to "Pix","dinheiro" to "Dinheiro","cartao" to "Cartão","transferencia" to "Transferência").forEach{(value,label)->
     Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
      RadioButton(selected=paymentMethod==value,onClick={paymentMethod=value})
      Text(label,modifier=Modifier.clickable{paymentMethod=value})
     }
    }
    val cents=amount.replace(",",".").toBigDecimalOrNull()?.multiply(java.math.BigDecimal(100))?.toLong()
    if(cents!=null&&cents>0)Text("Saldo após pagamento: ${money.format((sale.totalCents-alreadyPaid-cents)/100.0)}")
   }},
   confirmButton={TextButton(onClick={
    val cents=amount.replace(",",".").toBigDecimalOrNull()?.multiply(java.math.BigDecimal(100))?.toLong()
    val sale=paymentSale!!
    val balance=sale.totalCents-payments.filter{it.saleId==sale.id}.sumOf{it.amountCents}
    if(cents==null||cents<=0||cents>balance){message="Informe um valor maior que zero e até o saldo devedor."}
    else{
     val method=paymentMethod
     scope.launch{
      try{dao.pay(sale.id,cents,method);message="Pagamento salvo. Saldo atualizado automaticamente."}
      catch(e:Exception){message=e.message?:"Erro ao registrar pagamento"}
     }
     paymentSale=null;amount="";paymentMethod="pix"
    }
   }){Text("Confirmar pagamento")}},
   dismissButton={TextButton(onClick={paymentSale=null}){Text("Cancelar")}}
  )
 }
}
