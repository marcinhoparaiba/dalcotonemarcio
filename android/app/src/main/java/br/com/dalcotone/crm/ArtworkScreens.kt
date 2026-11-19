package br.com.dalcotone.crm

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ArtBlack=Color(0xFF09090C)
private val ArtPanel=Color(0xFF18171D)
private val ArtRed=Color(0xFFF10A23)
private val ArtInk=Color(0xFFF8F7F9)
private val ArtMuted=Color(0xFFBFB8C1)
private val ArtGreen=Color(0xFF09C650)

@Composable private fun Artwork(name:String,modifier:Modifier=Modifier){
 val context=LocalContext.current
 val bitmap=remember(name){
  runCatching{ context.assets.open(name).use{BitmapFactory.decodeStream(it)?.asImageBitmap()} }.getOrNull()
 }
 Box(modifier.background(ArtBlack)){
  if(bitmap!=null) Canvas(Modifier.fillMaxSize()){
   drawImage(
    image=bitmap,
    srcOffset=IntOffset.Zero,
    srcSize=IntSize(bitmap.width,bitmap.height),
    dstOffset=IntOffset.Zero,
    dstSize=IntSize(size.width.toInt().coerceAtLeast(1),size.height.toInt().coerceAtLeast(1))
   )
  }
 }
}

@Composable fun ArtworkHome(
 salons:Int,contacts:Int,appointments:Int,onNavigate:(String)->Unit
){
 BoxWithConstraints(Modifier.fillMaxSize().background(ArtBlack)){
  Artwork("dalcotone_inicio.png",Modifier.fillMaxSize())
  val w=maxWidth
  val h=maxHeight
  // The image itself supplies the approved logo, salon photography and six buttons.
  // These independently clickable areas route to the actual CRM modules.
  val destinations=listOf("Rastrear","Rota","Clientes","Financeiro","Clientes","Financeiro")
  destinations.forEachIndexed{index,destination->
   val column=index%3
   val row=index/3
   Box(Modifier.offset(x=w*(0.052f+0.328f*column),y=h*(0.352f+0.139f*row))
    .width(w*0.30f).height(h*0.131f)
    .clickable{onNavigate(destination)})
  }
  // Replace the COMPLETE fake statistics region: never show 328/187/42 or fake growth.
  Surface(
   modifier=Modifier.offset(x=w*0.049f,y=h*0.648f).width(w*0.902f).height(h*0.225f),
   shape=RoundedCornerShape(18.dp),
   color=ArtPanel,
   border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF52404A))
  ){
   Row{
    Box(Modifier.width(4.dp).fillMaxHeight().background(ArtRed))
    Column(Modifier.fillMaxSize().padding(horizontal=15.dp,vertical=10.dp),verticalArrangement=Arrangement.SpaceEvenly){
     Text("Resumo da Semana",color=ArtInk,fontWeight=FontWeight.ExtraBold,fontSize=17.sp)
     listOf("Salões mapeados" to salons,"Contatos nos últimos 7 dias" to contacts,"Visitas agendadas" to appointments).forEach{(label,value)->
      Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
       Text(label,color=ArtInk,fontSize=12.sp,modifier=Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis)
       Text(value.toString(),color=ArtInk,fontSize=17.sp,fontWeight=FontWeight.ExtraBold)
      }
     }
    }
   }
  }
  // Five navigation slots match the original artwork.
  listOf("Início","Mapa","Clientes","Rota","Mais").forEachIndexed{index,destination->
   Box(Modifier.offset(x=w*(index/5f),y=h*0.889f).width(w*0.2f).height(h*0.111f)
    .clickable{onNavigate(destination)})
  }
  Box(Modifier.offset(x=w*0.01f,y=h*0.018f).width(w*0.16f).height(h*0.077f)
   .clickable{onNavigate("Mais")})
 }
}

