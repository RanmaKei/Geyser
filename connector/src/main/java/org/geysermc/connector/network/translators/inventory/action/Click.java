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

package org.geysermc.connector.network.translators.inventory.action;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.window.ClickItemParam;
import com.github.steveice10.mc.protocol.data.game.window.ShiftClickItemParam;
import com.github.steveice10.mc.protocol.data.game.window.WindowAction;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientConfirmTransactionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientWindowActionPacket;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.geysermc.connector.inventory.PlayerInventory;
import org.geysermc.connector.utils.InventoryUtils;

/**
 * Send a Left, Right or Shift+Click to the Downstream Server
 */
@Getter
@ToString
@AllArgsConstructor
public class Click extends BaseAction {

    private final Type clickType;
    private final int javaSlot;

    @Override
    public void execute(ActionPlan plan) {
        ItemStack clickedItem = plan.getInventory().getItem(javaSlot);
        PlayerInventory playerInventory = plan.getSession().getInventory();
        final short actionId = (short) plan.getInventory().getTransactionId().getAndIncrement();
        final ItemStack cursorItem = playerInventory.getCursor();

        switch (clickType) {
            case LEFT:
            case RIGHT:
                ClientWindowActionPacket clickPacket = new ClientWindowActionPacket(plan.getInventory().getId(),
                        actionId, javaSlot, clickedItem,
                        WindowAction.CLICK_ITEM, clickType == Type.LEFT ? ClickItemParam.LEFT_CLICK : ClickItemParam.RIGHT_CLICK);

                switch (clickType) {
                    case LEFT:
                        if (!InventoryUtils.canStack(cursorItem, clickedItem)) {
                            playerInventory.setCursor(clickedItem);
                            plan.getInventory().setItem(javaSlot, cursorItem);
                        } else {
                            playerInventory.setCursor(null);
                            plan.getInventory().setItem(javaSlot, new ItemStack(clickedItem.getId(),
                                    clickedItem.getAmount() + cursorItem.getAmount(), clickedItem.getNbt()));
                        }
                        break;
                    case RIGHT:
                        if (cursorItem == null && clickedItem != null) {
                            ItemStack halfItem = new ItemStack(clickedItem.getId(),
                                    clickedItem.getAmount() / 2, clickedItem.getNbt());
                            plan.getInventory().setItem(javaSlot, halfItem);
                            playerInventory.setCursor(new ItemStack(clickedItem.getId(),
                                    clickedItem.getAmount() - halfItem.getAmount(), clickedItem.getNbt()));
                        } else if (cursorItem != null && clickedItem == null) {
                            playerInventory.setCursor(new ItemStack(cursorItem.getId(),
                                    cursorItem.getAmount() - 1, cursorItem.getNbt()));
                            plan.getInventory().setItem(javaSlot, new ItemStack(cursorItem.getId(),
                                    1, cursorItem.getNbt()));
                        } else if (InventoryUtils.canStack(cursorItem, clickedItem)) {
                            playerInventory.setCursor(new ItemStack(cursorItem.getId(),
                                    cursorItem.getAmount() - 1, cursorItem.getNbt()));
                            plan.getInventory().setItem(javaSlot, new ItemStack(clickedItem.getId(),
                                    clickedItem.getAmount() + 1, clickedItem.getNbt()));
                        }
                        break;
                }
                plan.getSession().sendDownstreamPacket(clickPacket);
                plan.getSession().sendDownstreamPacket(new ClientConfirmTransactionPacket(plan.getInventory().getId(), actionId, true));
                break;

            case SHIFT_CLICK:
                clickedItem = plan.getInventory().getItem(javaSlot);

                ClientWindowActionPacket shiftClickPacket = new ClientWindowActionPacket(
                        plan.getInventory().getId(),
                        plan.getInventory().getTransactionId().getAndIncrement(),
                        javaSlot, clickedItem,
                        WindowAction.SHIFT_CLICK_ITEM,
                        ShiftClickItemParam.LEFT_CLICK
                );
                plan.getSession().sendDownstreamPacket(shiftClickPacket);
                break;
        }
    }

    public enum Type {
        LEFT,
        RIGHT,
        SHIFT_CLICK
    }
}
