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

package 清汩萌.造.空间;

import net.minecraft.block.state.IBlockState;
import 清汩萌.造.工具.StringUtil;
import 清汩萌.造.词块.词块;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author QiguaiAAAA
 */
public final class 词块网格 {
    private final @Nonnull 空间构造器 $构造器;
    private final List<一层词块> $层列表 = new ArrayList<>();
    private 词块 $默认填充方块;
    private int $行数 = 0;
    private int $列数 = 0;

    public 词块网格(@Nonnull final 空间构造器 $构造器) {
        this.$构造器 = $构造器;
    }

    @Nonnull
    public 词块网格 默认用(final @Nonnull String $默认方块) {
        this.$默认填充方块 = 词块.of($默认方块);
        return this;
    }

    @Nonnull
    public 词块网格 默认用(final @Nonnull 词块 $词块) {
        this.$默认填充方块 = $词块;
        return this;
    }

    @Nonnull
    public 词块网格 默认用(final @Nonnull IBlockState state) {
        this.$默认填充方块 = $构造器.进行映射(state);
        return this;
    }

    @Nonnull
    public 一层词块 层() {
        return new 一层词块();
    }

    @Nonnull
    public 词块网格 略() {
        略(1);
        return this;
    }

    @Nonnull
    public 词块网格 略(final int $略去层数) {
        if ($略去层数 < 1) throw new IllegalArgumentException();
        for (int i = 0; i < $略去层数; i++) $层列表.add(new 一层词块());
        return this;
    }

    @Nonnull
    public IBlockState[][][] 构造() {
        填充默认();
        return $构造器.构造($层列表.stream().map($单层 -> $单层.$行列表).collect(Collectors.toList()));
    }

    private void 填充默认(){
       for(final @Nonnull 一层词块 $单层:$层列表) $单层.填充默认();
    }

    @Override
    public String toString() {
        填充默认();
        return $层列表.stream().map(Object::toString).collect(Collectors.joining("\n\n"));
    }

    public final class 一层词块 {
        private final List<List<词块>> $行列表 = new ArrayList<>();
        private int $最大列数 = 0;

        @Nonnull
        public 一层词块 行(final @Nonnull String line) {
            final @Nonnull List<词块> dat = 空间构造器.解析行(StringUtil.removeWhites(line));
            this.$行列表.add(dat);
            $最大列数 = Math.max($最大列数, dat.size());
            return this;
        }

        @Nonnull
        public 词块网格 完成() {
            $层列表.add(this);
            $行数 = Math.max($行数, $行列表.size());
            $列数 = Math.max($列数, $最大列数);
            return 词块网格.this;
        }

        @Override
        public String toString() {
            填充默认();
            return $行列表.stream().map($单行 -> $单行.stream().map(Objects::toString).collect(Collectors.joining())).collect(Collectors.joining("\n"));
        }

        private void 填充默认(){
            while ($行列表.size() < $行数) $行列表.add(new ArrayList<>());
            if($默认填充方块 == null) return;
            for (final @Nonnull List<词块> $单行 : $行列表)
                while ($单行.size() < $列数) $单行.add($默认填充方块);
        }
    }
}