@Composable fun ArtworkRadar(
 city:String,onCity:(String)->Unit,
 neighborhood:String,onNeighborhood:(String)->Unit,
 radiusKm:Float,onRadius:(Float)->Unit,
 searching:Boolean,progress:Int,message:String,resultsCount:Int,
 onBack:()->Unit,onScan:()->Unit,onMap:()->Unit,onResults:()->Unit
){
 BoxWithConstraints(Modifier.fillMaxSize().background(ArtBlack)){
  Artwork("dalcotone_rastrear_saloes.png",Modifier.fillMaxSize())
  val w=maxWidth
  val h=maxHeight
  // Cover illustration-only form values; native controls are editable and accessible.
  Column(Modifier.offset(x=w*0.053f,y=h*0.223f).width(w*0.894f)
   .height(h*0.429f).background(ArtBlack).padding(horizontal=7.dp),
   verticalArrangement=Arrangement.spacedBy(5.dp)){
   Text("Cidade",color=ArtInk,fontWeight=FontWeight.Bold,fontSize=13.sp)
   OutlinedTextField(value=city,onValueChange=onCity,modifier=Modifier.fillMaxWidth().height(51.dp),
    singleLine=true,placeholder={Text("Cidade da região",color=ArtMuted)},shape=RoundedCornerShape(13.dp),
    colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=ArtRed,unfocusedBorderColor=Color(0xFF5B505B),focusedTextColor=ArtInk,unfocusedTextColor=ArtInk))
   Text("Bairro (opcional)",color=ArtInk,fontWeight=FontWeight.Bold,fontSize=13.sp)
   OutlinedTextField(value=neighborhood,onValueChange=onNeighborhood,
    modifier=Modifier.fillMaxWidth().height(51.dp),singleLine=true,
    placeholder={Text("Todos os bairros",color=ArtMuted)},shape=RoundedCornerShape(13.dp),
    colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=ArtRed,unfocusedBorderColor=Color(0xFF5B505B),focusedTextColor=ArtInk,unfocusedTextColor=ArtInk))
   Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth()){
    Text("Raio de busca: ${radiusKm.toInt()} km",color=ArtInk,fontSize=13.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
    Text("GPS",color=ArtMuted,fontSize=11.sp)
   }
   Slider(value=radiusKm,onValueChange=onRadius,valueRange=1f..50f,steps=48,modifier=Modifier.fillMaxWidth().height(30.dp),
    colors=SliderDefaults.colors(thumbColor=ArtRed,activeTrackColor=ArtRed))
   Button(onClick=onScan,enabled=!searching,modifier=Modifier.fillMaxWidth().height(49.dp),
    shape=RoundedCornerShape(14.dp),
    colors=ButtonDefaults.buttonColors(containerColor=ArtRed,contentColor=Color.White)){
    Text(if(searching)"Rastreando salões..." else "⌕  Rastrear Salões",fontWeight=FontWeight.Bold)
   }
  }
  Box(Modifier.offset(x=0.dp,y=0.dp).width(w*0.15f).height(h*0.12f).clickable{onBack()})
  if(searching || message.isNotBlank()){
   Surface(Modifier.offset(x=w*0.06f,y=h*0.67f).width(w*0.88f),
    shape=RoundedCornerShape(15.dp),color=Color(0xF0090A10)){
    Column(Modifier.padding(11.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
     Text(if(searching)"Consultando estabelecimentos reais..." else message,
      color=ArtInk,fontSize=12.sp,maxLines=3)
     if(searching){
      LinearProgressIndicator(progress={progress.coerceIn(0,100)/100f},
       modifier=Modifier.fillMaxWidth(),color=ArtRed,trackColor=Color(0xFF4C2631))
      Text("${progress.coerceIn(0,100)}%",color=ArtInk,fontSize=12.sp)
     }
    }
   }
  }
  Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(h*0.122f),
   horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically){
   TextButton(onClick=onMap){Text("Mapa ao vivo",color=ArtInk)}
   TextButton(onClick=onResults,enabled=resultsCount>0){Text("Resultados ($resultsCount)",color=ArtInk)}
  }
 }
}

