import {NextRequest,NextResponse} from "next/server";
export const runtime="nodejs";
const limits=new Map<string,{time:number,count:number}>();
export async function GET(req:NextRequest){
 const key=process.env.GOOGLE_PLACES_API_KEY;
 if(!key)return NextResponse.json({error:"Google Places não configurado"},{status:503});
 // This endpoint must not be publicly usable without authentication.
 const {createClient}=await import("@supabase/supabase-js");
 const url=process.env.NEXT_PUBLIC_SUPABASE_URL,anon=process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;
 const bearer=req.headers.get("authorization")?.match(/^Bearer (.+)$/)?.[1];
 if(!url||!anon||!bearer)return NextResponse.json({error:"Autenticação necessária"},{status:401});
 const auth=createClient(url,anon,{global:{headers:{Authorization:`Bearer ${bearer}`}},auth:{persistSession:false}});
 const {data:{user}}=await auth.auth.getUser(bearer);
 if(!user)return NextResponse.json({error:"Sessão inválida"},{status:401});
 const now=Date.now(),lim=limits.get(user.id)||{time:now,count:0};
 if(now-lim.time>60000){lim.time=now;lim.count=0}if(++lim.count>12)return NextResponse.json({error:"Limite de consultas por minuto"},{status:429});limits.set(user.id,lim);
 const lat=Number(req.nextUrl.searchParams.get("lat")),lng=Number(req.nextUrl.searchParams.get("lng"));
 if(!Number.isFinite(lat)||!Number.isFinite(lng)||Math.abs(lat)>90||Math.abs(lng)>180||!req.nextUrl.searchParams.has("lat")||!req.nextUrl.searchParams.has("lng"))return NextResponse.json({error:"Coordenadas inválidas"},{status:400});
 const upstream=await fetch("https://places.googleapis.com/v1/places:searchNearby",{method:"POST",headers:{"Content-Type":"application/json","X-Goog-Api-Key":key,"X-Goog-FieldMask":"places.id,places.displayName,places.location,places.formattedAddress,places.businessStatus,places.googleMapsUri"},body:JSON.stringify({includedTypes:["beauty_salon","hair_salon"],maxResultCount:20,locationRestriction:{circle:{center:{latitude:lat,longitude:lng},radius:10000}}}),cache:"no-store"});
 if(!upstream.ok)return NextResponse.json({error:"Falha ao consultar Google Places",status:upstream.status},{status:502});
 const json=await upstream.json();
 const places=(json.places||[]).filter((p:{businessStatus?:string})=>p.businessStatus!=="CLOSED_PERMANENTLY").map((p:{id:string;displayName?:{text:string};location?:{latitude:number;longitude:number};formattedAddress?:string;googleMapsUri?:string;businessStatus?:string})=>({placeId:p.id,name:p.displayName?.text||"Salão",address:p.formattedAddress||"",lat:p.location?.latitude,lng:p.location?.longitude,mapsUrl:p.googleMapsUri,status:p.businessStatus||"UNKNOWN"}));
 return NextResponse.json({places,notice:"Resultados não são lista exaustiva de salões ativos; consulte termos e custos do Google Places."},{headers:{"Cache-Control":"no-store"}});
}
