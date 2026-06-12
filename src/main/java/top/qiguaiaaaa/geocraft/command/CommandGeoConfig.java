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

package top.qiguaiaaaa.geocraft.command;

import moe.qingu.nickel.command.builder.CommandBuilder;
import moe.qingu.nickel.command.context.ExecuteContext;
import moe.qingu.nickel.command.exception.NickelRuntimeException;
import moe.qingu.nickel.command.node.parameter.generic.StringNode;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import top.qiguaiaaaa.geocraft.GeoCraft;
import top.qiguaiaaaa.geocraft.api.configs.item.ConfigItem;
import top.qiguaiaaaa.geocraft.api.configs.item.base.ConfigBoolean;
import top.qiguaiaaaa.geocraft.api.configs.item.base.ConfigCustom;
import top.qiguaiaaaa.geocraft.api.configs.item.base.ConfigString;
import top.qiguaiaaaa.geocraft.api.configs.item.number.ConfigDouble;
import top.qiguaiaaaa.geocraft.api.configs.item.number.ConfigInteger;
import top.qiguaiaaaa.geocraft.api.configs.item.number.ConfigLong;
import top.qiguaiaaaa.geocraft.command.node.ConfigItemNode;
import top.qiguaiaaaa.geocraft.configs.ConfigurationLoader;

import javax.annotation.Nonnull;

import java.util.HashMap;

import static moe.qingu.nickel.command.Nodes.*;
import static moe.qingu.nickel.text.Texts.*;

/**
 * @author QGMoe
 */
public final class CommandGeoConfig {
    public static final String GEO_CONFIG_COMMAND_NAME = "geoconfig";
    public static final String GEO_CONFIG_PERMISSION_NODE = "geocraft.command."+GEO_CONFIG_COMMAND_NAME;

    private static final HashMap<Class<? extends ConfigItem<?,?>>, Processor> applicationProcessors = new HashMap<>();

    static{
        putProcessor(ConfigString.class, ConfigItem::setValue);
        applicationProcessors.put((Class<? extends ConfigItem<?,?>>) (Object) ConfigCustom.class,(config,val)->{ //NND这东西泛型推断太麻烦了，(Object)不能删不然编译过不了
            try {
                ((ConfigCustom)config).setValue(((ConfigCustom<?>)config).getParser().apply(val));
            }catch (final @Nonnull RuntimeException e){
                throw new NickelRuntimeException(translation("geocraft.command.geoconfig.set.custom.error").arg(e.getMessage()));
            }
        });
        putProcessor(ConfigBoolean.class, (config, val) -> {
            if("true".equals(val)) config.setValue(true);
            else if("false".equals(val)) config.setValue(false);
            else throw new NickelRuntimeException(translation("geocraft.command.geoconfig.set.boolean.synatx_error").arg(plain(val).italic(true)));
        });
        putProcessor(ConfigInteger.class,(config,val) -> {
            try{
                final int num = Integer.parseInt(val);
                if(num < config.getMinValue()) throw throwTooMin(val,String.valueOf(config.getMinValue()));
                else if(num > config.getMaxValue()) throw throwTooMax(val,String.valueOf(config.getMaxValue()));
                config.setValue(num);
            }catch (final NumberFormatException e){
                throw new NickelRuntimeException(translation("geocraft.command.geoconfig.set.int.synatx_error").arg(plain(val).italic(true)));
            }
        });
        putProcessor(ConfigDouble.class,(config,val) -> {
            try{
                final double num = Double.parseDouble(val);
                if(num < config.getMinValue()) throw throwTooMin(val,String.valueOf(config.getMinValue()));
                else if(num > config.getMaxValue()) throw throwTooMax(val,String.valueOf(config.getMaxValue()));
                config.setValue(num);
            }catch (final NumberFormatException e){
                throw new NickelRuntimeException(translation("geocraft.command.geoconfig.set.double.synatx_error").arg(plain(val).italic(true)));
            }
        });
        putProcessor(ConfigLong.class,(config, val) -> {
            try{
                final long num = Long.parseLong(val);
                if(num < config.getMinValue()) throw throwTooMin(val,String.valueOf(config.getMinValue()));
                else if(num > config.getMaxValue()) throw throwTooMax(val,String.valueOf(config.getMaxValue()));
                config.setValue(num);
            }catch (final NumberFormatException e){
                throw new NickelRuntimeException(translation("geocraft.command.geoconfig.set.long.synatx_error").arg(plain(val).italic(true)));
            }
        });
    }

    @Nonnull
    private static NickelRuntimeException throwTooMin(final @Nonnull String num, final @Nonnull String minVal){
        return new NickelRuntimeException(translation("geocraft.command.geoconfig.set.num.too_min")
                .arg(plain(num).italic(true))
                .arg(plain(minVal).color(TextFormatting.AQUA).bold(true)));
    }

