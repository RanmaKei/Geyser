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

import com.github.steveice10.mc.protocol.data.game.window.ClickItemParam;
import com.github.steveice10.mc.protocol.data.game.window.WindowAction;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientConfirmTransactionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientWindowActionPacket;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.geysermc.connector.utils.InventoryUtils;

/**
 * Send an invalid click to refresh all slots
 *
 * We will filter out repeat refreshes and ensre our executation happens last in the plan
 */
@Getter
@ToString
@AllArgsConstructor
public class Refresh extends BaseAction {

    private final int weight = 10;

    @Override
    public void execute(ActionPlan plan) {
        if (true) return;

        // Only execute if we are the only refresh in the queue
        if (plan.getActions().stream().anyMatch(a -> a instanceof Refresh)) {
            return;
        }

        final short actionId = (short) plan.getInventory().getTransactionId().getAndIncrement();
        ClientWindowActionPacket clickPacket = new ClientWindowActionPacket(plan.getInventory().getId(),
                actionId, -800, InventoryUtils.REFRESH_ITEM, WindowAction.CLICK_ITEM, ClickItemParam.LEFT_CLICK);

        plan.getSession().sendDownstreamPacket(clickPacket);
        plan.getSession().sendDownstreamPacket(new ClientConfirmTransactionPacket(plan.getInventory().getId(), actionId, true));
    }
}
