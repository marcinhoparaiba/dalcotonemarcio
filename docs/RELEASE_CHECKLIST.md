# Checklist para publicação
## Já existe no código
- Protótipo Ruby Clean e cadastro local de clientes, visitas, vendas, parcelas e agenda
- Modelo relacional Supabase com RLS e armazenamento privado de comprovantes
- Endpoint protegido para consultar Google Places num raio de 10 km
- Componente de mapa com geolocalização, marcadores de clientes e oportunidades
- Manifest e service worker com tela offline (não significa sincronização offline)
## Falta integrar e validar
- Conectar todos os formulários ao Supabase com login, autorização e sincronização
- Implementar upload/download privado de comprovantes e estornos auditáveis
- Criar push subscriptions e provedor de notificações + agendamento
- Integrar componente CommercialMap na página principal e testar a interface
- Resolver duplicações por place_id e revisar limites de armazenamento do Google Places
- Revisar segurança, testes automatizados, acessibilidade e LGPD
- Configurar domínio, hospedagem, secrets e orçamento das APIs
- Testar em Android e iOS e validar build de produção
## Dependências do proprietário
- Projeto Supabase, credenciais e políticas aplicadas
- Google Cloud com faturamento, chaves restritas e APIs ativadas
- Hospedagem/ambiente de produção e domínio, se desejado
- Consentimento e base legal para dados de clientes