    @Nonnull
    private static NickelRuntimeException throwTooMax(final @Nonnull String num, final @Nonnull String maxVal){
        return new NickelRuntimeException(translation("geocraft.command.geoconfig.set.num.too_max")
                .arg(plain(num).italic(true))
                .arg(plain(maxVal).color(TextFormatting.AQUA).bold(true)));
    }

    private static <T extends ConfigItem<?,T>> void putProcessor(final @Nonnull Class<T> cls,final @Nonnull ConfigProcessor<T> processor){
        applicationProcessors.put(cls,processor);
    }

    @Nonnull
    public static ICommand create(){
        return new CommandBuilder(GEO_CONFIG_COMMAND_NAME)
                .require(GEO_CONFIG_PERMISSION_NODE).allow(DefaultPermissionLevel.OP).register()
                .require(2)
                .then(ConfigItemNode.configItem("config_entry")
                        .translate("geocraft.command.geoconfig.arg.config_entry")
                        .smart()
                        .literal("set")
                        .require(4)
                        .requirePlayer(true)
                        .require(GEO_CONFIG_PERMISSION_NODE+".set").allow(DefaultPermissionLevel.OP).register()
                        .then(string("config_value")
                                .translate("geocraft.command.geoconfig.arg.config_value")
                                .then(execute(CommandGeoConfig::applyChanges))
                        ).done()
                        .execute(CommandGeoConfig::showInfo)
                        .done())
                .build();
    }

    public static void showInfo(@Nonnull final ExecuteContext ctx) {
        final ConfigItem<?,?> item = ctx.get("config_entry",ConfigItemNode.class);
        ctx.getSender().sendMessage(translation("geocraft.command.geoconfig.info.title")
                .arg(item.getPath())
                .color(TextFormatting.AQUA)
                .bold(true)
                .hoverTo(HoverEvent.Action.SHOW_TEXT)
                .then((item.hasComment()?plain(item.getComment()):translation("geocraft.command.geoconfig.info.no_comment"))
                        .italic(true))
                .done());
        ctx.getSender().sendMessage(translation("geocraft.command.geoconfig.info.type").arg(item.getClass().getSimpleName()).done());
        ctx.getSender().sendMessage(translation("geocraft.command.geoconfig.info.mode")
                .arg(translation(item.getEffectiveMode().getTranslationKey())
                        .color(item.getEffectiveMode().getColor())
                        .bold(true))
                .done());
        final String value = item.getValue().toString();
        ctx.getSender().sendMessage(translation("geocraft.command.geoconfig.info.cur_value")
                .arg(value.codePoints().count()<50?plain(value).color(TextFormatting.AQUA):
                        translation("geocraft.command.geoconfig.info.cur_value.out_of_range").color(TextFormatting.GOLD))
                .done());
    }

    public static void applyChanges(@Nonnull final ExecuteContext ctx) throws CommandException {
        final ConfigItem<?,?> item = ctx.get("config_entry",ConfigItemNode.class);
        final String val = ctx.get("config_value", StringNode.class);
        for(Class<?> cls = item.getClass();cls != Object.class;cls = cls.getSuperclass()){
            final Processor processor = applicationProcessors.get(cls);
            if(processor != null){
                processor.process(item,val);
                final String value = item.getValue().toString();
                try {
                    ConfigurationLoader.save();
                }catch (final @Nonnull RuntimeException e){
                    GeoCraft.getLogger().error(e);
                    ctx.getSender().sendMessage(translation("geocraft.command.geoconfig.set.save_fail")
                            .arg(value.codePoints().count()<50?plain(value).color(TextFormatting.AQUA):
                                    translation("geocraft.command.geoconfig.info.cur_value.out_of_range").color(TextFormatting.GOLD))
                            .arg(e.getMessage())
                            .color(TextFormatting.RED)
                            .done());
                    return;
                }
                ctx.getSender().sendMessage(translation("geocraft.command.geoconfig.set.success")
                        .arg(value.codePoints().count()<50?plain(value).color(TextFormatting.AQUA):
                                translation("geocraft.command.geoconfig.info.cur_value.out_of_range").color(TextFormatting.GOLD))
                        .arg(translation(item.getEffectiveMode().getTranslationKey())
                                .color(item.getEffectiveMode().getColor())
                                .bold(true))
                        .color(TextFormatting.GREEN)
                        .done());
                return;
            }
        }
        throw new NickelRuntimeException(translation("geocraft.command.geoconfig.set.unsupported"));
    }

    @FunctionalInterface
    private interface Processor{
        void process(final @Nonnull Object item,final @Nonnull String val) throws CommandException;
    }

    @FunctionalInterface
    private interface ConfigProcessor<P extends ConfigItem<?,P>> extends Processor{
        void process(final @Nonnull P item,final @Nonnull String val) throws CommandException;

        @Override
        @SuppressWarnings("unchecked")
        default void process(@Nonnull final Object item, @Nonnull final String val) throws CommandException{
            this.process((P)item,val);
        }
    }
}
