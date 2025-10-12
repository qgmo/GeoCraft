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

import git.jbredwards.fluidlogged_api.api.util.FluidState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.Fluid;
import org.apache.commons.lang3.tuple.Pair;
import top.qiguaiaaaa.geocraft.api.util.APIUtil;
import top.qiguaiaaaa.geocraft.api.util.FluidUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

/**
 * 可透水(或其他流体)方块<br/>
 * 注意这和含水方块有本质区别，含水方块是透水方块的子集。例如，泥土不是含水方块，但应该为透水方块。<br/>
 * 当前透水方块系统可能会迎来大改,以兼容{@link git.jbredwards.fluidlogged_api.mod.FluidloggedAPI}
 * @author QiguaiAAAA
 */
public interface IPermeableBlock {
    Set<Fluid> ANY_FLUIDS = Collections.emptySet();

    /**
     * 默认最大的水位高度,控制着其他方块的同种液体是否能够流入此方块<br/>
     * 此值为1~16所有整数的最小公倍数
     */
    int DEFAULT_MAX_HEIGHT = 720720;

    int EIGHTH_OF_HEIGHT = DEFAULT_MAX_HEIGHT/8;

    /**
     * 返回当前状态透水方块所可以接受的流体集合。若可以含有任意种流体，则返回空集{@link Collections#emptySet()}或{@link #ANY_FLUIDS}。
     * @param state 需要查询的方块状态
     * @return 流体集合
     */
    @Nonnull
    Set<Fluid> getAcceptedFluids(@Nonnull IBlockState state);

    /**
     * 查询指定方块状态的指定流体的层数。请勿在含水方块上使用该方法。
     * @param state 方块状态
     * @param fluid 流体,若为null则表示查询当前方块所有流体的合层数
     * @return 对应流体的层数
     */
    int getQuanta(@Nonnull IBlockState state,@Nullable Fluid fluid);

    /**
     * 查询指定方块状态在指定流体状态下指定流体的层数。
     * @param state 方块状态
     * @param fluid 流体,若为null则表示查询当前方块所有流体的合层数
     * @param fluidState 流体状态，若当前方块为含水方块则必须提供。必须是{@link git.jbredwards.fluidlogged_api.api.util.FluidState}及其子类。
     * @return 对应流体的层数
     */
    default int getQuanta(@Nonnull IBlockState state,@Nullable Fluid fluid,@Nonnull Pair<Fluid,IBlockState> fluidState){
        if(APIUtil.ModChecker.FLUIDLOGGED_API){
            return ((FluidState)fluidState).getQuantaValue();
        }
        return getQuanta(state,fluid);
    }

    /**
     * 查询指定方块状态指定流体能够存储的最大层数。请勿在含水方块上使用该方法。若有FluidState应使用{@link #getMaxQuanta(IBlockState, Fluid, Pair)}
     * @param state 方块状态
     * @param fluid 流体,若为null则表示查询当前方块所有流体的最大合层数
     * @return 最大层数
     */
    default int getMaxQuanta(@Nonnull IBlockState state,@Nullable Fluid fluid){
        return (DEFAULT_MAX_HEIGHT -getEmptyHeight(state,fluid))/getHeightPerQuanta(state);
    }

    /**
     * 查询指定方块状态和流体状态下指定流体能够存储的最大层数。
     * @param state 方块状态
     * @param fluid 流体,若为null则表示查询当前方块所有流体的最大合层数
     * @return 最大层数
     */
    default int getMaxQuanta(@Nonnull IBlockState state,@Nullable Fluid fluid,@Nonnull Pair<Fluid,IBlockState> fluidState){
        if(APIUtil.ModChecker.FLUIDLOGGED_API){
            return ((FluidState)fluidState).getQuantaPerBlock();
        }
        return getMaxQuanta(state,fluid);
    }

    /**
     * 返回水位高度。请勿在含水方块上使用该方法。
     * @param state 方块状态
     * @return 水位高度，数字越大越高
     */
    default int getHeight(@Nonnull IBlockState state,@Nullable Fluid fluid){
        return getEmptyHeight(state,fluid)+getQuanta(state,fluid)*getHeightPerQuanta(state);
    }

