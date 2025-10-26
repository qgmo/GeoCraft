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

package top.qiguaiaaaa.geocraft.mixin.groundwater.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
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
import top.qiguaiaaaa.geocraft.api.block.IBlockFalling;
import top.qiguaiaaaa.geocraft.api.util.APIMathUtil;
import top.qiguaiaaaa.geocraft.block.IBlockSoil;
import top.qiguaiaaaa.geocraft.configs.SoilConfig;
import top.qiguaiaaaa.geocraft.geography.soil.BlockSoilType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

import static top.qiguaiaaaa.geocraft.api.block.BlockProperties.HUMIDITY;

/**
 * @author QiguaiAAAA
 */
@Mixin(BlockFarmland.class)
public abstract class BlockFarmlandMixin extends Block implements IBlockSoil, IBlockFalling {
    @Unique
    private static final int STABLE_HUMIDITY = SoilConfig.STABLE_HUMIDITY.getValue().get(BlockSoilType.FARMLAND);

    @Unique
    private static final double FLOW_IN_P = SoilConfig.FLOW_IN_POSSIBILITY.getValue().get(BlockSoilType.FARMLAND),
            RAIN_IN_P = SoilConfig.RAIN_IN_POSSIBILITY.getValue().get(BlockSoilType.FARMLAND);

    @Unique
    private static boolean isRandomTick = false;
    @Shadow @Final protected static AxisAlignedBB field_194405_c;

    @Shadow @Final public static PropertyInteger MOISTURE;

    public BlockFarmlandMixin(Material materialIn) {
        super(materialIn);
    }

    @Override
    @Unique
    public void randomTick(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Random random) {
        isRandomTick = true;
        this.onRandomTick(worldIn, pos, state, random);
        super.randomTick(worldIn, pos, state, random);
        isRandomTick = false;
    }

    @Inject(method = "updateTick",at = @At("HEAD"),cancellable = true,order = 999)
    public void updateTick(@Nonnull World worldIn, @Nonnull BlockPos pos, IBlockState state, @Nonnull Random rand, CallbackInfo ci) {
        if(isRandomTick) return;
        ci.cancel();
        if(getLayers(worldIn,pos,state,FluidRegistry.WATER) <= getMaxStableHumidity(state)) return;
        if (!worldIn.isRemote) {
            this.checkAndFall(worldIn, pos);
        }
    }

    @Override
    @Unique
    public int getDustColor(IBlockState state) {
        return 0xFF866043;
    }

    @Override
    public void onPlayerDestroy(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
        dropWaterWhenBroken(worldIn, pos, state);
    }

    @Inject(method = "neighborChanged",at =@At("TAIL"))
    public void neighborChanged(IBlockState state, @Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull Block blockIn, @Nonnull BlockPos fromPos,CallbackInfo ci) {
        state = worldIn.getBlockState(pos);
        if(state.getBlock() != this) return;
        if(getLayers(worldIn,pos,state,FluidRegistry.WATER) <= getMaxStableHumidity(state)) return;
        worldIn.scheduleUpdate(pos, this, this.tickRate(worldIn));
    }

    @Override
    public int tickRate(@Nonnull World worldIn) {
        return 5;
    }

    @Override
    public boolean onBlockActivated(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityPlayer playerIn, @Nonnull EnumHand hand, @Nonnull EnumFacing facing, float hitX, float hitY, float hitZ) {
        return onPlayerUseBottle(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
    }

    @Inject(method = "turnToDirt",at = @At("HEAD"),cancellable = true)
    private static void turnToDirt_Inject(World world, BlockPos pos, CallbackInfo ci) {
        IBlockState state = world.getBlockState(pos);
        if(!(state.getBlock() instanceof IBlockSoil)) return;
        ci.cancel();
        int quanta = ((IBlockSoil)state.getBlock()).getLayers(world,pos,state,FluidRegistry.WATER);
        world.setBlockState(pos, Blocks.DIRT.getDefaultState().withProperty(HUMIDITY,quanta));

        AxisAlignedBB emptyAABBAbove = field_194405_c.offset(pos);

        for (Entity entity : world.getEntitiesWithinAABBExcludingEntity(null, emptyAABBAbove)) {
            double d0 = Math.min(emptyAABBAbove.maxY - emptyAABBAbove.minY, emptyAABBAbove.maxY - entity.getEntityBoundingBox().minY);
            entity.setPositionAndUpdate(entity.posX, entity.posY + d0 + 0.001, entity.posZ);
        }
    }

    //***********
    // IBlockSoil
    //***********

    @Nonnull
    @Override
    public BlockSoilType getType(@Nonnull IBlockState state) {
        return BlockSoilType.FARMLAND;
    }

    @Override
    public int getMaxStableHumidity(@Nonnull IBlockState state) {
        return STABLE_HUMIDITY;
    }

    @Override
    public double getFlowInPossibility(@Nonnull IBlockState state) {
        return FLOW_IN_P;
    }

    @Override
    public double getRainInPossibility(@Nonnull IBlockState state) {
        return RAIN_IN_P;
    }

    //***********
    // IPermeableBlock
    //***********

    @Override
    public int getLayers(@Nullable World world, @Nullable BlockPos pos, @Nonnull IBlockState state, @Nullable Fluid fluid) {
        if(fluid != null && fluid != FluidRegistry.WATER) return 0;
        int moisture = state.getValue(MOISTURE);
        return (moisture+1)>>1;
    }

    @Override
    public int getMaxLayers(@Nullable World world, @Nullable BlockPos pos, @Nonnull IBlockState state, @Nullable Fluid fluid) {
        if(fluid == FluidRegistry.WATER || fluid == null) return 4;
        return 0;
    }

    @Override
    public int getHeight(@Nullable World world, @Nullable BlockPos pos, @Nonnull IBlockState state, @Nullable Fluid fluid) {
        if(fluid == FluidRegistry.WATER || fluid == null) return getHeightPerLayer(world,pos,state)* getLayers(world,pos,state,fluid);
        return 0;
    }

    @Override
    public int getMaxHeight(@Nullable World world, @Nullable BlockPos pos, @Nonnull IBlockState state, @Nullable Fluid fluid) {
        if(fluid == FluidRegistry.WATER || fluid == null) return 576576;
        return 0;
    }

    @Override
    public int getHeightPerLayer(@Nullable World world, @Nullable BlockPos pos, @Nonnull IBlockState state) {
        return 144144;
    }

    @Override
    public void setLayer(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Fluid fluid, int newLayer,final int disabledBlockFlags,final int enabledBlockFlags) {
        if(fluid != FluidRegistry.WATER) return;
        newLayer = MathHelper.clamp(newLayer,0,4);
        int moisture = newLayer == 0?0: newLayer *2-1;
        world.setBlockState(pos,state.withProperty(MOISTURE,moisture), APIMathUtil.getModifiedFlag(Constants.BlockFlags.DEFAULT,disabledBlockFlags,enabledBlockFlags));
    }

    @Nullable
    @Override
    public IBlockState getLayerState(@Nonnull IBlockState state, @Nonnull Fluid fluid, int layer) {
        if(fluid != FluidRegistry.WATER) return null;
        if(layer <0 || layer >4) return null;
        int moisture = layer == 0?0: layer *2-1;
        return state.withProperty(MOISTURE,moisture);
    }

    @Override
    public boolean isFull(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nullable Fluid fluid) {
        if(fluid != FluidRegistry.WATER) return true;
        return state.getValue(MOISTURE) == 7;
    }
}
