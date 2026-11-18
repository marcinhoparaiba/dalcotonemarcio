create table if not exists public.push_subscriptions (
 id uuid primary key default gen_random_uuid(),
 owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
 endpoint text not null,
 subscription jsonb not null,
 created_at timestamptz not null default now(),
 unique(owner_id,endpoint)
);
create index if not exists push_owner_idx on public.push_subscriptions(owner_id);
alter table public.push_subscriptions enable row level security;
drop policy if exists push_subscription_owner on public.push_subscriptions;
create policy push_subscription_owner on public.push_subscriptions for all to authenticated
using(owner_id=auth.uid()) with check(owner_id=auth.uid());
-- Schedule the protected /api/reminders endpoint every minute using your hosting scheduler.
