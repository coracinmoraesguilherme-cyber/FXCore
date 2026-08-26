package bz.fxcore.fxunify;

public class FXUnifyConfig {

    public static Data DATA = new Data();

    public static class Data {
        public int retentionTimeMinutes = 60;
        public int maxItemsBeforeTransfer = 1000;
        
        // Novo limite máximo acumulado por TIPO de item (256K = 262.144)
        public long maxItemsPerType = 262144L;

        public String playerGuiTitle = "&8Seus Itens Recuperáveis (Pág. %page%/%max%)";
        public String staffGuiTitle = "&8Itens de %player% (Pág. %page%/%max%)";
        public String emptyInventoryMessage = "&cVocê não possui itens para resgatar.";
        public String staffNoItemsMessage = "&cO jogador %player% não possui itens para resgatar.";
        public String staffClearSuccessMessage = "&aOs itens de %player% foram limpos com sucesso.";
        public String lagProtectionMessage = "&e[AntiLag] Este container continha mais de %count% itens. Eles foram movidos com segurança para o seu /fxgiveback!";
        
        public String staffAlertMessage = "&c[AntiLag Staff] O jogador %player% quebrou um container com %count% itens. Movidos para o /fxgiveback.";
        
        // Alerta para quebras ambientais ou explosões
        public String staffWorldAlertMessage = "&c[AntiLag Staff] Um container em %pos% foi destruído por %reason% com %count% itens. Itens enviados para o /fxgiveback do dono/último criador.";
    }
}