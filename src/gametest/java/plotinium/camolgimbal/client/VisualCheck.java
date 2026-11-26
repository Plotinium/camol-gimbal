package plotinium.camolgimbal.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;
import cy.jdkdigital.camol.utils.CamoHelper;
import cy.jdkdigital.camol.utils.CamoPosition;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import plotinium.camolgimbal.CamolGimbal;
import plotinium.camolgimbal.data.SavedCopy;
import plotinium.camolgimbal.item.MenuHook;
import plotinium.camolgimbal.registry.ModRegistry;

/**
 * The visual check, development only (this source set is never shipped). With the system property
 * {@code camolgimbal.visual} set, the client creates a flat creative world, places camouflaged blocks, gives the player
 * a Gimbal with a saved copy, shows every screen state, the label and the preview at two GUI scales, saves one PNG per
 * state in {@code screenshots/} of its folder, logs "visual check: ok" or "visual check: failed: why", and quits. A
 * watchdog halts a client that gets stuck.
 */
@EventBusSubscriber(modid = CamolGimbal.MOD_ID, value = Dist.CLIENT)
public final class VisualCheck {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("camolgimbal.visual");
    private static final long WATCHDOG_MILLIS = 6 * 60 * 1000;
    private static final String WORLD = "visual";
    /** The scales that make a 1280x720 window 427x240, 640x360 and 1280x720, with the width in the file names. */
    private static final int[][] SCALES = {{3, 427}, {2, 640}, {1, 1280}};

    private static final BlockPos TRAPDOOR = new BlockPos(0, -59, 0);
    private static final BlockPos STAIRS = new BlockPos(4, -59, 0);
    private static final BlockPos LOG = new BlockPos(8, -59, 0);
    private static final BlockPos BOOKSHELF = new BlockPos(12, -59, 0);
    private static final BlockPos WIRE = new BlockPos(16, -59, 0);
    private static final BlockPos STONE_CAMO = new BlockPos(20, -59, 0);
    private static final BlockPos FENCE = new BlockPos(24, -59, 0);
    /** The first of a row of plain stone, for the preview. */
    private static final BlockPos PLAIN = new BlockPos(30, -59, 0);

    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static final List<String> FAILURES = new ArrayList<>();
    @Nullable
    private static Step current;
    private static int ticksLeft;
    @Nullable
    private static String wantedShot;
    @Nullable
    private static double[] pointer;
    private static boolean creating;
    private static boolean finished;
    private static int menuTicks;
    private static int worldTicks;
    private static boolean warnedPointer;

    static {
        if (ENABLED) {
            Thread watchdog = new Thread(() -> {
                try {
                    Thread.sleep(WATCHDOG_MILLIS);
                } catch (InterruptedException ignored) {
                    return;
                }
                LOGGER.error("visual check: failed: timed out at step {}", current == null ? "start" : current.name);
                Runtime.getRuntime().halt(3);
            }, "visual-check-watchdog");
            watchdog.setDaemon(true);
            watchdog.start();
        }
    }

    private VisualCheck() {
    }

    private static final class Step {
        private final String name;
        private final Runnable action;
        private final int wait;
        @Nullable
        private final String shot;
        @Nullable
        private final BooleanSupplier check;

        private Step(String name, Runnable action, int wait, @Nullable String shot, @Nullable BooleanSupplier check) {
            this.name = name;
            this.action = action;
            this.wait = wait;
            this.shot = shot;
            this.check = check;
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!ENABLED || finished) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        movePointer();
        if (minecraft.level == null) {
            if (!creating && minecraft.getOverlay() == null && minecraft.screen != null
                    && (minecraft.screen instanceof TitleScreen || ++menuTicks > 200)) {
                creating = true;
                createWorld(minecraft);
            }
            return;
        }
        if (minecraft.player == null || worldTicks < 100) {
            if (minecraft.player != null && minecraft.screen == null) {
                worldTicks++;
                if (worldTicks == 100) {
                    plan();
                }
            }
            return;
        }
        try {
            advance();
        } catch (RuntimeException exception) {
            LOGGER.error("visual check: step {} threw", current == null ? "?" : current.name, exception);
            FAILURES.add((current == null ? "?" : current.name) + " threw " + exception);
            current = null;
            wantedShot = null;
        }
    }

