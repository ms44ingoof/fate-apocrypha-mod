package com.example.fateapocrypha.world;

import java.util.Random;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Обстановка замка поверх построенной оболочки: тронный зал во дворце на крыше, четыре мастерские во дворе
 * (кузница, оружейная, кухня, мастерская мага) и сундуки с ресурсами в библиотеке, пиршественном зале и спальнях.
 * Запускается последним шагом CastleBuilder, всё в мировых координатах.
 */
final class CastleInterior {
    private static final int Y = CastleSite.Y;
    private static final int X0 = CastleSite.X0;
    private static final int X1 = CastleSite.X0 + CastleSite.W;
    private static final int CX = CastleSite.CX;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final ServerLevel level;
    private final BuildKit k;
    private final Random rnd = new Random(1305L);

    private CastleInterior(ServerLevel level) {
        this.level = level;
        this.k = new BuildKit(level);
    }

    static void build(ServerLevel level) {
        CastleInterior in = new CastleInterior(level);
        in.throneRoom();
        in.workshops();
        in.hallDecor();
        in.firstFloor();
        in.upperRooms();
        in.grounds();
        in.landmarks();
        in.life();
    }

    private static BlockState s(Block block) {
        return block.defaultBlockState();
    }

    private static BlockState facing(Block block, Direction dir) {
        return block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, dir);
    }

    private void chest(int x, int y, int z, Direction dir, LootFiller.Kind kind) {
        k.set(x, y, z, facing(Blocks.CHEST, dir));
        LootFiller.fill(level, x, y, z, rnd, kind);
    }

    private void barrel(int x, int y, int z, LootFiller.Kind kind) {
        k.set(x, y, z, s(Blocks.BARREL));
        LootFiller.fill(level, x, y, z, rnd, kind);
    }

    // ================= тронный зал (дворец на крыше корпуса) =================
    private void throneRoom() {
        int base = Y + 24;           // уровень пола дворца
        int z1 = 184;                // северная стена зала (внутри)
        int z2 = 206;                // южная стена, у двери с террасы
        // дорожка от двери к трону: красная с синей каймой
        for (int z = 190; z <= z2; z++) {
            for (int dx = -1; dx <= 1; dx++) {
                k.set(CX + dx, base, z, s(Blocks.RED_CARPET));
            }
            k.set(CX - 2, base, z, s(Blocks.BLUE_CARPET));
            k.set(CX + 2, base, z, s(Blocks.BLUE_CARPET));
        }
        // помост в два яруса: нижний широкий, верхний под трон
        k.fill(CX - 8, base, z1, CX + 8, base, 189, s(Blocks.SMOOTH_QUARTZ));
        for (int x = CX - 8; x <= CX + 8; x++) {
            k.set(x, base, 190, BuildKit.stairs(Blocks.QUARTZ_STAIRS, Direction.NORTH, false));
        }
        k.fill(CX - 5, base + 1, z1, CX + 5, base + 1, 186, s(Blocks.SMOOTH_QUARTZ));
        for (int x = CX - 5; x <= CX + 5; x++) {
            k.set(x, base + 1, 187, BuildKit.stairs(Blocks.QUARTZ_STAIRS, Direction.NORTH, false));
        }
        k.fill(CX - 8, base, 189, CX + 8, base, 189, s(Blocks.GOLD_BLOCK));   // золотая кайма под нижней ступенью
        // трон: сиденье, подлокотники и высокая спинка из резного кварца с золотой короной
        k.set(CX, base + 2, 185, BuildKit.stairs(Blocks.QUARTZ_STAIRS, Direction.NORTH, false));
        k.set(CX - 1, base + 2, 185, s(Blocks.QUARTZ_SLAB));
        k.set(CX + 1, base + 2, 185, s(Blocks.QUARTZ_SLAB));
        k.fill(CX - 1, base + 2, z1, CX + 1, base + 7, z1, s(Blocks.CHISELED_QUARTZ_BLOCK));
        k.fill(CX, base + 8, z1, CX, base + 8, z1, s(Blocks.GOLD_BLOCK));
        k.set(CX - 1, base + 8, z1, s(Blocks.GOLD_BLOCK));
        k.set(CX + 1, base + 8, z1, s(Blocks.GOLD_BLOCK));
        // знамёна на северной стене
        for (int dx : new int[] {-12, -8, -4, 4, 8, 12}) {
            k.set(CX + dx, base + 4, z1, s(Blocks.BLUE_WALL_BANNER).setValue(WallBannerBlock.FACING, Direction.SOUTH));
        }
        // колонны вдоль зала: кварц, золотое основание, капитель и фонарь
        for (int x : new int[] {CX - 17, CX + 17}) {
            for (int z = 191; z <= 203; z += 6) {
                k.set(x, base, z, s(Blocks.GOLD_BLOCK));
                k.fill(x, base + 1, z, x, base + 8, z, s(Blocks.QUARTZ_PILLAR));
                k.set(x, base + 9, z, s(Blocks.CHISELED_QUARTZ_BLOCK));
                k.set(x, base + 10, z, s(Blocks.LANTERN));
            }
        }
        // люстры под коньком крыши: цепь, золотое кольцо и фонари
        for (int x : new int[] {CX - 9, CX, CX + 9}) {
            for (int y = base + 24; y >= base + 18; y--) {
                k.set(x, y, 195, s(Blocks.CHAIN));
            }
            k.set(x, base + 17, 195, BuildKit.HANGING_LANTERN);
            for (int[] d : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                k.set(x + d[0], base + 18, 195 + d[1], BuildKit.HANGING_LANTERN);
            }
        }
        // сундуки с сокровищами по краям помоста
        chest(CX - 7, base + 1, 185, Direction.SOUTH, LootFiller.Kind.TREASURE);
        chest(CX + 7, base + 1, 185, Direction.SOUTH, LootFiller.Kind.TREASURE);
        // скамьи вдоль боковых стен
        for (int z = 192; z <= 200; z += 2) {
            k.set(194, base, z, BuildKit.stairs(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
            k.set(236, base, z, BuildKit.stairs(Blocks.SPRUCE_STAIRS, Direction.WEST, false));
        }
    }

    // ================= мастерские во дворе =================
    private void workshops() {
        smithy(X0 + 8, 255, X0 + 20, 266);
        armory(X1 - 20, 255, X1 - 8, 266);
        kitchen(X0 + 8, 326, X0 + 20, 337);
        alchemy(X1 - 20, 326, X1 - 8, 337);
    }

    /** Кузница: горн, наковальни, точило, сундук и бочка с железом. */
    private void smithy(int x1, int z1, int x2, int z2) {
        int y = Y + 1;
        k.set(x1 + 3, y, z1 + 1, facing(Blocks.BLAST_FURNACE, Direction.SOUTH));
        k.set(x1 + 4, y, z1 + 1, s(Blocks.GRINDSTONE));
        k.set(x1 + 5, y, z1 + 1, s(Blocks.SMITHING_TABLE));
        k.set(x2 - 2, y, z1 + 1, s(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, 3));
        k.set(x1 + 1, y, z1 + 4, s(Blocks.ANVIL));
        chest(x1 + 1, y, z1 + 3, Direction.EAST, LootFiller.Kind.SMITHY);
        barrel(x2 - 1, y, z1 + 1, LootFiller.Kind.SMITHY);
        k.set(x2 - 1, y, z1 + 3, s(Blocks.IRON_BLOCK));
    }

    /** Оружейная: стойки с сундуками, точило, стол лучника. */
    private void armory(int x1, int z1, int x2, int z2) {
        int y = Y + 1;
        for (int dx : new int[] {4, 6, 8}) {
            chest(x1 + dx, y, z1 + 1, Direction.SOUTH, LootFiller.Kind.ARMORY);
        }
        k.set(x1 + 10, y, z1 + 1, s(Blocks.GRINDSTONE));
        k.set(x1 + 10, y, z1 + 3, s(Blocks.FLETCHING_TABLE));
        barrel(x2 - 1, y, z1 + 1, LootFiller.Kind.ARMORY);
        k.fill(x1 + 2, y, z1 + 5, x1 + 4, y, z1 + 5, s(Blocks.RED_CARPET));
        k.set(x1 + 3, y, z1 + 4, s(Blocks.TARGET));
    }

    /** Кухня: коптильня, бочки с едой, сено, стол. */
    private void kitchen(int x1, int z1, int x2, int z2) {
        int y = Y + 1;
        k.set(x1 + 3, y, z1 + 1, facing(Blocks.SMOKER, Direction.SOUTH));
        k.set(x1 + 4, y, z1 + 1, s(Blocks.COMPOSTER));
        barrel(x1 + 6, y, z1 + 1, LootFiller.Kind.KITCHEN);
        barrel(x1 + 8, y, z1 + 1, LootFiller.Kind.KITCHEN);
        barrel(x2 - 1, y, z1 + 1, LootFiller.Kind.KITCHEN);
        chest(x2 - 1, y, z1 + 4, Direction.WEST, LootFiller.Kind.KITCHEN);
        k.set(x1 + 1, y, z1 + 5, s(Blocks.HAY_BLOCK));
        k.set(x1 + 1, y, z1 + 6, s(Blocks.HAY_BLOCK));
        for (int dx = 4; dx <= 6; dx++) {
            k.set(x1 + dx, y, z1 + 5, s(Blocks.SPRUCE_FENCE));
            k.set(x1 + dx, y + 1, z1 + 5, s(Blocks.SPRUCE_PRESSURE_PLATE));
        }
    }

    /** Мастерская мага: стойки зельеварения, стол зачарований, книжные полки. */
    private void alchemy(int x1, int z1, int x2, int z2) {
        int y = Y + 1;
        k.set(x1 + 3, y, z1 + 1, s(Blocks.BREWING_STAND));
        k.set(x1 + 4, y, z1 + 1, s(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, 3));
        k.set(x1 + 5, y, z1 + 1, s(Blocks.BREWING_STAND));
        barrel(x2 - 1, y, z1 + 1, LootFiller.Kind.WORKSHOP);
        chest(x1 + 1, y, z1 + 4, Direction.EAST, LootFiller.Kind.WORKSHOP);
        for (int z = z1 + 3; z <= z1 + 8; z++) {
            k.fill(x2 - 1, y, z, x2 - 1, y + 2, z, s(Blocks.BOOKSHELF));
        }
        k.set(x2 - 3, y, z1 + 5, s(Blocks.ENCHANTING_TABLE));
        k.set(x1 + 1, y, z1 + 7, facing(Blocks.LECTERN, Direction.EAST));
        for (int[] c : new int[][] {{x1 + 2, z1 + 8}, {x2 - 3, z1 + 8}}) {
            k.set(c[0], y, c[1], s(Blocks.CANDLE).setValue(CandleBlock.LIT, true));
        }
    }

    // ================= общие помощники для мебели =================
    private void bed(int x, int y, int z, Direction head, Block bed) {
        k.set(x, y, z, s(bed).setValue(BedBlock.FACING, head).setValue(BedBlock.PART, BedPart.FOOT));
        k.set(x + head.getStepX(), y, z + head.getStepZ(),
                s(bed).setValue(BedBlock.FACING, head).setValue(BedBlock.PART, BedPart.HEAD));
    }

    /** Стул: человек смотрит в направлении look, спинка за ним. */
    private void chair(int x, int y, int z, Direction look) {
        k.set(x, y, z, BuildKit.stairs(Blocks.SPRUCE_STAIRS, look.getOpposite(), false));
    }

    private void tableTop(int x1, int z1, int x2, int z2, int y) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                k.set(x, y, z, s(Blocks.SPRUCE_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP));
            }
        }
    }

    private void carpet(int x1, int z1, int x2, int z2, int y, Block block) {
        k.fill(x1, y, z1, x2, y, z2, s(block));
    }

    private void candle(int x, int y, int z) {
        k.set(x, y, z, s(Blocks.CANDLE).setValue(CandleBlock.LIT, true));
    }

    private void pot(int x, int y, int z) {
        Block[] pots = {Blocks.POTTED_FERN, Blocks.POTTED_RED_TULIP, Blocks.POTTED_BLUE_ORCHID,
                Blocks.POTTED_DANDELION, Blocks.POTTED_CORNFLOWER, Blocks.POTTED_ALLIUM};
        k.set(x, y, z, s(pots[rnd.nextInt(pots.length)]));
    }

    /** Колонка в крыле: глубина kk от наружной стены (0) к стене зала (20). */
    private static int wingX(boolean west, int kk) {
        return west ? CastleSite.KX1 + 3 + kk : CastleSite.KX2 - 3 - kk;
    }

    // ================= большой зал: скамьи и знамёна между пилястрами =================
    private void hallDecor() {
        for (int z = 186; z <= 238; z += 8) {
            for (int dz = 2; dz <= 6; dz++) {
                k.set(187, Y + 1, z + dz, BuildKit.stairs(Blocks.SPRUCE_STAIRS, Direction.WEST, false));
                k.set(243, Y + 1, z + dz, BuildKit.stairs(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
            }
            k.set(186, Y + 5, z + 4, s(Blocks.BLUE_WALL_BANNER).setValue(WallBannerBlock.FACING, Direction.EAST));
            k.set(244, Y + 5, z + 4, s(Blocks.BLUE_WALL_BANNER).setValue(WallBannerBlock.FACING, Direction.WEST));
            pot(187, Y + 1, z + 1);
            pot(243, Y + 1, z + 1);
        }
    }

    // ================= первый этаж крыльев: камины, кресла, свечи =================
    private void firstFloor() {
        for (int side = 0; side < 2; side++) {
            boolean west = side == 0;
            int cxw = west ? 173 : 257;
            // камин в южной стене: каменная кладка, очаг с костром, полка и фонари
            k.fill(cxw - 2, Y + 1, 243, cxw + 2, Y + 6, 244, s(Blocks.STONE_BRICKS));
            k.fill(cxw - 1, Y + 1, 243, cxw + 1, Y + 3, 244, AIR);
            k.set(cxw, Y + 1, 244, s(Blocks.CAMPFIRE));
            k.fill(cxw - 2, Y + 4, 242, cxw + 2, Y + 4, 242, s(Blocks.STONE_BRICK_SLAB));
            k.set(cxw - 2, Y + 5, 242, s(Blocks.LANTERN));
            k.set(cxw + 2, Y + 5, 242, s(Blocks.LANTERN));
            if (west) {
                carpet(cxw - 3, 237, cxw + 3, 241, Y + 1, Blocks.CYAN_CARPET);
                chair(cxw - 2, Y + 1, 240, Direction.SOUTH);
                chair(cxw + 2, Y + 1, 240, Direction.SOUTH);
                k.set(cxw, Y + 1, 240, s(Blocks.SPRUCE_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP));
            }
        }
        // библиотека: стулья у столов и свечи
        int x1 = 163;
        for (int z = 190; z <= 222; z += 8) {
            chair(x1 + 9, Y + 1, z, Direction.WEST);
            candle(x1 + 6, Y + 2, z);
            candle(x1 + 8, Y + 2, z);
            pot(x1 + 4, Y + 2, z);
            chair(x1 + 5, Y + 1, z - 1, Direction.SOUTH);
        }
        for (int z : new int[] {187, 203, 219}) {
            chest(x1 + 1, Y + 1, z, Direction.EAST, LootFiller.Kind.LIBRARY);
        }
        // пиршественный зал: свечи, пироги, кружки (бочонки с едой у стены)
        int fx1 = 247;
        int fx2 = 267;
        for (int z = 186; z <= 240; z += 6) {
            candle(fx1 + 8, Y + 2, z);
            candle(fx2 - 8, Y + 2, z);
            k.set(257, Y + 2, z, s(Blocks.CAKE));
        }
        for (int z : new int[] {190, 202, 214}) {
            barrel(fx2 - 1, Y + 1, z, LootFiller.Kind.KITCHEN);
        }
        for (int z : new int[] {184, 196, 208}) {
            chair(fx1 + 5, Y + 1, z + 2, Direction.EAST);
            chair(fx2 - 5, Y + 1, z + 2, Direction.WEST);
        }
        // тяжёлый стол во главе зала, у северной стены
        tableTop(fx1 + 6, 182, fx2 - 6, 183, Y + 1);
        for (int x = fx1 + 6; x <= fx2 - 6; x += 2) {
            chair(x, Y + 1, 184, Direction.NORTH);
        }
    }

    // ================= второй этаж крыльев: разные комнаты =================
    private void upperRooms() {
        int y = CastleSite.F2 + 1;
        int[][] R = CastleSite.ROOMS;
        // ---------- западное крыло ----------
        boolean w = true;
        Direction in = Direction.EAST;
        Direction out = Direction.WEST;
        // R0: спальня (две кровати у стены зала, ковёр, сундук между ними)
        for (int z : new int[] {186, 191}) {
            bed(wingX(w, 17), y, z, in, Blocks.RED_BED);
        }
        chest(wingX(w, 18), y, 188, out, LootFiller.Kind.BEDROOM);
        k.set(wingX(w, 18), y + 1, 188, s(Blocks.LANTERN));
        carpet(wingX(w, 8), 185, wingX(w, 15), 192, y, Blocks.PURPLE_CARPET);
        carpet(wingX(w, 9), 186, wingX(w, 14), 191, y, Blocks.WHITE_CARPET);
        pot(wingX(w, 19), y, 185);
        pot(wingX(w, 19), y, 193);
        barrel(wingX(w, 5), y, 185, LootFiller.Kind.BEDROOM);
        // R1: кабинет (стол с креслами, стеллажи, глобус из блоков, зона отдыха), проход с балкона по z197-199
        tableTop(wingX(w, 8), 201, wingX(w, 10), 203, y);
        chair(wingX(w, 7), y, 202, in);
        chair(wingX(w, 11), y, 202, out);
        chair(wingX(w, 9), y, 204, Direction.NORTH);
        candle(wingX(w, 9), y + 1, 202);
        for (int z : new int[] {197, 198, 199, 203, 204, 205}) {
            k.fill(wingX(w, 0), y, z, wingX(w, 0), y + 2, z, s(Blocks.BOOKSHELF));
        }
        k.set(wingX(w, 1), y, 205, s(Blocks.LECTERN));
        chest(wingX(w, 1), y, 197, in, LootFiller.Kind.LIBRARY);
        carpet(wingX(w, 5), 200, wingX(w, 13), 205, y, Blocks.RED_CARPET);
        k.set(wingX(w, 19), y, 205, s(Blocks.CARTOGRAPHY_TABLE));
        k.set(wingX(w, 19), y, 203, s(Blocks.BARREL));
        pot(wingX(w, 19), y, 201);
        // R2: казарма (четыре койки, сундуки, оружейный угол)
        for (int z : new int[] {209, 212, 215, 218}) {
            bed(wingX(w, 2), y, z, out, Blocks.GRAY_BED);
        }
        for (int z : new int[] {210, 213, 216}) {
            chest(wingX(w, 3), y, z, in, LootFiller.Kind.CAMP);
        }
        k.set(wingX(w, 12), y, 209, s(Blocks.GRINDSTONE));
        k.set(wingX(w, 14), y, 209, s(Blocks.FLETCHING_TABLE));
        k.set(wingX(w, 16), y, 209, s(Blocks.ANVIL));
        chest(wingX(w, 19), y, 209, out, LootFiller.Kind.ARMORY);
        chest(wingX(w, 19), y, 211, out, LootFiller.Kind.ARMORY);
        tableTop(wingX(w, 9), 213, wingX(w, 12), 214, y);
        chair(wingX(w, 8), y, 213, in);
        chair(wingX(w, 13), y, 214, out);
        candle(wingX(w, 10), y + 1, 213);
        carpet(wingX(w, 6), 210, wingX(w, 8), 217, y, Blocks.GRAY_CARPET);
        // R3: гостиная у лестничного проёма (проём z227-228 у наружной стены)
        carpet(wingX(w, 8), 221, wingX(w, 16), 225, y, Blocks.BLUE_CARPET);
        tableTop(wingX(w, 11), 222, wingX(w, 13), 223, y);
        chair(wingX(w, 10), y, 222, in);
        chair(wingX(w, 14), y, 223, out);
        for (int z = 221; z <= 224; z++) {
            k.set(wingX(w, 19), y, z, BuildKit.stairs(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
        }
        k.set(wingX(w, 17), y, 229, s(Blocks.JUKEBOX));
        pot(wingX(w, 19), y, 229);
        pot(wingX(w, 5), y, 221);
        // R4: кладовая (бочки, сундуки, мешки, прачечный чан)
        for (int z = 233; z <= 242; z++) {
            barrel(wingX(w, 0), y, z, LootFiller.Kind.KITCHEN);
            if (z % 2 == 0) {
                k.set(wingX(w, 0), y + 1, z, s(Blocks.BARREL));
            }
        }
        for (int z : new int[] {234, 238, 242}) {
            chest(wingX(w, 19), y, z, out, LootFiller.Kind.BEDROOM);
        }
        for (int z = 233; z <= 242; z += 3) {
            k.set(wingX(w, 8), y, z, s(Blocks.HAY_BLOCK));
        }
        k.set(wingX(w, 12), y, 237, s(Blocks.CAULDRON));
        k.set(wingX(w, 14), y, 237, s(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, 3));

        // ---------- восточное крыло ----------
        w = false;
        in = Direction.WEST;
        out = Direction.EAST;
        // R0: картографическая (столы с картами, знамёна, свитки в сундуках)
        k.set(wingX(w, 6), y, 185, s(Blocks.CARTOGRAPHY_TABLE));
        k.set(wingX(w, 8), y, 185, s(Blocks.LECTERN));
        k.set(wingX(w, 10), y, 185, s(Blocks.LOOM));
        tableTop(wingX(w, 11), 188, wingX(w, 15), 190, y);
        for (int kk = 11; kk <= 15; kk += 2) {
            candle(wingX(w, kk), y + 1, 189);
        }
        for (int z : new int[] {188, 190}) {
            chair(wingX(w, 10), y, z, in);
            chair(wingX(w, 16), y, z, out);
        }
        chest(wingX(w, 19), y, 185, out, LootFiller.Kind.LIBRARY);
        chest(wingX(w, 19), y, 192, out, LootFiller.Kind.LIBRARY);
        carpet(wingX(w, 7), 187, wingX(w, 17), 192, y, Blocks.GREEN_CARPET);
        k.set(wingX(w, 19), y, 188, s(Blocks.BLUE_WALL_BANNER).setValue(WallBannerBlock.FACING, out));
        // R1: зал совета (длинный стол и кресла), проход с балкона по z197-199 свободен
        tableTop(wingX(w, 5), 201, wingX(w, 15), 203, y);
        for (int kk = 5; kk <= 15; kk += 2) {
            chair(wingX(w, kk), y, 200, Direction.SOUTH);
            chair(wingX(w, kk), y, 204, Direction.NORTH);
            candle(wingX(w, kk), y + 1, 202);
        }
        chair(wingX(w, 4), y, 202, in);
        chair(wingX(w, 16), y, 202, out);
        carpet(wingX(w, 3), 199, wingX(w, 17), 205, y, Blocks.RED_CARPET);
        for (int z : new int[] {203, 205}) {
            k.fill(wingX(w, 0), y, z, wingX(w, 0), y + 2, z, s(Blocks.BOOKSHELF));
        }
        pot(wingX(w, 19), y, 205);
        // R2: лазарет (светлые койки, чаны с водой, стойка зелий, цветы)
        for (int z : new int[] {209, 212, 215}) {
            bed(wingX(w, 2), y, z, out, Blocks.WHITE_BED);
        }
        for (int z : new int[] {210, 213, 216}) {
            barrel(wingX(w, 3), y, z, LootFiller.Kind.WORKSHOP);
        }
        k.set(wingX(w, 12), y, 209, s(Blocks.BREWING_STAND));
        k.set(wingX(w, 14), y, 209, s(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, 3));
        k.set(wingX(w, 16), y, 209, s(Blocks.BREWING_STAND));
        chest(wingX(w, 19), y, 210, out, LootFiller.Kind.WORKSHOP);
        for (int z : new int[] {212, 216}) {
            pot(wingX(w, 8), y, z);
        }
        carpet(wingX(w, 6), 210, wingX(w, 10), 217, y, Blocks.WHITE_CARPET);
        // R3: музыкальная (проигрыватель и нотные блоки, диваны, кафедра)
        carpet(wingX(w, 8), 221, wingX(w, 16), 226, y, Blocks.PINK_CARPET);
        k.set(wingX(w, 17), y, 229, s(Blocks.JUKEBOX));
        for (int z : new int[] {222, 224, 226}) {
            k.set(wingX(w, 19), y, z, s(Blocks.NOTE_BLOCK));
        }
        for (int z = 221; z <= 225; z++) {
            k.set(wingX(w, 10), y, z, BuildKit.stairs(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
        }
        tableTop(wingX(w, 13), 222, wingX(w, 14), 224, y);
        candle(wingX(w, 13), y + 1, 223);
        k.set(wingX(w, 6), y, 229, s(Blocks.LECTERN));
        pot(wingX(w, 19), y, 220);
        // R4: прачечная и кладовая
        k.set(wingX(w, 0), y, 233, s(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, 3));
        k.set(wingX(w, 0), y, 235, s(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, 3));
        k.set(wingX(w, 0), y, 237, s(Blocks.CAULDRON));
        for (int z = 233; z <= 242; z += 3) {
            chest(wingX(w, 19), y, z, out, LootFiller.Kind.BEDROOM);
        }
        for (int z = 233; z <= 242; z += 2) {
            barrel(wingX(w, 14), y, z, LootFiller.Kind.KITCHEN);
        }
        k.set(wingX(w, 4), y, 238, s(Blocks.HAY_BLOCK));
        k.set(wingX(w, 4), y, 239, s(Blocks.HAY_BLOCK));
        k.set(wingX(w, 5), y, 238, s(Blocks.HAY_BLOCK));
        // общие светильники у дверей между комнатами
        for (int zp : new int[] {195, 207, 219, 231}) {
            for (int sideW = 0; sideW < 2; sideW++) {
                boolean ww = sideW == 0;
                k.set(wingX(ww, 8), y + 3, zp - 1, s(Blocks.LANTERN));
            }
        }
    }

    // ================= двор: сад, конюшня, скамьи =================
    private void grounds() {
        // огород на западе: грядки пшеницы, моркови и картофеля, канал воды, изгородь, пугало
        int gx1 = 139;
        int gx2 = 158;
        int gz1 = 274;
        int gz2 = 292;
        for (int x = gx1; x <= gx2; x++) {
            for (int z = gz1; z <= gz2; z++) {
                boolean edge = x == gx1 || x == gx2 || z == gz1 || z == gz2;
                if (edge) {
                    if (x == 148 && z == gz2) {
                        k.set(x, Y + 1, z, s(Blocks.OAK_FENCE_GATE).setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    } else {
                        k.set(x, Y + 1, z, s(Blocks.OAK_FENCE));
                    }
                    continue;
                }
                if (z == 277 || z == 283 || z == 289) {
                    k.set(x, Y, z, s(Blocks.WATER));
                    continue;
                }
                k.set(x, Y, z, s(Blocks.FARMLAND).setValue(FarmBlock.MOISTURE, 7));
                Block crop = (z / 3) % 3 == 0 ? Blocks.WHEAT : (z / 3) % 3 == 1 ? Blocks.CARROTS : Blocks.POTATOES;
                k.set(x, Y + 1, z, s(crop).setValue(BlockStateProperties.AGE_7, 4 + rnd.nextInt(4)));
            }
        }
        k.set(gx2 - 2, Y + 1, gz1 + 2, s(Blocks.OAK_FENCE));
        k.set(gx2 - 2, Y + 2, gz1 + 2, s(Blocks.HAY_BLOCK));
        k.set(gx2 - 2, Y + 3, gz1 + 2, s(Blocks.CARVED_PUMPKIN).setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        k.set(gx1 + 2, Y + 1, gz1 + 2, s(Blocks.COMPOSTER));
        k.set(gx1 + 2, Y + 1, gz2 - 2, s(Blocks.BARREL));
        // конюшня на востоке: загон с изгородью и навес с сеном и поилкой
        int sx1 = 272;
        int sx2 = 292;
        int sz1 = 272;
        int sz2 = 294;
        for (int x = sx1; x <= sx2; x++) {
            for (int z = sz1; z <= sz2; z++) {
                boolean edge = x == sx1 || x == sx2 || z == sz1 || z == sz2;
                if (!edge) {
                    continue;
                }
                if (z == sz2 && x == 281) {
                    k.set(x, Y + 1, z, s(Blocks.SPRUCE_FENCE_GATE).setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                } else {
                    k.set(x, Y + 1, z, s(Blocks.SPRUCE_FENCE));
                }
            }
        }
        // навес у восточной стороны загона
        for (int x = 284; x <= 291; x++) {
            for (int z = 274; z <= 292; z++) {
                k.set(x, Y, z, s(Blocks.SPRUCE_PLANKS));
                k.set(x, Y + 4, z, s(Blocks.SPRUCE_SLAB));
            }
        }
        for (int[] c : new int[][] {{284, 274}, {284, 292}, {291, 274}, {291, 292}, {284, 283}, {291, 283}}) {
            k.fill(c[0], Y + 1, c[1], c[0], Y + 3, c[1], s(Blocks.STRIPPED_SPRUCE_LOG));
        }
        for (int z = 276; z <= 290; z += 2) {
            k.set(291, Y + 1, z, s(Blocks.HAY_BLOCK));
            if (z % 4 == 0) {
                k.set(291, Y + 2, z, s(Blocks.HAY_BLOCK));
            }
        }
        k.set(287, Y + 1, 275, s(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, 3));
        k.set(288, Y + 1, 275, s(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, 3));
        k.set(287, Y + 3, 283, BuildKit.HANGING_LANTERN);
        chest(286, Y + 1, 291, Direction.NORTH, LootFiller.Kind.CAMP);
        // скамьи и клумбы у фонтана
        for (int[] b : new int[][] {{CX - 10, 272, 1}, {CX + 10, 272, 3}}) {
            Direction face = b[2] == 1 ? Direction.EAST : Direction.WEST;
            for (int dz = -2; dz <= 2; dz++) {
                k.set(b[0], Y + 1, b[1] + dz, BuildKit.stairs(Blocks.SPRUCE_STAIRS, face.getOpposite(), false));
            }
            pot(b[0], Y + 1, b[1] - 3);
            pot(b[0], Y + 1, b[1] + 3);
        }
    }

    // ================= жители и животные =================
    private <T extends Mob> T spawn(EntityType<T> type, double x, double y, double z) {
        T e = type.create(level);
        if (e == null) {
            return null;
        }
        e.moveTo(x + 0.5D, y, z + 0.5D, rnd.nextFloat() * 360.0F, 0.0F);
        e.setPersistenceRequired();
        level.addFreshEntity(e);
        return e;
    }

    private void villager(VillagerProfession job, double x, double y, double z) {
        Villager v = spawn(EntityType.VILLAGER, x, y, z);
        if (v == null) {
            return;
        }
        v.setVillagerData(v.getVillagerData().setProfession(job).setLevel(2));
        v.getOffers().clear();   // жители обживают замок, но не торгуют
    }

    private void life() {
        // мастера и слуги рядом со своим рабочим местом
        villager(VillagerProfession.ARMORER, X0 + 14, Y + 1, 260);
        villager(VillagerProfession.TOOLSMITH, X0 + 16, Y + 1, 260);
        villager(VillagerProfession.WEAPONSMITH, X1 - 14, Y + 1, 260);
        villager(VillagerProfession.FLETCHER, X1 - 12, Y + 1, 260);
        villager(VillagerProfession.BUTCHER, X0 + 14, Y + 1, 331);
        villager(VillagerProfession.CLERIC, X1 - 14, Y + 1, 331);
        villager(VillagerProfession.LIBRARIAN, 170, Y + 1, 196);
        villager(VillagerProfession.LIBRARIAN, 175, Y + 1, 212);
        villager(VillagerProfession.CARTOGRAPHER, wingX(false, 13), CastleSite.F2 + 1, 186);
        villager(VillagerProfession.FARMER, 146, Y + 1, 279);
        villager(VillagerProfession.FARMER, 150, Y + 1, 288);
        villager(VillagerProfession.SHEPHERD, 280, Y + 1, 280);
        // животные: кошки в залах, куры в огороде, лошади и овцы в загоне
        spawn(EntityType.CAT, 176, Y + 1, 238);
        spawn(EntityType.CAT, 254, Y + 1, 238);
        spawn(EntityType.CAT, 215, CastleSite.F2 + 1, 195);
        for (int i = 0; i < 4; i++) {
            spawn(EntityType.CHICKEN, 143 + i * 4, Y + 1, 279 + (i % 2) * 8);
        }
        spawn(EntityType.HORSE, 279, Y + 1, 280);
        spawn(EntityType.HORSE, 281, Y + 1, 288);
        spawn(EntityType.SHEEP, 278, Y + 1, 276);
        spawn(EntityType.SHEEP, 276, Y + 1, 290);
        spawn(EntityType.SHEEP, 283, Y + 1, 279);
        // стража на стенах ходит по маршрутам, слуги гуляют по двору, двое стоят у ворот
        int x1 = X0 + CastleSite.W;
        ambient(VillagerProfession.WEAPONSMITH, "fa_patrol:wall_w", X0 + 2, Y + 14, 200, true, 0.0F);
        ambient(VillagerProfession.WEAPONSMITH, "fa_patrol:wall_w", X0 + 2, Y + 14, 300, true, 0.0F);
        ambient(VillagerProfession.WEAPONSMITH, "fa_patrol:wall_n", 180, Y + 14, Z0() + 2, true, 0.0F);
        ambient(VillagerProfession.WEAPONSMITH, "fa_patrol:wall_n", 250, Y + 14, Z0() + 2, true, 0.0F);
        ambient(VillagerProfession.WEAPONSMITH, "fa_patrol:wall_e", x1 - 1, Y + 14, 200, true, 0.0F);
        ambient(VillagerProfession.WEAPONSMITH, "fa_patrol:wall_e", x1 - 1, Y + 14, 290, true, 0.0F);
        ambient(VillagerProfession.SHEPHERD, "fa_patrol:plaza", CX + 17, Y + 1, 272, false, 0.0F);
        ambient(VillagerProfession.FARMER, "fa_patrol:plaza", CX - 17, Y + 1, 272, false, 0.0F);
        ambient(VillagerProfession.LIBRARIAN, "fa_patrol:plaza", CX, Y + 1, 255, false, 0.0F);
        ambient(VillagerProfession.CLERIC, "fa_patrol:road", X0 + 20, Y + 1, 298, false, 0.0F);
        ambient(VillagerProfession.FLETCHER, "fa_patrol:road", x1 - 20, Y + 1, 298, false, 0.0F);
        ambient(VillagerProfession.WEAPONSMITH, null, CX - 6, Y + 1, 339, true, 0.0F);
        ambient(VillagerProfession.WEAPONSMITH, null, CX + 5, Y + 1, 339, true, 0.0F);
    }

    private static int Z0() {
        return CastleSite.Z0;
    }

    /** Житель без собственного ИИ: стоит на посту или ходит по маршруту (тег), не получает урона и не торгует. */
    private void ambient(VillagerProfession job, String tag, double x, double y, double z, boolean armed, float yaw) {
        Villager v = spawn(EntityType.VILLAGER, x, y, z);
        if (v == null) {
            return;
        }
        v.setVillagerData(v.getVillagerData().setProfession(job).setLevel(3));
        v.getOffers().clear();
        v.setNoAi(true);
        v.setInvulnerable(true);
        v.setYRot(yaw);
        v.yBodyRot = yaw;
        v.setYHeadRot(yaw);
        if (tag != null) {
            v.addTag(tag);
        }
        if (armed) {
            v.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
    }

    // ================= колокольня, трубы, жаровни =================
    private void landmarks() {
        // колокольня на крыше западного крыла: два столба, балка и колокол под ней
        int bx = CastleLife.BELL.getX();
        int bz = CastleLife.BELL.getZ();
        for (int dx : new int[] {-2, 2}) {
            k.fill(bx + dx, Y + 24, bz, bx + dx, Y + 27, bz, s(Blocks.STONE_BRICK_WALL));
            k.set(bx + dx, Y + 28, bz, s(Blocks.STONE_BRICKS));
            k.set(bx + dx, Y + 29, bz, s(Blocks.LANTERN));
        }
        k.fill(bx - 1, Y + 28, bz, bx + 1, Y + 28, bz, s(Blocks.STONE_BRICKS));
        k.set(bx, Y + 27, bz, s(Blocks.BELL).setValue(BellBlock.FACING, Direction.NORTH)
                .setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING));
        // трубы над каминами первого этажа: дым поднимается с крыши
        for (int cx : new int[] {173, 257}) {
            k.fill(cx - 1, Y + 24, 242, cx + 1, Y + 28, 244, s(Blocks.STONE_BRICKS));
            k.set(cx, Y + 27, 243, s(Blocks.HAY_BLOCK));
            k.set(cx, Y + 28, 243, s(Blocks.CAMPFIRE).setValue(CampfireBlock.SIGNAL_FIRE, true));
        }
        // жаровни: основание и костёр (ночью зажигаются, утром гаснут: CastleLife)
        for (int[] b : CastleLife.BRAZIERS) {
            k.set(b[0], Y + 1, b[1], s(Blocks.STONE_BRICKS));
            k.set(b[0], Y + 2, b[1], s(Blocks.CAMPFIRE));
        }
    }
}
