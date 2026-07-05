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

package moe.qingu.nickel.nbt;

import moe.qingu.nickel.command.exception.NickelRuntimeException;
import moe.qingu.nickel.command.exception.NickelScanEOFSignal;
import moe.qingu.nickel.command.reader.InputReader;
import moe.qingu.nickel.util.StringUtils;
import net.minecraft.command.CommandException;
import net.minecraft.nbt.*;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static moe.qingu.nickel.text.Texts.plain;
import static moe.qingu.nickel.text.Texts.translation;
import static moe.qingu.nickel.util.StringUtils.stringOf;

/**
 * @author QGMoe
 */
public final class SNBTReader {

    private final InputReader input;

    public SNBTReader(final @Nonnull InputReader input) {
        this.input = input;
    }

    @Nonnull
    public static NBTTagCompound readNBTFromInput(final @Nonnull InputReader input) throws CommandException {
        return new SNBTReader(input).readCompound();
    }

    public static void scanNBTFromInput(final @Nonnull InputReader input) throws CommandException {
        try{
            new SNBTReader(input).scanCompound();
        } catch (final NickelScanEOFSignal ignored) {}
    }

    @Nonnull
    public static NBTTagCompound readSingleNBTFromInput(final @Nonnull InputReader input) throws CommandException {
        final NBTTagCompound compound =  new SNBTReader(input).readCompound();
        if(input.canRead() && !Character.isWhitespace(input.peek())) return input.panic(input.getCursor(),"nickel.command.parameter.nbt.escape");
        return compound;
    }

    public static void scanSingleNBTFromInput(final @Nonnull InputReader input) throws CommandException {
        scanNBTFromInput(input);
        if(input.canRead() && !Character.isWhitespace(input.peek())) input.panic(input.getCursor(),"nickel.command.parameter.nbt.escape");
    }

    @Nonnull
    public NBTTagCompound readCompound() throws CommandException {
        expect('{');
        final NBTTagCompound compound = new NBTTagCompound();
        while (input.canRead() && input.peek() != '}'){
            final String key = this.readKey();
            input.skipWhitespaces();
            expect(':');
            compound.setTag(key,this.readValue('i'));
            if(this.shouldExit('}')) break;
        }
        expect('}');
        return compound;
    }

    public void scanCompound() throws CommandException, NickelScanEOFSignal {
        input.skipIf('{');
        while (input.canRead() && input.peek() != '}'){
            this.scanKey();
            input.skipWhitespaces();
            expectOrEnd(':');
            this.scanValue();
            if(this.shouldExit('}')) break;
        }
        expectOrEnd('}');
    }

    @Nonnull
    public NBTBase readListOrArray() throws CommandException {
        expect('[');
        input.skipWhitespaces();
        final int begin = input.getCursor();
        if(!input.canRead()) return input.panic(begin,translation("nickel.command.parameter.nbt.expected_value"));
        try{
            switch (input.peek()){
                case 'B':
                case 'I':
                case 'L':{
                    if(!input.canRead(2)) break;
                    final int cp = input.read();
                    if(input.read() != ';') break;
                    return readArray((char) Character.toLowerCase(cp));
                }
            }
        }finally {
            input.setCursor(begin);
        }
        return readList();
    }

    public void scanListOrArray() throws CommandException, NickelScanEOFSignal {
        input.skipIf('[');
        input.skipWhitespaces();
        if(!input.canRead()) throw NickelScanEOFSignal.INSTANCE;
        switch (input.read()){
            case 'B':
            case 'I':
            case 'L':{
                if(!input.canRead()) throw NickelScanEOFSignal.INSTANCE;
                if(input.read() != ';') input.unread();
            } default:input.unread();
        }
        while (input.canRead() && input.peek() != ']'){
            input.skipWhitespaces();
            this.scanValue();
            if(shouldExit(']')) break;
        }
        expectOrEnd(']');
    }

