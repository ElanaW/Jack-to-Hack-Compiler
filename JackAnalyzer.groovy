//Nessya Nakache AG743463
//Elana Weiss 341390961

/**
 * The JackAnalyzer class tests the Jack tokenizer.
 * Prints the list of tokens and their types.
 */
class JackAnalyzer {

    static void main(String[] args) {
        // if the number of arguments is not 1, print usage and exit
        if (args.length != 1) {
            println "Usage: groovy JackAnalyzer.groovy <jack_file>"
            System.exit(1)
        }
        // create a file object for the .jack file
        def jackFile = new File(args[0])
        // if the file does not exist, print an error and exit
        if (!jackFile.exists()) {
            println "File not found: " + args[0]
            System.exit(1)
        }
        // create a JackTokenizer object
        def tokenizer = new JackTokenizer(jackFile)
        // while there are more tokens, print the token and its type
        System.out.println("<tokens>")
        while (tokenizer.hasMoreTokens()) {
            tokenizer.advance()
            def tokenType = tokenizer.tokenType()
            def token = tokenizer.token()
            System.out.println("<${tokenType}> ${token} </${tokenType}>")
        }
        System.out.println("</tokens>")
    }
}