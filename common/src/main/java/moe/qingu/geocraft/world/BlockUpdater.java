/*
 * Copyright 2025 QiguaiAAAA
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 版权所有 2025 QiguaiAAAA
 * 根据Apache许可证第2.0版（“本许可证”）许可；
 * 除非符合本许可证的规定，否则你不得使用此文件。
 * 你可以在此获取本许可证的副本：
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * 除非所适用法律要求或经书面同意，在本许可证下分发的软件是“按原样”分发的，
 * 没有任何形式的担保或条件，不论明示或默示。
 * 请查阅本许可证了解有关本许可证下许可和限制的具体要求。
 * 中文译文来自开放原子开源基金会，非官方译文，如有疑议请以英文原文为准
 */

package moe.qingu.geocraft.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import moe.qingu.geocraft.api.world.tick.IScheduledTick;
import moe.qingu.geocraft.api.world.tick.TickPriority;
import moe.qingu.geocraft.api.world.tick.scheduler.BlockTickScheduler;
import moe.qingu.geocraft.configs.GeneralConfig;
import moe.qingu.geocraft.handler.CapabilityHandler;
import moe.qingu.geocraft.util.math.MathUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.init.Blocks;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import moe.qingu.geocraft.api.util.annotation.MultiThread;
import moe.qingu.geocraft.api.util.annotation.ThreadOnly;
import moe.qingu.geocraft.api.util.annotation.ThreadType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @since 0.1
 * @version 0.2.0-alpha.3
 * @author QiguaiAAAA
 */
@ThreadSafe
@MultiThread({ThreadType.CHUNK_IO_THREADS,ThreadType.MINECRAFT_SERVER})
public class BlockUpdater extends BlockTickScheduler {
    private final int maxUpdateNum;
    private final Long2ObjectOpenHashMap<ScheduledTicksData> data = new Long2ObjectOpenHashMap<>();
    private final ConcurrentLinkedQueue<ScheduledTicksData> dirties = new ConcurrentLinkedQueue<>();
    private final LongOpenHashSet schedules = new LongOpenHashSet();
    private long[] temp = new long[0];

    public BlockUpdater(final @Nonnull World world){
        super(world);
        this.maxUpdateNum = GeneralConfig.BLOCK_UPDATER_MAX_UPDATES_BLOCK.getValue();
    }

    @Override
    @ThreadOnly(ThreadType.MINECRAFT_SERVER)
    public boolean schedule(@Nonnull final BlockPos pos,
                            @Nonnull final Block block,
                            final int delay,
                            @Nonnull final TickPriority priority) {
        final int chunkX = pos.getX()>>4;
        final int chunkZ = pos.getZ()>>4;
        final ScheduledTicksData datum = getDatum(chunkX,chunkZ);
        if(datum == null) return false;
        final IScheduledTick tick = IScheduledTick.of(block,pos,this.world.getTotalWorldTime()+delay,priority);
        if(datum.isScheduled(tick)) return false;
        datum.schedule(tick);
        schedules.add(ChunkPos.asLong(chunkX,chunkZ));
        if(!datum.isDirty() && datum.markDirty()) dirties.add(datum);
        return true;
    }

    @Nonnull
    @Override
    @ThreadOnly(ThreadType.MINECRAFT_SERVER)
    public Set<IScheduledTick> query(@Nonnull final BlockPos pos) {
        final int chunkX = pos.getX()>>4;
        final int chunkZ = pos.getZ()>>4;
        final ScheduledTicksData datum = getDatum(chunkX,chunkZ);
        if(datum == null) return Collections.emptySet();
        return datum.query(pos);
    }

    @Nonnull
    @Override
    @ThreadOnly(ThreadType.MINECRAFT_SERVER)
    public Set<IScheduledTick> query(final int x,final int y,final int z,final int dx,final int dy,final int dz) {
        final int tx = x + dx;
        final int ty = y + dy;
        final int tz = z + dz;
        final int minX = Math.min(x,tx);
        final int minY = Math.min(y,ty);
        final int minZ = Math.min(z,tz);
        final int maxX = Math.max(x,tx);
        final int maxY = Math.max(y,ty);
        final int maxZ = Math.max(z,tz);
        final int minChunkX = minX>>4;
        final int minChunkZ = minZ>>4;
        final int maxChunkX = maxX>>4;
        final int maxChunkZ = maxZ>>4;
        final ObjectOpenHashSet<IScheduledTick> collector = new ObjectOpenHashSet<>();
        for(int chunkX = minChunkX;chunkX<=maxChunkX;chunkX++){
            for(int chunkZ = minChunkZ;chunkZ<=maxChunkZ;chunkZ++){
                if(MathUtil.inRangeOpen(chunkX,minChunkX,maxChunkX) && MathUtil.inRangeOpen(chunkZ,minChunkZ,maxChunkZ)) collect(chunkX,chunkZ,minY,maxY,collector);
                else {
                    final ScheduledTicksData datum = getDatum(chunkX,chunkZ);
                    if(datum == null) continue;
                    for(final IScheduledTick tick:datum.queue)
                        if(MathUtil.inRangeClose(tick.pos().getX(),x,tx) && MathUtil.inRangeClose(tick.pos().getZ(),z,tz) && MathUtil.inRangeClose(tick.pos().getY(),y,ty))
                            collector.add(tick);
                }
            }
        }
        return collector;
    }

