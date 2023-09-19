package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.StreamSupport;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;

    private ArrayList<Token> tokens;

    private ArrayList<String> file_lines;

    enum State{
        ZERO,
        FORTEEN,
        SIXTEEN
    };

    public LexicalAnalyzer(SymbolTable symbolTable) {
        tokens = new ArrayList<>();
        file_lines = new ArrayList<>();
        this.symbolTable = symbolTable;
    }


    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        // TODO: 词法分析前的缓冲区实现
        // 可自由实现各类缓冲区
        // 或直接采用完整读入方法
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                file_lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        // TODO: 自动机实现的词法分析过程
        for (String line : file_lines) {
            State state = State.ZERO;
            StringBuilder sb = new StringBuilder();
            System.out.println(line);
            System.out.println("====================================");
            for (char c : line.toCharArray()) {
                switch (state) {
                    case FORTEEN:
                        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                            state = State.FORTEEN;
                            sb.append(c);
                        } else {
                            state = State.ZERO;
                            if (sb.toString().equals("int")) {
                                tokens.add(Token.normal(TokenKind.fromString("int"), ""));
                            } else if (sb.toString().equals("return")){
                                tokens.add(Token.normal(TokenKind.fromString("return"), ""));
                            } else {
                                tokens.add(Token.normal(TokenKind.fromString("id"), sb.toString()));
                                symbolTable.add(sb.toString());
                            }
                            sb = new StringBuilder();
                            System.out.println(tokens);
                        }
                        break;
                    case SIXTEEN:
                        if (c >= '0' && c <= '9') {
                            state = State.SIXTEEN;
                            sb.append(c);
                        } else {
                            state = State.ZERO;
                            tokens.add(Token.normal(TokenKind.fromString("IntConst"), sb.toString()));
                            sb = new StringBuilder();
                            System.out.println(tokens);
                        }

                        break;
                    default:
                        break;
                }

                if (state == State.ZERO) {
                    if (c == '\n' || c == ' ' || c == '\t') {
                        state = State.ZERO;
                    } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                        state = State.FORTEEN;
                        sb.append(c);
                    } else if (c >= '0' && c <= '9') {
                        state = State.SIXTEEN;
                        sb.append(c);
                    } else if (c == '*') {
                        tokens.add(Token.normal(TokenKind.fromString("*"), ""));
                    } else if (c == '=') {
                        tokens.add(Token.normal(TokenKind.fromString("="), ""));
                    } else if (c == '(') {
                        tokens.add(Token.normal(TokenKind.fromString("("), ""));
                    } else if (c == ')') {
                        tokens.add(Token.normal(TokenKind.fromString(")"), ""));
                    } else if (c == ';') {
                        tokens.add(Token.normal(TokenKind.fromString("Semicolon"), ""));
                    } else if (c == '+') {
                        tokens.add(Token.normal(TokenKind.fromString("+"), ""));
                    } else if (c == '-') {
                        tokens.add(Token.normal(TokenKind.fromString("-"), ""));
                    } else if (c == '/') {
                        tokens.add(Token.normal(TokenKind.fromString("/"), ""));
                    } else {

                    }
                }
            }
            System.out.println("==================");
        }

        tokens.add(Token.normal(TokenKind.eof(), ""));
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // TODO: 从词法分析过程中获取 Token 列表
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        return tokens;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }
}
