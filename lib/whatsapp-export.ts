import type {Salon} from "./types";
export function mobileBR(raw:string):string|null {
 let d=String(raw||"").replace(/\D/g,"");
 if(d.startsWith("55")&&(d.length===12||d.length===13))d=d.slice(2);
 if(d.length===10&&d[2]!=="2"&&d[2]!=="3"&&d[2]!=="4"&&d[2]!=="5")d=d.slice(0,2)+"9"+d.slice(2);
 if(d.length!==11||d[2]!=="9"||!/^[1-9][1-9]/.test(d))return null;
 return d;
}
export function salonWhatsapp(s:Salon):string|null {
 const phone=mobileBR(s.phone);
 return phone?"https://wa.me/55"+phone:null;
}
export function salonLine(s:Salon,index:number):string {
 const phone=mobileBR(s.phone);
 const address=[s.address,s.city].filter(Boolean).join(", ")||"Não informado";
 return [String(index+1)+". "+s.name,"Endereço: "+address,"Telefone: "+(phone||"Não disponível"),...(phone?["WhatsApp: https://wa.me/55"+phone]:[])].join("\n");
}
export function listMessage(salons:Salon[],heading="Salões"):string {
 return ["DAL COTONE | "+heading,"Total: "+salons.length,"",...salons.map(salonLine).flatMap(s=>[s,""])].join("\n").trim();
}
export function messageChunks(salons:Salon[],heading="Salões",limit=3500):string[] {
 const chunks:string[]=[];let group:Salon[]=[];
 for(const s of salons){const next=[...group,s];if(group.length&&listMessage(next,heading).length>limit){chunks.push(listMessage(group,heading));group=[s]}else group=next}
 if(group.length)chunks.push(listMessage(group,heading));
 return chunks;
}
export function shareWhatsapp(text:string):void {
 window.open("https://api.whatsapp.com/send?text="+encodeURIComponent(text),"_blank","noopener,noreferrer");
}
