package xiacq.chunkcleaner.randomtick;

public class Result {

    private final String POSITION;
    private final String RESULT_MATERIAL;
    private final int FILLING_RATE;

    public Result(String position, String resultMaterial, int filling) {
        this.POSITION = position;
        this.RESULT_MATERIAL = resultMaterial;
        this.FILLING_RATE = filling;
    }

    public String getPosition() {return this.POSITION;}
    public String getResultMaterial() {return this.RESULT_MATERIAL;}
    public int getFillingRate() {return this.FILLING_RATE;}
}
