//Nessya Nakache AG743463
//Elana Weiss 341390961

import groovy.xml.XmlParser

/**
 * The CompilationEngine class is responsible for parsing the tokens from a Jack source file.
 */
class CompilationEngine {
    
    private final output
    // the amount of spaces to indent each time we indent
    private final int INDENT_SIZE = 2
    // array of XML tokens parsed from the input stream
    private Node[] tokens = []
    // current token index
    private int tokenIndex = 0
    // VMWriter object to write VM commands to a file
    private VMWriter vmWriter
    // symbol tables for the class and subroutine
    private final SymbolTable symbolTable
    // if statements counter
    private int ifCounter = 0
    // while statements counter
    private int whileCounter = 0
    // current class name
    private String className

    // current indentation level
    private int indentLevel = 0

    // constructor that takes an input xml string and an output stream
    CompilationEngine(parseTreeOutputStream) {
        // initialize the input and output streams
        this.output = parseTreeOutputStream
        // create symbol table
        symbolTable = new SymbolTable()
    }

    // compile a Jack source file
    void compile(File jackFile) {
        // create a string writer
        def writer = new StringWriter()
        // tokenize the Jack file and write the output to the writer
        JackAnalyzer.tokenize(jackFile, writer)
        // get the tokens as a string
        def tokenXml = writer.toString()
        // parse the tokens from the input stream
        def root = new XmlParser().parseText(tokenXml)
        this.tokens = root.children()
        // initialize the token index
        this.tokenIndex = 0
        // vm file name
        def vmFileName = jackFile.path.replace(".jack", ".vm")
        // vm file object
        def vmFile = new File(vmFileName)
        // create a VMWriter object
        this.vmWriter = new VMWriter(vmFile)
        // get the class name from the file name
        this.className = jackFile.name.replace(".jack", "")
        // compile the class
        compileClass()
    }

    // get the type of a token
    static String getType(token) {
        return token.name()
    }

    // get the text of a token
    static String getText(token) {
        String text = token.text()
        // remove the first and last characters from the token text because they are spaces
        return text.substring(1, text.length() - 1)
    }

    // write a string to the output stream
    void writeString(String string) {
        output.println(" " * indentLevel + string)
    }

    // write the current token to the output stream
    void writeToken() {
        def token = tokens[tokenIndex++]
        def type = getType(token)
        def text = JackTokenizer.escapeToken(getText(token))
        writeString("<$type> $text </$type>")
    }

    // read the next token
    Node readToken() {
        def token = tokens[tokenIndex++]
        return token
    }

    // get the token that was just read
    Node getLastToken() {
        return tokenIndex > 0 ? tokens[tokenIndex - 1] : null
    }

    // get a token without advancing the index
    Node peekToken() {
        return tokens[tokenIndex]
    }

    // get the lookahead token without advancing the index
    Node peekLookahead() {
        return tokenIndex + 1 < tokens.size() ? tokens[tokenIndex + 1] : null
    }

    // throw a compilation error with the given message
    Node compilationError(String message) {
        // print context for debugging
        println (tokenIndex >= 3 ? tokens[tokenIndex - 3] : "")
        println (tokenIndex >= 2 ? tokens[tokenIndex - 2] : "")
        println (tokenIndex >= 1 ? tokens[tokenIndex - 1] : "")
        println ("---> " + tokens[tokenIndex] + " <---")
        println (tokenIndex + 1 < tokens.size() ? tokens[tokenIndex + 1] : "")
        println (tokenIndex + 2 < tokens.size() ? tokens[tokenIndex + 2] : "")
        println (tokenIndex + 3 < tokens.size() ? tokens[tokenIndex + 3] : "")
        // throw an exception with the error message
        throw new Exception("Compilation error: $message")
    }

    // increment the indentation level
    void indent() {
        indentLevel += INDENT_SIZE
    }

    // decrement the indentation level
    void unindent() {
        indentLevel -= INDENT_SIZE
    }