@Composable fun ArtworkResults(
 results:List<Prospect>,selectedIds:Set<String>,
 onSelect:(String)->Unit,onSelectAll:(Boolean)->Unit,
 onOpen:(Prospect)->Unit,onBack:()->Unit,onShare:()->Unit,onCopy:()->Unit
){
 var filter by remember{mutableStateOf("Todos")}
 val displayed=results.filter{
  when(filter){
   "Com número"->normalisedSalonPhone(it.phone)!=null
   "Sem número"->normalisedSalonPhone(it.phone)==null
   else->true
  }
 }
 Box(Modifier.fillMaxSize().background(ArtBlack)){
  Artwork("dalcotone_resultado_busca.png",Modifier.fillMaxSize())
  // Native list covers sample salon names and 148 illustrative results.
  Column(Modifier.fillMaxSize().background(Color(0xF909090C)).padding(horizontal=14.dp,vertical=9.dp),
   verticalArrangement=Arrangement.spacedBy(10.dp)){
   Row(verticalAlignment=Alignment.CenterVertically){
    TextButton(onClick=onBack){Text("‹",fontSize=30.sp,color=ArtInk)}
    Text("Resultado da Busca",color=ArtInk,fontWeight=FontWeight.ExtraBold,fontSize=21.sp)
   }
   Surface(shape=RoundedCornerShape(17.dp),color=ArtPanel,border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF4C3A44))){
    Column(Modifier.fillMaxWidth().padding(14.dp)){
     Text("📍 Salões encontrados",fontWeight=FontWeight.Bold,color=ArtInk)
     Text("${results.size} estabelecimentos reais disponíveis",color=ArtMuted,fontSize=12.sp)
    }
   }
   Row(horizontalArrangement=Arrangement.spacedBy(7.dp),modifier=Modifier.fillMaxWidth()){
    listOf("Todos","Com número","Sem número").forEach{value->
     val active=filter==value
     Surface(onClick={filter=value},modifier=Modifier.weight(1f),shape=RoundedCornerShape(13.dp),
      color=if(active)ArtRed else ArtPanel,border=if(active)null else androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF50434D))){
      Box(Modifier.padding(vertical=10.dp),contentAlignment=Alignment.Center){
       Text(value,color=ArtInk,fontSize=11.sp,maxLines=1)
      }
     }
    }
   }
   if(displayed.isEmpty()){
    Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){
     Text("Nenhum salão neste filtro. Faça uma nova pesquisa.",color=ArtMuted)
    }
   }else{
    LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(9.dp)){
     items(displayed,key={it.placeId}){p->
      Surface(shape=RoundedCornerShape(17.dp),color=ArtPanel,
       border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF46343D))){
       Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(9.dp)){
        Checkbox(checked=p.placeId in selectedIds,onCheckedChange={onSelect(p.placeId)},
         colors=CheckboxDefaults.colors(checkedColor=ArtRed))
        Column(Modifier.weight(1f).clickable{onOpen(p)},verticalArrangement=Arrangement.spacedBy(3.dp)){
         Text(p.name,color=ArtInk,fontWeight=FontWeight.Bold,maxLines=2)
         Text(p.address.ifBlank{"Endereço não informado"},color=ArtMuted,fontSize=12.sp,maxLines=2)
         Text(p.phone?.takeIf{it.isNotBlank()} ?: "Telefone não disponível",
          color=if(normalisedSalonPhone(p.phone)!=null)ArtGreen else ArtMuted,fontSize=12.sp)
        }
        Text("❯",color=ArtRed,modifier=Modifier.clickable{onOpen(p)})
       }
      }
     }
    }
   }
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
    Text("Selecionados: ${results.count{it.placeId in selectedIds}}",color=ArtInk,fontSize=12.sp)
    TextButton(onClick={onSelectAll(true)}){Text("Todos",color=ArtInk)}
    TextButton(onClick={onSelectAll(false)}){Text("Limpar",color=ArtMuted)}
   }
   Button(onClick=onShare,enabled=results.any{it.placeId in selectedIds},modifier=Modifier.fillMaxWidth().height(53.dp),
    colors=ButtonDefaults.buttonColors(containerColor=ArtGreen),shape=RoundedCornerShape(14.dp)){
    Text("◉  Enviar lista para WhatsApp",color=Color.White,fontWeight=FontWeight.Bold)
   }
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){
    TextButton(onClick=onCopy,enabled=results.isNotEmpty()){Text("Copiar lista",color=ArtInk)}
    TextButton(onClick=onBack){Text("Nova pesquisa",color=ArtInk)}
   }
  }
 }
}

fun normalisedSalonPhone(number:String?):String?{
 val digits=number.orEmpty().filter{it.isDigit()}
 return when{
  digits.length in 12..13 && digits.startsWith("55")->digits
  digits.length in 10..11->"55"+digits
  else->null
 }
}

fun formatSalonExport(results:List<Prospect>,heading:String="DALCOTONE | Lista de Salões"):String{
 val entries=results.mapIndexed{index,p->
  val number=p.phone?.trim().takeUnless{it.isNullOrEmpty()} ?: "Não disponível"
  val numberForLink=normalisedSalonPhone(p.phone)
  val map=if(p.latitude!=null&&p.longitude!=null) "https://www.google.com/maps/search/?api=1&query=${p.latitude},${p.longitude}" else "Localização indisponível"
  "${index+1}. ${p.name}\nEndereço: ${p.address.ifBlank{"Não informado"}}\nTelefone: $number\nWhatsApp: ${if(numberForLink!=null)"https://wa.me/$numberForLink" else "Número não disponível"}\nGoogle Maps: $map"
 }
 return heading+"\nTotal: ${results.size}\n\n"+entries.joinToString("\n\n")
}

