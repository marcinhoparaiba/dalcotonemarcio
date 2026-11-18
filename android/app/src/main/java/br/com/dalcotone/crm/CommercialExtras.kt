package br.com.dalcotone.crm

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Additional offline commercial modules, stored separately to preserve the CRM v1 database. */
@Entity(tableName="sample_deliveries",indices=[Index("salonId")])
data class SampleDelivery(@PrimaryKey val id:String=UUID.randomUUID().toString(),val salonId:String,val product:String,val quantity:Int,val deliveredAt:Long=System.currentTimeMillis(),val feedback:String="")
@Entity(tableName="satisfaction_surveys",indices=[Index("salonId")])
data class SatisfactionSurvey(@PrimaryKey val id:String=UUID.randomUUID().toString(),val salonId:String,val product:String,val rating:Int,val result:String,val wouldBuyAgain:Boolean,val createdAt:Long=System.currentTimeMillis())
@Entity(tableName="salon_professionals",indices=[Index("salonId")])
data class SalonProfessional(@PrimaryKey val id:String=UUID.randomUUID().toString(),val salonId:String,val name:String,val specialty:String="",val phone:String="",val preferences:String="")
@Entity(tableName="competitor_products",indices=[Index("salonId")])
data class CompetitorProduct(@PrimaryKey val id:String=UUID.randomUUID().toString(),val salonId:String,val brand:String,val product:String,val priceCents:Long?=null,val alternative:String="",val observedAt:Long=System.currentTimeMillis())
@Entity(tableName="courtesies",indices=[Index("salonId")])
data class Courtesy(@PrimaryKey val id:String=UUID.randomUUID().toString(),val salonId:String,val description:String,val kind:String,val estimatedCostCents:Long=0,val deliveredAt:Long=System.currentTimeMillis())
@Entity(tableName="commission_goals")
data class CommissionGoal(@PrimaryKey val month:String,val targetCents:Long,val commissionBasisPoints:Int,val updatedAt:Long=System.currentTimeMillis())
@Entity(tableName="sales_objections",indices=[Index("salonId")])
data class SalesObjection(@PrimaryKey val id:String=UUID.randomUUID().toString(),val salonId:String,val reason:String,val details:String="",val recordedAt:Long=System.currentTimeMillis())
@Entity(tableName="special_dates",indices=[Index("salonId")])
data class SpecialDate(@PrimaryKey val id:String=UUID.randomUUID().toString(),val salonId:String,val label:String,val month:Int,val day:Int,val notes:String="")
@Entity(tableName="technical_demos",indices=[Index("salonId")])
data class TechnicalDemo(@PrimaryKey val id:String=UUID.randomUUID().toString(),val salonId:String,val product:String,val attendees:String="",val result:String="",val demonstratedAt:Long=System.currentTimeMillis())
@Entity(tableName="work_expenses")
data class WorkExpense(@PrimaryKey val id:String=UUID.randomUUID().toString(),val category:String,val amountCents:Long,val occurredAt:Long=System.currentTimeMillis(),val description:String="",val salonId:String?=null)

@Dao interface CommercialExtrasDao {
 @Query("SELECT * FROM sample_deliveries ORDER BY deliveredAt DESC") fun samples():Flow<List<SampleDelivery>>
 @Query("SELECT * FROM satisfaction_surveys ORDER BY createdAt DESC") fun surveys():Flow<List<SatisfactionSurvey>>
 @Query("SELECT * FROM salon_professionals ORDER BY name") fun professionals():Flow<List<SalonProfessional>>
 @Query("SELECT * FROM competitor_products ORDER BY observedAt DESC") fun competitors():Flow<List<CompetitorProduct>>
 @Query("SELECT * FROM courtesies ORDER BY deliveredAt DESC") fun courtesies():Flow<List<Courtesy>>
 @Query("SELECT * FROM commission_goals ORDER BY month DESC") fun goals():Flow<List<CommissionGoal>>
 @Query("SELECT * FROM sales_objections ORDER BY recordedAt DESC") fun objections():Flow<List<SalesObjection>>
 @Query("SELECT * FROM special_dates ORDER BY month,day") fun dates():Flow<List<SpecialDate>>
 @Query("SELECT * FROM technical_demos ORDER BY demonstratedAt DESC") fun demos():Flow<List<TechnicalDemo>>
 @Query("SELECT * FROM work_expenses ORDER BY occurredAt DESC") fun expenses():Flow<List<WorkExpense>>
 @Insert suspend fun addSample(value:SampleDelivery)
 @Insert suspend fun addSurvey(value:SatisfactionSurvey)
 @Insert suspend fun addProfessional(value:SalonProfessional)
 @Insert suspend fun addCompetitor(value:CompetitorProduct)
 @Insert suspend fun addCourtesy(value:Courtesy)
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun saveGoal(value:CommissionGoal)
 @Insert suspend fun addObjection(value:SalesObjection)
 @Insert suspend fun addDate(value:SpecialDate)
 @Insert suspend fun addDemo(value:TechnicalDemo)
 @Insert suspend fun addExpense(value:WorkExpense)
 @Query("SELECT COALESCE(SUM(amountCents),0) FROM work_expenses WHERE occurredAt>=:start AND occurredAt<:end") suspend fun expensesBetween(start:Long,end:Long):Long
 @Query("SELECT COALESCE(SUM(estimatedCostCents),0) FROM courtesies WHERE deliveredAt>=:start AND deliveredAt<:end") suspend fun courtesiesBetween(start:Long,end:Long):Long
}

@Database(entities=[SampleDelivery::class,SatisfactionSurvey::class,SalonProfessional::class,CompetitorProduct::class,Courtesy::class,CommissionGoal::class,SalesObjection::class,SpecialDate::class,TechnicalDemo::class,WorkExpense::class],version=1,exportSchema=false)
abstract class CommercialExtrasDatabase:RoomDatabase(){
 abstract fun dao():CommercialExtrasDao
 companion object {
  @Volatile private var instance:CommercialExtrasDatabase?=null
  fun get(context:Context):CommercialExtrasDatabase=instance?:synchronized(this){instance?:Room.databaseBuilder(context.applicationContext,CommercialExtrasDatabase::class.java,"dalcotone_comercial.sqlite").build().also{instance=it}}
 }
}
