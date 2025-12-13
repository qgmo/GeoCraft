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

package top.qiguaiaaaa.geocraft.api.command.builder;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import top.qiguaiaaaa.geocraft.api.command.Context;
import top.qiguaiaaaa.geocraft.api.command.node.*;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * @author QiguaiAAAA
 */
public class Nodes {

    @Nonnull
    public static ConditionalSplitNodeBuilder split(){
        return new ConditionalSplitNodeBuilder();
    }

    @Nonnull
    public static LiteralNodeBuilder literal(){
        return new LiteralNodeBuilder();
    }

    @Nonnull
    public static ExecuteNodeBuilder execute(){
        return new ExecuteNodeBuilder();
    }

    @Nonnull
    public static RelayExecuteNodeBuilder relay(){return new RelayExecuteNodeBuilder();}

    @Nonnull
    public static DoubleNodeBuilder doubleP(@Nonnull final String name){
        return new DoubleNodeBuilder(name);
    }

    @Nonnull
    public static BlockPosNodeBuilder blockPos(@Nonnull final String name){
        return new BlockPosNodeBuilder(name);
    }

    @Nonnull
    public static ItemSelectorNodeBuilder selectItem(@Nonnull final String name){
        return new ItemSelectorNodeBuilder(name);
    }

    @Nonnull
    public static AtmosphereSelectorNodeBuilder selectAtmosphere(@Nonnull final String name){
        return new AtmosphereSelectorNodeBuilder(name);
    }

    @Nonnull
    public static PermitNodeBuilder permit(){
        return new PermitNodeBuilder();
    }

    public abstract static class ParameterNodeBuilder<T extends ParameterNode> implements INodeBuilder<T> {
        protected final String name;
        protected INodeBuilder<?> childNode;
        protected ICommandNode bakedChildNode;
        protected boolean optional;

        protected ParameterNodeBuilder(@Nonnull String name) {
            this.name = name;
        }

        @Nonnull
        public ParameterNodeBuilder<T> asOptional(){
            optional = true;
            return this;
        }

        @Nonnull
        public ParameterNodeBuilder<T> then(@Nonnull INodeBuilder<?> childNode){
            this.childNode = childNode;
            return this;
        }

        @Nonnull
        public ParameterNodeBuilder<T> then(@Nonnull ICommandNode childNode){
            this.bakedChildNode = childNode;
            return this;
        }

        @Nonnull
        @Override
        public T build() {
            final T instance = buildInstance();
            instance.setChildNode(bakedChildNode!=null?bakedChildNode:childNode.build());
            return instance;
        }

        @Nonnull
        protected abstract T buildInstance();
    }

    public static class DoubleNodeBuilder extends ParameterNodeBuilder<DoubleNode>{

        protected DoubleNodeBuilder(@Nonnull String name) {
            super(name);
        }

        @Nonnull
        @Override
        protected DoubleNode buildInstance() {
            return new DoubleNode(name);
        }
    }

    public static class AtmosphereSelectorNodeBuilder extends ParameterNodeBuilder<AtmosphereNode>{

        protected AtmosphereSelectorNodeBuilder(@Nonnull String name) {
            super(name);
        }

        @Nonnull
        @Override
        protected AtmosphereNode buildInstance() {
            return new AtmosphereNode(name);
        }
    }

    public static class ItemSelectorNodeBuilder extends ParameterNodeBuilder<ItemSelectorNode>{

        protected ItemSelectorNodeBuilder(@Nonnull String name) {
            super(name);
        }

        @Nonnull
        @Override
        protected ItemSelectorNode buildInstance() {
            return new ItemSelectorNode(name);
        }
    }

    public static class BlockPosNodeBuilder extends ParameterNodeBuilder<BlockPosNode>{

        protected BlockPosNodeBuilder(@Nonnull String name) {
            super(name);
        }

        @Nonnull
        @Override
        protected BlockPosNode buildInstance() {
            return new BlockPosNode(name);
        }
    }
    public static class PermitNodeBuilder implements INodeBuilder<PermitNode>{
        private BiFunction<MinecraftServer, ICommandSender,Boolean> funcCheckPermission = CommandBuilder.PERMIT_ALL;

        private INodeBuilder<?> childNode;
        private ICommandNode bakedChildNode;

        private PermitNodeBuilder(){}

        @Nonnull
        public PermitNodeBuilder then(@Nonnull INodeBuilder<?> childNode){
            this.childNode = childNode;
            return this;
        }

        @Nonnull
        public PermitNodeBuilder then(@Nonnull ICommandNode childNode){
            this.bakedChildNode = childNode;
            return this;
        }

        @Nonnull
        public PermitNodeBuilder passIf(@Nonnull BiFunction<MinecraftServer, ICommandSender,Boolean> funcCheckPermission){
            this.funcCheckPermission = funcCheckPermission;
            return this;
        }

        @Nonnull
        @Override
        public PermitNode build() {
            final PermitNode node = new PermitNode();
            node.setChecker(funcCheckPermission);
            node.setChildNode(bakedChildNode!=null?bakedChildNode:childNode.build());
            return node;
        }
    }

    public static class ExecuteNodeBuilder implements INodeBuilder<ExecuteNode>{
        public static final CommandRunFunction DO_NOTHING = (server, sender,args, serializedArgs) -> {};
        protected CommandRunFunction funcExecute = DO_NOTHING;

        protected ExecuteNodeBuilder(){}

        @Nonnull
        public ExecuteNodeBuilder run(@Nonnull CommandRunFunction runFunc){
            this.funcExecute = runFunc;
            return this;
        }