    /**
     * 返回含水方块水位高度。
     * @param state 方块状态
     * @return 水位高度，数字越大越高
     */
    default int getHeight(@Nonnull IBlockState state,@Nullable Fluid fluid,@Nonnull Pair<Fluid,IBlockState> fluidState){
        if(APIUtil.ModChecker.FLUIDLOGGED_API){
            return (int) (((FluidState)fluidState).getHeight()*DEFAULT_MAX_HEIGHT);
        }
        return getHeight(state,fluid);
    }

    /**
     * 返回若没有指定液体的情况下的基准高度。含水方块应固定为0
     * @param state 方块状态
     * @param fluid 流体,若为null则意思是没有含有任何流体时的基准高度
     * @return 基准高度,单位和水位高度一样
     */
    int getEmptyHeight(@Nonnull IBlockState state,@Nullable Fluid fluid);

    /**
     * 返回指定方块状态下指定流体能够有的最高水位高度
     * @param state 方块状态
     * @param fluid 流体,若为null则意思是含有任意流体时所能达到的最高高度
     * @return 最高水位高度
     */
    default int getMaxHeight(@Nonnull IBlockState state,@Nullable Fluid fluid){
        return getEmptyHeight(state,fluid) + getMaxQuanta(state,fluid)*getHeightPerQuanta(state);
    }

    /**
     * 对于提供的方块状态来说,每层液体的高度，必须是{@link #DEFAULT_MAX_HEIGHT}的因数
     * @param state 方块状态
     * @return 每层的高度
     */
    int getHeightPerQuanta(@Nonnull IBlockState state);

    /**
     * 添加或移除指定层(quanta)指定的流体<br/>
     * 在添加前一般需要检测{@link #canFill(World, BlockPos, IBlockState, Fluid, EnumFacing, IBlockState)}
     * @param world 世界
     * @param pos 位置
     * @param state 状态，必须是当前方块的状态
     * @param fluid 需要添加或移除的流体
     * @param quanta 层数,若为负值则为提取(remove or drain).建议使用{@link #drainQuanta(World, BlockPos, IBlockState, Fluid, int, boolean)}
     */
    default void addQuanta(@Nonnull World world,@Nonnull BlockPos pos,@Nonnull IBlockState state,@Nonnull Fluid fluid,int quanta){
        int newQuanta = MathHelper.clamp(getQuanta(state,fluid)+quanta,0,getMaxQuanta(state,fluid));
        setQuanta(world, pos, state,fluid, newQuanta);
    }

    /**
     * 添加指定层液体，其中每层的含量为{@link FluidUtil#ONE_IN_EIGHT_OF_BUCKET_VOLUME}<br/>
     * 在添加前一般需要检测{@link #canFill(World, BlockPos, IBlockState, Fluid, EnumFacing, IBlockState)}
     * @param world 世界
     * @param pos 位置
     * @param state 状态
     * @param fluid 要添加的流体
     * @param quanta 层数
     * @param doAdd 是否真的添加
     * @return 实际添加的层数
     */
    default int addQuanta(@Nonnull World world,@Nonnull BlockPos pos,@Nonnull IBlockState state,@Nonnull Fluid fluid,int quanta,boolean doAdd){
        if(quanta == 0) return 0;
        int curQuanta = getQuanta(state,fluid);
        int quantaInFact = MathHelper.clamp(quanta,-curQuanta,getMaxQuanta(state,fluid)-curQuanta);
        if(quantaInFact == 0) return 0;
        if(doAdd) addQuanta(world, pos, state, fluid,quantaInFact);
        return quantaInFact;
    }

    /**
     * 添加指定层液体，并返回添加后的方块状态，其中每层的含量为{@link FluidUtil#ONE_IN_EIGHT_OF_BUCKET_VOLUME}<br/>
     * 在添加前建议检测{@link #canFill(World, BlockPos, IBlockState, Fluid, EnumFacing, IBlockState)}
     * @param state 状态
     * @param fluid 要添加的流体
     * @param quanta 层数
     * @return 实际添加的层数
     */
    @Nullable
    default IBlockState addQuanta(@Nonnull IBlockState state,@Nonnull Fluid fluid,int quanta){
        if(quanta == 0) return state;
        int curQuanta = getQuanta(state,fluid);
        int quantaInFact = MathHelper.clamp(quanta,-curQuanta,getMaxQuanta(state,fluid)-curQuanta);
        if(quantaInFact == 0) return state;
        return getQuantaState(state,fluid,quanta+quantaInFact);
    }

