# ARCHITECTURE.md — FXCore Framework v2 (SSOT)

> **Fonte Única de Verdade (SSOT) Técnica e Arquitetural**  
> Este documento define a arquitetura, padrões de projeto, fluxos de dados, invariantes e diretrizes de desenvolvimento para o mod **FXCore**.  
> Qualquer desenvolvedor ou agente mantendo ou estendendo este repositório **DEVE** seguir estas diretrizes rigorosamente.

---

## 1. Visão Geral e Escopo

| Propriedade | Descrição |
| :--- | :--- |
| **Nome do Mod** | FXCore |
| **Mod ID** | `fxcore` |
| **Pacote Base** | `bz.fxcore` |
| **Versão Base** | FXCore Framework v2 |
| **Ambiente Alvo** | Minecraft **1.21.1** |
| **Mod Loader** | **NeoForge** (21.1.x) |
| **Filosofia de Design** | **Server-Side First** (zero dependência de cliente obrigatório para jogadores) |

### Propósito do Framework
O **FXCore** é um framework modular server-side projetado para servidores SMP (Survival Multiplayer) e servidores com temática de Roleplay (RP). Ele unifica funcionalidades críticas de infraestrutura e jogabilidade em uma única suíte de alto desempenho, eliminando a sobrecarga e fragmentação de múltiplos plugins e mods:
- **Infraestrutura e Performance:** Thread pool assíncrona dedicada (`FXTaskExecutor`), monitoramento espacial e banimento/restauração de chunks com NBT íntegro, visualizador e analisador de lag limitado por AABB, e detecção inteligente de AFK com avisos progressivos no HUD.
- **Sistemas de Jogabilidade:** Sistema de Roleplay (ON/OFF RP com nametag dinâmico via Scoreboard Teams, preservação e restauração do time original do jogador, e feedback no action bar), chat multicanal dinâmico com raio de proximidade, facções/times e proteção/devolução de perdas (*GiveBack*) com proteção contra duplicações e exploits de menu.
- **Ferramentas de Moderação e Construção:** Limpeza periódica inteligente de entidades/drops (*FXClear*), ferramentas administrativas de Staff com histórico e punições temporais, e utilitários de construção sem física e renderizador de partículas geométricas configuráveis via JSON.

---

## 2. Mapa Arquitetural de Pacotes e Responsabilidades

A arquitetura do FXCore é estruturada em duas camadas principais: **Core (`bz.fxcore.core`)** e **Módulos de Domínio (`bz.fxcore.modules`)**.

```
bz.fxcore
├── FXCore.java                  # Ponto de entrada (@Mod), bootstrap de configs e ciclo de vida
├── core                         # Camada de Serviços Centrais e Infraestrutura
│   ├── commands                 # Registro centralizado de comandos Brigadier
│   │   └── CommandRegistry.java
│   ├── config                   # Configuração central do Core (JSON)
│   │   └── FXCoreConfig.java
│   ├── database                 # Persistência assíncrona de jogadores e cache em memória
│   │   ├── FXPlayerData.java
│   │   ├── PlayerConnectionHandler.java
│   │   └── PlayerDataManager.java
│   ├── otimi                    # Otimização, monitoramento de performance e tarefas
│   │   ├── chunk                # Análise espacial de chunks, lag viewer e chunk ban
│   │   │   ├── FXChunkAnalyzer.java
│   │   │   ├── FXChunkBan.java
│   │   │   └── FXLagView.java
│   │   ├── server               # Execução assíncrona desacoplada
│   │   │   └── FXTaskExecutor.java
│   │   ├── FXCoreCommand.java
│   │   ├── FXCoreWarnings.java
│   │   └── SmartAFKFarm.java    # Monitoramento de inatividade AFK com avisos HUD e kick
│   ├── staff                    # Suporte a modo manutenção e restrição de acesso
│   │   └── FXJustStaff.java
│   └── util                     # Diagnóstico de TPS/MSPT e utilitários
│       └── SimpleCommand.java
└── modules                      # Módulos de Domínio e Regras de Negócio
    ├── build                    # Construção sem física e partículas customizadas
    │   ├── FXBuildCommand.java
    │   ├── FXBuildConfig.java
    │   ├── FXBuildManager.java
    │   └── particles
    │       ├── FXParticleManager.java
    │       └── ParticleShapeJson.java
    ├── chat                     # Sistema de canais de chat, tells privados e spy
    │   ├── ChannelManager.java
    │   ├── ChatChannel.java
    │   ├── ChatCommand.java
    │   ├── ChatEventListener.java
    │   └── TeamChatCommand.java
    ├── clear                    # Limpeza periódica inteligente de itens e entidades
    │   ├── FXClearCommand.java
    │   ├── FXClearConfig.java
    │   ├── FXClearManager.java
    │   └── FXClearTask.java
    ├── giveback                 # Resgate de itens pós-morte/explosão com retenção segura
    │   ├── GiveBBlockEvents.java
    │   ├── GiveBCommand.java
    │   ├── GiveBConfig.java
    │   ├── GiveBItemEvents.java
    │   └── GiveBManager.java
    ├── rp                       # Roleplay (ON/OFF RP, nametags vanilla e action bar)
    │   ├── RPAdminCommand.java
    │   ├── RPCommand.java
    │   ├── RPManager.java
    │   └── RPTickHandler.java
    ├── staff                    # Ferramentas administrativas e punições temporais
    │   ├── FXStaffData.java
    │   ├── StaffCommands.java
    │   ├── StaffEvents.java
    │   └── TimeUtil.java
    └── team                     # Facções e equipes de jogadores
        ├── TeamCommands.java
        ├── TeamData.java
        └── TeamManager.java
```

