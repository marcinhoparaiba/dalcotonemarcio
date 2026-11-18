import type {Salon,Visit,Sale,Payment} from "./types";
import {remaining} from "./finance";
export function crmReport(salons:Salon[],visits:Visit[],sales:Sale[],payments:Payment[]){
 const now=Date.now(),month=new Date().toISOString().slice(0,7);
 const monthlySales=sales.filter(s=>s.date.startsWith(month));
 const due=sales.filter(s=>remaining(s,payments)>0);
 return {
  salons:salons.length,clients:salons.filter(s=>s.status==="cliente").length,
  unvisited:salons.filter(s=>s.status==="nao_visitado").length,
  visitsThisMonth:visits.filter(v=>v.date.startsWith(month)).length,
  salesThisMonth:monthlySales.reduce((n,s)=>n+s.total,0),
  outstanding:due.reduce((n,s)=>n+remaining(s,payments),0),
  overdue:due.filter(s=>s.dueDate&&new Date(s.dueDate+"T23:59:59").getTime()<now).reduce((n,s)=>n+remaining(s,payments),0),
  neglected:salons.filter(s=>s.status==="cliente"&&(!s.lastContactAt||now-Date.parse(s.lastContactAt)>14*86400000)).length
 };
}
