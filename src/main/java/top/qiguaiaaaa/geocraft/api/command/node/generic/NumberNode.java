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

package top.qiguaiaaaa.geocraft.api.command.node.generic;

import com.google.common.collect.Lists;
import net.minecraft.command.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import top.qiguaiaaaa.geocraft.api.command.Context;
import top.qiguaiaaaa.geocraft.api.command.node.ParameterNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Deque;
import java.util.List;

/**
 * @author QiguaiAAAA
 */
public abstract class NumberNode<T extends Number> extends ParameterNode<T> {
    public NumberNode(@Nonnull String name) {
        super(name);
    }

    protected T minValue;
    protected T maxValue;

    public void setMinValue(@Nonnull T minValue) {
        this.minValue = minValue;
    }

    public void setMaxValue(@Nonnull T maxValue) {
        this.maxValue = maxValue;
    }

    @Override
    public int getParametersLength() {
        return 1;
    }

    protected abstract T parseNumber(@Nonnull String arg) throws NumberInvalidException;

    @Override
    public <V extends List<String> & Deque<String>> boolean checkValid(@Nonnull V args, @Nonnull Context context) throws WrongUsageException {
        if(args.isEmpty()&&!isOptional()) throw new WrongUsageException("wrong!");
        return !args.isEmpty();
    }

    @Override
    public <T1 extends List<String> & Deque<String>> void parseParameter(@Nonnull T1 args, @Nonnull Context context) throws CommandException {
        context.put(name, parseNumber(args.get(0)));
    }

    @Nullable
    @Override
    public List<String> suggestParameter(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull List<String> args, @Nullable BlockPos targetPos) {
        return Lists.newArrayList(String.valueOf(MathHelper.clamp(0,minValue.doubleValue(),maxValue.doubleValue())));
    }
}