        @Nonnull
        @Override
        public ExecuteNode build() {
            final ExecuteNode node = (context, args) -> funcExecute.run(context.getWorld(),context.getSender(),args, context);
            return node;
        }

        @FunctionalInterface
        public interface CommandRunFunction{
            void run(@Nonnull World world, @Nonnull ICommandSender sender, @Nonnull List<String> args, @Nonnull Context context) throws CommandException;
        }
    }

    public static class RelayExecuteNodeBuilder extends ExecuteNodeBuilder{
        protected RelayExecuteNodeBuilder(){}

        protected INodeBuilder<?> childNode;
        protected ICommandNode bakedChildNode;

        @Nonnull
        public RelayExecuteNodeBuilder then(@Nonnull INodeBuilder<?> childNode){
            this.childNode = childNode;
            return this;
        }

        @Nonnull
        public RelayExecuteNodeBuilder then(@Nonnull ICommandNode childNode){
            this.bakedChildNode = childNode;
            return this;
        }

        @Nonnull
        @Override
        public RelayExecuteNodeBuilder run(@Nonnull CommandRunFunction runFunc) {
            return (RelayExecuteNodeBuilder) super.run(runFunc);
        }

        @Nonnull
        @Override
        public RelayExecuteNode build() {
            final RelayExecuteNode node = new RelayExecuteNode() {
                @Override
                public void run(@Nonnull Context context, @Nonnull List<String> args) throws CommandException {
                    funcExecute.run(context.getWorld(),context.getSender(),args,context);
                }
            };
            node.setChildNode(bakedChildNode != null?bakedChildNode:childNode.build());
            return node;
        }
    }

    public static class LiteralNodeBuilder implements INodeBuilder<LiteralNode>{
        private final Map<String, Object> literal2NodeMap = new LinkedHashMap<>();
        private ICommandNode bakedDefaultChild = null;
        private INodeBuilder<?> defaultChild = null;
        private boolean optional = false;
        private BiFunction<MinecraftServer, ICommandSender,Boolean> funcCheckPermission = CommandBuilder.PERMIT_ALL;

        private LiteralNodeBuilder(){}

        @Nonnull
        public LiteralNodeBuilder asOptional(){
            this.optional = true;
            return this;
        }

        @Nonnull
        public LiteralNodeBuilder defaultThen(@Nonnull ICommandNode node){
            bakedDefaultChild = node;
            return this;
        }

        @Nonnull
        public LiteralNodeBuilder defaultThen(@Nonnull INodeBuilder<?> nodeBuilder){
            defaultChild = nodeBuilder;
            return this;
        }

        @Nonnull
        public LiteralNodeBuilder defaultAs(@Nonnull String name){
            final Object node = literal2NodeMap.get(name);
            if(node instanceof ICommandNode) bakedDefaultChild = (ICommandNode) node;
            else if(node instanceof INodeBuilder<?>) defaultChild = (INodeBuilder<?>) node;
            else throw new IllegalArgumentException(name);
            return this;
        }

        @Nonnull
        public LiteralNodeBuilder ifThen(@Nonnull String name, @Nonnull ICommandNode node){
            literal2NodeMap.put(name,node);
            return this;
        }

        @Nonnull
        public LiteralNodeBuilder ifThen(@Nonnull String name, @Nonnull INodeBuilder<?> nodeBuilder){
            literal2NodeMap.put(name,nodeBuilder);
            return this;
        }

        @Nonnull
        public LiteralNodeBuilder permitIf(@Nonnull BiFunction<MinecraftServer, ICommandSender,Boolean> funcCheckPermission){
            this.funcCheckPermission = funcCheckPermission;
            return this;
        }

        @Nonnull
        @Override
        public LiteralNode build(){
            final LiteralNode node = new LiteralNode();
            node.setChecker(funcCheckPermission);
            literal2NodeMap.forEach((l,n)->{
                if(n instanceof ICommandNode) node.addLiteral(l,(ICommandNode) n);
                else if(n instanceof INodeBuilder<?>) node.addLiteral(l,((INodeBuilder<?>)n).build());
                else throw new IllegalArgumentException(l);
            });
            node.setDefaultNode(bakedDefaultChild!=null?bakedDefaultChild:defaultChild.build());
            node.setOptional(optional);
            return node;
        }
    }

    public static class ConditionalSplitNodeBuilder implements INodeBuilder<ConditionalSplitNode>{
        protected List<ConditionBuilder> conditions = new ArrayList<>();

        @Nonnull
        public ConditionBuilder as(@Nonnull BiPredicate<List<String>,Context> condition){
            final ConditionBuilder builder =new ConditionBuilder(condition);
            conditions.add(builder);
            return builder;
        }

        @Nonnull
        @Override
        public ConditionalSplitNode build() {
            final ConditionalSplitNode node = new ConditionalSplitNode();
            conditions.forEach(builder -> node.addCondition(builder.predicate,builder.bakedChild!=null?builder.bakedChild:builder.child.build()));
            return node;
        }

        public class ConditionBuilder{
            final BiPredicate<List<String>,Context> predicate;
            INodeBuilder<?> child;
            ICommandNode bakedChild;

            public ConditionBuilder(@Nonnull BiPredicate<List<String>, Context> predicate) {
                this.predicate = predicate;
            }

            @Nonnull
            public ConditionalSplitNodeBuilder then(@Nonnull INodeBuilder<?> node){
                child = node;
                return ConditionalSplitNodeBuilder.this;
            }

            @Nonnull
            public ConditionalSplitNodeBuilder then(@Nonnull ICommandNode node){
                bakedChild = node;
                return ConditionalSplitNodeBuilder.this;
            }
        }
    }

    public interface INodeBuilder<T extends ICommandNode>{
        @Nonnull
        T build();
    }
}
