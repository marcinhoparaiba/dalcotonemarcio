# Notificações push reais — ativação
1. Aplicar `supabase/schema.sql` e `supabase/push.sql` no banco Supabase.
2. Instalar dependência `web-push` e publicar o aplicativo em HTTPS.
3. Gerar chaves VAPID com `npx web-push generate-vapid-keys`. **Nunca enviar a chave privada ao GitHub.**
4. Configurar `NEXT_PUBLIC_VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY`, `VAPID_SUBJECT=mailto:email@dominio.com`, `SUPABASE_SERVICE_ROLE_KEY` e `CRON_SECRET` no host.
5. Configurar agendador HTTPS para chamar `GET /api/reminders` a cada minuto com `Authorization: Bearer <CRON_SECRET>`.
6. Entrar na conta Supabase e ativar notificações no aplicativo, no dispositivo desejado.
7. No iPhone, instalar o PWA na Tela de Início antes de autorizar notificações. Suporte depende da versão do sistema.
8. Testar com compromisso marcado para daqui a 31–35 minutos, acompanhar logs e confirmar recebimento.
9. A entrega depende do navegador, conectividade, políticas do sistema e serviço push; não há garantia de entrega no minuto exato.
10. Não incluir valores de dívidas ou dados sensíveis no texto de notificações exibidas na tela bloqueada.
