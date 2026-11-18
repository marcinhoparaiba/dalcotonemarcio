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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);Reminders.channel(this)
  setContent { MaterialTheme(colorScheme=lightColorScheme(primary=Ruby,background=Color.White,surface=Color.White)) { CRMApp() } }
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
  var name by remember{mutableStateOf("")};var city by remember{mutableStateOf("")};var address by remember{mutableStateOf("")};var phone by remember{mutableStateOf("")}
  var message by remember{mutableStateOf("")}
  var search by remember{mutableStateOf("")}
  var filter by remember{mutableStateOf("todos")}
  var editOutcome by remember{mutableStateOf(false)}
  var outcome by remember{mutableStateOf("cliente")}
  var notes by remember{mutableStateOf("")}
  var schedule by remember{mutableStateOf(false)}
  var dateTime by remember{mutableStateOf("")}
  var sell by remember{mutableStateOf(false)}
  var product by remember{mutableStateOf("")};var qty by remember{mutableStateOf("1")};var price by remember{mutableStateOf("")}
  var paymentSale by remember{mutableStateOf<Sale?>(null)}
  var amount by remember{mutableStateOf("")}
  var prospectList by remember{mutableStateOf<List<Prospect>>(emptyList())}
  val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){}
  val locationPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){}
  val locationManager=remember{getSystemService(LOCATION_SERVICE) as android.location.LocationManager}
  val places=remember{PlacesRepository()}
  val money=remember{NumberFormat.getCurrencyInstance(Locale("pt","BR"))}
  fun navigate(s:Salon){val q=Uri.encode(listOf(s.address,s.city,"Paraíba Brasil").joinToString(" "));startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/search/?api=1&query=$q")))}
  fun scan(){
   if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
    locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION));message="Autorize a localização e toque novamente.";return
   }
   val provider=when{locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)->android.location.LocationManager.GPS_PROVIDER;locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)->android.location.LocationManager.NETWORK_PROVIDER;else->null}
   val location=provider?.let{locationManager.getLastKnownLocation(it)}
   if(location==null){message="Sem posição recente. Ative o GPS e tente novamente.";return}
   scope.launch{try{val known=salons.mapNotNull{it.placeId}.toSet();prospectList=places.nearby(location.latitude,location.longitude).filter{it.placeId !in known};message="${prospectList.size} oportunidades encontradas."}catch(e:Exception){message=e.message?:"Busca indisponível"}}
  }
  Column(Modifier.fillMaxSize().background(Color.White).padding(16.dp)){
   Text("DAL COTONE",color=Ruby,fontWeight=FontWeight.Bold)
   Text("CRM Comercial",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
   Spacer(Modifier.height(12.dp))
   Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("Mapa","Clientes","Agenda","Financeiro").forEach{t->FilterChip(selected=tab==t,onClick={tab=t;selected=null},label={Text(t)})}}
   if(message.isNotBlank())Text(message,color=Ruby,style=MaterialTheme.typography.bodySmall)
   if(selected!=null){
    val s=selected!!
    TextButton(onClick={selected=null}){Text("← Voltar")}
    Text(s.name,style=MaterialTheme.typography.titleLarge)
    Text("${s.city} • ${s.status}")
    Text(s.address)
    Row{TextButton(onClick={navigate(s)}){Text("Abrir rota")};TextButton(onClick={if(s.phone.isNotBlank())startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://wa.me/"+s.phone.filter{it.isDigit()})))}){Text("WhatsApp")}}
    val ownSales=sales.filter{it.salonId==s.id}
    val outstanding=ownSales.sumOf{sale->sale.totalCents-payments.filter{it.saleId==sale.id}.sumOf{it.amountCents}}
    Text("Saldo a receber: ${money.format(outstanding/100.0)}",fontWeight=FontWeight.Bold,color=Ruby)
    Row{TextButton(onClick={editOutcome=true}){Text("Registrar visita")};TextButton(onClick={schedule=true}){Text("Agendar")};TextButton(onClick={sell=true}){Text("Nova venda")}}
    Text("Últimas compras",fontWeight=FontWeight.Bold)
    LazyColumn{items(ownSales){sale->
     Card(Modifier.fillMaxWidth().padding(vertical=5.dp)){Column(Modifier.padding(12.dp)){
      Text("Venda: ${money.format(sale.totalCents/100.0)}")
      items.filter{it.saleId==sale.id}.forEach{Text("${it.quantity}x ${it.product}")}
      val due=sale.totalCents-payments.filter{it.saleId==sale.id}.sumOf{it.amountCents}
      Text("Falta: ${money.format(due/100.0)}")
      if(due>0)TextButton(onClick={paymentSale=sale}){Text("Registrar pagamento")}
     }}
    }}
   }else when(tab){
    "Mapa","Clientes"->{
     if(tab=="Mapa"){
      Text("Radar comercial • 10 km",fontWeight=FontWeight.Bold)
      Text("Clientes salvos funcionam offline. Novos salões exigem internet.",style=MaterialTheme.typography.bodySmall)
      Button(onClick={::scan}){Text("Buscar salões próximos (online)")}
      prospectList.take(20).forEach{p->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(p.name,Modifier.weight(1f));TextButton(onClick={scope.launch{dao.saveSalon(Salon(name=p.name,address=p.address,placeId=p.placeId,latitude=p.latitude,longitude=p.longitude));prospectList=prospectList.filterNot{it.placeId==p.placeId}}}){Text("Salvar")}}}
     }
     OutlinedTextField(search,{search=it},label={Text("Buscar salão ou cidade")},modifier=Modifier.fillMaxWidth())
     Row{listOf("todos","cliente","retorno","nao_visitado").forEach{f->FilterChip(selected=filter==f,onClick={filter=f},label={Text(f)},modifier=Modifier.padding(end=3.dp))}}
     Button(onClick={add=true}){Text("+ Novo salão")}
     LazyColumn{items(salons.filter{(filter=="todos"||it.status==filter)&&("${it.name} ${it.city}".contains(search,true))}){s->Card(onClick={selected=s},modifier=Modifier.fillMaxWidth().padding(vertical=5.dp)){Column(Modifier.padding(14.dp)){Text(s.name,fontWeight=FontWeight.Bold);Text("${s.city} • ${s.status}",style=MaterialTheme.typography.bodySmall)}}}}
    }
    "Agenda"->LazyColumn{items(appointments){a->val s=salons.find{it.id==a.salonId};Card(Modifier.fillMaxWidth().padding(vertical=5.dp)){Column(Modifier.padding(12.dp)){Text(s?.name?:"Salão");Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(a.startsAt)));Text(a.notes)}}}}
    else->{
     Text("Total a receber",style=MaterialTheme.typography.titleMedium)
     Text(money.format(sales.sumOf{it.totalCents}.minus(payments.sumOf{it.amountCents})/100.0),color=Ruby,style=MaterialTheme.typography.headlineMedium)
     LazyColumn{items(sales){sale->val s=salons.find{it.id==sale.salonId};Text("${s?.name}: ${money.format((sale.totalCents-payments.filter{it.saleId==sale.id}.sumOf{it.amountCents})/100.0)}")}}
    }
   }
  }
  if(add) AlertDialog(onDismissRequest={add=false},title={Text("Novo salão")},text={Column{OutlinedTextField(name,{name=it},label={Text("Nome")});OutlinedTextField(city,{city=it},label={Text("Cidade")});OutlinedTextField(address,{address=it},label={Text("Endereço")});OutlinedTextField(phone,{phone=it},label={Text("Telefone")})}},confirmButton={TextButton(onClick={if(name.isNotBlank()){scope.launch{dao.saveSalon(Salon(name=name,city=city,address=address,phone=phone))};add=false;name="";city="";address="";phone=""}}){Text("Salvar")}},dismissButton={TextButton(onClick={add=false}){Text("Cancelar")}})
  if(editOutcome&&selected!=null)AlertDialog(onDismissRequest={editOutcome=false},title={Text("Registrar atendimento")},text={Column{listOf("cliente","retorno","recusou","inativo").forEach{f->Row{RadioButton(selected=outcome==f,onClick={outcome=f});Text(f)}};OutlinedTextField(notes,{notes=it},label={Text("Observações")})}},confirmButton={TextButton(onClick={val s=selected!!;scope.launch{dao.addVisit(Visit(salonId=s.id,outcome=outcome,notes=notes));val updated=s.copy(status=outcome,lastContactAt=System.currentTimeMillis(),notes=notes,doNotContact=outcome=="recusou");dao.saveSalon(updated);selected=updated};editOutcome=false}){Text("Registrar")}})
  if(schedule&&selected!=null)AlertDialog(onDismissRequest={schedule=false},title={Text("Agendar retorno")},text={Column{Text("Data e hora: AAAA-MM-DD HH:MM");OutlinedTextField(dateTime,{dateTime=it});Text("Lembrete 30 minutos antes, offline")}},confirmButton={TextButton(onClick={try{val formatter=java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");val at=java.time.LocalDateTime.parse(dateTime,formatter).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();val s=selected!!;val a=Appointment(salonId=s.id,startsAt=at);scope.launch{dao.addAppointment(a)};val exact=Reminders.schedule(this,a,s.name);message=if(exact)"Agendado com alarme exato." else "Agendado; alerta pode atrasar conforme Android.";if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)permission.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS));schedule=false}catch(e:Exception){message="Data inválida: use AAAA-MM-DD HH:MM"}}){Text("Salvar")}})
  if(sell&&selected!=null)AlertDialog(onDismissRequest={sell=false},title={Text("Nova venda")},text={Column{OutlinedTextField(product,{product=it},label={Text("Produto")});OutlinedTextField(qty,{qty=it},label={Text("Quantidade")});OutlinedTextField(price,{price=it},label={Text("Preço unitário (R$)")})}},confirmButton={TextButton(onClick={val q=qty.toIntOrNull();val cents=(price.replace(",",".").toBigDecimalOrNull()?.multiply(java.math.BigDecimal(100)))?.toLong();if(q!=null&&q>0&&cents!=null&&cents>0&&product.isNotBlank()){val sale=Sale(salonId=selected!!.id,dueAt=null,totalCents=q*cents);scope.launch{dao.addSale(sale);dao.addItem(SaleItem(saleId=sale.id,product=product,quantity=q,unitPriceCents=cents))};sell=false;product="";price=""}else message="Confira os valores."}){Text("Salvar")}})
  if(paymentSale!=null)AlertDialog(onDismissRequest={paymentSale=null},title={Text("Pagamento parcial")},text={OutlinedTextField(amount,{amount=it},label={Text("Valor recebido (R$) ")})},confirmButton={TextButton(onClick={val cents=amount.replace(",",".").toBigDecimalOrNull()?.multiply(java.math.BigDecimal(100))?.toLong();val sale=paymentSale!!;if(cents!=null){scope.launch{try{dao.pay(sale.id,cents,"pix");message="Pagamento registrado"}catch(e:Exception){message=e.message?:"Erro"}};paymentSale=null;amount=""}}){Text("Registrar Pix")}})
 }
}
