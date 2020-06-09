/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.network.translators.inventory.action;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.data.game.window.DropItemParam;
import com.github.steveice10.mc.protocol.data.game.window.WindowAction;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockFace;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientWindowActionPacket;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * Send a Drop packet to the Downstream server
 */
@Getter
@ToString
@AllArgsConstructor
public class Drop extends BaseAction {

    private final Type dropType;
    private final int javaSlot;

    @Override
    public void execute(ActionPlan plan) {


        switch (dropType) {
            case DROP_ITEM:
            case DROP_STACK:
                ClientWindowActionPacket dropPacket = new ClientWindowActionPacket(
                        plan.getInventory().getId(),
                        plan.getInventory().getTransactionId().getAndIncrement(),
                        javaSlot,
                        null,
                        WindowAction.DROP_ITEM,
                        dropType == Type.DROP_ITEM ? DropItemParam.DROP_FROM_SELECTED : DropItemParam.DROP_SELECTED_STACK
                );
                plan.getSession().sendDownstreamPacket(dropPacket);

                ItemStack cursor = plan.getSession().getInventory().getCursor();
                if (cursor != null) {
                    plan.getSession().getInventory().setCursor(
                            new ItemStack(
                                    cursor.getId(),
                                    dropType == Type.DROP_ITEM ? cursor.getAmount() - 1 : 0,
                                    cursor.getNbt()
                            )
                    );
                }
                break;
            case DROP_ITEM_HOTBAR:
            case DROP_STACK_HOTBAR:
                ClientPlayerActionPacket actionPacket = new ClientPlayerActionPacket(
                        dropType == Type.DROP_ITEM_HOTBAR ? PlayerAction.DROP_ITEM : PlayerAction.DROP_ITEM_STACK,
                        new Position(0, 0, 0),
                        BlockFace.DOWN
                );
                plan.getSession().sendDownstreamPacket(actionPacket);
                ItemStack item = plan.getSession().getInventory().getItem(javaSlot);
                if (item != null) {
                    plan.getSession().getInventory().setItem(
                            javaSlot,
                            new ItemStack(
                                    item.getId(),
                                    dropType == Type.DROP_ITEM_HOTBAR ? item.getAmount() - 1 : 0,
                                    item.getNbt()
                            )
                    );
                }
        }

    }

    public enum Type {
        DROP_ITEM,
        DROP_STACK,
        DROP_ITEM_HOTBAR,
        DROP_STACK_HOTBAR
    }
}
