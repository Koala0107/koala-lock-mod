package kr.koala.crouchlock;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CctvBlockEntity extends BlockEntity {
    private static final int MAX_RECORDS = 64;
    private final List<String> records = new ArrayList<>();
    private final Set<UUID> visiblePlayers = new HashSet<>();
    private int scanCooldown;

    public CctvBlockEntity(BlockPos pos, BlockState state) {
        super(SceneToolsMod.CCTV_BLOCK_ENTITY, pos, state);
    }

    public List<String> getRecords() {
        return List.copyOf(records);
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, CctvBlockEntity cctv) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (++cctv.scanCooldown < 20) return;
        cctv.scanCooldown = 0;

        Box area = new Box(pos).expand(6.0);
        List<PlayerEntity> nearby = serverWorld.getEntitiesByClass(PlayerEntity.class, area,
                player -> !player.isSpectator() && player.isAlive());

        Set<UUID> now = new HashSet<>();
        for (PlayerEntity player : nearby) {
            now.add(player.getUuid());
            if (!cctv.visiblePlayers.contains(player.getUuid())) {
                cctv.addRecord(formatGameTime(serverWorld.getTimeOfDay()) + " — " + player.getName().getString() + "이(가) CCTV 감지 범위에 들어왔습니다.");
            }
        }
        cctv.visiblePlayers.clear();
        cctv.visiblePlayers.addAll(now);
    }

    private static String formatGameTime(long worldTime) {
        long day = Math.floorDiv(worldTime, 24000L) + 1L;
        long ticks = Math.floorMod(worldTime, 24000L);
        int totalMinutes = (int)((ticks + 6000L) % 24000L * 60L / 1000L);
        int hour = totalMinutes / 60;
        int minute = totalMinutes % 60;
        return day + "일차 " + String.format("%02d:%02d", hour, minute);
    }

    private void addRecord(String line) {
        records.add(line);
        while (records.size() > MAX_RECORDS) records.remove(0);
        markDirty();
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.getChunkManager().markForUpdate(pos);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        NbtList list = new NbtList();
        for (String record : records) list.add(NbtString.of(record));
        nbt.put("Records", list);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        records.clear();
        if (nbt.contains("Records", NbtElement.LIST_TYPE)) {
            NbtList list = nbt.getList("Records", NbtElement.STRING_TYPE);
            for (int i = 0; i < list.size(); i++) records.add(list.getString(i));
        }
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    @Override
    public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}
