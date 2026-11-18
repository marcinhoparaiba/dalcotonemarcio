export type SalonStatus = "cliente" | "retorno" | "recusou" | "nao_visitado" | "inativo";
export type Salon = {id:string;name:string;city:string;address:string;phone:string;status:SalonStatus;lastContactAt?:string;notes:string;latitude?:number;longitude?:number;placeId?:string;};
export type Visit = {id:string;salonId:string;date:string;outcome:SalonStatus;notes:string;};
export type Appointment = {id:string;salonId:string;startsAt:string;notes:string;remindMinutes:number;completed:boolean;};
export type Sale = {id:string;salonId:string;date:string;items:{name:string;quantity:number;unitPrice:number}[];total:number;dueDate:string;};
export type Payment = {id:string;saleId:string;date:string;amount:number;method:"pix"|"dinheiro"|"cartao"|"transferencia";receiptName?:string;};
export const STATUS_LABEL:Record<SalonStatus,string>={cliente:"Cliente",retorno:"Retornar depois",recusou:"Não quis atendimento",nao_visitado:"Nunca visitado",inativo:"Inativa"};
