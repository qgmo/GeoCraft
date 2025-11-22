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

package test_pack;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import test_pack.block.FakeBlock;
import test_pack.block.state.FakeBlockStateContainer;
import top.qiguaiaaaa.geocraft.api.block.ILayeredFluidHost;
import top.qiguaiaaaa.geocraft.api.util.LayeredFluidHostUtil;
import top.qiguaiaaaa.geocraft.api.util.QBUtil;
import top.qiguaiaaaa.geocraft.api.util.math.FlowChoice;
import top.qiguaiaaaa.geocraft.fluid.FluidSnow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author QiguaiAAAA
 */
public class LayeredFluidHostTest {
    public static final PropertyInteger LAYERS = PropertyInteger.create("layers",1,8);
    public static final Fluid testFluid = new FluidSnow();
    public static final TestBlock block = new TestBlock(Material.WATER);

    public static final Logger LOGGER = LogManager.getLogger("Layered Test");

    @Test
    public void testQB(){
        long filled = block.addAmountInQB(null,BlockPos.ORIGIN,block.getDefaultState().withProperty(LAYERS,7),testFluid,QBUtil.QUANTA_VOLUME,false);
        Assert.assertEquals(QBUtil.QUANTA_VOLUME,filled);
        filled = block.addAmountInQB(null,BlockPos.ORIGIN,block.getDefaultState().withProperty(LAYERS,3),testFluid,QBUtil.BUCKET_VOLUME,false);
        Assert.assertEquals(QBUtil.BUCKET_VOLUME-3*QBUtil.QUANTA_VOLUME,filled);
        filled = block.addAmountInQB(null,BlockPos.ORIGIN,block.getDefaultState().withProperty(LAYERS,1),testFluid,QBUtil.BUCKET_VOLUME,true);
        Assert.assertEquals(QBUtil.BUCKET_VOLUME-QBUtil.QUANTA_VOLUME,filled);
    }

    @Test
    public void testAverageFlow(){
        int T = 5000;
        while (T-->0){
            LOGGER.info("Test {} begin!",T+1);
            final Map<EnumFacing,IBlockState> facingState = new HashMap<>();
            final Random random = new Random(System.nanoTime());
            for(EnumFacing facing:EnumFacing.HORIZONTALS){
                if(random.nextDouble()<0.2) continue;
                IBlockState state = block.getDefaultState().withProperty(LAYERS,random.nextInt(8)+1);
                facingState.put(facing,state);
                LOGGER.info("Dir {} is state {}",facing,state);
            }

            final List<FlowChoice> averageModeFlowDirections = new ArrayList<>();
            facingState.forEach((facing, state) -> averageModeFlowDirections.add(new FlowChoice(null,BlockPos.ORIGIN,state,block,facing,testFluid)));

            final int centralLayers = random.nextInt(8)+1;
            LOGGER.info("Central layers is {}",centralLayers);
            int left = LayeredFluidHostUtil.averageFlow(centralLayers,
                    block.getHeightPerLayer(null,null,null),
                    block.getAmountInQBPerLayer(null,null,null,testFluid),
                    0,
                    averageModeFlowDirections
            );

            LOGGER.info("Central left : {}",left);

            for(FlowChoice choice:averageModeFlowDirections){
                Assert.assertNotNull(choice);
                Assert.assertEquals(0,choice.apply(null,BlockPos.ORIGIN,facingState.get(choice.direction),testFluid));
            }
        }
    }

    public static class TestBlock extends FakeBlock implements ILayeredFluidHost{

        public TestBlock(@Nonnull Material material) {
            super(material);
            this.setDefaultState(this.getDefaultState().withProperty(LAYERS,1));
            this.setRegistryName(GeoCraftTest.MODID,"test_block");
        }

        @Nonnull
        @Override
        public FakeBlockStateContainer createBlockStates() {
            return new FakeBlockStateContainer(this,LAYERS);
        }

        @Override
        public boolean isAcceptedFluid(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Fluid fluid) {
            return fluid == testFluid;
        }

        @Override
        public int getLayers(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nullable Fluid fluid) {
            return fluid == null || isAcceptedFluid(world, pos, state, fluid)?state.getValue(LAYERS):0;
        }

        @Override
        public int getEmptyHeight(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nullable Fluid fluid) {
            return LayeredFluidHostUtil.EMPTY_HEIGHT;
        }

        @Override
        public int getHeightPerLayer(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
            return LayeredFluidHostUtil.EIGHTH_HEIGHT;
        }

        @Override
        public long getAmountInQBPerLayer(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Fluid fluid) {
            return isAcceptedFluid(world, pos, state, fluid)?QBUtil.QUANTA_VOLUME:0L;
        }

        @Override
        public boolean setLayer(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Fluid fluid, int newLayer, @Nullable NBTTagCompound nbt, int disabledBlockFlags, int enabledBlockFlags) {
            return true;
        }
    }
}
