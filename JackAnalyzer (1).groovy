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
        // print the output to the console
        tokenize(jackFile)
        // if there is a <name>T.xml file, compare the output with the expected output
        def expectedFile = new File(args[0].replace(".jack", "T.xml"))
        if (expectedFile.exists()) {
            // create a string writer to capture the output
            def writer = new StringWriter()
            // tokenize the Jack file and write the output to the writer
            tokenize(jackFile, writer)
            // compare the output with the expected output
            if (expectedFile.text == writer.toString()) {
                println "Success: Output matches expected output"
            } else {
                println "Error: Output does not match expected output"
            }
        }
    }

    // Tokenize the Jack file and print the tokens and their types
    // @param jackFile the .jack file to tokenize
    // @param output the output stream to print the tokens to
    static void tokenize(File jackFile, output = System.out) {
        // create a JackTokenizer object
        def tokenizer = new JackTokenizer(jackFile)
        // while there are more tokens, print the token and its type
        output.println("<tokens>")
        while (tokenizer.hasMoreTokens()) {
            tokenizer.advance()
            def tokenType = tokenizer.tokenType()
            def token = tokenizer.token()
            output.println("<${tokenType}> ${token} </${tokenType}>")
        }
        output.println("</tokens>")
    }
}