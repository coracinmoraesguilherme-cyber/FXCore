# CODE_REVIEW.md — Relatório Técnico de Auditoria de Código e Arquitetura

> **Base de Referência:** [`ARCHITECTURE.md`](ARCHITECTURE.md) (SSOT)  
> **Framework:** FXCore Framework v2  
> **Ambiente Alvo:** Minecraft 1.21.1 (NeoForge)  
> **Classificação:** Níveis Críticos (P0) até Não Críticos / Polimento (P3)

---

## 📑 Sumário Executivo

Esta auditoria de código avaliou a totalidade do repositório **FXCore** confrontando o código-fonte implementado com os requisitos estipulados no [`ARCHITECTURE.md`](ARCHITECTURE.md). Foram identificados problemas críticos de **segurança de dados, concorrência, perda de inventário de jogadores, travamento de ticks (watchdog crashes) e violações das invariantes do framework**.

### Distribuição de Severidade

| Nível de Severidade | Quantidade | Descrição / Impacto Primário |
| :--- | :---: | :--- |
| 🔴 **Nível Crítico (P0)** | **4** | Corrupção/perda de dados de jogadores, exploits de itens, travamento do servidor e quebra de regras invioláveis de arquitetura. |
| 🟠 **Nível Alto (P1)** | **6** | Condições de corrida (race conditions), memory leaks, comandos inoperantes (stubs) e bugs lógicos de entidades. |
| 🟡 **Nível Médio (P2)** | **6** | Gargalos de performance de varredura, flood de pacotes de rede, falsos positivos no AFK e acoplamento indevido do Core. |
| 🟢 **Nível Baixo (P3)** | **6** | Valores hardcoded, classes vazias, não conformidade com DoD de comandos e polimento geral. |

---

## 🔴 1. Nível Crítico (P0 — Blocker / Perda de Dados / Crash)

### 1.1 Violação da Invariante 1: I/O Bloqueante e Varredura de Disco na Main Thread
* **Arquivos Afetados:**
  * [`PlayerDataManager.java`](src/main/java/bz/fxcore/core/database/PlayerDataManager.java) (linhas 63–72, 91–101)
  * [`StaffCommands.java`](src/main/java/bz/fxcore/modules/staff/StaffCommands.java) (linhas 344, 358, 380, 416, 438)
  * [`PlayerConnectionHandler.java`](src/main/java/bz/fxcore/core/database/PlayerConnectionHandler.java) (linha 44)
  * [`FXCoreCommand.java`](src/main/java/bz/fxcore/core/otimi/FXCoreCommand.java) (linhas 162–164)
  * [`TeamManager.java`](src/main/java/bz/fxcore/modules/team/TeamManager.java) (linhas 71, 81, 89–95)