### Detalhamento das Responsabilidades

#### Camada Core (`bz.fxcore.core`)
1. **`core.commands.CommandRegistry`:** Ponto central de montagem da árvore do Brigadier. Todos os comandos do mod convergem para esta classe no evento `RegisterCommandsEvent`.
2. **`core.config.FXCoreConfig`:** Configuração central (`config/fxcore/core.json`) com controle de alertas e parâmetros operacionais do AFK (`maxAfkSeconds`, `ignoreStaff`, mensagens personalizadas).
3. **`core.database`:**
   - **`FXPlayerData`:** DTO contendo histórico de IPs, status de freeze, spy, punições (ban/mute), notas da staff e o campo `previousTeam` (para restauração do Scoreboard ao alternar RP).
   - **`PlayerDataManager`:** Gerencia persistência em `config/fxcore/data/players/<UUID>.json`. Opera com escritas assíncronas via `FXTaskExecutor.runAsync(...)`, escrita síncrona segura no desligamento (`saveSync`), cache de instâncias ativas (`CACHE`) e cache reverso em memória (`NAME_TO_UUID_CACHE`) para resolver nomes de jogadores sem acesso ao disco na Main Thread.
   - **`PlayerConnectionHandler`:** Listener unificado que carrega os dados no login, aplica punições ativas e descarrega os dados do cache (`evict`) no logout para prevenir memory leaks.
4. **`core.otimi.server.FXTaskExecutor`:** Pool dedicado de threads assíncronas (`FXCore-AsyncThread-X`). No desligamento (`shutdown()`), aguarda até 5 segundos (`awaitTermination`) para garantir que todas as escritas pendentes no disco sejam concluídas sem perda de dados.
5. **`core.otimi.SmartAFKFarm`:** Monitoramento puro de AFK: detecta inatividade de movimento e ângulo de visão, reseta tempo ao falar no chat ou abrir containers, emite avisos no action bar com efeito sonoro e desconecta jogadores ociosos com kick seguro.
6. **`core.otimi.chunk`:**
   - **`FXChunkAnalyzer`:** Análise quantitativa de entidades e BlockEntities delimitada pela caixa espacial (`AABB`) da chunk, sem varredura pesada no mundo todo.
   - **`FXChunkBan`:** Remoção segura de blocos de chunks com salvamento assíncrono de backup JSON e restauração completa com suporte a NBT de `BlockEntity` via `TagParser`.
   - **`FXLagView`:** Relatório de ranking das chunks mais pesadas da dimensão, varrendo ativamente a área de visualização dos jogadores online para identificar fábricas e acúmulos de TileEntities mesmo sem monstros vivos presentes.
7. **`core.staff.FXJustStaff`:** Modo de manutenção (`/fxcore juststaff <on|off>`) que desconecta jogadores não-staff usando lista isolada para prevenir `ConcurrentModificationException`, com suporte a comandos originados tanto por jogadores quanto pelo Console.
8. **`core.util.SimpleCommand`:** Comandos utilitários como `/tps` (calculado dinamicamente com base em `server.getAverageTickTimeNanos()`, exibindo TPS e MSPT reais), `/playtime` e `/repair`.

