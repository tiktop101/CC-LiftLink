package com.tom.createelevatorcc;

import com.simibubi.create.content.contraptions.elevator.ElevatorColumn;
import com.simibubi.create.content.contraptions.elevator.ElevatorContactBlockEntity;
import com.simibubi.create.content.contraptions.elevator.ElevatorPulleyBlockEntity;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import dan200.computercraft.api.peripheral.PeripheralType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElevatorMethods implements GenericPeripheral {
    @Override
    public String id() {
        return "ccliftlink:elevator";
    }

    @Override
    public PeripheralType getType() {
        return PeripheralType.ofType("create_elevator");
    }

    @LuaFunction(mainThread = true)
    public final String getRole(ElevatorContactBlockEntity contact) {
        return "contact";
    }

    @LuaFunction(mainThread = true)
    public final List<Map<String, Object>> listFloors(ElevatorContactBlockEntity contact) throws LuaException {
        return listFloors0(contact.getLevel(), contact);
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getState(ElevatorContactBlockEntity contact) throws LuaException {
        return getState0(contact.getLevel(), contact);
    }

    @LuaFunction(mainThread = true)
    public final Double getCabY(ElevatorContactBlockEntity contact) throws LuaException {
        ElevatorColumn column = requireColumn(contact.getLevel(), contact);
        ElevatorPulleyBlockEntity pulley = ElevatorHelpers.resolvePulley(contact.getLevel(), contact.getBlockPos());
        Double liveCabY = ElevatorHelpers.getCurrentCabY(contact.getLevel(), pulley);
        if (liveCabY != null) return liveCabY;
        ElevatorContactBlockEntity current = ElevatorHelpers.findCurrentFloorContact(contact.getLevel(), column);
        return current != null ? (double) current.getBlockPos().getY() : null;
    }

    @LuaFunction(mainThread = true)
    public final double getSpeed(ElevatorContactBlockEntity contact) {
        ElevatorPulleyBlockEntity pulley = ElevatorHelpers.resolvePulley(contact.getLevel(), contact.getBlockPos());
        return pulley == null ? 0.0 : pulley.getMovementSpeed();
    }

    @LuaFunction(mainThread = true)
    public final Double getOffset(ElevatorContactBlockEntity contact) {
        ElevatorPulleyBlockEntity pulley = ElevatorHelpers.resolvePulley(contact.getLevel(), contact.getBlockPos());
        return ElevatorHelpers.getCurrentOffset(pulley);
    }

    @LuaFunction(mainThread = true)
    public final Integer getTargetY(ElevatorContactBlockEntity contact) throws LuaException {
        return getTargetY0(contact.getLevel(), contact);
    }

    @LuaFunction(mainThread = true)
    public final boolean isMoving(ElevatorContactBlockEntity contact) throws LuaException {
        ElevatorPulleyBlockEntity pulley = ElevatorHelpers.resolvePulley(contact.getLevel(), contact.getBlockPos());
        if (pulley != null) return Math.abs(pulley.getMovementSpeed()) > 0.0001f;
        ElevatorColumn column = requireColumn(contact.getLevel(), contact);
        Integer targetY = column.isTargetAvailable() ? column.getTargetedYLevel() : null;
        ElevatorContactBlockEntity current = ElevatorHelpers.findCurrentFloorContact(contact.getLevel(), column);
        return targetY != null && current != null && targetY.intValue() != current.getBlockPos().getY();
    }

    @LuaFunction(mainThread = true)
    public final boolean callToY(ElevatorContactBlockEntity contact, int y) throws LuaException {
        return callToY0(contact.getLevel(), contact, y);
    }

    @LuaFunction(mainThread = true)
    public final boolean callToFloor(ElevatorContactBlockEntity contact, String floorName) throws LuaException {
        return callToFloor0(contact.getLevel(), contact, floorName);
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getNearestFloor(ElevatorContactBlockEntity contact) throws LuaException {
        return getNearestFloor0(contact.getLevel(), contact);
    }

    private static ElevatorColumn requireColumn(Level level, net.minecraft.world.level.block.entity.BlockEntity be) throws LuaException {
        ElevatorColumn column = ElevatorHelpers.resolveColumn(level, be.getBlockPos());
        if (column == null) throw new LuaException("No elevator column found for this contact");
        return column;
    }

    private static List<Map<String, Object>> listFloors0(Level level, net.minecraft.world.level.block.entity.BlockEntity be) throws LuaException {
        ElevatorColumn column = requireColumn(level, be);
        List<Map<String, Object>> out = new ArrayList<>();
        Integer targetY = column.isTargetAvailable() ? column.getTargetedYLevel() : null;
        ElevatorContactBlockEntity current = ElevatorHelpers.findCurrentFloorContact(level, column);
        for (ElevatorContactBlockEntity contact : ElevatorHelpers.getSortedContacts(level, column)) {
            Map<String, Object> row = new HashMap<>();
            row.put("y", contact.getBlockPos().getY());
            row.put("shortName", contact.shortName);
            row.put("longName", contact.longName);
            row.put("name", ElevatorHelpers.bestFloorName(contact));
            row.put("currentFloorName", ElevatorHelpers.nonBlank(contact.lastReportedCurrentFloor));
            row.put("isTarget", targetY != null && targetY == contact.getBlockPos().getY());
            row.put("isCurrent", current != null && current.getBlockPos().equals(contact.getBlockPos()));
            out.add(row);
        }
        return out;
    }

    private static Map<String, Object> getState0(Level level, net.minecraft.world.level.block.entity.BlockEntity be) throws LuaException {
        ElevatorColumn column = requireColumn(level, be);
        ElevatorPulleyBlockEntity pulley = ElevatorHelpers.resolvePulley(level, be.getBlockPos());
        ElevatorContactBlockEntity currentFloor = ElevatorHelpers.findCurrentFloorContact(level, column);
        Double speed = pulley != null ? (double) pulley.getMovementSpeed() : 0.0;
        Double offset = ElevatorHelpers.getCurrentOffset(pulley);
        Double cabY = ElevatorHelpers.getCurrentCabY(level, pulley);
        Integer currentTargetY = ElevatorHelpers.getCurrentTargetY(level, pulley);
        Integer columnTargetY = column.isTargetAvailable() ? column.getTargetedYLevel() : null;
        Integer targetY = currentTargetY != null ? currentTargetY : columnTargetY;
        if (cabY == null && currentFloor != null) cabY = (double) currentFloor.getBlockPos().getY();

        Map<String, Object> out = new HashMap<>();
        out.put("modName", "CC:LiftLink");
        out.put("role", "contact");
        out.put("active", column.isActive());
        out.put("targetAvailable", column.isTargetAvailable());
        out.put("targetY", targetY);
        out.put("currentTargetY", currentTargetY);
        out.put("speed", speed);
        out.put("offset", offset);
        out.put("cabY", cabY);
        out.put("moving", pulley != null ? Math.abs(speed) > 0.0001 : (targetY != null && currentFloor != null && targetY.intValue() != currentFloor.getBlockPos().getY()));
        out.put("floorCount", ElevatorHelpers.getSortedContacts(level, column).size());
        out.put("blockX", be.getBlockPos().getX());
        out.put("blockY", be.getBlockPos().getY());
        out.put("blockZ", be.getBlockPos().getZ());
        out.put("hasPulley", pulley != null);
        out.put("trackingMode", pulley != null ? "live_pulley" : "single_contact");
        out.put("currentFloorName", currentFloor != null ? ElevatorHelpers.bestFloorName(currentFloor) : null);
        out.put("currentFloorY", currentFloor != null ? currentFloor.getBlockPos().getY() : null);
        return out;
    }

    private static Integer getTargetY0(Level level, net.minecraft.world.level.block.entity.BlockEntity be) throws LuaException {
        ElevatorColumn column = requireColumn(level, be);
        ElevatorPulleyBlockEntity pulley = ElevatorHelpers.resolvePulley(level, be.getBlockPos());
        Integer live = ElevatorHelpers.getCurrentTargetY(level, pulley);
        return live != null ? live : (column.isTargetAvailable() ? column.getTargetedYLevel() : null);
    }

    private static boolean callToY0(Level level, net.minecraft.world.level.block.entity.BlockEntity be, int y) throws LuaException {
        ElevatorColumn column = requireColumn(level, be);
        ElevatorContactBlockEntity target = ElevatorHelpers.findContactByY(level, column, y);
        if (target != null) return ElevatorHelpers.callTo(level, column, target);
        if (ElevatorHelpers.callToArbitraryY(level, column, be.getBlockPos(), y)) return true;
        throw new LuaException("Y=" + y + " is not a real floor and is outside the elevator travel range");
    }

    private static boolean callToFloor0(Level level, net.minecraft.world.level.block.entity.BlockEntity be, String floorName) throws LuaException {
        ElevatorColumn column = requireColumn(level, be);
        ElevatorContactBlockEntity target = ElevatorHelpers.findContactByName(level, column, floorName);
        if (target == null) throw new LuaException("No floor named '" + floorName + "'");
        return ElevatorHelpers.callTo(level, column, target);
    }

    private static Map<String, Object> getNearestFloor0(Level level, net.minecraft.world.level.block.entity.BlockEntity be) throws LuaException {
        ElevatorColumn column = requireColumn(level, be);
        ElevatorPulleyBlockEntity pulley = ElevatorHelpers.resolvePulley(level, be.getBlockPos());
        Double cabY = ElevatorHelpers.getCurrentCabY(level, pulley);
        ElevatorContactBlockEntity best = ElevatorHelpers.findBestContactForCabY(level, column, cabY);
        if (best == null) best = ElevatorHelpers.findCurrentFloorContact(level, column);
        if (best == null) return null;
        Map<String, Object> out = new HashMap<>();
        out.put("y", best.getBlockPos().getY());
        out.put("shortName", best.shortName);
        out.put("longName", best.longName);
        out.put("name", ElevatorHelpers.bestFloorName(best));
        out.put("distance", cabY == null ? 0.0 : Math.abs(best.getBlockPos().getY() - cabY));
        return out;
    }
}