    /**
     * 吸取指定层液体，其中每片的含量为{@link FluidUtil#ONE_IN_EIGHT_OF_BUCKET_VOLUME}<br/>
     * 在吸取前一般需要先检查{@link #canDrain(World, BlockPos, IBlockState, Fluid, EnumFacing, IBlockState)}
     * @param world 世界
     * @param pos 位置
     * @param state 状态
     * @param fluid 要吸取的流体
     * @param quanta 层数
     * @param doDrain 是否真的吸取
     * @return 实际吸取的层数
     */
    default int drainQuanta(@Nonnull World world,@Nonnull BlockPos pos,@Nonnull IBlockState state,@Nonnull Fluid fluid,int quanta,boolean doDrain){
        if(quanta == 0) return 0;
        return -addQuanta(world, pos, state, fluid, -quanta, doDrain);
    }

    /**
     * 将当前方块的指定流体流体层数设置到指定层数<br/>
     * 为更灵活的拓展性，现在已弃用
     * @param world 世界
     * @param pos 当前位置
     * @param state 方框状态
     * @param fluid 指定流体
     * @param newQuanta 新的层数
     */
    @Deprecated
    default void setQuanta(@Nonnull World world,@Nonnull BlockPos pos,@Nonnull IBlockState state,@Nonnull Fluid fluid,int newQuanta){
        if(!getAcceptedFluids(state).isEmpty() && !getAcceptedFluids(state).contains(fluid)) return;
        newQuanta = MathHelper.clamp(newQuanta,0,getMaxQuanta(state,fluid));
        IBlockState quantaState = getQuantaState(state,fluid,newQuanta);
        if(quantaState == null) return;
        world.setBlockState(pos,quantaState, Constants.BlockFlags.SEND_TO_CLIENTS);
    }

    /**
     * 获取指定流体下指定层数时的方块状态
     * @param state 查询的方块状态
     * @param fluid 指定流体
     * @param quanta 指定层数
     * @return 若指定状态不存在,返回null
     */
    @Nullable
    IBlockState getQuantaState(@Nonnull IBlockState state,@Nonnull Fluid fluid, int quanta);

    /**
     * 指定流体以指定条件是否能够流入当前方块
     * @param world 世界
     * @param pos 位置
     * @param state 方块状态
     * @param fluid 需要流入的流体
     * @param side 流入的面
     * @param source 来源。若来源为{@link Blocks#AIR}则表示为大气。
     * @return 若可以，则返回true
     */
    default boolean canFill(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Fluid fluid, @Nonnull EnumFacing side, @Nullable IBlockState source){
        return !isFull(state, fluid);
    }

    /**
     * 以指定条件是否能够吸取当前方块的流体
     * @param world 世界
     * @param pos 位置
     * @param state 方块状态
     * @param fluid 需要吸取的流体
     * @param side 吸取的面
     * @param source 来源。
     * @return 若可以，返回true。
     */
    default boolean canDrain(@Nonnull World world,@Nonnull BlockPos pos,@Nonnull IBlockState state,@Nonnull Fluid fluid,@Nonnull EnumFacing side,@Nullable IBlockState source){
        return getQuanta(state, fluid) != 0;
    }

    /**
     * 指定方块状态的指定流体含量是否已满
     * @param state 方块状态
     * @param fluid 流体
     * @return 若当前方块不支持含有该流体或当前流体已满，则返回true，否则为false
     */
    default boolean isFull(@Nonnull IBlockState state,@Nullable Fluid fluid){
        return getQuanta(state,fluid) == getMaxQuanta(state,fluid);
    }

    /**
     * 指定状态的指定液体是否是空的
     * @param state 状态
     * @param fluid 液体
     * @return 若液体的 {@link #getMaxQuanta(IBlockState, Fluid)}为0或{@link #getQuanta(IBlockState, Fluid)}不为0则为false，否则为true
     */
    default boolean isEmpty(@Nonnull IBlockState state,@Nullable Fluid fluid){
        if(isFull(state,fluid)) return false;
        return getQuanta(state,fluid) == 0;
    }
}
