package com.cortezromeo.clansplus.clan;

import com.cortezromeo.clansplus.ClansPlus;
import com.cortezromeo.clansplus.api.enums.CurrencyType;
import com.cortezromeo.clansplus.language.Messages;
import com.cortezromeo.clansplus.util.MessageUtil;
import com.cortezromeo.clansplus.util.StringUtil;
import org.bukkit.entity.Player;

public class UpgradeManager {

    public static boolean checkPlayerCurrency(Player player, CurrencyType currencyType, long value, boolean take) {
        if (currencyType == CurrencyType.VAULT) {
            if (ClansPlus.support.getVault() == null) {
                MessageUtil.throwErrorMessage("THE SERVER DOES NOT HAVE THE VAULT PLUGIN TO PERFORM THE ACTION, PLEASE CHECK AGAIN");
                player.sendMessage("Error: Vault plugin is missing, please contact the server admin immediately");
                return false;
            }
            if (ClansPlus.support.getVault().getBalance(player) >= value) {
                if (take) ClansPlus.support.getVault().withdrawPlayer(player, value);
                return true;
            }
        }
        if (currencyType == CurrencyType.PLAYERPOINTS) {
            if (ClansPlus.support.getPlayerPointsAPI() == null) {
                MessageUtil.throwErrorMessage("THE SERVER DOES NOT HAVE THE PLAYERPOINTS PLUGIN TO PERFORM THE ACTION, PLEASE CHECK AGAIN");
                player.sendMessage("Error: PlayerPoints plugin is missing, please contact the server admin immediately");
                return false;
            }
            if (ClansPlus.support.getPlayerPointsAPI().look(player.getUniqueId()) >= value) {
                if (take) ClansPlus.support.getPlayerPointsAPI().take(player.getUniqueId(), (int) value);
                return true;
            }
        }
        MessageUtil.sendMessage(player, Messages.NOT_ENOUGH_CURRENCY.replace("%currencySymbol%", StringUtil.getCurrencySymbolFormat(currencyType)).replace("%price%", String.valueOf(value)).replace("%currencyName%", StringUtil.getCurrencyNameFormat(currencyType)));
        return false;
    }

}
