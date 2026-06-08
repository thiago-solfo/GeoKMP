# GeoKMP
App de localização em tempo real que funciona tanto em Android quanto em iOS.

## Problemas Resolvidos
1. **Erro de versão no GPS**: A ferramenta utilizada para obter a localização exigia uma configuração de precisão obrigatória. O problema foi resolvido através da passagem explícita do parâmetro de `accuracy` na inicialização do serviço.
2. **Pedido de localização não reconhecido**: O sistema não identificava as requisições de GPS por ausência de módulos específicos. A solução foi a inclusão da dependência de `moko-permissions-location` no projeto compartilhado.
3. **Dados de coordenadas travados**: O aplicativo tentava ler a latitude e longitude de uma estrutura incompatível. O acesso aos dados foi corrigido através da refatoração do fluxo de coleta do objeto `LatLng`.
4. **Botão de permissão que não respondia**: Após múltiplas negativas, o sistema bloqueia novos diálogos de autorização. O erro foi tratado com a captura da exceção `DeniedAlwaysException` e redirecionamento para as configurações do sistema.

## Tecnologias Usadas
- **Kotlin Multiplatform**: Para compartilhar o código entre sistemas.
- **Compose Multiplatform**: Para criar a interface visual única.
- **Moko Geo & Permissions**: Para lidar com o GPS e autorizações de segurança.
