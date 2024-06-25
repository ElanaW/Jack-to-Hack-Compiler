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
        return token.text().trim()
    }

    // write a string to the output stream
    void writeString(String string) {
        output.println(" " * indentLevel + string)
    }

    // write the current token to the output stream
    void writeToken() {
        def token = tokens[tokenIndex++]
        def type = getType(token)
        def text = getText(token)
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
        writeToken()
        // write the type
        compileType()
        // write the variable name
        compileIdentifier()
        // write the rest of the variable names
        while (getText(peekToken()) == ",") {
            writeToken()  // write the comma
            compileIdentifier()
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
        // write the constructor, function, or method keyword
        writeToken()
        // write the return type
        if (getText(peekToken()) == "void") {
            writeToken()
        } else {
            compileType()
        }
        // write the subroutine name
        compileIdentifier()
        // write the opening parenthesis
        if (getText(peekToken()) != "(") {
            compilationError("Expected '(', got '${getText(peekToken())}' instead")
        }
        writeToken()
        // compile the parameter list
        compileParameterList()
        // write the closing parenthesis
        if (getText(peekToken()) != ")") {
            compilationError("Expected ')', got '${getText(peekToken())}' instead")
        }
        writeToken()
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
            compileIdentifier()
            // write the rest of the parameters
            while (getText(peekToken()) == ",") {
                // write the comma
                writeToken()
                // write the type
                compileType()
                // write the variable name
                compileIdentifier()
            }
        }
        // decrement the indentation level
        unindent()
        // write the end of the parameter list
        writeString("</parameterList>")
    }

    // compile a subroutine body
    void compileSubroutineBody() {}

    // compile a var declaration
    void compileVarDec() {}

    // compile statements
    void compileStatements() {}

    // parse a let statement
    void compileLet() {}

    // parse an if statement
    void compileIf() {}

    // parse a while statement
    void compileWhile() {}

    // parse a do statement
    void compileDo() {}

    // parse a return statement
    void compileReturn() {}

    // parse an expression
    void compileExpression() {}

    // parse a term
    void compileTerm() {}

    // parse an expression list
    // returns the number of expressions in the list
    int compileExpressionList() {}
}