package com.github.dreadslicer.tekkitrestrict;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.server.EntityHuman;

import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import com.github.dreadslicer.tekkitrestrict.commands.TRCommandAlc;

import ee.AlchemyBagData;
import ee.ItemAlchemyBag;

public class TRNoDupe_BagCache {
	public Player player;
	public boolean hasBHBInBag = false;
	public String inBagColor = "";
	public String dupeItem = "";

	public Object[] hasBHB() {
		// returns whether the player has a BHB in one of their bags
		return new Object[] { hasBHBInBag, inBagColor };
	}

	public boolean isOnline() {
		return player == null ? false : player.isOnline();
	}

	public void removeAlc() {
		// removes all "Devices" form alc bag.
		if (!preventAlcDupe) return;
		
		if (!tekkitrestrict.EEEnabled || Util.hasBypass(player, "dupe", "alcbag")) return;
		for (int i = 0; i < 16; i++) {
			try {
				EntityHuman H = ((CraftPlayer) player).getHandle();
				AlchemyBagData ABD = ItemAlchemyBag.getBagData(i, H, H.world);
				// tekkitrestrict.log.info("???l5");
				// ok, now we search!
				net.minecraft.server.ItemStack[] iss = ABD.items;
				// TRLogger.Log("debug",
				// "info: TTAlc slot "+iss.length);
				for (int j = 0; j < iss.length; j++) {
					if (iss[j] == null) continue;
					if (iss[j].id == 27532 || iss[j].id == 27593)
						iss[j] = null;
				}
				ABD.items = iss;
			} catch (Exception ex) {
				// This alc bag does not exist
			}
		}
	}

	// /STATIC

	@SuppressWarnings("unused")
	private static boolean preventAlcDupe, showDupesOnConsole;
	public static String lastPlayer = "";
	private static Map<Player, TRNoDupe_BagCache> watchers = Collections
			.synchronizedMap(new LinkedHashMap<Player, TRNoDupe_BagCache>());

	public static void init() {
		// initializes a thread that watches player bags.
		Thread c = new Thread(new Runnable() {
			@Override
			public void run() {
				watchThread();
			}
		});
		c.start();
	}

	public static void reload() {
		preventAlcDupe = tekkitrestrict.config.getBoolean("PreventAlcDupe");
		showDupesOnConsole = tekkitrestrict.config.getBoolean("ShowDupesOnConsole");
	}

	private static void watchThread() {
		while (true) { // yet another infinite loop =) :D
			// We want to loop through all of their alchemy bags HERE.
			// This function will also clean up the ones with non-players.

			// loop through all of teh players
			Player[] players = tekkitrestrict.getInstance().getServer().getOnlinePlayers();
			for (int i = 0; i < players.length; i++) {
				try {
					setCheck(players[i]); // checks and sets the vars.
				} catch (Exception e) {
				}
			}

			// remove offline players!
			for (int i = 0; i < watchers.size(); i++) {
				try {
					TRNoDupe_BagCache cc = watchers.get(i);
					if (!cc.isOnline()) {
						watchers.remove(i);
						i--;
					}
				} catch (Exception e) {
				}
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
	}

	public static void setCheck(Player player) {
		if (!preventAlcDupe) return;
		
		if (!tekkitrestrict.EEEnabled || Util.hasBypass(player, "dupe", "alcbag")) return;
		
		for (int i = 0; i < 16; i++) {
			try {
				EntityHuman H = ((CraftPlayer) player).getHandle();
				AlchemyBagData ABD = ItemAlchemyBag.getBagData(i, H, H.world);
				// ok, now we search!
				net.minecraft.server.ItemStack[] iss = ABD.items;

				for (int j = 0; j < iss.length; j++) {
					if (iss[j] == null) continue;
					if (iss[j].id == 27532 || iss[j].id == 27593) {
						if (!player.isOnline()) return;
						
						// they are attempting to dupe?
						
						TRNoDupe_BagCache cache = watchers.get(player);
						if (cache == null) cache = new TRNoDupe_BagCache();

						cache.player = player;
						cache.inBagColor = TRCommandAlc.getColor(i);
						cache.dupeItem = (iss[j].id == 27532) ? "Black Hole Band" : "Void Ring";
						cache.hasBHBInBag = true;
						// tekkitrestrict.log.info("has in bag!");
						watchers.put(player, cache);
						// player.kickPlayer("[TRDupe] you have a Black Hole Band in your ["+Color+"] Alchemy Bag! Please remove it NOW!");

						/*
						 * if(showDupesOnConsole)
						 * tekkitrestrict.
						 * log.info(player.getName()+" ["+Color+
						 * " bag] attempted to dupe with the "
						 * +s+"!"); TRLogger.Log("Dupe",
						 * player.getName()+" ["+Color+
						 * " bag] attempted to dupe with the "
						 * +s+"!");
						 * TRLogger.broadcastDupe(player
						 * .getName(),
						 * "the Alchemy Bag and "+s);
						 */

						// lastPlayer = player.getName();
					}
				}
			} catch (Exception ex) {
				// This alc bag does not exist
			}
		}
	}

	// private static Player lastPlayer=null;
	public static TRNoDupe_BagCache check(Player p) {
		TRNoDupe_BagCache r = null;
		if (!preventAlcDupe) return r;
		TRNoDupe_BagCache cc = watchers.get(p);
		if (cc == null) return r;
		if (cc.player == null) return r;
		if (cc.hasBHBInBag && cc.isOnline()) {
			// tekkitrestrict.log.info("hasbhb");
			return cc;
		}

		return r;
	}

	/**
	 * removes the cache from the list.<br>
	 * this removes some other errors.
	 */
	public static void expire(TRNoDupe_BagCache cache) {
		watchers.remove(cache);
	}
}
