# Auditoria técnica inicial — 18/11/2026

## Ambiente de teste
- O container disponível tem Java 21, mas não possui Gradle instalado nem Android SDK configurado.
- Acesso direto ao github.com pelo terminal falhou por indisponibilidade de resolução DNS; os arquivos foram inspecionados via conector GitHub.
- Portanto **nenhum APK foi compilado, nenhum emulador foi executado e nenhum teste de integração foi aprovado**.

## Verificações estáticas
- Banco Room contém tabelas para salões, visitas, agenda, vendas, itens e pagamentos.
- O DAO rejeita pagamentos não positivos ou superiores ao saldo, mas não garante atomicidade entre múltiplos processos/dispositivos.
- AlarmManager oferece fallback inexato sem permissão de alarme exato.
- Backup utiliza PBKDF2-HMAC-SHA256 e AES-GCM; o código ainda precisa de testes de restauração, limites de arquivo, controle de anexos e interface.
- Google Places é chamado somente pelo botão de pesquisa, mas a chave está embutida no APK e precisa de restrições e revisão de segurança.
- A lista de salões é local; o mapa cartográfico offline não está implementado.

## Ajustes adicionados
- Receiver de reinício para reagendar lembretes após boot e atualização.
- Casos de teste JUnit para regras financeiras simples e antecedência de lembretes.
- Pipeline Android no GitHub Actions para tentar compilação e testes unitários em ambiente com SDK.

## Pendências bloqueadoras de produção
1. Confirmar pipeline verde e corrigir erros reais de compilação.
2. Testar backup criptografado, recuperação e perdas de energia.
3. Validar AlarmManager em Android 12–15 e fluxo de permissões.
4. Implementar UI de backup, restaurar e anexos.
5. Confirmar Google Places com chave restrita e políticas de dados.
6. Testar offline, migrações Room e desempenho em dispositivo.
7. Implementar mapa e roteirização offline, se desejados.
