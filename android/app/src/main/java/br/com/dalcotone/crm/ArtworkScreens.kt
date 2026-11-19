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


@Composable private fun ArtworkCrop(name:String,left:Int,top:Int,right:Int,bottom:Int,modifier:Modifier=Modifier){
 val context=LocalContext.current
 val bitmap=remember(name){
  runCatching{context.assets.open(name).use{BitmapFactory.decodeStream(it)?.asImageBitmap()}}.getOrNull()
 }
 Box(modifier.background(ArtBlack)){
  if(bitmap!=null)Canvas(Modifier.fillMaxSize()){
   val x=left.coerceIn(0,bitmap.width-1)
   val y=top.coerceIn(0,bitmap.height-1)
   drawImage(bitmap,srcOffset=IntOffset(x,y),
    srcSize=IntSize((right-x).coerceIn(1,bitmap.width-x),(bottom-y).coerceIn(1,bitmap.height-y)),
    dstOffset=IntOffset.Zero,
    dstSize=IntSize(size.width.toInt().coerceAtLeast(1),size.height.toInt().coerceAtLeast(1)))
  }
 }
}

@Composable fun ArtworkHome(
 salons:Int,contacts:Int,appointments:Int,onNavigate:(String)->Unit
){
 // Layout is built from independent, functional regions. Only the approved
 // logo, product photograph and six button artworks remain raster images.
 Column(Modifier.fillMaxSize().background(ArtBlack)){
  ArtworkBrandHeader(onMenu={onNavigate("Mais")},onNotifications={onNavigate("Contatos")})
  ArtworkCrop("dalcotone_inicio.png",0,91,384,306,Modifier.fillMaxWidth().weight(.33f))
  val tiles=listOf(
   Triple("Rastrear",18 to 306,142 to 424),
   Triple("Rota",146 to 306,267 to 424),
   Triple("Clientes",272 to 306,383 to 424),
   Triple("Financeiro",18 to 427,142 to 549),
   Triple("Contatos",146 to 427,267 to 549),
   Triple("Relatórios",272 to 427,383 to 549)
  )
  Column(Modifier.fillMaxWidth().weight(.36f).padding(horizontal=11.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
   tiles.chunked(3).forEach{row->
    Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(5.dp)){
     row.forEach{(destination,start,finish)->
      Box(Modifier.weight(1f).fillMaxHeight().clickable{onNavigate(destination)}){
       ArtworkCrop("dalcotone_inicio.png",start.first,start.second,finish.first,finish.second,Modifier.fillMaxSize())
      }
     }
    }
   }
  }
  Surface(Modifier.fillMaxWidth().weight(.31f).padding(horizontal=15.dp,vertical=8.dp),
   shape=RoundedCornerShape(18.dp),color=ArtPanel,
   border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF55414C))){
   Row{
    Box(Modifier.width(4.dp).fillMaxHeight().background(ArtRed))
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp,vertical=8.dp),
     verticalArrangement=Arrangement.SpaceEvenly){
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
  ArtworkBottomNavigation(active="Início",onNavigate=onNavigate)
 }
}

