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

package top.qiguaiaaaa.geocraft.api.block;

import git.jbredwards.fluidlogged_api.api.block.IFluidloggable;
import git.jbredwards.fluidlogged_api.api.util.FluidState;
import git.jbredwards.fluidlogged_api.api.util.FluidloggedUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.Fluid;
import top.qiguaiaaaa.geocraft.api.util.QBUtil;
import top.qiguaiaaaa.geocraft.api.util.LayeredFluidHostUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 还没写好
 * @author QiguaiAAAA
 */
public interface IFluidloggableLayeredFluidHost extends IFluidloggable, ILayeredFluidHost {

    @Override
    default boolean isAcceptedFluid(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Fluid fluid){
        return isFluidloggable(state,world,pos) && isFluidValid(state,world,pos,fluid);
    }

    @Override
    default int getLayers(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nullable Fluid fluid){
        FluidState fluidState = FluidState.get(world,pos);
        if(fluidState.isEmpty()) return 0;
        return fluidState.getQuantaValue();
    }

    @Override
    default int getMaxLayers(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nullable Fluid fluid) {
        FluidState fluidState = FluidState.get(world,pos);
        if(fluidState.isEmpty()) return 8;
        return fluidState.getQuantaPerBlock();
    }

    @Override
    default int getEmptyHeight(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nullable Fluid fluid){
        return 0;
    }

    @Override
    default int getHeightPerLayer(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state){
        FluidState fluidState = FluidState.get(world,pos);
        if(fluidState.isEmpty()) return LayeredFluidHostUtil.EIGHTH_HEIGHT;
        return LayeredFluidHostUtil.DEFAULT_MAX_HEIGHT/fluidState.getQuantaPerBlock();
    }

    @Override
    default long getAmountInQBPerLayer(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Fluid fluid){
        FluidState fluidState = FluidState.get(world,pos);
        if(fluidState.isEmpty()) return QBUtil.QUANTA_VOLUME;
        return QBUtil.QUANTA_VOLUME/fluidState.getQuantaPerBlock();
    }

    @Override
    default void addLayer(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Fluid fluid, int layer, @Nullable NBTTagCompound nbt, int disabledBlockFlags,int enabledBlockFlags) {
        FluidState fluidState = FluidState.get(world,pos);
        if(layer == 0) return;
        if(fluidState.isEmpty()){
            if(!isAcceptedFluid(world,pos,state,fluid)) return;
            fluidState = FluidState.of(fluid);
            int flag = Constants.BlockFlags.DEFAULT & ~disabledBlockFlags;
            FluidloggedUtils.setFluidState(world,pos,state,fluidState.withLevel(-layer),false,flag);
        }else if(fluid == fluidState.getFluid()){
            int quanta = fluidState.getQuantaValue();
            layer = MathHelper.clamp(layer,-quanta,fluidState.getQuantaPerBlock()-quanta);
            fluidState.addLevel(-layer);
        }
    }

    @Override
    default boolean canFill(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Fluid fluid, @Nonnull EnumFacing side, @Nullable IBlockState source) {
        if(!isFluidloggable(state,world,pos)) return false;
        if(!isFluidValid(state,world, pos, fluid)) return false;
        if(isFull(world, pos, state, fluid)) return false;
        return canFluidFlow(world,pos,state,side);
    }

    @Override
    default boolean canDrain(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Fluid fluid, @Nonnull EnumFacing side, @Nullable IBlockState source) {
        FluidState fluidState = FluidState.get(world,pos);
        return !fluidState.isEmpty() && fluidState.getFluid() == fluid && fluidState.getQuantaValue()>0;
    }

    @Override
    default boolean isFull(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nullable Fluid fluid) {
        return fluid != null && !isAcceptedFluid(world, pos, state, fluid) || FluidState.get(world,pos).isSource();
    }

    @Override
    default boolean isEmpty(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nullable Fluid fluid) {
        return FluidState.get(world,pos).isEmpty();
    }
}
