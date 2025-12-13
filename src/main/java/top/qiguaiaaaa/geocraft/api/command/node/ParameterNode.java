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

package top.qiguaiaaaa.geocraft.api.command.node;

import net.minecraft.command.CommandException;
import net.minecraft.command.WrongUsageException;
import top.qiguaiaaaa.geocraft.api.command.Context;

import javax.annotation.Nonnull;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * @author QiguaiAAAA
 */
public abstract class ParameterNode extends NoSplitNode implements IOptionalNode{
    protected final String name;
    protected boolean optional;
    protected DefaultParser defaultParser;

    public ParameterNode(@Nonnull String name) {
        this.name = name;
    }

    public void setDefaultParser(DefaultParser defaultParser) {
        this.defaultParser = defaultParser;
    }

    public String getName() {
        return name;
    }

    @Override
    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    @Override
    public boolean isOptional() {
        return optional;
    }

    public abstract <T extends List<String> & Deque<String>> boolean checkValid(@Nonnull T args, @Nonnull Context context) throws WrongUsageException;
    public abstract <T extends List<String> & Deque<String>> void parseParameter(@Nonnull T args, @Nonnull Context context) throws CommandException;

    protected final <T extends List<String> & Deque<String>> boolean checkAndParse(@Nonnull T args, @Nonnull Context context) throws CommandException{
        if(checkValid(args,context)){
            parseParameter(args,context);
            return true;
        }
        if(defaultParser!=null) defaultParser.parser(this,context);
        return false;
    }

    public interface DefaultParser{
        void parser(@Nonnull ParameterNode node,@Nonnull Context context) throws CommandException;

        default DefaultParser andThen(@Nonnull DefaultParser after) {
            Objects.requireNonNull(after);

            return (l, r) -> {
                parser(l, r);
                after.parser(l, r);
            };
        }
    }
}
