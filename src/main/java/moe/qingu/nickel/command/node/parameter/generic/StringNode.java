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

package moe.qingu.nickel.command.node.parameter.generic;

import moe.qingu.nickel.command.exception.NickelSyntaxException;
import moe.qingu.nickel.command.reader.InputReader;
import moe.qingu.nickel.command.node.parameter.ParameterNode;
import net.minecraft.command.CommandException;
import moe.qingu.nickel.command.context.CommandContext;
import moe.qingu.nickel.command.utils.Acceptor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static moe.qingu.nickel.text.Texts.translation;

/**
 * @author QiguaiAAAA
 */
public class StringNode extends ParameterNode<String> {
    public static final Acceptor CHECK_WHITELIST = (self, input) -> {
        checkList(((StringNode)self).readContent(input),((StringNode) self).whitelist,false,input.getContext());
        return true;
    };
    public static final Acceptor CHECK_BLACKLIST = (self, input) -> {
        checkList(((StringNode)self).readContent(input),((StringNode) self).blacklist,true,input.getContext());
        return true;
    };
    public static final Acceptor CHECK_PATTERN = (self, input) -> {
        checkPattern(((StringNode)self).readContent(input),((StringNode) self).pattern,input.getContext());
        return true;
    };

    protected Mode mode = Mode.STRING;
    protected Set<String> whitelist = null;
    protected Set<String> blacklist = null;
    protected Pattern pattern = null;
    protected Acceptor checker = Acceptor.REQUIRE_ONE_TOKEN;

    public StringNode(@Nonnull final String name) {
        super(name);
    }

    public static void checkList(final @Nonnull String str, final @Nullable Set<String> list, final boolean isBlackList, final @Nonnull CommandContext context) throws CommandException{
        if(list == null) return;
        if(list.contains(str) ^ isBlackList) return;
        context.input.panic(translation(isBlackList?"nickel.command.parameter.string.invalid.black":"nickel.command.parameter.string.invalid.white")
                .arg(str,String.join(" ", list)));
    }

    public static void checkPattern(final @Nonnull String str, final @Nullable Pattern pattern, final @Nonnull CommandContext context) throws CommandException{
        if(pattern == null) return;
        if(pattern.matcher(str).matches()) return;
        context.input.panic(translation("nickel.command.parameter.string.nonMatch",str,pattern.toString()));
    }

    @Nonnull
    public String readContent(final @Nonnull InputReader input) throws CommandException {
        switch (mode){
            case GREED:return input.readRemaining();
            case TOKEN:return input.readToken();
            default:return input.readString();
        }
    }

    public void setMode(final @Nonnull Mode mode) {
        this.mode = mode;
    }

    public void addAllowValue(@Nonnull final String content){
        if(whitelist == null) whitelist = new HashSet<>();
        whitelist.add(content);
    }

    public void addDisallowedValue(@Nonnull final String content){
        if(blacklist == null) blacklist = new HashSet<>();
        blacklist.add(content);
    }

    public void setPattern(@Nullable final Pattern pattern){
        this.pattern = pattern;
    }

    @Nonnull
    public StringNode refresh(){
        checker = Acceptor.REQUIRE_ONE_TOKEN;
        if(whitelist != null && !whitelist.isEmpty()) checker = checker.and(CHECK_WHITELIST);
        if(blacklist != null && !blacklist.isEmpty()) checker = checker.and(CHECK_BLACKLIST);
        if(pattern != null) checker = checker.and(CHECK_PATTERN);
        return this;
    }

    @Nonnull
    @Override
    public Class<String> getTypeClass() {
        return String.class;
    }

    @Nonnull
    @Override
    public String getTypeTranslationKey() {
        switch (mode){
            case TOKEN:return "nickel.command.parameter.generic.token";
            case GREED:return "nickel.command.parameter.generic.greed";
            default:return "nickel.command.parameter.generic.string";
        }
    }

    @Override
    public boolean accepts(@Nonnull final InputReader input) throws CommandException {
        return checker.check(this,input);
    }

    @Override
    public String parse(@Nonnull final InputReader input, final boolean resolve) throws CommandException {
        switch (mode){
            case TOKEN: if(resolve) return input.readToken();
            else{
                input.skipContents();
                return null;
            }
            case GREED: if(resolve) return input.readRemaining();
            else {
                input.setCursor(input.getLength());
                return null;
            } default:{
                final String res;
                if(resolve) res = input.readString();
                else {
                    input.scanString();
                    res = null;
                }
                if(input.canRead() && !Character.isWhitespace(input.peek())) throw new NickelSyntaxException(this.currentBranch,this);
                return res;
            }
        }
    }

    public enum Mode{
        STRING,
        TOKEN,
        GREED
    }
}
