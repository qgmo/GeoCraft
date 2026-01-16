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

package top.qiguaiaaaa.geocraft.api.command.node.parament.minecraft;

import net.minecraft.command.*;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import top.qiguaiaaaa.geocraft.api.command.context.CommandContext;
import top.qiguaiaaaa.geocraft.api.command.context.ExecuteContext;
import top.qiguaiaaaa.geocraft.api.command.context.SuggestContext;
import top.qiguaiaaaa.geocraft.api.command.node.parament.SmartParameterNode;
import top.qiguaiaaaa.geocraft.api.command.utils.Matchers;
import top.qiguaiaaaa.geocraft.api.command.utils.ValidChecker;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Stack;
import java.util.function.BiFunction;

/**
 * @author QiguaiAAAA
 */
public class ItemStackNode extends SmartParameterNode<ItemStack> {
    private static final ThreadLocal<Stack<String>> SelfParasStack = ThreadLocal.withInitial(Stack::new);
    public static final DefaultParser<ItemStack> DEFAULT_PARSER = (node, context) -> new ItemStack(Items.AIR,1);

    public static final BiFunction<List<String>, SuggestContext,List<String>> DEFAULT_SUGGESTOR = ((args, context) -> {
        switch (args.size()){
            case 0:
            case 1:
                return ItemSelectorNode.DEFAULT_SUGGESTOR.apply(args,context);
            case 2:
                return Collections.singletonList("1");
            case 3:
                return Collections.singletonList("0");
            default: return null;
        }
    });

    protected boolean allowNBT = true;

    public ItemStackNode(@Nonnull String name) {
        super(name);
        setDefaultParser(DEFAULT_PARSER);
        setSuggestProvider(DEFAULT_SUGGESTOR);
        setMatcher(Matchers.RESOURCE_LOCATION);
    }

    @Override
    public <T extends List<String> & Deque<String>> void execute(@Nonnull T args, @Nonnull ExecuteContext context) throws CommandException {
        if(childNode == null) return;
        if(!allowNBT){
            super.execute(args,context);
            return;
        }
        int usedParas = 0;
        if(checkValid(args,context)){
            if(!args.get(0).contains("\\{")){
                usedParas = 3;
                context.put(name,parseParameter(args,context));
            } else usedParas = parseStackWithNBT(args,context);
        }else if(defaultParser!=null){
            context.put(name,defaultParser.parser(this,context));
        }

        final Stack<String> paras = usedParas>0?SelfParasStack.get():null;
        if(paras != null){
            for(int i=0;i<usedParas;i++){
                paras.push(args.pollFirst());
            }
        }

        try {
            childNode.execute(args,context);
        }finally {
            if(paras != null){
                for(int i=0;i<usedParas;i++){
                    args.addFirst(paras.pop());
                }
            }
            context.remove(name);
        }
    }

    @Override
    public int getParametersLength() {
        return allowNBT?-1:3; //NBT会包含空格，所以是动态的
    }

    @Nonnull
    @Override
    public Class<ItemStack> getType() {
        return ItemStack.class;
    }

    @Override
    public <T extends List<String> & Deque<String>> ItemStack parseParameter(@Nonnull T args, @Nonnull ExecuteContext context) throws CommandException {
        final int count = CommandBase.parseInt(args.get(1),0,Integer.MAX_VALUE);
        final int meta = CommandBase.parseInt(args.get(2),-1,Integer.MAX_VALUE);
        final @Nonnull Item item = CommandBase.getItemByText(context.getSender(),args.getFirst());
        return new ItemStack(item,count,meta);
    }

    public <T extends List<String> & Deque<String>> int parseStackWithNBT(@Nonnull T args,@Nonnull ExecuteContext context) throws CommandException {
        final String[] split = args.getFirst().split("\\{",2);
        if(split.length <= 1) throw new IllegalArgumentException();
        final @Nonnull Item item = CommandBase.getItemByText(context.getSender(),split[0]);

        final int[] prefix = new int[args.size()]; //长度前缀和
        prefix[0] = split[1].length() + 1;
        final StringBuilder builder = new StringBuilder("{").append(split[1]);
        for(int i=1;i<args.size();i++){
            prefix[i] = prefix[i-1] + 1 + args.get(i).length();
            builder.append(" ").append(args.get(i));
        }
        NBTTagCompound compound = null;
        int nbtEndIndex = args.size()-1;
        for(;nbtEndIndex>=0;nbtEndIndex--){
            try {
                compound = JsonToNBT.getTagFromJson(builder.substring(0,prefix[nbtEndIndex]));
                break;
            }catch (NBTException e) {
                if(nbtEndIndex>0) continue;
                throw new CommandException("commands.give.tagError", e.getMessage());
            }
        }
        assert compound != null;
        final int countIndex = nbtEndIndex+1;
        final int count = CommandBase.parseInt(args.get(countIndex),0,Integer.MAX_VALUE);
        final int meta = CommandBase.parseInt(args.get(countIndex+1),-1,Integer.MAX_VALUE);
        final ItemStack stack = new ItemStack(item,count,meta);
        stack.setTagCompound(compound);
        context.put(name,stack);
        return countIndex+2;
    }

    @Override
    public boolean checkValid(@Nonnull List<String> args, @Nonnull CommandContext context) throws SyntaxErrorException, NumberInvalidException, InvalidBlockStateException {
        if(!ValidChecker.MATCH_THREE_PARAMETER.check(this,args,context)) return false;
        return ValidChecker.MATCH_RESOURCE_LOCATION.check(this, args, context); //数字是否正确就在运行时检查
    }
}
