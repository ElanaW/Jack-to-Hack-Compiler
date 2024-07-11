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
    private final Node[] tokens = []
    // current token index
    private int tokenIndex = 0

    // current indentation level
    private int indentLevel = 0

    // constructor that takes an input xml string and an output stream
    CompilationEngine(String input, outputStream) {
        // initialize the input and output streams
        this.output = outputStream
        // parse the tokens from the input stream
        def root = new XmlParser().parseText(input)
        this.tokens = root.children()
    }

    static String getType(token) {
        return token.name()
    }

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

    // write the current token and check the text against an expected value
    // @param expected the expected text of the token
    // @throws CompilationError if the text of the token does not match the expected value
    void writeExpectedToken(String expected) {
        if (getText(peekToken()) != expected) {
            compilationError("Expected '$expected', got '${getText(peekToken())}' instead")
        }
        writeToken()
    }

    // write the current token and check the type against an expected value
    // @param expected the expected type of the token
    // @throws CompilationError if the type of the token does not match the expected value
    void writeExpectedType(String expected) {
        if (getType(peekToken()) != expected) {
            compilationError("Expected a token of type '$expected', got '${getType(peekToken())}' instead")
        }
        writeToken()
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
        // write the start of the class
        writeString("<class>")
        // increment the indentation level
        indent()
        // write the class keyword
        writeExpectedToken("class")
        // write the class name if it is an identifier
        writeExpectedType("identifier")
        // write the opening curly brace
        writeExpectedToken("{")
        // compile the class var declarations
        while (getText(peekToken()) == "static" || getText(peekToken()) == "field") {
            compileClassVarDec()
        }
        // compile the subroutine declarations
        while (getText(peekToken()) == "constructor" || getText(peekToken()) == "function" || getText(peekToken()) == "method") {
            compileSubroutine()
        }
        // write the closing curly brace
        writeExpectedToken("}")
        // decrement the indentation level
        unindent()
        // write the end of the class
        writeString("</class>")
    }

    // compile a class var declaration
    void compileClassVarDec() {
        // write the start of the class var declaration
        writeString("<classVarDec>")
        // increment the indentation level
        indent()
        // write the static or field keyword
        writeToken()
        // write the type
        compileType()
        // write the variable name
        writeExpectedType("identifier")
        // write the rest of the variable names
        while (getText(peekToken()) == ",") {
            writeExpectedToken(",")
            writeExpectedType("identifier")
        }
        // write the semicolon
        writeExpectedToken(";")
        // decrement the indentation level
        unindent()
        // write the end of the class var declaration
        writeString("</classVarDec>")
    }

    // compile a type
    // @throws CompilationError if the next token is not a type
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
        // write the constructor, function, or method keyword
        writeToken()
        // write the return type
        if (getText(peekToken()) == "void") {
            writeExpectedToken("void")
        } else {
            compileType()
        }
        // write the subroutine name
        writeExpectedType("identifier")
        // write the opening parenthesis
        writeExpectedToken("(")
        // compile the parameter list
        compileParameterList()
        // write the closing parenthesis
        writeExpectedToken(")")
        // compile the subroutine body
        compileSubroutineBody()
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
            compileType()
            // write the variable name
            writeExpectedType("identifier")
            // write the rest of the parameters
            while (getText(peekToken()) == ",") {
                // write the comma
                writeExpectedToken(",")
                // write the type
                compileType()
                // write the variable name
                writeExpectedType("identifier")
            }
        }
        // decrement the indentation level
        unindent()
        // write the end of the parameter list
        writeString("</parameterList>")
    }

    // compile a subroutine body
    void compileSubroutineBody() {
        // write the start of the subroutine body
        writeString("<subroutineBody>")
        // increment the indentation level
        indent()
        // write the opening curly brace
        writeExpectedToken("{")
        // compile the var declarations
        while (getText(peekToken()) == "var") {
            compileVarDec()
        }
        // compile the statements
        compileStatements()
        // write the closing curly brace
        writeExpectedToken("}")
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
        writeExpectedToken("var")
        // write the type
        compileType()
        // write the variable name
        writeExpectedType("identifier")
        // write the rest of the variable names
        while (getText(peekToken()) == ",") {
            writeExpectedToken(",")
            writeExpectedType("identifier")
        }
        // write the semicolon
        writeExpectedToken(";")
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
        writeExpectedToken("let")
        // write the variable name
        writeExpectedType("identifier")
        // write the array index if it exists
        if (getText(peekToken()) == "[") {
            writeExpectedToken("[")  // write the opening square bracket
            compileExpression()
            writeExpectedToken("]")  // write the closing square bracket
        }
        // write the equals sign
        writeExpectedToken("=")
        // compile the expression
        compileExpression()
        // write the semicolon
        writeExpectedToken(";")
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
        writeExpectedToken("if")
        // write the opening parenthesis
        writeExpectedToken("(")
        // compile the expression
        compileExpression()
        // write the closing parenthesis
        writeExpectedToken(")")
        // write the opening curly brace
        writeExpectedToken("{")
        // compile the statements
        compileStatements()
        // write the closing curly brace
        writeExpectedToken("}")
        // write the else clause if it exists
        if (getText(peekToken()) == "else") {
            // write the else keyword
            writeExpectedToken("else")
            // write the opening curly brace
            writeExpectedToken("{")
            // compile the statements
            compileStatements()
            // write the closing curly brace
            writeExpectedToken("}")
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
        writeExpectedToken("while")
        // write the opening parenthesis
        writeExpectedToken("(")
        // compile the expression
        compileExpression()
        // write the closing parenthesis
        writeExpectedToken(")")
        // write the opening curly brace
        writeExpectedToken("{")
        // compile the statements
        compileStatements()
        // write the closing curly brace
        writeExpectedToken("}")
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
        writeExpectedToken("do")
        // compile the subroutine call
        compileSubroutineCall()
        // write the semicolon
        writeExpectedToken(";")
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
        writeExpectedToken("return")
        // write the expression if it exists
        if (getText(peekToken()) != ";") {
            compileExpression()
        }
        // write the semicolon
        writeExpectedToken(";")
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
            String lookaheadToken = getText(peekLookahead())
            // if the next token is an opening square bracket, this is an array access
            if (lookaheadToken == "[") {
                writeExpectedType("identifier")  // write the variable name
                // write the opening square bracket
                writeExpectedToken("[")
                // compile the expression and the index will be pushed onto the stack
                compileExpression()
                // write the closing square bracket
                writeExpectedToken("]")
            }
            // if the next token is a period or opening parenthesis, this is a subroutine call
            else if (lookaheadToken == "." || lookaheadToken == "(") {
                compileSubroutineCall()
            }
            // if the next token is not an opening square bracket, period, or opening parenthesis, this is a variable
            else {
                writeExpectedType("identifier")  // write the variable name
            }
        }
        // handle expression in parentheses
        else if (getText(peekToken()) == "(") {
            writeExpectedToken("(")  // write the opening parenthesis
            compileExpression()
            writeExpectedToken(")")  // write the closing parenthesis
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
                writeExpectedToken(".")
                writeExpectedType("identifier")  // write the subroutine name
            }
            // write the opening parenthesis
            writeExpectedToken("(")
            // compile the expression list
            compileExpressionList()
            // write the closing parenthesis
            writeExpectedToken(")")
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
                writeExpectedToken(",")
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
