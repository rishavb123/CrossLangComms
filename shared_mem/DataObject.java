public class DataObject {
    
    private String name;
    private String type;
    private String value;

    public DataObject(String name, String type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return type + " " + value;
    }

    public static boolean validate(String type, String val) {
        return true;
    }

}
