package plotinium.camolgimbal.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Reports a mixin with {@code require = 0} that found nothing to change. It looks, in the transformed class, for a
 * call to the mixin's handler inside the targeted method; it references no game class, so it is safe on either side.
 */
public final class CamolGimbalMixinPlugin implements IMixinConfigPlugin {
    static final String DRAWING_METHOD = "lambda$geometryEvent$5";
    private static final String DRAWING_MIXIN = "plotinium.camolgimbal.mixin.CamolDrawingMixin";
    private static final String DRAWING_HANDLER = "camolgimbal$drawSeeThrough";

    private static final Logger LOGGER = LoggerFactory.getLogger("camolgimbal");

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (DRAWING_MIXIN.equals(mixinClassName) && !callsHandler(targetClass, DRAWING_METHOD, DRAWING_HANDLER)) {
            LOGGER.warn("Camol's {}.{} was not found or has changed: camouflaged blocks are not drawn see-through "
                    + "while a Gimbal is held", targetClassName, DRAWING_METHOD);
        }
    }

    private static boolean callsHandler(ClassNode targetClass, String methodName, String handlerName) {
        for (MethodNode method : targetClass.methods) {
            if (!method.name.equals(methodName)) {
                continue;
            }
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn.getOpcode() == Opcodes.INVOKESTATIC && insn instanceof MethodInsnNode call
                        && call.owner.equals(targetClass.name)
                        && (call.name.equals(handlerName) || call.name.endsWith("$" + handlerName))) {
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
