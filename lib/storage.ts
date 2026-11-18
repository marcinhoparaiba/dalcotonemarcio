"use client";
import type {Salon,Visit,Appointment,Sale,Payment} from "./types";
export type CRMData={salons:Salon[];visits:Visit[];appointments:Appointment[];sales:Sale[];payments:Payment[]};
export const EMPTY_DATA:CRMData={salons:[],visits:[],appointments:[],sales:[],payments:[]};
const KEY="dalcotone-crm-local-v1";
export function loadData():CRMData{try{const value=localStorage.getItem(KEY);if(!value)return EMPTY_DATA;const raw=JSON.parse(value);return {salons:Array.isArray(raw.salons)?raw.salons:[],visits:Array.isArray(raw.visits)?raw.visits:[],appointments:Array.isArray(raw.appointments)?raw.appointments:[],sales:Array.isArray(raw.sales)?raw.sales:[],payments:Array.isArray(raw.payments)?raw.payments:[]};}catch{return EMPTY_DATA;}}
export function saveData(data:CRMData){localStorage.setItem(KEY,JSON.stringify(data));}
export function exportData(data:CRMData){const blob=new Blob([JSON.stringify(data,null,2)],{type:"application/json"});const url=URL.createObjectURL(blob);const a=document.createElement("a");a.href=url;a.download="dal-cotone-backup.json";a.click();URL.revokeObjectURL(url);}
