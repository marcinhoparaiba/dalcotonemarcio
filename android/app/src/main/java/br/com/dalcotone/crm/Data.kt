package br.com.dalcotone.crm

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName="salons", indices=[Index(value=["placeId"], unique=true)])
data class Salon(
    @PrimaryKey val id:String=java.util.UUID.randomUUID().toString(),
    val name:String,
    val city:String="",
    val address:String="",
    val phone:String="",
    val status:String="nao_visitado",
    val notes:String="",
    val placeId:String?=null,
    val latitude:Double?=null,
    val longitude:Double?=null,
    val lastContactAt:Long?=null,
    val doNotContact:Boolean=false
)
@Entity(tableName="visits", foreignKeys=[ForeignKey(entity=Salon::class,parentColumns=["id"],childColumns=["salonId"],onDelete=ForeignKey.CASCADE)],indices=[Index("salonId")])
data class Visit(@PrimaryKey val id:String=java.util.UUID.randomUUID().toString(),val salonId:String,val visitedAt:Long=System.currentTimeMillis(),val outcome:String,val notes:String="")
@Entity(tableName="appointments",foreignKeys=[ForeignKey(entity=Salon::class,parentColumns=["id"],childColumns=["salonId"],onDelete=ForeignKey.CASCADE)],indices=[Index("salonId")])
data class Appointment(@PrimaryKey val id:String=java.util.UUID.randomUUID().toString(),val salonId:String,val startsAt:Long,val notes:String="",val remindMinutes:Int=30,val completed:Boolean=false)
@Entity(tableName="sales",foreignKeys=[ForeignKey(entity=Salon::class,parentColumns=["id"],childColumns=["salonId"],onDelete=ForeignKey.CASCADE)],indices=[Index("salonId")])
data class Sale(@PrimaryKey val id:String=java.util.UUID.randomUUID().toString(),val salonId:String,val soldAt:Long=System.currentTimeMillis(),val dueAt:Long?,val totalCents:Long)
@Entity(tableName="sale_items",foreignKeys=[ForeignKey(entity=Sale::class,parentColumns=["id"],childColumns=["saleId"],onDelete=ForeignKey.CASCADE)],indices=[Index("saleId")])
data class SaleItem(@PrimaryKey val id:String=java.util.UUID.randomUUID().toString(),val saleId:String,val product:String,val quantity:Int,val unitPriceCents:Long)
@Entity(tableName="payments",foreignKeys=[ForeignKey(entity=Sale::class,parentColumns=["id"],childColumns=["saleId"],onDelete=ForeignKey.CASCADE)],indices=[Index("saleId")])
data class Payment(@PrimaryKey val id:String=java.util.UUID.randomUUID().toString(),val saleId:String,val amountCents:Long,val paidAt:Long=System.currentTimeMillis(),val method:String,val receiptUri:String?=null)
@Dao interface CrmDao {
    @Query("SELECT * FROM salons ORDER BY name") fun salons():Flow<List<Salon>>
    @Query("SELECT * FROM appointments ORDER BY startsAt") fun appointments():Flow<List<Appointment>>
    @Query("SELECT * FROM sales ORDER BY soldAt DESC") fun sales():Flow<List<Sale>>
    @Query("SELECT * FROM payments ORDER BY paidAt DESC") fun payments():Flow<List<Payment>>
    @Query("SELECT * FROM sale_items ORDER BY product") fun items():Flow<List<SaleItem>>
    @Query("SELECT * FROM visits ORDER BY visitedAt DESC") fun visits():Flow<List<Visit>>
    @Upsert suspend fun saveSalon(s:Salon)
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun saveDiscoveredSalon(s:Salon):Long
    @Upsert suspend fun restoreVisit(v:Visit)
    @Upsert suspend fun restoreAppointment(a:Appointment)
    @Upsert suspend fun restoreSale(s:Sale)
    @Upsert suspend fun restoreItem(i:SaleItem)
    @Upsert suspend fun restorePayment(p:Payment)
    @Insert suspend fun addVisit(v:Visit)
    @Insert suspend fun addAppointment(a:Appointment)
    @Insert suspend fun addSale(s:Sale)
    @Insert suspend fun addItem(i:SaleItem)
    @Insert suspend fun addPayment(p:Payment)
    @Query("SELECT * FROM sales WHERE id=:id LIMIT 1") suspend fun sale(id:String):Sale?
    @Query("SELECT COALESCE(SUM(amountCents),0) FROM payments WHERE saleId=:id") suspend fun paid(id:String):Long
    @Transaction suspend fun pay(saleId:String,amount:Long,method:String){
        val sale=sale(saleId) ?: error("Venda não encontrada")
        require(amount>0 && amount<=sale.totalCents-paid(saleId)){"Pagamento excede saldo"}
        addPayment(Payment(saleId=saleId,amountCents=amount,method=method))
    }
}
@Database(entities=[Salon::class,Visit::class,Appointment::class,Sale::class,SaleItem::class,Payment::class],version=1,exportSchema=false)
abstract class CrmDatabase:RoomDatabase(){
 abstract fun dao():CrmDao
 companion object {
  @Volatile private var instance:CrmDatabase?=null
  fun get(context:android.content.Context):CrmDatabase=instance?:synchronized(this){instance?:Room.databaseBuilder(context.applicationContext,CrmDatabase::class.java,"dalcotone.sqlite").build().also{instance=it}}
 }
}
