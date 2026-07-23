/*
 * Copyright 2026 QGMoe
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
 * 版权所有 2026 QGMoe
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

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import moe.qingu.geocraft.api.util.annotation.MultiThread;
import moe.qingu.geocraft.api.util.annotation.ThreadOnly;
import moe.qingu.geocraft.api.util.annotation.ThreadType;
import moe.qingu.geocraft.api.world.tick.IScheduledTick;
import moe.qingu.geocraft.api.world.tick.TickPriority;
import moe.qingu.geocraft.handler.CapabilityHandler;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.Constants;
import moe.qingu.geocraft.GeoCraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.SoftReference;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @since 0.2.0
 * @author QiguaiAAAA
 */
public final class ScheduledTicksData implements ICapabilitySerializable<NBTTagCompound> {
    public static final ResourceLocation ID = new ResourceLocation(GeoCraft.MODID,"scheduled_ticks_data");
    private final @Nonnull Chunk chunk;
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private volatile SoftReference<NBTTagCompound> save = new SoftReference<>(new NBTTagCompound());
    final ReentrantLock lock = new ReentrantLock();
    final PriorityQueue<IScheduledTick> queue = new PriorityQueue<>();
    final ObjectOpenHashSet<IScheduledTick> set = new ObjectOpenHashSet<>();

    public ScheduledTicksData(@Nonnull final Chunk chunk) {
        this.chunk = chunk;
    }

    @Nonnull
    public Chunk getChunk() {
        return chunk;
    }

    @ThreadOnly(ThreadType.MINECRAFT_SERVER)
    public void schedule(final @Nonnull IScheduledTick tick){
        lock.lock();
        try {
            this.queue.add(tick);
        }finally {
            lock.unlock();
        }
    }

    @ThreadOnly(ThreadType.MINECRAFT_SERVER)
    public boolean isScheduled(final @Nonnull IScheduledTick tick){
        return set.contains(tick);
    }

    @Nonnull
    @ThreadOnly(ThreadType.MINECRAFT_SERVER)
    public Set<IScheduledTick> query(final @Nonnull BlockPos pos) {
        final ObjectOpenHashSet<IScheduledTick> ticks = new ObjectOpenHashSet<>();
        for(final IScheduledTick tick:queue) if(tick.pos().equals(pos)) ticks.add(tick);
        return ticks;
    }

    /* -------------------------------
         Serialisation Area
       ------------------------------- */

    @Override
    @Nonnull
    @MultiThread({ThreadType.MINECRAFT_SERVER,ThreadType.CHUNK_IO_THREADS,ThreadType.GEO_MISC_DAEMON})
    public NBTTagCompound serializeNBT() {
        final @Nullable NBTTagCompound cache;
        if(isDirty() || (cache = this.save.get()) == null){
            lock.lock();
            try {
                final NBTTagCompound compound = new NBTTagCompound();
                final NBTTagList updateLists = new NBTTagList();
                final long totalTime = chunk.getWorld().getTotalWorldTime();
                compound.setLong("totalWorldTime",totalTime);
                for(@Nonnull IScheduledTick tick:queue){
                    final @Nonnull NBTTagCompound c = new NBTTagCompound();
                    c.setInteger("id",Block.REGISTRY.getIDForObject(tick.block()));
                    c.setIntArray("pos",new int[]{tick.pos().getX(),tick.pos().getY(),tick.pos().getZ()});
                    c.setInteger("time",(int)(tick.triggeredTick()-totalTime));
                    c.setByte("pri",(byte) tick.priority().ordinal());
                    updateLists.appendTag(c);
                }
                compound.setTag("block_updating_entries",updateLists);
                compound.setInteger("version",1);
                this.save = new SoftReference<>(compound);
                return compound;
            }finally {
                this.clearDirty();
                lock.unlock();
            }
        }
        return cache;
    }

    @Override
    public void deserializeNBT(final @Nonnull NBTTagCompound nbt) {
        final int v = nbt.getInteger("version");
        if(v > 1) throw new IllegalArgumentException();
        final NBTTagList updateLists = nbt.getTagList("block_updating_entries", Constants.NBT.TAG_COMPOUND);
        final long savedTime = nbt.getLong("totalWorldTime");
        for(@Nonnull NBTBase base:updateLists){
            final @Nonnull NBTTagCompound compound = (NBTTagCompound) base;
            final @Nonnull Block block = Block.REGISTRY.getObjectById(compound.getInteger("id"));
            final int[] posArray = compound.getIntArray("pos");
            final @Nonnull BlockPos pos = new BlockPos(posArray[0],posArray[1],posArray[2]);
            final int time = compound.getInteger("time");
            final int pri = MathHelper.clamp(compound.getByte("pri"),0,15);
            queue.add(IScheduledTick.of(block,pos,savedTime+time, TickPriority.of(pri)));
        }
    }

    /* -------------------------------
               Capability Area
       ------------------------------- */

    @Override
    public boolean hasCapability(@Nonnull final Capability<?> capability, @Nullable final EnumFacing facing) {
        return capability == CapabilityHandler.SCHEDULED_TICKS_DATA;
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull final Capability<T> capability, @Nullable final EnumFacing facing) {
        if(hasCapability(capability,facing)){
            return CapabilityHandler.SCHEDULED_TICKS_DATA.cast(this);
        }else return null;
    }

    /* -------------------------------
              Getter And Setter
       ------------------------------- */

    public boolean isDirty() {
        return dirty.get();
    }

    @Nonnull
    public ReentrantLock getLock() {
        return lock;
    }

    @ThreadOnly(ThreadType.MINECRAFT_SERVER)
    public boolean markDirty(){
        this.chunk.markDirty();
        return dirty.compareAndSet(false,true);
    }

    public void clearDirty(){
        dirty.set(false);
    }

}
