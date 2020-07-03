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

package org.geysermc.connector.entity.type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.geysermc.connector.entity.*;
import java.util.EnumMap;
import java.util.Map;

@Getter
public enum EntityType {

    CHICKEN,
    COW,
    PIG,
    SHEEP,
    WOLF,
    VILLAGER,
    MOOSHROOM,
    SQUID,
    RABBIT,
    BAT,
    IRON_GOLEM,
    SNOW_GOLEM,
    OCELOT,
    HORSE,
    DONKEY,
    MULE,
    SKELETON_HORSE,
    ZOMBIE_HORSE,
    POLAR_BEAR,
    LLAMA,
    TRADER_LLAMA,
    PARROT,
    DOLPHIN,
    ZOMBIE,
    GIANT,
    CREEPER,
    SKELETON,
    SPIDER,
    ZOMBIFIED_PIGLIN,
    SLIME,
    ENDERMAN,
    SILVERFISH,
    CAVE_SPIDER,
    GHAST,
    MAGMA_CUBE,
    BLAZE,
    ZOMBIE_VILLAGER,
    WITCH,
    STRAY,
    HUSK,
    WITHER_SKELETON,
    GUARDIAN,
    ELDER_GUARDIAN,
    NPC,
    WITHER,
    ENDER_DRAGON,
    SHULKER,
    ENDERMITE,
    AGENT,
    VINDICATOR,
    PILLAGER,
    WANDERING_TRADER,
    PHANTOM,
    RAVAGER,

    ARMOR_STAND,
    TRIPOD_CAMERA,
    PLAYER,
    ITEM,
    PRIMED_TNT,
    FALLING_BLOCK,
    MOVING_BLOCK,
    THROWN_EXP_BOTTLE,
    EXPERIENCE_ORB,
    EYE_OF_ENDER,
    END_CRYSTAL,
    FIREWORK_ROCKET,
    TRIDENT,
    TURTLE,
    CAT,
    SHULKER_BULLET,
    FISHING_BOBBER,
    CHALKBOARD,
    DRAGON_FIREBALL,
    ARROW,
    SPECTRAL_ARROW,
    SNOWBALL,
    THROWN_EGG,
    PAINTING,
    MINECART,
    FIREBALL,
    THROWN_POTION,
    THROWN_ENDERPEARL,
    LEASH_KNOT,
    WITHER_SKULL,
    BOAT,
    WITHER_SKULL_DANGEROUS,
    LIGHTNING_BOLT,
    SMALL_FIREBALL,
    AREA_EFFECT_CLOUD,
    MINECART_HOPPER,
    MINECART_TNT,
    MINECART_CHEST,
    MINECART_FURNACE,
    MINECART_SPAWNER,
    MINECART_COMMAND_BLOCK,
    LINGERING_POTION,
    LLAMA_SPIT,
    EVOKER_FANGS,
    EVOKER,
    VEX,
    ICE_BOMB,
    BALLOON,
    PUFFERFISH,
    SALMON,
    DROWNED,
    TROPICAL_FISH,
    COD,
    PANDA,
    FOX,
    BEE,
    STRIDER,
    HOGLIN,
    ZOGLIN,
    PIGLIN,
    ITEM_FRAME,
    ILLUSIONER;

    private static final Map<EntityType, Entity.Data> VALUES = new EnumMap<>(EntityType.class);
    public static Register REGISTER = new Register();
    
    public static class Register {
        @SuppressWarnings("SuspiciousNameCombination")
        public <T extends Entity> Register entityType(EntityType entityType, Class<T> entityClass, T.Data.DataBuilder builder) {
            VALUES.put(entityType, builder.entityType(entityType).build());
            return this;
        }
    }

    public Entity.Data getData() {
        return VALUES.get(this);
    }

    public static EntityType getFromIdentifier(String identifier) {
        return VALUES.entrySet().stream()
                .filter(e -> e.getValue().getIdentifier().equals(identifier))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