    // compile a class
    void compileClass() {
        // check that the current token is "class"
        if (getText(peekToken()) != "class") {
            compilationError("Expected 'class', got '${getText(peekToken())}' instead")
        }
        // write the start of the class
        writeString("<class>")
        // increment the indentation level
        indent()
        // write the class keyword
        writeToken()
        // write the class name if it is an identifier
        compileIdentifier()
        // write the opening curly brace
        if (getText(peekToken()) != "{") {
            compilationError("Expected '{', got '${getText(peekToken())}' instead")
        }
        writeToken()
        // compile the class var declarations
        while (getText(peekToken()) == "static" || getText(peekToken()) == "field") {
            compileClassVarDec()
        }
        // compile the subroutine declarations
        while (getText(peekToken()) == "constructor" || getText(peekToken()) == "function" || getText(peekToken()) == "method") {
            compileSubroutine()
        }
        // write the closing curly brace
        if (getText(peekToken()) != "}") {
            compilationError("Expected '}', got '${getText(peekToken())}' instead")
        }
        writeToken()
        // decrement the indentation level
        unindent()
        // write the end of the class
        writeString("</class>")
    }

    // compile an identifier
    void compileIdentifier() {
        if (getType(peekToken()) != "identifier") {
            compilationError("Expected an identifier, got '${getText(peekToken())}' instead")
        }
        writeToken()
    }

    // compile a class var declaration
    void compileClassVarDec() {
        // write the start of the class var declaration
        writeString("<classVarDec>")
        // increment the indentation level
        indent()
        // write the static or field keyword
        String kind = getText(peekToken())
        writeToken()
        // write the type
        String type = getText(peekToken())
        compileType()
        // write the variable name
        String name = getText(peekToken())
        compileIdentifier()
        // define the class variable in the symbol table
        symbolTable.define(name, type, kind)
        // write the rest of the variable names
        while (getText(peekToken()) == ",") {
            writeToken()  // write the comma
            name = getText(peekToken())
            compileIdentifier()
            // define the class variable in the symbol table
            symbolTable.define(name, type, kind)
        }
        // write the semicolon
        if (getText(peekToken()) != ";") {
            compilationError("Expected ';', got '${getText(peekToken())}' instead")
        }
        writeToken()
        // decrement the indentation level
        unindent()
        // write the end of the class var declaration
        writeString("</classVarDec>")
    }

    // compile a type
    void compileType() {
        if (getText(peekToken()).matches("int|char|boolean") || getType(peekToken()) == "identifier") {
            writeToken()
        } else {
            compilationError("Expected a type, got '${getText(peekToken())}' instead")
        }
    }

    // compile a subroutine
    void compileSubroutine() {
        // write the start of the subroutine
        writeString("<subroutineDec>")
        // increment the indentation level
        indent()
        // clear the subroutine symbol table
        symbolTable.startSubroutine()
        // write the constructor, function, or method keyword
        String subroutineKind = getText(peekToken())
        writeToken()
        // write the return type
        if (getText(peekToken()) == "void") {
            writeToken()
        } else {
            compileType()
        }
        // write the subroutine name
        String subroutineName = getText(peekToken())
        compileIdentifier()
        // write the opening parenthesis
        if (getText(peekToken()) != "(") {
            compilationError("Expected '(', got '${getText(peekToken())}' instead")
        }
        writeToken()
        // if the subroutine is a method, define "this" as an argument
        if (subroutineKind == "method") {
            symbolTable.define("this", className, "argument")
        }
        // compile the parameter list
        compileParameterList()
        // write the closing parenthesis
        if (getText(peekToken()) != ")") {
            compilationError("Expected ')', got '${getText(peekToken())}' instead")
        }
        writeToken()
        // compile the subroutine body
        compileSubroutineBody(subroutineKind, subroutineName)
        // decrement the indentation level
        unindent()
        // write the end of the subroutine
        writeString("</subroutineDec>")
    }

    // compile a parameter list
    void compileParameterList() {
        // write the start of the parameter list
        writeString("<parameterList>")
        // increment the indentation level
        indent()
        // if the next token is a type, compile the parameter
        if (getText(peekToken()).matches("int|char|boolean") || getType(peekToken()) == "identifier") {
            // write the type
            String type = getText(peekToken())
            compileType()
            // write the variable name
            String name = getText(peekToken())
            compileIdentifier()
            // define the parameter in the symbol table
            symbolTable.define(name, type, "argument")
            // write the rest of the parameters
            while (getText(peekToken()) == ",") {
                // write the comma
                writeToken()
                // write the type
                type = getText(peekToken())
                compileType()
                // write the variable name
                name = getText(peekToken())
                compileIdentifier()
                // define the parameter in the symbol table
                symbolTable.define(name, type, "argument")
            }
        }
        // decrement the indentation level
        unindent()
        // write the end of the parameter list
        writeString("</parameterList>")
    }

