package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {
    public AssemblyGenerator() {
        for (int i = 0; i <= 6; i ++) {
            registers.put(i, false);
        }
    }

    class RegisterInfo {
        RegisterInfo() {
            refcnt = 1;
        }
        public int getRegisterNo() {
            return registerNo;
        }

        public void setRegisterNo(int registerNo) {
            this.registerNo = registerNo;
        }

        private int registerNo = Integer.MAX_VALUE;
        private int refcnt = 0;

        public void IncreaseRefcnt() {
            refcnt ++;
        }

        public void DecreaseRefcnt() {
            refcnt --;
        }

        public int getRefcnt() {
            return refcnt;
        }
    };

    // for register renaming
    // register coding: 0-6 [t0-t6]; register info: refcnt, occupied variable
    Map<Integer, Boolean> registers = new HashMap<>();
    Map<IRVariable, RegisterInfo> variables = new HashMap<>();
    List<Instruction> itrs = new ArrayList<>();
    List<String> asm = new ArrayList<>();

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        // TODO: 读入前端提供的中间代码并生成所需要的信息
        // a fix instruction mode: XX, variable, variable, imm/variable
        for (Instruction itr : originInstructions) {
            switch (itr.getKind()) {
                case MOV:
                    itrs.add(itr);
                    break;
                case ADD:
                    if (itr.getLHS().isImmediate() && itr.getRHS().isImmediate()) {
                        itrs.add(Instruction.createMov(itr.getResult(), IRImmediate.of(Integer.parseInt(itr.getRHS().toString()) + Integer.parseInt(itr.getLHS().toString()))));
                    } else if (itr.getLHS().isImmediate() || itr.getRHS().isImmediate()) {
                        IRValue var = itr.getLHS().isImmediate() ? itr.getRHS() : itr.getLHS();
                        IRValue imm = itr.getLHS().isImmediate() ? itr.getLHS() : itr.getRHS();
                        itrs.add(Instruction.createAdd(itr.getResult(), var, imm));
                    } else {
                        itrs.add(itr);
                    }
                    break;
                case SUB:
                    if (itr.getLHS().isImmediate() && itr.getRHS().isImmediate()) {
                        itrs.add(Instruction.createMov(itr.getResult(), IRImmediate.of(Integer.parseInt(itr.getLHS().toString()) - Integer.parseInt(itr.getRHS().toString()))));
                    } else if (itr.getLHS().isImmediate()) {
                        // change to a two-register calculation
                        IRVariable t = IRVariable.temp();
                        itrs.add(Instruction.createMov(t, itr.getLHS()));
                        itrs.add(Instruction.createSub(itr.getResult(), t, itr.getRHS()));
                    } else{
                        itrs.add(itr);
                    }
                    break;
                case MUL:
                    if (itr.getLHS().isImmediate() && itr.getRHS().isImmediate()) {
                        itrs.add(Instruction.createMov(itr.getResult(), IRImmediate.of(Integer.parseInt(itr.getRHS().toString()) * Integer.parseInt(itr.getLHS().toString()))));
                    } else if (itr.getLHS().isImmediate() || itr.getRHS().isImmediate()) {
                        // change to a three-register calculation
                        if (itr.getLHS().isImmediate()) {
                            IRVariable t = IRVariable.temp();
                            itrs.add(Instruction.createMov(t, itr.getLHS()));
                            itrs.add(Instruction.createMul(itr.getResult(), t, itr.getRHS()));
                        } else {
                            IRVariable t = IRVariable.temp();
                            itrs.add(Instruction.createMov(t, itr.getRHS()));
                            itrs.add(Instruction.createMul(itr.getResult(), itr.getLHS(), t));
                        }
                    } else {
                        itrs.add(itr);
                    }
                    break;
                case RET:
                    itrs.add(itr);
                    break;
                default:
                    break;
            }
        }

        // record the variable refcnt info
        for (Instruction itr : itrs) {
            switch (itr.getKind()) {
                case MOV:
                    if (variables.containsKey(itr.getResult())) {
                        variables.get(itr.getResult()).IncreaseRefcnt();
                    } else {
                        variables.put(itr.getResult(), new RegisterInfo());
                    }
                    if (itr.getFrom().isIRVariable()) {
                        if (variables.containsKey(itr.getFrom())) {
                            variables.get(itr.getFrom()).IncreaseRefcnt();
                        } else {
                            variables.put((IRVariable) (itr.getFrom()), new RegisterInfo());
                        }
                    }
                    break;
                case RET:
                    if (itr.getReturnValue().isIRVariable()) {
                        if (variables.containsKey(itr.getReturnValue())) {
                            variables.get(itr.getReturnValue()).IncreaseRefcnt();
                        } else {
                            variables.put((IRVariable) itr.getReturnValue(), new RegisterInfo());
                        }
                    }
                    break;
                case ADD:
                case SUB:
                case MUL:
                    if (variables.containsKey(itr.getResult())) {
                        variables.get(itr.getResult()).IncreaseRefcnt();
                    } else {
                        variables.put(itr.getResult(), new RegisterInfo());
                    }
                    if (itr.getLHS().isIRVariable()) {
                        if (variables.containsKey(itr.getLHS())) {
                            variables.get(itr.getLHS()).IncreaseRefcnt();
                        } else {
                            variables.put((IRVariable) itr.getLHS(), new RegisterInfo());
                        }
                    }
                    if (itr.getRHS().isIRVariable()) {
                        if (variables.containsKey(itr.getRHS())) {
                            variables.get(itr.getRHS()).IncreaseRefcnt();
                        } else {
                            variables.put((IRVariable) itr.getRHS(), new RegisterInfo());
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        // for debug
        for (Instruction itr : itrs) {
            System.out.println(itr);
        }
    }


    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        // TODO: 执行寄存器分配与代码生成
        asm.add(".text");
        for (Instruction itr : itrs) {
            StringBuilder sb = new StringBuilder();
            sb.append("\t");
            switch (itr.getKind()) {
                case MOV:
                    if (itr.getFrom().isImmediate()) {
                        // li reg, imm
                        int reg = getRegister(itr.getResult());
                        sb.append("li t" + new Integer(reg).toString() + ", " + Integer.parseInt(itr.getFrom().toString()));
                    } else {
                        // mv reg1, reg2
                        int reg1 = getRegister(itr.getResult());
                        int reg2 = getRegister(itr.getFrom());
                        if (reg1 != reg2)
                            sb.append("mv t" + new Integer(reg1).toString() + ", t" + new Integer(reg2).toString());
                    }
                    break;
                case RET:
                    if (itr.getReturnValue().isIRVariable()) {
                        int reg1 = getRegister(itr.getReturnValue());
                        sb.append("mv a0, t" + new Integer(reg1).toString());
                    } else {
                        sb.append("mv a0, " + itr.getReturnValue().toString());
                    }
                    break;
                case ADD:
                    int reg1 = getRegister(itr.getResult());
                    int reg2 = getRegister(itr.getLHS());
                    if (itr.getRHS().isIRVariable()) {
                        int reg3 = getRegister(itr.getRHS());
                        sb.append("add t" + new Integer(reg1).toString() + ", t" + new Integer(reg2).toString() + ", t" + new Integer(reg3).toString());
                    } else {
                        sb.append("addi t" + new Integer(reg1).toString() + ", t" + new Integer(reg2).toString() + ", " + itr.getRHS().toString());
                    }
                    break;
                case SUB: // mode: subi reg1, reg2, imm  |  sub reg1, reg2, reg3
                    reg1 = getRegister(itr.getResult());
                    reg2 = getRegister(itr.getLHS());
                    if (itr.getRHS().isIRVariable()) {
                        int reg3 = getRegister(itr.getRHS());
                        sb.append("sub t" + new Integer(reg1).toString() + ", t" + new Integer(reg2).toString() + ", t" + new Integer(reg3).toString());
                    } else {
                        sb.append("subi t" + new Integer(reg1).toString() + ", t" + new Integer(reg2).toString() + ", " + itr.getRHS().toString());
                    }
                    break;
                case MUL:
                    reg1 = getRegister(itr.getResult());
                    reg2 = getRegister(itr.getLHS());
                    int reg3 = getRegister(itr.getRHS());
                    sb.append("mul t" + new Integer(reg1).toString() + ", t" + new Integer(reg2).toString() + ", t" + new Integer(reg3).toString());
                    break;
                default:
                    break;
            }
            sb.append("\t\t# " + itr.toString());
            asm.add(sb.toString());
            proccessInvalid();
        }
    }

    private void proccessInvalid() {
        for (Map.Entry<IRVariable, RegisterInfo> en : variables.entrySet()) {
            if (en.getValue().getRefcnt() == 0 && en.getValue().getRegisterNo() != Integer.MAX_VALUE) {
                registers.put(en.getValue().registerNo, false); // release
                en.getValue().setRegisterNo(Integer.MAX_VALUE);
            }
        }
    }

    private int getRegister(IRValue result) {
        int registerNo = variables.get(result).getRegisterNo();
        if (registerNo != Integer.MAX_VALUE) {
            variables.get(result).DecreaseRefcnt();
            return registerNo;
        }

        // a steal
        for (Map.Entry<Integer, Boolean> en : registers.entrySet()) {
            if (en.getValue())  continue; // is occupied
            registerNo = en.getKey();
            variables.get(result).setRegisterNo(en.getKey());
            break;
        }

        if (registerNo == Integer.MAX_VALUE)
            throw new RuntimeException();
        registers.put(registerNo, true);
        variables.get(result).DecreaseRefcnt();
        return registerNo;
    }


    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        // TODO: 输出汇编代码到文件
        FileUtils.writeLines(path, asm.stream().toList());
    }
}