    @ThreadOnly(ThreadType.MINECRAFT_SERVER)
    public void collect(final int chunkX,final int chunkZ,final int minY,final int maxY,final @Nonnull Set<IScheduledTick> collector){
        final ScheduledTicksData datum = getDatum(chunkX,chunkZ);
        if(datum == null) return;
        for(final IScheduledTick tick:datum.queue) if(MathUtil.inRangeClose(tick.pos().getY(),minY,maxY)) collector.add(tick);
    }

    @Override
    @ThreadOnly(ThreadType.MINECRAFT_SERVER)
    public void update() {
        final long beginTime = System.currentTimeMillis(),maxTime = GeneralConfig.BLOCK_UPDATER_MAX_TIME_USAGE.getValue();
        final long totalWorldTime = world.getTotalWorldTime();
        final IScheduledTick[] tempArr = new IScheduledTick[100];
        temp = schedules.toLongArray(temp);
        final int size = schedules.size();
        long count = 0;
        int i = 0;
        while (count < maxUpdateNum && i < size){
            final long pos = temp[i++];
            final ScheduledTicksData datum = data.get(pos);
            if(datum == null) {
                schedules.remove(pos);
                continue;
            }
            int cot = 0;
            datum.lock.lock();
            final Chunk chunk = datum.getChunk();
            final ExtendedBlockStorage[] ebs= chunk.getBlockStorageArray();
            try {
                int n = 0;
                do {
                    while (!datum.queue.isEmpty() && n < tempArr.length && datum.queue.peek().triggeredTick() <= totalWorldTime) tempArr[n++] = datum.queue.poll();
                    cot += n;
                    count += n;
                    int j = n;
                    while (j>0){
                        final IScheduledTick tick = tempArr[j--];
                        final BlockPos position = tick.pos();
                        final int y = position.getY();
                        final @Nonnull IBlockState state;
                        if(y<0 || y > 255) state = chunk.getBlockState(position);
                        else {
                            final ExtendedBlockStorage storage = ebs[tick.pos().getY()>>4];
                            if(storage == Chunk.NULL_BLOCK_STORAGE) state = Blocks.AIR.getDefaultState();
                            else state = storage.get(position.getX() & 0xF,y &0xF,position.getZ() &0xF);
                        }
                        final Block block = state.getBlock();
                        if(isInvalidTickEntry(tick,state)) continue;
                        try {
                            block.updateTick(world,position,state,world.rand);
                        } catch (final Throwable t) {
                            final @Nonnull CrashReport report = CrashReport.makeCrashReport(t, "Exception while ??? ticking a block"); //todo
                            final @Nonnull CrashReportCategory category = report.makeCategory("Block being ticked");
                            CrashReportCategory.addBlockInfo(category, position.toImmutable(), state);
                            throw new ReportedException(report);
                        }
                    }
                } while (n>0 && count < maxUpdateNum);
            }finally {
                datum.lock.unlock();
            }
            if(cot != 0 && datum.markDirty()) dirties.add(datum);
            if(datum.queue.isEmpty()) schedules.remove(pos);
            if(System.currentTimeMillis() - beginTime > maxTime) break;
        }
    }

    /**
     * 当前的 NTE 是否需要丢弃，否则就会被更新
     * @param tick NTE 计划
     * @param curState 目前该位置的真正方块状态
     * @return 如果要被丢弃，返回 true
     */
    @ThreadOnly(ThreadType.MINECRAFT_SERVER)
    protected boolean isInvalidTickEntry(@Nonnull final IScheduledTick tick,
                                         @Nonnull final IBlockState curState){
        return curState.getBlock() != tick.block();
    }

    @Nullable
    public ScheduledTicksData getDatum(final int cx, final int cz){
        ScheduledTicksData res = data.get(ChunkPos.asLong(cx,cz));
        if(res != null) return res;
        final Chunk chunk = world.getChunk(cx,cz);
        if(chunk.hasCapability(CapabilityHandler.SCHEDULED_TICKS_DATA,null)){
            data.put(ChunkPos.asLong(cx,cz),res = chunk.getCapability(CapabilityHandler.SCHEDULED_TICKS_DATA,null));
            return res;
        }else return null;
    }

    @Nonnull
    public Long2ObjectOpenHashMap<ScheduledTicksData> getData() {
        return data;
    }

    @Nonnull
    public LongOpenHashSet getSchedules() {
        return schedules;
    }

    @Nonnull
    public ConcurrentLinkedQueue<ScheduledTicksData> getDirties() {
        return dirties;
    }

    @Deprecated
    @ThreadOnly(ThreadType.MINECRAFT_SERVER)
    public static void scheduleUpdate(@Nonnull final World world,@Nonnull final BlockPos pos,@Nonnull final Block block,final int delay){
        schedule(world,pos,block,delay);
    }
}
