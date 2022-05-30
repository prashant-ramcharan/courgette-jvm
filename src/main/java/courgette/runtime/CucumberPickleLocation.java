package courgette.runtime;

public class CucumberPickleLocation {

    private final int line;
    private final int column;

    public CucumberPickleLocation(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