    @Nonnull
    private NBTTagList readList() throws CommandException {
        int type = -1;
        final @Nonnull NBTTagList list = new NBTTagList();
        while (input.canRead() && input.peek() != ']'){
            input.skipWhitespaces();
            final int begin = input.getCursor();
            final NBTBase value = this.readValue('i');
            if(type == -1) type = value.getId();
            else if(value.getId() != type) return input.panic(begin, translation("nickel.command.parameter.nbt.list.type_mismatch")
                    .arg(NBTBase.NBT_TYPES[type],NBTBase.NBT_TYPES[value.getId()]));
            list.appendTag(value);
            if(shouldExit(']')) break;
        }
        expect(']');
        return list;
    }


    @Nonnull
    @SuppressWarnings("unchecked")
    private NBTBase readArray(final char cp) throws CommandException {
        final int type = getArrayContentTypeByChar(cp);
        final @Nonnull List<Number> array = new ArrayList<>();
        while (input.canRead() && input.peek() != ']'){
            input.skipWhitespaces();
            final int begin = input.getCursor();
            final NBTBase value = this.readValue(cp);
            if(value.getId() != type && value.getId() > type) //对于整型类型，id越大，范围越大，小范围装不了大范围
                return input.panic(begin, translation("nickel.command.parameter.nbt.array.type_mismatch").arg(NBTBase.NBT_TYPES[type],NBTBase.NBT_TYPES[value.getId()]));
            final NBTPrimitive primitive = (NBTPrimitive) value;
            if(cp == 'b') array.add(primitive.getByte());
            else if(cp == 'l') array.add(primitive.getLong());
            else array.add(primitive.getInt());
            if(shouldExit(']')) break;
        }
        expect(']');
        switch (cp){
            case 'b': return new NBTTagByteArray((List<Byte>) (List<?>) array);
            case 'l': return new NBTTagLongArray((List<Long>) (List<?>) array);
            default:return new NBTTagIntArray((List<Integer>) (List<?>) array);
        }
    }

    private int getArrayContentTypeByChar(final char c){
        switch (c){
            case 'B': return Constants.NBT.TAG_BYTE;
            case 'L': return Constants.NBT.TAG_LONG;
            default:return Constants.NBT.TAG_INT;
        }
    }

    @Nonnull
    public String readKey() throws CommandException {
        input.skipWhitespaces();
        final int begin = input.getCursor();
        final String key;
        if(!input.canRead()) return input.panic(begin,translation("nickel.command.parameter.nbt.expected_key"));
        if(input.peek() == '"' || input.peek() == '\'') key = StringUtils.strip(input.readString());
        else key = readUnquotedString();
        if(begin == input.getCursor() || key.isEmpty()) return input.panic(begin,translation("nickel.command.parameter.nbt.empty_key"));
        return key;
    }

    public void scanKey() throws NickelScanEOFSignal {
        input.skipWhitespaces();
        if(!input.canRead()) throw NickelScanEOFSignal.INSTANCE;
        if(input.peek() == '"' || input.peek() == '\'') input.scanString();
        else this.scanUnquotedString();
    }

    @Nonnull
    public String readUnquotedString(){
        input.skipWhitespaces();
        final int begin = input.getCursor();
        while (input.canRead() && isAllowedUnquoted(input.peek())) input.skip();
        return StringUtils.strip(input.getSubInput(begin,input.getCursor()));
    }

    public void scanUnquotedString(){
        input.skipWhitespaces();
        while (input.canRead() && isAllowedUnquoted(input.peek())) input.skip();
    }

    @Nonnull
    public NBTBase readValue(final char defaultNumType) throws CommandException {
        input.skipWhitespaces();
        if(!input.canRead()) return input.panic(input.getCursor(),translation("nickel.command.parameter.nbt.expected_value"));
        switch (input.peek()){
            case '{': return readCompound();
            case '[': return readListOrArray();
            case '\'':
            case '"': return new NBTTagString(input.readString());
            default: return readTypedValue(defaultNumType);
        }
    }

    public void scanValue() throws CommandException, NickelScanEOFSignal {
        input.skipWhitespaces();
        if(!input.canRead()) throw NickelScanEOFSignal.INSTANCE;
        switch (input.peek()){
            case '{': {
                scanCompound();
                break;
            }
            case '[': {
                scanListOrArray();
                break;
            }
            case '\'':
            case '"': {
                input.scanString();
                break;
            }
            default: this.scanTypedValue();
        }
    }

