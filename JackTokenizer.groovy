//Nessya Nakache AG743463
//Elana Weiss 341390961

/**
 * The JackTokenizer class is responsible for tokenizing a Jack source file.
 * It takes a jackFile as input and reads the tokens from that file.
 */
class JackTokenizer {

    private final File jackFile
    private final DataInputStream jackInputStream
    private String currentToken
    private String nextToken

    // types of tokens
    static final String KEYWORD = "keyword"
    static final String SYMBOL = "symbol"
    static final String IDENTIFIER = "identifier"
    static final String INT_CONST = "integerConstant"
    static final String STRING_CONST = "stringConstant"

    // token identifiers
    static final String[] WHITESPACE = [" ", "\t", "\n", "\r", "\f"]
    static final String[] SYMBOLS = ["{", "}", "(", ")", "[", "]", ".", ",", ";", "+", "-", "*", "/", "&", "|", "<", ">", "=", "~"]
    static final String[] KEYWORDS = ["class", "constructor", "function", "method", "field", "static", "var", "int", "char", "boolean", "void", "true", "false", "null", "this", "let", "do", "if", "else", "while", "return"]
    static final String IDENTIFIER_FIRST_SYMBOL_REGEX = "[a-zA-Z_]"
    static final String IDENTIFIER_SYMBOL_REGEX = "[a-zA-Z0-9_]"
    static final String IDENTIFIER_REGEX = IDENTIFIER_FIRST_SYMBOL_REGEX + IDENTIFIER_SYMBOL_REGEX + "*"
    static final String DIGIT_REGEX = "[0-9]"
    static final String INT_CONST_REGEX = DIGIT_REGEX + "+"

    // Constructor for the JackTokenizer class
    // @param jackFile the .jack file to tokenize
    JackTokenizer(File jackFile) {
        this.jackFile = jackFile
        this.jackInputStream = jackFile.newDataInputStream()
        this.currentToken = null
        this.nextToken = null
        loadNextToken()
    }

    // Check if there are more lines to process
    boolean hasMoreTokens() {
        return nextToken != null
    }

    // Gets the current token and advances the input
    void advance() {
        currentToken = nextToken
        loadNextToken()
    }

    // Returns the type of the current token
    String tokenType() {
        if (KEYWORDS.contains(currentToken)) {
            return KEYWORD
        } else if (SYMBOLS.contains(currentToken)) {
            return SYMBOL
        } else if (currentToken.matches(IDENTIFIER_REGEX)) {
            return IDENTIFIER
        } else if (currentToken.matches(INT_CONST_REGEX)) {
            return INT_CONST
        } else {
            return STRING_CONST
        }
    }

    // Returns the current token
    String token() {
        return escapeToken(currentToken)
    }

    // Load the next token from the file
    private void loadNextToken() {
        // clear the next token which will be set in the loop if there are more tokens
        nextToken = null
        // load the next character from the file
        String nextChar = loadNextChar()
        // loop until a token is found or the end of the file is reached
        while (nextChar != null) {
            // skip whitespace
            if (isWhitespace(nextChar)) {
                nextChar = loadNextChar()
                continue
            }

            // skip comments
            if (nextChar == "/") {
                nextChar = loadNextChar()
                // if the next characters are "//" then skip until the end of the line
                if (nextChar == "/") {
                    while (nextChar != null && nextChar != "\n") {
                        nextChar = loadNextChar()
                    }
                    continue
                }
                // if the next characters are "/*" then skip until "*/"
                else if (nextChar == "*") {
                    nextChar = loadNextChar()
                    while (nextChar != null) {
                        if (nextChar == "*") {
                            nextChar = loadNextChar()
                            if (nextChar == "/") {
                                break
                            }
                        } else {
                            nextChar = loadNextChar()
                        }
                    }
                    nextChar = loadNextChar()
                    continue
                }
                // if the next character is not a comment, move the input back one character and set the next character back to "/"
                else {
                    jackInputStream.skip(-1)
                    nextChar = "/"
                }
            }

            // check for symbols
            if (SYMBOLS.contains(nextChar)) {
                nextToken = nextChar
                return
            }

            // check for keywords or identifiers
            if (nextChar.matches(IDENTIFIER_FIRST_SYMBOL_REGEX)) {
                String token = nextChar
                nextChar = loadNextChar()
                while (nextChar != null && nextChar.matches(IDENTIFIER_SYMBOL_REGEX)) {
                    token += nextChar
                    nextChar = loadNextChar()
                }
                // move the input back one character so it can be read again for the next token
                if (nextChar != null) {
                    jackInputStream.skip(-1)
                }
                nextToken = token
                return
            }

            // check for string constants (eg. "hello" including the quotes)
            if (nextChar == "\"") {
                String token = ""
                nextChar = loadNextChar()
                while (nextChar != null && nextChar != "\"") {
                    token += nextChar
                    nextChar = loadNextChar()
                }
                nextToken = token
                return
            }

            // check for integer constants
            if (nextChar.matches(DIGIT_REGEX)) {
                String token = nextChar
                nextChar = loadNextChar()
                while (nextChar != null && nextChar.matches(DIGIT_REGEX)) {
                    token += nextChar
                    nextChar = loadNextChar()
                }
                // move the input back one character so it can be read again
                if (nextChar != null) {
                    jackInputStream.skip(-1)
                }
                nextToken = token
                return
            }

            throw new IllegalArgumentException("Invalid next character: '$nextChar'")
        }
    }

    // Load the next character from the file
    private String loadNextChar() {
        int nextCharCode = jackInputStream.read()
        if (nextCharCode == -1) {
            return null
        }
        return (char) nextCharCode
    }

    // check if the character is whitespace
    private boolean isWhitespace(String c) {
        return WHITESPACE.contains(c)
    }

    // xml escape the token
    private String escapeToken(String token) {
        return token.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    }
}