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

package top.qiguaiaaaa.geocraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import top.qiguaiaaaa.geocraft.api.atmosphere.Atmosphere;
import top.qiguaiaaaa.geocraft.api.atmosphere.AtmosphereSystemManager;
import top.qiguaiaaaa.geocraft.api.atmosphere.accessor.IAtmosphereAccessor;
import top.qiguaiaaaa.geocraft.api.block.IBlockStateLayeredFluidHost;
import top.qiguaiaaaa.geocraft.api.block.ILayeredFluidHost;
import top.qiguaiaaaa.geocraft.api.configs.value.geo.FluidPhysicsMode;
import top.qiguaiaaaa.geocraft.api.fluid.StateOfMatter;
import top.qiguaiaaaa.geocraft.api.util.*;
import top.qiguaiaaaa.geocraft.api.util.math.FlowChoice;
import top.qiguaiaaaa.geocraft.geography.fluid_physics.vanilla.BlockLiquidUpdater;
import top.qiguaiaaaa.geocraft.geography.soil.BlockSoilType;
import top.qiguaiaaaa.geocraft.util.BaseUtil;
import top.qiguaiaaaa.geocraft.util.ChunkUtil;
import top.qiguaiaaaa.geocraft.util.WaterUtil;
import top.qiguaiaaaa.geocraft.util.fluid.FluidOperationUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static top.qiguaiaaaa.geocraft.api.block.BlockProperties.HUMIDITY;
import static top.qiguaiaaaa.geocraft.configs.FluidPhysicsConfig.FLUID_PHYSICS_MODE;

public interface IBlockSoil extends IBlockStateLayeredFluidHost {
    ThreadLocal<List<FlowChoice>> averageModeFlowChoices = ThreadLocal.withInitial(ArrayList::new);

