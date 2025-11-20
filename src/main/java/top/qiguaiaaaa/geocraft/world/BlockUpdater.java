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

package top.qiguaiaaaa.geocraft.world;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import top.qiguaiaaaa.geocraft.GeoCraft;
import top.qiguaiaaaa.geocraft.capability.SchedulingTicksCapability;
import top.qiguaiaaaa.geocraft.configs.GeneralConfig;
import top.qiguaiaaaa.geocraft.util.math.MathUtil;
import top.qiguaiaaaa.geocraft.util.misc.ExtendedNextTickListEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static top.qiguaiaaaa.geocraft.configs.GeneralConfig.BLOCK_UPDATER_MAX_UPDATES_BLOCK;
import static top.qiguaiaaaa.geocraft.configs.GeneralConfig.ENABLE_BLOCK_UPDATER;

/**
 * @since 0.1
 * @version 0.2.0-alpha.2
 * @author QiguaiAAAA
 */
public final class BlockUpdater {
    public static final ResourceLocation ID = new ResourceLocation(GeoCraft.MODID,"block_updater");
    private static final Function<World,BlockUpdater> putBlockUpdateToCache = w -> w.hasCapability(SchedulingTicksCapability.BLOCK_UPDATER,null)?
            w.getCapability(SchedulingTicksCapability.BLOCK_UPDATER,null):null;
    private static final Comparator<ExtendedNextTickListEntry> compareByDistanceToPlayer =
            Comparator.comparingDouble(ExtendedNextTickListEntry::getDisSqToNearestPlayer);
    static final int MAX_UPDATE_NUM = BLOCK_UPDATER_MAX_UPDATES_BLOCK.getValue();
    static final Map<World,BlockUpdater> UPDATERS_CACHE = new HashMap<>();

    final Set<ExtendedNextTickListEntry> schedules = new LinkedHashSet<>();
    final LinkedList<ExtendedNextTickListEntry> readyTicks = new LinkedList<>();

    World world;
    Consumer<ExtendedNextTickListEntry> calcDistanceToClosestPlayer;

    public void setWorld(@Nonnull World world) {
        this.world = world;
        calcDistanceToClosestPlayer = entry -> entry.calcDisSqToNearestPlayer(world);
    }

    public World getWorld() {
        return world;
    }

    @Nonnull
    public Set<ExtendedNextTickListEntry> getPendingTicks() {
        return schedules;
    }

    /**
     * @since 0.2.0
     */
    public void schedule(@Nonnull BlockPos pos,@Nonnull Block block,int delay){
        ExtendedNextTickListEntry entry = new ExtendedNextTickListEntry(world,pos,block,delay,0);
        schedules.add(entry);
    }

    /**
     * @since 0.2.0
     */
    public void scheduleAll(@Nonnull Collection<ExtendedNextTickListEntry> entries){
        schedules.addAll(entries);
    }

    /**
     * @since 0.2.0
     */
    public void updateTick(){
        final long beginTime = System.currentTimeMillis();
        Iterator<ExtendedNextTickListEntry> iterator = schedules.iterator();
        while (iterator.hasNext()) {
            ExtendedNextTickListEntry entry = iterator.next();
            if(world.getTotalWorldTime()<entry.scheduledTime){
                continue;
            }
            iterator.remove();
            readyTicks.add(entry);
            if(readyTicks.size()>MAX_UPDATE_NUM) break;
        }

        if(GeneralConfig.SORT_UPDATE_TASKS_BY_DISTANCE_TO_PLAYERS.getValue()){
            readyTicks.forEach(calcDistanceToClosestPlayer);
            readyTicks.sort(compareByDistanceToPlayer);
        }

        final int maxTimeUsage = GeneralConfig.BLOCK_UPDATER_MAX_TIME_USAGE.getValue();
        int i = 0;
        while (!readyTicks.isEmpty()){
            final ExtendedNextTickListEntry entry = readyTicks.poll();
            final IBlockState state = world.getBlockState(entry.position);
            if(state.getBlock() != entry.getBlock()) continue;
            state.getBlock().updateTick(world,entry.position,state,world.rand);
            i++;
            if((i&127) == 0){
                if(maxTimeUsage <0) continue;
                if(System.currentTimeMillis()-beginTime>maxTimeUsage) break;
            }
        }
        schedules.addAll(readyTicks);
        readyTicks.clear();
    }

