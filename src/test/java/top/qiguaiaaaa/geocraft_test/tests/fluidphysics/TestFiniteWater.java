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

package top.qiguaiaaaa.geocraft_test.tests.fluidphysics;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import top.qiguaiaaaa.geocraft.configs.GeneralConfig;
import top.qiguaiaaaa.geocraft_test.GeoCraftTest;
import top.qiguaiaaaa.geocraft_test.assets.MockBlocks;
import top.qiguaiaaaa.geocraft_test.world.MockSimpleWorld;
import top.qiguaiaaaa.geocraft_test.world.sandbox.MockSimpleSandbox;
import top.qiguaiaaaa.geocraft_test.world.storage.MockWorldInfo;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;

import static top.qiguaiaaaa.geocraft_test.assets.MockBlocks.Bases.〇;
import static top.qiguaiaaaa.geocraft_test.assets.MockBlocks.FiniteFluids.*;
import static top.qiguaiaaaa.geocraft_test.world.sandbox.MockSandboxEnvBuilder.layer;

/**
 * @author QiguaiAAAA
 */
public final class TestFiniteWater extends GeoCraftTest {

    private static MockSimpleWorld world;

    @Test
    public void 测试直接下落() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        test();
    }

    @SuppressWarnings("unused")
    public static void 测试直接下落_Inner(){
        try {
            GeneralConfig.ENABLE_BLOCK_UPDATER.setValue(false);

            world = MockSimpleWorld.create(MockWorldInfo.create(b-> b.withGameType(GameType.CREATIVE)), false);
            world.setAirBlock(〇);
            final BlockPos raw = new BlockPos(1,1,1);

            /*
             Case 1
             */

            // 往右 X 正方向，往下 Z 正方向
            final String[] structure1 = new String[]{
                    layer("",
                            "石石石",
                            "石灬石",
                            "石石石"),
                    layer("",
                            "石石石",
                            "石灬石",
                            "石石石")
            };
            final String[] expected1 = new String[]{
                    layer("",
                            "石石石",
                            "石溢石",
                            "石石石"),
                    layer("",
                            "石石石",
                            "石〇石",
                            "石石石")
            };
            直接下落(structure1,expected1,raw,灬);

            /*
              Case 2
             */
            final String[] structure2 = new String[]{
                    layer("",
                            "石石石",
                            "木灬木",
                            "石石石"),
                    layer("",
                            "石〇石",
                            "〇灬〇",
                            "〇石石")
            };
            final String[] expected2 = new String[]{
                    layer("",
                            "石石石",
                            "木溢木",
                            "石石石"),
                    layer("",
                            "石〇石",
                            "〇〇〇",
                            "〇石石")
            };

            直接下落(structure2,expected2,raw,灬);

            /*
              Case 3
             */
            final String[] structure3 = new String[]{
                    layer("",
                            "石石石",
                            "木水木",
                            "石石石"),
                    layer("",
                            "石〇石",
                            "〇水〇",
                            "〇石石")
            };
            final String[] expected3 = new String[]{
                    layer("",
                            "石石石",
                            "木溢木",
                            "石石石"),
                    layer("",
                            "石〇石",
                            "〇浅〇",
                            "〇石石")
            };

            直接下落(structure3,expected3,raw,水);

            /*
              Case 4
             */
            final String[] structure4 = new String[]{
                    layer("",
                            "石石石",
                            "木水木",
                            "石石石"),
                    layer("",
                            "石〇石",
                            "〇丶〇",
                            "〇石石")
            };
            final String[] expected4 = new String[]{
                    layer("",
                            "石石石",
                            "木洪木",
                            "石石石"),
                    layer("",
                            "石〇石",
                            "〇〇〇",
                            "〇石石")
            };

            直接下落(structure4,expected4,raw,丶);

        }finally {
            GeneralConfig.ENABLE_BLOCK_UPDATER.setValue(true);
        }
    }

    private static void 直接下落(@Nonnull final String[] structure,
                                 @Nonnull final String[] expected,
                                 @Nonnull final BlockPos beginPos,
                                 @Nonnull final IBlockState beginState){
        final MockSimpleSandbox sandbox = new MockSimpleSandbox(MockBlocks.FiniteFluids.BUILDER.generateFromCharacters(structure));
        world.setSandbox(sandbox);
        Assertions.assertEquals(beginState,world.getBlockState(beginPos));
        MockBlocks.FiniteFluids.WATER_FLOWING.flowDown(
                world,
                beginPos,
                world.getBlockState(beginPos.down()),
                8-world.getBlockState(beginPos).getValue(BlockLiquid.LEVEL),
                5
        );
        BUILDER.print(sandbox.getStructure());
        BUILDER.assertEqualStructure(sandbox.getStructure(),expected);

    }
}
