import type {Payment,Sale} from "./types";
export function paidAmount(saleId:string,payments:Payment[]){return payments.filter(p=>p.saleId===saleId).reduce((sum,p)=>sum+p.amount,0);}
export function remaining(sale:Sale,payments:Payment[]){return Math.max(0,Math.round((sale.total-paidAmount(sale.id,payments))*100)/100);}
export function balanceForSalon(salonId:string,sales:Sale[],payments:Payment[]){return sales.filter(s=>s.salonId===salonId).reduce((sum,s)=>sum+remaining(s,payments),0);}
export function validatePayment(sale:Sale,payments:Payment[],amount:number){return Number.isFinite(amount)&&amount>0&&amount<=remaining(sale,payments);}
