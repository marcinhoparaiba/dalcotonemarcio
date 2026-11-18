package br.com.dalcotone.crm

import org.junit.Assert.*
import org.junit.Test

class FinancialRulesTest {
 @Test fun partialPaymentsReduceBalance(){
  val sale=80000L
  val paid=listOf(32000L,10000L).sum()
  assertEquals(38000L,sale-paid)
 }
 @Test fun excessivePaymentMustBeRejected(){
  val sale=80000L
  val paid=32000L
  val attempted=50000L
  assertFalse(attempted<=sale-paid)
 }
 @Test fun reminderLeadTime(){
  val start=1_800_000_000_000L
  assertEquals(1_799_998_200_000L,start-30*60_000L)
 }
}