    @Nonnull
    public NBTBase readTypedValue(final char defaultNumType) throws CommandException {
        input.skipWhitespaces();
        final int begin = input.getCursor();
        final String raw = readUnquotedString();
        switch (raw) {
            case "":
                return input.panic(begin, translation("nickel.command.parameter.nbt.empty_value"));
            case "true":
                return new NBTTagByte((byte) 1);
            case "false":
                return new NBTTagByte((byte) 0);
        }
        if(input.peek() == '(') return readFunction(begin,raw);
        if(InputReader.isNumber(raw.charAt(0))){
            try{
                if(raw.startsWith("0x")) return parseInt(begin+2,raw.substring(2),16,false,false,defaultNumType);
                else if(raw.startsWith("+0x") || raw.startsWith("-0x"))
                    return parseInt(begin+3,(raw.charAt(0) + raw.substring(3)),16,false,true,defaultNumType);
                if(raw.length() > 2 && raw.startsWith("0b")) return parseInt(begin+2,raw.substring(2),2,false,false,defaultNumType); //等于2时是Byte 0
                else if(raw.length() > 3 && (raw.startsWith("+0b") || raw.startsWith("-0b")))
                    return parseInt(begin+3,raw.charAt(0) + raw.substring(3),2,false,true,defaultNumType);
                try {
                    return parseInt(begin,raw,10,true,true,defaultNumType);
                }catch (final CommandException | NumberFormatException e){
                    return parseFloat(begin,raw);
                }
            }catch (final NumberFormatException e){
                return input.panic(begin,translation("nickel.command.parameter.nbt.invalid_num",raw)
                        .hoverTo(HoverEvent.Action.SHOW_TEXT).content(plain(e.getLocalizedMessage())));
            }
        }else return new NBTTagString(raw);
    }

