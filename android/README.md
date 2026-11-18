# Dal Cotone CRM — Android Offline First

Aplicativo Android nativo em Kotlin, Jetpack Compose e Room (SQLite). Os registros são locais e não exigem Supabase.

## Primeira implementação
- Cadastro de salões, filtro e busca offline.
- Histórico de atendimentos e status de cliente/retorno/recusa.
- Vendas com produtos, pagamentos parciais e saldo calculado em centavos.
- Agenda e lembrete local via AlarmManager (com fallback inexato).
- Busca sob demanda pelo Google Places (New) a 10 km, com permissão de localização.
- A interface inicial é uma lista comercial; mapa cartográfico offline ainda não está implementado.

## Construção
Abrir a pasta `android` no Android Studio (JDK 17), sincronizar Gradle e executar em um aparelho Android 8+.
Para Google Places, configurar `PLACES_API_KEY` em `~/.gradle/gradle.properties` ou via propriedade Gradle. A chave no APK pode ser extraída; antes de produção, aplicar restrições de API e de aplicativo Android no Google Cloud e validar compatibilidade com Places REST. A integração não deve ser considerada segura até essa revisão.

## Limitações atuais
- Não foi executado build/teste Android nesta sessão.
- O Android pode atrasar alarmes se o acesso a alarmes exatos não estiver autorizado; ainda faltam fluxo de permissão e reagendamento após reinício do aparelho.
- O aplicativo ainda não oferece backup criptografado, anexos de comprovantes, importação, mapa cartográfico ou otimização de rotas.
- A consulta Places usa última posição conhecida; pode exigir atualização manual do GPS.
- Não inserir dados financeiros reais antes dos testes, backup criptografado e revisão de segurança.
- Os dados locais são apagados se o usuário desinstalar o app ou limpar seus dados; `allowBackup=false` evita backup automático não criptografado.
