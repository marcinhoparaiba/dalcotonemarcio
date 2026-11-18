# Dal Cotone CRM — produto e critérios de aceite
## Visão
CRM pessoal para representante de cosméticos profissionais com mapa como tela principal e identidade Ruby Clean (#9B111E, branco e grafite).

## Entidades
Salões, visitas, retornos, vendas com itens, pagamentos, comprovantes, usuários e locais prospectados.

## Fluxos
1. Abrir mapa e identificar salões cadastrados e oportunidades ainda não visitadas.
2. Selecionar salão, consultar endereço, WhatsApp, última compra e saldo em aberto.
3. Registrar resultado de visita: cliente, retorno, recusou, não visitado, inativo.
4. Agendar retorno com horário e lembrete 30 minutos antes; abrir rota.
5. Registrar venda, vencimento e pagamento parcial sem ultrapassar o saldo.
6. Anexar comprovante privado e consultar histórico.
7. Ativar Modo Viagem, obter consentimento de localização e consultar estabelecimentos em raio de 10 km.
8. Identificar place_id já conhecido e evitar prospectar recusas com do_not_contact.
9. Planejar visitas combinando clientes existentes e novas oportunidades, respeitando horário e distância.
10. Consultar relatórios e exportar dados.

## Qualidade exigida antes de produção
- Build e lint aprovados no CI
- Autenticação real e todas as operações com Supabase/RLS
- Testes de isolamento entre usuários e pagamentos concorrentes
- Notificações push reais e fallback de agenda
- Upload de comprovantes com URLs temporárias, limites e exclusão
- Revisão de políticas do Google Maps/Places e LGPD
- Testes em dispositivos, acessibilidade e falhas de conexão
- Não apresentar resultados de Places como lista completa de salões ativos
