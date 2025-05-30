package me.youhavetrouble.preventstabby.hooks;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import me.youhavetrouble.preventstabby.PreventStabby;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

public class WorldGuardHook {

    private static FlagRegistry flagRegistry;
    public static StateFlag FORCE_PVP_FLAG;

    public static void init(Logger logger) {
        try {
            Class.forName("com.sk89q.worldguard.protection.flags.registry.FlagRegistry");
            WorldGuardPlugin worldGuardPlugin = WorldGuardPlugin.inst();
            if (WorldGuard.getInstance() == null || worldGuardPlugin == null) return;
            logger.info("Hooking into WorldGuard");
            flagRegistry = WorldGuard.getInstance().getFlagRegistry();
            createForcePvpFlag();
        } catch (NoClassDefFoundError | ClassNotFoundException ignored) {
        }
    }

    private static void createForcePvpFlag() {
        if (flagRegistry == null) return;
        String flagName = "preventstabby-force-pvp";
        try {
            StateFlag flag = new StateFlag(flagName, false);
            flagRegistry.register(flag);
            FORCE_PVP_FLAG = flag;
        } catch (FlagConflictException e) {
            Flag<?> existing = flagRegistry.get(flagName);
            if (existing instanceof StateFlag) {
                FORCE_PVP_FLAG = (StateFlag) existing;
            } else {
                PreventStabby.getPlugin().getLogger().severe("[PreventStabby] There is a conflict between flag names!");
            }
        }
    }

    public static boolean isPlayerForcedToPvp(Player player) {
        if (player == null) return false;
        if (!PreventStabby.worldGuardHookEnabled()) return false;
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        org.bukkit.Location loc = player.getLocation();
        if (loc.getWorld() == null) return false;
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        ApplicableRegionSet set = query.getApplicableRegions(new Location(BukkitAdapter.adapt(loc.getWorld()), loc.getX(), loc.getY(), loc.getZ()));
        return set.testState(localPlayer, FORCE_PVP_FLAG);
    }

}