    @SubscribeEvent
    public static void onFrameStart(RenderFrameEvent.Pre event) {
        if (ENABLED && !finished) {
            movePointer();
        }
    }

    @SubscribeEvent
    public static void onFrameEnd(RenderFrameEvent.Post event) {
        if (!ENABLED || wantedShot == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        String name = wantedShot + ".png";
        wantedShot = null;
        Screenshot.grab(minecraft.gameDirectory, name, minecraft.getMainRenderTarget(),
                message -> LOGGER.info("visual check: saved {}", name));
    }

    private static void advance() {
        if (current == null) {
            current = STEPS.poll();
            if (current == null) {
                finish();
                return;
            }
            current.action.run();
            ticksLeft = current.wait;
            return;
        }
        if (--ticksLeft > 0) {
            return;
        }
        if (ticksLeft == 0 && current.shot != null) {
            Minecraft.getInstance().getToasts().clear();
            wantedShot = current.shot;
            return;
        }
        if (wantedShot != null) {
            return;
        }
        if (current.check != null) {
            boolean passed = current.check.getAsBoolean();
            LOGGER.info("visual check: {} {}", passed ? "pass" : "FAIL", current.name);
            if (!passed) {
                FAILURES.add(current.name);
            }
        }
        current = null;
    }

    private static void finish() {
        finished = true;
        if (FAILURES.isEmpty()) {
            LOGGER.info("visual check: ok");
        } else {
            LOGGER.error("visual check: failed: {}", FAILURES);
        }
        Minecraft.getInstance().stop();
    }

    private static void createWorld(Minecraft minecraft) {
        LOGGER.info("visual check: creating the world");
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules,
                WorldDataConfiguration.DEFAULT);
        minecraft.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(1L, false, false),
                access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                        .value().createWorldDimensions(),
                minecraft.screen);
    }

    // The scenes.

    private static void plan() {
        logOfferedStates();
        step("build", () -> server(VisualCheck::build), 100, null, null);
        for (int[] scale : SCALES) {
            planScale(scale[0], "-" + scale[1]);
        }
        planBehaviour();
    }

    private static void planScale(int scale, String suffix) {
        step("scale" + suffix, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.options.guiScale().set(scale);
            minecraft.resizeDisplay();
            server(VisualCheck::reset);
        }, 30, null, () -> {
            Window window = Minecraft.getInstance().getWindow();
            LOGGER.info("visual check: GUI is {}x{}", window.getGuiScaledWidth(), window.getGuiScaledHeight());
            return true;
        });

        // The label and the preview.
        shot("label-trapdoor" + suffix, () -> look(TRAPDOOR), () -> targets(TRAPDOOR));
        shot("label-solid-fence" + suffix, () -> look(FENCE), () -> targets(FENCE));
        shot("label-bookshelf" + suffix, () -> look(BOOKSHELF), () -> targets(BOOKSHELF));
        shot("label-hidden-f3" + suffix, () -> {
            look(TRAPDOOR);
            Minecraft.getInstance().getDebugOverlay().toggleOverlay();
        }, null);
        shot("label-hidden-f1" + suffix, () -> {
            Minecraft.getInstance().getDebugOverlay().toggleOverlay();
            Minecraft.getInstance().options.hideGui = true;
        }, null);
        step("show the HUD", () -> Minecraft.getInstance().options.hideGui = false, 2, null, null);
        shot("preview-one" + suffix, () -> look(PLAIN), () -> targets(PLAIN));
        shot("preview-series" + suffix, () -> GimbalKeys.SERIES.setDown(true), null);
        step("release the series key", () -> GimbalKeys.SERIES.setDown(false), 2, null, null);

        // The main page.
        open("trapdoor", TRAPDOOR);
        shot("menu-camo-trapdoor" + suffix, () -> hoverSector(1, 4), VisualCheck::menuOpen);
        shot("menu-camo-trapdoor-open" + suffix, () -> {
            select(Blocks.OAK_TRAPDOOR, BlockStateProperties.OPEN);
            hoverSector(0, 2);
        }, VisualCheck::menuOpen);
        close();
        open("stairs", STAIRS);
        shot("menu-camo-stairs-shape" + suffix, () -> {
            select(Blocks.OAK_STAIRS, BlockStateProperties.STAIRS_SHAPE);
            hoverSector(2, 5);
        }, VisualCheck::menuOpen);
        close();
        open("log", LOG);
        shot("menu-camo-log" + suffix, () -> hoverSector(0, 3), VisualCheck::menuOpen);
        close();
        open("bookshelf", BOOKSHELF);
        shot("menu-camo-bookshelf" + suffix, () -> select(Blocks.CHISELED_BOOKSHELF,
                BlockStateProperties.CHISELED_BOOKSHELF_SLOT_5_OCCUPIED), VisualCheck::menuOpen);
        close();
        open("wire", WIRE);
        shot("menu-camo-wire-power" + suffix, () -> {
            select(Blocks.REDSTONE_WIRE, BlockStateProperties.POWER);
            hoverSector(12, 16);
        }, VisualCheck::menuOpen);
        close();
        open("fence", FENCE);
        shot("menu-camo-fence" + suffix, () -> hoverSector(1, 2), VisualCheck::menuOpen);
        close();
        open("stone", STONE_CAMO);
        shot("menu-camo-nothing-to-change" + suffix, () -> point(0, 0), VisualCheck::menuOpen);
        close();

        // The saved copy, the settings and the ticks.
        open("saved copy", null);
        shot("menu-copy" + suffix, () -> hoverSector(2, 4), VisualCheck::menuOpen);
        shot("settings" + suffix, () -> {
            press("menu.camolgimbal.settings");
            point(0, 0);
        }, VisualCheck::menuOpen);
        shot("settings-tooltip" + suffix, () -> hoverBeside("menu.camolgimbal.direction.as_copied"),
                VisualCheck::menuOpen);
        shot("ticks" + suffix, () -> {
            press("menu.camolgimbal.states.all");
            point(0, 0);
        }, VisualCheck::menuOpen);
        shot("ticks-one-off" + suffix, () -> press("state.camolgimbal.half"), VisualCheck::menuOpen);
        shot("settings-changed" + suffix, () -> {
            press("menu.camolgimbal.back");
            press("menu.camolgimbal.paste_type.copied");
            press("menu.camolgimbal.replace.kept");
            point(0, 0);
        }, VisualCheck::menuOpen);
        step("back to the main page", () -> press("menu.camolgimbal.back"), 5, null, VisualCheck::menuOpen);
        shot("menu-copy-cleared" + suffix, () -> {
            press("menu.camolgimbal.clear");
            point(0, 0);
        }, () -> menuOpen() && MenuState.gimbal().get(ModRegistry.COPY) == null);
        close();
        open("camo without a copy", TRAPDOOR);
        shot("menu-camo-no-copy" + suffix, () -> point(0, 0), VisualCheck::menuOpen);
        close();

        // The longest copy: a Shape Retaining camo with seven states.
        step("copy a bookshelf", () -> server(VisualCheck::copyBookshelf), 20, null, null);
        open("long copy", null);
        shot("menu-copy-long" + suffix, () -> hoverSector(1, 4), VisualCheck::menuOpen);
        shot("ticks-long" + suffix, () -> {
            press("menu.camolgimbal.settings");
            press("menu.camolgimbal.states.all");
            point(0, 0);
        }, VisualCheck::menuOpen);
        close();
        step("reset after the long copy", () -> server(VisualCheck::reset), 20, null, null);
    }

    /** What the screenshots cannot show: clicks and releases. */
    private static void planBehaviour() {
        step("reset for the checks", () -> server(VisualCheck::reset), 30, null, null);
        open("click check", TRAPDOOR);
        step("a click on a sector applies and keeps the menu", () -> {
            select(Blocks.OAK_TRAPDOOR, BlockStateProperties.OPEN);
            // The values of a yes or no are listed yes first.
            clickSector(0, 2);
        }, 20, null, () -> menuOpen() && value(TRAPDOOR, BlockStateProperties.OPEN));
        // The second edit is sent before the server's answer to the first one has arrived.
        step("two edits at once keep each other", () -> {
            select(Blocks.OAK_TRAPDOOR, BlockStateProperties.HALF);
            clickSector(0, 2);
            select(Blocks.OAK_TRAPDOOR, BlockStateProperties.HORIZONTAL_FACING);
            // North at the top, then east, south and west.
            clickSector(2, 4);
        }, 30, null, () -> menuOpen() && value(TRAPDOOR, BlockStateProperties.OPEN)
                && camo(TRAPDOOR).getValue(BlockStateProperties.HALF) == Half.TOP
                && camo(TRAPDOOR).getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.SOUTH);
        step("a right click closes", () -> screen().mouseClicked(1, 1, InputConstants.MOUSE_BUTTON_RIGHT), 5, null,
                () -> Minecraft.getInstance().screen == null);

        step("look for the held mode checks", () -> look(TRAPDOOR), 20, null, () -> targets(TRAPDOOR));
        // The menu key is not physically down, so the next tick of the menu takes it as released.
        step("releasing over a sector applies and closes", () -> {
            openHeld();
            hoverSector(1, 4);
        }, 20, null, () -> {
            boolean active = Minecraft.getInstance().isWindowActive();
            LOGGER.info("visual check: window active {}", active);
            // An inactive window applies nothing on release.
            Direction expected = active ? Direction.EAST : Direction.SOUTH;
            return Minecraft.getInstance().screen == null
                    && camo(TRAPDOOR).getValue(BlockStateProperties.HORIZONTAL_FACING) == expected;
        });
        BlockState[] before = new BlockState[1];
        step("releasing over the list applies nothing", () -> {
            before[0] = camo(TRAPDOOR);
            openHeld();
            rebuild();
            AbstractWidget row = widget("state.camolgimbal.facing");
            point(row.getX() + row.getWidth() / 2.0, row.getY() + row.getHeight() / 2.0);
        }, 20, null, () -> Minecraft.getInstance().screen == null && camo(TRAPDOOR) == before[0]);
        step("releasing on the settings page keeps it open", () -> {
            openHeld();
            press("menu.camolgimbal.settings");
        }, 20, null, VisualCheck::menuOpen);
        close();
    }

    private static void logOfferedStates() {
        Map<String, Block> blocks = new LinkedHashMap<>();
        for (Block block : new Block[] {Blocks.OAK_TRAPDOOR, Blocks.OAK_STAIRS, Blocks.OAK_LOG, Blocks.OAK_DOOR,
                Blocks.OAK_SLAB, Blocks.OAK_FENCE, Blocks.OAK_FENCE_GATE, Blocks.COBBLESTONE_WALL,
                Blocks.CHISELED_BOOKSHELF, Blocks.REDSTONE_WIRE, Blocks.LANTERN, Blocks.GRASS_BLOCK, Blocks.WHEAT,
                Blocks.FURNACE, Blocks.REDSTONE_LAMP, Blocks.OAK_LEAVES, Blocks.NOTE_BLOCK, Blocks.STONE}) {
            blocks.put(block.getDescriptionId(), block);
        }
        blocks.forEach((name, block) -> LOGGER.info("visual check: offered states of {}: {}", name,
                BlockLooks.visibleStates(block).stream().map(Property::getName).toList()));
        List<String> trapdoor = BlockLooks.visibleStates(Blocks.OAK_TRAPDOOR).stream().map(Property::getName)
                .toList();
        if (!trapdoor.equals(List.of("facing", "half", "open"))) {
            FAILURES.add("the oak trapdoor offers " + trapdoor);
        }
    }

    // Steps.

    private static void step(String name, Runnable action, int wait, @Nullable String shot,
            @Nullable BooleanSupplier check) {
        STEPS.add(new Step(name, action, wait, shot, check));
    }

    private static void shot(String name, Runnable action, @Nullable BooleanSupplier check) {
        step(name, action, 25, name, check);
    }

    private static void open(String what, @Nullable BlockPos pos) {
        if (pos != null) {
            step("look at " + what, () -> look(pos), 15, null, () -> targets(pos));
        }
        step("open on " + what, () -> {
            point(0, 0);
            MenuHook.open(InteractionHand.MAIN_HAND, pos == null ? null : hit());
        }, 5, null, VisualCheck::menuOpen);
    }

    private static void openHeld() {
        GimbalMenuScreen.open(hit(), true);
    }

    private static void close() {
        step("close", () -> {
            Minecraft.getInstance().setScreen(null);
            pointer = null;
        }, 3, null, null);
    }

    // The client.

    /** Builds the page again now, as the next frame would: the steps act between two frames. */
    private static void rebuild() {
        Window window = Minecraft.getInstance().getWindow();
        screen().resize(Minecraft.getInstance(), window.getGuiScaledWidth(), window.getGuiScaledHeight());
    }

    private static boolean menuOpen() {
        return Minecraft.getInstance().screen instanceof GimbalMenuScreen;
    }

    private static Screen screen() {
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null) {
            throw new IllegalStateException("no screen");
        }
        return screen;
    }

    @Nullable
    private static BlockHitResult hit() {
        return Minecraft.getInstance().hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    private static boolean targets(BlockPos pos) {
        BlockHitResult hit = hit();
        return hit != null && hit.getBlockPos().equals(pos);
    }

    private static BlockState camo(BlockPos pos) {
        CamoPosition camo = ClientCamos.VIEW.camoAt(pos);
        if (camo == null) {
            throw new IllegalStateException("no camo at " + pos);
        }
        return camo.state();
    }

    private static boolean value(BlockPos pos, Property<Boolean> property) {
        return camo(pos).getValue(property);
    }

    /** Picks a state with the mouse wheel; a state the menu does not offer changes nothing. */
    private static void select(Block block, Property<?> property) {
        int index = BlockLooks.visibleStates(block).indexOf(property);
        if (index < 0) {
            return;
        }
        for (int i = 0; i < 64; i++) {
            screen().mouseScrolled(0, 0, 0, 1);
        }
        for (int i = 0; i < index; i++) {
            screen().mouseScrolled(0, 0, 0, -1);
        }
    }

    /** A point in the ring, in the middle of a sector. The wheel is centred, a little below the middle. */
    private static double[] sector(int index, int sectors) {
        Window window = Minecraft.getInstance().getWindow();
        double radians = Math.toRadians(index * 360.0 / sectors);
        // Inside the ring at every size: the wheel stops growing at a radius of 100.
        double radius = Math.min(window.getGuiScaledHeight() * 0.23, 78);
        return new double[] {window.getGuiScaledWidth() / 2.0 + Math.sin(radians) * radius,
                window.getGuiScaledHeight() / 2.0 + 6 - Math.cos(radians) * radius};
    }

    private static void hoverSector(int index, int sectors) {
        double[] at = sector(index, sectors);
        point(at[0], at[1]);
    }

    private static void clickSector(int index, int sectors) {
        rebuild();
        double[] at = sector(index, sectors);
        screen().mouseClicked(at[0], at[1], InputConstants.MOUSE_BUTTON_LEFT);
    }

    private static AbstractWidget widget(String key) {
        for (GuiEventListener child : screen().children()) {
            if (child instanceof AbstractWidget widget
                    && widget.getMessage().getContents() instanceof TranslatableContents contents
                    && contents.getKey().equals(key)) {
                return widget;
            }
        }
        throw new IllegalStateException("no button " + key);
    }

    private static void press(String key) {
        rebuild();
        AbstractWidget widget = widget(key);
        screen().mouseClicked(widget.getX() + widget.getWidth() / 2.0, widget.getY() + widget.getHeight() / 2.0,
                InputConstants.MOUSE_BUTTON_LEFT);
        rebuild();
    }

    private static void hoverBeside(String key) {
        rebuild();
        AbstractWidget widget = widget(key);
        point(widget.getX() - 50, widget.getY() + widget.getHeight() / 2.0);
    }

    /** Where the mouse rests, in scaled pixels, until it is moved again. */
    private static void point(double x, double y) {
        pointer = new double[] {x, y};
        movePointer();
    }

    private static void movePointer() {
        Minecraft minecraft = Minecraft.getInstance();
        if (pointer == null || minecraft.screen == null) {
            return;
        }
        Window window = minecraft.getWindow();
        double x = pointer[0] * window.getScreenWidth() / window.getGuiScaledWidth();
        double y = pointer[1] * window.getScreenHeight() / window.getGuiScaledHeight();
        try {
            // Written into the mouse handler, so that the real cursor of whoever uses the computer stays put.
            set(minecraft.mouseHandler, "xpos", x);
            set(minecraft.mouseHandler, "ypos", y);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (!warnedPointer) {
                warnedPointer = true;
                LOGGER.warn("visual check: cannot place the pointer directly", exception);
            }
            GLFW.glfwSetCursorPos(window.getWindow(), x, y);
        }
    }

    private static void set(Object owner, String name, double value) throws ReflectiveOperationException {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.setDouble(owner, value);
    }

    // The server.

    private static void server(Consumer<ServerPlayer> action) {
        Minecraft minecraft = Minecraft.getInstance();
        var server = minecraft.getSingleplayerServer();
        var id = minecraft.player.getUUID();
        server.execute(() -> action.accept(server.getPlayerList().getPlayer(id)));
    }

    /** Stands the player a little to the side of a block, looking at its middle. */
    private static void look(BlockPos pos) {
        server(player -> {
            double x = pos.getX() + 1.7;
            double y = -60;
            double z = pos.getZ() + 2.6;
            double dx = pos.getX() + 0.5 - x;
            double dy = pos.getY() + 0.5 - (y + player.getEyeHeight());
            double dz = pos.getZ() + 0.5 - z;
            float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
            float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
            player.teleportTo(player.serverLevel(), x, y, z, yaw, pitch);
        });
    }

    private static void build(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.setDayTime(6000);
        Map<BlockPos, CamoPosition> camos = new LinkedHashMap<>();
        camos.put(TRAPDOOR, normal(Blocks.OAK_TRAPDOOR.defaultBlockState()));
        camos.put(STAIRS, normal(Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)));
        camos.put(LOG, normal(Blocks.OAK_LOG.defaultBlockState()));
        camos.put(BOOKSHELF, normal(Blocks.CHISELED_BOOKSHELF.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
                .setValue(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_1_OCCUPIED, true)));
        camos.put(WIRE, normal(Blocks.REDSTONE_WIRE.defaultBlockState().setValue(BlockStateProperties.POWER, 7)));
        camos.put(STONE_CAMO, normal(Blocks.SMOOTH_STONE.defaultBlockState()));
        camos.put(FENCE, new CamoPosition(SavedCopy.SOLID, Blocks.DARK_OAK_FENCE.defaultBlockState()
                .setValue(BlockStateProperties.NORTH, true).setValue(BlockStateProperties.EAST, true)));
        camos.keySet().forEach(pos -> level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState()));
        for (int i = -1; i < 3; i++) {
            level.setBlockAndUpdate(PLAIN.east(i), Blocks.STONE.defaultBlockState());
        }
        CamoHelper.setCamo(level, camos);
    }

    private static CamoPosition normal(BlockState state) {
        return new CamoPosition(SavedCopy.NORMAL, state);
    }

    private static void copyBookshelf(ServerPlayer player) {
        player.getMainHandItem().set(ModRegistry.COPY, new SavedCopy(SavedCopy.SOLID,
                Blocks.CHISELED_BOOKSHELF.defaultBlockState()
                        .setValue(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_2_OCCUPIED, true), Direction.NORTH));
    }

    /** A fresh Gimbal with a saved copy of stairs, and the camos as built. */
    private static void reset(ServerPlayer player) {
        build(player);
        ItemStack gimbal = new ItemStack(ModRegistry.GIMBAL.get());
        gimbal.set(ModRegistry.COPY, new SavedCopy(SavedCopy.NORMAL, Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST), Direction.NORTH));
        player.getInventory().clearContent();
        player.getInventory().selected = 0;
        player.setItemInHand(InteractionHand.MAIN_HAND, gimbal);
    }
}