#### Camada de Módulos (`bz.fxcore.modules`)
1. **`modules.build`:** Modo de construção sem física (`/fxbuild place`) e gerenciador de partículas geométricas (`/fxbuild particle`) com interpolação de frames e animações (rotação/pulso) via `FXParticleManager` e `ParticleShapeJson`.
2. **`modules.chat`:** Canais dinâmicos de chat (`l`, `g`, `s`), comandos de tell (`/msg`, `/tell`, `/w`, `/r`), visualização administrativa (`/spy`) e chat isolado de equipe (`/tc`).
3. **`modules.clear`:** Limpeza periódica automatizada de itens no chão com contagem regressiva e avisos sonoros no chat.
4. **`modules.giveback`:** Proteção contra perda de itens por quebra e explosão de containers. Interface de resgate (`/fxgiveback`) blindada contra shift-click (`quickMoveStack` retornando `EMPTY`) e atualizações de slots in-place, imunidade restrita a shulkers/mochilas legítimas e rotina autônoma de limpeza de retenção expirada.
5. **`modules.rp`:** Alternância de Roleplay (`/rp`, `/rpadm`). Altera a visualização do nametag via Scoreboard Vanilla (`off_rp_team`), preservando o time anterior do jogador no perfil (`FXPlayerData.previousTeam`) e restaurando-o automaticamente ao reativar o RP.
6. **`modules.staff`:** Punições de ban/mute temporais ou permanentes (`/kick`, `/ban`, `/mute`, `/freeze`, `/unban`, `/unmute`, `/unfreeze`), com verificação no login e envio de mensagens pelo chat.
7. **`modules.team`:** Sistema de criação e gestão de facções/equipes (`/fxteam` e `/fxteams`). Sincronização concorrente entre membros, mapa de pertencimento (`PLAYER_TEAM_MAP`) e persistência assíncrona em JSON.

---

## 3. Ciclo de Vida e Fluxos de Dados Críticos

### 3.1 Inicialização e Bootstrap do Mod
```
[FXCore Constructor]
   ├── modEventBus.addListener(setup)
   ├── FXCoreConfig.load()
   ├── FXBuildConfig.load()
   ├── GiveBConfig.load()
   ├── FXClearConfig.load()
   ├── ChannelManager.loadChannels()
   ├── ParticleShapeJson.loadShapes(configDir)
   ├── NeoForge.EVENT_BUS.addListener(onRegisterCommands)
   └── NeoForge.EVENT_BUS.addListener(onServerStopping)
            ↓
[FMLCommonSetupEvent]
   ├── PlayerDataManager.init() (cria diretórios e carrega mapa de nomes)
   └── TeamManager.init() (carrega times e mapeamento reverso de membros)
```

### 3.2 Ciclo de Tick do Servidor e Tarefas Periódicas
- O `FXCore` opera com eventos desacoplados anotados com `@EventBusSubscriber(modid = FXCore.MODID)`:
  - **`FXClearTask`:** Dispara a contagem e limpeza de drops a cada intervalo configurado.
  - **`GiveBManager`:** Varre a retenção de drops a cada 100 ticks (5s) para remover itens expirados da memória.
  - **`FXParticleManager`:** Atualiza a física e renderização das partículas ativas a cada tick no servidor.
  - **`SmartAFKFarm`:** Monitora a ociosidade dos jogadores a cada 20 ticks (1 segundo), emitindo avisos no action bar e aplicando desconexão se o tempo for excedido.
  - **`RPTickHandler`:** Emite aviso visual no action bar (`§c§lOFF RP`) para jogadores com RP desativado a cada 20 ticks.

### 3.3 Ciclo de Vida de Dados do Jogador (`PlayerDataManager`)
```
[PlayerLoggedInEvent]
   ├── PlayerConnectionHandler detecta conexão
   ├── PlayerDataManager.loadPlayerData(uuid) (lê JSON e insere em CACHE)
   ├── Registra nome -> UUID em NAME_TO_UUID_CACHE
   └── Se isBanned == true: desconecta jogador com motivo e tempo restante
            │
      [Durante o Jogo]
         └── Acessos via PlayerDataManager.get(uuid) em memória
            │
[PlayerLoggedOutEvent]
   ├── PlayerDataManager.save(data) (envio assíncrono para FXTaskExecutor)
   └── PlayerDataManager.evict(uuid) (remove de CACHE para evitar memory leak)
```

### 3.4 Fluxo do Sistema de Roleplay (ON / OFF RP) e Scoreboard
```
[Comando /rp ou /rpadm toggle]
   │
   ├── RPManager.toggle(uuid)
   │
   └── RPCommand.updateNameTag(player, isOff):
         ├── Obtém Scoreboard do servidor
         ├── Obtém time "off_rp_team" com prefixo "§c[OFF RP] "
         │
         ├── Se isOff == true:
         │     ├── Salva o time atual do jogador em FXPlayerData.previousTeam
         │     ├── Salva perfil assincronamente via PlayerDataManager.save()
         │     └── Adiciona o jogador ao "off_rp_team"
         │
         └── Se isOff == false:
               ├── Remove o jogador de "off_rp_team"
               ├── Se existia previousTeam:
               │     └── Readiciona jogador ao seu time original do Scoreboard
               └── Limpa o campo previousTeam no perfil do jogador
```

