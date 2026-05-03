package xiacq.chunkcleaner.randomtick;

import java.util.List;

public class Goal {

    private final List<String> POSITIONS;
    private final boolean REQUIRED;
    private final boolean REQUIRED_ALL;
    private final String REPLACEMENT;

    public Goal(
            List<String> positions,
            boolean required,
            boolean requiredAll,
            String replacement
    ) {
        this.POSITIONS = positions;
        this.REQUIRED = required;
        this.REQUIRED_ALL = requiredAll;
        this.REPLACEMENT = replacement;
    }


    public List<String> returnPositions() {return this.POSITIONS;}
    public boolean returnRequired() {return this.REQUIRED;}
    public boolean returnRequiredAll() {return this.REQUIRED_ALL;}
    public String returnReplacement() {return this.REPLACEMENT;}

}
