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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A transaction is created when changes are made to the Inventory. This will store changes sent to us from
 * downstream and playback a series of actions.
 */

@Getter
@ToString(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
public class Transaction {

    @ToString.Include
    private final PriorityQueue<BaseAction> actions = new PriorityQueue<>();

    @ToString.Include
    private BaseAction currentAction = null;

    private final GeyserSession session;
    private final InventoryTranslator translator;
    private final Inventory inventory;

    public void add(BaseAction action) {
        action.setTransaction(this);
        actions.add(action);
    }

    /**
     * Start Execution of the Transaction
     */
    public void execute() {
        if (actions.isEmpty()) {
            return;
        }

        next();

//        // Update session at end
//        add(new Execute(() -> {
//            session.setActionPlan(null);
//        }, 10));

//        /*if (refresh) {
//            translator.updateInventory(session, inventory);
//            InventoryUtils.updateCursor(session);
//        }*/
    }

    /**
     * Execute the next action
     */
    public void next() {
        if (actions.isEmpty()) {
            currentAction = null;
            return;
        }

        currentAction = actions.remove();
        System.err.println("Executing: " + currentAction);
        currentAction.execute();
    }

}
