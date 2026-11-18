# Dal Cotone CRM

CRM pessoal para representante comercial de produtos profissionais de salão, com interface centrada no mapa.

## Módulos planejados
- Mapa de salões e status: cliente, não visitado, retorno, recusou, inativo.
- Radar Modo Viagem: busca autorizada de salões em raio de 10 km, com atualização controlada.
- Cadastro e histórico de contatos, incluindo "não quis atendimento".
- Agendamentos e lembretes 30 minutos antes.
- Pedidos, itens da última compra, parcelas, pagamentos parciais e comprovantes.
- WhatsApp e navegação por endereço, com ações iniciadas pelo usuário.

## Desenvolvimento
Base Next.js + TypeScript. Integrações futuras: Supabase (Auth/Postgres/Storage), Google Maps/Places e push notifications.

**Importante:** este repositório é público. Nunca adicionar dados reais de clientes, comprovantes, tokens ou chaves. A integração com Google Places deve respeitar regras de uso, armazenamento, cotas e custos. Resultados não garantem todos os salões ativos.

## Executar
```bash
npm install
npm run dev
```
Acesse http://localhost:3000.

## Estado
Primeiro protótipo de interface com dados fictícios. Sem backend, busca de salões ou notificações reais ainda.