    /**
     * @since 0.2.0
     */
    @Nonnull
    public Set<ExtendedNextTickListEntry> queryEntries(@Nonnull final BlockPos pos,final boolean doRemove){
        Set<ExtendedNextTickListEntry> set = null;
        for(byte i =(byte) 0;i<2;i++){
            final Iterator<ExtendedNextTickListEntry> iterator;
            if (i == 0) {
                iterator = schedules.iterator();
            } else {
                iterator = readyTicks.iterator();
            }
            while (iterator.hasNext()){
                final ExtendedNextTickListEntry entry = iterator.next();
                if(entry.position.equals(pos)){
                    if(doRemove) iterator.remove();
                    if(set == null) set = new HashSet<>();
                    set.add(entry);
                }
            }
        }
        return set == null?Collections.emptySet():set;
    }

    /**
     * @since 0.2.0
     */
    @Nonnull
    public Set<ExtendedNextTickListEntry> queryEntries(@Nonnull final Chunk chunk,final boolean doRemove){
        return queryEntries(chunk.x<<4,0,chunk.z<<4,16,256,16,doRemove);
    }

    /**
     * @since 0.2.0
     */
    @Nonnull
    public Set<ExtendedNextTickListEntry> queryEntries(final int x,final int y,final int z,final int dx,final int dy,final int dz,final boolean doRemove){
        final int toX = x + dx,toY = y+dy,toZ=z+dz;
        Set<ExtendedNextTickListEntry> set = null;
        for(byte i =(byte) 0;i<2;i++){
            final Iterator<ExtendedNextTickListEntry> iterator;
            if (i == 0) {
                iterator = schedules.iterator();
            } else {
                iterator = readyTicks.iterator();
            }
            while (iterator.hasNext()){
                final ExtendedNextTickListEntry entry = iterator.next();
                if(MathUtil.inRange(entry.position.getX(),x,toX) && MathUtil.inRange(entry.position.getY(),y,toY) && MathUtil.inRange(entry.position.getZ(),z,toZ)){
                    if(doRemove) iterator.remove();
                    if(set == null) set = new HashSet<>();
                    set.add(entry);
                }
            }
        }
        return set == null?Collections.emptySet():set;
    }

    @Nullable
    public static BlockUpdater getBlockUpdater(@Nonnull World world){
        return UPDATERS_CACHE.computeIfAbsent(world,putBlockUpdateToCache);
    }

    public static void scheduleUpdate(@Nonnull World world,@Nonnull BlockPos pos,@Nonnull Block block, int delay){
        if(!ENABLE_BLOCK_UPDATER.getValue()){
            world.scheduleUpdate(pos,block,delay);
            return;
        }
        final BlockUpdater updater = getBlockUpdater(world);
        if(updater == null) return;
        updater.schedule(pos,block,delay);
    }

    /**
     * @since 0.2.0
     */
    public static void scheduleUpdates(@Nonnull World world,@Nonnull Set<ExtendedNextTickListEntry> entries){
        if(!ENABLE_BLOCK_UPDATER.getValue()){
            for(ExtendedNextTickListEntry entry:entries){
                world.scheduleBlockUpdate(entry.position,entry.getBlock(),(int) (entry.scheduledTime-world.getTotalWorldTime()),entry.priority);
            }
            return;
        }
        final BlockUpdater updater = getBlockUpdater(world);
        if(updater == null) return;
        updater.scheduleAll(entries);
    }

    @Nonnull
    public static Set<ExtendedNextTickListEntry> getEntries(@Nonnull final World world){
        final BlockUpdater updater = getBlockUpdater(world);
        if(updater == null) return Collections.emptySet();
        return updater.getPendingTicks();
    }

    public static void onWorldTick(@Nonnull WorldServer world){
        if(!ENABLE_BLOCK_UPDATER.getValue()) return;
        final BlockUpdater updater = UPDATERS_CACHE.computeIfAbsent(world,putBlockUpdateToCache);
        if(updater == null) return;
        updater.updateTick();
    }

    public static void onServerStop(){
        UPDATERS_CACHE.clear();
    }
}
