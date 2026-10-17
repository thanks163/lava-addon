package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * LavaRestock
 *
 * 1) Kalau slot hotbar yang lagi dipegang isinya empty bucket (atau kosong),
 *    ambil lava bucket dari inventory utama dan taruh di slot itu.
 * 2) Gabungin empty bucket di inventory utama jadi stack (maks 16).
 *
 * Catatan: belum dites di 1.21.11, nama method bisa perlu disesuaikan.
 */
public class LavaRestock extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Jeda antar aksi inventory (tick). Naikin kalau di server.")
        .defaultValue(2)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> restock = sgGeneral.add(new BoolSetting.Builder()
        .name("restock")
        .description("Isi ulang slot yang dipegang dengan lava bucket dari inventory.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> mergeBuckets = sgGeneral.add(new BoolSetting.Builder()
        .name("merge-empty-buckets")
        .description("Gabungin empty bucket di inventory jadi stack 16.")
        .defaultValue(true)
        .build()
    );

    private int timer = 0;

    public LavaRestock() {
        super(AddonTemplate.CATEGORY, "lava-restock", "Restock lava bucket ke hotbar dan merge empty bucket.");
    }

    @Override
    public void onActivate() {
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.interactionManager == null) return;

        if (timer > 0) {
            timer--;
            return;
        }

        if (restock.get() && doRestock()) {
            timer = delay.get();
            return;
        }

        if (mergeBuckets.get() && doMerge()) {
            timer = delay.get();
        }
    }

    /** @return true kalau ada aksi inventory yang dilakukan. */
    private boolean doRestock() {
        int selected = mc.player.getInventory().getSelectedSlot();
        ItemStack hand = mc.player.getInventory().getStack(selected);

        if (!hand.isEmpty() && !hand.isOf(Items.BUCKET)) return false;

        // Cari lava bucket di inventory utama (slot 9..35)
        FindItemResult lava = InvUtils.find(stack -> stack.isOf(Items.LAVA_BUCKET), 9, 35);
        if (!lava.found()) return false;

        InvUtils.move().from(lava.slot()).toHotbar(selected);
        return true;
    }

    /** @return true kalau ada aksi merge yang dilakukan. */
    private boolean doMerge() {
        int target = -1;
        int targetCount = -1;
        int source = -1;
        int sourceCount = Integer.MAX_VALUE;

        // Target: stack empty bucket paling penuh yang belum 16.
        for (int i = 9; i <= 35; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isOf(Items.BUCKET)) continue;

            int count = stack.getCount();
            if (count < stack.getMaxCount() && count > targetCount) {
                target = i;
                targetCount = count;
            }
        }

        if (target == -1) return false;

        // Source: stack empty bucket lain yang paling kecil.
        for (int i = 9; i <= 35; i++) {
            if (i == target) continue;

            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isOf(Items.BUCKET)) continue;

            int count = stack.getCount();
            if (count < sourceCount) {
                source = i;
                sourceCount = count;
            }
        }

        if (source == -1) return false;

        InvUtils.move().from(source).to(target);
        return true;
    }
}
