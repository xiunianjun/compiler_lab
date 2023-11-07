package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.lexer.TokenKind;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {
    private Stack<IRValue> irStack = new Stack<>();
    private List<Instruction> irs = new ArrayList<>();

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO
        if (currentToken.getKindId().equals("IntConst")) {
            irStack.push(IRImmediate.of(Integer.parseInt(currentToken.getText())));
        } else if (currentToken.getKindId().equals("id")) {
            irStack.push(IRVariable.named(currentToken.getText()));
        }
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        if (irStack.empty()) {
            return;
        }
        // TODO
        switch (production.index()) {
            case 6:
                IRValue irv = irStack.peek();
                irStack.pop();
                irs.add(Instruction.createMov(((IRVariable)irStack.peek()), irv));
                irStack.pop();
                break;
            case 7:
                irs.add(Instruction.createRet(irStack.peek()));
                irStack.pop();
                break;
            case 8:
                IRValue lhs = irStack.peek();
                irStack.pop();
                IRValue rhs = irStack.peek();
                irStack.pop();
                IRVariable t = IRVariable.temp();
                irs.add(Instruction.createAdd(t, rhs, lhs));
                irStack.push(t);
                break;
            case 9:
                lhs = irStack.peek();
                irStack.pop();
                rhs = irStack.peek();
                irStack.pop();
                t = IRVariable.temp();
                irs.add(Instruction.createSub(t, rhs, lhs));
                irStack.push(t);
                break;
            case 11:
                lhs = irStack.peek();
                irStack.pop();
                rhs = irStack.peek();
                irStack.pop();
                t = IRVariable.temp();
                irs.add(Instruction.createMul(t, rhs, lhs));
                irStack.push(t);
                break;
        }
    }


    @Override
    public void whenAccept(Status currentStatus) {
        // TODO
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO
    }

    public List<Instruction> getIR() {
        // TODO
        return irs;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}

