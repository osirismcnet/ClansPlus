package com.cortezromeo.clansplus.listener;

import com.cortezromeo.clansplus.ClansPlus;
import com.cortezromeo.clansplus.api.storage.IClanData;
import com.cortezromeo.clansplus.api.storage.IPlayerData;
import com.cortezromeo.clansplus.clan.ClanManager;
import com.cortezromeo.clansplus.clan.EventManager;
import com.cortezromeo.clansplus.storage.PluginDataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    public PlayerDeathListener() {
        Bukkit.getPluginManager().registerEvents(this, ClansPlus.plugin);
    }

    @EventHandler
    public void onDie(PlayerDeathEvent event) {
        // Delegate to war event
        EventManager.getWarEvent().onPlayerDie(event);

        // Internal point system
        Entity entityVictim = event.getEntity();
        Entity entityKiller = event.getEntity().getKiller();

        // Victim must be in a clan to lose points
        if (!ClanManager.isPlayerInClan(entityVictim.getName())) return;

        IClanData victimClanData = PluginDataManager.getClanDatabaseByPlayerName(entityVictim.getName());
        if (victimClanData == null) return;

        // Victim's clan loses 1 point (minimum 0)
        int newVictimScore = Math.max(0, victimClanData.getScore() - 1);
        victimClanData.setScore(newVictimScore);
        PluginDataManager.saveClanDatabaseToStorage(victimClanData.getName(), victimClanData);

        // Track pointsLost for the victim player
        IPlayerData victimPlayerData = PluginDataManager.getPlayerDatabase(entityVictim.getName());
        if (victimPlayerData != null) {
            victimPlayerData.setPointsLost(victimPlayerData.getPointsLost() + 1);
            PluginDataManager.savePlayerDatabaseToStorage(entityVictim.getName(), victimPlayerData);
        }

        // If killer is a player from a different clan, killer's clan gains 1 point
        if (entityKiller == null) return;
        if (entityKiller.getType() != EntityType.PLAYER) return;

        Player killer = (Player) entityKiller;

        if (!ClanManager.isPlayerInClan(killer.getName())) return;

        IClanData killerClanData = PluginDataManager.getClanDatabaseByPlayerName(killer.getName());
        if (killerClanData == null) return;

        // Must be different clans
        if (killerClanData.getName().equals(victimClanData.getName())) return;

        killerClanData.setScore(killerClanData.getScore() + 1);
        PluginDataManager.saveClanDatabaseToStorage(killerClanData.getName(), killerClanData);

        // Track pointsGained for the killer player
        IPlayerData killerPlayerData = PluginDataManager.getPlayerDatabase(killer.getName());
        if (killerPlayerData != null) {
            killerPlayerData.setPointsGained(killerPlayerData.getPointsGained() + 1);
            PluginDataManager.savePlayerDatabaseToStorage(killer.getName(), killerPlayerData);
        }
    }

}
