package world.bentobox.level.hooks;

import java.lang.reflect.Method;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.Nullable;

public final class CraftEngineHook {
    private static final String PLUGIN_NAME = "CraftEngine";
    private static final Object INIT_LOCK = new Object();
    private static volatile Boolean available;

    private static Method blocksGetCustomBlockState;
    private static Method itemsGetCustomItemId;
    private static Method itemsById;
    private static Method keyOf;
    private static Method keyAsString;
    private static Method immutableBlockStateOwner;
    private static Method holderValue;
    private static Method customBlockId;
    private static Method customItemBehaviors;
    private static Method customItemBuildItemStack;
    private static Class<?> blockBoundBehaviorClass;
    private static Method blockBoundBehaviorBlock;

    private CraftEngineHook() {
    }

    public static boolean isAvailable() {
        if (available != null) {
            return available.booleanValue();
        }
        synchronized (INIT_LOCK) {
            if (available != null) {
                return available.booleanValue();
            }
            available = Boolean.valueOf(init());
            return available.booleanValue();
        }
    }

    @Nullable
    public static String getCustomBlockId(@Nullable BlockData blockData) {
        if (!isAvailable() || blockData == null) {
            return null;
        }
        try {
            Object state = blocksGetCustomBlockState.invoke(null, blockData);
            if (state == null) {
                return null;
            }
            Object owner = immutableBlockStateOwner.invoke(state);
            if (owner == null) {
                return null;
            }
            Object value = holderValue.invoke(owner);
            if (value == null) {
                return null;
            }
            Object key = customBlockId.invoke(value);
            return keyToString(key);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static String getCustomBlockId(@Nullable ItemStack itemStack) {
        if (!isAvailable() || itemStack == null) {
            return null;
        }
        try {
            Object key = itemsGetCustomItemId.invoke(null, itemStack);
            if (key == null) {
                return null;
            }
            Object customItem = itemsById.invoke(null, key);
            if (customItem == null || customItemBehaviors == null || blockBoundBehaviorClass == null
                    || blockBoundBehaviorBlock == null) {
                return null;
            }
            @SuppressWarnings("unchecked")
            List<Object> behaviors = (List<Object>) customItemBehaviors.invoke(customItem);
            if (behaviors == null) {
                return null;
            }
            for (Object behavior : behaviors) {
                if (blockBoundBehaviorClass.isInstance(behavior)) {
                    Object blockKey = blockBoundBehaviorBlock.invoke(behavior);
                    return keyToString(blockKey);
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static String getCustomItemId(@Nullable ItemStack itemStack) {
        if (!isAvailable() || itemStack == null) {
            return null;
        }
        try {
            Object key = itemsGetCustomItemId.invoke(null, itemStack);
            return keyToString(key);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static ItemStack getItemStack(@Nullable String id) {
        if (!isAvailable() || id == null || id.isBlank() || keyOf == null || itemsById == null
                || customItemBuildItemStack == null) {
            return null;
        }
        try {
            Object key = keyOf.invoke(null, id);
            if (key == null) {
                return null;
            }
            Object customItem = itemsById.invoke(null, key);
            if (customItem == null) {
                return null;
            }
            Object stack = customItemBuildItemStack.invoke(customItem);
            return stack instanceof ItemStack ? (ItemStack) stack : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isNamespacedKey(@Nullable String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return NamespacedKey.fromString(key) != null;
    }

    private static boolean init() {
        if (!Bukkit.getPluginManager().isPluginEnabled(PLUGIN_NAME)) {
            return false;
        }
        try {
            Class<?> blocks = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineBlocks");
            Class<?> items = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineItems");
            Class<?> key = Class.forName("net.momirealms.craftengine.core.util.Key");
            Class<?> immutableBlockState = Class.forName("net.momirealms.craftengine.core.block.ImmutableBlockState");
            Class<?> holder = Class.forName("net.momirealms.craftengine.core.registry.Holder");
            Class<?> customBlock = Class.forName("net.momirealms.craftengine.core.block.CustomBlock");
            Class<?> customItem = Class.forName("net.momirealms.craftengine.core.item.CustomItem");

            blocksGetCustomBlockState = blocks.getMethod("getCustomBlockState", BlockData.class);
            itemsGetCustomItemId = items.getMethod("getCustomItemId", ItemStack.class);
            itemsById = items.getMethod("byId", key);
            keyOf = key.getMethod("of", String.class);
            try {
                keyAsString = key.getMethod("asString");
            } catch (NoSuchMethodException e) {
                keyAsString = null;
            }

            immutableBlockStateOwner = immutableBlockState.getMethod("owner");
            holderValue = holder.getMethod("value");
            customBlockId = customBlock.getMethod("id");
            customItemBehaviors = customItem.getMethod("behaviors");
            customItemBuildItemStack = customItem.getMethod("buildItemStack");

            try {
                blockBoundBehaviorClass = Class.forName(
                        "net.momirealms.craftengine.core.item.behavior.BlockBoundItemBehavior");
                blockBoundBehaviorBlock = blockBoundBehaviorClass.getMethod("block");
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                blockBoundBehaviorClass = null;
                blockBoundBehaviorBlock = null;
            }
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return false;
        }
    }

    @Nullable
    private static String keyToString(@Nullable Object key) {
        if (key == null) {
            return null;
        }
        if (keyAsString != null) {
            try {
                return (String) keyAsString.invoke(key);
            } catch (Exception e) {
                return key.toString();
            }
        }
        return key.toString();
    }
}
