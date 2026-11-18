import {NextRequest,NextResponse} from "next/server";
import {createClient} from "@supabase/supabase-js";
export const runtime="nodejs";
export async function POST(req:NextRequest){
 const url=process.env.NEXT_PUBLIC_SUPABASE_URL,key=process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;
 const token=req.headers.get("authorization")?.match(/^Bearer (.+)$/)?.[1];
 if(!url||!key)return NextResponse.json({error:"Supabase não configurado"},{status:503});
 if(!token)return NextResponse.json({error:"Autenticação necessária"},{status:401});
 const db=createClient(url,key,{global:{headers:{Authorization:`Bearer ${token}`}},auth:{persistSession:false}});
 const {data:{user},error}=await db.auth.getUser(token);
 if(error||!user)return NextResponse.json({error:"Sessão inválida"},{status:401});
 let body:any;try{body=await req.json()}catch{return NextResponse.json({error:"JSON inválido"},{status:400})}
 const subscription=body?.subscription;
 if(typeof subscription?.endpoint!=="string"||!subscription.endpoint.startsWith("https://")||subscription.endpoint.length>2048||typeof subscription?.keys?.p256dh!=="string"||typeof subscription?.keys?.auth!=="string")return NextResponse.json({error:"Assinatura inválida"},{status:400});
 const {error:writeError}=await db.from("push_subscriptions").upsert({owner_id:user.id,endpoint:subscription.endpoint,subscription},{onConflict:"owner_id,endpoint"});
 if(writeError)return NextResponse.json({error:"Não foi possível salvar assinatura"},{status:500});
 return NextResponse.json({ok:true});
}
export async function DELETE(req:NextRequest){
 const url=process.env.NEXT_PUBLIC_SUPABASE_URL,key=process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;
 const token=req.headers.get("authorization")?.match(/^Bearer (.+)$/)?.[1];
 if(!url||!key||!token)return NextResponse.json({error:"Não autorizado"},{status:401});
 const db=createClient(url,key,{global:{headers:{Authorization:`Bearer ${token}`}},auth:{persistSession:false}});
 const {data:{user}}=await db.auth.getUser(token);
 if(!user)return NextResponse.json({error:"Não autorizado"},{status:401});
 let endpoint="";try{endpoint=(await req.json()).endpoint}catch{}
 if(!endpoint)return NextResponse.json({error:"Endpoint obrigatório"},{status:400});
 const {error}=await db.from("push_subscriptions").delete().eq("owner_id",user.id).eq("endpoint",endpoint);
 return NextResponse.json({ok:!error},{status:error?500:200});
}
