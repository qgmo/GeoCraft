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

package top.qiguaiaaaa.geocraft.mixin.reality.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.qiguaiaaaa.geocraft.api.GeoFluids;
import top.qiguaiaaaa.geocraft.api.atmosphere.accessor.IAtmosphereAccessor;
import top.qiguaiaaaa.geocraft.api.block.IBlockStateLayeredFluidHost;
import top.qiguaiaaaa.geocraft.api.block.ILayeredFluidHost;
import top.qiguaiaaaa.geocraft.api.util.APIMathUtil;
import top.qiguaiaaaa.geocraft.api.util.AtmosphereUtil;
import top.qiguaiaaaa.geocraft.api.util.LayeredFluidHostUtil;
import top.qiguaiaaaa.geocraft.api.util.QBUtil;
import top.qiguaiaaaa.geocraft.block.IBlockSnow;
import top.qiguaiaaaa.geocraft.geography.fluid_physics.reality.RealitySnowUpdater;
import top.qiguaiaaaa.geocraft.util.fluid.FluidOperationUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

import static top.qiguaiaaaa.geocraft.api.block.BlockProperties.MIXTURE;

/**
 * @see top.qiguaiaaaa.geocraft.mixin.common.block.BlockSnowMixin
 * @author QiguaiAAAA
 */
@Mixin(value = BlockSnow.class)
public class BlockSnowMixin extends Block implements IBlockStateLayeredFluidHost, IBlockSnow {
    @Shadow @Final public static PropertyInteger LAYERS;

    public BlockSnowMixin(Material materialIn) {
        super(materialIn);
    }

    @Override
    @Unique
    public int tickRate(@Nonnull World worldIn) {
        return 5;
    }

    @Inject(method = "canPlaceBlockAt",at = @At("HEAD"),cancellable = true)
    public void canPlaceBlockAt(World worldIn, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        cir.cancel();
        IBlockState state = worldIn.getBlockState(pos.down());
        Block block = state.getBlock();

        if (block != Blocks.PACKED_ICE && block != Blocks.BARRIER) {
            BlockFaceShape blockfaceshape = state.getBlockFaceShape(worldIn, pos.down(), EnumFacing.UP);
            cir.setReturnValue(blockfaceshape == BlockFaceShape.SOLID || state.getBlock().isLeaves(state, worldIn, pos.down()) || block == this && state.getValue(BlockSnow.LAYERS) == 8);
        } else {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "checkAndDropBlock",at = @At("HEAD"),cancellable = true)
    private void checkAndDropBlock(World worldIn, BlockPos pos, IBlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!this.canPlaceBlockAt(worldIn, pos)) {
            worldIn.scheduleUpdate(pos,this,tickRate(worldIn));
            cir.setReturnValue(false);
        } else {
            cir.setReturnValue(true);
        }
    }