@Composable fun ArtworkShare(
 results:List<Prospect>,onBack:()->Unit,onSend:(String)->Unit,onCopy:(String)->Unit
){
 var onlyPhones by remember{mutableStateOf(false)}
 var split by remember{mutableStateOf(true)}
 var part by remember{mutableIntStateOf(0)}
 val eligible=results.filter{!onlyPhones||normalisedSalonPhone(it.phone)!=null}
 val chunks=eligible.chunked(30)
 val currentIndex=part.coerceIn(0,(chunks.size-1).coerceAtLeast(0))
 val sendResults=if(split && chunks.size>1)chunks[currentIndex] else eligible
 val heading=if(split && chunks.size>1)"DALCOTONE | Parte ${currentIndex+1}/${chunks.size}" else "DALCOTONE | Lista de Salões"
 val message=formatSalonExport(sendResults,heading)
 Box(Modifier.fillMaxSize().background(ArtBlack)){
  Artwork("dalcotone_enviar_whatsapp.png",Modifier.fillMaxSize())
  Column(Modifier.fillMaxSize().background(Color(0xF909090C)).padding(horizontal=14.dp,vertical=8.dp),
   verticalArrangement=Arrangement.spacedBy(10.dp)){
   Row(verticalAlignment=Alignment.CenterVertically){
    TextButton(onClick=onBack){Text("‹",fontSize=30.sp,color=ArtInk)}
    Text("Enviar para WhatsApp",color=ArtInk,fontSize=21.sp,fontWeight=FontWeight.ExtraBold)
   }
   Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),color=ArtPanel,
    border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF473A42))){
    Column(Modifier.padding(12.dp)){
     Text("Prévia da mensagem",color=ArtInk,fontWeight=FontWeight.Bold)
     Text("${eligible.size} salões selecionados",color=ArtMuted,fontSize=12.sp)
    }
   }
   Surface(Modifier.weight(1f).fillMaxWidth(),shape=RoundedCornerShape(15.dp),color=ArtPanel){
    Column(Modifier.padding(14.dp).verticalScroll(rememberScrollState())){
     Text(message,color=ArtInk,fontSize=12.sp,lineHeight=18.sp)
    }
   }
   Surface(shape=RoundedCornerShape(16.dp),color=ArtPanel){
    Column(Modifier.fillMaxWidth().padding(10.dp)){
     Text("Opções de envio",color=ArtInk,fontWeight=FontWeight.Bold)
     Row(verticalAlignment=Alignment.CenterVertically){
      Checkbox(checked=onlyPhones,onCheckedChange={onlyPhones=it;part=0},colors=CheckboxDefaults.colors(checkedColor=ArtRed))
      Text("Apenas com telefone válido",color=ArtInk,fontSize=13.sp)
     }
     Row(verticalAlignment=Alignment.CenterVertically){
      Checkbox(checked=split,onCheckedChange={split=it;part=0},colors=CheckboxDefaults.colors(checkedColor=ArtRed))
      Text("Dividir mensagens em grupos de 30",color=ArtInk,fontSize=13.sp)
     }
     if(split && chunks.size>1){
      Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween,modifier=Modifier.fillMaxWidth()){
       TextButton(onClick={part=(part-1).coerceAtLeast(0)},enabled=currentIndex>0){Text("Anterior")}
       Text("Parte ${currentIndex+1} de ${chunks.size}",color=ArtInk)
       TextButton(onClick={part=(part+1).coerceAtMost(chunks.lastIndex)},enabled=currentIndex<chunks.lastIndex){Text("Próxima")}
      }
     }
    }
   }
   Button(onClick={onSend(message)},enabled=sendResults.isNotEmpty(),modifier=Modifier.fillMaxWidth().height(56.dp),
    shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors(containerColor=ArtGreen)){
    Text(if(split&&chunks.size>1)"◉  Enviar parte ${currentIndex+1} ao WhatsApp" else "◉  Enviar TODOS ao WhatsApp",
     color=Color.White,fontWeight=FontWeight.ExtraBold)
   }
   TextButton(onClick={onCopy(formatSalonExport(eligible))},enabled=eligible.isNotEmpty(),modifier=Modifier.fillMaxWidth()){
    Text("Copiar lista completa",color=ArtInk)
   }
  }
 }
}
