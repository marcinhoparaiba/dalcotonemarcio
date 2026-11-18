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
import java.text.NumberFormat
import java.util.Locale

private val Ruby=Color(0xFF9B111E)
private val Ink=Color(0xFF22232A)
private val Muted=Color(0xFF6D707B)
private val CanvasBg=Color(0xFFF7F7FA)
private val CardBg=Color.White
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
  setContent { MaterialTheme(colorScheme=lightColorScheme(primary=Ruby,onPrimary=Color.White,background=CanvasBg,surface=CardBg,onSurface=Ink,secondary=Ink,surfaceVariant=Color(0xFFF0EDF0))) { CRMApp() } }
 }
 @Composable private fun CRMApp(){
  val dao=remember{CrmDatabase.get(this).dao()}
  val salons by dao.salons().collectAsStateWithLifecycle(initialValue=emptyList())
  val appointments by dao.appointments().collectAsStateWithLifecycle(initialValue=emptyList())
  val sales by dao.sales().collectAsStateWithLifecycle(initialValue=emptyList())
  val payments by dao.payments().collectAsStateWithLifecycle(initialValue=emptyList())
  val items by dao.items().collectAsStateWithLifecycle(initialValue=emptyList())
  val scope=rememberCoroutineScope()
  var tab by remember{mutableStateOf("Mapa")}
  var selected by remember{mutableStateOf<Salon?>(null)}
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
  val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){}
  val locationPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){}
  val locationManager=remember{getSystemService(LOCATION_SERVICE) as android.location.LocationManager}
  val places=remember{PlacesRepository()}
  val osm=remember{OpenStreetMapRepository()}
  val cnpjLookup=remember{CnpjLookupRepository()}
  val money=remember{NumberFormat.getCurrencyInstance(Locale("pt","BR"))}
  fun whatsapp(raw:String){
   val digits=raw.filter{it.isDigit()}
   val international=when{digits.length==10||digits.length==11->"55"+digits;digits.length in 12..13&&digits.startsWith("55")->digits;else->""}
   if(international.isBlank()){message="Número inválido para WhatsApp. Confira o DDD.";return}
   startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://wa.me/"+international)))
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
    message="Buscando salões próximos no Google..."
    try{
     val known=salons.mapNotNull{it.placeId}.toSet()
     val found=places.nearby(location.latitude,location.longitude).filter{it.placeId !in known}
     val saved=persistDiscovery(found)
     prospectList=emptyList()
     message="${found.size} salões encontrados no Google • $saved novos salvos offline."
    }catch(e:Exception){message="Não foi possível consultar o Google: "+(e.message?:"erro de conexão")}
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
  Column(Modifier.fillMaxSize().background(CanvasBg).padding(horizontal=16.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
   Surface(shape=RoundedCornerShape(20.dp),color=Ink,modifier=Modifier.fillMaxWidth()){
    Row(Modifier.padding(horizontal=18.dp,vertical=14.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
     Surface(shape=RoundedCornerShape(12.dp),color=Ruby,modifier=Modifier.size(44.dp)){
      Box(contentAlignment=androidx.compose.ui.Alignment.Center){Text("DC",color=Color.White,fontWeight=FontWeight.ExtraBold)}
     }
     Column(Modifier.weight(1f)){
      Text("DAL COTONE",color=Color(0xFFFFB6C0),style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
      Text(when(tab){"Mapa"->"Radar comercial";"Cidades"->"Territórios";"Clientes"->"Minha carteira";"Rota"->"Planejar visitas";"Agenda"->"Minha agenda";else->"Financeiro"},color=Color.White,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
     }
     Column(horizontalAlignment=androidx.compose.ui.Alignment.End){
      Text("${salons.size}",color=Color.White,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
      Text("salões",color=Color(0xFFD6D3D9),style=MaterialTheme.typography.labelSmall)
     }
    }
   }
   val navigationItems=listOf("Mapa" to "◉","Cidades" to "⌖","Clientes" to "♙","Rota" to "➤","Agenda" to "▦","Financeiro" to "R$")
   LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(vertical=2.dp)){
    items(navigationItems){(target,symbol)->
     val active=tab==target
     Surface(onClick={tab=target;selected=null;message=""},shape=RoundedCornerShape(15.dp),
      color=if(active)Ruby else Color.White,
      border=if(active)null else androidx.compose.foundation.BorderStroke(1.dp,Color(0xFFE6E3E8))){
      Row(Modifier.padding(horizontal=14.dp,vertical=11.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
       Text(symbol,color=if(active)Color.White else Ruby,fontWeight=FontWeight.Bold)
       Text(target,color=if(active)Color.White else Ink,fontWeight=if(active)FontWeight.Bold else FontWeight.Medium)
      }
     }
    }
   }
   if(message.isNotBlank())Surface(color=Color(0xFFF5E9EC),shape=RoundedCornerShape(12.dp)){
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
     Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
      Button(onClick={navigate(s)}){Text("Abrir rota")}
      if(s.phone.isNotBlank())OutlinedButton(onClick={whatsapp(s.phone)}){Text("Consultar WhatsApp")}
     }
    }
    val ownSales=sales.filter{it.salonId==s.id}
    val outstanding=ownSales.sumOf{sale->sale.totalCents-payments.filter{it.saleId==sale.id}.sumOf{it.amountCents}}
    PremiumCard{
     Text("FINANCEIRO DO SALÃO",style=MaterialTheme.typography.labelMedium,color=Muted)
     Text(money.format(outstanding/100.0),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold,color=Ruby)
     Text("Saldo a receber",color=Muted)
    }
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
    "Cidades"->{
     Text("Salões por cidade",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
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
          Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
           TextButton(onClick={selected=s}){Text("Ficha")}
           TextButton(onClick={navigate(s)}){Text("Rota")}
           if(s.phone.isNotBlank())TextButton(onClick={whatsapp(s.phone)}){Text("WhatsApp")}
          }
         }
        }
       }
      }
     }
    }
    "Rota"->{
     Text("Criar roteiro",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
     Text("Rotas de clientes, prospecção ou mistas.",style=MaterialTheme.typography.bodySmall)
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
      Surface(color=Color(0xFFF9F2F3),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth()){
       Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
        Text("RADAR DE OPORTUNIDADES",color=Ruby,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelMedium)
        Text("${prospectList.size} salões para prospectar",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge)
        Text("Explore o mapa, abra um salão e entre em contato.",style=MaterialTheme.typography.bodySmall)
       }
      }
      SalonMap(salons=salons,prospects=prospectList,onSelect={selected=it},onProspect={chosenProspect=it},modifier=Modifier.fillMaxWidth().height(340.dp))
      Text("Mapa OpenStreetMap: ruas precisam de internet na primeira visualização; pontos cadastrados ficam salvos offline.",style=MaterialTheme.typography.bodySmall)
      Text("Clientes salvos funcionam offline. Novos salões exigem internet.",style=MaterialTheme.typography.bodySmall)
      Button(onClick={captureHere()},modifier=Modifier.fillMaxWidth()){Text("📍 Salvar salão aqui (GPS offline)")}
      OutlinedButton(onClick={scan()},enabled=!searching,modifier=Modifier.fillMaxWidth()){Text(if(searching)"Aguarde a pesquisa..." else "Buscar no Google Places (online)")}
      OutlinedButton(onClick={searchOsm()},enabled=!searching,modifier=Modifier.fillMaxWidth()){Text(if(searching)"Pesquisando..." else "Buscar no OpenStreetMap (online)")}
      TextButton(onClick={cnpjDialog=true}){Text("Consultar CNPJ de um salão (online)")}
      Text("Fontes: Google Places e OpenStreetMap para descoberta; Receita/CNPJ para consulta pontual; CRM SQLite para visitas e clientes.",style=MaterialTheme.typography.bodySmall)
      Text("Salões salvos no celular: ${salons.size} • os marcadores permanecem no mapa após fechar o app.",style=MaterialTheme.typography.bodySmall)
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
     Text("Próximos compromissos",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
     Text("${appointments.count{!it.completed}} agendamentos registrados",color=Muted)
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
