package com.tom.createelevatorcc;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.elevator.ElevatorColumn;
import com.simibubi.create.content.contraptions.elevator.ElevatorColumn.ColumnCoords;
import com.simibubi.create.content.contraptions.elevator.ElevatorContactBlock;
import com.simibubi.create.content.contraptions.elevator.ElevatorContactBlockEntity;
import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
import com.simibubi.create.content.contraptions.elevator.ElevatorPulleyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ElevatorHelpers {
    private static final Field CONTROLLER_POS_FIELD;

    static {
        Field field = null;
        try {
            field = ControlledContraptionEntity.class.getDeclaredField("controllerPos");
            field.setAccessible(true);
        } catch (ReflectiveOperationException ignored) {
        }
        CONTROLLER_POS_FIELD = field;
    }

    private ElevatorHelpers() {
    }

    public static ElevatorColumn resolveColumn(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof ElevatorPulleyBlockEntity pulley) {
            ElevatorColumn fromContraption = resolveColumnFromPulley(level, pulley);
            if (fromContraption != null) return fromContraption;

            Map<ColumnCoords, ElevatorColumn> loaded = ElevatorColumn.LOADED_COLUMNS.get(level);
            if (loaded != null) {
                for (Map.Entry<ColumnCoords, ElevatorColumn> entry : loaded.entrySet()) {
                    ColumnCoords coords = entry.getKey();
                    if (coords.x() == pos.getX() && coords.z() == pos.getZ()) {
                        ElevatorColumn column = entry.getValue();
                        column.gatherAll();
                        if (!column.getContacts().isEmpty()) return column;
                    }
                }
            }
            return null;
        }

        ColumnCoords coords = null;
        if (be instanceof ElevatorContactBlockEntity contact && contact.columnCoords != null) coords = contact.columnCoords;
        if (coords == null) coords = ElevatorContactBlock.getColumnCoords(level, pos);
        if (coords == null) return null;

        ElevatorColumn column = ElevatorColumn.getOrCreate(level, coords);
        column.gatherAll();
        if (column.getContacts().isEmpty()) return null;

        if (be instanceof ElevatorContactBlockEntity contact) contact.columnCoords = coords;
        return column;
    }

    private static ElevatorColumn resolveColumnFromPulley(Level level, ElevatorPulleyBlockEntity pulley) {
        AbstractContraptionEntity attached = pulley.getAttachedContraption();
        if (!(attached instanceof ControlledContraptionEntity controlled)) return null;
        Contraption contraption = controlled.getContraption();
        if (!(contraption instanceof ElevatorContraption elevatorContraption)) return null;
        ColumnCoords coords = elevatorContraption.getGlobalColumn();
        if (coords == null) return null;
        ElevatorColumn column = ElevatorColumn.getOrCreate(level, coords);
        column.gatherAll();
        return column.getContacts().isEmpty() ? null : column;
    }

    private static ColumnCoords resolveCoords(Level level, BlockPos pos, ElevatorColumn column) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ElevatorContactBlockEntity contact && contact.columnCoords != null) return contact.columnCoords;
        ColumnCoords coords = ElevatorContactBlock.getColumnCoords(level, pos);
        if (coords != null) return coords;
        if (column != null && !column.getContacts().isEmpty()) {
            BlockPos first = column.getContacts().iterator().next();
            BlockEntity firstBe = level.getBlockEntity(first);
            if (firstBe instanceof ElevatorContactBlockEntity contact && contact.columnCoords != null) return contact.columnCoords;
            return ElevatorContactBlock.getColumnCoords(level, first);
        }
        return null;
    }

    private static BlockPos getControllerPos(ControlledContraptionEntity controlled) {
        if (CONTROLLER_POS_FIELD == null) return null;
        try {
            Object value = CONTROLLER_POS_FIELD.get(controlled);
            return value instanceof BlockPos pos ? pos : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static ElevatorPulleyBlockEntity resolvePulleyFromEntities(Level level, ColumnCoords coords) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        AABB box = new AABB(coords.x() - 4, minY, coords.z() - 4, coords.x() + 5, maxY, coords.z() + 5);
        for (ControlledContraptionEntity controlled : level.getEntitiesOfClass(ControlledContraptionEntity.class, box)) {
            Contraption contraption = controlled.getContraption();
            if (!(contraption instanceof ElevatorContraption elevatorContraption)) continue;
            ColumnCoords global = elevatorContraption.getGlobalColumn();
            if (global == null || global.x() != coords.x() || global.z() != coords.z()) continue;
            BlockPos controllerPos = getControllerPos(controlled);
            if (controllerPos == null) continue;
            BlockEntity be = level.getBlockEntity(controllerPos);
            if (be instanceof ElevatorPulleyBlockEntity pulley) return pulley;
        }
        return null;
    }

    private static ElevatorPulleyBlockEntity scanForPulley(Level level, int centerX, int centerZ, int radius) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                for (int y = maxY; y >= minY; y--) {
                    BlockEntity scanBe = level.getBlockEntity(new BlockPos(x, y, z));
                    if (scanBe instanceof ElevatorPulleyBlockEntity pulley) return pulley;
                }
            }
        }
        return null;
    }

    public static ElevatorPulleyBlockEntity resolvePulley(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ElevatorPulleyBlockEntity pulley) return pulley;
        ElevatorColumn column = resolveColumn(level, pos);
        if (column == null) return null;
        ColumnCoords coords = resolveCoords(level, pos, column);
        if (coords != null) {
            ElevatorPulleyBlockEntity pulley = resolvePulleyFromEntities(level, coords);
            if (pulley != null) return pulley;
            pulley = scanForPulley(level, coords.x(), coords.z(), 3);
            if (pulley != null) return pulley;
        }
        for (BlockPos contactPos : column.getContacts()) {
            ElevatorPulleyBlockEntity pulley = scanForPulley(level, contactPos.getX(), contactPos.getZ(), 3);
            if (pulley != null) return pulley;
        }
        return null;
    }

    public static List<ElevatorContactBlockEntity> getSortedContacts(Level level, ElevatorColumn column) {
        List<ElevatorContactBlockEntity> result = new ArrayList<>();
        for (BlockPos contactPos : column.getContacts()) {
            BlockEntity be = level.getBlockEntity(contactPos);
            if (be instanceof ElevatorContactBlockEntity contact) result.add(contact);
        }
        result.sort(Comparator.comparingInt(c -> c.getBlockPos().getY()));
        return result;
    }

    public static ElevatorContactBlockEntity findContactByY(Level level, ElevatorColumn column, int y) {
        for (ElevatorContactBlockEntity contact : getSortedContacts(level, column)) if (contact.getBlockPos().getY() == y) return contact;
        return null;
    }

    public static ElevatorContactBlockEntity findContactByName(Level level, ElevatorColumn column, String name) {
        String wanted = name.trim();
        for (ElevatorContactBlockEntity contact : getSortedContacts(level, column)) {
            if (wanted.equalsIgnoreCase(contact.shortName) || wanted.equalsIgnoreCase(contact.longName)) return contact;
        }
        return null;
    }

    public static ElevatorContactBlockEntity findCurrentFloorContact(Level level, ElevatorColumn column) {
        for (ElevatorContactBlockEntity contact : getSortedContacts(level, column)) {
            String currentName = nonBlank(contact.lastReportedCurrentFloor);
            if (currentName == null) continue;
            if (currentName.equalsIgnoreCase(nonBlank(contact.shortName)) || currentName.equalsIgnoreCase(nonBlank(contact.longName))) return contact;
        }
        return null;
    }

    public static ElevatorContactBlockEntity findBestContactForCabY(Level level, ElevatorColumn column, Double cabY) {
        if (cabY == null) return null;
        ElevatorContactBlockEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ElevatorContactBlockEntity contact : getSortedContacts(level, column)) {
            double distance = Math.abs(contact.getBlockPos().getY() - cabY);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = contact;
            }
        }
        return best;
    }

    public static boolean callTo(Level level, ElevatorColumn column, ElevatorContactBlockEntity target) {
        if (target == null) return false;
        BlockState state = target.getBlockState();
        if (!(state.getBlock() instanceof ElevatorContactBlock block)) return false;
        block.callToContactAndUpdate(column, state, level, target.getBlockPos(), false);
        return true;
    }

    public static Integer getCurrentTargetY(Level level, ElevatorPulleyBlockEntity pulley) {
        if (pulley == null) return null;
        AbstractContraptionEntity attached = pulley.getAttachedContraption();
        if (!(attached instanceof ControlledContraptionEntity controlled)) return null;
        if (!(controlled.getContraption() instanceof ElevatorContraption contraption)) return null;
        return contraption.getCurrentTargetY(level);
    }

    public static Double getCurrentOffset(ElevatorPulleyBlockEntity pulley) {
        return pulley == null ? null : (double) pulley.offset;
    }

    public static Double getCurrentCabY(Level level, ElevatorPulleyBlockEntity pulley) {
        if (pulley == null) return null;
        AbstractContraptionEntity attached = pulley.getAttachedContraption();
        if (!(attached instanceof ControlledContraptionEntity controlled)) return null;
        if (!(controlled.getContraption() instanceof ElevatorContraption contraption)) return null;
        double offset = pulley.offset;
        return pulley.getBlockPos().getY() + contraption.getContactYOffset() - 1.0 - offset;
    }

    public static String nonBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public static String bestFloorName(ElevatorContactBlockEntity contact) {
        return Objects.requireNonNullElse(nonBlank(contact.shortName), nonBlank(contact.longName));
    }
}
