package dto;

public class TypeAlias {
    public Class<?> type;
    public String alias;

    TypeAlias(Class<?> type, String alias) {
        this.type = type;
        this.alias = alias;
    }
}