    // compile a subroutine body
    // @param subroutineKind the kind of subroutine (constructor, function, or method)
    // @param subroutineName the name of the subroutine
    void compileSubroutineBody(String subroutineKind, String subroutineName) {
        // write the start of the subroutine body
        writeString("<subroutineBody>")
        // increment the indentation level
        indent()
        // write the opening curly brace
        if (getText(peekToken()) != "{") {
            compilationError("Expected '{', got '${getText(peekToken())}' instead")
        }
        writeToken()
        // compile the var declarations
        while (getText(peekToken()) == "var") {
            compileVarDec()
        }
        // write the function statement in VM
        String functionName = className + "." + subroutineName
        int numLocals = symbolTable.varCount("local")
        vmWriter.writeFunction(functionName, numLocals)
        // if the subroutine is a constructor, allocate memory for the object fields
        if (subroutineKind == "constructor") {
            int numFields = symbolTable.varCount("this")
            // push the number of fields onto the stack as an argument
            vmWriter.writePush("constant", numFields)
            // call Memory.alloc - takes one argument, returns the address of the allocated memory
            vmWriter.writeCall("Memory.alloc", 1)
            // pop the address of the allocated memory into the "this" pointer
            vmWriter.writePop("pointer", 0)
        }
        // if the subroutine is a method, set the "this" pointer to the first argument
        if (subroutineKind == "method") {
            // push the argument 0 onto the stack (the address of the object memory)
            vmWriter.writePush("argument", 0)
            // pop the argument 0 into the "this" pointer
            vmWriter.writePop("pointer", 0)
        }
        // compile the statements
        compileStatements()
        // write the closing curly brace
        if (getText(peekToken()) != "}") {
            compilationError("Expected '}', got '${getText(peekToken())}' instead")
        }
        writeToken()
        // decrement the indentation level
        unindent()
        // write the end of the subroutine body
        writeString("</subroutineBody>")
    }

    // compile a var declaration
    void compileVarDec() {
        // write the start of the var declaration
        writeString("<varDec>")
        // increment the indentation level
        indent()
        // write the var keyword
        writeToken()
        // write the type
        String type = getText(peekToken())
        compileType()
        // write the variable name
        String name = getText(peekToken())
        compileIdentifier()
        // define the variable in the symbol table
        symbolTable.define(name, type, "local")
        // write the rest of the variable names
        while (getText(peekToken()) == ",") {
            writeToken()  // write the comma
            name = getText(peekToken())
            compileIdentifier()
            // define the variable in the symbol table
            symbolTable.define(name, type, "local")
        }
        // write the semicolon
        if (getText(peekToken()) != ";") {
            compilationError("Expected ';', got '${getText(peekToken())}' instead")
        }
        writeToken()
        // decrement the indentation level
        unindent()
        // write the end of the var declaration
        writeString("</varDec>")
    }

    // compile statements
    void compileStatements() {
        // write the start of the statements
        writeString("<statements>")
        // increment the indentation level
        indent()
        // compile the statements
        while (getText(peekToken()).matches("let|if|while|do|return")) {
            if (getText(peekToken()) == "let") {
                compileLet()
            } else if (getText(peekToken()) == "if") {
                compileIf()
            } else if (getText(peekToken()) == "while") {
                compileWhile()
            } else if (getText(peekToken()) == "do") {
                compileDo()
            } else if (getText(peekToken()) == "return") {
                compileReturn()
            }
        }
        // decrement the indentation level
        unindent()
        // write the end of the statements
        writeString("</statements>")
    }

