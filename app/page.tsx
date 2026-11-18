"use client";
import { useMemo, useState } from "react";
import { MapPin, Users, CalendarDays, Wallet, Radar, MessageCircle, Navigation, Clock3, Search, X } from "lucide-react";
type Status = "cliente" | "retorno" | "recusou" | "nao_visitado";
type Salao = { id: number; nome: string; cidade: string; status: Status; dias: number; saldo: number; ultimaCompra: string[]; telefone?: string };
const saloes: Salao[] = [
  { id:1,nome:"Studio Bella Hair",cidade:"João Pessoa",status:"cliente",dias:12,saldo:480,ultimaCompra:["Shampoo profissional (2 un.)","Máscara de tratamento (1 un.)"] },
  { id:2,nome:"Espaço Beleza & Cia",cidade:"Campina Grande",status:"retorno",dias:9,saldo:0,ultimaCompra:[] },
  { id:3,nome:"Salão Exemplo Centro",cidade:"Patos",status:"nao_visitado",dias:0,saldo:0,ultimaCompra:[] },
  { id:4,nome:"Studio Exemplo Norte",cidade:"Bayeux",status:"recusou",dias:18,saldo:0,ultimaCompra:[] }
];
const labels:Record<Status,string>={cliente:"Cliente",retorno:"Retornar",recusou:"Não quis atendimento",nao_visitado:"Não visitado"};
const brl=(n:number)=>n.toLocaleString("pt-BR",{style:"currency",currency:"BRL"});
export default function Home(){
 const [selected,setSelected]=useState<Salao|null>(null);const [filter,setFilter]=useState<Status|"todos">("todos");const [query,setQuery]=useState("");const [travel,setTravel]=useState(false);
 const visible=useMemo(()=>saloes.filter(s=>(filter==="todos"||s.status===filter)&&[s.nome,s.cidade].join(" ").toLowerCase().includes(query.toLowerCase())),[filter,query]);
 return <main className="shell"><header><div><small>CRM COMERCIAL</small><h1>Dal Cotone</h1></div><span className="avatar">DC</span></header>
 <nav><button className="active"><MapPin/>Mapa</button><button onClick={()=>setFilter("cliente")}><Users/>Clientes</button><button onClick={()=>alert("Agenda em desenvolvimento")}><CalendarDays/>Agenda</button><button onClick={()=>alert("Financeiro em desenvolvimento")}><Wallet/>Financeiro</button></nav>
 <section className="travel"><div><strong><Radar size={18}/> Modo Viagem</strong><p>Radar de salões • raio de 10 km</p></div><button aria-pressed={travel} onClick={()=>setTravel(!travel)}>{travel?"Ativo":"Ativar"}</button></section>
 {travel&&<p className="notice">Modo demonstrativo: busca automática e localização ainda não conectadas.</p>}
 <div className="search"><Search size={18}/><input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Buscar salão ou cidade..." /></div>
 <div className="filters">{(["todos","cliente","retorno","recusou","nao_visitado"] as const).map(v=><button key={v} className={filter===v?"chosen":""} onClick={()=>setFilter(v)}>{v==="todos"?"Todos":labels[v]}</button>)}</div>
 <section className="map"><div className="map-grid"/><div className="map-title"><MapPin size={18}/> Paraíba • Mapa demonstrativo</div>{visible.map((s,i)=><button key={s.id} className={`pin ${s.status}`} style={{left:`${18+(i*23)%70}%`,top:`${25+(i*19)%52}%`}} onClick={()=>setSelected(s)} aria-label={s.nome}>{s.nome.slice(0,2).toUpperCase()}</button>)}<div className="map-foot">Mapa ilustrativo — sem coordenadas reais</div></section>
 <section className="results"><h2>Salões ({visible.length})</h2>{visible.map(s=><button key={s.id} className="salon" onClick={()=>setSelected(s)}><span className={`dot ${s.status}`}/><span><b>{s.nome}</b><small>{s.cidade} • {labels[s.status]}</small></span><span>›</span></button>)}</section>
 {selected&&<div className="overlay" onClick={()=>setSelected(null)}><article className="drawer" onClick={e=>e.stopPropagation()}><button className="close" onClick={()=>setSelected(null)} aria-label="Fechar"><X/></button><small>FICHA COMERCIAL</small><h2>{selected.nome}</h2><p>{selected.cidade} • {labels[selected.status]}</p><div className="actions"><button disabled={!selected.telefone} onClick={()=>selected.telefone&&window.open(`https://wa.me/${selected.telefone.replace(/\D/g,"")}`,"_blank")}><MessageCircle/> WhatsApp</button><button onClick={()=>window.open(`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(selected.nome+" "+selected.cidade+" PB")}`,"_blank")}><Navigation/> Localizar</button></div><div className="info"><small>SALDO EM ABERTO (EXEMPLO)</small><h2>{brl(selected.saldo)}</h2><p>Último atendimento: {selected.dias? `há ${selected.dias} dias`:"não registrado"}</p></div><h3>Última compra</h3>{selected.ultimaCompra.length?<ul>{selected.ultimaCompra.map(x=><li key={x}>{x}</li>)}</ul>:<p>Nenhuma compra registrada.</p>}<p className="notice">Dados fictícios. Cadastro, pagamentos e comprovantes serão conectados ao banco de dados.</p></article></div>}
 </main>;
}
