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
import net.minecraft.block.BlockGrassPath;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.qiguaiaaaa.geocraft.api.block.IBlockFalling;
import top.qiguaiaaaa.geocraft.block.IBlockSoil;
import top.qiguaiaaaa.geocraft.configs.SoilConfig;
import top.qiguaiaaaa.geocraft.geography.soil.BlockSoilType;

import javax.annotation.Nonnull;

import java.util.Random;

import static top.qiguaiaaaa.geocraft.api.block.BlockProperties.HUMIDITY;

/**
 * @author QiguaiAAAA
 */
@Mixin(BlockGrassPath.class)
public class BlockGrassPathMixin extends Block implements IBlockSoil,IBlockFalling {

    @Nonnull
    @Override
    @Unique
    public IBlockState getStateFromMeta(int meta) {
        if(meta>4) return getDefaultState();
        return this.getDefaultState().withProperty(HUMIDITY,meta);
    }

    @Override
    @Unique
    public int getMetaFromState(@Nonnull IBlockState state) {
        return state.getValue(HUMIDITY);
    }

    public BlockGrassPathMixin(Material materialIn) {
        super(materialIn);
    }

    @Override
    @Unique
    public void randomTick(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Random random) {
        this.onRandomTick(worldIn, pos, state, random);
    }

    @Override
    public void updateTick(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Random rand) {
        if(state.getValue(HUMIDITY) <= getMaxStableHumidity(state)) return;
        IBlockFalling.super.updateTick(worldIn, pos, state, rand);
    }

    @Override
    @Unique
    public void onPlayerDestroy(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
        this.dropWaterWhenBroken(worldIn, pos, state);
    }

    @Inject(method = "neighborChanged",at =@At("TAIL"))
    public void neighborChanged(IBlockState state, @Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull Block blockIn, @Nonnull BlockPos fromPos,CallbackInfo ci) {
        state = worldIn.getBlockState(pos);
        if(state.getBlock() != this) return;
        if(state.getValue(HUMIDITY) <= getMaxStableHumidity(state)) return;
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

    @Nonnull
    @Override
    @Unique
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this,HUMIDITY);
    }

    @Inject(method = "<init>",at = @At(value = "RETURN"))
    private void injectDefaultState(CallbackInfo ci) {
        this.setTickRandomly(true);
        this.setDefaultState((this.blockState.getBaseState().withProperty(HUMIDITY, 0)));
    }

    @Nonnull
    @Override
    public BlockSoilType getType(@Nonnull IBlockState state) {
        return BlockSoilType.GRASS_PATH;
    }
}