    // parse a let statement
    void compileLet() {
        // write the start of the let statement
        writeString("<letStatement>")
        // increment the indentation level
        indent()
        // write the let keyword
        writeToken()
        // write the variable name
        String name = getText(peekToken())
        String kind = symbolTable.kindOf(name)
        int index = symbolTable.indexOf(name)
        compileIdentifier()
        // write the array index if it exists
        if (getText(peekToken()) == "[") {
            writeToken()  // write the opening square bracket
            // compile the expression and the index will be pushed onto the stack
            compileExpression()
            // push address of the variable onto the stack
            vmWriter.writePush(kind, index)
            // write the closing square bracket
            if (getText(peekToken()) != "]") {
                compilationError("Expected ']', got '${getText(peekToken())}' instead")
            }
            writeToken()  // write the closing square bracket
            // add the index to the base address
            vmWriter.writeArithmetic("add")
            // write the equals sign
            if (getText(peekToken()) != "=") {
                compilationError("Expected '=', got '${getText(peekToken())}' instead")
            }
            writeToken()
            // compile the expression
            compileExpression()
            // pop the result of the expression into the array
            vmWriter.writePop("temp", 0)  // store the expression result in temp 0
            vmWriter.writePop("pointer", 1)  // set THAT to the address of the array[i]
            vmWriter.writePush("temp", 0)  // push the expression result onto the stack
            vmWriter.writePop("that", 0)  // pop the expression result into array[i]
        } else {
            // write the equals sign
            if (getText(peekToken()) != "=") {
                compilationError("Expected '=', got '${getText(peekToken())}' instead")
            }
            writeToken()
            // compile the expression
            compileExpression()
        }
        // write the semicolon
        if (getText(peekToken()) != ";") {
            compilationError("Expected ';', got '${getText(peekToken())}' instead")
        }
        writeToken()
        // decrement the indentation level
        unindent()
        // write the end of the let statement
        writeString("</letStatement>")
    }

    // parse an if statement
    void compileIf() {
        // write the start of the if statement
        writeString("<ifStatement>")
        // increment the indentation level
        indent()
        // write the if keyword
        writeToken()
        // write the opening parenthesis
        if (getText(peekToken()) != "(") {
            compilationError("Expected '(', got '${getText(peekToken())}' instead")
        }
        writeToken()
        // compile the expression
        compileExpression()
        // write the closing parenthesis
        if (getText(peekToken()) != ")") {
            compilationError("Expected ')', got '${getText(peekToken())}' instead")
        }
        writeToken()
        // write the opening curly brace
        if (getText(peekToken()) != "{") {
            compilationError("Expected '{', got '${getText(peekToken())}' instead")
        }
        writeToken()
        // compile the statements
        compileStatements()
        // write the closing curly brace
        if (getText(peekToken()) != "}") {
            compilationError("Expected '}', got '${getText(peekToken())}' instead")
        }
        writeToken()
        // write the else clause if it exists
        if (getText(peekToken()) == "else") {
            writeToken()  // write the else keyword
            // write the opening curly brace
            if (getText(peekToken()) != "{") {
                compilationError("Expected '{', got '${getText(peekToken())}' instead")
            }
            writeToken()
            // compile the statements
            compileStatements()
            // write the closing curly brace
            if (getText(peekToken()) != "}") {
                compilationError("Expected '}', got '${getText(peekToken())}' instead")
            }
            writeToken()
        }
        // decrement the indentation level
        unindent()
        // write the end of the if statement
        writeString("</ifStatement>")
    }

    // parse a while statement
    void compileWhile() {
        // write the start of the while statement
        writeString("<whileStatement>")
        // increment the indentation level
        indent()
        // write the while keyword
        writeToken()
        // write the opening parenthesis
        if (getText(peekToken()) != "(") {
            compilationError("Expected '(', got '${getText(peekToken())}' instead")
        }
        writeToken()
        // compile the expression
        compileExpression()
        // write the closing parenthesis
        if (getText(peekToken()) != ")") {
            compilationError("Expected ')', got '${getText(peekToken())}' instead")
        }
        writeToken()
        // write the opening curly brace
        if (getText(peekToken()) != "{") {
            compilationError("Expected '{', got '${getText(peekToken())}' instead")
        }
        writeToken()
        // compile the statements
        compileStatements()
        // write the closing curly brace
        if (getText(peekToken()) != "}") {
            compilationError("Expected '}', got '${getText(peekToken())}' instead")
        }
        writeToken()
        // decrement the indentation level
        unindent()
        // write the end of the while statement
        writeString("</whileStatement>")
    }

    // parse a do statement
    void compileDo() {
        // write the start of the do statement
        writeString("<doStatement>")
        // increment the indentation level
        indent()
        // write the do keyword
        writeToken()
        // compile the subroutine call
        compileSubroutineCall()
        // write the semicolon
        if (getText(peekToken()) != ";") {
            compilationError("Expected ';', got '${getText(peekToken())}' instead")
        }
        writeToken()
        // decrement the indentation level
        unindent()
        // write the end of the do statement
        writeString("</doStatement>")
    }

