package com.cortezromeo.clansplus.inventory;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

// Skills have been removed. This class is kept as a stub.
public class SkillsMenuInventory extends ClanPlusInventoryBase {

    public SkillsMenuInventory(Player owner, String clanName, boolean fromViewClan) {
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
