package dto;

public class TypeAlias {
    Class<?> type;
    String alias;

    public TypeAlias(Class<?> type, String alias) {
        this.type = type;
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }

    public Class<?> getType() {
        return type;
    }
}
