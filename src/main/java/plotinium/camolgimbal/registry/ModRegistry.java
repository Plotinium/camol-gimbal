package plotinium.camolgimbal.registry;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import plotinium.camolgimbal.CamolGimbal;
import plotinium.camolgimbal.data.GimbalSettings;
import plotinium.camolgimbal.data.SavedCopy;
import plotinium.camolgimbal.item.GimbalItem;

public final class ModRegistry {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CamolGimbal.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(CamolGimbal.MOD_ID);

    public static final DeferredItem<GimbalItem> GIMBAL =
            ITEMS.registerItem("gimbal", GimbalItem::new, new Item.Properties().stacksTo(1));

    /** The camouflage copied into a Gimbal; absent while nothing is copied. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SavedCopy>> COPY =
            COMPONENTS.registerComponentType("copy",
                    builder -> builder.persistent(SavedCopy.CODEC).networkSynchronized(SavedCopy.STREAM_CODEC));

    /** The switches of a Gimbal; absent while they are all at their default. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GimbalSettings>> SETTINGS =
            COMPONENTS.registerComponentType("settings",
                    builder -> builder.persistent(GimbalSettings.CODEC)
                            .networkSynchronized(GimbalSettings.STREAM_CODEC));

    private ModRegistry() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        COMPONENTS.register(modBus);
        modBus.addListener(ModRegistry::fillCreativeTab);
    }

    private static void fillCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(GIMBAL.get());
        }
    }
}
