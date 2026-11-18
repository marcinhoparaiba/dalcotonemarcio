package br.com.dalcotone.crm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider

/** OpenStreetMap tiles online with local tile cache; saved pins remain in Room offline. */
@Composable
fun SalonMap(salons:List<Salon>,prospects:List<Prospect> = emptyList(),onSelect:(Salon)->Unit,onProspect:(Prospect)->Unit = {},modifier:Modifier=Modifier,center:GeoPoint?=null){
 val context=androidx.compose.ui.platform.LocalContext.current
 val latestSelect by rememberUpdatedState(onSelect)
 val latestProspect by rememberUpdatedState(onProspect)
 var follow by remember{mutableStateOf(false)}
 val map=remember(context){
  Configuration.getInstance().load(context,PreferenceManager.getDefaultSharedPreferences(context))
  Configuration.getInstance().userAgentValue=context.packageName
  MapView(context).apply{
   setTileSource(TileSourceFactory.MAPNIK)
   setMultiTouchControls(true)
   zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
   controller.setZoom(13.5)
   controller.setCenter(GeoPoint(-7.1195,-34.8450))
   setUseDataConnection(true)
   if(androidx.core.content.ContextCompat.checkSelfPermission(context,android.Manifest.permission.ACCESS_FINE_LOCATION)==android.content.pm.PackageManager.PERMISSION_GRANTED || androidx.core.content.ContextCompat.checkSelfPermission(context,android.Manifest.permission.ACCESS_COARSE_LOCATION)==android.content.pm.PackageManager.PERMISSION_GRANTED){
    val myLocation=MyLocationNewOverlay(GpsMyLocationProvider(context),this)
    myLocation.enableMyLocation()
    overlays.add(myLocation)
   }
  }
 }
 DisposableEffect(map){map.onResume();onDispose{map.overlays.filterIsInstance<MyLocationNewOverlay>().forEach{it.disableFollowLocation();it.disableMyLocation()};map.onPause();map.onDetach()}}
 Box(modifier=modifier.fillMaxWidth()){
 AndroidView(factory={map},modifier=Modifier.fillMaxSize(),update={view->
  view.overlays.filterIsInstance<Marker>().toList().forEach{view.overlays.remove(it)}
  salons.filter{it.latitude!=null&&it.longitude!=null}.forEach{s->
   val marker=Marker(view).apply{
    position=GeoPoint(s.latitude!!,s.longitude!!)
    title=s.name
    snippet=if(s.status=="nao_visitado")"Não visitado" else s.status
    setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM)
    setOnMarkerClickListener{_,_->latestSelect(s);true}
   }
   view.overlays.add(marker)
  }
  prospects.filter{it.latitude!=null&&it.longitude!=null}.forEach{p->
   val marker=Marker(view).apply{
    position=GeoPoint(p.latitude!!,p.longitude!!)
    title=p.name
    snippet="Oportunidade • toque para cadastrar"
    setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM)
    setOnMarkerClickListener{_,_->latestProspect(p);true}
   }
   view.overlays.add(marker)
  }
  if(center!=null)view.controller.animateTo(center)
  else if(salons.any{it.latitude!=null&&it.longitude!=null}){
   val first=salons.first{it.latitude!=null&&it.longitude!=null}
   if(!view.getMapCenter().latitude.isFinite())view.controller.setCenter(GeoPoint(first.latitude!!,first.longitude!!))
  }
  view.invalidate()
 })
 FloatingActionButton(onClick={
  map.overlays.filterIsInstance<MyLocationNewOverlay>().firstOrNull()?.let{overlay->
   follow=!follow
   if(follow){overlay.enableFollowLocation();overlay.myLocation?.let{map.controller.animateTo(it)}}else overlay.disableFollowLocation()
  }
 },modifier=Modifier.align(Alignment.BottomEnd).padding(12.dp)){Text(if(follow)"● GPS" else "◎ GPS")}
 }
}