---

## 4. Invariantes e Regras de Ouro (O que NUNCA quebrar)

### 1. Invariante de Performance e Assincronia
- **REGRA INVIOLÁVEL:** **Nenhuma operação de I/O de disco (FileReader/FileWriter/GSON serialization/deserialization)** pode ser realizada na Main Thread do Minecraft. Todas as escritas devem ser delegadas ao `FXTaskExecutor.runAsync(...)`.
- **Modificações de Estado de Jogo na Main Thread:** Operações de mutação de blocos (`level.setBlock`), spawn de entidades, teleporte e alterações de `Scoreboard` devem permanecer na Main Thread do servidor.

### 2. Padrão Server-Side Friendly
- O mod não deve exigir arquivos ou mods instalados no lado do cliente (Minecraft Vanilla compatível). Todos os recursos de interface utilizam menus Vanilla (`ChestMenu`), pacotes de action bar, títulos, sons e partículas vanilla.

### 3. Invariante de Menus Customizados (Anti-Exploit)
- Menus que utilizam inventários temporários ou virtuais (como o `/fxgiveback`) **DEVEM OBRIGATORIAMENTE** sobrescrever `quickMoveStack` retornando `ItemStack.EMPTY`. Isso impede que cliques com Shift transfiram itens da mão/inventário do jogador para contêineres descartáveis.
- Atualizações de itens em interfaces GUI devem ser realizadas via mutação dos slots do container seguida de `broadcastChanges()`, sem fechar e reabrir menus continuamente na cara do jogador.

### 4. Invariante de Times e Scoreboards
- No Minecraft Vanilla, cada jogador pode pertencer a **apenas um** `PlayerTeam` no `Scoreboard`. Ao aplicar times utilitários (como `off_rp_team`), o time original anterior deve ser persistido (em `FXPlayerData.previousTeam`) para ser restaurado intacto quando a condição for desfeita.

### 5. Invariante de Busca Espacial de Entidades
- **Nunca** utilize `level.getAllEntities()` para filtros baseados em posições de bloco ou chunk. Utilize consultas indexadas espacialmente via `level.getEntities(..., AABB, ...)`, garantindo complexidade $O(\text{entidades na área})$ em vez de varreduras lineares por todo o mundo.

### 6. Desacoplamento entre Módulos
- Módulos em `bz.fxcore.modules.*` não devem acoplar dependências internas diretas entre si. Comunicações transversais devem ocorrer através de eventos, classes do `core` ou de DTOs compartilhados.

---

## 5. Guia de Extensão para Novas Features e Módulos

### 5.1 Criando um Novo Módulo
1. **Pacote:** Crie o subpacote sob `bz.fxcore.modules.<modulo>`.
2. **Configuração (se aplicável):** Crie `<Modulo>Config.java` com carregamento JSON sob `config/fxcore/<modulo>.json`.
3. **Gerenciador de Estado:** Crie `<Modulo>Manager.java` utilizando coleções thread-safe (`ConcurrentHashMap`).
4. **Comandos:** Crie a classe de comando com Brigadier e registre a chamada dentro de `bz.fxcore.core.commands.CommandRegistry.java`.
5. **Eventos:** Anote as classes ouvintes de eventos com `@EventBusSubscriber(modid = FXCore.MODID)` e especifique o método com `@SubscribeEvent`.

### 5.2 Checklist de Definition of Done (DoD)
Antes de finalizar qualquer alteração ou adição no repositório:
- [ ] **Sem I/O na Main Thread:** Operações de leitura e escrita pesadas delegadas a `FXTaskExecutor.runAsync()`.
- [ ] **Validação de Emissor:** Comandos checam `source.getEntity() instanceof ServerPlayer` e permissões com `source.hasPermission(...)`.
- [ ] **Registro Centralizado:** Comando registrado no `CommandRegistry`.
- [ ] **Thread-Safety:** Variáveis estáticas de controle usam estruturas concorrentes (`ConcurrentHashMap`, `newKeySet()`).
- [ ] **Anti-Exploit em GUIs:** `quickMoveStack` bloqueado quando aplicável.
- [ ] **Compilação Limpa:** `.\gradlew.bat compileJava --no-daemon` executado e aprovado sem warnings críticos.
