import {NextRequest,NextResponse} from "next/server";
export const runtime="nodejs";
export async function GET(req:NextRequest){
 const secret=process.env.CRON_SECRET;
 if(!secret||req.headers.get("authorization")!==`Bearer ${secret}`)return NextResponse.json({error:"Não autorizado"},{status:401});
 const url=process.env.NEXT_PUBLIC_SUPABASE_URL,key=process.env.SUPABASE_SERVICE_ROLE_KEY;
 if(!url||!key)return NextResponse.json({error:"Serviço não configurado"},{status:503});
 const {createClient}=await import("@supabase/supabase-js");
 const db=createClient(url,key,{auth:{persistSession:false}});
 const now=new Date(),end=new Date(now.getTime()+30*60000);
 const {data,error}=await db.from("appointments").select("id,owner_id,salon_id,starts_at,remind_minutes,notes").eq("completed",false).is("notified_at",null).gte("starts_at",now.toISOString()).lte("starts_at",end.toISOString()).limit(200);
 if(error)return NextResponse.json({error:"Consulta falhou"},{status:500});
 // Notifications are not sent until push subscriptions and a delivery provider are configured.
 return NextResponse.json({pending:(data||[]).length,delivery:"not_configured"});
}