    // parse a return statement
    void compileReturn() {
        // write the start of the return statement
        writeString("<returnStatement>")
        // increment the indentation level
        indent()
        // write the return keyword
        writeToken()
        // write the expression if it exists
        if (getText(peekToken()) != ";") {
            compileExpression()
        }
        // write the semicolon
        if (getText(peekToken()) != ";") {
            compilationError("Expected ';', got '${getText(peekToken())}' instead")
        }
        writeToken()
        // decrement the indentation level
        unindent()
        // write the end of the return statement
        writeString("</returnStatement>")
    }

    // parse an expression
    void compileExpression() {
        // write the start of the expression
        writeString("<expression>")
        // increment the indentation level
        indent()
        // compile the term
        compileTerm()
        // compile the rest of the terms
        while (getText(peekToken()) == "+" || getText(peekToken()) == "-" || getText(peekToken()) == "*" || getText(peekToken()) == "/" || getText(peekToken()) == "&" || getText(peekToken()) == "|" || getText(peekToken()) == "<" || getText(peekToken()) == ">" || getText(peekToken()) == "=") {
            writeToken()  // write the operator
            compileTerm()
        }
        // decrement the indentation level
        unindent()
        // write the end of the expression
        writeString("</expression>")
    }

    // parse a term
    void compileTerm() {
        // write the start of the term
        writeString("<term>")
        // increment the indentation level
        indent()
        // write the term
        // handle integer constant, string constant, keyword constant
        if (getType(peekToken()) == "integerConstant" || getType(peekToken()) == "stringConstant" || getText(peekToken()).matches("true|false|null|this")) {
            writeToken()
        }
        // handle varname, varName[expression], subroutineCall
        else if (getType(peekToken()) == "identifier") {
            writeToken()  // write the varName/className/subroutineName
            // handle varName[expression]
            if (getText(peekToken()) == "[") {
                writeToken()  // write the opening square bracket
                compileExpression()
                if (getText(peekToken()) != "]") {
                    compilationError("Expected ']', got '${getText(peekToken())}' instead")
                }
                writeToken()  // write the closing square bracket
            }
            // handle identifier.subroutineName subroutine call
            else if (getText(peekToken()) == ".") {
                writeToken()  // write the period
                compileIdentifier()  // write the subroutine name
            }
            // handle rest of subroutine call
            if (getText(peekToken()) == "(") {
                writeToken()  // write the opening parenthesis
                compileExpressionList()
                if (getText(peekToken()) != ")") {
                    compilationError("Expected ')', got '${getText(peekToken())}' instead")
                }
                writeToken()  // write the closing parenthesis
            }
        }
        // handle expression in parentheses
        else if (getText(peekToken()) == "(") {
            writeToken()  // write the opening parenthesis
            compileExpression()
            if (getText(peekToken()) != ")") {
                compilationError("Expected ')', got '${getText(peekToken())}' instead")
            }
            writeToken()  // write the closing parenthesis
        }
        // handle unary operator
        else if (getText(peekToken()) == "-" || getText(peekToken()) == "~") {
            writeToken()  // write the unary operator
            compileTerm()
        }
        // decrement the indentation level
        unindent()
        // write the end of the term
        writeString("</term>")
    }

    // parse a subroutine call
    void compileSubroutineCall() {
        // optionally write the class name or var name
        if (getType(peekToken()) == "identifier") {
            writeToken()  // write the varName/className/subroutineName
            // if the next token is a period, write it and the subroutine name
            if (getText(peekToken()) == ".") {
                writeToken()  // write the period
                compileIdentifier()  // write the subroutine name
            }
            // write the opening parenthesis
            if (getText(peekToken()) != "(") {
                compilationError("Expected '(', got '${getText(peekToken())}' instead")
            }
            writeToken()
            // compile the expression list
            compileExpressionList()
            // write the closing parenthesis
            if (getText(peekToken()) != ")") {
                compilationError("Expected ')', got '${getText(peekToken())}' instead")
            }
            writeToken()
        }
    }

    // parse an expression list
    // returns the number of expressions in the list
    int compileExpressionList() {
        int numExpressions = 0
        // write the start of the expression list
        writeString("<expressionList>")
        // increment the indentation level
        indent()
        // compile the expression if it exists
        if (getText(peekToken()) != ")") {
            compileExpression()
            numExpressions++
            // compile the rest of the expressions
            while (getText(peekToken()) == ",") {
                writeToken()  // write the comma
                compileExpression()
                numExpressions++
            }
        }
        // decrement the indentation level
        unindent()
        // write the end of the expression list
        writeString("</expressionList>")
        // return the number of expressions
        return numExpressions
    }
}
