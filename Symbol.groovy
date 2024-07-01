//Nessya Nakache AG743463
//Elana Weiss 341390961

/**
 * The Symbol represents a symbol in the symbol table with a name, type, kind, and index.
 */
class Symbol {

    private String name
    private String type
    private String kind
    private int index

    Symbol(String name, String type, String kind, int index) {
        this.name = name
        this.type = type
        this.kind = kind
        this.index = index
    }

    String getName() {
        return name
    }

    String getType() {
        return type
    }

    String getKind() {
        return kind
    }

    int getIndex() {
        return index
    }
}