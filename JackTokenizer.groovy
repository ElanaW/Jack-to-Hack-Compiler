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
    static final String[] WHITESPACE = [" ", "\t", "\n", "\r", "\f"]
    static final String[] SYMBOL = ["{", "}", "(", ")", "[", "]", ".", ",", ";", "+", "-", "*", "/", "&", "|", "<", ">", "=", "~"]
    static final String[] KEYWORD = ["class", "constructor", "function", "method", "field", "static", "var", "int", "char", "boolean", "void", "true", "false", "null", "this", "let", "do", "if", "else", "while", "return"]
    static final String IDENTIFIER_FIRST_SYMBOL_REGEX = "[a-zA-Z_]"
    static final String IDENTIFIER_SYMBOL_REGEX = "[a-zA-Z0-9_]"
    static final String DIGIT_REGEX = "[0-9]"

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
    boolean hasMoreTokens() {}

    // Gets the current token and advances the input
    void advance() {}

    // Returns the type of the current token
    String tokenType() {}

    // Returns the current token
    String token() {
        return currentToken
    }

    // Load the next token from the file
    private void loadNextToken() {}
}