    /**
     * 土壤将自身水掉下去的能力
     * @return 湿度变化
     */
    default int dropWaterDown(World worldIn, BlockPos pos){
        BlockPos down = pos.down();
        IBlockState downState = worldIn.getBlockState(down);
        if(downState.getMaterial() == Material.AIR){
            if(FLUID_PHYSICS_MODE.getValue() == FluidPhysicsMode.MORE_REALITY)
                worldIn.setBlockState(down, Blocks.FLOWING_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL,7), Constants.BlockFlags.DEFAULT);
            return -1;
        }else if(FluidUtil.getFluid(downState) == FluidRegistry.WATER){
            if(FLUID_PHYSICS_MODE.getValue() != FluidPhysicsMode.MORE_REALITY) return -1;
            int meta = downState.getValue(BlockLiquid.LEVEL);
            if(meta >0 && meta<=7){
                worldIn.setBlockState(down,downState.withProperty(BlockLiquid.LEVEL,meta-1),Constants.BlockFlags.DEFAULT);
                return -1;
            }
        }else if(!BlockLiquidUpdater.isBlocked(downState)){
            if(FLUID_PHYSICS_MODE.getValue() == FluidPhysicsMode.MORE_REALITY) {
                FluidOperationUtil.triggerDestroyBlockEffectByFluid(worldIn,down,downState,FluidRegistry.WATER);
                worldIn.setBlockState(down, Blocks.FLOWING_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL,7), Constants.BlockFlags.DEFAULT);
            }
            return -1;
        }else if(downState.getBlock() == Blocks.CAULDRON){
            if(FLUID_PHYSICS_MODE.getValue() == FluidPhysicsMode.VANILLA){
                if(!BaseUtil.getRandomResult(worldIn.rand,0.3)) return 0;
                if(downState.getValue(BlockCauldron.LEVEL) <3){
                    worldIn.setBlockState(pos, downState.cycleProperty(BlockCauldron.LEVEL), Constants.BlockFlags.SEND_TO_CLIENTS);
                    return -1;
                }
            }
        }
        return 0;
    }

    /**
     * 土壤水向四周流动的能力
     * @param humidity 当前湿度
     */
    default void flowWaterHorizontally(World worldIn,BlockPos pos,IBlockState state,int humidity){
        if (!worldIn.isAreaLoaded(pos, 1)) return;
        //可流动方向检查
        final List<FlowChoice> averageModeFlowDirections = averageModeFlowChoices.get();
        averageModeFlowDirections.clear();

        for(EnumFacing facing:EnumFacing.Plane.HORIZONTAL){
            BlockPos facingPos = pos.offset(facing);
            IBlockState facingState = worldIn.getBlockState(facingPos);
            if(!canFlowInto(worldIn,facingPos,facingState)) continue;
            if(facingState.getMaterial() == Material.AIR){
                averageModeFlowDirections.add(new FlowChoice(facing));
                continue;
            }
            ILayeredFluidHost host = (ILayeredFluidHost)facingState.getBlock();
            int facingHeight = host.getHeight(worldIn,facingPos,facingState,FluidRegistry.WATER);
            int facingHeightPerLayer = host.getHeightPerLayer(worldIn,facingPos,facingState);
            if(facingHeight+facingHeightPerLayer<=(humidity-1)*getHeightPerLayer(worldIn,pos,state)){
                averageModeFlowDirections.add(new FlowChoice(worldIn,facingPos,facingState,host,facing,FluidRegistry.WATER));
            }
        }

        final int newHumidity = LayeredFluidHostUtil.averageFlow(humidity,getHeightPerLayer(worldIn,pos,state), this.getAmountInQBPerLayer(worldIn,pos,state,FluidRegistry.WATER),
                getMaxStableHumidity(state),averageModeFlowDirections);

        if(newHumidity != humidity){
            setLayer(worldIn,pos,state,FluidRegistry.WATER,newHumidity);
            for(FlowChoice choice:averageModeFlowDirections){ //向四周流动
                if(choice.getAddedLayers() == 0) continue;
                BlockPos facingPos = pos.offset(choice.direction);
                if(choice.isAir()){
                    if(FLUID_PHYSICS_MODE.getValue() != FluidPhysicsMode.MORE_REALITY) continue;
                    worldIn.setBlockState(facingPos,Blocks.FLOWING_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL,8-choice.getNewLayers()));
                    continue;
                }
                IBlockState facingState = worldIn.getBlockState(facingPos);
                choice.apply(worldIn,facingPos,facingState,FluidRegistry.WATER);
            }
        }

        averageModeFlowDirections.clear();
    }

    /**
     * 土壤吸收上层水的能力
     * @return 湿度变化
     */
    default int drainUpWater(World worldIn, BlockPos pos,IBlockState state){
        BlockPos upPos = pos.up();
        IBlockState upState = worldIn.getBlockState(upPos);
        if(upState.getBlock() instanceof ILayeredFluidHost){
            ILayeredFluidHost block = (ILayeredFluidHost) upState.getBlock();
            if(!block.canDrain(worldIn,upPos,upState,FluidRegistry.WATER,EnumFacing.DOWN,state)) return 0;
            int drained = block.drainLayer(worldIn,upPos,upState,FluidRegistry.WATER,1,false);
            if(drained < 1) return 0;
            return block.drainLayer(worldIn,upPos,upState,FluidRegistry.WATER,1,true);
        }
        return 0;
    }

    default int onEvaporate(World world,BlockPos pos,IBlockState state,Random random){
        BlockPos up = pos.up();
        if(world.isAirBlock(up)) return 0;
        int humidity = getLayers(world,pos,state,FluidRegistry.WATER);
        if(humidity ==0) return 0;
        try(@Nullable IAtmosphereAccessor accessor = AtmosphereSystemManager.getAtmosphereAccessor(world,pos,true)) {
            if (accessor == null) return 0;
            int light = ChunkUtil.getNeighborsLightFor(world,EnumSkyBlock.SKY,pos);
            accessor.setSkyLight(light);

            if(!accessor.getAtmosphereInfo().canWaterEvaporate()) return 0;
            if(!accessor.canAccessAtmosphere()) return 0;

            double basePossibility = WaterUtil.getWaterEvaporatePossibility(accessor);
            basePossibility /= (8-humidity)*2;
            if(!BaseUtil.getRandomResult(random,basePossibility)) return 0;

            accessor.drainHeatFromUnderlying(AtmosphereUtil.Constants.WATER_EVAPORATE_LATENT_HEAT_PER_QUANTA);
            accessor.fillFluidToAtmosphere(FluidRegistry.WATER,FluidUtil.ONE_IN_EIGHT_OF_BUCKET_VOLUME, StateOfMatter.GAS,accessor.getTemperature(true),true);
            return -1;
        }

    }

    default void onRandomTick(World worldIn, BlockPos pos, IBlockState state, Random random){
        if(worldIn.isRemote) return;
        int humidity = getLayers(worldIn,pos,state,FluidRegistry.WATER);
        int newHumidity = humidity;
        int rnd = random.nextInt(3);
        if(rnd == 0){ //吸收上面的水
            if(humidity < 4) {
                newHumidity += drainUpWater(worldIn,pos,state);
            }
        }else if(rnd == 1){ //向下掉水
            if(humidity >getMaxStableHumidity(state)){
                newHumidity += dropWaterDown(worldIn, pos);
            }
        }else if(humidity>getMaxStableHumidity(state)) { //水平平衡
            flowWaterHorizontally(worldIn,pos,state,humidity);
            return;
        }
        if(humidity == newHumidity){
            if(humidity == 0) return;
            newHumidity += onEvaporate(worldIn, pos, state, random);
        }
        if(humidity == newHumidity) return;
        setLayer(worldIn,pos,state,FluidRegistry.WATER,newHumidity);
    }

    /**
     * 土壤在破坏时掉水的能力
     */
    default void dropWaterWhenBroken(World world, BlockPos pos, IBlockState state){
        int humidity = getLayers(world,pos,state,FluidRegistry.WATER);
        if(humidity == 0) return;
        if(FLUID_PHYSICS_MODE.getValue() != FluidPhysicsMode.MORE_REALITY){
            world.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                    pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5,
                    0, 0, 0, Block.getStateId(Blocks.WATER.getDefaultState()));
            return;
        }
        world.setBlockState(pos, Blocks.FLOWING_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL,8-humidity),Constants.BlockFlags.DEFAULT);
    }

    default boolean onPlayerUseBottle(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
        if(worldIn.isRemote) return false;
        ItemStack stack = playerIn.getHeldItem(hand);
        if(stack.isEmpty()) return false;
        int moisture = getLayers(worldIn,pos,state,FluidRegistry.WATER);
        Item item = stack.getItem();
        if(moisture >2) return false;
        if (item == Items.POTIONITEM && PotionUtils.getPotionFromItem(stack) == PotionTypes.WATER) {
            if (!playerIn.capabilities.isCreativeMode) {
                ItemStack bottleStack = new ItemStack(Items.GLASS_BOTTLE);
                playerIn.setHeldItem(hand, bottleStack);

                if (playerIn instanceof EntityPlayerMP) {
                    ((EntityPlayerMP)playerIn).sendContainerToPlayer(playerIn.inventoryContainer);
                }
            }

            worldIn.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
            this.addLayer(worldIn,pos,state,FluidRegistry.WATER,2);
            return true;
        }
        return false;
    }

    /**
     * 检查土壤水是否能够流进指定方块
     * @param state 目标方块状态
     * @return 能，则true，否，则反之
     */
    default boolean canFlowInto(@Nonnull World world,@Nonnull BlockPos pos,@Nonnull IBlockState state){
        if(state.getMaterial() == Material.AIR) return true;
        Block block = state.getBlock();
        if(block instanceof ILayeredFluidHost){
            ILayeredFluidHost permeableBlock = (ILayeredFluidHost) block;
            return permeableBlock.isAcceptedFluid(world,pos,state,FluidRegistry.WATER)
                    && !permeableBlock.isFull(world,pos,state,FluidRegistry.WATER);
        }
        return false;
    }

    @Nonnull
    BlockSoilType getType(@Nonnull IBlockState state);

    int getMaxStableHumidity(@Nonnull IBlockState state);

    double getFlowInPossibility(@Nonnull IBlockState state);

    double getRainInPossibility(@Nonnull IBlockState state);

    @Override
    default boolean isAcceptedFluid(@Nullable World world, @Nullable BlockPos pos, @Nonnull IBlockState state, @Nonnull Fluid fluid){
        return fluid == FluidRegistry.WATER;
    }

    @Override
    default int getLayers(@Nullable World world, @Nullable BlockPos pos, @Nonnull IBlockState state, @Nullable Fluid fluid){
        if(fluid != FluidRegistry.WATER && fluid != null) return 0;
        return state.getValue(HUMIDITY);
    }

    @Override
    default int getMaxLayers(@Nullable World world, @Nullable BlockPos pos, @Nonnull IBlockState state, @Nullable Fluid fluid) {
        if(fluid != FluidRegistry.WATER && fluid != null) return 0;
        return 4;
    }

    @Override
    default int getHeight(@Nullable World world, @Nullable BlockPos pos, @Nonnull IBlockState state, @Nullable Fluid fluid) {
        if(fluid != FluidRegistry.WATER && fluid != null) return 0;
        return state.getValue(HUMIDITY)* getHeightPerLayer(world,pos,state);
    }

    @Override
    default int getEmptyHeight(@Nullable World world, @Nullable BlockPos pos, @Nonnull IBlockState state, @Nullable Fluid fluid){
        return 0;
    }

    @Override
    default int getHeightPerLayer(@Nullable World world,@Nullable BlockPos pos,@Nonnull IBlockState state){
        return 180180;
    }

    @Override
    default long getAmountInQBPerLayer(@Nullable World world, @Nullable BlockPos pos, @Nonnull IBlockState state, @Nonnull Fluid fluid){
        return QBUtil.QUANTA_VOLUME;
    }

    @Override
    default void setLayer(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Fluid fluid, int newLayer,final int disabledBlockFlags,final int enabledBlockFlags){
        if(fluid != FluidRegistry.WATER) return;
        world.setBlockState(pos,state.withProperty(HUMIDITY, newLayer), APIMathUtil.getModifiedFlag(Constants.BlockFlags.SEND_TO_CLIENTS,disabledBlockFlags,enabledBlockFlags));
    }

    @Nullable
    @Override
    default IBlockState getLayerState(@Nonnull IBlockState state, @Nonnull Fluid fluid, int layer){
        if(fluid != FluidRegistry.WATER) return null;
        return state.withProperty(HUMIDITY, layer);
    }

    /**
     * 指定流体是否能够流入当前方块
     * @param world 世界
     * @param pos 位置
     * @param state 方块状态
     * @param fluid 需要流入的流体
     * @return 若可以，则返回true
     */
    @Override
    default boolean canFill(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Fluid fluid, @Nonnull EnumFacing side, @Nullable IBlockState source){
        if(fluid != FluidRegistry.WATER) return false;
        if(isFull(world,pos,state,fluid)) return false;
        if(source != null && source.getBlock() == Blocks.AIR){
            return BaseUtil.getRandomResult(world.rand,getRainInPossibility(state));
        }
        return BaseUtil.getRandomResult(world.rand, getFlowInPossibility(state));
    }

    @Override
    default boolean canDrain(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Fluid fluid, @Nonnull EnumFacing side, @Nullable IBlockState source) {
        if(fluid != FluidRegistry.WATER) return false;
        return getLayers(world,pos,state,fluid)>getMaxStableHumidity(state);
    }

    @Override
    default boolean isFull(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nullable Fluid fluid) {
        if(fluid != FluidRegistry.WATER && fluid != null) return true;
        return state.getValue(HUMIDITY) == 4;
    }
}
