package com.cortezromeo.clansplus.inventory;

import com.cortezromeo.clansplus.clan.skill.PluginSkill;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

// Skills have been removed. This class is kept as a stub.
public class UpgradePluginSkillInventory extends UpgradeSkillPaginatedInventory {

    public UpgradePluginSkillInventory(Player owner, String clanName, PluginSkill pluginSkill, boolean fromViewClan) {
        super(owner);
    }

    @Override
    public String getMenuName() { return ""; }

    @Override
    public int getSlots() { return 27; }

    @Override
    public boolean handleMenu(InventoryClickEvent event) { return true; }

    @Override
    public void setMenuItems() {}
}
