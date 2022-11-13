package dto;

public class Result {
    String property;
    String column;

    public Result(String property, String column) {
        this.property = property;
        this.column = column;
    }

    public String getColumn() {
        return column;
    }
}
