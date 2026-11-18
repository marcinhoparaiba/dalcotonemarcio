package br.com.dalcotone.crm

import org.junit.Assert.*
import org.junit.Test

/**
 * Smoke tests for prospect registration and offline entity defaults.
 * These do not test the external Google/OSM/CNPJ services or Android UI.
 */
class ProspectOfflineTest {
 @Test fun googleProspectCanBeSavedAsUnvisited(){
  val prospect=Prospect("google:123","Studio Teste","Rua Exemplo", -7.12,-34.84)
  val salon=Salon(name=prospect.name,address=prospect.address,placeId=prospect.placeId,latitude=prospect.latitude,longitude=prospect.longitude,status="nao_visitado")
  assertEquals("nao_visitado",salon.status)
  assertEquals(prospect.placeId,salon.placeId)
  assertEquals(-7.12,salon.latitude!!,0.00001)
 }
 @Test fun osmProspectHasDistinctSourceId(){
  val prospect=Prospect("osm:node:123","Salão de Teste","", -7.12,-34.84)
  assertTrue(prospect.placeId.startsWith("osm:"))
  assertNotEquals("123",prospect.placeId)
 }
 @Test fun newManualSalonIsUnvisitedByDefault(){
  assertEquals("nao_visitado",Salon(name="Novo salão").status)
 }
 @Test fun independentCommercialRecordsReferenceSalon(){
  val salon=Salon(name="Studio")
  assertEquals(salon.id,SampleDelivery(salonId=salon.id,product="Amostra",quantity=1).salonId)
  assertEquals(salon.id,SalonProfessional(salonId=salon.id,name="Profissional").salonId)
 }
}