* **Confronto com a Arquitetura:**
  * A Seção 4 do [`ARCHITECTURE.md`](ARCHITECTURE.md#L181-L184) define como **REGRA INVIOLÁVEL**:  
    > *"Nenhuma operação de I/O de disco (FileReader/FileWriter/NBT Save) ou chamadas bloqueantes de rede pode ser executada na Main Thread do servidor. Use sempre FXTaskExecutor.runAsync(...)."*
* **Problemas Encontrados:**
  1. `PlayerDataManager.save(data)` é chamado de forma **síncrona na Main Thread** nos comandos `/fxs ban`, `/fxs mute`, `/fxs unmute`, `/fxs note add`, `/fxs note remove` e em `PlayerConnectionHandler.onPlayerLogin`.
  2. No comando `/fxcore reload`, há um loop que salva **todos os jogadores conectados** simultaneamente e de forma síncrona no disco.
  3. `PlayerDataManager.getUUIDByName(...)` faz `DATA_DIR.listFiles()` e lê todos os arquivos `.json` da pasta `players/` na Main Thread toda vez que comandos da staff (`/fxs info`, `/fxs history`, etc.) ou `/fxteam remove` são usados para jogadores offline.
  4. `TeamManager.save()` faz `FileWriter` síncrono no arquivo `teams.json` na Main Thread ao criar, deletar ou alterar times.
* **Impacto:** Quedas violentas de TPS, congelamentos (lag spikes) em logins/comandos e risco iminente de **Watchdog Crash** (servidor desliga forçado caso um tick passe de 60s).
* **Mitigação Recomendada:**
  * Tornar `PlayerDataManager.save()` e `TeamManager.save()` estritamente assíncronos via `FXTaskExecutor.runAsync(...)`.
  * Manter um índice em memória ou cache reverso `(PlayerName.toLowerCase() -> UUID)` para buscas rápidas sem varrer o diretório.

---

### 1.2 Destruição de Tick (98k Blocos) e Perda Total de NBT ao Restaurar Chunks
* **Arquivo Afetado:** [`FXChunkBan.java`](src/main/java/bz/fxcore/core/otimi/chunk/FXChunkBan.java) (linhas 77–120, 133–165, 170–201)
* **Problemas Encontrados:**
  1. **Loop Síncrono de 98.304 Blocos:** No Minecraft 1.21.1 (Y=-64 a Y=320, altura 384), uma chunk tem 98.304 blocos. O método `deleteChunk` itera todos os blocos na mesma tick chamando `level.setBlock(bPos, Blocks.AIR..., 2)`, serializa um JSON massivo e grava no disco de forma síncrona via `FileWriter`.
  2. **Perda Irreversível de NBT na Restauração:** O backup salva o NBT no JSON (`bObj.addProperty("nbt", tag.toString())`). Porém, em `restoreChunk`, o código **ignora totalmente o campo `nbt`** e executa apenas `level.setBlock(bPos, block.defaultBlockState(), 2)`.
  3. **Perda de Estados de Blocos:** O bloco é restaurado sem suas propriedades (direção de escadas, baús, portas, comparadores, etc. voltam para o estado padrão).
  4. **Falsa Regeneração em `regenerateChunk`:** O código chama `chunkCache.getChunk(pos.x, pos.z, ChunkStatus.FULL, true)`. Esse método **não regenera** o terreno da chunk se ela já existe; ele apenas recarrega os dados já salvos no mundo.
* **Impacto:** Crash do servidor ao tentar deletar uma chunk sob lag e **destruição total dos itens de baús, fornalhas e máquinas** ao restaurar um backup.
* **Mitigação Recomendada:**
  * Processar a limpeza fatiada por seções (sub-chunks) ou diretamente através do array de blocos do `LevelChunkSection`.
  * Em `restoreChunk`, converter a string NBT de volta via `TagParser.parseTag(nbtString)` e aplicá-la na `BlockEntity` restaurada através de `be.loadWithComponents(tag, level.registryAccess())`.

---

### 1.3 Exploit de Desaparecimento Permanente de Itens no Menu do GiveBack
* **Arquivo Afetado:** [`GiveBCommand.java`](src/main/java/bz/fxcore/modules/giveback/GiveBCommand.java) (linhas 143–198)
* **Problemas Encontrados:**
  1. O menu GUI do GiveBack utiliza um `ChestMenu` conectado a um `new SimpleContainer(54)` volátil.
  2. O método `clicked` sobrescrito apenas intercepta e consome cliques nos slots do menu (`slotId >= 0 && slotId < 54`).
  3. **Shift + Clique / Quick Move:** Se o jogador ou staff pressionar Shift + Clique em qualquer item do próprio inventário (`slotId >= 54`), o `super.clicked(...)` é executado, movendo o item do jogador para dentro do container temporário.
  4. Quando o jogador fecha o menu ou avança de página, o container volátil é destruído pelo Garbage Collector, **apagando permanentemente os itens do inventário do jogador**.
  5. A cada resgate de item, o código chama recursivamente `openRecoveryMenu(...)`, forçando um novo pacote `ClientboundOpenScreenPacket`. Se o jogador clicar repetidamente, pacotes com `containerId` divergentes causam desincronização e fechamento abrupto da interface.
* **Impacto:** Jogadores e staffs perdem itens legítimos do seu inventário por acidente ao interagir com a interface.
* **Mitigação Recomendada:**
  * Sobrescrever `quickMoveStack` para retornar `ItemStack.EMPTY`, bloqueando transferências do inventário do jogador para a GUI.
  * Atualizar os slots em tempo real usando `container.setItem(...)` e `broadcastChanges()` em vez de fechar e reabrir o menu via `viewer.openMenu(...)`.

---

### 1.4 Bloqueio Permanente de Jogadores Removidos ou que Saem de Times
* **Arquivos Afetados:**
  * [`TeamManager.java`](src/main/java/bz/fxcore/modules/team/TeamManager.java) (linhas 55–57)
  * [`TeamCommands.java`](src/main/java/bz/fxcore/modules/team/TeamCommands.java) (linhas 126, 285)
* **Problemas Encontrados:**
  * O `TeamManager` armazena as relações no mapa `PLAYER_TEAM_MAP (UUID -> TeamID)`.
  * Quando um jogador usa `/fxteam leave` ou é removido por um líder em `/fxteam remove`, o código remove o UUID da lista `team.members`, mas **esquece de remover a chave de `PLAYER_TEAM_MAP`**.
  * Consequentemente, `TeamManager.hasTeam(player.getUUID())` continuará retornando `true` indefinidamente.
* **Impacto:** Jogadores que saem ou são expulsos de uma equipe ficam permanentemente bloqueados de criar um novo time ou de aceitar convites de outros times.
* **Mitigação Recomendada:**
  * Criar um método centralizado `TeamManager.removeMember(TeamData team, UUID memberUuid)` que realize atomicamente:
    ```java
    team.members.remove(memberUuid);
    PLAYER_TEAM_MAP.remove(memberUuid);
    saveAsync();
    ```

---

## 🟠 2. Nível Alto (P1 — Falhas Lógicas, Concorrência e Memory Leaks)

### 2.1 Condição de Corrida (Race Condition) no Login de Jogadores
* **Arquivos Afetados:**
  * [`PlayerConnectionHandler.java`](src/main/java/bz/fxcore/core/database/PlayerConnectionHandler.java) (linhas 12–46)
  * [`PlayerAsyncHandler.java`](src/main/java/bz/fxcore/core/otimi/server/PlayerAsyncHandler.java) (linhas 13–18)
* **Problemas Encontrados:**
  * Existem duas classes diferentes com `@EventBusSubscriber` escutando simultaneamente o evento `PlayerLoggedInEvent`.
  * `PlayerConnectionHandler` roda na Main Thread, lê o arquivo de dados síncronamente, altera IP/histórico e grava em disco.
  * Ao mesmo tempo, `PlayerAsyncHandler` submete uma thread assíncrona ao `FXTaskExecutor` chamando `PlayerDataManager.loadPlayerData(uuid)`.
  * Não há locks nem controle de transação: uma thread pode ler o arquivo enquanto a outra está gravando, gerando corrupção de JSON ou perda da atualização de IP.
* **Mitigação Recomendada:**
  * Excluir `PlayerAsyncHandler` e concentrar todo o fluxo de conexão em `PlayerConnectionHandler`.

---

### 2.2 Despawn Impossibilitado e Acúmulo Infinito de Peitorais no Chão
* **Arquivo Afetado:** [`GiveBItemEvents.java`](src/main/java/bz/fxcore/modules/giveback/GiveBItemEvents.java) (linhas 36–38)
* **Problemas Encontrados:**
  * O método `isShulkerOrBackpackItem` contém a checagem:
    ```java
    String name = stack.getItem().toString().toLowerCase();
    return name.contains("shulker") || name.contains("backpack") || name.contains("chest");
    ```
  * O termo `"chest"` faz match com **todos os peitorais do Minecraft** (`diamond_chestplate`, `iron_chestplate`, `netherite_chestplate`, etc.) e baús normais (`chest`, `trapped_chest`).
  * Qualquer peitoral dropado por mobs (zumbis, esqueletos) ou mortes recebe `itemEntity.lifespan = Integer.MAX_VALUE` e `itemEntity.setInvulnerable(true)`.
* **Impacto:** Centenas de peitorais acumulam no chão pelo mundo e nunca somem nem queimam na lava, degradando o TPS do servidor de forma cumulativa.
* **Mitigação Recomendada:**
  * Validar tags de item específicas (`stack.is(Tags.Items.CHESTS)`) e rejeitar peitorais explicitamente (`!(stack.getItem() instanceof ArmorItem)`).

---

### 2.3 Conflito do Módulo de RP com Scoreboard Vanilla e Quebra de Times
* **Arquivo Afetado:** [`RPCommand.java`](src/main/java/bz/fxcore/modules/rp/RPCommand.java) (linhas 40–61)
* **Problemas Encontrados:**
  * No Minecraft Vanilla, um jogador **só pode pertencer a uma única `PlayerTeam` no Scoreboard por vez**.
  * Ao alternar para OFF RP via `/rp`, o jogador é adicionado à equipe `off_rp_team`. Essa ação **expulsa o jogador de sua equipe anterior** no Scoreboard (equipes de facções, minigames ou tags cosméticas).
  * Ao voltar para ON RP (`isOff == false`), o código apenas o remove da `off_rp_team`, deixando o jogador sem equipe. O vínculo original é perdido para sempre.
* **Mitigação Recomendada:**
  * Armazenar o time anterior do jogador em memória ou no `FXPlayerData` antes de vinculá-lo ao `off_rp_team`, recolocando-o na equipe original ao desativar o OFF RP.

---

### 2.4 Spawner de Partículas Vazio / Não Implementado
* **Arquivos Afetados:**
  * [`FXParticleManager.java`](src/main/java/bz/fxcore/modules/build/particles/FXParticleManager.java) (linhas 122–125)
  * [`FXBuildCommand.java`](src/main/java/bz/fxcore/modules/build/FXBuildCommand.java) (linha 67)
* **Problemas Encontrados:**
  * O comando `/fxbuild particle ...` aciona o método `FXParticleManager.startTaskAt(...)`.
  * Esse método possui apenas um comentário placeholder:
    ```java
    public static void startTaskAt(...) {
        // Se você já tiver um método principal, chame-o passando a posição 'pos'
        // Caso contrário, implemente a lógica...
    }
    ```
  * O jogador recebe a mensagem de sucesso no chat, mas nenhuma partícula é renderizada no mundo.
* **Mitigação Recomendada:**
  * Implementar a lógica em `startTaskAt` instanciando uma `ParticleTask` que suporte coordenadas fixas além da posição do jogador.

---

### 2.5 Colisão de Registro entre Comandos `/tc` de Chat e Time
* **Arquivos Afetados:**
  * [`ChannelManager.java`](src/main/java/bz/fxcore/modules/chat/ChannelManager.java) (linha 58)
  * [`ChatCommand.java`](src/main/java/bz/fxcore/modules/chat/ChatCommand.java) (linhas 53–55)
  * [`TeamChatCommand.java`](src/main/java/bz/fxcore/modules/chat/TeamChatCommand.java) (linha 19)
  * [`CommandRegistry.java`](src/main/java/bz/fxcore/core/commands/CommandRegistry.java) (linhas 34–36)
* **Problemas Encontrados:**
  * O `ChannelManager` registra por padrão um canal com comando `"tc"`.
  * O `ChatCommand` registra o comando `/tc` no Brigadier. Logo em seguida, `TeamChatCommand` registra **outro** comando `/tc`.
  * Se o jogador digitar `/tc` (sem mensagem), ele entra no canal genérico `"tc"`. Quando envia uma mensagem no chat padrão, a mensagem é distribuída globalmente (`radius = -1`) sem qualquer verificação de membros de time!
* **Mitigação Recomendada:**
  * Remover o canal `"tc"` das definições automáticas do `ChannelManager` e manter o comando `/tc` sob responsabilidade exclusiva do `TeamChatCommand`.

---

### 2.6 Vazamentos de Memória (Memory Leaks) em Caches Estáticos
* **Arquivos Afetados:**
  * [`RPManager.java`](src/main/java/bz/fxcore/modules/rp/RPManager.java) (linha 8)
  * [`PlayerDataManager.java`](src/main/java/bz/fxcore/core/database/PlayerDataManager.java) (linha 19)
* **Problemas Encontrados:**
  * O conjunto `RPManager.RP_OFF_PLAYERS` utiliza um `HashSet` estático que armazena os UUIDs de jogadores em OFF RP e **nunca remove jogadores ao desconectarem**.
  * No `PlayerDataManager`, perfis de jogadores carregados por comandos da Staff em jogadores offline nunca possuem política de expiração/evicção (não é LRU).
* **Mitigação Recomendada:**
  * Remover o jogador de `RP_OFF_PLAYERS` no evento `PlayerLoggedOutEvent` ou salvar o estado no `FXPlayerData`.
  * Utilizar um cache com expiração (ex.: Caffeine ou `WeakReference`/TTL de 10 minutos para jogadores offline).

---

## 🟡 3. Nível Médio (P2 — Performance, Redes e Acoplamento)

### 3.1 Inundação de Pacotes de Rede no `PlayerTickEvent`
* **Arquivo Afetado:** [`FXBuildManager.java`](src/main/java/bz/fxcore/modules/build/FXBuildManager.java) (linhas 84–104)
* **Problemas Encontrados:**
  * Para cada jogador que ativou `/fxbuild ptime` ou `pweather`, o servidor envia `ClientboundSetTimePacket` e `ClientboundGameEventPacket` **a cada tick (20 vezes por segundo)**.
  * Isso resulta em até **2.400 pacotes de rede por minuto por jogador**.
* **Mitigação Recomendada:**
  * No protocolo vanilla, congelar o tempo pessoal do cliente exige o envio do pacote com parâmetro de parada apenas uma única vez (ou espaçado a cada 200–600 ticks).

---

### 3.2 Falsos Positivos e Kicks Injustificados no AFK Guard
* **Arquivo Afetado:** [`SmartAFKFarm.java`](src/main/java/bz/fxcore/core/otimi/SmartAFKFarm.java) (linhas 128–139, 171)
* **Problemas Encontrados:**
  1. O evento `PlayerInteractEvent.RightClickItem` é disparado continuadamente ao comer alimentos, puxar arcos ou segurar escudos. O algoritmo identifica a taxa rápida como macro/autoclicker e cancela a ação.
  2. A checagem de inatividade exige movimentação de blocos e rotação de câmera. Jogadores parados mexendo em baús, forjando na bigorna ou conversando no chat RP serão alertados e expulsos por inatividade indevidamente.
* **Mitigação Recomendada:**
  * Ignorar itens de uso contínuo no detector de macro (`!player.isUsingItem()`).
  * Resetar a inatividade também ao enviar mensagens no chat e ao interagir com menus de containers.

---

### 3.3 Quebra de Desacoplamento: Core Diretamente Dependente de Módulo Secundário
* **Arquivo Afetado:** [`FXServerManager.java`](src/main/java/bz/fxcore/core/otimi/server/FXServerManager.java) (linha 22)
* **Problemas Encontrados:**
  * A Seção 4 (Invariante 4) do [`ARCHITECTURE.md`](ARCHITECTURE.md#L200-L203) dita que módulos devem ser independentes e o Core não deve depender diretamente de classes de módulos de negócio.
  * A classe de infraestrutura `core.otimi.server.FXServerManager` referencia e chama diretamente `bz.fxcore.modules.giveback.GiveBManager.cleanAllExpiredDrops()`.
* **Mitigação Recomendada:**
  * Fazer o módulo `giveback` assinar seu próprio `ServerTickEvent.Post` ou registrar um callback de limpeza via interface neutra no Core.

---

### 3.4 Insegurança de Threads (Thread Safety) em Coleções Compartilhadas
* **Arquivos Afetados:**
  * [`GiveBManager.java`](src/main/java/bz/fxcore/modules/giveback/GiveBManager.java) (linha 14)
  * [`TeamManager.java`](src/main/java/bz/fxcore/modules/team/TeamManager.java) (linhas 14–18)
  * [`FXCoreWarnings.java`](src/main/java/bz/fxcore/core/otimi/FXCoreWarnings.java) (linha 13)
  * [`FXBuildManager.java`](src/main/java/bz/fxcore/modules/build/FXBuildManager.java) (linhas 36–38)
* **Problemas Encontrados:**
  * Em `GiveBManager`: o mapa principal é um `ConcurrentHashMap`, mas o valor interno é um `ArrayList<StoredDrop>` comum acessado sem sincronização.
  * Em `TeamManager`: `TEAMS`, `PLAYER_TEAM_MAP` e `PENDING_INVITES` são instâncias simples de `HashMap` que são serializadas no disco ao mesmo tempo em que comandos adicionam membros.
* **Mitigação Recomendada:**
  * Utilizar `ConcurrentHashMap`, `CopyOnWriteArrayList` ou clonar as listas antes de despachar a serialização JSON para threads assíncronas.

---

### 3.5 Chunks Ocultas no `FXLagView` e Varredura Global Ineficiente no `FXChunkAnalyzer`
* **Arquivos Afetados:**
  * [`FXLagView.java`](src/main/java/bz/fxcore/core/otimi/chunk/FXLagView.java) (linhas 33–38)
  * [`FXChunkAnalyzer.java`](src/main/java/bz/fxcore/core/otimi/chunk/FXChunkAnalyzer.java) (linhas 32–39)
* **Problemas Encontrados:**
  * Em `FXLagView`, a lista `activeChunks` só considera chunks com entidades comuns. Chunks com acúmulos massivos de BlockEntities (ex.: 5.000 fornalhas ou baús) e zero monstros são completamente ignoradas pelo ranking de lag.
  * Em `FXChunkAnalyzer`, para contar as entidades de uma única chunk, o código itera `level.getAllEntities()` (todas as entidades do mundo inteiro).
* **Mitigação Recomendada:**
  * Coletar chunks ativas também a partir de `chunkSource.getLoadedChunks()`.
  * Filtrar entidades da chunk analisando a caixa de colisão da chunk (`new AABB(pos.getMinBlockX(), level.getMinBuildHeight(), pos.getMinBlockZ(), ...)`).

---

### 3.6 Configurações "Placebo" e Desconexão com a Mecânica Real
* **Arquivos Afetados:**
  * [`FXServerConfig.java`](src/main/java/bz/fxcore/core/otimi/server/FXServerConfig.java) (linhas 40–45)
  * [`FXServerManager.java`](src/main/java/bz/fxcore/core/otimi/server/FXServerManager.java) (linhas 69–75)
* **Problemas Encontrados:**
  * `mobCapMultiplier` é alterado pela otimização automática e exibido no `/fxcore server status`, mas **nunca é aplicado a nenhum evento de spawn de entidades**.
  * As opções `enableChunkLoadDelay`, `enableRedstoneGuard` e `enableDimensionUnloader` existem no arquivo JSON, mas não há código correspondente implementando essas mecânicas.
* **Mitigação Recomendada:**
  * Interceptar `FinalizeSpawnChildEvent` / `EntityJoinLevelEvent` para limitar o mobcap ou remover parâmetros não suportados da configuração.

---

## 🟢 4. Nível Baixo (P3 — Qualidade de Código, Estilo e Polimento)

### 4.1 Valor Falso e Hardcoded no Comando `/tps`
* **Arquivo Afetado:** [`SimpleCommand.java`](src/main/java/bz/fxcore/core/util/SimpleCommand.java) (linhas 21–26)
* **Problemas Encontrados:**
  * O comando `/tps` define `double averageTickTime = 50.0` e `double tps = 20.0` cravados no código. Ele sempre responderá "20.0 TPS" mesmo que o servidor esteja operando a 3 TPS.
* **Mitigação Recomendada:**
  * Conectar o comando ao cálculo dinâmico de nanos por tick do servidor que já existe em `FXServerManager`.

---

### 4.2 Arquivo Central de Configuração Vazio (`FXCoreConfig.java`)
* **Arquivo Afetado:** [`FXCoreConfig.java`](src/main/java/bz/fxcore/core/config/FXCoreConfig.java) (0 bytes)
* **Problemas Encontrados:**
  * A classe citada no mapa arquitetural da Seção 2 do [`ARCHITECTURE.md`](ARCHITECTURE.md#L57) está completamente vazia.
* **Mitigação Recomendada:**
  * Implementar as constantes e limites globais ou remover a menção no SSOT.

---

### 4.3 Desvio do Padrão DoD de Comandos Brigadier
* **Arquivos Afetados:**
  * [`SimpleCommand.java`](src/main/java/bz/fxcore/core/util/SimpleCommand.java) (linha 38)
  * [`TeamCommands.java`](src/main/java/bz/fxcore/modules/team/TeamCommands.java) (linha 299)
* **Problemas Encontrados:**
  * O checklist de Definition of Done (Seção 5.2 do [`ARCHITECTURE.md`](ARCHITECTURE.md#L233)) exige a checagem `source.getEntity() instanceof ServerPlayer`.
  * Em vários comandos, o código utiliza `source.getPlayerOrException()` dentro de blocos `try/catch (Exception e)`, usando exceções para controle de fluxo.
* **Mitigação Recomendada:**
  * Padronizar as guardas em todos os nós de comando.

---

### 4.4 Código de Cor Inválido e Variáveis Mortas
* **Arquivos Afetados:**
  * [`ChatCommand.java`](src/main/java/bz/fxcore/modules/chat/ChatCommand.java) (linhas 26, 173)
  * [`RPTickHandler.java`](src/main/java/bz/fxcore/modules/rp/RPTickHandler.java) (linha 12)
* **Problemas Encontrados:**
  * O texto de tell privado usa o prefixo `"§sEu"`. O caractere de formatação `§s` não existe na tabela do Minecraft.
  * O conjunto `SPY_ATIVOS` em `ChatCommand` é instanciado mas nunca utilizado (o sistema lê `FXPlayerData.spyEnabled`).
  * Em `RPTickHandler`, o campo `tickCounter` não tem utilidade.
* **Mitigação Recomendada:**
  * Corrigir as cores e remover campos não utilizados.

---

### 4.5 Contagem Duplicada de Membros no `/fxteam info`
* **Arquivos Afetados:**
  * [`TeamData.java`](src/main/java/bz/fxcore/modules/team/TeamData.java) (linha 28)
  * [`TeamCommands.java`](src/main/java/bz/fxcore/modules/team/TeamCommands.java) (linha 330)
* **Problemas Encontrados:**
  * O construtor de `TeamData` já adiciona o líder na lista (`this.members.add(owner)`).
  * No comando `/fxteam info`, o texto exibe `totalMembers = team.members.size() + 1`, reportando 1 membro a mais do que a realidade.
* **Mitigação Recomendada:**
  * Utilizar diretamente `team.members.size()` como a contagem total de membros.

---

### 4.6 Encerramento Abrupto da ThreadPool no Desligamento do Servidor
* **Arquivo Afetado:** [`FXTaskExecutor.java`](src/main/java/bz/fxcore/core/otimi/server/FXTaskExecutor.java) (linhas 50–52)
* **Problemas Encontrados:**
  * O método `shutdown()` apenas chama `ASYNC_EXECUTOR.shutdown()` sem aguardar o encerramento das tarefas em andamento (`awaitTermination`).
  * Tarefas de persistência em disco em execução durante o desligamento do servidor podem ser abortadas abruptamente, deixando arquivos JSON incompletos.
* **Mitigação Recomendada:**
  * Adicionar `awaitTermination(5, TimeUnit.SECONDS)` com fallback de `shutdownNow()`.

---

## 🗺️ Roadmap de Refatoração Recomendado

```mermaid
flowchart TD
    subgraph Fase 1 - Integridade e Segurança Imediata
        P0_1["Tornar Saves e Leituras Assíncronas (Invariante 1)"]
        P0_2["Corrigir NBT e Fatiamento de ChunkBan"]
        P0_3["Bloquear QuickMove (Shift+Click) no GiveBack"]
        P0_4["Corrigir Limpeza do PLAYER_TEAM_MAP"]
    end

    subgraph Fase 2 - Concorrência e Estabilidade de Redes
        P1_1["Unificar Listeners de Login (Eliminar Race Condition)"]
        P1_2["Corrigir Filtro de Armaduras em GiveBItemEvents"]
        P1_3["Espaçar Pacotes de PTime/PWeather"]
        P1_4["Resolver Conflito de /tc entre Chat e Teams"]
    end

    subgraph Fase 3 - Desacoplamento e Polimento
        P2_1["Desacoplar FXServerManager do GiveBManager"]
        P2_2["Conectar /tps ao Cálculo Real de MSPT"]
        P2_3["Implementar startTaskAt do FXParticleManager"]
        P2_4["Sanitizar Variáveis Mortas e Limpar Código"]
    end

    Fase 1 --> Fase 2 --> Fase 3
```

