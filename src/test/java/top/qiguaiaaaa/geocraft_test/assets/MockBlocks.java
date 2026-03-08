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

package top.qiguaiaaaa.geocraft_test.assets;

import net.minecraft.block.*;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import top.qiguaiaaaa.geocraft.geography.fluidphysics.finite.flow.FiniteFlowingVanilla;
import top.qiguaiaaaa.geocraft_test.GeoCraftTest;
import top.qiguaiaaaa.geocraft_test.block.*;
import top.qiguaiaaaa.geocraft_test.world.sandbox.MockSandboxEnvBuilder;

import javax.annotation.Nonnull;

/**
 * @author QiguaiAAAA
 */
public final class MockBlocks {
    public static final class LayeredFluidHosts{
        public static MockBlockFluidHostCommon FLUID_HOST_COMMON = new MockBlockFluidHostCommon();
    }

    public static final class Bases{
        public static final MockSandboxEnvBuilder.Impl BUILDER;

        public static BlockAir AIR = new MockBlockAir(new ResourceLocation(GeoCraftTest.MODID,"air"));
        public static Block STONE = MockBlockSimple.create(b -> b.withID("stone"));
        public static Block WOOD = MockBlockSimple.create(b -> b.withID("wood").withMapColor(MapColor.WOOD).withMaterial(Material.WOOD));

        public static IBlockState 〇 = AIR.getDefaultState();
        public static IBlockState 石 = STONE.getDefaultState();
        public static IBlockState 木 = WOOD.getDefaultState();

        static {
            BUILDER = MockSandboxEnvBuilder.create().withStateData(Bases.class);
        }
    }

    public static final class FiniteFluids{
        public static final MockSandboxEnvBuilder.Impl BUILDER;

        public static final FiniteFlowingVanilla WATER_FLOWING = MockBlockLiquid.create(b ->
                b.withID("water").withMaterial(Material.WATER).withMapColor(MapColor.WATER).withFluid(MockFluids.WATER));

        public static final BlockDynamicLiquid DYNAMIC_WATER = WATER_FLOWING.dynamic;
        public static final BlockStaticLiquid STATIC_LIQUID = WATER_FLOWING._static;

        public static IBlockState 丶 = STATIC_LIQUID.getDefaultState().withProperty(BlockLiquid.LEVEL,7); // 主 （读音）
        public static IBlockState 冫 = STATIC_LIQUID.getDefaultState().withProperty(BlockLiquid.LEVEL,6); // 冰
        public static IBlockState 氵 = STATIC_LIQUID.getDefaultState().withProperty(BlockLiquid.LEVEL,5); // 水
        public static IBlockState 灬 = STATIC_LIQUID.getDefaultState().withProperty(BlockLiquid.LEVEL,4); // 火
        public static IBlockState 水 = STATIC_LIQUID.getDefaultState().withProperty(BlockLiquid.LEVEL,3);
        public static IBlockState 沝 = STATIC_LIQUID.getDefaultState().withProperty(BlockLiquid.LEVEL,2); // 子
        public static IBlockState 淼 = STATIC_LIQUID.getDefaultState().withProperty(BlockLiquid.LEVEL,1); // 汪淼的淼
        public static IBlockState 㵘 = STATIC_LIQUID.getDefaultState().withProperty(BlockLiquid.LEVEL,0); // 打 水水水水

        public static IBlockState 涸 = DYNAMIC_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL,7); //几乎干涸
        public static IBlockState 浅 = DYNAMIC_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL,6); //浅水
        public static IBlockState 涓 = DYNAMIC_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL,5); //涓涓细流
        public static IBlockState 盈 = DYNAMIC_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL,4); //充盈的水流
        public static IBlockState 涨 = DYNAMIC_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL,3); //涨起来的水流
        public static IBlockState 洪 = DYNAMIC_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL,2); //洪水
        public static IBlockState 滔 = DYNAMIC_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL,1); //滔滔不绝
        public static IBlockState 溢 = DYNAMIC_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL,0); //快溢出来了

        static {
            BUILDER = MockSandboxEnvBuilder.create().withStateData(Bases.BUILDER).withStateData(FiniteFluids.class);
        }

        @Nonnull
        public static IBlockState 流(final @Nonnull IBlockState 静水){
            return WATER_FLOWING.dynamic.getDefaultState().withProperty(BlockLiquid.LEVEL,静水.getValue(BlockLiquid.LEVEL));
        }
    }
}
