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
    private String currentClassName

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
        this.currentClassName = jackFile.name.replace(".jack", "")
        // clear the symbol table
        symbolTable.startClass()
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
            symbolTable.define("this", currentClassName, "argument")
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
        String functionName = currentClassName + "." + subroutineName
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
            vmWriter.writeCommand("add")
            // write the equals sign
            if (getText(peekToken()) != "=") {
                compilationError("Expected '=', got '${getText(peekToken())}' instead")
            }
            writeToken()
            // compile the expression and the result will be pushed onto the stack
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
            // compile the expression and the result will be pushed onto the stack
            compileExpression()
            // pop the result of the expression into the variable
            vmWriter.writePop(kind, index)
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
        // set up labels for the if statement
        String ifTrueLabel = "IF_TRUE" + ifCounter
        String ifFalseLabel = "IF_FALSE" + ifCounter
        String ifEndLabel = "IF_END" + ifCounter
        ifCounter++
        // write the if keyword
        writeToken()
        // write the opening parenthesis
        if (getText(peekToken()) != "(") {
            compilationError("Expected '(', got '${getText(peekToken())}' instead")
        }
        writeToken()
        // compile the expression and the result will be pushed onto the stack
        compileExpression()
        // jump to the correct label based on the expression result
        vmWriter.writeIfGoto(ifTrueLabel)  // if the expression is true (non-zero), jump to the if true label
        vmWriter.writeGoto(ifFalseLabel)  // otherwise, jump to the false label
        // write the if true label
        vmWriter.writeLabel(ifTrueLabel)
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
            // if we completed the if statement, jump to the end
            vmWriter.writeGoto(ifEndLabel)
            // write the if false label
            vmWriter.writeLabel(ifFalseLabel)
            // write the else keyword
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
            // write the end label
            vmWriter.writeLabel(ifEndLabel)
        }
        // if there is no else clause, write the if false label at the end
        else {
            vmWriter.writeLabel(ifFalseLabel)
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
        // set up labels
        String whileLabel = "WHILE_EXP" + whileCounter
        String whileEndLabel = "WHILE_END" + whileCounter
        whileCounter++
        // write the while keyword
        writeToken()
        // write the while label
        vmWriter.writeLabel(whileLabel)
        // write the opening parenthesis
        if (getText(peekToken()) != "(") {
            compilationError("Expected '(', got '${getText(peekToken())}' instead")
        }
        writeToken()
        // compile the expression and the result will be pushed onto the stack
        compileExpression()
        // jump out of the loop if the expression is zero
        vmWriter.writeCommand("not")  // negate result so that we jump if the expression *is* zero
        vmWriter.writeIfGoto(whileEndLabel)  // if the negated result is not zero (expression was zero), jump to the end of the loop
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
        // write the end of the loop
        vmWriter.writeGoto(whileLabel)  // go back to the start of the loop
        vmWriter.writeLabel(whileEndLabel)  // end of the loop
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
        // pop the result of the subroutine call off the stack to discard it
        vmWriter.writePop("temp", 0)
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

    // parse a subroutine call
    void compileSubroutineCall() {
        // optionally write the class name or var name
        if (getType(peekToken()) == "identifier") {
            String name = getText(peekToken())
            Boolean isMethod = false  // if the subroutine is a method, we need to add the 'this' pointer as an argument
            String subroutineName = ""
            writeToken()  // write the varName/className/subroutineName
            // if the next token is a period, write it and the subroutine name
            if (getText(peekToken()) == ".") {
                writeToken()  // write the period
                // determine if this is a variable or a class name
                // if found in the symbol table, it is a variable, so the subroutine is a method
                String type = symbolTable.typeOf(name)
                if (type != null) {
                    isMethod = true
                    String kind = symbolTable.kindOf(name)
                    int index = symbolTable.indexOf(name)
                    vmWriter.writePush(kind, index)  // push the object onto the stack
                    // build name using the class name of the variable and the subroutine name
                    subroutineName = type + "." + getText(peekToken())
                }
                // if the variable is not found in the symbol table, it is a class name
                else {
                    // build name using the class name and the subroutine name
                    subroutineName = name + "." + getText(peekToken())
                }
                compileIdentifier()  // write the subroutine name
            } else {
                // if no "." is found, this is a method of the current class
                subroutineName = currentClassName + "." + name
                vmWriter.writePush("pointer", 0)  // push the "this" pointer onto the stack
                isMethod = true
            }
            // write the opening parenthesis
            if (getText(peekToken()) != "(") {
                compilationError("Expected '(', got '${getText(peekToken())}' instead")
            }
            writeToken()
            // compile the expression list
            int numArgs = compileExpressionList()
            if (isMethod) {
                numArgs++  // add the "this" pointer as an argument
            }
            vmWriter.writeCall(subroutineName, numArgs)
            // write the closing parenthesis
            if (getText(peekToken()) != ")") {
                compilationError("Expected ')', got '${getText(peekToken())}' instead")
            }
            writeToken()
        }
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
            // compile the expression and the result will be pushed onto the stack
            compileExpression()
        }
        // if there is no expression, push 0 onto the stack
        else {
            vmWriter.writePush("constant", 0)
        }
        // write the semicolon
        if (getText(peekToken()) != ";") {
            compilationError("Expected ';', got '${getText(peekToken())}' instead")
        }
        writeToken()
        // write the return statement in VM
        vmWriter.writeReturn()
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
        // mapping of symbols to VM arithmetic commands
        def opMap = [
            "+" : "add",
            "-" : "sub",
            "*" : "call Math.multiply 2",
            "/" : "call Math.divide 2",
            "&" : "and",
            "|" : "or",
            "<" : "lt",
            ">" : "gt",
            "=" : "eq"
        ]
        // compile the rest of the terms
        String nextToken = getText(peekToken())
        // if the next token is an operator, compile the next term and write the VM command for the operation
        while (opMap.containsKey(nextToken)) {
            writeToken()  // write the operator
            compileTerm()  // compile the term
            // write the VM command for the operator
            vmWriter.writeCommand(opMap[nextToken])
            // get the next token to check if there is another operator
            nextToken = getText(peekToken())
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
        // handle integer constant
        if (getType(peekToken()) == "integerConstant") {
            int value = Integer.parseInt(getText(peekToken()))
            writeToken()  // write the integer constant
            vmWriter.writePush("constant", value)
        }
        // handle string constant
        else if (getType(peekToken()) == "stringConstant") {
            String value = getText(peekToken())
            writeToken()  // write the string constant
            // push the length of the string onto the stack
            vmWriter.writePush("constant", value.length())
            // call String.new - takes one argument, returns the address of the allocated memory
            vmWriter.writeCall("String.new", 1)
            // push each character of the string onto the stack
            for (int i = 0; i < value.length(); i++) {
                vmWriter.writePush("constant", Character.codePointAt(value, i))
                vmWriter.writeCall("String.appendChar", 2)
            }
        }
        // handle keyword constants
        else if (getText(peekToken()).matches("true|false|null|this")) {
            String keyword = getText(peekToken())
            writeToken()  // write the keyword constant
            if (keyword == "true") {
                vmWriter.writePush("constant", 0)
                vmWriter.writeCommand("not")
            } else if (keyword == "false" || keyword == "null") {
                vmWriter.writePush("constant", 0)
            } else if (keyword == "this") {
                vmWriter.writePush("pointer", 0)
            }
        }
        // handle varname, varName[expression], subroutineCall
        else if (getType(peekToken()) == "identifier") {
            String lookaheadToken = getText(peekLookahead())
            // if the next token is an opening square bracket, this is an array access
            if (lookaheadToken == "[") {
                String name = getText(peekToken())
                String kind = symbolTable.kindOf(name)
                int index = symbolTable.indexOf(name)
                compileIdentifier()  // write the variable name
                // write the opening square bracket
                writeToken()
                // compile the expression and the index will be pushed onto the stack
                compileExpression()
                // push the address of the variable onto the stack
                vmWriter.writePush(kind, index)
                // add the index to the base address
                vmWriter.writeCommand("add")
                // pop the result of the expression into the array
                vmWriter.writePop("pointer", 1)  // set THAT to the address of the array[i]
                vmWriter.writePush("that", 0)  // push the value of the array[i] onto the stack
                // write the closing square bracket
                if (getText(peekToken()) != "]") {
                    compilationError("Expected ']', got '${getText(peekToken())}' instead")
                }
                writeToken()
            }
            // if the next token is a period or opening parenthesis, this is a subroutine call
            else if (lookaheadToken == "." || lookaheadToken == "(") {
                compileSubroutineCall()
            }
            // if the next token is not an opening square bracket, period, or opening parenthesis, this is a variable
            else {
                String name = getText(peekToken())
                String kind = symbolTable.kindOf(name)
                int index = symbolTable.indexOf(name)
                compileIdentifier()  // write the variable name
                // push the value of the variable onto the stack
                vmWriter.writePush(kind, index)
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
            String operator = getText(peekToken())
            writeToken()  // write the unary operator
            // compile the term and the result will be pushed onto the stack
            compileTerm()
            // write the VM command for the unary operator
            if (operator == "-") {
                vmWriter.writeCommand("neg")
            } else if (operator == "~") {
                vmWriter.writeCommand("not")
            }
        }
        // decrement the indentation level
        unindent()
        // write the end of the term
        writeString("</term>")
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
