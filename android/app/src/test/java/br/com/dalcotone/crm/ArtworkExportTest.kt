package br.com.dalcotone.crm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkExportTest {
 @Test fun convertsValidBrazilianNumbersToWhatsAppLinks(){
  assertEquals("5583999912345",normalisedSalonPhone("(83) 99991-2345"))
  assertEquals("5583999912345",normalisedSalonPhone("+55 83 99991-2345"))
  assertEquals(null,normalisedSalonPhone("12345"))
  assertEquals(null,normalisedSalonPhone(null))
 }
 @Test fun fullListHasRealAddressTelephoneAndMapWithoutInventingMissingPhone(){
  val places=listOf(
   Prospect(placeId="one",name="Salão Teste",address="Rua Exemplo, 100",latitude=-7.12,longitude=-34.86,phone="(83) 99991-2345"),
   Prospect(placeId="two",name="Sem Número",address="Bairro Centro",latitude=null,longitude=null,phone=null)
  )
  val message=formatSalonExport(places)
  assertTrue(message.contains("Total: 2"))
  assertTrue(message.contains("Salão Teste"))
  assertTrue(message.contains("Rua Exemplo, 100"))
  assertTrue(message.contains("https://wa.me/5583999912345"))
  assertTrue(message.contains("https://www.google.com/maps/search/?api=1&query=-7.12,-34.86"))
  assertTrue(message.contains("Sem Número"))
  assertTrue(message.contains("Telefone: Não disponível"))
  assertTrue(message.contains("Localização indisponível"))
  assertFalse(message.contains("https://wa.me/null"))
 }
 @Test fun preservingAllDiscoveredEntriesWhenPreparingBatches(){
  val entries=(1..75).map{Prospect("salon-$it","Salão $it","Rua $it",null,null)}
  val groups=entries.chunked(30)
  assertEquals(listOf(30,30,15),groups.map{it.size})
  assertEquals(75,entries.size)
  assertTrue(formatSalonExport(entries).contains("Salão 75"))
 }
}
