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

package top.qiguaiaaaa.geocraft_test.block;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import org.junit.jupiter.api.Assertions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * @author QiguaiAAAA
 */
public class SerialisedBlockState {
    public static final IntSet ALLOWED_SUBSCRIPTS = IntSets.unmodifiable(
            new IntOpenHashSet("0123456789abcdefghijklmnopqrstuvwxyz".codePoints().toArray())
    );
    protected final int 汉字;
    protected final @Nonnull String subscripts;

    protected SerialisedBlockState(final int 汉字, @Nonnull final String subscripts) {
        Assertions.assertTrue(isValidSubscripts(subscripts));
        this.汉字 = 汉字;
        this.subscripts = subscripts;
    }

    @Nonnull
    public static SerialisedBlockState of(final @Nonnull String serialised){
        final int hanzi = serialised.codePointAt(0);
        final String subscripts = serialised.substring(Character.charCount(hanzi));
        return new SerialisedBlockState(hanzi,subscripts);
    }

    @Nonnull
    public static SerialisedBlockState of(final @Nonnull String 汉字, @Nonnull final String subscripts) {
        Assertions.assertFalse(汉字.isEmpty());
        Assertions.assertEquals(汉字.length(),Character.charCount(汉字.codePointAt(0)));
        return new SerialisedBlockState(汉字.codePointAt(0),subscripts);
    }

    public static boolean isValidSubscripts(final @Nonnull String subscripts){
        for(final int code: subscripts.codePoints().toArray()){
            if(!isSubscript(code)) return false;
        }
        return true;
    }

    public static boolean isSubscript(final int codePoint){
        return ALLOWED_SUBSCRIPTS.contains(codePoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(汉字,subscripts);
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        if(obj instanceof SerialisedBlockState){
            final SerialisedBlockState other = (SerialisedBlockState) obj;
            return other.汉字 == this.汉字 && this.subscripts.equals(other.subscripts);
        }else return false;
    }

    @Override
    public String toString() {
        return new StringBuilder().appendCodePoint(this.汉字).append(subscripts).toString();
    }
}