@Composable fun ArtworkRadar(
 city:String,onCity:(String)->Unit,
 neighborhood:String,onNeighborhood:(String)->Unit,
 radiusKm:Float,onRadius:(Float)->Unit,
 searching:Boolean,progress:Int,message:String,resultsCount:Int,
 onBack:()->Unit,onScan:()->Unit,onMap:()->Unit,onResults:()->Unit
){
 // Header and red map art are cropped from the exact approved PNG; input values
 // and the search button are real Compose controls, never baked into the image.
 Column(Modifier.fillMaxSize().background(ArtBlack)){
  Box(Modifier.fillMaxWidth().weight(.23f)){
   ArtworkCrop("dalcotone_rastrear_saloes.png",0,0,384,207,Modifier.fillMaxSize())
   Box(Modifier.align(Alignment.TopStart).fillMaxHeight(.4f).width(70.dp).clickable{onBack()})
  }
  Column(Modifier.fillMaxWidth().weight(.43f).padding(horizontal=18.dp),
   verticalArrangement=Arrangement.spacedBy(5.dp)){
   Text("Cidade (filtro de endereço)",color=ArtInk,fontSize=13.sp,fontWeight=FontWeight.Bold)
   OutlinedTextField(value=city,onValueChange=onCity,modifier=Modifier.fillMaxWidth().height(52.dp),
    singleLine=true,placeholder={Text("Todas as cidades próximas",color=ArtMuted)},
    shape=RoundedCornerShape(13.dp),colors=OutlinedTextFieldDefaults.colors(
     focusedBorderColor=ArtRed,unfocusedBorderColor=Color(0xFF55444D),
     focusedTextColor=ArtInk,unfocusedTextColor=ArtInk))
   Text("Bairro (opcional)",color=ArtInk,fontSize=13.sp,fontWeight=FontWeight.Bold)
   OutlinedTextField(value=neighborhood,onValueChange=onNeighborhood,modifier=Modifier.fillMaxWidth().height(52.dp),
    singleLine=true,placeholder={Text("Todos os bairros",color=ArtMuted)},
    shape=RoundedCornerShape(13.dp),colors=OutlinedTextFieldDefaults.colors(
     focusedBorderColor=ArtRed,unfocusedBorderColor=Color(0xFF55444D),
     focusedTextColor=ArtInk,unfocusedTextColor=ArtInk))
   Text("Raio de busca pelo GPS: ${radiusKm.toInt()} km",color=ArtInk,fontSize=13.sp,fontWeight=FontWeight.Bold)
   Slider(value=radiusKm,onValueChange=onRadius,valueRange=1f..50f,steps=48,
    modifier=Modifier.fillMaxWidth().height(29.dp),colors=SliderDefaults.colors(thumbColor=ArtRed,activeTrackColor=ArtRed))
   Button(onClick=onScan,enabled=!searching,modifier=Modifier.fillMaxWidth().height(47.dp),
    shape=RoundedCornerShape(14.dp),
    colors=ButtonDefaults.buttonColors(containerColor=ArtRed,contentColor=Color.White)){
    Text(if(searching)"Buscando salões..." else "⌕  Rastrear Salões",fontWeight=FontWeight.ExtraBold)
   }
  }
  Box(Modifier.fillMaxWidth().weight(.24f).clickable{onMap()}){
   ArtworkCrop("dalcotone_rastrear_saloes.png",0,548,384,755,Modifier.fillMaxSize())
   if(searching||message.isNotBlank()){
    Surface(Modifier.align(Alignment.Center).fillMaxWidth(.9f),
     color=Color(0xED110C15),shape=RoundedCornerShape(14.dp)){
     Column(Modifier.padding(11.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
      Text(if(searching)"Consultando salões e números públicos..." else message,
       color=ArtInk,fontSize=12.sp,maxLines=4)
      if(searching){
       LinearProgressIndicator(progress={progress.coerceIn(0,100)/100f},
        modifier=Modifier.fillMaxWidth(),color=ArtRed,trackColor=Color(0xFF472533))
       Text("${progress.coerceIn(0,100)}%",color=ArtInk)
      }
     }
    }
   }
  }
  Row(Modifier.fillMaxWidth().weight(.10f),verticalAlignment=Alignment.CenterVertically,
   horizontalArrangement=Arrangement.SpaceEvenly){
   TextButton(onClick=onBack){Text("Início",color=ArtInk)}
   TextButton(onClick=onMap){Text("Mapa interativo",color=ArtInk)}
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

@Composable fun ArtworkBrandHeader(onMenu:()->Unit,onNotifications:()->Unit){
 val context=LocalContext.current
 val bitmap=remember{
  runCatching{context.assets.open("dalcotone_inicio.png").use{BitmapFactory.decodeStream(it)?.asImageBitmap()}}.getOrNull()
 }
 BoxWithConstraints(Modifier.fillMaxWidth().height(80.dp).background(ArtBlack)){
  if(bitmap!=null)Canvas(Modifier.fillMaxSize()){
   drawImage(bitmap,srcOffset=IntOffset(0,0),
    srcSize=IntSize(bitmap.width,92.coerceAtMost(bitmap.height)),
    dstOffset=IntOffset.Zero,dstSize=IntSize(size.width.toInt().coerceAtLeast(1),size.height.toInt().coerceAtLeast(1)))
  }
  Box(Modifier.align(Alignment.CenterStart).fillMaxHeight().width(maxWidth*0.20f).clickable{onMenu()})
  Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(maxWidth*0.18f).clickable{onNotifications()})
 }
}

@Composable fun ArtworkBottomNavigation(active:String,onNavigate:(String)->Unit){
 val items=listOf("Início" to "⌂","Mapa" to "⌖","Clientes" to "♙","Rota" to "◇","Mais" to "•••")
 Surface(Modifier.fillMaxWidth().height(76.dp),shape=RoundedCornerShape(topStart=18.dp,topEnd=18.dp),
  color=Color(0xFF0D0D12),border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF3C2630))){
  Row(Modifier.fillMaxSize().padding(horizontal=5.dp,vertical=4.dp)){
   items.forEach{(destination,symbol)->
    val selected=active==destination || (destination=="Mais" && active !in listOf("Início","Mapa","Clientes","Rota"))
    Column(Modifier.weight(1f).fillMaxHeight()
     .background(if(selected)Color(0xFF970017) else Color.Transparent,RoundedCornerShape(13.dp))
     .clickable{onNavigate(destination)},verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){
     Text(symbol,fontSize=23.sp,color=if(selected)Color(0xFFFF4E65) else ArtInk,fontWeight=FontWeight.Bold)
     Text(destination,fontSize=11.sp,color=ArtInk,fontWeight=if(selected)FontWeight.Bold else FontWeight.Normal)
    }
   }
  }
 }
}
