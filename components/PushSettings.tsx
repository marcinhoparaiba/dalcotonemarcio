"use client";
import {useState} from "react";
import {Bell} from "lucide-react";
import {supabaseBrowser} from "../lib/supabase";
function base64ToBytes(input:string){const padding="=".repeat((4-input.length%4)%4);const binary=atob((input+padding).replace(/-/g,"+").replace(/_/g,"/"));return Uint8Array.from(binary,c=>c.charCodeAt(0));}
export default function PushSettings(){
 const [message,setMessage]=useState(""),[busy,setBusy]=useState(false);
 const enable=async()=>{setBusy(true);try{
 if(!("serviceWorker" in navigator)||!("PushManager" in window)||!("Notification" in window))throw Error("Seu navegador não oferece suporte a notificações push.");
 if(!window.isSecureContext)throw Error("É necessário HTTPS para ativar notificações.");
 const publicKey=process.env.NEXT_PUBLIC_VAPID_PUBLIC_KEY;if(!publicKey)throw Error("Chave pública de notificações ainda não configurada.");
 const client=supabaseBrowser();if(!client)throw Error("Supabase ainda não configurado.");
 const {data:{session}}=await client.auth.getSession();if(!session)throw Error("Faça login para vincular notificações à sua conta.");
 const permission=await Notification.requestPermission();if(permission!=="granted")throw Error("Permissão de notificação não concedida.");
 const registration=await navigator.serviceWorker.register("/sw.js");await navigator.serviceWorker.ready;
 let subscription=await registration.pushManager.getSubscription();
 if(!subscription)subscription=await registration.pushManager.subscribe({userVisibleOnly:true,applicationServerKey:base64ToBytes(publicKey)});
 const response=await fetch("/api/push/subscribe",{method:"POST",headers:{"Content-Type":"application/json",Authorization:`Bearer ${session.access_token}`},body:JSON.stringify({subscription:subscription.toJSON()})});
 if(!response.ok)throw Error((await response.json()).error||"Falha ao registrar dispositivo");
 setMessage("Dispositivo cadastrado para notificações. O envio depende do agendador ativo.");
 }catch(e){setMessage(e instanceof Error?e.message:"Não foi possível ativar notificações")}finally{setBusy(false)}};
 return <div className="panel"><h3>Notificações de visitas</h3><p>Receba um aviso 30 minutos antes de cada compromisso, inclusive com o aplicativo fechado, quando seu dispositivo oferecer suporte.</p><button className="primary" disabled={busy} onClick={enable}><Bell size={17}/>{busy?"Configurando...":"Ativar notificações"}</button>{message&&<p role="status">{message}</p>}</div>;
}
