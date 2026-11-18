-- Run in Supabase SQL editor. Private, per-user records protected by RLS.
create extension if not exists pgcrypto;
create table if not exists public.salons (
 id uuid primary key default gen_random_uuid(), owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
 name text not null, city text not null default '', address text not null default '', phone text not null default '',
 status text not null default 'nao_visitado' check(status in ('cliente','retorno','recusou','nao_visitado','inativo')),
 notes text not null default '', last_contact_at timestamptz, latitude double precision, longitude double precision,
 place_id text, do_not_contact boolean not null default false, created_at timestamptz not null default now(),
 unique(owner_id,place_id)
);
create table if not exists public.visits (
 id uuid primary key default gen_random_uuid(), owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
 salon_id uuid not null references public.salons(id) on delete cascade, visited_at timestamptz not null default now(),
 outcome text not null, notes text not null default ''
);
create table if not exists public.appointments (
 id uuid primary key default gen_random_uuid(), owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
 salon_id uuid not null references public.salons(id) on delete cascade, starts_at timestamptz not null,
 remind_minutes int not null default 30 check(remind_minutes between 0 and 1440),
 notes text not null default '', completed boolean not null default false, notified_at timestamptz
);
create table if not exists public.sales (
 id uuid primary key default gen_random_uuid(), owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
 salon_id uuid not null references public.salons(id) on delete cascade, sold_at timestamptz not null default now(),
 items jsonb not null default '[]'::jsonb, total numeric(12,2) not null check(total>=0), due_date date
);
create table if not exists public.payments (
 id uuid primary key default gen_random_uuid(), owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
 sale_id uuid not null references public.sales(id) on delete cascade, paid_at timestamptz not null default now(),
 amount numeric(12,2) not null check(amount>0), method text not null check(method in ('pix','dinheiro','cartao','transferencia')),
 receipt_path text
);
create index if not exists salons_owner_idx on public.salons(owner_id);
create index if not exists appointments_owner_start_idx on public.appointments(owner_id,starts_at);
create index if not exists sales_owner_idx on public.sales(owner_id);
create index if not exists payments_owner_idx on public.payments(owner_id);
alter table public.salons enable row level security;
alter table public.visits enable row level security;
alter table public.appointments enable row level security;
alter table public.sales enable row level security;
alter table public.payments enable row level security;
do $$ declare t text; begin
 foreach t in array array['salons','visits','appointments','sales','payments'] loop
  execute format('drop policy if exists owner_access on public.%I',t);
  execute format('create policy owner_access on public.%I for all to authenticated using (owner_id = auth.uid()) with check (owner_id = auth.uid())',t);
 end loop;
end $$;
-- Guard against forged cross-account references on child rows.
create or replace function public.verify_crm_parent_owner() returns trigger language plpgsql security definer set search_path=public as $$
declare parent_owner uuid;
begin
 if TG_TABLE_NAME in ('visits','appointments','sales') then
   select owner_id into parent_owner from public.salons where id=NEW.salon_id;
 elsif TG_TABLE_NAME='payments' then
   select owner_id into parent_owner from public.sales where id=NEW.sale_id;
 end if;
 if parent_owner is distinct from NEW.owner_id then raise exception 'Parent record owner mismatch'; end if;
 return NEW;
end $$;
do $$ declare t text; begin
 foreach t in array array['visits','appointments','sales','payments'] loop
  execute format('drop trigger if exists verify_parent on public.%I',t);
  execute format('create trigger verify_parent before insert or update on public.%I for each row execute function public.verify_crm_parent_owner()',t);
 end loop;
end $$;
-- Ensure total payments cannot exceed sale total, including concurrent inserts.
create or replace function public.validate_payment_total() returns trigger language plpgsql security definer set search_path=public as $$
declare sale_total numeric(12,2); paid numeric(12,2);
begin
 select total into sale_total from public.sales where id=NEW.sale_id for update;
 select coalesce(sum(amount),0) into paid from public.payments where sale_id=NEW.sale_id and id<>NEW.id;
 if paid+NEW.amount>sale_total then raise exception 'Payment exceeds outstanding balance'; end if;
 return NEW;
end $$;
drop trigger if exists payment_total_guard on public.payments;
create trigger payment_total_guard before insert or update on public.payments for each row execute function public.validate_payment_total();
insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values ('receipts','receipts',false,10485760,array['image/jpeg','image/png','application/pdf'])
on conflict(id) do nothing;
drop policy if exists receipt_owner_access on storage.objects;
create policy receipt_owner_access on storage.objects for all to authenticated
using(bucket_id='receipts' and (storage.foldername(name))[1]=auth.uid()::text)
with check(bucket_id='receipts' and (storage.foldername(name))[1]=auth.uid()::text);
