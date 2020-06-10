/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.connector.network.translators.inventory;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.message.Message;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientRenameItemPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.protocol.bedrock.data.ContainerId;
import com.nukkitx.protocol.bedrock.data.ContainerType;
import com.nukkitx.protocol.bedrock.data.InventoryActionData;
import com.nukkitx.protocol.bedrock.data.ItemData;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.action.Transaction;
import org.geysermc.connector.network.translators.inventory.action.Execute;
import org.geysermc.connector.network.translators.inventory.updater.CursorInventoryUpdater;

import java.util.List;
import java.util.stream.Collectors;

public class AnvilInventoryTranslator extends BlockInventoryTranslator {
    public AnvilInventoryTranslator() {
        super(3, "minecraft:anvil[facing=north]", ContainerType.ANVIL, new CursorInventoryUpdater());
    }

    @Override
    public int bedrockSlotToJava(InventoryActionData action) {
        if (action.getSource().getContainerId() == ContainerId.CURSOR) {
            switch (action.getSlot()) {
                case 1:
                    return 0;
                case 2:
                    return 1;
                case 50:
                    return 2;
            }
        }
        return super.bedrockSlotToJava(action);
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        switch (slot) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 50;
        }
        return super.javaSlotToBedrock(slot);
    }

    @Override
    public boolean isOutput(InventoryActionData action) {
        return bedrockSlotToJava(action) == 2;
    }



    @Override
    protected void processAction(Transaction transaction, ActionData cursor, ActionData from, ActionData to) {
        // If from is ANVIL_RESULT we add a rename packet
        if (from.action.getSource().getContainerId() == ContainerId.ANVIL_RESULT) {
            transaction.add(new Execute(() -> {
                ItemData item = from.action.getFromItem();
                com.nukkitx.nbt.tag.CompoundTag tag = item.getTag();
                String rename = tag != null ? tag.getCompound("display").getString("Name") : "";
                ClientRenameItemPacket renameItemPacket = new ClientRenameItemPacket(rename);
                System.err.println("Rename: " + renameItemPacket);
                transaction.getSession().sendDownstreamPacket(renameItemPacket);
            }));
        }

        super.processAction(transaction, cursor, from, to);
    }

    @Override
    public void translateActions(GeyserSession session, Inventory inventory, List<InventoryActionData> actions) {
        // If we have an anvil_result then we filter out anvil_material and container_input
        if (actions.stream().anyMatch(this::isOutput)) {
            actions = actions.stream()
                    .filter(a -> a.getSource().getContainerId() != ContainerId.ANVIL_MATERIAL)
                    .filter(a -> a.getSource().getContainerId() != ContainerId.CONTAINER_INPUT)
                    .collect(Collectors.toList());
        }

        super.translateActions(session, inventory, actions);
    }

    @Override
    public void updateSlot(GeyserSession session, Inventory inventory, int slot) {
        if (slot >= 0 && slot <= 2) {
            ItemStack item = inventory.getItem(slot);
            if (item != null) {
                String rename;
                CompoundTag tag = item.getNbt();
                if (tag != null) {
                    CompoundTag displayTag = tag.get("display");
                    if (displayTag != null) {
                        String itemName = displayTag.get("Name").getValue().toString();
                        Message message = Message.fromString(itemName);
                        rename = message.getText();
                    } else {
                        rename = "";
                    }
                } else {
                    rename = "";
                }
                ClientRenameItemPacket renameItemPacket = new ClientRenameItemPacket(rename);
                session.sendDownstreamPacket(renameItemPacket);
            }
        }
        super.updateSlot(session, inventory, slot);
    }
}
