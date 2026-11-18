import {NextRequest,NextResponse} from "next/server";
import {createClient} from "@supabase/supabase-js";
import webpush from "web-push";
export const runtime="nodejs";
export async function GET(req:NextRequest){
 const secret=process.env.CRON_SECRET;
 if(!secret||req.headers.get("authorization")!==`Bearer ${secret}`)return NextResponse.json({error:"Não autorizado"},{status:401});
 const url=process.env.NEXT_PUBLIC_SUPABASE_URL,key=process.env.SUPABASE_SERVICE_ROLE_KEY;
 const pub=process.env.NEXT_PUBLIC_VAPID_PUBLIC_KEY,priv=process.env.VAPID_PRIVATE_KEY,subject=process.env.VAPID_SUBJECT;
 if(!url||!key||!pub||!priv||!subject)return NextResponse.json({error:"Envio push não configurado"},{status:503});
 webpush.setVapidDetails(subject,pub,priv);
 const db=createClient(url,key,{auth:{persistSession:false}});
 const now=new Date(),max=new Date(now.getTime()+1440*60000);
 const {data:appointments,error}=await db.from("appointments").select("id,owner_id,salon_id,starts_at,remind_minutes,notes").eq("completed",false).is("notified_at",null).gte("starts_at",now.toISOString()).lte("starts_at",max.toISOString()).limit(500);
 if(error)return NextResponse.json({error:"Consulta falhou"},{status:500});
 let sent=0,failed=0;
 for(const a of appointments||[]){
  const target=Date.parse(a.starts_at)-a.remind_minutes*60000;
  if(target>Date.now())continue;
  // Claim once; protects against duplicate cron executions.
  const claimedAt=new Date().toISOString();
  const {data:claimed,error:claimError}=await db.from("appointments").update({notified_at:claimedAt}).eq("id",a.id).is("notified_at",null).select("id").maybeSingle();
  if(claimError||!claimed)continue;
  const {data:salon}=await db.from("salons").select("name").eq("id",a.salon_id).single();
  const {data:subs}=await db.from("push_subscriptions").select("id,subscription").eq("owner_id",a.owner_id);
  if(!subs?.length){await db.from("appointments").update({notified_at:null}).eq("id",a.id).eq("notified_at",claimedAt);continue}
  let success=false;
  for(const sub of subs){
   try{await webpush.sendNotification(sub.subscription,JSON.stringify({title:"Dal Cotone • Visita agendada",body:`Visita ao salão ${salon?.name||"cadastrado"} em ${a.remind_minutes} minutos.`,url:"/?tab=agenda",tag:`appointment-${a.id}`}),{TTL:3600});sent++;success=true}
   catch(e:any){failed++;if(e?.statusCode===404||e?.statusCode===410)await db.from("push_subscriptions").delete().eq("id",sub.id)}
  }
  if(!success)await db.from("appointments").update({notified_at:null}).eq("id",a.id).eq("notified_at",claimedAt);
 }
 return NextResponse.json({sent,failed,checked:appointments?.length||0});
}
