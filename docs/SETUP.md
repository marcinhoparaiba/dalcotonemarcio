# Configuração das integrações
1. Criar projeto Supabase e executar `supabase/schema.sql` no SQL Editor.
2. Ativar login por email no Supabase Auth. Configurar URLs de redirecionamento.
3. Configurar variáveis de `.env.example` localmente e no host. Nunca enviar valores ao GitHub.
4. Criar projeto Google Cloud com cobrança e limites de orçamento. Ativar Maps JavaScript API e Places API (New). Restringir a chave de navegador por HTTP referrers e a chave de servidor por API e ambiente.
5. A API `/api/places?lat=...&lng=...` requer bearer token Supabase e retorna até 20 estabelecimentos por busca de 10 km. A cobertura não é completa e não deve ser copiada em massa. Evitar persistir conteúdo do Places sem verificar as condições de uso.
6. O endpoint `/api/reminders` é um esqueleto protegido por CRON_SECRET. Ainda não envia notificações. Para notificações reais, implementar push subscriptions, autorização, agendador e tratamento de fuso horário.
7. O protótipo atual usa armazenamento local. A migração para operações autenticadas em Supabase ainda deve ser implementada na interface antes de inserir dados reais.
8. Não use o protótipo com informações financeiras reais até validar autenticação, backups, controle de acesso e privacidade.