    public void scanTypedValue() throws CommandException, NickelScanEOFSignal {
        this.scanUnquotedString();
        if(input.peek() == '(') scanFunction();
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public NBTBase readFunction(final int begin,final @Nonnull String name) throws CommandException {
        expect('(');
        final List<NBTBase> args = new ArrayList<>();
        boolean beforeAnyArgs = true;
        while (input.canRead() && input.peek() != ')'){
            input.skipWhitespaces();
            if(beforeAnyArgs && input.peek() == ')') break; // func()
            beforeAnyArgs = false;
            args.add(this.readValue('i'));
            if(shouldExit(')')) break;
        }
        expect(')');
        final NBTBase[] argsArr = args.toArray(new NBTBase[0]);
        final @Nullable SNBTOperation func = SNBTOperations.resolve(name,argsArr);
        if(func == null) return input.panic(begin,translation("nickel.command.parameter.nbt.function.undefined")
                .arg(plain(name+'('+ args.stream()
                        .map(NBTBase::toString)
                        .collect(Collectors.joining(",")) +')'
                ).color(TextFormatting.GOLD).underlined(true))
                .arg(plain(name+new SNBTOperation.OperationType(args.stream()
                        .map(Object::getClass)
                        .toArray(Class[]::new))
                ).color(TextFormatting.AQUA).underlined(true)));
        try{
            return func.invoke(argsArr);
        }catch (final NickelRuntimeException e){
            return input.panic(begin,translation("nickel.command.parameter.nbt.function.run_exception",name+'('+args.size()+')')
                    .hoverTo(HoverEvent.Action.SHOW_TEXT).content(e.getInformation()));
        }catch (final Exception e){
            return input.panic(begin,translation("nickel.command.parameter.nbt.function.run_exception",name+'('+args.size()+')')
                    .hoverTo(HoverEvent.Action.SHOW_TEXT).content(e.getLocalizedMessage()));
        }
    }

    public void scanFunction() throws CommandException, NickelScanEOFSignal {
        input.skipIf('(');
        boolean beforeAnyArgs = true;
        while (input.canRead() && input.peek() != ')'){
            input.skipWhitespaces();
            if(beforeAnyArgs && input.peek() == ')') break; // func()
            beforeAnyArgs = false;
            this.scanValue();
            if(shouldExit(')')) break;
        }
        expectOrEnd(')');
    }

    @Nonnull
    public NBTBase parseInt(final int begin,
                            final @Nonnull String raw,
                            final int radix,
                            final boolean checkZero,
                            final boolean canSign,
                            final char defaultType) throws CommandException{
        boolean inDigit = false;
        boolean zero = false;
        boolean hasMark = false;
        boolean unsigned = false;
        int cursor = 0;
        loop: for(;cursor<raw.length();cursor++){
            final char cp = raw.charAt(cursor);
            final int digit = Character.digit(cp,radix);
            if(canSign && cursor==0 && (cp == '+' || cp == '-')) continue ;
            else if(digit != -1){
                if(checkZero && zero) return input.panic(begin + cursor,"nickel.command.parameter.nbt.num.invalid_0"); //已经有前导0
                if(digit == 0 && !inDigit) zero = true; //第一个0可以接受
                inDigit = true;
            }
            else if(inDigit){
                switch (cp){
                    case 'u':
                    case 'U': unsigned = true;
                    case 's':
                    case 'S': hasMark = unsigned || raw.length() > cursor+1; //signed的和Short符号一样，如后面还有则这必然是short
                    case 'b':
                    case 'B':
                    case 'l':
                    case 'L': break loop;
                    case '_':{
                        cursor = checkValid_(begin,raw,radix,cursor)-1;//continue会+1
                        continue;
                    }
                    default: return input.panic(begin+cursor,translation("nickel.command.parameter.nbt.int.invalid_code",cp));
                }
            } else return input.panic(begin+cursor,translation("nickel.command.parameter.nbt.int.invalid_code",cp));
        }
        final char type;
        if(hasMark){
            if(raw.length() == cursor+1) type = defaultType;
            else if(raw.length() == cursor+2) type = getIntType(begin+cursor+1,raw.charAt(cursor+1));
            else return input.panic(begin+cursor+2,"nickel.command.parameter.nbt.int.invalid_escape"); //最后最多两个字母了
        }else if(raw.length() > cursor+1)
            return input.panic(begin+cursor+1,"nickel.command.parameter.nbt.int.invalid_escape");  //没符号标记，数字后必须最多只剩下一个字母
        else if(raw.length() == cursor +1) type = getIntType(begin+cursor,raw.charAt(cursor));
        else type = defaultType;
        final String val = raw.substring(0,cursor).replace("_","");
        switch (type){
            case 'b': return new NBTTagByte((byte) (unsigned?validateRange(begin,Long.parseUnsignedLong(val,radix),0,255):Byte.parseByte(val,radix)));
            case 's': return new NBTTagShort((short) (unsigned?validateRange(begin,Long.parseUnsignedLong(val,radix),0,65535):Short.parseShort(val,radix)));
            case 'i': return new NBTTagInt(unsigned?Integer.parseUnsignedInt(val,radix):Integer.parseInt(val,radix));
            case 'l': return new NBTTagLong(unsigned?Long.parseUnsignedLong(val,radix):Long.parseLong(val,radix));
            default:return input.panic(begin,translation("nickel.command.parameter.nbt.int.invalid_type",type));
        }
    }

    @Nonnull
    public NBTBase parseFloat(final int begin,final @Nonnull String raw) throws CommandException{
        final int last = raw.charAt(raw.length()-1);
        final char type = last >= '0' && last <= '9' || last == '.' ? 'd': getFloatType(begin,raw);
        for(int i=0;i<raw.length();i++) if(raw.charAt(i) == '_') i = checkValid_(begin,raw,10,i);
        for(int i=raw.length()-1;i>=0;i--) if(raw.charAt(i) == '0') i = checkPreZeroInFloat(begin,raw,10,i);
        final String val = raw.replace("_","");
        switch (type){
            case 'f':{
                final float res = Float.parseFloat(val);
                if(Float.isInfinite(res) || Float.isNaN(res)) return input.panic(begin,"nickel.command.parameter.nbt.float.infinity");
                return new NBTTagFloat(res);
            }
            case 'd':{
                final double res = Double.parseDouble(val); //那些.1f、.1d、1.f、1.d、1ef之类的写法Java原生支持
                if(Double.isInfinite(res) || Double.isNaN(res)) return input.panic(begin,"nickel.command.parameter.nbt.float.infinity");
                return new NBTTagDouble(res);
            }
            default:return input.panic(begin,translation("nickel.command.parameter.nbt.float.invalid_type",type));
        }
    }

    /**
     * 检查非法的前导0
     * @param raw 字符串
     * @param radix 进制
     * @param loc 前导0位置
     * @return 跳过这个0向前的第一个非0位置的后一位
     */
    public int checkPreZeroInFloat(final int begin,final @Nonnull String raw, final int radix, final int loc) throws CommandException{
        int i=loc-1;
        while (i>=0 && raw.charAt(i) == '0') i--;
        if(i == loc -1 && (loc >= raw.length()-1 || Character.digit(raw.charAt(loc+1),radix) == -1)) return loc; //单个0也在最后，是没问题的，否则0后面接了数字
        if(i < 0 || raw.charAt(i) != '.' && Character.digit(raw.charAt(i),radix) == -1)
            return input.panic(begin+i+1,"nickel.command.parameter.nbt.num.invalid_0");//这一串0最前面不是数字或小数点，是非法的前导0
        else return i+1;
    }

    public int checkValid_(final int begin,final @Nonnull String raw,final int radix,final int loc) throws CommandException{
        int i=loc-1;
        int j=loc+1;
        while (i>=0 && raw.charAt(i) == '_') i--;
        while (j < raw.length() && raw.charAt(j) == '_') j++;
        if(i >= 0 && Character.digit(raw.charAt(i),radix) != -1 && j < raw.length() && Character.digit(raw.charAt(j),radix) != -1) return j;
        else return input.panic(begin+loc,"nickel.command.parameter.nbt.num.invalid_split");
    }

    private char getIntType(final int begin,final char cp) throws CommandException{
        switch (cp){
            case 'b':
            case 'B': return 'b';
            case 's':
            case 'S': return 's';
            case 'l':
            case 'L': return  'l';
            default:return input.panic(begin,translation("nickel.command.parameter.nbt.int.invalid_type",cp));
        }
    }

    private char getFloatType(final int begin,final @Nonnull String raw) throws CommandException{
        final char c = raw.charAt(raw.length()-1);
        switch (c){
            case 'f':
            case 'F': return 'f';
            case 'd':
            case 'D': return 'd';
            default:return input.panic(begin+raw.length()-1,translation("nickel.command.parameter.nbt.float.invalid_type",c));
        }
    }

    private long validateRange(final int begin,final long num,final long min,final long max) throws CommandException{
        if(num<min) return input.panic(begin,translation("nickel.command.parameter.nbt.int.too_min",num,min));
        if(num>max) return input.panic(begin,translation("nickel.command.parameter.nbt.int.too_max",num,max));
        return num;
    }

    public boolean shouldExit(final int end){
        input.skipWhitespaces();
        if(input.canRead() && input.peek() == ','){
            input.skip();
            input.skipWhitespaces();
            return input.canRead() && input.peek() == end;
        }return true;
    }

    public void expect(final int cp) throws CommandException {
        if(input.skipIf(cp)) return;
        input.panic(input.getCursor(),translation("nickel.command.parameter.nbt.unexpect",stringOf(cp),stringOf(input.peek())));
    }

    public void expectOrEnd(final int cp) throws CommandException, NickelScanEOFSignal{
        if(input.canRead()){
            if(input.peek() != cp) input.panic(input.getCursor(),translation("nickel.command.parameter.nbt.unexpect",stringOf(cp),stringOf(input.peek())));
            else input.skip();
        }else throw NickelScanEOFSignal.INSTANCE;
    }

    public static boolean isAllowedUnquoted(final int cp) {
        return cp >= '0' && cp <= '9' || cp >= 'A' && cp <= 'Z' || cp >= 'a' && cp <= 'z' || cp == '_' || cp == '-' || cp == '.' || cp == '+';
    }

}
