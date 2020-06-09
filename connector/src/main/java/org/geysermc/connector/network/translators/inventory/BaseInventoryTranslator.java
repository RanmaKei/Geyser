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
import com.nukkitx.protocol.bedrock.data.ContainerId;
import com.nukkitx.protocol.bedrock.data.InventoryActionData;
import com.nukkitx.protocol.bedrock.data.InventorySource;
import com.nukkitx.protocol.bedrock.data.ItemData;
import lombok.NonNull;
import lombok.ToString;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.action.Click;
import org.geysermc.connector.network.translators.inventory.action.ActionPlan;
import org.geysermc.connector.network.translators.inventory.action.Drop;
import org.geysermc.connector.network.translators.inventory.action.Refresh;
import org.geysermc.connector.utils.InventoryUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseInventoryTranslator extends InventoryTranslator{
    protected BaseInventoryTranslator(int size) {
        super(size);
    }

    @Override
    public void updateProperty(GeyserSession session, Inventory inventory, int key, int value) {
        //
    }

    @Override
    public int bedrockSlotToJava(InventoryActionData action) {
        int slotnum = action.getSlot();
        if (action.getSource().getContainerId() == ContainerId.INVENTORY) {
            //hotbar
            if (slotnum >= 9) {
                return slotnum + this.size - 9;
            } else {
                return slotnum + this.size + 27;
            }
        }
        return slotnum;
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        if (slot >= this.size) {
            final int tmp = slot - this.size;
            if (tmp < 27) {
                return tmp + 9;
            } else {
                return tmp - 27;
            }
        }
        return slot;
    }

    @Override
    public void translateActions(GeyserSession session, Inventory inventory, List<InventoryActionData> actions) {
        List<ActionData> actionDataList = new ArrayList<>();
        ActionData cursor = null;

        for (InventoryActionData action : actions) {
            ActionData actionData = new ActionData(this, action);

            if (isCursor(action)) {
                cursor = actionData;
            }
            actionDataList.add(actionData);
        }

        if (cursor == null) {
            // Create a fake cursor action
            cursor = new ActionData(this, new InventoryActionData(
                    InventorySource.fromContainerWindowId(124),
                    0,
                    ItemData.of(0, (short) 0,0),
                    ItemData.of(0, (short) 0,0)
            ));
            actionDataList.add(cursor);
        }

        ActionPlan plan = new ActionPlan(session, this, inventory);

        outer:
        while (actionDataList.size() > 0) {
            ActionData a1 = actionDataList.remove(0);

            // Check if a1 is already fulfilled
            if (a1.isResolved()) {
                continue;
            }

            for (ActionData a2 : actionDataList) {

                // Check if a1 is already fulfilled
                if (a2.isResolved()) {
                    continue;
                }

                // Directions have to be opposite or equal
                if ((a1.currentCount > a1.toCount && a2.currentCount > a2.toCount)
                        || (a1.currentCount < a1.toCount && a2.currentCount < a2.toCount)) {
                    continue;
                }

                // Work out direction
                ActionData from;
                ActionData to;
                if (a1.currentCount > a1.toCount) {
                    from = a1;
                    to = a2;
                } else {
                    from = a2;
                    to = a1;
                }

                // Check if to and from cancel each other out
                // @TODO This may not be needed anymore as we now filter useless packets
                if (from.javaSlot == to.javaSlot
                        && from.remaining() == to.remaining()
                        && from.action.getSource().getContainerId() == to.action.getSource().getContainerId()
                ) {
                    from.currentCount = from.toCount;
                    to.currentCount = to.toCount;
                    continue outer;
                }

                // Process
                processAction(session, inventory, plan, cursor, from, to);
            }

            // Log unresolved for the moment
            if (a1.remaining() > 0) {
                GeyserConnector.getInstance().getLogger().warning("Inventory Items Unresolved: " + a1);
            }
        }

        plan.execute();
    }

    protected void processAction(GeyserSession session, Inventory inventory, ActionPlan plan, ActionData cursor, ActionData from, ActionData to) {
        // Dropping to the world?
        if (to.action.getSource().getFlag() == InventorySource.Flag.DROP_ITEM) {

            // Is it dropped without a window?
            if (session.getInventoryCache().getOpenInventory() == null
                    && from.action.getSource().getContainerId() == ContainerId.INVENTORY
                    && from.action.getSlot() == session.getInventory().getHeldItemSlot()) {

                // Dropping everything?
                if (from.toCount == 0 && from.currentCount <= to.remaining()) {
                    to.currentCount = from.currentCount;
                    from.currentCount =0;
                    plan.add(new Drop(Drop.Type.DROP_STACK_HOTBAR, from.javaSlot));
                } else {
                    while (from.remaining() > 0 && to.remaining() > 0) {
                        to.currentCount++;
                        from.currentCount--;
                        plan.add(new Drop(Drop.Type.DROP_ITEM_HOTBAR, from.javaSlot));
                    }
                }
            } else {
                // Dropping everything?
                if (from.toCount == 0 && from.currentCount <= to.remaining()) {
                    to.currentCount += from.currentCount;
                    from.currentCount = 0;
                    plan.add(new Drop(Drop.Type.DROP_STACK, from.javaSlot));
                } else {
                    while (from.remaining() > 0 && to.remaining() > 0) {
                        to.currentCount++;
                        from.currentCount--;
                        plan.add(new Drop(Drop.Type.DROP_ITEM, from.javaSlot));
                    }
                }
            }
            return;
        }

        // Can we swap the contents of to and from? Only applicable if either is the cursor or the cursor is empty
        if ((cursor.currentCount == 0 || cursor == from  || cursor == to)
            && (from.getCurrentId() == to.getToId()
                    && to.getCurrentId() == from.getToId()
                    && from.getCurrentId() != to.getCurrentId())) {

            if (from != cursor) {
                plan.add(new Click(Click.Type.LEFT, from.javaSlot));
            }

            if (to != cursor) {
                plan.add(new Click(Click.Type.LEFT, to.javaSlot));
            }

            if (from != cursor) {
                plan.add(new Click(Click.Type.LEFT, from.javaSlot));
            }

            int currentCount = from.currentCount;
            int currentId = from.getCurrentId();

            from.currentCount = to.currentCount;
            from.currentId = to.getCurrentId();
            to.currentCount = currentCount;
            to.currentId = currentId;
            return;
        }

        // Incompatible Items?
        if (!InventoryUtils.canStack(from.action.getFromItem(), to.action.getToItem())) {
            return;
        }

        // Can we drop anything from cursor onto to?
        if (cursor != to && to.remaining() > 0 && cursor.currentCount > 0 && cursor.getCurrentId() == to.getToId()
                && (to.getCurrentId() == 0 || to.getCurrentId() == to.getToId())) {

            // @TODO: Optimize by checking if we can left click
            to.currentId = cursor.getCurrentId();
            while (cursor.currentCount > 0 && to.remaining() > 0) {
                plan.add(new Click(Click.Type.RIGHT, to.javaSlot));
                cursor.currentCount--;
                to.currentCount++;
            }
        }

        // If from is not the cursor and the cursor is empty or is to we can pick up from from and drop onto to
        if (from != cursor && (cursor.currentCount == 0 || to == cursor)) {

            // Pick up everything
            // @TODO: Maybe optimize here by seeing if we can pick up half and slice it closer to the amount
            plan.add(new Click(Click.Type.LEFT, from.javaSlot));
            cursor.currentCount += from.currentCount;
            cursor.currentId = from.getCurrentId();
            from.currentCount = 0;

            // Drop what we don't need if not an output - NOTE This has the chance of leaking items to the cursor
            // due to the fact bedrock allows arbitrary pickup amounts.
            int leak = 0;
            while(from.remaining() > 0) {
                if (!isOutput(from.action)) {
                    plan.add(new Click(Click.Type.RIGHT, from.javaSlot));
                    cursor.currentCount--;
                    from.currentCount++;
                } else {
                    leak++;
                    from.toCount--;
                }
            }

            // Drop onto to if not the cursor
            if (to != cursor) {
                to.currentId = cursor.getCurrentId();
                while (to.remaining() > 0 && cursor.currentCount > 0) {
                    plan.add(new Click(Click.Type.RIGHT, to.javaSlot));
                    cursor.currentCount--;
                    to.currentCount++;
                }

                // If we have leaks we try drop everything else onto to
                if (leak > 0) {
                    plan.add(new Click(Click.Type.LEFT, to.javaSlot));
                    to.toCount += leak;
                }
            }

            // Leaks so we refresh. Maybe it will be ok
            if (leak > 0) {
                plan.add(new Refresh());
            }
        } else {
            // From is the cursor, so we can assume to is not

            // Can we drop everything onto to?
            if (cursor.toCount == 0 && cursor.remaining() <= to.remaining()) {
                to.currentCount += cursor.currentCount;
                to.currentId = cursor.getCurrentId();
                cursor.currentCount = 0;

                plan.add(new Click(Click.Type.LEFT, to.javaSlot));
            } else {
                // Drop what we need onto to
                to.currentId = cursor.getCurrentId();
                while (cursor.remaining() > 0 && to.remaining() > 0) {
                    cursor.currentCount--;
                    to.currentCount++;

                    plan.add(new Click(Click.Type.RIGHT, to.javaSlot));
                }
            }
        }

        // Can we drop anything from cursor onto to?
        // @TODO: Is this needed still?
        if (cursor != to && to.remaining() > 0 && cursor.currentCount > 0 && cursor.getCurrentId() == to.getToId()
                && (to.getCurrentId() == 0 || to.getCurrentId() == to.getToId())) {
            to.currentId = cursor.getCurrentId();

            // @TODO: Optimize by checking if we can left click
            while (cursor.currentCount > 0 && to.remaining() > 0) {
                plan.add(new Click(Click.Type.RIGHT, to.javaSlot));
                cursor.currentCount--;
                to.currentCount++;
            }
        }
    }

    private int findTempSlot(Inventory inventory, ItemStack item, List<Integer> slotBlacklist, boolean emptyOnly) {
        /*try and find a slot that can temporarily store the given item
        only look in the main inventory and hotbar
        only slots that are empty or contain a different type of item are valid*/
        int offset = inventory.getId() == 0 ? 1 : 0; //offhand is not a viable slot (some servers disable it)
        List<ItemStack> itemBlacklist = new ArrayList<>(slotBlacklist.size() + 1);
        itemBlacklist.add(item);
        for (int slot : slotBlacklist) {
            ItemStack blacklistItem = inventory.getItem(slot);
            if (blacklistItem != null)
                itemBlacklist.add(blacklistItem);
        }
        for (int i = inventory.getSize() - (36 + offset); i < inventory.getSize() - offset; i++) {
            ItemStack testItem = inventory.getItem(i);
            boolean acceptable = true;
            if (testItem != null) {
                if (emptyOnly) {
                    continue;
                }
                for (ItemStack blacklistItem : itemBlacklist) {
                    if (InventoryUtils.canStack(testItem, blacklistItem)) {
                        acceptable = false;
                        break;
                    }
                }
            }
            if (acceptable && !slotBlacklist.contains(i))
                return i;
        }
        //could not find a viable temp slot
        return -1;
    }

    /**
     * Return true if the action represents the temporary cursor slot
     * @return boolean true if is the cursor
     */
    @Override
    public boolean isCursor(InventoryActionData action) {
        return (
                action.getSource().getContainerId() == ContainerId.CURSOR
                        && action.getSlot() == 0
        );
    }

    /**
     * Return true if action represents an output slot
     * @return boolean true if an output slot
     */
    @Override
    public boolean isOutput(InventoryActionData action) {
        return false;
    }

    @ToString
    public static class ActionData {
        public final InventoryTranslator translator;
        public final InventoryActionData action;

        public final int toId;
        public int toCount;
        public final int javaSlot;

        public int currentId;

        public int currentCount;

        public ActionData(InventoryTranslator translator, @NonNull InventoryActionData action) {
            this.translator = translator;
            this.action = action;

            this.toId = action.getToItem().getId();
            this.toCount = action.getToItem().getCount();
            this.javaSlot = translator.bedrockSlotToJava(action);
            this.currentId = action.getFromItem().getId();
            this.currentCount = action.getFromItem().getCount();
        }

        public int getCurrentId() {
            return currentCount == 0 ? 0 : currentId;
        }

        public int getToId() {
            return toCount == 0 ? 0 : toId;
        }

        public int remaining() {
            return Math.abs(toCount - currentCount);
        }

        public boolean isResolved() {
            return remaining() == 0 && ((currentId == toId) || (currentId == 0 || toId == 0));
        }
    }
}
