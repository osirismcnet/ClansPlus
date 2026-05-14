package com.cortezromeo.clansplus.inventory;

import com.cortezromeo.clansplus.ClansPlus;
import com.cortezromeo.clansplus.Settings;
import com.cortezromeo.clansplus.api.enums.CurrencyType;
import com.cortezromeo.clansplus.api.enums.ItemType;
import com.cortezromeo.clansplus.api.enums.Rank;
import com.cortezromeo.clansplus.api.enums.Subject;
import com.cortezromeo.clansplus.api.storage.IClanData;
import com.cortezromeo.clansplus.clan.ClanManager;
import com.cortezromeo.clansplus.clan.UpgradeManager;
import com.cortezromeo.clansplus.clan.subject.Spawn;
import com.cortezromeo.clansplus.file.UpgradeFile;
import com.cortezromeo.clansplus.file.inventory.ClanMenuInventoryFile;
import com.cortezromeo.clansplus.language.Messages;
import com.cortezromeo.clansplus.storage.PluginDataManager;
import com.cortezromeo.clansplus.util.ItemUtil;
import com.cortezromeo.clansplus.util.MessageUtil;
import com.cortezromeo.clansplus.util.StringUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ClanMenuInventory extends ClanPlusInventoryBase {

    FileConfiguration fileConfiguration = ClanMenuInventoryFile.get();

    public ClanMenuInventory(Player owner) {
        super(owner);
    }

    @Override
    public void open() {
        if (PluginDataManager.getClanDatabaseByPlayerName(getOwner().getName()) == null) {
            MessageUtil.sendMessage(getOwner(), Messages.MUST_BE_IN_CLAN);
            getOwner().closeInventory();
            return;
        }
        super.open();
    }

    @Override
    public String getMenuName() {
        String title = fileConfiguration.getString("title");
        String playerClanName = PluginDataManager.getPlayerDatabase(getOwner().getName()).getClan();
        if (playerClanName != null)
            title = StringUtil.setClanNamePlaceholder(title, playerClanName);
        return ClansPlus.nms.addColor(title);
    }

    @Override
    public int getSlots() {
        int rows = fileConfiguration.getInt("rows") * 9;
        if (rows < 27 || rows > 54)
            return 54;
        return rows;
    }

    @Override
    public boolean handleMenu(InventoryClickEvent event) {
        if (!super.handleMenu(event))
            return false;

        if (PluginDataManager.getClanDatabaseByPlayerName(getOwner().getName()) == null) {
            MessageUtil.sendMessage(getOwner(), Messages.MUST_BE_IN_CLAN);
            getOwner().closeInventory();
            return false;
        }

        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null || itemStack.getType() == org.bukkit.Material.AIR) return true;
        String itemCustomData = ClansPlus.nms.getCustomData(itemStack);

        playClickSound(fileConfiguration, itemCustomData);

        if (itemCustomData.equals("close"))
            getOwner().closeInventory();
        if (itemCustomData.equals("members"))
            new MembersMenuInventory(getOwner()).open();
        if (itemCustomData.equals("clanList"))
            new ClanListInventory(getOwner()).open();
        if (itemCustomData.equals("allies"))
            new AlliesMenuInventory(getOwner()).open();
        if (itemCustomData.equals("upgradeMaxMember")) {
            IClanData playerClanData = PluginDataManager.getClanDatabaseByPlayerName(getOwner().getName());
            if (playerClanData == null) return true;
            Rank upgradeRequiredRank = Settings.CLAN_SETTING_PERMISSION_DEFAULT.get(Subject.UPGRADE);
            if (!Settings.CLAN_SETTING_PERMISSION_DEFAULT_FORCED)
                upgradeRequiredRank = PluginDataManager.getClanDatabase(playerClanData.getName()).getSubjectPermission().get(Subject.UPGRADE);
            if (!ClanManager.isPlayerRankSatisfied(getOwner().getName(), upgradeRequiredRank)) {
                MessageUtil.sendMessage(getOwner(), Messages.REQUIRED_RANK.replace("%requiredRank%", ClanManager.getFormatRank(upgradeRequiredRank)));
                return true;
            }
            CurrencyType ct = CurrencyType.valueOf(UpgradeFile.get().getString("upgrade.max-members.currency-type").toUpperCase());
            int newMaxMembers = playerClanData.getMaxMembers() + 1;
            long value = UpgradeFile.get().getLong("upgrade.max-members.price." + newMaxMembers);
            if (!UpgradeFile.get().getConfigurationSection("upgrade.max-members.price").getKeys(false).contains(String.valueOf(newMaxMembers)))
                value = UpgradeFile.get().getLong("upgrade.max-members.price.else");
            if (UpgradeManager.checkPlayerCurrency(getOwner(), ct, value, true)) {
                playerClanData.setMaxMembers(newMaxMembers);
                PluginDataManager.saveClanDatabaseToStorage(playerClanData.getName(), playerClanData);
                ClanManager.alertClan(playerClanData.getName(), Messages.CLAN_BROADCAST_UPGRADE_MAX_MEMBERS
                        .replace("%player%", getOwner().getName())
                        .replace("%rank%", ClanManager.getFormatRank(PluginDataManager.getPlayerDatabase(getOwner().getName()).getRank()))
                        .replace("%newMaxMembers%", String.valueOf(playerClanData.getMaxMembers())));
                super.open();
            }
        }
        if (itemCustomData.equals("events"))
            new EventsMenuInventory(getOwner()).open();
        if (itemCustomData.equals("settings"))
            new ClanSettingsInventory(getOwner()).open();
        if (itemCustomData.equals("spawn"))
            new Spawn(Settings.CLAN_SETTING_PERMISSION_DEFAULT.get(Subject.SPAWN), getOwner(), getOwner().getName()).execute();
        if (itemCustomData.equals("leave"))
            new LeaveConfirmationInventory(getOwner()).open();
        if (itemCustomData.equals("storage")) {
            if (Settings.STORAGE_SETTINGS_ENABLED)
                new StorageListInventory(getOwner()).open();
            else
                MessageUtil.sendMessage(getOwner(), Messages.FEATURE_DISABLED);
        }

        return true;
    }

    @Override
    public void setMenuItems() {
        ClansPlus.support.getFoliaLib().getScheduler().runAsync(task -> {

            addBasicButton(fileConfiguration, false);

            IClanData clanData = PluginDataManager.getClanDatabaseByPlayerName(getOwner().getName());

            if (fileConfiguration.getBoolean("items.members.enabled", true)) {
                List<String> membersItemLore = new ArrayList<>();
                for (String lore : fileConfiguration.getStringList("items.members.lore")) {
                    lore = lore.replace("%totalMembers%", String.valueOf(clanData.getMembers().size()));
                    membersItemLore.add(lore);
                }
                ItemStack membersClanItem = ClansPlus.nms.addCustomData(ItemUtil.getItem(
                        ItemType.valueOf(fileConfiguration.getString("items.members.type").toUpperCase()),
                        fileConfiguration.getString("items.members.value"),
                        fileConfiguration.getInt("items.members.customModelData"),
                        fileConfiguration.getString("items.members.name"),
                        membersItemLore, false), "members");
                int membersItemSlot = fileConfiguration.getInt("items.members.slot");
                inventory.setItem(membersItemSlot, membersClanItem);
            }

            if (fileConfiguration.getBoolean("items.allies.enabled", true)) {
                List<String> alliesItemLore = new ArrayList<>();
                for (String lore : fileConfiguration.getStringList("items.allies.lore")) {
                    lore = lore.replace("%totalAllies%", String.valueOf(clanData.getAllies().size()));
                    alliesItemLore.add(lore);
                }
                ItemStack alliesClanItem = ClansPlus.nms.addCustomData(ItemUtil.getItem(
                        ItemType.valueOf(fileConfiguration.getString("items.allies.type").toUpperCase()),
                        fileConfiguration.getString("items.allies.value"),
                        fileConfiguration.getInt("items.allies.customModelData"),
                        fileConfiguration.getString("items.allies.name"),
                        alliesItemLore, false), "allies");
                int alliesItemSlot = fileConfiguration.getInt("items.allies.slot");
                inventory.setItem(alliesItemSlot, alliesClanItem);
            }

            if (fileConfiguration.getBoolean("items.clanList.enabled", true)) {
                List<String> listClanItemLore = new ArrayList<>();
                for (String lore : fileConfiguration.getStringList("items.clanList.lore")) {
                    lore = lore.replace("%totalClans%", String.valueOf(PluginDataManager.getClanDatabase().size()));
                    listClanItemLore.add(lore);
                }
                ItemStack listClanItem = ClansPlus.nms.addCustomData(ItemUtil.getItem(
                        ItemType.valueOf(fileConfiguration.getString("items.clanList.type").toUpperCase()),
                        fileConfiguration.getString("items.clanList.value"),
                        fileConfiguration.getInt("items.clanList.customModelData"),
                        fileConfiguration.getString("items.clanList.name"),
                        listClanItemLore, false), "clanList");
                int listClanItemSlot = fileConfiguration.getInt("items.clanList.slot");
                inventory.setItem(listClanItemSlot, listClanItem);
            }

            if (fileConfiguration.getBoolean("items.upgradeMaxMember.enabled", true)) {
                List<String> upgradeMaxMembersItemLore = new ArrayList<>();
                int newMaxMembers = clanData.getMaxMembers() + 1;
                CurrencyType upgradeMaxMembersCT = CurrencyType.valueOf(UpgradeFile.get().getString("upgrade.max-members.currency-type").toUpperCase());
                for (String lore : fileConfiguration.getStringList("items.upgradeMaxMember.lore")) {
                    lore = lore.replace("%totalMembers%", String.valueOf(clanData.getMembers().size()));
                    lore = lore.replace("%newMaxMembers%", String.valueOf(newMaxMembers));
                    lore = lore.replace("%currencySymbol%", StringUtil.getCurrencySymbolFormat(upgradeMaxMembersCT));
                    lore = lore.replace("%currencyName%", StringUtil.getCurrencyNameFormat(upgradeMaxMembersCT));
                    if (UpgradeFile.get().getConfigurationSection("upgrade.max-members.price").getKeys(false).contains(String.valueOf(newMaxMembers))) {
                        lore = lore.replace("%price%", String.valueOf(UpgradeFile.get().getLong("upgrade.max-members.price." + newMaxMembers)));
                    } else {
                        lore = lore.replace("%price%", String.valueOf(UpgradeFile.get().getLong("upgrade.max-members.price.else")));
                    }
                    upgradeMaxMembersItemLore.add(lore);
                }
                ItemStack upgradeMaxMemberItem = ClansPlus.nms.addCustomData(ItemUtil.getItem(
                        ItemType.valueOf(fileConfiguration.getString("items.upgradeMaxMember.type").toUpperCase()),
                        fileConfiguration.getString("items.upgradeMaxMember.value"),
                        fileConfiguration.getInt("items.upgradeMaxMember.customModelData"),
                        fileConfiguration.getString("items.upgradeMaxMember.name"),
                        upgradeMaxMembersItemLore, false), "upgradeMaxMember");
                int upgradeMaxMemberItemSlot = fileConfiguration.getInt("items.upgradeMaxMember.slot");
                inventory.setItem(upgradeMaxMemberItemSlot, upgradeMaxMemberItem);
            }

            if (fileConfiguration.getBoolean("items.events.enabled", true)) {
                ItemStack eventsItem = ClansPlus.nms.addCustomData(ItemUtil.getItem(
                        ItemType.valueOf(fileConfiguration.getString("items.events.type").toUpperCase()),
                        fileConfiguration.getString("items.events.value"),
                        fileConfiguration.getInt("items.events.customModelData"),
                        fileConfiguration.getString("items.events.name"),
                        fileConfiguration.getStringList("items.events.lore"), false), "events");
                int eventsItemSlot = fileConfiguration.getInt("items.events.slot");
                inventory.setItem(eventsItemSlot, eventsItem);
            }

            if (fileConfiguration.getBoolean("items.settings.enabled", true)) {
                ItemStack settingsItem = ClansPlus.nms.addCustomData(ItemUtil.getItem(
                        ItemType.valueOf(fileConfiguration.getString("items.settings.type").toUpperCase()),
                        fileConfiguration.getString("items.settings.value"),
                        fileConfiguration.getInt("items.settings.customModelData"),
                        fileConfiguration.getString("items.settings.name"),
                        fileConfiguration.getStringList("items.settings.lore"), false), "settings");
                int settingsItemSlot = fileConfiguration.getInt("items.settings.slot");
                inventory.setItem(settingsItemSlot, settingsItem);
            }

            if (fileConfiguration.getBoolean("items.leave.enabled", true)) {
                ItemStack leaveItem = ClansPlus.nms.addCustomData(ItemUtil.getItem(
                        ItemType.valueOf(fileConfiguration.getString("items.leave.type").toUpperCase()),
                        fileConfiguration.getString("items.leave.value"),
                        fileConfiguration.getInt("items.leave.customModelData"),
                        fileConfiguration.getString("items.leave.name"),
                        fileConfiguration.getStringList("items.leave.lore"), false), "leave");
                int leaveItemSlot = fileConfiguration.getInt("items.leave.slot");
                inventory.setItem(leaveItemSlot, leaveItem);
            }

            if (fileConfiguration.getBoolean("items.clanInfo.enabled", true)) {
                ItemStack clanInfoItem = ClansPlus.nms.addCustomData(ItemUtil.getItem(
                        clanData.getIconType(),
                        clanData.getIconValue(),
                        fileConfiguration.getInt("items.clanInfo.customModelData"),
                        fileConfiguration.getString("items.clanInfo.name"),
                        fileConfiguration.getStringList("items.clanInfo.lore"), false), "clanInfo");
                int clanInfoItemSlot = fileConfiguration.getInt("items.clanInfo.slot");
                inventory.setItem(clanInfoItemSlot, ItemUtil.getClanItemStack(clanInfoItem, clanData));
            }

            if (fileConfiguration.getBoolean("items.spawn.enabled", true)) {
                List<String> spawnItemLore = new ArrayList<>();
                for (String lore : fileConfiguration.getStringList("items.spawn.lore." + (clanData.getSpawnPoint() != null ? "valid-spawn-point" : "invalid-spawn-point"))) {
                    if (clanData.getSpawnPoint() != null) {
                        lore = lore.replace("%x%", String.valueOf((int) clanData.getSpawnPoint().getX()));
                        lore = lore.replace("%y%", String.valueOf((int) clanData.getSpawnPoint().getY()));
                        lore = lore.replace("%z%", String.valueOf((int) clanData.getSpawnPoint().getZ()));
                        lore = lore.replace("%worldName%", clanData.getSpawnPoint().getWorld().getName());
                    }
                    spawnItemLore.add(lore);
                }
                ItemStack spawnItem = ClansPlus.nms.addCustomData(ItemUtil.getItem(
                        ItemType.valueOf(fileConfiguration.getString("items.spawn.type").toUpperCase()),
                        fileConfiguration.getString("items.spawn.value"),
                        fileConfiguration.getInt("items.spawn.customModelData"),
                        fileConfiguration.getString("items.spawn.name"),
                        spawnItemLore, false), "spawn");
                int spawnItemSlot = fileConfiguration.getInt("items.spawn.slot");
                inventory.setItem(spawnItemSlot, spawnItem);
            }

            if (fileConfiguration.getBoolean("items.storage.enabled", true)) {
                int itemsStored = 0;
                if (!clanData.getStorageHashMap().isEmpty()) {
                    for (int clanStorageNumber : clanData.getStorageHashMap().keySet()) {
                        for (ItemStack itemStack : clanData.getStorageHashMap().get(clanStorageNumber).getContents()) {
                            if (itemStack == null)
                                continue;

                            if (ClansPlus.nms.getCustomData(itemStack).equals("next") || ClansPlus.nms.getCustomData(itemStack).equals("previous") || ClansPlus.nms.getCustomData(itemStack).equals("noStorage"))
                                continue;

                            itemsStored = itemsStored + 1;
                        }
                    }
                }

                List<String> clanStorageLore = fileConfiguration.getStringList("items.storage.lore");
                int finalItemsStored = itemsStored;
                clanStorageLore.replaceAll(string -> ClansPlus.nms.addColor(string
                        .replace("%clanMaxStorage%", String.valueOf(clanData.getMaxStorage()))
                        .replace("%serverMaxStorage%", String.valueOf(Settings.STORAGE_SETTINGS_MAX_INVENTORY))
                        .replace("%itemsStored%", String.valueOf(finalItemsStored))));

                ItemStack clanStorageItem = ClansPlus.nms.addCustomData(ItemUtil.getItem(
                        ItemType.valueOf(fileConfiguration.getString("items.storage.type").toUpperCase()),
                        fileConfiguration.getString("items.storage.value"),
                        fileConfiguration.getInt("items.storage.customModelData"),
                        fileConfiguration.getString("items.storage.name"),
                        clanStorageLore, false), "storage");
                int clanStorageItemSlot = fileConfiguration.getInt("items.storage.slot");
                inventory.setItem(clanStorageItemSlot, clanStorageItem);
            }
        });
    }
}
