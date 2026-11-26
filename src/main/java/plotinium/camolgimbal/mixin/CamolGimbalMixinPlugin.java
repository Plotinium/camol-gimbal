package plotinium.camolgimbal.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Reports a mixin with {@code require = 0} that found nothing to change: one warning when its target class loads.
 * It looks, in the transformed class, for a call to the mixin's handler inside the targeted method. It references no
 * game class, so it is safe on either side.
 */
public final class CamolGimbalMixinPlugin implements IMixinConfigPlugin {
    static final String DRAWING_METHOD = "lambda$geometryEvent$5";

    static final String CAMO_ITEM_METHOD = "useOn";

    /**
     * Mixins with {@code require = 0}: the method they change, a call that must be in it after the mixin, and what is
     * lost without it. The drawing mixin is checked by the call to its handler. The camo item mixin is a MixinExtras
     * wrap, which is applied after this plugin runs, so it is checked by the call it wraps.
     */
    private record OptionalMixin(String mixin, String method, String callOwner, String callName, String lost) {
    }

    private static final List<OptionalMixin> OPTIONAL = List.of(
            new OptionalMixin("plotinium.camolgimbal.mixin.CamolDrawingMixin", DRAWING_METHOD,
                    "cy/jdkdigital/camol/event/ClientEventHandler", "camolgimbal$drawSeeThrough",
                    "camouflaged blocks are not drawn see-through while a Gimbal is held"),
            new OptionalMixin("plotinium.camolgimbal.mixin.CamoItemMixin", CAMO_ITEM_METHOD,
                    "net/minecraft/world/level/block/Block", "getStateForPlacement",
                    "camo items do not follow the clicked face"));

    private static final Logger LOGGER = LoggerFactory.getLogger("camolgimbal");

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        for (OptionalMixin optional : OPTIONAL) {
            if (optional.mixin().equals(mixinClassName)
                    && !calls(targetClass, optional.method(), optional.callOwner(), optional.callName())) {
                LOGGER.warn("Camol's {}.{} was not found or has changed: {}", targetClassName, optional.method(),
                        optional.lost());
            }
        }
    }

    /** Whether the method calls {@code owner.name}, or a merged handler whose name ends with {@code $name}. */
    private static boolean calls(ClassNode targetClass, String methodName, String owner, String name) {
        for (MethodNode method : targetClass.methods) {
            if (!method.name.equals(methodName)) {
                continue;
            }
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode call && call.owner.equals(owner)
                        && (call.name.equals(name) || call.name.endsWith("$" + name))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
