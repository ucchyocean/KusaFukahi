/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.kusafukahi;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * wwwとチャットに発言すると、自分の周りに草が生えるプラグイン
 * @author ucchy
 */
public class KusaFukahi extends JavaPlugin implements Listener {

    /** 発言とマッチさせる正規表現 */
    private static final String CHAT_PATTERN = ".*(www|WWW|ｗｗｗ|ＷＷＷ).*";

    /** エイプリルフール限定で動作させるかどうか */
    private boolean aprilfoolTimer;
    /** 草が生える範囲 */
    private int range;
    /** 草を生やすと同時に流すメッセージ */
    private String message;

    /**
     * プラグインが起動するときに呼び出されるメソッドです。
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {

        // イベントリスナーとして登録
        getServer().getPluginManager().registerEvents(this, this);

        // コンフィグの読み込み処理
        saveDefaultConfig();
        reloadConfig();
        aprilfoolTimer = getConfig().getBoolean("aprilfoolTimer", true);
        range = getConfig().getInt("range", 5);
        message = getConfig().getString("message", "草不可避！");
    }

    /**
     * プレイヤーがチャット発言したときに呼び出されるメソッドです。
     * @param event
     */
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onChat(AsyncPlayerChatEvent event) {

        // 草がないチャット発言には、用は無いッ！
        if ( !event.getMessage().matches(CHAT_PATTERN) ) {
            return;
        }

        // エイプリルフール限定の場合は、4月1日じゃなければ動作しない。
        if ( aprilfoolTimer ) {
            Calendar cal = Calendar.getInstance();
            if ( cal.get(Calendar.MONTH) != Calendar.APRIL ||
                    cal.get(Calendar.DAY_OF_MONTH) != 1 ) {
                return;
            }
        }

        // 草を生やす処理
        Location location = event.getPlayer().getLocation();
        World world = location.getWorld();
        final ArrayList<BlockSetData> setdata = new ArrayList<BlockSetData>();

        for ( int x=-range; x<=range; x++ ) {
            for ( int y=-range; y<=range; y++ ) {
                for ( int z=-range; z<=range; z++ ) {

                    Block block = world.getBlockAt(
                            location.getBlockX() + x,
                            location.getBlockY() + y,
                            location.getBlockZ() + z);

                    if ( block.getType() != Material.AIR ) {
                        continue;
                    }

                    Block down = block.getRelative(BlockFace.DOWN);

                    if ( down.getType() == Material.GRASS ) {
                        // 下が草ブロック
                        setdata.add(new BlockSetData(block, Material.LONG_GRASS));

                    } else if ( down.getType() == Material.DIRT ) {
                        // 下が土ブロック
                        setdata.add(new BlockSetData(block, Material.LONG_GRASS));
                        setdata.add(new BlockSetData(down, Material.GRASS));

                    } else if ( down.getType() == Material.SAND ) {
                        // 下が砂ブロック
                        setdata.add(new BlockSetData(block, Material.DEAD_BUSH));

                    }
                }
            }
        }

        // 草を生やす対象があるなら、メッセージを流してブロックの更新をおこなう
        if ( setdata.size() > 0 ) {

            if ( !message.equals("") ) {
                new BukkitRunnable() {
                    public void run() {
                        Bukkit.broadcastMessage(message);
                    }
                }.runTaskLater(this, 3);
            }

            Collections.shuffle(setdata);
            new BukkitRunnable() {
                private int index;
                public void run() {
                    if ( setdata.size() > index ) {
                        setdata.get(index).set();
                        index++;
                    } else {
                        cancel();
                    }
                }
            }.runTaskTimer(this, 3, 1);
        }
    }

    public class BlockSetData {

        private Block block;
        private Material material;

        public BlockSetData(Block block, Material material) {
            this.block = block;
            this.material = material;
        }

        @SuppressWarnings("deprecation")
        protected void set() {
            if ( material == Material.LONG_GRASS ) {
                block.setTypeIdAndData(material.getId(), (byte)1, false);
            } else {
                block.setType(material);
            }
            block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, 8);
        }
    }
}