    /**
     * 注意该Mixin的优先级低于 {@link top.qiguaiaaaa.geocraft.mixin.common.block.BlockSnowMixin#updateTick(World, BlockPos, IBlockState, Random, CallbackInfo)}
     * 因此当前的 Mixin 会覆盖 Common 的 Mixin
     * @reason 复写自定义逻辑
     */
    @Inject(method = "updateTick",at =@At("HEAD"),cancellable = true)
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand, CallbackInfo ci) {
        ci.cancel();
        if(worldIn.isRemote) return;
        if(trySmelt(worldIn, pos, state, rand)) return;
        state = worldIn.getBlockState(pos);
        tryFallDown(worldIn, pos, state);
    }

    @Inject(method = "isReplaceable",at =@At("HEAD"),cancellable = true)
    public void isReplaceable(@Nonnull IBlockAccess worldIn, @Nonnull BlockPos pos,CallbackInfoReturnable<Boolean> cir){
        cir.cancel();
        cir.setReturnValue(false);
    }

    @Unique
    protected boolean tryFallDown(World world,BlockPos pos,IBlockState state){
        if(world.isRemote) return false;
        final BlockPos downPos = pos.down();
        IBlockState downState = world.getBlockState(downPos);
        if(RealitySnowUpdater.isBlocked(world,downPos,downState,state)){
            return false;
        }
        boolean isMixture = state.getValue(MIXTURE);
        if(downState.getBlock() == Blocks.SNOW_LAYER){ //雪和雪
            boolean isDownMixture = downState.getValue(MIXTURE);
            final int newLayers = state.getValue(BlockSnow.LAYERS) + downState.getValue(BlockSnow.LAYERS);
            if(isMixture == isDownMixture){ //直接合并
                if(newLayers<=8){
                    world.setBlockToAir(pos);
                    world.setBlockState(downPos,downState.withProperty(BlockSnow.LAYERS,newLayers));
                }else{
                    world.setBlockState(pos,state.withProperty(BlockSnow.LAYERS,newLayers-8));
                    world.setBlockState(downPos,downState.withProperty(BlockSnow.LAYERS,8));
                }
                world.scheduleUpdate(downPos,this,tickRate(world));
            }else{
                final int totalWater = isMixture? getLayers(world,pos,state,FluidRegistry.WATER): getLayers(world,downPos,downState,FluidRegistry.WATER);
                if(newLayers<=8){
                    world.setBlockToAir(pos);
                    world.setBlockState(downPos,downState
                            .withProperty(BlockSnow.LAYERS,newLayers)
                            .withProperty(MIXTURE,false));
                }else{
                    world.setBlockState(pos,state.withProperty(BlockSnow.LAYERS,newLayers-8)
                            .withProperty(MIXTURE,false));
                    world.setBlockState(downPos,downState.withProperty(BlockSnow.LAYERS,8)
                            .withProperty(MIXTURE,false));
                }
                try(@Nullable IAtmosphereAccessor accessor = AtmosphereUtil.getLightedAtmosphereAccessor(world,pos,true)){
                    if(accessor == null) return true;

                    accessor.putHeatToUnderlying(AtmosphereUtil.Constants.WATER_MELT_LATENT_HEAT_PER_QUANTA*totalWater/2d);
                }
            }
        }else if(downState.getBlock() instanceof ILayeredFluidHost){ //雪和其他方块
            ILayeredFluidHost host = (ILayeredFluidHost) downState.getBlock();
            final int curLayers = getLayers(world,pos,state,null);
            final boolean hasHalfQuanta = ((curLayers>>1)&1) !=0 && state.getValue(MIXTURE);

            final @Nullable IAtmosphereAccessor accessor = AtmosphereUtil.getLightedAtmosphereAccessor(world,pos,true);

            long curAmountWater = getAmountInQB(world,pos,state,FluidRegistry.WATER);
            boolean melted = false,doMelted = false;
            if(hasHalfQuanta) {
                curAmountWater += QBUtil.HALF_QUANTA_VOLUME; //将另外半层雪融化，补充成整数层layer
                melted = true;
            }
            boolean changed = false;
            if(curAmountWater>0 && host.canFill(world,downPos,downState,FluidRegistry.WATER,EnumFacing.UP,state)){
                final long filledAmount = host.addAmountInQB(world,downPos,downState,FluidRegistry.WATER,curAmountWater,true);
                curAmountWater -=filledAmount; //减去已经填充的量
                changed = filledAmount>0;
                //现在，若剩余的量大于等于刚才融化以补充成整数层的量，说明没有用到融化的部分，因此不需要融化一部分雪以补充水。但若小于，则直接当成融化，并扣除相应热量。
                if(curAmountWater<QBUtil.HALF_QUANTA_VOLUME){
                    if(melted && accessor != null){
                        accessor.drainHeatFromUnderlying(AtmosphereUtil.Constants.WATER_MELT_LATENT_HEAT_PER_QUANTA*0.5);//真的融化掉
                        doMelted = true;
                    }
                }
            }

            if(melted && !doMelted){
                melted = false;
                curAmountWater-= QBUtil.HALF_QUANTA_VOLUME; //没有用到，所以扣掉
            }

            if(changed) downState = world.getBlockState(downPos); //更新状态，因为下面的状态可能改变
            if(downState.getBlock() instanceof ILayeredFluidHost) host = (ILayeredFluidHost) downState.getBlock();
            else host = null;

            long curAmountSnow = getAmountInQB(world,pos,state,GeoFluids.SNOW);
            if(melted) curAmountSnow -= QBUtil.HALF_QUANTA_VOLUME; //如果上面真的融化，雪需要扣除相应的量
            boolean frozen = false,doFreeze = false;
            if(host != null && curAmountWater>=QBUtil.HALF_QUANTA_VOLUME && hasHalfQuanta){ //若有足够的水，则将半层水冻结成雪。这里水一定不会小于1/16 B
                curAmountSnow += QBUtil.HALF_QUANTA_VOLUME;
                curAmountWater-= QBUtil.HALF_QUANTA_VOLUME; //冻结一部分水
                frozen = true;
            }
            if(host != null && curAmountSnow>0 && host.canFill(world,downPos,downState,GeoFluids.SNOW,EnumFacing.UP,state)){
                final long filledAmount = host.addAmountInQB(world,downPos,downState,GeoFluids.SNOW,curAmountSnow,true);
                curAmountSnow -= filledAmount;
                if(curAmountSnow<QBUtil.HALF_QUANTA_VOLUME){ //说明可能用到了冻结的部分
                    if(frozen && accessor != null) {
                        accessor.putHeatToUnderlying(AtmosphereUtil.Constants.WATER_MELT_LATENT_HEAT_PER_QUANTA*0.5);
                        doFreeze = true;
                    }
                }
                changed |= filledAmount>0;
            }

            if(frozen && !doFreeze){
                frozen = false;
                curAmountSnow -= QBUtil.HALF_QUANTA_VOLUME;//没用到，还回去
                curAmountWater+= QBUtil.HALF_QUANTA_VOLUME;
            }

            if(!changed) return false;

            if(curAmountSnow+curAmountWater<QBUtil.QUANTA_VOLUME){
                world.setBlockToAir(pos);
            }else{
                final long totalAmount = curAmountSnow + curAmountWater;
                final int totalLayers = QBUtil.toQuanta(totalAmount);
                if(curAmountWater>curAmountSnow){
                    turnIntoWater(world,pos,accessor,totalLayers);
                    if(accessor != null)
                        accessor.drainHeatFromUnderlying(AtmosphereUtil.Constants.WATER_MELT_LATENT_HEAT_PER_QUANTA*QBUtil.toPreciseQuanta(curAmountSnow));
                }else if(curAmountSnow == curAmountWater){
                    world.setBlockState(pos,state.withProperty(LAYERS,totalLayers)
                            .withProperty(MIXTURE,true));
                }else {
                    world.setBlockState(pos,state.withProperty(LAYERS,totalLayers)
                            .withProperty(MIXTURE,false));
                    if(accessor != null)
                        accessor.putHeatToUnderlying(AtmosphereUtil.Constants.WATER_MELT_LATENT_HEAT_PER_QUANTA*QBUtil.toPreciseQuanta(curAmountWater));
                }
            }
            if(accessor != null) accessor.close();
            return true;
        }else{
            FluidOperationUtil.triggerDestroyBlockEffectByFluid(world,downPos,downState, GeoFluids.SNOW);
            world.setBlockToAir(pos);
            world.setBlockState(downPos,state);
            world.scheduleUpdate(downPos,this,tickRate(world));
        }
        return true;
    }

    @Shadow
    public boolean canPlaceBlockAt(@Nonnull World worldIn, @Nonnull BlockPos pos) {return false;}

    //**********
    // ILayeredFluidHost Block
    //**********

    @Override
    public boolean isAcceptedFluid(@Nullable World world, @Nullable BlockPos pos, @Nonnull IBlockState state, @Nonnull Fluid fluid) {
        return fluid == FluidRegistry.WATER || fluid == GeoFluids.SNOW;
    }

    @Override
    public int getLayers(@Nullable World world, @Nullable BlockPos pos, @Nonnull IBlockState state, @Nullable Fluid fluid) {
        if(fluid == FluidRegistry.WATER){
            return state.getValue(MIXTURE)?state.getValue(LAYERS):0;
        }else if(fluid == GeoFluids.SNOW){
            return state.getValue(MIXTURE)?state.getValue(LAYERS):state.getValue(LAYERS)*2;
        }else if(fluid == null){
            return state.getValue(LAYERS)*2;
        }
        return 0;
    }

    @Override
    public int getMaxLayers(@Nullable World world, @Nullable BlockPos pos, @Nonnull IBlockState state, @Nullable Fluid fluid) {
        if(fluid == null) return 16;
        if(!isAcceptedFluid(world, pos, state, fluid)) return 0;
        boolean mixture = state.getValue(MIXTURE);
        final int layer = state.getValue(LAYERS);
        if(mixture) return 16-layer;
        return fluid == GeoFluids.SNOW?16:16-2*layer; //不是混合物
    }

    @Override
    public int getEmptyHeight(@Nullable World world, @Nullable BlockPos pos, @Nonnull IBlockState state, @Nullable Fluid fluid) {
        if(fluid == FluidRegistry.WATER){
            return 0;
        }else if(fluid == GeoFluids.SNOW){
            return state.getValue(MIXTURE)?state.getValue(LAYERS)* getHeightPerLayer(world,pos,state):0;
        }
        return 0;
    }

    @Override
    public int getHeightPerLayer(@Nullable World world, @Nullable BlockPos pos, @Nonnull IBlockState state) {
        return LayeredFluidHostUtil.SIXTEENTH_HEIGHT;
    }

    @Override
    public long getAmountInQBPerLayer(@Nullable World world, @Nullable BlockPos pos, @Nonnull IBlockState state, @Nonnull Fluid fluid) {
        return QBUtil.HALF_QUANTA_VOLUME;
    }

    @Override
    public void addLayer(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Fluid fluid, int layer, @Nullable NBTTagCompound nbt, int disabledBlockFlags, int enabledBlockFlags) {
        if(!isAcceptedFluid(world, pos, state, fluid)) return;
        if(layer == 0) return;
        final boolean mixture = state.getValue(MIXTURE);

        final @Nullable IAtmosphereAccessor accessor = AtmosphereUtil.getLightedAtmosphereAccessor(world,pos,true);

        final int flag = APIMathUtil.getModifiedFlag(Constants.BlockFlags.DEFAULT,disabledBlockFlags,enabledBlockFlags);

        int curSnowLayer = state.getValue(LAYERS);
        if(mixture){
            int curWaterLayer = curSnowLayer;
            layer = MathHelper.clamp(layer,-curWaterLayer,16-2*curWaterLayer);
            if(fluid == FluidRegistry.WATER){
                curWaterLayer += layer;
                if(layer <0){
                    world.setBlockState(pos,state.withProperty(MIXTURE,false)
                            .withProperty(LAYERS,(curSnowLayer+curWaterLayer)>>1),flag);
                    if(accessor != null)
                        accessor.putHeatToUnderlying(AtmosphereUtil.Constants.WATER_MELT_LATENT_HEAT_PER_QUANTA*curWaterLayer/2d);
                }else{
                    world.setBlockState(pos,Blocks.FLOWING_WATER.getDefaultState()
                            .withProperty(BlockLiquid.LEVEL,8-(curSnowLayer+curWaterLayer)/2),flag);
                    if(accessor != null)
                        accessor.drainHeatFromUnderlying(AtmosphereUtil.Constants.WATER_MELT_LATENT_HEAT_PER_QUANTA*curSnowLayer/2d);
                }
            }else{
                curSnowLayer += layer;
                if(layer <0){
                    world.setBlockState(pos,Blocks.FLOWING_WATER.getDefaultState()
                            .withProperty(BlockLiquid.LEVEL,8-(curSnowLayer+curWaterLayer)/2),flag);
                    if(accessor != null)
                        accessor.drainHeatFromUnderlying(AtmosphereUtil.Constants.WATER_MELT_LATENT_HEAT_PER_QUANTA*curSnowLayer/2d);
                }else{
                    world.setBlockState(pos,state.withProperty(MIXTURE,false)
                            .withProperty(LAYERS,(curSnowLayer+curWaterLayer)>>1),flag);
                    if(accessor != null)
                        accessor.putHeatToUnderlying(AtmosphereUtil.Constants.WATER_MELT_LATENT_HEAT_PER_QUANTA*curWaterLayer/2d);
                }
            }
        }else{
            if(fluid == GeoFluids.SNOW){
                layer = MathHelper.clamp(layer,-2*curSnowLayer,(8-curSnowLayer)<<1);
                world.setBlockState(pos,state.withProperty(LAYERS,(curSnowLayer+ layer)>>1),flag);
            }else {
                layer = MathHelper.clamp(layer,0,(8-curSnowLayer)<<1);
                if(layer <curSnowLayer<<1){
                    world.setBlockState(pos,state.withProperty(LAYERS,(curSnowLayer+ layer)>>1),flag);
                    if(accessor != null)
                        accessor.putHeatToUnderlying(AtmosphereUtil.Constants.WATER_MELT_LATENT_HEAT_PER_QUANTA* layer/2d);
                }else if(layer == curSnowLayer<<1){
                    world.setBlockState(pos,state.withProperty(LAYERS,(curSnowLayer+ layer)>>1)
                            .withProperty(MIXTURE,true),flag);
                }else{
                    world.setBlockState(pos,Blocks.FLOWING_WATER.getDefaultState()
                            .withProperty(BlockLiquid.LEVEL,8-(curSnowLayer+ layer)/2),flag);
                    if(accessor != null){
                        accessor.drainHeatFromUnderlying(AtmosphereUtil.Constants.WATER_MELT_LATENT_HEAT_PER_QUANTA* layer/2d);
                    }
                }
            }
        }
        if(accessor != null) accessor.close();
    }

    @Override
    public int addLayer(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Fluid fluid, int layer, @Nullable NBTTagCompound nbt, int disabledBlockFlags, int enabledBlockFlags, boolean doAdd) {
        layer = (layer>>>1)<<1;
        return IBlockStateLayeredFluidHost.super.addLayer(world, pos, state, fluid, layer, nbt, disabledBlockFlags, enabledBlockFlags, doAdd);
    }

    @Override
    public boolean setLayer(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Fluid fluid, int newLayer,@Nullable NBTTagCompound nbt,final int disabledBlockFlags,final int enabledBlockFlags) {
        if(!isAcceptedFluid(world, pos, state, fluid)) return false;
        if(newLayer<0 || newLayer > 16) return false;
        final boolean mixture = state.getValue(MIXTURE);

        final int flag = APIMathUtil.getModifiedFlag(Constants.BlockFlags.DEFAULT,disabledBlockFlags,enabledBlockFlags);

        int curSnowLayer = state.getValue(LAYERS);
        if(mixture){
            int curWaterLayer = curSnowLayer;
            if(fluid == FluidRegistry.WATER){
                curWaterLayer = newLayer;
                if(curWaterLayer <curSnowLayer){
                    world.setBlockState(pos,state.withProperty(MIXTURE,false)
                            .withProperty(LAYERS,(curSnowLayer+curWaterLayer)>>1),flag);
                }else if(curWaterLayer>curSnowLayer){
                    world.setBlockState(pos,Blocks.FLOWING_WATER.getDefaultState()
                            .withProperty(BlockLiquid.LEVEL,8-(curSnowLayer+curWaterLayer)/2),flag);
                }
            }else{
                curSnowLayer = newLayer;
                if(curSnowLayer <curWaterLayer){
                    world.setBlockState(pos,Blocks.FLOWING_WATER.getDefaultState()
                            .withProperty(BlockLiquid.LEVEL,8-(curSnowLayer+curWaterLayer)/2),flag);
                }else if(curSnowLayer>curWaterLayer){
                    world.setBlockState(pos,state.withProperty(MIXTURE,false)
                            .withProperty(LAYERS,(curSnowLayer+curWaterLayer)>>1),flag);
                }
            }
        }else{
            if(fluid == GeoFluids.SNOW){
                world.setBlockState(pos,state.withProperty(LAYERS,(newLayer)>>1),flag);
            }else {
                if(newLayer <curSnowLayer<<1){
                    world.setBlockState(pos,state.withProperty(LAYERS,(curSnowLayer+ newLayer)>>1),flag);
                }else if(newLayer == curSnowLayer<<1){
                    world.setBlockState(pos,state.withProperty(LAYERS,(curSnowLayer+ newLayer)>>1)
                            .withProperty(MIXTURE,true),flag);
                }else{
                    world.setBlockState(pos,Blocks.FLOWING_WATER.getDefaultState()
                            .withProperty(BlockLiquid.LEVEL,8-(curSnowLayer+ newLayer)/2),flag);
                }
            }
        }
        return true;
    }

    @Nullable
    @Override
    public IBlockState getLayerState(@Nonnull IBlockState state, @Nonnull Fluid fluid, int layer) {
        if(!isAcceptedFluid(null,null,state,fluid)) return null;
        final boolean mixture = state.getValue(MIXTURE);
        final int curLayer = state.getValue(LAYERS);
        if(layer<0) return null;
        if(mixture && layer > getMaxLayers(null, null, state, fluid)) return null;
        else if(!mixture){
            if(fluid == FluidRegistry.WATER) if(layer>16-curLayer*2) return null;
            if(fluid == GeoFluids.SNOW) if(layer>16) return null;
        }

        if(fluid == GeoFluids.SNOW){
            if(mixture){
                if(layer ==curLayer) return state;
                if(layer == 0 && curLayer>1) return Blocks.FLOWING_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL,8-curLayer/2);
                if(layer <curLayer) return Blocks.FLOWING_WATER.getDefaultState()
                        .withProperty(BlockLiquid.LEVEL,8-(curLayer+ layer)/2);
                return state.withProperty(MIXTURE,false).withProperty(LAYERS,(curLayer+ layer)>>1);
            }
            if(layer == 0) return Blocks.AIR.getDefaultState();
            return state.withProperty(LAYERS, layer>>1);
        }else if(fluid == FluidRegistry.WATER){
            if(mixture){
                if(layer ==curLayer) return state;
                if(layer == 0) return state.withProperty(MIXTURE,false)
                        .withProperty(LAYERS,curLayer/2);
                if(layer >curLayer) return Blocks.FLOWING_WATER.getDefaultState()
                        .withProperty(BlockLiquid.LEVEL,8-(curLayer+ layer)/2);
                return state.withProperty(MIXTURE,false).withProperty(LAYERS,(curLayer+ layer)>>1);
            }
            if(layer == 0) return state;
            if(layer == curLayer<<1) return
                    state.withProperty(MIXTURE,true)
                            .withProperty(LAYERS, layer);
            else if(layer > curLayer<<1) return Blocks.FLOWING_WATER.getDefaultState()
                    .withProperty(BlockLiquid.LEVEL,8-(curLayer*2+ layer)/2);
            else return state.withProperty(LAYERS, (layer +curLayer*2)>>1);
        }
        return null;
    }
}
